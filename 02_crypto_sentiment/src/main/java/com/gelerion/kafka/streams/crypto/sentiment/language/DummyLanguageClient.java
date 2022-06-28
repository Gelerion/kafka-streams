package com.gelerion.kafka.streams.crypto.sentiment.language;

import com.gelerion.kafka.streams.crypto.sentiment.model.EntitySentiment;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DummyLanguageClient implements LanguageClient {

    @Override
    public Tweet translate(Tweet tweet, String targetLanguage) {
        tweet.setText("Translated: " + tweet.getText());
        return tweet;
    }

    @Override
    public List<EntitySentiment> getEntitySentiment(Tweet tweet) {
        List<EntitySentiment> results = new ArrayList<>();

        Iterable<String> words = Splitter.on(' ')
                .split(tweet.getText().toLowerCase().replace("#", ""));

        for (String entity : words) {
            EntitySentiment entitySentiment =
                    EntitySentiment.newBuilder()
                            .setCreatedAt(tweet.getCreatedAt())
                            .setId(tweet.getId())
                            .setEntity(entity)
                            .setText(tweet.getText())
                            .setSalience(randomDouble())
                            .setSentimentScore(randomDouble())
                            .setSentimentMagnitude(randomDouble())
                            .build();

            results.add(entitySentiment);
        }
        return results;
    }

    Double randomDouble() {
        return ThreadLocalRandom.current().nextDouble(0, 1);
    }
}
