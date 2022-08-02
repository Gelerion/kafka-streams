package com.gelerion.kafka.streams.digital.twin;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class DigitalTwinApp {
    private static final Config config = ConfigFactory.load().getConfig("streams");
    private static final Logger log = LoggerFactory.getLogger(DigitalTwinApp.class);

    public static void main(String[] args) {
        Topology topology = DigitalTwinTopology.build();

        // set the required properties for running Kafka Streams
        Properties props = new Properties();
        config.entrySet().forEach(e -> props.setProperty(e.getKey(), config.getString(e.getKey())));

        KafkaStreams streams = new KafkaStreams(topology, props);
        // clean up local state
        // you should run this sparingly in production since it will force the state
        // store to be rebuilt on start up
        streams.cleanUp();

        log.info("Starting Digital Twin Streams App");
        streams.start();
        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        @SuppressWarnings("StringSplitter")
        String[] endpointParts = config.getString(StreamsConfig.APPLICATION_SERVER_CONFIG).split(":");
        HostInfo hostInfo = new HostInfo(endpointParts[0], Integer.parseInt(endpointParts[1]));
        DigitalTwinService service = new DigitalTwinService(hostInfo, streams);
        log.info("Starting Digital Twin REST Service");
        service.start();

    }
}
