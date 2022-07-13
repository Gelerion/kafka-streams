package com.gelerion.kafka.streams.patient.monitoring;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.HostInfo;

import java.util.Properties;

public class PatientMonitoringApp {

    public static void main(String[] args) {
        Topology topology = PatientMonitoringTopology.build();

        String host = "localhost";
        int port = 8091;
        String stateDir = "/tmp";
        String endpoint = String.format("%s:%s", host, port);

        // set the required properties for running Kafka Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, endpoint);
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);

        // build the topology
        System.out.println("Starting Patient Monitoring Application");
        KafkaStreams streams = new KafkaStreams(topology, props);
        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // clean up local state
        // you should run this sparingly in production since it will force the state
        // store to be rebuilt on start up
        streams.cleanUp();

        // start streaming
        streams.start();

        // start the REST service
        HostInfo hostInfo = new HostInfo(host, port);
        PatientMonitoringService service = new PatientMonitoringService(hostInfo, streams);
        service.start();
    }
}
