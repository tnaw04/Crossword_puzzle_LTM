// src/client/gui/LoginWindow.java
package client.gui;

import client.network.Client;
import shared.Message;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    private final JTextField userText;
    private final JPasswordField passText;
    private final Client client;

    public LoginWindow(Client client) {
        this.client = client;

        setTitle("Game Ô Chữ - Đăng nhập");
        setSize(400, 350); // Điều chỉnh kích thước
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Sử dụng BorderLayout cho layout chính
        setLayout(new BorderLayout());

        // --- Tiêu đề ---
        JLabel titleLabel = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- Form đăng nhập ---
        // Sử dụng GridBagLayout để căn chỉnh linh hoạt
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Khoảng cách giữa các thành phần
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tên đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Tên đăng nhập:"), gbc);

        gbc.gridx = 1;
        userText = new JTextField();
        formPanel.add(userText, gbc);

        // Mật khẩu
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Mật khẩu:"), gbc);

        gbc.gridx = 1;
        passText = new JPasswordField();
        formPanel.add(passText, gbc);

        // Nút đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Nút chiếm 2 cột
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE; // Không kéo dài nút
        JButton loginButton = new JButton("Đăng nhập");
        JButton registerButton = new JButton("Đăng ký");

        // Panel chứa 2 nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        formPanel.add(buttonPanel, gbc);

        // Thêm ActionListener cho button
        loginButton.addActionListener(e -> attemptLogin());
        registerButton.addActionListener(e -> openRegisterWindow());

        // Cho phép nhấn Enter để đăng nhập
        getRootPane().setDefaultButton(loginButton);

        // Thêm form vào trung tâm cửa sổ
        add(formPanel, BorderLayout.CENTER);
    }

    private void attemptLogin() {
        String username = userText.getText();
        String password = new String(passText.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Tạo payload và message
        String[] payload = {username, password};
        Message loginMessage = new Message(Message.MessageType.LOGIN_REQUEST, payload);
        
        // Gửi tin nhắn đăng nhập qua client
        client.sendMessage(loginMessage);
    }

    private void openRegisterWindow() {
        RegisterWindow registerWindow = new RegisterWindow(this, client);
        registerWindow.setVisible(true);
    }
}