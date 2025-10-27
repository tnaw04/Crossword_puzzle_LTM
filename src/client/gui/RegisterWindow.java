// src/client/gui/RegisterWindow.java
package client.gui;

import client.network.Client;
import shared.Message;

import javax.swing.*;
import java.awt.*;

public class RegisterWindow extends JDialog {
    private final JTextField userText;
    private final JPasswordField passText;
    private final JPasswordField confirmPassText;
    private final Client client;

    public RegisterWindow(Frame owner, Client client) {
        super(owner, "Đăng ký tài khoản", true); // true để nó là modal dialog
        this.client = client;

        setSize(400, 350);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // --- Tiêu đề ---
        JLabel titleLabel = new JLabel("TẠO TÀI KHOẢN", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- Form đăng ký ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tên đăng nhập
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        userText = new JTextField();
        formPanel.add(userText, gbc);

        // Mật khẩu
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        passText = new JPasswordField();
        formPanel.add(passText, gbc);

        // Xác nhận mật khẩu
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
        gbc.gridx = 1;
        confirmPassText = new JPasswordField();
        formPanel.add(confirmPassText, gbc);

        // Nút đăng ký
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        JButton registerButton = new JButton("Đăng ký");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(registerButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Sự kiện
        registerButton.addActionListener(e -> attemptRegister());
        getRootPane().setDefaultButton(registerButton);
    }

    private void attemptRegister() {
        String username = userText.getText().trim();
        String password = new String(passText.getPassword());
        String confirmPassword = new String(confirmPassText.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập và mật khẩu không được để trống.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Gửi yêu cầu đăng ký đến server
        String[] payload = {username, password};
        Message registerMessage = new Message(Message.MessageType.REGISTER_REQUEST, payload);
        client.sendMessage(registerMessage);

        // Client sẽ nhận phản hồi và hiển thị thông báo thành công/thất bại.
        // Nếu thành công, cửa sổ này sẽ được đóng lại từ Client.java
    }
}