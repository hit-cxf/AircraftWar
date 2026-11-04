package edu.hitsz.dao;

import java.time.LocalDateTime;

public class ScoreRecord {
    private final String playerName;
    private final int score;
    private final LocalDateTime recordedAt;

    public ScoreRecord(String playerName, int score, LocalDateTime recordedAt) {
        this.playerName = playerName;
        this.score = score;
        this.recordedAt = recordedAt;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
