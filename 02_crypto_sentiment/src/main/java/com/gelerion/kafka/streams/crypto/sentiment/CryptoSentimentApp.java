package com.gelerion.kafka.streams.crypto.sentiment;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

/**
 * 1. Tweets that mention certain digital currencies (#bitcoin, #ethereum) should be consumed from a source
 *    topic called tweets:
 *      1. Since each record is JSON-encoded, we need to figure out how to properly deserialize
 *         these records into higher-level data classes.
 *      2. Unneeded fields should be removed during the deserialization process to simplify our code.
 *         Selecting only a subset of fields to work with is referred to as projection, and is one of
 *         the most common tasks in stream processing.
 * 2. Retweets should be excluded from processing. This will involve some form of data filtering.
 * 3. Tweets that aren’t written in English should be branched into a separate stream for translating.
 * 4. Non-English tweets need to be translated to English. This involves mapping one input value (the non-English tweet)
 *    to a new output value (an English-translated tweet).
 * 5. The newly translated tweets should be merged with the English tweets stream to create one unified stream.
 * 6. Each tweet should be enriched with a sentiment score, which indicates whether Twitter users are conveying
 *    positive or negative emotion when discussing certain digital currencies. Since a single tweet could mention
 *    multiple cryptocurrencies, we will demonstrate how to convert each input (tweet) into a variable number
 *    of outputs using a flatMap operator.
 * 6. The enriched tweets should be serialized using Avro, and written to an output topic called crypto-sentiment.
 *    Our fictional trading algorithm will read from this topic and make investment decisions based
 *    on the signals it sees.
 */
public class CryptoSentimentApp {

    public static void main(String[] args) {
        Topology topology = CryptoTopology.build();

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // build the topology and start streaming!
        KafkaStreams streams = new KafkaStreams(topology, config);

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        System.out.println("Starting Twitter streams");
        streams.start();
    }
}
