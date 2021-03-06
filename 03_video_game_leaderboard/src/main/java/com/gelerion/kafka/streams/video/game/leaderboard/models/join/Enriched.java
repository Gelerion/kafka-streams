package com.gelerion.kafka.streams.video.game.leaderboard.models.join;

import com.gelerion.kafka.streams.video.game.leaderboard.models.Product;

public class Enriched implements Comparable<Enriched> {
    private final Long playerId;
    private final Long productId;
    private final String playerName;
    private final String gameName;
    private final Double score;

    public Enriched(ScoreWithPlayer scoreEventWithPlayer, Product product) {
        this.playerId = scoreEventWithPlayer.getPlayer().getId();
        this.productId = product.getId();
        this.playerName = scoreEventWithPlayer.getPlayer().getName();
        this.gameName = product.getName();
        this.score = scoreEventWithPlayer.getScoreEvent().getScore();
    }

    @Override
    // In order for the TreeSet to know how to sort Enriched objects (and therefore, be able to identify the Enriched
    // record with the lowest score to remove when our highScores aggregate exceeds three values
    public int compareTo(Enriched o) {
        return Double.compare(o.score, score);
    }

    public Long getPlayerId() {
        return this.playerId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getGameName() {
        return this.gameName;
    }

    public Double getScore() {
        return this.score;
    }

    @Override
    public String toString() {
        return "{"
                + " playerId='"
                + getPlayerId()
                + "'"
                + ", playerName='"
                + getPlayerName()
                + "'"
                + ", gameName='"
                + getGameName()
                + "'"
                + ", score='"
                + getScore()
                + "'"
                + "}";
    }
}
