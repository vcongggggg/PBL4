# Hệ thống Quản lý Sinh viên (Student Management System)

## Mô tả dự án

Hệ thống Quản lý Sinh viên là một ứng dụng Java phân tán được xây dựng với kiến trúc Client-Server, sử dụng MySQL làm cơ sở dữ liệu. Hệ thống cho phép quản lý thông tin sinh viên, khóa học, điểm số và các hoạt động học tập khác.

## Tính năng chính

### Dành cho Sinh viên:
- Xem thông tin cá nhân
- Xem danh sách khóa học đã đăng ký
- Xem điểm số và kết quả học tập
- Đổi mật khẩu

### Dành cho Giảng viên:
- Quản lý thông tin sinh viên trong lớp
- Quản lý khóa học giảng dạy
- Nhập và quản lý điểm số
- Xem báo cáo và thống kê lớp học

### Dành cho Quản trị viên:
- Quản lý toàn bộ sinh viên trong hệ thống
- Quản lý tất cả khóa học
- Quản lý người dùng và phân quyền
- Xem báo cáo tổng hợp
- Quản trị hệ thống

## Kiến trúc hệ thống

```
┌─────────────────┐    TCP/IP    ┌─────────────────┐    JDBC    ┌─────────────────┐
│   Client GUI    │ ◄─────────► │     Server      │ ◄─────────► │   MySQL DB      │
│   (Swing/AWT)   │             │   (Multi-thread)│             │                 │
└─────────────────┘             └─────────────────┘             └─────────────────┘
```

### Thành phần chính:
1. **Server**: Xử lý logic nghiệp vụ, quản lý kết nối client, tương tác với database
2. **Client**: Giao diện người dùng, giao tiếp với server qua TCP/IP
3. **Database**: MySQL lưu trữ dữ liệu hệ thống

## Yêu cầu hệ thống

### Phần mềm cần thiết:
- **Java**: JDK 11 hoặc cao hơn
- **Maven**: 3.6.0 hoặc cao hơn
- **MySQL**: 8.0 hoặc cao hơn
- **IDE**: IntelliJ IDEA, Eclipse, hoặc Visual Studio Code

### Thư viện sử dụng:
- MySQL Connector/J 8.0.33
- FlatLaf 3.2.5 (Modern Look and Feel)
- Jackson 2.15.2 (JSON processing)
- BCrypt 0.4 (Password hashing)
- Logback 1.4.11 (Logging)

## Cài đặt và chạy

### 1. Chuẩn bị Database

```sql
-- Tạo database
CREATE DATABASE student_management_system;

-- Import schema và dữ liệu mẫu
mysql -u root -p student_management_system < database_setup.sql
```

### 2. Cấu hình kết nối Database

Chỉnh sửa file `src/main/resources/database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/student_management_system
db.username=root
db.password=your_password
db.driver=com.mysql.cj.jdbc.Driver
```

### 3. Build dự án

```bash
# Clone dự án
git clone <repository-url>
cd student-management-system

# Build với Maven
mvn clean compile
```

### 4. Chạy Server

```bash
# Chạy server (mặc định port 8888)
mvn exec:java -Dexec.mainClass="com.university.sms.server.ServerMain"

# Hoặc chạy với port tùy chỉnh
mvn exec:java -Dexec.mainClass="com.university.sms.server.ServerMain" -Dexec.args="9999"
```

### 5. Chạy Client

```bash
# Chạy client GUI
mvn exec:java -Dexec.mainClass="com.university.sms.client.ClientMain"
```

## Tài khoản mặc định

Sau khi import database, bạn có thể sử dụng các tài khoản sau để đăng nhập:

| Loại tài khoản | Username | Password | Mô tả |
|----------------|----------|----------|-------|
| Admin | admin | password | Quản trị viên hệ thống |
| Giảng viên | gv001 | password | Nguyễn Văn A |
| Giảng viên | gv002 | password | Trần Thị B |
| Sinh viên | sv001 | password | Lê Văn C |
| Sinh viên | sv002 | password | Phạm Thị D |

## Cấu trúc dự án

```
src/
├── main/
│   ├── java/
│   │   └── com/university/sms/
│   │       ├── client/          # Client application
│   │       │   ├── gui/         # Swing GUI components
│   │       │   ├── ServerConnection.java
│   │       │   └── ClientMain.java
│   │       ├── server/          # Server application
│   │       │   ├── StudentManagementServer.java
│   │       │   ├── ClientHandler.java
│   │       │   └── ServerMain.java
│   │       ├── common/          # Shared classes
│   │       │   ├── Message.java
│   │       │   └── Constants.java
│   │       ├── model/           # Data models
│   │       ├── dao/             # Data Access Objects
│   │       ├── service/         # Business logic services
│   │       └── util/            # Utility classes
│   └── resources/
│       └── database.properties  # Database configuration
├── database_setup.sql           # Database schema and sample data
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

## Tính năng nổi bật

### 1. Kiến trúc phân tán
- Server đa luồng, hỗ trợ nhiều client đồng thời
- Giao tiếp qua TCP/IP với protocol tùy chỉnh
- Xử lý lỗi và tự động kết nối lại

### 2. Bảo mật
- Mã hóa mật khẩu với BCrypt
- Phân quyền người dùng (Admin, Teacher, Student)
- Xác thực và phiên làm việc

### 3. Giao diện thân thiện
- Modern Look and Feel với FlatLaf
- Responsive design
- Hỗ trợ đa vai trò người dùng

### 4. Quản lý dữ liệu
- ORM đơn giản với DAO pattern
- Transaction support
- Database connection pooling

## Lệnh Server Console

Khi server đang chạy, bạn có thể sử dụng các lệnh sau:

- `status` - Hiển thị trạng thái server
- `clients` - Danh sách client đang kết nối
- `stats` - Thống kê chi tiết
- `broadcast` - Gửi thông báo đến tất cả client
- `db` - Test kết nối database
- `stop` - Dừng server
- `help` - Hiển thị trợ giúp

## Phát triển tiếp

### Tính năng có thể mở rộng:
- [ ] Quản lý điểm danh tự động
- [ ] Hệ thống thông báo real-time
- [ ] Export/Import dữ liệu Excel
- [ ] API REST cho mobile app
- [ ] Dashboard analytics
- [ ] Email notifications
- [ ] File upload/download
- [ ] Multi-language support

### Cải tiến kỹ thuật:
- [ ] Connection pooling optimization
- [ ] Caching layer (Redis)
- [ ] Microservices architecture
- [ ] Docker containerization
- [ ] Unit testing coverage
- [ ] Performance monitoring

## Troubleshooting

### Lỗi thường gặp:

1. **Không kết nối được database**
   - Kiểm tra MySQL service đã chạy
   - Xác nhận thông tin kết nối trong `database.properties`
   - Đảm bảo database đã được tạo

2. **Client không kết nối được server**
   - Kiểm tra server đã được khởi động
   - Xác nhận port và địa chỉ IP
   - Kiểm tra firewall settings

3. **Lỗi Maven build**
   - Đảm bảo JDK 11+ được cài đặt
   - Chạy `mvn clean install` để tải dependencies
   - Kiểm tra kết nối internet

## Đóng góp

Để đóng góp vào dự án:

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## License

Dự án này được phát triển cho mục đích học tập trong khóa học PBL4.

## Liên hệ

- **Nhóm phát triển**: PBL4 Team
- **Email**: [your-email@university.edu.vn]
- **Năm**: 2024

---

*Hệ thống Quản lý Sinh viên - Phiên bản 1.0*
