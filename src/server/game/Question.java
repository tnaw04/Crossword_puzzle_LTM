package server.game;

import java.io.Serializable;

/**
 * Lớp này đại diện cho một câu hỏi và đáp án trong ô chữ.
 */
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String questionText;
    private final String answer;
    private final int row;
    private final int col;
    private final char direction; // 'H' for Horizontal, 'V' for Vertical

    public Question(int id, String questionText, String answer, int row, int col, char direction) {
        this.id = id;
        this.questionText = questionText;
        this.answer = answer;
        this.row = row;
        this.col = col;
        this.direction = direction;
    }

    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswer() {
        return answer;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public char getDirection() {
        return direction;
    }
}