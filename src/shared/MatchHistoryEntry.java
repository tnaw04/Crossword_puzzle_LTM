// src/shared/MatchHistoryEntry.java
package shared;

import java.io.Serializable;
import java.util.Date;

public class MatchHistoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String opponent;
    private final int myScore;
    private final int opponentScore;
    private final String result; // "Thắng", "Thua", "Hòa"
    private final Date matchDate;

    public MatchHistoryEntry(String opponent, int myScore, int opponentScore, String result, Date matchDate) {
        this.opponent = opponent;
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.result = result;
        this.matchDate = matchDate;
    }

    public String getOpponent() {
        return opponent;
    }

    public int getMyScore() {
        return myScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public String getResult() {
        return result;
    }

    public Date getMatchDate() {
        return matchDate;
    }
}