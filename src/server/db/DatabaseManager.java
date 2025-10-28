// src/server/db/DatabaseManager.java
package server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import server.game.Crossword;
import server.game.Question;
import shared.MatchHistoryEntry;
import shared.CrosswordInfo;
import shared.PlayerStats;

/**
 * Lớp này quản lý tất cả các tương tác với cơ sở dữ liệu SQL Server.
 */
public class DatabaseManager {
    // Cấu hình kết nối CSDL trực tiếp
    private static final String DB_SERVER_HOST = "LAPTOP-L0C9IQ6J";
    private static final String DB_PORT = "1433";
    private static final String DB_NAME = "CrosswordDB";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "Nam562004@";

    // Khối static để đảm bảo trình điều khiển JDBC được tải khi lớp này được sử dụng lần đầu tiên.
    static {
        try {
            // Tải lớp trình điều khiển SQL Server một cách tường minh.
            // Điều này đăng ký trình điều khiển với DriverManager.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy trình điều khiển SQL Server JDBC.");
            System.err.println("Vui lòng đảm bảo tệp 'mssql-jdbc-....jar' nằm trong thư mục 'lib' và được thêm vào classpath.");
            e.printStackTrace();
            System.exit(1); // Dừng server nếu không có driver.
        }
    }

    private static final String CONNECTION_STRING = String.format(
            "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
            DB_SERVER_HOST, DB_PORT, DB_NAME
    );

    /**
     * Tạo và trả về một đối tượng Connection mới tới SQL Server.
     * @return Một đối tượng {@link Connection} đã được thiết lập.
     * @throws SQLException nếu không thể thiết lập kết nối.
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng với CSDL.
     * @param username Tên người dùng
     * @param password Mật khẩu (chưa mã hóa)
     * @return true nếu thông tin hợp lệ, ngược lại false.
     */
    public boolean validateUser(String username, String password) {
        // CẢNH BÁO: So sánh mật khẩu dạng văn bản thuần, không an toàn.
        String sql = "SELECT PasswordHash FROM Users WHERE Username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("PasswordHash");
                    // So sánh trực tiếp
                    return password.equals(storedPassword);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi xác thực người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        return false; // Trả về false nếu có lỗi hoặc không tìm thấy user
    }
    
    /**
     * Lấy một bộ ô chữ cụ thể từ cơ sở dữ liệu bằng ID.
     * @param crosswordId ID của bộ ô chữ cần lấy.
     * @return một đối tượng Crossword hoặc null nếu có lỗi.
     */
    public Crossword getCrosswordById(int crosswordId) {
        String sql = "SELECT CrosswordID, Theme, GridWidth, GridHeight, KeyWordClue, KeyWordAnswer FROM Crosswords WHERE CrosswordID = ?";
        String questionSql = "SELECT QuestionID, QuestionText, Answer, PositionRow, PositionCol, Direction, KeyWordIndex FROM Questions WHERE CrosswordID = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, crosswordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    List<Question> questions = new ArrayList<>();

                    try (PreparedStatement qStmt = conn.prepareStatement(questionSql)) {
                        qStmt.setInt(1, crosswordId);
                        try (ResultSet qRs = qStmt.executeQuery()) {
                            while (qRs.next()) {
                                questions.add(new Question(
                                    qRs.getInt("QuestionID"),
                                    qRs.getString("QuestionText"),
                                    qRs.getString("Answer"),
                                    // Chuyển đổi từ tọa độ 1-based (DB) sang 0-based (Java)
                                    qRs.getInt("PositionRow") - 1,
                                    qRs.getInt("PositionCol") - 1,
                                    qRs.getString("Direction").charAt(0),
                                    (Integer) qRs.getObject("KeyWordIndex") // Có thể là NULL
                                ));
                            }
                        }
                    }
                    int width = rs.getInt("GridWidth");
                    int height = rs.getInt("GridHeight");
                    String keyWordClue = rs.getString("KeyWordClue");
                    String keyWordAnswer = rs.getString("KeyWordAnswer");
                    return new Crossword(width, height, new char[height][width], questions, keyWordClue, keyWordAnswer);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy ô chữ: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách các bộ ô chữ có sẵn (ID và chủ đề).
     * @return Danh sách các CrosswordInfo.
     */
    public List<CrosswordInfo> getAvailableCrosswords() {
        List<CrosswordInfo> availableCrosswords = new ArrayList<>();
        String sql = "SELECT CrosswordID, Theme FROM Crosswords";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                availableCrosswords.add(new CrosswordInfo(
                    rs.getInt("CrosswordID"),
                    rs.getString("Theme")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách bộ ô chữ: " + e.getMessage());
            e.printStackTrace();
        }
        return availableCrosswords;
    }

    /**
     * Cập nhật điểm và số trận thắng cho người chơi sau trận đấu.
     * @param username Tên người dùng của người chiến thắng
     * @param scoreGained Số điểm nhận được
     * @param isWinner true nếu người chơi này thắng, false nếu thua hoặc hòa
     */
    public void updateUserStats(String username, int scoreGained, boolean isWinner) {
        String sql;
        if (isWinner) {
            sql = "UPDATE Users SET TotalScore = TotalScore + ?, Wins = Wins + 1 WHERE Username = ?";
        } else {
            sql = "UPDATE Users SET TotalScore = TotalScore + ? WHERE Username = ?";
        }
        
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Gán giá trị cho các tham số '?'
            pstmt.setInt(1, scoreGained);
            // Nếu là người thắng, có 2 tham số (score, username)
            // Nếu không, cũng có 2 tham số (score, username)
            // Cách viết cũ (isWinner ? 2 : 2) tuy đúng nhưng khó hiểu. Sửa lại cho rõ ràng.
            pstmt.setString(2, username);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật thông số người chơi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy danh sách người chơi hàng đầu từ cơ sở dữ liệu.
     * @return Danh sách PlayerStats
     */
    public List<PlayerStats> getLeaderboard() {
        List<PlayerStats> leaderboard = new ArrayList<>();
        // Lấy top 10 người chơi có điểm cao nhất
        String sql = "SELECT TOP 10 Username, TotalScore, Wins FROM Users ORDER BY TotalScore DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                leaderboard.add(new PlayerStats(
                    rs.getString("Username"),
                    rs.getInt("TotalScore"),
                    rs.getInt("Wins")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy bảng xếp hạng: " + e.getMessage());
        }
        return leaderboard;
    }

    /**
     * Đăng ký một người dùng mới.
     * @param username Tên người dùng
     * @param password Mật khẩu
     * @return 0 nếu thành công, 1 nếu tên người dùng đã tồn tại, -1 nếu có lỗi DB.
     */
    public int registerUser(String username, String password) {
        // 1. Kiểm tra xem username đã tồn tại chưa
        String checkSql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        try (Connection conn = connect();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return 1; // Tên người dùng đã tồn tại
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra người dùng tồn tại: " + e.getMessage());
            return -1; // Lỗi DB
        }

        // 2. Nếu chưa tồn tại, thêm người dùng mới
        // CẢNH BÁO: Lưu mật khẩu dưới dạng văn bản thuần, không an toàn.
        String insertSql = "INSERT INTO Users (Username, PasswordHash, TotalScore, Wins) VALUES (?, ?, 0, 0)";
        try (Connection conn = connect();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, password); // Lưu mật khẩu thuần
            int affectedRows = insertStmt.executeUpdate();
            return (affectedRows > 0) ? 0 : -1; // 0 nếu thành công
        } catch (SQLException e) {
            System.err.println("Lỗi khi đăng ký người dùng mới: " + e.getMessage());
            return -1; // Lỗi DB
        }
    }

    /**
     * Ghi lại kết quả một trận đấu vào cơ sở dữ liệu.
     * @param p1Username Tên người chơi 1
     * @param p2Username Tên người chơi 2
     * @param p1Score Điểm người chơi 1
     * @param p2Score Điểm người chơi 2
     * @param winnerUsername Tên người thắng (hoặc null nếu hòa)
     */
    public void recordMatch(String p1Username, String p2Username, int p1Score, int p2Score, String winnerUsername) {
        // Đổi tên bảng và các cột cho phù hợp với schema mới
        String sql = "INSERT INTO GameHistory (Player1_UserID, Player2_UserID, Player1_Score, Player2_Score, Winner_UserID) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect()) {
            // Lấy UserID từ Username
            Integer p1Id = getUserIdByUsername(conn, p1Username);
            Integer p2Id = getUserIdByUsername(conn, p2Username);
            Integer winnerId = getUserIdByUsername(conn, winnerUsername);

            if (p1Id == null || p2Id == null) {
                System.err.println("Lỗi khi ghi lịch sử: Không tìm thấy UserID cho người chơi.");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, p1Id);
                pstmt.setInt(2, p2Id);
                pstmt.setInt(3, p1Score);
                pstmt.setInt(4, p2Score);
                // Xử lý trường hợp hòa (winnerId là null)
                if (winnerId != null) {
                    pstmt.setInt(5, winnerId);
                } else {
                    pstmt.setNull(5, java.sql.Types.INTEGER);
                }
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi ghi lại lịch sử trận đấu: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử đấu của một người chơi.
     * @param username Tên người chơi cần xem
     * @return Danh sách các trận đấu
     */
    public List<MatchHistoryEntry> getMatchHistory(String username) {
        List<MatchHistoryEntry> history = new ArrayList<>();
        // Câu lệnh SQL mới JOIN 3 bảng để lấy Username từ UserID
        String sql = "SELECT " +
                     "   g.GameDate, g.Player1_Score, g.Player2_Score, " +
                     "   p1.Username AS Player1Username, " +
                     "   p2.Username AS Player2Username, " +
                     "   w.Username AS WinnerUsername " +
                     "FROM GameHistory g " +
                     "JOIN Users p1 ON g.Player1_UserID = p1.UserID " +
                     "JOIN Users p2 ON g.Player2_UserID = p2.UserID " +
                     "LEFT JOIN Users w ON g.Winner_UserID = w.UserID " + // LEFT JOIN vì có thể hòa (Winner_UserID là NULL)
                     "WHERE p1.Username = ? OR p2.Username = ? " +
                     "ORDER BY g.GameDate DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String p1 = rs.getString("Player1Username");
                    String p2 = rs.getString("Player2Username");
                    int score1 = rs.getInt("Player1_Score");
                    int score2 = rs.getInt("Player2_Score");
                    String winner = rs.getString("WinnerUsername"); // Có thể là null

                    boolean isPlayer1 = p1.equals(username);
                    String opponent = isPlayer1 ? p2 : p1;
                    int myScore = isPlayer1 ? score1 : score2;
                    int opponentScore = isPlayer1 ? score2 : score1;
                    String result = (winner == null) ? "Hòa" : (winner.equals(username) ? "Thắng" : "Thua");

                    history.add(new MatchHistoryEntry(opponent, myScore, opponentScore, result, rs.getTimestamp("GameDate")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy lịch sử đấu: " + e.getMessage());
        }
        return history;
    }

    /**
     * Phương thức trợ giúp để lấy UserID từ Username.
     */
    private Integer getUserIdByUsername(Connection conn, String username) throws SQLException {
        if (username == null) return null;
        String sql = "SELECT UserID FROM Users WHERE Username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UserID");
                }
            }
        }
        return null; // Không tìm thấy user
    }
}