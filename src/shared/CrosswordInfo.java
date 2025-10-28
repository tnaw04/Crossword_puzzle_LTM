package shared;

import java.io.Serializable;

/**
 * Lớp này chứa thông tin tóm tắt về một bộ ô chữ (ID và chủ đề)
 * để gửi đến client cho việc lựa chọn.
 */
public class CrosswordInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String theme;

    public CrosswordInfo(int id, String theme) {
        this.id = id;
        this.theme = theme;
    }

    public int getId() { return id; }
    public String getTheme() { return theme; }

    // Phương thức này sẽ được dùng để hiển thị trong JComboBox của client
    @Override
    public String toString() {
        return String.format("Bộ %d - Chủ đề: %s", id, theme);
    }
}