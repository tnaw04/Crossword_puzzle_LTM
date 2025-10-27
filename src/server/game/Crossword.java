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

    private final int width;
    private final int height;
    private final char[][] grid; // Lưới ô chữ, ví dụ: '#' cho ô đen
    private final List<Question> questions;

    public Crossword(int width, int height, char[][] grid, List<Question> questions) {
        this.width = width;
        this.height = height;
        this.grid = grid;
        this.questions = questions;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public char[][] getGrid() {
        return grid;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
