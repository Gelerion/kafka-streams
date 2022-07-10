package com.gelerion.kafka.streams.video.game.leaderboard;

import com.gelerion.kafka.streams.video.game.leaderboard.models.join.Enriched;
import io.javalin.Javalin;
import io.javalin.http.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To access local state we’ll use Javalin to implement a REST service due to its simple API.
 * We will also use OkHttp, developed by Square, for our REST client for its ease of use.
 */
public class LeaderboardService {
    private final HostInfo hostInfo;
    private final KafkaStreams streams;
    private String storeName = "leaderboards";

    LeaderboardService(HostInfo hostInfo, KafkaStreams streams) {
        this.hostInfo = hostInfo;
        this.streams = streams;
    }

    void start() {
        Javalin app = Javalin.create().start(hostInfo.port());
        /* Local key-value store query: all entries */
        app.get("/leaderboard", this::getAll);
        /* Local key-value store query: approximate number of entries */
        app.get("/leaderboard/count/local", this::getCountLocal);

        app.get("/leaderboard/{key}", this::getKey);
        app.get("/leaderboard/count", this::getCount);
    }

    private void getCountLocal(Context context) {
        long count = 0L;
        try {
            count = getStore().approximateNumEntries();
        } catch (Exception e) {
            //log.error("Could not get local leaderboard count", e);
        } finally {
            context.result(String.valueOf(count));
        }
    }

    private void getCount(Context context) {
        long count = getStore().approximateNumEntries();

        for (StreamsMetadata metadata : streams.allMetadataForStore(storeName)) {
            if (!hostInfo.equals(metadata.hostInfo())) {
                continue;
            }
            count += fetchCountFromRemoteInstance(
                    metadata.hostInfo().host(),
                    metadata.hostInfo().port());
        }

        context.json(count);
    }

    long fetchCountFromRemoteInstance(String host, int port) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format("http://%s:%d/leaderboard/count/local", host, port);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            return Long.parseLong(response.body().string());
        } catch (Exception e) {
            // log.error("Could not get leaderboard count", e);
            return 0L;
        }
    }

    private void getAll(Context ctx) {
        Map<String, List<Enriched>> leaderboard = new HashMap<>();

        KeyValueIterator<String, HighScores> range = getStore().all();
        while (range.hasNext()) {
            KeyValue<String, HighScores> next = range.next();
            String game = next.key;
            HighScores highScores = next.value;
            leaderboard.put(game, highScores.toList());
        }
        // close the iterator to avoid memory leaks!
        range.close();
        // return a JSON response
        ctx.json(leaderboard);
    }

    private void getKey(Context context) {
        String productId = context.pathParam("key");

        //discover the application instance (local or remote) that a specific key lives on
        KeyQueryMetadata metadata = streams
                .queryMetadataForKey(storeName, productId, Serdes.String().serializer());

        if (hostInfo.equals(metadata.activeHost())) {
            HighScores highScores = getStore().get(productId);

            // The queryMetadataForKey method doesn't actually check to see if the key exists. It uses the default
            // stream partitioner to determine where the key would exist, if it existed. Therefore, we check
            // for null (which is returned if the key isn’t found) and return a 404 response if it doesn't exist.
            if (highScores == null) {
                // game wasn't found
                context.status(404);
                return;
            }
            // game was found, so return the high scores
            context.json(highScores.toList());
        }

        // a remote instance has the key
        String remoteHost = metadata.activeHost().host();
        int remotePort = metadata.activeHost().port();
        String url =
                String.format(
                        "http://%s:%d/leaderboard/%s",
                        remoteHost, remotePort, productId);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            context.result(response.body().string());
        } catch (Exception e) {
            context.status(500);
        }
    }

    ReadOnlyKeyValueStore<String, HighScores> getStore() {
        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        storeName,
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }

}
