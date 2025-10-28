// src/server/game/GameSession.java
package server.game;

import server.core.ClientHandler;
import server.db.DatabaseManager;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import shared.Message;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final DatabaseManager dbManager;
    private Crossword crossword;
    private boolean gameOver = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private ClientHandler currentPlayerTurn; // Có thể dùng để quản lý lượt chơi
    private Set<Integer> answeredQuestionIds = new HashSet<>(); // Theo dõi các câu hỏi đã được trả lời
    private boolean keywordPhaseActive = false; // Trạng thái của giai đoạn từ khóa
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    
    public GameSession(ClientHandler player1, ClientHandler player2, DatabaseManager dbManager, int crosswordId) {
        this.player1 = player1;
        this.player2 = player2;
        this.dbManager = dbManager;
        // Lấy bộ ô chữ cụ thể từ CSDL dựa trên lựa chọn của người chơi
        this.crossword = this.dbManager.getCrosswordById(crosswordId);
        if (this.crossword == null) {
            // Xử lý lỗi nếu không tìm thấy ô chữ
            System.err.println("GameSession: Không tìm thấy ô chữ với ID " + crosswordId);
            player1.sendMessage(new Message(Message.MessageType.GAME_OVER, "Lỗi: Không tìm thấy ô chữ. Trận đấu bị hủy."));
            player2.sendMessage(new Message(Message.MessageType.GAME_OVER, "Lỗi: Không tìm thấy ô chữ. Trận đấu bị hủy."));
            gameOver = true;
        }
        // Quyết định lượt chơi đầu tiên (ví dụ: ngẫu nhiên)
        this.currentPlayerTurn = (Math.random() < 0.5) ? player1 : player2;
    }

    @Override
    public void run() {
        if (gameOver) return; // Không chạy nếu có lỗi khởi tạo

        // Gửi thông tin bắt đầu game (chứa ô chữ) cho cả 2 người chơi
        Message gameStartMessage = new Message(Message.MessageType.GAME_START, this.crossword);
        player1.sendMessage(gameStartMessage);
        player2.sendMessage(gameStartMessage);

        // Gửi thông báo lượt chơi đầu tiên
        broadcastChatMessage(null, "Trận đấu bắt đầu! Lượt của " + currentPlayerTurn.getUsername());

        // Có thể thêm logic timeout cho game tại đây
        // scheduler.schedule(() -> endGame("Hết giờ!"), 30, TimeUnit.MINUTES);
    }

    // Xử lý khi một người chơi gửi đáp án
    public synchronized void processAnswer(ClientHandler player, int questionId, String answer) {
        if (gameOver || keywordPhaseActive) {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Không thể trả lời câu hỏi lúc này."));
            return;
        }

        // KIỂM TRA LƯỢT CHƠI
        if (player != currentPlayerTurn) {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Chưa đến lượt của bạn!"));
            return;
        }

        // Kiểm tra xem câu hỏi đã được trả lời chưa
        if (answeredQuestionIds.contains(questionId)) {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Câu hỏi này đã được trả lời."));
            return;
        }

        Question question = crossword.getQuestionById(questionId);

        if (question == null) {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Câu hỏi không tồn tại."));
            return;
        }

        if (question.getAnswer().equalsIgnoreCase(answer)) {
            question.setAnswered(true);
            answeredQuestionIds.add(questionId);

            // Cập nhật điểm
            if (player == player1) {
                player1Score += 10; // Ví dụ: 10 điểm cho mỗi câu đúng
            } else {
                player2Score += 10;
            }

            // Thông báo cho cả hai người chơi
            player1.sendMessage(new Message(Message.MessageType.ANSWER_CORRECT, new Object[]{questionId, question.getAnswer(), player.getUsername()}));
            player2.sendMessage(new Message(Message.MessageType.ANSWER_CORRECT, new Object[]{questionId, question.getAnswer(), player.getUsername()}));
            broadcastChatMessage(null, player.getUsername() + " đã trả lời đúng câu " + questionId + "!");

            // Kiểm tra xem tất cả các câu hỏi đã được trả lời chưa
            if (crossword.areAllQuestionsAnswered()) {
                startKeywordPhase();
            } else {
                // Chuyển lượt chơi
                switchTurn(player);
            }

        } else {
            player.sendMessage(new Message(Message.MessageType.ANSWER_WRONG, "Sai rồi! Vui lòng thử lại."));
            // Có thể trừ điểm hoặc không
        }
    }

    public void processKeywordAnswer(ClientHandler sender, String keyword) {
        if (gameOver || !keywordPhaseActive) {
            sender.sendMessage(new Message(Message.MessageType.KEYWORD_WRONG, "Không thể gửi từ khóa lúc này."));
            return;
        }

        if (crossword.getKeyWordAnswer().equalsIgnoreCase(keyword)) {
            // Từ khóa đúng
            if (sender == player1) {
                player1Score += 50; // Ví dụ: 50 điểm thưởng cho từ khóa
            } else {
                player2Score += 50;
            }
            player1.sendMessage(new Message(Message.MessageType.KEYWORD_CORRECT, sender.getUsername()));
            player2.sendMessage(new Message(Message.MessageType.KEYWORD_CORRECT, sender.getUsername()));
            endGame(sender.getUsername() + " đã giải đúng từ khóa! Trận đấu kết thúc.");
        } else {
            // Từ khóa sai
            sender.sendMessage(new Message(Message.MessageType.KEYWORD_WRONG, "Từ khóa sai rồi! Vui lòng thử lại."));
        }
    }

    private void startKeywordPhase() {
        keywordPhaseActive = true;
        // Gửi gợi ý từ khóa đến cả hai client
        player1.sendMessage(new Message(Message.MessageType.KEYWORD_PHASE_START, crossword.getKeyWordClue()));
        player2.sendMessage(new Message(Message.MessageType.KEYWORD_PHASE_START, crossword.getKeyWordClue()));
        broadcastChatMessage(null, "Tất cả các câu hỏi đã được giải! Hãy tìm từ khóa.");
        // Không chuyển lượt chơi trong giai đoạn từ khóa, ai trả lời đúng trước thì thắng
    }

    private void switchTurn(ClientHandler currentTurnPlayer) {
        if (currentTurnPlayer == player1) {
            currentPlayerTurn = player2;
        } else {
            currentPlayerTurn = player1;
        }
        broadcastChatMessage(null, "Lượt của " + currentPlayerTurn.getUsername());
        // Có thể gửi tin nhắn riêng để client biết lượt của mình
    }

    private synchronized void endGame(String reason) {
        if (gameOver) {
            return; // Nếu game đã kết thúc, không làm gì cả
        }
        gameOver = true;
        scheduler.shutdownNow(); // Dừng scheduler nếu có

        String winnerUsername = null;
        if (player1Score > player2Score) {
            winnerUsername = player1.getUsername();
        } else if (player2Score > player1Score) {
            winnerUsername = player2.getUsername();
        } else {
            winnerUsername = null; // Hòa
        }

        // Cập nhật điểm và lịch sử đấu trong DB
        dbManager.updateUserStats(player1.getUsername(), player1Score, player1.getUsername().equals(winnerUsername));
        dbManager.updateUserStats(player2.getUsername(), player2Score, player2.getUsername().equals(winnerUsername));
        dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, winnerUsername);

        Message gameOverMessage = new Message(Message.MessageType.GAME_OVER, reason + "\nĐiểm số: " + player1.getUsername() + " " + player1Score + " - " + player2.getUsername() + " " + player2Score);
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
        String formattedMessage;
        if (sender != null) {
            formattedMessage = sender.getUsername() + ": " + message;
        } else {
            formattedMessage = "[Hệ thống]: " + message;
        }
        player1.sendMessage(new Message(Message.MessageType.GAME_CHAT_MESSAGE, formattedMessage));
        player2.sendMessage(new Message(Message.MessageType.GAME_CHAT_MESSAGE, formattedMessage));
    }

    /**
     * Xử lý khi một người chơi ngắt kết nối đột ngột.
     * @param disconnectedPlayer Người chơi đã ngắt kết nối.
     */
    public synchronized void handlePlayerDisconnect(ClientHandler disconnectedPlayer) {
        if (gameOver) {
            return; // Game đã kết thúc rồi, không cần xử lý nữa
        }
        gameOver = true;

        System.out.println("SERVER: " + disconnectedPlayer.getUsername() + " disconnected from the game.");

        // Xác định người chơi còn lại
        ClientHandler remainingPlayer = (disconnectedPlayer == player1) ? player2 : player1;

        // Thông báo cho người chơi còn lại
        remainingPlayer.sendMessage(new Message(Message.MessageType.GAME_OVER, "Đối thủ " + disconnectedPlayer.getUsername() + " đã ngắt kết nối. Bạn thắng!"));
        
        // Cập nhật điểm và lịch sử đấu (người còn lại thắng)
        if (remainingPlayer == player1) {
            player1Score += 100; // Thưởng điểm cho người thắng do đối thủ thoát
            dbManager.updateUserStats(player1.getUsername(), player1Score, true);
            dbManager.updateUserStats(player2.getUsername(), player2Score, false); // Người thoát không được điểm
            dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, player1.getUsername());
        } else {
            player2Score += 100;
            dbManager.updateUserStats(player2.getUsername(), player2Score, true);
            dbManager.updateUserStats(player1.getUsername(), player1Score, false);
            dbManager.recordMatch(player1.getUsername(), player2.getUsername(), player1Score, player2Score, player2.getUsername());
        }

        scheduler.shutdownNow();

        // Dọn dẹp session
        player1.setGameSession(null);
        player2.setGameSession(null);
    }
}