// src/client/gui/GameWindow.java
package client.gui;

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

    public GameWindow(Client client, Crossword crossword) {
        this.client = client;
        this.crossword = crossword;

        setTitle("Trận Đấu Ô Chữ");
        setSize(1150, 700); // Tăng chiều rộng để chứa khung chat
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        inputPanel.add(submitButton, BorderLayout.CENTER);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        // Sự kiện cho nút gửi
        submitButton.addActionListener(e -> submitAnswer());
        // Cho phép nhấn Enter để gửi
        getRootPane().setDefaultButton(submitButton);

        // Sự kiện cho chat
        sendChatButton.addActionListener(e -> sendChatMessage());
        chatInputField.addActionListener(e -> sendChatMessage());

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
        boolean[][] isLetterCell = new boolean[rows][cols];
        for (Question q : crossword.getQuestions()) {
            int r = q.getRow();
            int c = q.getCol();
            for (int i = 0; i < q.getAnswer().length(); i++) {
                if (q.getDirection() == 'H') { // Ngang
                    isLetterCell[r][c + i] = true;
                } else { // Dọc
                    isLetterCell[r + i][c] = true;
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
                    cell.setForeground(new Color(25, 25, 112)); // Midnight Blue
                    cell.setBorder(border);
                    gridPanel.add(cell);
                    cellMap.put(r + "-" + c, cell); // Lưu lại để cập nhật sau
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

    private void submitAnswer() {
        try {
            int questionId = Integer.parseInt(questionIdField.getText().trim());
            String answer = answerField.getText().trim().toUpperCase();

            if (answer.isEmpty()) {
                showNotification("Vui lòng nhập câu trả lời.");
                return;
            }

            // Gửi tin nhắn đến server
            Object[] payload = {questionId, answer};
            client.sendMessage(new shared.Message(shared.Message.MessageType.SUBMIT_ANSWER, payload));

            // Xóa trường nhập liệu sau khi gửi
            questionIdField.setText("");
            answerField.setText("");
            answerField.requestFocus();

        } catch (NumberFormatException e) {
            showNotification("Mã câu hỏi phải là một con số.");
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
                        cell.setBackground(new Color(200, 255, 200)); // Màu xanh lá cây nhạt cho ô đúng
                    }
                }
            });
    }

    /**
     * Vô hiệu hóa các thành phần nhập liệu khi game kết thúc.
     */
    public void disableInputs() {
        questionIdField.setEnabled(false);
        answerField.setEnabled(false);
        submitButton.setEnabled(false);
        chatInputField.setEnabled(false);
    }
}