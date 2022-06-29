package com.gelerion.kafka.streams.crypto.sentiment.serialization.avro;

import com.gelerion.kafka.streams.crypto.sentiment.model.EntitySentiment;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;

import java.util.Collections;
import java.util.Map;

public class AvroSerdes {

    /*
    The register-aware Avro Serdes requires us to configure the Schema Registry endpoint

    You can interact with Schema Registry directly for registering, deleting, modifying, or listing schemas.
    https://docs.confluent.io/platform/current/schema-registry/develop/api.html
    However, when using a registry-aware Avro Serdes from Kafka Streams, your schema will automatically be
    registered for you. Furthermore, to improve performance, the registry-aware Avro Serdes minimizes
    the number of schema lookups by caching schema IDs and schemas locally.
     */
    public static Serde<EntitySentiment> entitySentiment(String url) {
        boolean isKey = false;
        Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", url);
        Serde<EntitySentiment> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig, isKey);
        return serde;
    }
}
