I. Cơ sở Lập trình Mạng và Kiến trúc Hệ thống
1. Kiến thức Mạng Cơ sở (TCP/IP, Socket)
Lý thuyết của bạn mô tả chính xác mô hình mà game đang sử dụng:

Giao thức: Game sử dụng TCP (Stream Socket) để đảm bảo dữ liệu (như câu trả lời, tin nhắn chat) được gửi đi một cách đáng tin cậy và đúng thứ tự.
Giao diện Socket:
Phía Server: Trong c:\LTM\CrosswordGame\src\server\core\Server.java, ServerSocket được tạo để lắng nghe kết nối. Phương thức serverSocket.accept() chính là điểm chờ client kết nối tới.
java
// c:\LTM\CrosswordGame\src\server\core\Server.java
try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    // ...
    while (true) {
        Socket socket = serverSocket.accept(); // Chờ và chấp nhận kết nối từ client
        // ...
    }
}
Phía Client: Trong c:\LTM\CrosswordGame\src\client\network\Client.java, một Socket được tạo để chủ động kết nối đến Server.
java
// c:\LTM\CrosswordGame\src\client\network\Client.java
public void connect() {
    try {
        socket = new Socket(hostname, port); // Thiết lập kết nối TCP
        // ...
    } // ...
}
2. Mô hình Lập trình Mạng (Client/Server)
Dự án của bạn là một ví dụ kinh điển của mô hình Client/Server:

Server (MainServer.java): Đóng vai trò trung tâm. Nó không có giao diện, chạy liên tục để quản lý người dùng, các trận đấu (GameSession), và làm "trọng tài" (xử lý câu trả lời, tính điểm).
Client (MainClient.java): Là chương trình mà người chơi tương tác. Nó có giao diện đồ họa (GUI), gửi yêu cầu (đăng nhập, thách đấu, trả lời) đến server và hiển thị thông tin nhận được từ server.
3. Ngôn ngữ Lập trình (Java)
Dự án đã tận dụng rất tốt các thế mạnh của Java:

Lập trình Hướng đối tượng (OOP): Cấu trúc được chia thành các lớp có trách nhiệm rõ ràng như Client, Server, GameSession, DatabaseManager, Message.
Giao diện đồ họa (GUI): Toàn bộ package client.gui sử dụng thư viện Swing để tạo các cửa sổ LoginWindow, LobbyWindow, GameWindow.
I/O theo luồng: Game sử dụng ObjectInputStream và ObjectOutputStream để gửi và nhận toàn bộ đối tượng Message qua mạng. Điều này giúp đóng gói dữ liệu một cách mạch lạc.
Gợi ý cải tiến: Việc tuần tự hóa đối tượng Java (Serialization) rất tiện lợi nhưng có thể không linh hoạt nếu client và server được viết bằng các ngôn ngữ khác nhau. Trong các dự án lớn hơn, người ta thường sử dụng các định dạng dữ liệu trung gian như JSON hoặc Protobuf để giao tiếp.
Lập trình Đa luồng: Đây là yếu tố cốt lõi giúp server phục vụ nhiều người chơi cùng lúc.
Server: Mỗi khi có client kết nối, một ClientHandler mới được tạo và chạy trên một Thread riêng.
java
// c:\LTM\CrosswordGame\src\server\core\Server.java
ClientHandler clientHandler = new ClientHandler(socket, this);
new Thread(clientHandler).start();
Client: Một luồng ServerListener được tạo để lắng nghe tin nhắn từ server, giúp giao diện không bị "đơ" khi chờ dữ liệu.
II. Các Kỹ thuật Lập trình và Giao thức Tương tác
1. Lập trình với Socket (TCP)
Như đã phân tích ở trên, dự án sử dụng Socket và ServerSocket cho giao thức TCP. Luồng dữ liệu liên tục được duy trì trong suốt quá trình client kết nối.

2. Lập trình Đa luồng
Phía Server: Lớp Server sử dụng ConcurrentHashMap để lưu danh sách người dùng online. Đây là một cấu trúc dữ liệu an toàn cho môi trường đa luồng, giúp tránh lỗi khi nhiều ClientHandler cùng lúc đọc/ghi vào danh sách.
Phía Client: Các cập nhật lên giao diện từ luồng ServerListener đều được đặt trong SwingUtilities.invokeLater(). Điều này đảm bảo rằng mọi thay đổi trên GUI đều được thực hiện trên luồng xử lý sự kiện (Event Dispatch Thread - EDT), tuân thủ đúng quy tắc của Swing và tránh các lỗi hiển thị không mong muốn.
3. Lập trình Phân tán với RMI & WebSocket
Dự án của bạn không sử dụng RMI hay WebSocket. Nó được xây dựng hoàn toàn dựa trên Java Sockets cơ bản, là một nền tảng vững chắc và phù hợp cho loại game này.
III. Cấu trúc Phát triển và CSDL
1. Mô hình MVC (Model-View-Controller)
Dự án của bạn có cấu trúc phân lớp rất gần với mô hình MVC:

Model: Các lớp trong package shared (như Message, PlayerStats, CrosswordInfo) và các lớp game logic trong server.game (như Crossword, Question) đại diện cho dữ liệu.
View: Toàn bộ package client.gui (LoginWindow, GameWindow,...) là tầng View, chịu trách nhiệm hiển thị và nhận tương tác từ người dùng.
Controller:
Phía Client: Lớp client.network.Client hoạt động như một Controller, nhận sự kiện từ View (ví dụ: người dùng nhấn nút "Đăng nhập"), tạo Message và gửi đi. Nó cũng nhận dữ liệu từ ServerListener và ra lệnh cho View cập nhật.
Phía Server: Các lớp ClientHandler và GameSession là Controller, xử lý logic nghiệp vụ, tương tác với Model và DatabaseManager.
2. Lập trình với Cơ sở dữ liệu (CSDL)
Lớp server.db.DatabaseManager là minh chứng rõ ràng nhất. Nó đóng gói toàn bộ logic truy vấn CSDL.
JDBC Driver: Tệp DatabaseManager.java đã có một khối static để tải com.microsoft.sqlserver.jdbc.SQLServerDriver, đây là một thực hành tốt để đảm bảo driver được nạp trước khi có bất kỳ kết nối nào được thực hiện.
Bảo mật: Mã nguồn đã có một cảnh báo rất quan trọng: mật khẩu đang được lưu dưới dạng văn bản thuần. Đây là một lỗ hổng bảo mật nghiêm trọng.
Đề xuất cải tiến: Bạn nên sử dụng các thuật toán băm (hashing) như BCrypt hoặc Argon2 để lưu mật khẩu. Khi người dùng đăng nhập, bạn sẽ băm mật khẩu họ nhập vào và so sánh với chuỗi băm đã lưu trong CSDL.
IV. Yêu cầu Đặc thù cho Game Tương tác
Dự án của bạn đã triển khai hầu hết các chức năng đặc thù này:

Quản lý Người dùng: DatabaseManager xử lý validateUser và registerUser.
Quản lý Trạng thái: Server.java dùng onlineUsers để quản lý ai đang online. Trạng thái "bận" được quản lý ngầm thông qua việc một ClientHandler có tham chiếu đến một GameSession hay không (currentGameSession != null).
Thách đấu và Chấp nhận: Logic này được xử lý trong ClientHandler qua các phương thức handleChallengeRequest và handleChallengeResponse.
Làm Trọng tài: Lớp GameSession chính là trọng tài. Nó nhận câu trả lời (processAnswer), kiểm tra tính đúng đắn, cập nhật điểm, và quyết định người thắng cuộc.
Thống kê/Xếp hạng: DatabaseManager có các phương thức getLeaderboard và getMatchHistory để cung cấp dữ liệu này cho client hiển thị.
