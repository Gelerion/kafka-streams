package com.gelerion.kafka.streams.crypto.sentiment;

import com.gelerion.kafka.streams.crypto.sentiment.language.DummyLanguageClient;
import com.gelerion.kafka.streams.crypto.sentiment.language.LanguageClient;
import com.gelerion.kafka.streams.crypto.sentiment.model.EntitySentiment;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.json.TweetSerdes;
import com.mitchseymour.kafka.serialization.avro.AvroSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.List;

public class CryptoTopology {
    private static final List<String> currencies = Arrays.asList("bitcoin", "ethereum");

    @SuppressWarnings({"varargs", "unchecked"})
    public static Topology build() {
        LanguageClient languageClient = new DummyLanguageClient();
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], Tweet> stream = builder.stream(
                "tweets",
                Consumed.with(Serdes.ByteArray(), new TweetSerdes()));

        // The print operator allows us to easily view data as it flows through our application.
        // It is generally recommended for development use only
        stream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));

        // Filter retweets
        KStream<byte[], Tweet> filtered = stream.filterNot((key, tweet) -> tweet.isRetweet());

        // Branching Data
        // Branching is typically required when events need to be routed to different stream processing steps
        // or output topics based on some attribute of the event itself.
        Predicate<byte[], Tweet> englishTweets = (key, tweet) -> tweet.getLang().equals("en");
        Predicate<byte[], Tweet> nonEnglishTweets = (key, tweet) -> !tweet.getLang().equals("en");

        // Now that we have defined our branching conditions, we can leverage Kafka Streams’ branch operator,
        // which accepts one or more predicates and returns a list of output streams that correspond to each predicate.
        // Note that each predicate is evaluated in order, and a record can only be added to a single branch.
        // If a record doesn’t match any predicate, then it will be dropped:
        KStream<byte[], Tweet>[] branches = filtered.branch(englishTweets, nonEnglishTweets);

        // English tweets
        KStream<byte[], Tweet> englishStream = branches[0];
        englishStream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-english"));

        // non-English tweets
        KStream<byte[], Tweet> nonEnglishStream = branches[1];
        nonEnglishStream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-non-english"));

        // Translating events
        KStream<byte[], Tweet> translatedStream = nonEnglishStream
                .mapValues(tweet -> languageClient.translate(tweet, "en"));

        // Merging streams
        // The equivalent of a merge in the SQL is a union query
        // select column_name from table1
        // union
        // select column_name from table2
        KStream<byte[], Tweet> merged = englishStream.merge(translatedStream);

        // Enriching tweets with a sentiment score

        /*
        Example:
         In:
          #bitcoin is looking super strong. #ethereum has me worried though
         Out:
          {"entity": "bitcoin", "sentiment_score": 0.80}
          {"entity": "ethereum", "sentiment_score": -0.20}
         */
        KStream<byte[], EntitySentiment> enriched = merged.flatMapValues(tweet -> {
            List<EntitySentiment> sentiments = languageClient.getEntitySentiment(tweet);
            // remove all entities that don’t match one of the cryptocurrencies we are tracking
            sentiments.removeIf(sentiment -> !currencies.contains(sentiment.getEntity()));
            return sentiments;
        });

        // Serializing Avro Data
        // Kafka is a bytes-in, bytes-out stream processing platform. Therefore, in order to write the EntitySentiment
        // records to our output topic, we need to serialize these Avro records into byte arrays.

        /*
        When we serialize data using Avro, we have two choices:
         1. Include the Avro schema in each record.
         2. Use an even more compact format, by saving the Avro schema in Confluent Schema Registry, and only
            including a much smaller schema ID in each record instead of the entire schema
         */
        enriched.to(
                "crypto-sentiment",
                Produced.with(
                        Serdes.ByteArray(),
                        AvroSerdes.get(EntitySentiment.class)
                )
        );

        return builder.build();
    }

}
