// src/client/gui/MatchHistoryWindow.java
package client.gui;

import client.network.Client;
import shared.MatchHistoryEntry;
import shared.Message;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class MatchHistoryWindow extends JDialog {
    private final DefaultTableModel tableModel;

    public MatchHistoryWindow(Frame owner, Client client) {
        super(owner, "Lịch sử đấu", true);
        setSize(600, 400);
        setLocationRelativeTo(owner);

        String[] columnNames = {"Đối thủ", "Kết quả", "Tỉ số", "Ngày chơi"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable historyTable = new JTable(tableModel);
        add(new JScrollPane(historyTable), BorderLayout.CENTER);

        // Yêu cầu server gửi dữ liệu
        client.sendMessage(new Message(Message.MessageType.REQUEST_MATCH_HISTORY, null));
    }

    /**
     * Cập nhật bảng lịch sử với dữ liệu từ server.
     * @param history danh sách các trận đấu
     */
    public void updateHistory(List<MatchHistoryEntry> history) {
        // Xóa dữ liệu cũ
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (MatchHistoryEntry entry : history) {
            String score = entry.getMyScore() + " - " + entry.getOpponentScore();
            tableModel.addRow(new Object[]{
                entry.getOpponent(),
                entry.getResult(),
                score,
                sdf.format(entry.getMatchDate())
            });
        }
    }
}