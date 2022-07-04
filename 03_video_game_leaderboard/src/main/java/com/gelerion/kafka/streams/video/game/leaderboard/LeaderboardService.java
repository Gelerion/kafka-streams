package com.gelerion.kafka.streams.video.game.leaderboard;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

/**
 * To access local state we’ll use Javalin to implement a REST service due to its simple API.
 * We will also use OkHttp, developed by Square, for our REST client for its ease of use.
 */
public class LeaderboardService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;

    LeaderboardService(HostInfo hostInfo, KafkaStreams streams) {
        this.hostInfo = hostInfo;
        this.streams = streams;
    }

    ReadOnlyKeyValueStore<String, HighScores> getStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        "leaderboards",
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());
        app.get("/leaderboard/:key", this::getKey);
    }

    private void getKey(Context context) {
        String productId = context.pathParam("key");
        HighScores highScores = getStore().get(productId);
        context.json(highScores.toList());

    }

}
