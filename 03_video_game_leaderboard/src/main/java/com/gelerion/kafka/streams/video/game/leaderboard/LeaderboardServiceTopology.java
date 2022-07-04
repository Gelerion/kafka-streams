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
                .stream("score-events", Consumed.with(Serdes.ByteArray(), JsonSerdes.ScoreEvent()))
                // The score-events topic are unkeyed, but we’ll be joining with the players KTable, which is keyed
                // by player ID. Therefore, we need to rekey the data in score-events by player ID as well,
                // prior to performing the join
                .selectKey((k, v) -> v.getPlayerId().toString());

        // Create a partitioned (or sharded) table for the players topic, using the KTable abstraction
        KTable<String, Player> players = builder
                .table("players", Consumed.with(Serdes.String(), JsonSerdes.Player()));

        // Create a GlobalKTable for the products topic, which will be replicated in full to each application instance
        GlobalKTable<String, Product> products = builder
                .globalTable("products", Consumed.with(Serdes.String(), JsonSerdes.Product()));

        // Joins
        Joined<String, ScoreEvent, Player> playerJoinParams =
                Joined.with(Serdes.String(), JsonSerdes.ScoreEvent(), JsonSerdes.Player());

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
        withProducts.print(Printed.<String, Enriched>toSysOut().withLabel("with-products"));

        // Group the enriched product stream
        // Before you perform any stream or table aggregations in Kafka Streams, you must first group the KStream or
        // KTable that you plan to aggregate. The purpose of grouping is the same as rekeying records prior to joining:
        // to ensure the related records are processed by the same observer, or Kafka Streams task.

        //Since we want to calculate the high scores for each product ID, and since our enriched stream
        // is currently keyed by player ID we need to repartition
        KGroupedStream<String, Enriched> grouped = withProducts.groupBy(
                (key, value) -> value.getProductId().toString(),
                Grouped.with(Serdes.String(), JsonSerdes.Enriched()));

        // Aggregations
        // When a new key is seen by our Kafka Streams topology, we need some way of initializing the aggregation.
        // The interface that helps us with this is Initializer
        Initializer<HighScores> highScoresInitializer = HighScores::new;

        // Adder
        // The next thing we need to do in order to build a stream aggregator is to define the logic for combining
        // two aggregates. This is accomplished using the Aggregator interface
        Aggregator<String, Enriched, HighScores> highScoresAdder =
                (key, value, aggregate) -> aggregate.add(value);

        KTable<String, HighScores> highScores =
                grouped.aggregate(highScoresInitializer, highScoresAdder);

        return builder.build();
    }

}
