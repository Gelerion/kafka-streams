package com.gelerion.kafka.streams.hello.world;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class ProcessorApiExample {

    public static void main(String[] args) {
        /*
        The Processor API lacks some abstractions available in the high-level DSL, and its syntax is more
        of a direct reminder that we’re building processor topologies, with methods like Topology.addSource,
        Topology.addProcessor, and Topology.addSink (the latter of which is not used in this example).
        The first step in using the processor topology is to instantiate a new Topology instance, like so:
         */
        Topology topology = new Topology();

        // Next, we will create a source processor to read data from the users' topic,
        // and a stream processor to print a simple greeting
        topology.addSource("UsersSource", "users");
        topology.addProcessor("SayHello", SayHelloProcessor::new, "UsersSource");

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev2");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Void().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming!
        KafkaStreams streams = new KafkaStreams(topology, config);
        System.out.println("Starting streams");
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
