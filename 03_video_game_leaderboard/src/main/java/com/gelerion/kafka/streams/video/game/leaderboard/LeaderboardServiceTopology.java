package com.gelerion.kafka.streams.video.game.leaderboard;

import com.gelerion.kafka.streams.video.game.leaderboard.models.Player;
import com.gelerion.kafka.streams.video.game.leaderboard.models.Product;
import com.gelerion.kafka.streams.video.game.leaderboard.models.ScoreEvent;
import com.gelerion.kafka.streams.video.game.leaderboard.models.join.Enriched;
import com.gelerion.kafka.streams.video.game.leaderboard.models.join.ScoreWithPlayer;
import com.gelerion.kafka.streams.video.game.leaderboard.serialization.json.JsonSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

public class LeaderboardServiceTopology {

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, ScoreEvent> scoreEvents = builder
                .stream("score-events", Consumed.with(Serdes.ByteArray(), JsonSerdes.scoreEvent()))
                // The score-events topic are unkeyed, but we’ll be joining with the players KTable, which is keyed
                // by player ID. Therefore, we need to rekey the data in score-events by player ID as well,
                // prior to performing the join
                .selectKey((k, v) -> v.getPlayerId().toString());

        // Create a partitioned (or sharded) table for the players topic, using the KTable abstraction
        KTable<String, Player> players = builder
                .table("players", Consumed.with(Serdes.String(), JsonSerdes.player()));

        // Create a GlobalKTable for the products topic, which will be replicated in full to each application instance
        GlobalKTable<String, Product> products = builder
                .globalTable("products", Consumed.with(Serdes.String(), JsonSerdes.product()));

        // Joins
        Joined<String, ScoreEvent, Player> playerJoinParams =
                Joined.with(Serdes.String(), JsonSerdes.scoreEvent(), JsonSerdes.player());

        ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner = ScoreWithPlayer::new;

        // Inner join KStream-KTable
        /*
        Using a stream-table join, incoming events in a stream can be joined against a table.
        Similar to a table-table join, this join is not windowed; however, the output of this operation
        is another stream, and not a table. In contrast to stream-stream and table-table join which are both symmetric,
        a stream-table join is asymmetric. By “symmetric” we mean that the arrival of each input (i.e., left or right)
        triggers a join computation and thus can result in a result record. However, for stream-table joins,
        only the (left) stream input triggers a join computation while (right) table input records only update
        the materialized table. Because the join is not-windowed, the (left) input stream is stateless and thus,
        join lookups from table record to stream records are not possible. The concept behind this semantics is
        the idea to enrich a data stream with auxiliary information.
         */
        KStream<String, ScoreWithPlayer> withPlayers =
                scoreEvents.join(players, scorePlayerJoiner, playerJoinParams);
        /*
        if you were to run the code at this point and then list all of the topics that are available in your Kafka
        cluster, you would see that Kafka Streams created two new internal topics for us.

        These topics are:
            - A repartition topic to handle the rekey operation that we performed.
            - A changelog topic for backing the state store, which is used by the join operator.
              This is part of the fault-tolerance
         */

        // KStream to GlobalKTable Join
        /*
         It is basically the same join as a KStream-KTable join. However, it yields different results because a
         GlobalKTable has different runtime behavior from a KTable.3 First, a GlobalKTable is completely populated
         before any processing is done. At startup, the GlobalKTables input topic’s end offsets are read and the
         topic is read up to this point populating the GlobalKTable before any processing starts. Second, if new
         updates are written to the GlobalKTables input topic, those updates are applied directly to the materialized table.
         */
        KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
                (leftKey, scoreWithPlayer) -> String.valueOf(scoreWithPlayer.getScoreEvent().getProductId());

        ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner = Enriched::new;

        KStream<String, Enriched> withProducts =
                withPlayers.join(products, keyMapper, productJoiner);

        return builder.build();
    }

}
