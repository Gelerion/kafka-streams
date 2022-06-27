package com.gelerion.kafka.streams.crypto.sentiment;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

public class CryptoTopology {

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], byte[]> stream = builder.stream("tweets");
        // The print operator allows us to easily view data as it flows through our application.
        // It is generally recommended for development use only
        stream.print(Printed.<byte[], byte[]>toSysOut().withLabel("tweets-stream"));

        return builder.build();
    }

}
