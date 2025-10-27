// src/server/MainServer.java
package server;

import server.core.Server;

/**
 * Lớp này chứa phương thức main() để khởi động server.
 * Nó đóng vai trò là điểm vào (entry point) cho ứng dụng phía server.
 */
public class MainServer {

    public static void main(String[] args) {
        // Tạo một đối tượng Server mới.
        Server gameServer = new Server();
        
        // Bắt đầu lắng nghe các kết nối từ client.
        // Chương trình sẽ chạy trong một vòng lặp vô tận bên trong phương thức start()
        // để liên tục chấp nhận các client mới.
        gameServer.start();
    }
}
