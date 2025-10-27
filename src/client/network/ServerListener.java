// src/client/network/ServerListener.java
package client.network;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

import shared.Message;

public class ServerListener implements Runnable {
    private final Socket socket;
    private final Client client; // Tham chiếu để gọi các hàm cập nhật UI
    private ObjectInputStream ois;

    public ServerListener(Socket socket, Client client) {
        this.socket = socket;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(socket.getInputStream());
            while (true) {
                // Đọc tin nhắn từ server
                Message serverMessage = (Message) ois.readObject();
                // Xử lý tin nhắn dựa trên loại của nó
                processMessage(serverMessage);
            }
        } catch (Exception e) {
            System.out.println("Mất kết nối với server. " + e.getMessage());
            // (Bạn có thể hiển thị thông báo lỗi cho người dùng ở đây)
        }
    }

    /**
     * Phân tích và gọi hàm xử lý tương ứng trong Client dựa trên loại tin nhắn.
     * @param message Tin nhắn từ server
     */
    @SuppressWarnings("unchecked") // Bỏ qua cảnh báo về ép kiểu không an toàn
    private void processMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_SUCCESS:
                // Payload chứa username
                client.onLoginSuccess((String) message.getPayload());
                break;
            case LOGIN_FAILURE:
                // Payload chứa thông báo lỗi
                client.onLoginFailure((String) message.getPayload());
                break;
            case USER_LIST_UPDATE:
                // Payload chứa List<String>
                client.onUserListUpdate((List<String>) message.getPayload());
                break;
            case CHALLENGE_RECEIVED:
                // Payload là username của người mời
                client.onChallengeReceived((String) message.getPayload());
                break;
            case REQUEST_USER_LIST:
                // Không cần xử lý ở client, đây là tin nhắn client -> server
                break;
            case CHALLENGE_DECLINED:
                // Payload là username của người từ chối
                client.onChallengeDeclined((String) message.getPayload());
                break;
            case GAME_START:
                // Payload là đối tượng Crossword
                client.onGameStart((server.game.Crossword) message.getPayload());
                break;
            case SUBMIT_ANSWER:
                // Không cần xử lý ở client, đây là tin nhắn client -> server
                break;
            case ANSWER_CORRECT:
                client.onAnswerCorrect((Object[]) message.getPayload());
                break;
            case ANSWER_WRONG:
                client.onAnswerWrong((String) message.getPayload());
                break;
            case GAME_OVER:
                client.onGameOver((String) message.getPayload());
                break;
            case GAME_CHAT_MESSAGE:
                client.onGameChatMessage((String) message.getPayload());
                break;
            case LEADERBOARD_UPDATE:
                client.onLeaderboardUpdate((List<shared.PlayerStats>) message.getPayload());
                break;
            case REGISTER_SUCCESS:
                client.onRegisterSuccess();
                break;
            case REGISTER_FAILURE:
                client.onRegisterFailure((String) message.getPayload());
                break;
            case MATCH_HISTORY_UPDATE:
                client.onMatchHistoryUpdate((List<shared.MatchHistoryEntry>) message.getPayload());
                break;
            default:
                System.out.println("Received unknown message from server: " + message.getType());
        }
    }
}