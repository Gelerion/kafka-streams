package com.gelerion.kafka.streams.video.game.leaderboard.models.join;

import com.gelerion.kafka.streams.video.game.leaderboard.models.Player;
import com.gelerion.kafka.streams.video.game.leaderboard.models.ScoreEvent;

//The data class that we’ll use to construct the joined score-events -> players record
public class ScoreWithPlayer {
    private final ScoreEvent scoreEvent;
    private final Player player;

    public ScoreWithPlayer(ScoreEvent scoreEvent, Player player) {
        this.scoreEvent = scoreEvent;
        this.player = player;
    }

    public ScoreEvent getScoreEvent() {
        return this.scoreEvent;
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public String toString() {
        return "{" + " scoreEvent='" + getScoreEvent() + "'" + ", player='" + getPlayer() + "'" + "}";
    }
}
