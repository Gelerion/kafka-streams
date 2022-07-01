package com.gelerion.kafka.streams.video.game.leaderboard.serialization.json;

import com.gelerion.kafka.streams.video.game.leaderboard.models.Player;
import com.gelerion.kafka.streams.video.game.leaderboard.models.Product;
import com.gelerion.kafka.streams.video.game.leaderboard.models.ScoreEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JsonSerdes {

//    public static Serde<HighScores> highScores() {
//        JsonSerializer<HighScores> serializer = new JsonSerializer<>();
//        JsonDeserializer<HighScores> deserializer = new JsonDeserializer<>(HighScores.class);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }

//    public static Serde<Enriched> enriched() {
//        JsonSerializer<Enriched> serializer = new JsonSerializer<>();
//        JsonDeserializer<Enriched> deserializer = new JsonDeserializer<>(Enriched.class);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }

    public static Serde<ScoreEvent> scoreEvent() {
        JsonSerializer<ScoreEvent> serializer = new JsonSerializer<>();
        JsonDeserializer<ScoreEvent> deserializer = new JsonDeserializer<>(ScoreEvent.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public static Serde<Player> player() {
        JsonSerializer<Player> serializer = new JsonSerializer<>();
        JsonDeserializer<Player> deserializer = new JsonDeserializer<>(Player.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public static Serde<Product> product() {
        JsonSerializer<Product> serializer = new JsonSerializer<>();
        JsonDeserializer<Product> deserializer = new JsonDeserializer<>(Product.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
