// src/client/network/Client.java
package client.network;

import client.gui.MatchHistoryWindow;
import client.gui.GameWindow;
import client.gui.LoginWindow;
import client.gui.LobbyWindow;
import shared.Message;
import client.audio.SoundManager;
import shared.CrosswordInfo;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.awt.Window;

public class Client {
    private final String hostname;
    private final int port;
    private Socket socket;
    private ObjectOutputStream oos;
    private String username;

    // Tham chiếu đến các cửa sổ GUI để cập nhật
    private LoginWindow loginWindow;
    private LobbyWindow lobbyWindow;
    private GameWindow gameWindow;
    private MatchHistoryWindow matchHistoryWindow;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }
    
    // Setter để MainClient có thể gán các cửa sổ
    public void setLoginWindow(LoginWindow loginWindow) { this.loginWindow = loginWindow; }
    public void setMatchHistoryWindow(MatchHistoryWindow window) { this.matchHistoryWindow = window; }

    public void connect() {
        try {
            socket = new Socket(hostname, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            // Khởi tạo và bắt đầu luồng lắng nghe server
            ServerListener listener = new ServerListener(socket, this);
            new Thread(listener).start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể kết nối tới server.", "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Gửi đối tượng tin nhắn đến server
    public void sendMessage(Message message) {
        try {
            oos.writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----- Các phương thức được gọi bởi ServerListener để cập nhật UI -----
    
    public void onLoginSuccess(String username) {
        this.username = username;
        SwingUtilities.invokeLater(() -> {
            loginWindow.dispose(); // Đóng cửa sổ đăng nhập
            lobbyWindow = new LobbyWindow(this); // Tạo sảnh chờ
            lobbyWindow.setVisible(true);

            // Sau khi sảnh chờ đã sẵn sàng, yêu cầu server gửi danh sách người dùng
            sendMessage(new Message(Message.MessageType.REQUEST_USER_LIST, null));
        });
    }

    public void onLoginFailure(String reason) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(loginWindow, reason, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
        });
    }

    public void onUserListUpdate(List<String> userList) {
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                // Tạo một bản sao của danh sách để xử lý an toàn trên luồng giao diện
                List<String> userListCopy = new java.util.ArrayList<>(userList);
                // Xóa username của chính mình khỏi danh sách trước khi hiển thị
                userListCopy.remove(this.username);
                lobbyWindow.updateUserList(userListCopy);
            }
        });
    }

    public void onChallengeReceived(String challengerUsername) {
        // Phát âm thanh thông báo có lời mời
        SoundManager.playSound("invite.wav");

        SwingUtilities.invokeLater(() -> {
            int response = JOptionPane.showConfirmDialog(
                lobbyWindow,
                challengerUsername + " đang mời bạn vào trận. Bạn có muốn chấp nhận?",
                "Lời mời chơi",
                JOptionPane.YES_NO_OPTION
            );

            String responseStr = (response == JOptionPane.YES_OPTION) ? "ACCEPT" : "DECLINE";
            
            // Gửi phản hồi lại cho server
            // Payload là một mảng String chứa {tên người mời, câu trả lời}
            sendMessage(new Message(Message.MessageType.CHALLENGE_RESPONSE, new String[]{challengerUsername, responseStr}));
        });
    }

    public void onChallengeDeclined(String declinerUsername) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                lobbyWindow,
                declinerUsername + " đã từ chối lời mời của bạn.",
                "Lời mời bị từ chối",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    public void onGameStart(server.game.Crossword crossword) {
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                lobbyWindow.dispose(); // Đóng cửa sổ sảnh chờ
            }
            gameWindow = new GameWindow(this, crossword); // Tạo cửa sổ game mới
            gameWindow.setVisible(true);
        });
    }

    public void onAnswerCorrect(Object[] payload) {
        // Phát âm thanh trả lời đúng
        SoundManager.playSound("correct.wav");

        SwingUtilities.invokeLater(() -> {
            if (gameWindow != null) {
                // Di chuyển việc đọc payload vào trong invokeLater để đảm bảo an toàn luồng
                Integer questionId = (Integer) payload[0];
                String answer = (String) payload[1];
                String winnerUsername = (String) payload[2];
                gameWindow.showNotification("'" + winnerUsername + "' đã trả lời đúng câu " + questionId + "!");
                gameWindow.updateGridWithAnswer(questionId, answer);
            }
        });
    }

    public void onAnswerWrong(String reason) {
        SwingUtilities.invokeLater(() -> {
            if (gameWindow != null) {
                gameWindow.showNotification(reason);
            }
        });
    }

    public void onGameOver(String resultMessage) {
        SwingUtilities.invokeLater(() -> {
            if (gameWindow != null) {
                // Vô hiệu hóa input và hiển thị thông báo kết quả
                gameWindow.disableInputs();
                JOptionPane.showMessageDialog(
                    gameWindow,
                    resultMessage,
                    "Trò chơi kết thúc",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Đóng cửa sổ game và mở lại sảnh chờ
                gameWindow.dispose();
                gameWindow = null;

                lobbyWindow = new LobbyWindow(this);
                lobbyWindow.setVisible(true);
                sendMessage(new Message(Message.MessageType.REQUEST_USER_LIST, null));
            }
        });
    }

    public void onGameChatMessage(String formattedMessage) {
        SwingUtilities.invokeLater(() -> {
            if (gameWindow != null) {
                gameWindow.appendChatMessage(formattedMessage);
            }
        });
    }

    public void onLeaderboardUpdate(List<shared.PlayerStats> stats) {
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                lobbyWindow.updateLeaderboard(stats);
            }
        });
    }

    public void onRegisterSuccess() {
        SwingUtilities.invokeLater(() -> {
            // Tìm cửa sổ Register đang mở và đóng nó
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()) {
                    window.dispose();
                }
            }
            JOptionPane.showMessageDialog(loginWindow, "Đăng ký thành công! Vui lòng đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void onRegisterFailure(String reason) {
        // Hiển thị lỗi trên cửa sổ đang active (có thể là RegisterWindow)
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, reason, "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE));
    }

    public void onMatchHistoryUpdate(List<shared.MatchHistoryEntry> history) {
        SwingUtilities.invokeLater(() -> {
            if (matchHistoryWindow != null) {
                matchHistoryWindow.updateHistory(history);
            }
        });
    }

    public void onCrosswordChoiceRequest(Object[] payload) {
        SwingUtilities.invokeLater(() -> {
            List<CrosswordInfo> availableCrosswords = (List<CrosswordInfo>) payload[0];
            String opponentUsername = (String) payload[1];

            // Hiển thị hộp thoại lựa chọn
            CrosswordInfo selected = (CrosswordInfo) JOptionPane.showInputDialog(
                lobbyWindow,
                "Bạn đã thắng tung đồng xu! Hãy chọn một bộ câu hỏi:",
                "Chọn bộ câu hỏi",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableCrosswords.toArray(),
                availableCrosswords.get(0)
            );

            if (selected != null) {
                // Gửi lựa chọn về server
                sendMessage(new Message(Message.MessageType.SUBMIT_CROSSWORD_CHOICE, new Object[]{selected.getId(), opponentUsername}));
                // Hiển thị thông báo chờ
                lobbyWindow.showWaitingDialog("Đã chọn bộ " + selected.getId() + ". Đang bắt đầu trận đấu...");
            } else {
                // Người chơi đóng hộp thoại -> coi như từ chối trận đấu
                // Gửi một lựa chọn không hợp lệ để server xử lý việc hủy trận
                sendMessage(new Message(Message.MessageType.SUBMIT_CROSSWORD_CHOICE, new Object[]{-1, opponentUsername}));
            }
        });
    }

    public void onWaitForCrosswordChoice(String chooserUsername) {
        SwingUtilities.invokeLater(() -> {
            if (lobbyWindow != null) {
                lobbyWindow.showWaitingDialog("Đối thủ " + chooserUsername + " đang chọn bộ câu hỏi. Vui lòng chờ...");
            }
        });
    }
}