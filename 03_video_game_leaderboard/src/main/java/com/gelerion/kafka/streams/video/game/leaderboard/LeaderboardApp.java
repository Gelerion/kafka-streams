package com.gelerion.kafka.streams.video.game.leaderboard;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class LeaderboardApp {
    public static void main(String[] args) {
        Topology topology = LeaderboardServiceTopology.build();

        // set the required properties for running Kafka Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");

        // We need some way of broadcasting which instances are running at any given point in time and where
        // they are running. The latter can be accomplished using the APPLICATION_SERVER_CONFIG parameter
        // to specify a host and port pair in Kafka Streams

        // Configure an endpoint. This will be communicated to other running application instances through Kafka’s
        // consumer group protocol. It’s important to use an IP and port pair that other instances can use to
        // communicate with your application (i.e., localhost would not work since it would resolve to different
        // IPs depending on the instance)
        // Note that setting the APPLICATION_SERVER_CONFIG parameter config doesn’t actually tell Kafka Streams to
        // start listening on whatever port you configure. In fact, Kafka Streams does not include a built-in RPC
        // service. However, this host/port information is transmitted to other running instances of your Kafka Streams
        // application and is made available through dedicated API methods
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "myapp:8080");

        // build the topology
        System.out.println("Starting Video-game Leaderboard");
        KafkaStreams streams = new KafkaStreams(topology, props);
        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        // start streaming!
        streams.start();
    }
}
