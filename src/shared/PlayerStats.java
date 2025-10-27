// src/shared/PlayerStats.java
package shared;

import java.io.Serializable;

public class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final int totalScore;
    private final int wins;

    public PlayerStats(String username, int totalScore, int wins) {
        this.username = username;
        this.totalScore = totalScore;
        this.wins = wins;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getWins() {
        return wins;
    }
}