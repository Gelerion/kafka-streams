package com.gelerion.kafka.streams.crypto.sentiment.language;

import com.gelerion.kafka.streams.crypto.sentiment.model.EntitySentiment;
import com.gelerion.kafka.streams.crypto.sentiment.serialization.Tweet;

import java.util.List;

public interface LanguageClient {

    Tweet translate(Tweet tweet, String targetLanguage);

    List<EntitySentiment> getEntitySentiment(Tweet tweet);

}
