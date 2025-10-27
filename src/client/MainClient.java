package client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf; // 1. Import thư viện FlatLaf

import client.gui.LoginWindow;
import client.network.Client;

/**
 * Lớp này chứa phương thức main() để khởi động ứng dụng Client.
 * Nó là điểm vào (entry point) cho toàn bộ chương trình phía client.
 */

public class MainClient {
    public static void main(String[] args) {
        // Luôn chạy các tác vụ GUI trên Event Dispatch Thread (EDT) của Swing
        // để đảm bảo an toàn luồng và tránh các lỗi giao diện không mong muốn.
        SwingUtilities.invokeLater(() -> {
            try {
                // 2. Thiết lập FlatLaf làm Look and Feel chính
                // Bạn có thể thử các theme khác như FlatDarkLaf, FlatIntelliJLaf, FlatMacLightLaf...
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Cấu hình thông tin để kết nối tới server.
            // Sử dụng "localhost" hoặc "127.0.0.1" nếu server chạy trên cùng máy.
            // Nếu không, hãy thay thế bằng địa chỉ IP của máy chủ.
            
            String hostname = "localhost"; 
            int port = 12345;              // Phải khớp với cổng mà Server đang lắng nghe

            // 1. Tạo đối tượng Client để quản lý kết nối và giao tiếp mạng
            Client client = new Client(hostname, port);

            // 2. Tạo cửa sổ đăng nhập và truyền đối tượng client vào
            LoginWindow loginWindow = new LoginWindow(client);
            client.setLoginWindow(loginWindow); // Cung cấp tham chiếu cửa sổ cho client

            // 3. Hiển thị cửa sổ đăng nhập cho người dùng
            loginWindow.setVisible(true);

            // 4. Bắt đầu quá trình kết nối tới server ở chế độ nền
            client.connect();
        });
    }
}
