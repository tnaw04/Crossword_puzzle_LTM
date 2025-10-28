package server.game;

import java.io.Serializable;

/**
 * Lớp này đại diện cho một câu hỏi và đáp án trong ô chữ.
 */
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String questionText;
    private String answer;
    private int row; // 0-based
    private int col; // 0-based
    private char direction; // 'H' for horizontal, 'V' for vertical
    private boolean answered;
    private Integer keyWordIndex; // Vị trí của chữ cái này trong từ khóa (1-based), null nếu không phải

    public Question(int id, String questionText, String answer, int row, int col, char direction, Integer keyWordIndex) {
        this.id = id;
        this.questionText = questionText;
        this.answer = answer.toUpperCase(); // Chuẩn hóa đáp án
        this.row = row;
        this.col = col;
        this.direction = direction;
        this.answered = false;
        this.keyWordIndex = keyWordIndex;
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

    public boolean isAnswered() { return answered; }
    public Integer getKeyWordIndex() { return keyWordIndex; }

    // Setter
    public void setAnswered(boolean answered) { this.answered = answered; }

    /**
     * Kiểm tra xem một ô (cellRow, cellCol) có nằm trong câu hỏi này không.
     * @param cellRow Tọa độ hàng của ô cần kiểm tra (0-based)
     * @param cellCol Tọa độ cột của ô cần kiểm tra (0-based)
     * @return true nếu ô nằm trong câu hỏi, ngược lại false.
     */
    public boolean cellIsInQuestion(int cellRow, int cellCol) {
        if (direction == 'H') { // Hàng ngang
            return cellRow == this.row && cellCol >= this.col && cellCol < (this.col + this.answer.length());
        } else { // Hàng dọc
            return cellCol == this.col && cellRow >= this.row && cellRow < (this.row + this.answer.length());
        }
    }
}