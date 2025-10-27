// src/client/gui/LobbyWindow.java
package client.gui;

import client.network.Client;
import shared.PlayerStats;
import shared.Message;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LobbyWindow extends JFrame {
    private final Client client;
    private final DefaultListModel<String> userListModel;
    private final JList<String> userJList;
    private final DefaultTableModel leaderboardTableModel;

    public LobbyWindow(Client client) {
        this.client = client;

        setTitle("Sảnh Chờ");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2, 10, 10)); // Chia làm 2 cột

        // Panel hiển thị danh sách người chơi
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder("Người chơi đang online"));

        userListModel = new DefaultListModel<>();
        userJList = new JList<>(userListModel);
        userJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(userJList);
        userPanel.add(scrollPane, BorderLayout.CENTER);

        // Nút mời chơi
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton challengeButton = new JButton("Thách đấu");
        JButton historyButton = new JButton("Lịch sử đấu");
        buttonPanel.add(challengeButton);
        buttonPanel.add(historyButton);

        userPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(userPanel);

        // Panel hiển thị bảng xếp hạng
        JPanel leaderboardPanel = new JPanel(new BorderLayout());
        leaderboardPanel.setBorder(BorderFactory.createTitledBorder("Bảng Xếp Hạng"));

        String[] columnNames = {"Hạng", "Tên", "Điểm", "Thắng"};
        leaderboardTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa bảng
            }
        };
        JTable leaderboardTable = new JTable(leaderboardTableModel);
        leaderboardPanel.add(new JScrollPane(leaderboardTable), BorderLayout.CENTER);
        add(leaderboardPanel);

        // Sự kiện cho nút thách đấu
        challengeButton.addActionListener(e -> {
            String selectedUser = userJList.getSelectedValue();
            if (selectedUser != null) {
                // Tách tên người dùng ra khỏi trạng thái (ví dụ: "player2 (Online)")
                String targetUsername = selectedUser.split(" ")[0];
                client.sendMessage(new Message(Message.MessageType.CHALLENGE_REQUEST, targetUsername));
                JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới: " + targetUsername);
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một người chơi để thách đấu.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Sự kiện cho nút lịch sử
        historyButton.addActionListener(e -> {
            MatchHistoryWindow historyWindow = new MatchHistoryWindow(this, client);
            client.setMatchHistoryWindow(historyWindow); // Cho client biết về cửa sổ này
            historyWindow.setVisible(true);
        });

        // Yêu cầu server gửi bảng xếp hạng khi mở sảnh
        client.sendMessage(new Message(Message.MessageType.REQUEST_LEADERBOARD, null));
    }

    /**
     * Cập nhật danh sách người chơi trên giao diện.
     * @param users danh sách tên người dùng
     */
    public void updateUserList(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user + " (Online)");
        }
    }

    /**
     * Cập nhật bảng xếp hạng trên giao diện.
     * @param stats danh sách thông tin người chơi
     */
    public void updateLeaderboard(List<PlayerStats> stats) {
        // Xóa dữ liệu cũ
        leaderboardTableModel.setRowCount(0);
        int rank = 1;
        for (PlayerStats stat : stats) {
            leaderboardTableModel.addRow(new Object[]{rank, stat.getUsername(), stat.getTotalScore(), stat.getWins()});
            rank++;
        }
    }
}
