package com.gelerion.kafka.streams.crypto.sentiment.language;

import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;

public interface LanguageClient {

    Tweet translate(Tweet tweet, String targetLanguage);

}
