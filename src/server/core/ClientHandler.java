// src/server/core/ClientHandler.java
package server.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import server.db.DatabaseManager;
import server.game.GameSession;
import shared.CrosswordInfo;
import shared.Message;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final DatabaseManager dbManager;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String username;
    private GameSession currentGameSession; // Tham chiếu đến phòng game hiện tại

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.dbManager = new DatabaseManager(); // Mỗi luồng có thể có DB manager riêng
    }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            // Vòng lặp lắng nghe tin nhắn từ client
            while (true) {
                Message clientMessage = (Message) ois.readObject();
                handleMessage(clientMessage);
            }
        } catch (IOException | ClassNotFoundException e) {
            // Khi client ngắt kết nối (đóng cửa sổ, mất mạng) hoặc có lỗi đọc dữ liệu,
            // một exception sẽ được ném ra và luồng sẽ đi vào khối catch này.
            System.out.println("Client " + (this.username != null ? this.username : socket.getInetAddress()) + " disconnected: " + e.getMessage());
            if (currentGameSession != null) {
                // Nếu người chơi đang trong một trận đấu, thông báo cho GameSession để xử lý.
                currentGameSession.handlePlayerDisconnect(this);
            }
            // Khi client ngắt kết nối hoặc có lỗi, luồng sẽ kết thúc
        } finally {
            server.removeUser(this.username);
            closeConnections();
        }
    }

    /**
     * Phân loại và xử lý tin nhắn từ client.
     * @param message Tin nhắn nhận được
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_REQUEST:
                handleLoginRequest((String[]) message.getPayload());
                break;
            case CHALLENGE_REQUEST:
                handleChallengeRequest((String) message.getPayload());
                break;
            case CHALLENGE_RESPONSE:
                handleChallengeResponse((String[]) message.getPayload());
                break;
            case REQUEST_USER_LIST:
                server.sendUserListTo(this);
                break;
            case SUBMIT_ANSWER:
                handleAnswerSubmission((Object[]) message.getPayload());
                break;
            case GAME_CHAT_MESSAGE:
                handleGameChatMessage((String) message.getPayload());
                break;
            case REQUEST_LEADERBOARD:
                handleLeaderboardRequest();
                break;
            case REGISTER_REQUEST:
                handleRegisterRequest((String[]) message.getPayload());
                break;
            case REQUEST_MATCH_HISTORY:
                handleMatchHistoryRequest();
                break;
            case SUBMIT_CROSSWORD_CHOICE:
                handleCrosswordChoice((Object[]) message.getPayload());
                break;
            // Thêm các case khác ở đây (ví dụ: CHALLENGE_REQUEST, SUBMIT_ANSWER...)
            default:
                System.out.println("Received unknown message type: " + message.getType());
        }
    }

    private void handleChallengeRequest(String opponentUsername) {
        ClientHandler opponentHandler = server.getClientHandler(opponentUsername);
        if (opponentHandler != null) {
            // Gửi lời mời đến đối thủ, payload là username của người mời (chính là client này)
            Message challengeMessage = new Message(Message.MessageType.CHALLENGE_RECEIVED, this.username);
            opponentHandler.sendMessage(challengeMessage);
            System.out.println("SERVER: " + this.username + " challenged " + opponentUsername);
        } else {
            // Thông báo lại cho người mời nếu đối thủ không online
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Người chơi " + opponentUsername + " không online."));
        }
    }

    private void handleChallengeResponse(String[] responsePayload) {
        String challengerUsername = responsePayload[0]; // Người đã gửi lời mời
        String response = responsePayload[1];           // "ACCEPT" hoặc "DECLINE"

        ClientHandler challengerHandler = server.getClientHandler(challengerUsername);
        if (challengerHandler == null) return; // Người mời đã offline

        if ("ACCEPT".equals(response)) {
            System.out.println("SERVER: " + this.username + " accepted challenge from " + challengerUsername);
            // Tung đồng xu để quyết định ai chọn bộ câu hỏi
            ClientHandler chooser = (Math.random() < 0.5) ? this : challengerHandler;
            ClientHandler waiter = (chooser == this) ? challengerHandler : this;

            // Lấy danh sách các bộ câu hỏi từ DB
            var availableCrosswords = dbManager.getAvailableCrosswords();

            if (availableCrosswords == null || availableCrosswords.isEmpty()) {
                // Xử lý lỗi nếu không có bộ câu hỏi nào
                sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Lỗi: Không tìm thấy bộ câu hỏi nào."));
                challengerHandler.sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Lỗi: Không tìm thấy bộ câu hỏi nào."));
                return;
            }

            // Gửi yêu cầu chọn cho người được chọn và yêu cầu chờ cho người còn lại
            chooser.sendMessage(new Message(Message.MessageType.REQUEST_CROSSWORD_CHOICE, new Object[]{availableCrosswords, waiter.getUsername()}));
            waiter.sendMessage(new Message(Message.MessageType.WAIT_FOR_CROSSWORD_CHOICE, chooser.getUsername()));
        } else {
            System.out.println("SERVER: " + this.username + " declined challenge from " + challengerUsername);
            // Gửi thông báo từ chối cho người đã mời
            challengerHandler.sendMessage(new Message(Message.MessageType.CHALLENGE_DECLINED, this.username));
        }
    }

    private void handleCrosswordChoice(Object[] payload) {
        int chosenCrosswordId = (Integer) payload[0];
        String opponentUsername = (String) payload[1];

        ClientHandler opponentHandler = server.getClientHandler(opponentUsername);

        if (opponentHandler == null) {
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Đối thủ đã thoát."));
            return;
        }

        // Xử lý trường hợp người chơi đóng hộp thoại lựa chọn (hủy trận)
        if (chosenCrosswordId == -1) {
            System.out.println("SERVER: " + this.username + " đã hủy việc chọn bộ câu hỏi.");
            // Thông báo cho cả hai người chơi rằng trận đấu đã bị hủy
            Message cancelMessage = new Message(Message.MessageType.LOGIN_FAILURE, "Trận đấu đã bị hủy do người chọn không đưa ra quyết định.");
            this.sendMessage(cancelMessage);
            opponentHandler.sendMessage(cancelMessage);
            return;
        }

        // Xác định lại vai trò challenger/challenged để truyền vào GameSession một cách nhất quán
        // Giả sử người gửi lựa chọn (this) là người được thách đấu (challenged)
        // và người chờ (opponentHandler) là người thách đấu (challenger)
        // Điều này cần được xem xét lại nếu logic thách đấu phức tạp hơn.
        // Trong kịch bản hiện tại, `this` là người chọn, `opponent` là người chờ.
        GameSession gameSession = new GameSession(opponentHandler, this, dbManager, chosenCrosswordId);
        this.setGameSession(gameSession);
        opponentHandler.setGameSession(gameSession);
        new Thread(gameSession).start();
    }

    private void handleAnswerSubmission(Object[] payload) {
        if (currentGameSession != null) {
            Integer questionId = (Integer) payload[0];
            String answer = (String) payload[1];
            // Chuyển tiếp cho GameSession xử lý
            currentGameSession.processAnswer(this, questionId, answer);
        }
    }

    private void handleGameChatMessage(String message) {
        if (currentGameSession != null) {
            currentGameSession.broadcastChatMessage(this, message);
        }
    }

    private void handleLeaderboardRequest() {
        var leaderboardData = dbManager.getLeaderboard();
        sendMessage(new Message(Message.MessageType.LEADERBOARD_UPDATE, leaderboardData));
    }

    private void handleMatchHistoryRequest() {
        var historyData = dbManager.getMatchHistory(this.username);
        sendMessage(new Message(Message.MessageType.MATCH_HISTORY_UPDATE, historyData));
    }

    /**
     * Xử lý yêu cầu đăng nhập.
     * @param credentials Mảng chứa {username, password}
     */
    private void handleLoginRequest(String[] credentials) {
        String username = credentials[0];
        String password = credentials[1];

        // CẢI TIẾN: Kiểm tra xem người dùng đã đăng nhập ở nơi khác chưa
        if (server.isUserOnline(username)) {
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Tài khoản này đã được đăng nhập ở nơi khác."));
            return;
        }

        if (dbManager.validateUser(username, password)) {
            // CẢI TIẾN: Thêm người dùng vào danh sách online của server TRƯỚC
            this.username = username;
            server.addUser(username, this);

            // Chỉ gửi tin nhắn thành công SAU KHI đã hoàn tất các thao tác trên server.
            // Điều này đảm bảo client sẽ không yêu cầu danh sách người dùng quá sớm.
            sendMessage(new Message(Message.MessageType.LOGIN_SUCCESS, username));
        } else {
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Tên đăng nhập hoặc mật khẩu không đúng."));
        }
    }

    private void handleRegisterRequest(String[] credentials) {
        String username = credentials[0];
        String password = credentials[1];

        int registrationResult = dbManager.registerUser(username, password);

        switch (registrationResult) {
            case 0: // Thành công
                sendMessage(new Message(Message.MessageType.REGISTER_SUCCESS, null));
                break;
            case 1: // Tên người dùng đã tồn tại
                sendMessage(new Message(Message.MessageType.REGISTER_FAILURE, "Tên đăng nhập đã tồn tại."));
                break;
            default: // Lỗi DB
                sendMessage(new Message(Message.MessageType.REGISTER_FAILURE, "Lỗi máy chủ, không thể đăng ký."));
                break;
        }
    }

    /**
     * Gửi một tin nhắn đến client này.
     * @param message Tin nhắn cần gửi
     */
    public void sendMessage(Message message) {
        try {
            oos.writeObject(message);
        } catch (IOException e) {
            System.err.println("Error sending message to " + username + ": " + e.getMessage());
        }
    }

    private void closeConnections() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setGameSession(GameSession session) {
        this.currentGameSession = session;
    }
}