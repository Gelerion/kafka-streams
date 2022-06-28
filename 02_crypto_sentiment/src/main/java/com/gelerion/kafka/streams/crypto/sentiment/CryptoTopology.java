package com.gelerion.kafka.streams.crypto.sentiment;

import com.gelerion.kafka.streams.crypto.sentiment.language.DummyLanguageClient;
import com.gelerion.kafka.streams.crypto.sentiment.language.LanguageClient;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.json.TweetSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Printed;

public class CryptoTopology {

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

        return builder.build();
    }

}
