package com.gelerion.kafka.streams.crypto.sentiment.language;

import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;

public class DummyLanguageClient implements LanguageClient {

    @Override
    public Tweet translate(Tweet tweet, String targetLanguage) {
        tweet.setText("Translated: " + tweet.getText());
        return tweet;
    }
}
