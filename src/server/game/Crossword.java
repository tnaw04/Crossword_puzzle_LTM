package server.game;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp này đại diện cho một bộ ô chữ hoàn chỉnh.
 * Nó chứa thông tin về lưới ô chữ và danh sách các câu hỏi.
 * Lớp này cần implement Serializable để có thể gửi đối tượng qua mạng.
 */
public class Crossword implements Serializable {
    private static final long serialVersionUID = 1L;

    private int width;
    private int height;
    private char[][] grid; // Có thể không cần thiết nếu chỉ lưu câu hỏi
    private List<Question> questions;
    private String keyWordClue;
    private String keyWordAnswer;

    public Crossword(int width, int height, char[][] grid, List<Question> questions, String keyWordClue, String keyWordAnswer) {
        this.width = width;
        this.height = height;
        this.grid = grid;
        this.questions = questions;
        this.keyWordClue = keyWordClue;
        this.keyWordAnswer = keyWordAnswer;
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Question> getQuestions() { return questions; }
    public String getKeyWordClue() { return keyWordClue; }
    public String getKeyWordAnswer() { return keyWordAnswer; }

    // Phương thức để kiểm tra xem tất cả các câu hỏi đã được giải chưa
    public boolean areAllQuestionsAnswered() {
        return questions.stream().allMatch(Question::isAnswered);
    }

    // Phương thức để lấy một câu hỏi theo ID
    public Question getQuestionById(int questionId) {
        return questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);
    }
}
