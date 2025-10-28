package client.gui;

import client.network.Client;
import shared.CrosswordInfo;
import shared.Message;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Một JDialog tùy chỉnh để người chơi chọn bộ câu hỏi.
 */
public class CrosswordChoiceDialog extends JDialog {
    private final Client client;
    private final String opponentUsername;
    private final JList<CrosswordInfo> crosswordList;
    private boolean selectionMade = false;

    public CrosswordChoiceDialog(Frame owner, Client client, List<CrosswordInfo> crosswords, String opponentUsername) {
        super(owner, "Chọn bộ câu hỏi", true); // true: Modal dialog
        this.client = client;
        this.opponentUsername = opponentUsername;

        setSize(450, 350);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Tiêu đề ---
        JLabel headerLabel = new JLabel("Bạn đã thắng tung đồng xu! Hãy chọn một bộ câu hỏi:", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(headerLabel, BorderLayout.NORTH);

        // --- Danh sách các bộ câu hỏi ---
        DefaultListModel<CrosswordInfo> listModel = new DefaultListModel<>();
        crosswords.forEach(listModel::addElement);

        crosswordList = new JList<>(listModel);
        crosswordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        crosswordList.setSelectedIndex(0); // Chọn mục đầu tiên mặc định
        crosswordList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        crosswordList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Thêm padding cho mỗi mục
                return renderer;
            }
        });
        add(new JScrollPane(crosswordList), BorderLayout.CENTER);

        // --- Các nút điều khiển ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton selectButton = new JButton("Chọn");
        JButton cancelButton = new JButton("Hủy");

        selectButton.addActionListener(e -> onSelect());
        cancelButton.addActionListener(e -> onCancel());

        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Xử lý khi người dùng đóng cửa sổ bằng nút 'X'
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (!selectionMade) {
                    client.cancelCrosswordChoice(opponentUsername);
                }
            }
        });
    }

    private void onSelect() {
        CrosswordInfo selected = crosswordList.getSelectedValue();
        if (selected != null) {
            selectionMade = true;
            // Gửi lựa chọn về server
            client.sendMessage(new Message(Message.MessageType.SUBMIT_CROSSWORD_CHOICE, new Object[]{selected.getId(), opponentUsername}));
            dispose(); // Đóng cửa sổ sau khi chọn
        }
    }

    private void onCancel() {
        client.cancelCrosswordChoice(opponentUsername);
        dispose();
    }
}