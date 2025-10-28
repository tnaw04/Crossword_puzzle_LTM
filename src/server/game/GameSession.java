// src/server/game/GameSession.java
package server.game;

import server.core.ClientHandler;
import server.db.DatabaseManager;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import shared.Message;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Crossword crossword;
    private final DatabaseManager dbManager;
    
    // Trạng thái game
    private final Set<Integer> answeredQuestionIds = new HashSet<>();
    private int player1Score = 0;
    private int player2Score = 0;
    private boolean gameEnded = false; // Cờ để đảm bảo game chỉ kết thúc một lần

    public GameSession(ClientHandler player1, ClientHandler player2, DatabaseManager dbManager, int crosswordId) {
        this.player1 = player1;
        this.player2 = player2;
        this.dbManager = dbManager;
        // Lấy bộ ô chữ cụ thể từ CSDL dựa trên lựa chọn của người chơi
        this.crossword = this.dbManager.getCrosswordById(crosswordId);
    }

    @Override
    public void run() {
        System.out.println("SERVER: Starting game between " + player1.getUsername() + " and " + player2.getUsername());
        // Gửi thông tin bắt đầu game (chứa ô chữ) cho cả 2 người chơi
        Message gameStartMessage = new Message(Message.MessageType.GAME_START, this.crossword);
        player1.sendMessage(gameStartMessage);
        player2.sendMessage(gameStartMessage);
        // Bắt đầu vòng lặp game
    }

    // Xử lý khi một người chơi gửi đáp án
    public synchronized void processAnswer(ClientHandler player, int questionId, String answer) {
        // Kiểm tra xem câu hỏi đã được trả lời chưa
        if (answeredQuestionIds.contains(questionId)) {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Câu hỏi này đã được trả lời."));
            return;
        }

        // Tìm câu hỏi trong bộ ô chữ
        Optional<Question> questionOpt = crossword.getQuestions().stream()
                .filter(q -> q.getId() == questionId)
                .findFirst();

        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            // So sánh đáp án (không phân biệt hoa thường)
            if (question.getAnswer().equalsIgnoreCase(answer)) {
                // TRẢ LỜI ĐÚNG
                answeredQuestionIds.add(questionId);
                int scoreGained = question.getAnswer().length() * 10; // Ví dụ: 10 điểm mỗi chữ cái

                if (player == player1) {
                    player1Score += scoreGained;
                } else {
                    player2Score += scoreGained;
                }

                // Gửi thông báo cho cả hai người chơi
                Object[] payload = {questionId, question.getAnswer(), player.getUsername()};
                Message correctMessage = new Message(Message.MessageType.ANSWER_CORRECT, payload);
                player1.sendMessage(correctMessage);
                player2.sendMessage(correctMessage);

                // Kiểm tra game kết thúc
                if (answeredQuestionIds.size() == crossword.getQuestions().size()) {
                    endGame();
                }
            } else {
                // TRẢ LỜI SAI
                player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Đáp án không chính xác!"));
            }
        } else {
            // Không tìm thấy câu hỏi
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Mã câu hỏi không hợp lệ."));
        }
    }

    private synchronized void endGame() {
        if (gameEnded) {
            return; // Nếu game đã kết thúc, không làm gì cả
        }
        gameEnded = true;
        String resultMessage;
        if (player1Score > player2Score) {
            resultMessage = String.format("Trò chơi kết thúc! %s thắng với tỉ số %d - %d.", player1.getUsername(), player1Score, player2Score);
            dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, player1.getUsername());
            dbManager.updateUserStats(player1.getUsername(), player1Score, true); // player1 thắng
            dbManager.updateUserStats(player2.getUsername(), player2Score, false); // player2 thua
        } else if (player2Score > player1Score) {
            resultMessage = String.format("Trò chơi kết thúc! %s thắng với tỉ số %d - %d.", player2.getUsername(), player2Score, player1Score);
            dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, player2.getUsername());
            dbManager.updateUserStats(player2.getUsername(), player2Score, true); // player2 thắng
            dbManager.updateUserStats(player1.getUsername(), player1Score, false); // player1 thua
        } else {
            resultMessage = String.format("Trò chơi kết thúc! Hai bạn hòa nhau với tỉ số %d - %d.", player1Score, player2Score);
            dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, null); // Hòa
        }

        Message gameOverMessage = new Message(Message.MessageType.GAME_OVER, resultMessage);
        player1.sendMessage(gameOverMessage);
        player2.sendMessage(gameOverMessage);

        // Dọn dẹp session
        player1.setGameSession(null);
        player2.setGameSession(null);
    }

    /**
     * Gửi tin nhắn chat đến cả hai người chơi trong phòng.
     * @param sender Người gửi tin nhắn
     * @param message Nội dung tin nhắn
     */
    public void broadcastChatMessage(ClientHandler sender, String message) {
        String formattedMessage = sender.getUsername() + ": " + message;
        player1.sendMessage(new Message(Message.MessageType.GAME_CHAT_MESSAGE, formattedMessage));
        player2.sendMessage(new Message(Message.MessageType.GAME_CHAT_MESSAGE, formattedMessage));
    }

    /**
     * Xử lý khi một người chơi ngắt kết nối đột ngột.
     * @param disconnectedPlayer Người chơi đã ngắt kết nối.
     */
    public synchronized void handlePlayerDisconnect(ClientHandler disconnectedPlayer) {
        if (gameEnded) {
            return; // Game đã kết thúc rồi, không cần xử lý nữa
        }
        gameEnded = true;

        System.out.println("SERVER: " + disconnectedPlayer.getUsername() + " disconnected from the game.");

        // Xác định người chơi còn lại
        ClientHandler remainingPlayer = (disconnectedPlayer == player1) ? player2 : player1;

        // Ghi nhận kết quả vào CSDL: người còn lại thắng, người ngắt kết nối thua
        // Điểm số được giữ nguyên tại thời điểm ngắt kết nối
        int p1FinalScore = (player1 == disconnectedPlayer) ? player1Score : player1Score;
        int p2FinalScore = (player2 == disconnectedPlayer) ? player2Score : player2Score;

        dbManager.recordMatch(player1.getUsername(), player2.getUsername(), p1FinalScore, p2FinalScore, remainingPlayer.getUsername());
        dbManager.updateUserStats(remainingPlayer.getUsername(), (remainingPlayer == player1 ? p1FinalScore : p2FinalScore), true); // Thắng
        dbManager.updateUserStats(disconnectedPlayer.getUsername(), (disconnectedPlayer == player1 ? p1FinalScore : p2FinalScore), false); // Thua

        // Gửi thông báo cho người chơi còn lại
        String resultMessage = String.format("Đối thủ %s đã thoát. Bạn đã thắng!", disconnectedPlayer.getUsername());
        remainingPlayer.sendMessage(new Message(Message.MessageType.GAME_OVER, resultMessage));

        // Dọn dẹp session
        player1.setGameSession(null);
        player2.setGameSession(null);
    }
}