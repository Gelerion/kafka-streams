package com.gelerion.kafka.streams.hello.world;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class DslExample {

    public static void main(String[] args) {
        // The DSL example is exceptionally simple. We first need to use a Kafka Streams
        // class called StreamsBuilder to build our processor topology
        StreamsBuilder builder = new StreamsBuilder();

        // Next we need to a source processor in order to read data from a Kafka topic
        // There are a few different methods we could use here depending on how we decide to model our data,
        // but for now, let's model our data as a stream
        KStream<Void, String> stream = builder.stream("users");

        // Now it's time to add a stream processor
        stream.foreach((key, value) -> System.out.println("(DSL) Hello, " + value));

        // you can also print using the 'print' operator
        // stream.print(Printed.<String, String>toSysOut().withLabel("source"));

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Void().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
