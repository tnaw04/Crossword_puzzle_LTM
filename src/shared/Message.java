
package shared;

import java.io.Serializable;

public class Message implements Serializable {
    // serialVersionUID cần thiết cho Serializable
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final Object payload; // Dữ liệu đi kèm

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    // Enum định nghĩa các loại tin nhắn có thể có
    public enum MessageType {
        // Client -> Server
        LOGIN_REQUEST,      // Yêu cầu đăng nhập, payload là String[] {username, password}
        CHALLENGE_REQUEST,  // Gửi lời mời chơi đến một người khác, payload là username của đối thủ
        CHALLENGE_RESPONSE, // Phản hồi lời mời, payload là String[] {challengerUsername, "ACCEPT"/"DECLINE"}
        REQUEST_USER_LIST,  // Client yêu cầu danh sách người dùng, payload là null
        SUBMIT_ANSWER,      // Client gửi câu trả lời, payload là Object[] {Integer questionId, String answer}
        GAME_CHAT_MESSAGE,  // Tin nhắn chat trong game, payload là String
        REQUEST_LEADERBOARD, // Client yêu cầu bảng xếp hạng
        REQUEST_MATCH_HISTORY, // Client yêu cầu lịch sử đấu
        REGISTER_REQUEST,   // Client yêu cầu đăng ký, payload là String[] {username, password}
        SUBMIT_CROSSWORD_CHOICE, // Client gửi ID bộ ô chữ đã chọn, payload là Integer
        
        // Server -> Client
        LOGIN_SUCCESS,     
        LOGIN_FAILURE,      // Đăng nhập thất bại, payload là String (lý do)
        USER_LIST_UPDATE,   // Cập nhật danh sách người dùng, payload là List<String>
        CHALLENGE_RECEIVED, // Nhận được lời mời, payload là username người mời
        CHALLENGE_DECLINED, // Thông báo lời mời bị từ chối, payload là username người từ chối
        GAME_START,         // Bắt đầu ván chơi, payload là đối tượng Crossword
        ANSWER_CORRECT,     // Trả lời đúng, payload là Object[] {Integer questionId, String answer, String username}
        ANSWER_WRONG,       // Trả lời sai, payload là String (thông báo)
        GAME_OVER,          // Kết thúc game, payload là String (thông báo kết quả)
        LEADERBOARD_UPDATE, // Gửi dữ liệu bảng xếp hạng, payload là List<PlayerStats>
        MATCH_HISTORY_UPDATE, // Gửi dữ liệu lịch sử đấu, payload là List<MatchHistoryEntry>
        REGISTER_SUCCESS,   // Đăng ký thành công
        REGISTER_FAILURE,   // Đăng ký thất bại, payload là String (lý do),
        REQUEST_CROSSWORD_CHOICE, // Yêu cầu client chọn bộ ô chữ, payload là List<CrosswordInfo>
        WAIT_FOR_CROSSWORD_CHOICE, // Yêu cầu client chờ người kia chọn, payload là String (tên người chọn)
    }
}