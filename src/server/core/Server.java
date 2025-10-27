// src/server/core/Server.java
package server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import shared.Message;

public class Server {
    public static final int PORT = 12345;
    // Sử dụng ConcurrentHashMap để đảm bảo an toàn luồng khi quản lý client
    private final ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public void start() {
        System.out.println("Server is starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started successfully. Waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                // Tạo một luồng xử lý riêng cho mỗi client
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Thêm người dùng vào danh sách online và thông báo cho mọi người.
     * @param username Tên người dùng
     * @param handler Đối tượng xử lý của người dùng đó
     */
    public void addUser(String username, ClientHandler handler) {
        // Thêm người dùng mới vào danh sách
        onlineUsers.put(username, handler);
        System.out.println(username + " has logged in.");
        // Gửi danh sách cập nhật cho những người khác để họ biết có người mới vào.
        // Người dùng mới sẽ tự yêu cầu danh sách sau khi vào sảnh.
        broadcastUserList();
    }

    /**
     * Xóa người dùng khỏi danh sách online (khi ngắt kết nối) và thông báo cho mọi người.
     * @param username Tên người dùng
     */
    public void removeUser(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            System.out.println(username + " has disconnected.");
            broadcastUserList();
        }
    }
    
    /**
     * Gửi danh sách người dùng đang online cập nhật đến tất cả các client.
     */
    public void broadcastUserList() {
        List<String> currentUsers = new ArrayList<>(onlineUsers.keySet());
        System.out.println("Broadcasting user list update: " + currentUsers);
        for (ClientHandler handler : onlineUsers.values()) {
            // Tạo một Message mới với một bản sao của danh sách cho mỗi client.
            // Điều này đảm bảo client này sửa đổi danh sách sẽ không ảnh hưởng đến client khác.
            Message message = new Message(Message.MessageType.USER_LIST_UPDATE, new ArrayList<>(currentUsers));
            handler.sendMessage(message);
        }
    }

    /**
     * Gửi danh sách người dùng hiện tại cho một client cụ thể.
     * @param targetHandler ClientHandler của người nhận.
     */
    public void sendUserListTo(ClientHandler targetHandler) {
        // Gửi một bản sao của danh sách để tránh các vấn đề về an toàn luồng
        List<String> userListCopy = new ArrayList<>(onlineUsers.keySet());
        Message message = new Message(Message.MessageType.USER_LIST_UPDATE, userListCopy);
        targetHandler.sendMessage(message);
    }

    /**
     * Tìm và trả về ClientHandler của một người dùng đang online.
     * @param username Tên người dùng cần tìm
     * @return ClientHandler hoặc null nếu không tìm thấy.
     */
    public ClientHandler getClientHandler(String username) {
        return onlineUsers.get(username);
    }

    /**
     * Kiểm tra xem một người dùng đã online hay chưa.
     * @param username Tên người dùng cần kiểm tra.
     * @return true nếu người dùng đã có trong danh sách online.
     */
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
}