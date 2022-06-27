package com.gelerion.kafka.streams.crypto.sentiment;

import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.json.TweetSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

public class CryptoTopology {

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], Tweet> stream = builder.stream(
                "tweets",
                Consumed.with(Serdes.ByteArray(), new TweetSerdes()));

        // The print operator allows us to easily view data as it flows through our application.
        // It is generally recommended for development use only
        stream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));

        return builder.build();
    }

}
