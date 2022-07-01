package com.gelerion.kafka.streams.video.game.leaderboard.serialization.json;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JsonDeserializer<T> implements Deserializer<T> {
    private final Gson gson =
            new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

    private Class<T> destinationClass;
    private Type reflectionTypeToken;

    /** Default constructor needed by Kafka */
    public JsonDeserializer(Class<T> destinationClass) {
        this.destinationClass = destinationClass;
    }

    public JsonDeserializer(Type reflectionTypeToken) {
        this.reflectionTypeToken = reflectionTypeToken;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        Type type = destinationClass != null ? destinationClass : reflectionTypeToken;
        return gson.fromJson(new String(data, UTF_8), type);
    }
}
