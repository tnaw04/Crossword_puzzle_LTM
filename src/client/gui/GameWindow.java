// src/client/gui/GameWindow.java
package client.gui;

import client.audio.SoundManager;
import client.network.Client;
import server.game.Crossword;
import server.game.Question;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameWindow extends JFrame {
    private final Client client;
    private final Crossword crossword;

    private JPanel gridPanel; // Panel chứa các ô chữ
    private JTextArea cluesArea; // Khu vực hiển thị gợi ý
    private JLabel notificationLabel; // Hiển thị thông báo
    private final Map<String, JTextField> cellMap = new HashMap<>(); // Lưu các ô để dễ truy cập
    private JTextField answerField;
    private JTextField questionIdField;
    private JButton submitButton;
    private JTextArea chatArea;
    private JTextField chatInputField;
    private JLabel keywordClueLabel; // Nhãn hiển thị gợi ý từ khóa
    private JTextField keywordAnswerField; // Trường nhập từ khóa
    private JButton submitKeywordButton; // Nút gửi từ khóa
    private boolean keywordPhaseActive = false; // Trạng thái giai đoạn từ khóa
    private JPanel cardPanel; // Biến thành viên để truy cập trực tiếp

    public GameWindow(Client client, Crossword crossword) {
        this.client = client;
        this.crossword = crossword;

        setTitle("Trận Đấu Ô Chữ");
        setSize(1150, 700); // Tăng chiều rộng để chứa khung chat
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Sẽ xử lý đóng cửa sổ khi game over
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 248, 255)); // Màu Alice Blue nhẹ nhàng

        // -- Phần trung tâm: Lưới ô chữ --
        gridPanel = new JPanel();
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gridPanel.setBackground(new Color(220, 235, 250));
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        // -- Phần bên phải: Gợi ý --
        JPanel cluesPanel = new JPanel(new BorderLayout());
        cluesPanel.setBackground(new Color(245, 245, 245)); // Màu trắng xám
        cluesPanel.setBorder(BorderFactory.createTitledBorder("Gợi ý"));
        cluesPanel.setPreferredSize(new Dimension(300, 0));

        cluesArea = new JTextArea();
        cluesArea.setEditable(false);
        cluesArea.setLineWrap(true);
        cluesArea.setWrapStyleWord(true);
        JScrollPane cluesScrollPane = new JScrollPane(cluesArea);
        cluesScrollPane.setPreferredSize(new Dimension(300, 0));
        cluesScrollPane.setBorder(BorderFactory.createTitledBorder("Gợi ý"));
        add(cluesPanel, BorderLayout.EAST);
        cluesPanel.add(cluesScrollPane, BorderLayout.CENTER);

        // -- Phần bên trái: Chat --
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.setPreferredSize(new Dimension(250, 0));
        chatPanel.setBackground(new Color(245, 245, 245));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 5));
        chatInputField = new JTextField();
        chatInputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton sendChatButton = new JButton("Gửi");
        styleButton(sendChatButton); // Áp dụng style cho nút gửi chat
        chatInputPanel.add(chatInputField, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.WEST);

        // -- Phần dưới: Nhập liệu và thông báo --
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        bottomPanel.setBackground(new Color(240, 248, 255));

        notificationLabel = new JLabel("Chào mừng đến với trận đấu!", SwingConstants.CENTER);
        notificationLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        notificationLabel.setForeground(new Color(0, 102, 204));
        bottomPanel.add(notificationLabel, BorderLayout.NORTH);

        // Panel cho Từ khóa (ban đầu ẩn)
        JPanel keywordPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        keywordPanel.setBackground(new Color(240, 248, 255));
        keywordClueLabel = new JLabel("Từ khóa: ", SwingConstants.CENTER);
        keywordClueLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        keywordClueLabel.setForeground(new Color(100, 100, 100));
        keywordPanel.add(keywordClueLabel);

        keywordAnswerField = new JTextField(20);
        keywordAnswerField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        keywordAnswerField.setEnabled(false); // Ban đầu vô hiệu hóa
        keywordPanel.add(keywordAnswerField);

        submitKeywordButton = new JButton("Gửi Từ khóa");
        styleButton(submitKeywordButton);
        submitKeywordButton.setEnabled(false); // Ban đầu vô hiệu hóa
        keywordPanel.add(submitKeywordButton);
        
        // Panel nhập liệu
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(new Color(240, 248, 255));
        inputPanel.add(new JLabel("Câu số:"));
        questionIdField = new JTextField(3);
        inputPanel.add(questionIdField);

        inputPanel.add(new JLabel("Đáp án:"));
        answerField = new JTextField(20);
        answerField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(answerField);

        submitButton = new JButton("Gửi");
        styleButton(submitButton); // Áp dụng style cho nút gửi đáp án
        inputPanel.add(submitButton);

        // Sử dụng CardLayout để chuyển đổi giữa inputPanel và keywordPanel
        cardPanel = new JPanel(new CardLayout()); // Gán cho biến thành viên
        cardPanel.add(inputPanel, "regularInput");
        cardPanel.add(keywordPanel, "keywordInput");
        bottomPanel.add(cardPanel, BorderLayout.CENTER);

        // Sự kiện cho nút gửi
        submitButton.addActionListener(e -> submitAnswer());
        // Cho phép nhấn Enter để gửi
        getRootPane().setDefaultButton(submitButton);

        // Sự kiện cho chat
        sendChatButton.addActionListener(e -> sendChatMessage());
        chatInputField.addActionListener(e -> sendChatMessage());

        // Sự kiện cho nút gửi từ khóa
        submitKeywordButton.addActionListener(e -> submitKeywordAnswer());

        add(bottomPanel, BorderLayout.SOUTH);

        // Vẽ ô chữ và hiển thị gợi ý
        renderCrossword();
        displayClues();
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(70, 130, 180)); // Steel Blue
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private void renderCrossword() {
        int rows = crossword.getHeight();
        int cols = crossword.getWidth();
        gridPanel.setLayout(new GridLayout(rows, cols));
        gridPanel.removeAll();

        // Tạo một mảng tạm để đánh dấu ô nào là ô chữ
        // Đồng thời đánh dấu các ô thuộc từ khóa
        boolean[][] isLetterCell = new boolean[rows][cols];
        for (Question q : crossword.getQuestions()) {
            int r = q.getRow();
            int c = q.getCol();
            for (int i = 0; i < q.getAnswer().length(); i++) {
                // Kiểm tra xem ô chữ có nằm trong giới hạn của lưới không
                if (q.getDirection() == 'H') { // Ngang
                    if (r >= 0 && r < rows && c + i >= 0 && c + i < cols) {
                        isLetterCell[r][c + i] = true;
                    } else {
                        System.err.println("CẢNH BÁO: Câu hỏi ngang " + q.getId() + " vượt ra ngoài giới hạn lưới tại (" + r + "," + (c + i) + ")");
                    }
                } else { // Dọc
                    if (r + i >= 0 && r + i < rows && c >= 0 && c < cols) {
                        isLetterCell[r + i][c] = true;
                    } else {
                        System.err.println("CẢNH BÁO: Câu hỏi dọc " + q.getId() + " vượt ra ngoài giới hạn lưới tại (" + (r + i) + "," + c + ")");
                    }
                }
            }
        }

        // Vẽ lưới
        Border border = BorderFactory.createLineBorder(new Color(170, 190, 210));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isLetterCell[r][c]) {
                    JTextField cell = new JTextField();
                    cell.setHorizontalAlignment(JTextField.CENTER);
                    cell.setFont(new Font("Segoe UI", Font.BOLD, 20));
                    cell.setBackground(Color.WHITE);
                    cell.setForeground(new Color(25, 25, 112)); // Midnight Blue (màu chữ mặc định)
                    cell.setBorder(border);
                    gridPanel.add(cell);
                    cellMap.put(r + "-" + c, cell); // Lưu lại để cập nhật sau

                    // SỬA LỖI: Tìm xem ô (r, c) có thuộc câu hỏi nào là từ khóa không
                    final int finalR = r;
                    final int finalC = c;
                    boolean isKeywordCell = crossword.getQuestions().stream().anyMatch(question -> {
                        return question.getKeyWordIndex() != null && question.cellIsInQuestion(finalR, finalC);
                    });
                    if (isKeywordCell) {
                        cell.setBackground(new Color(255, 255, 204)); // Màu vàng nhạt
                    }
                } else {
                    JPanel blackCell = new JPanel();
                    blackCell.setBackground(new Color(105, 105, 105)); // Màu Dim Gray, hài hòa hơn màu đen
                    blackCell.setBorder(border);
                    gridPanel.add(blackCell);
                }
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void displayClues() {
        cluesArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        StringBuilder sb = new StringBuilder();
        sb.append("HÀNG NGANG:\n");
        crossword.getQuestions().stream()
            .filter(q -> q.getDirection() == 'H')
            .forEach(q -> sb.append(String.format("%d. %s\n", q.getId(), q.getQuestionText())));

        sb.append("\nHÀNG DỌC:\n");
        crossword.getQuestions().stream()
            .filter(q -> q.getDirection() == 'V')
            .forEach(q -> sb.append(String.format("%d. %s\n", q.getId(), q.getQuestionText())));

        cluesArea.setText(sb.toString());
    }

    private CardLayout getCardLayout() {
        return (CardLayout) cardPanel.getLayout();
    }

    private void submitAnswer() {
        try {
            int questionId = Integer.parseInt(questionIdField.getText().trim());
            String answer = answerField.getText().trim().toUpperCase();

            if (answer.isEmpty()) {
                showNotification("Vui lòng nhập câu trả lời.");
                return;
            }

            if (!keywordPhaseActive) {
                // Gửi tin nhắn đến server
                Object[] payload = {questionId, answer};
                client.sendMessage(new shared.Message(shared.Message.MessageType.SUBMIT_ANSWER, payload));

                // Xóa trường nhập liệu sau khi gửi
                questionIdField.setText("");
                answerField.setText("");
                answerField.requestFocus();
            } else {
                showNotification("Bạn đang ở giai đoạn từ khóa. Vui lòng gửi từ khóa.");
            }

        } catch (NumberFormatException e) {
            showNotification("Mã câu hỏi phải là một con số.");
        }
    }

    private void submitKeywordAnswer() {
        String keyword = keywordAnswerField.getText().trim().toUpperCase();
        if (!keyword.isEmpty()) {
            client.sendMessage(new shared.Message(shared.Message.MessageType.SUBMIT_KEYWORD_ANSWER, keyword));
            keywordAnswerField.setText(""); // Xóa trường nhập sau khi gửi
            keywordAnswerField.requestFocus(); // Focus lại vào trường nhập từ khóa
        } else {
            showNotification("Vui lòng nhập từ khóa.");
        }
    }

    private void sendChatMessage() {
        String messageText = chatInputField.getText().trim();
        if (!messageText.isEmpty()) {
            client.sendMessage(new shared.Message(shared.Message.MessageType.GAME_CHAT_MESSAGE, messageText));
            chatInputField.setText("");
        }
    }

    /**
     * Thêm một tin nhắn vào khu vực chat.
     * @param formattedMessage Tin nhắn đã được định dạng (ví dụ: "Player1: Hello")
     */
    public void appendChatMessage(String formattedMessage) {
        chatArea.append(formattedMessage + "\n");
        // Tự động cuộn xuống cuối
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Hiển thị thông báo trên giao diện.
     * @param message Nội dung thông báo
     */
    public void showNotification(String message) {
        // Nếu là thông báo lỗi, phát âm thanh "sai"
        if (message.contains("Sai") || message.contains("lượt") || message.contains("tồn tại")) {
            SoundManager.playSound("wrong.wav");
        }
        notificationLabel.setText(message);
    }

    /**
     * Cập nhật lưới ô chữ khi có câu trả lời đúng.
     * @param questionId ID của câu hỏi được trả lời đúng
     * @param answer Đáp án đúng
     */
    public void updateGridWithAnswer(int questionId, String answer) {
        // Tìm câu hỏi tương ứng
        crossword.getQuestions().stream()
            .filter(q -> q.getId() == questionId)
            .findFirst()
            .ifPresent(question -> {
                int r = question.getRow();
                int c = question.getCol();
                for (int i = 0; i < answer.length(); i++) {
                    JTextField cell = cellMap.get((question.getDirection() == 'H' ? r : r + i) + "-" + (question.getDirection() == 'H' ? c + i : c));
                    if (cell != null) {
                        cell.setText(String.valueOf(answer.charAt(i)));
                        cell.setEditable(false); // Không cho sửa ô đã đúng
                        // Giữ màu vàng nhạt nếu là ô từ khóa, nếu không thì màu xanh lá cây nhạt
                        if (question.getKeyWordIndex() == null) {
                            cell.setBackground(new Color(200, 255, 200)); // Màu xanh lá cây nhạt cho ô đúng
                        }
                    }
                }
            });
    }

    /**
     * Bắt đầu giai đoạn từ khóa: ẩn input câu hỏi, hiện input từ khóa.
     * @param keyWordClue Gợi ý từ khóa.
     */
    public void startKeywordPhase(String keyWordClue) {
        keywordPhaseActive = true;
        showNotification("Tất cả các câu hỏi đã được giải! Hãy tìm từ khóa.");

        // Ẩn input câu hỏi thường
        getCardLayout().show(cardPanel, "keywordInput");

        // Hiển thị gợi ý từ khóa và kích hoạt input từ khóa
        keywordClueLabel.setText("Từ khóa: " + keyWordClue);
        keywordAnswerField.setEnabled(true);
        submitKeywordButton.setEnabled(true);
        keywordAnswerField.requestFocus();
        getRootPane().setDefaultButton(submitKeywordButton); // Thay đổi nút Enter mặc định
    }

    /**
     * Vô hiệu hóa các thành phần nhập liệu khi game kết thúc.
     */
    public void disableInputs() {
        questionIdField.setEnabled(false);
        answerField.setEnabled(false);
        submitButton.setEnabled(false);
        keywordAnswerField.setEnabled(false);
        submitKeywordButton.setEnabled(false);
        chatInputField.setEnabled(false);
    }
}