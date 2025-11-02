# Hệ thống Quản lý Sinh viên (Student Management System)

## Giới thiệu

Hệ thống Quản lý Sinh viên là một ứng dụng Client-Server được phát triển bằng Java, sử dụng kiến trúc phân tầng và phân quyền theo vai trò người dùng.

## Kiến trúc Hệ thống

### 1. Kiến trúc Tổng quan
- **Server**: Xử lý logic nghiệp vụ và kết nối database MySQL
- **Client**: Giao diện người dùng (GUI) sử dụng Java Swing
- **Database**: MySQL để lưu trữ dữ liệu
- **Protocol**: TCP/IP Socket với Object Serialization

### 2. Cấu trúc Package

```
com.university.sms/
├── server/              # Server-side code
│   ├── ServerMain.java
│   ├── StudentManagementServer.java
│   └── ClientHandler.java
│
├── client/              # Client-side code
│   ├── UnifiedClientMain.java
│   ├── ServerConnection.java
│   └── gui/
│       ├── common/      # Shared GUI components
│       ├── admin/       # Admin-specific UI
│       ├── teacher/     # Teacher-specific UI
│       └── student/     # Student-specific UI
│
├── service/             # Business logic layer
│   ├── AuthenticationService.java
│   ├── StudentService.java
│   └── CourseService.java
│
├── dao/                 # Data Access Objects
│   ├── UserDAO.java
│   ├── StudentDAO.java
│   └── CourseDAO.java
│
├── model/               # Domain models
│   ├── User.java
│   ├── Student.java
│   └── Course.java
│
├── common/              # Shared utilities
│   ├── Constants.java
│   └── Message.java
│
└── util/                # Utilities
    └── DatabaseConnection.java
```

## Yêu cầu Hệ thống

- **Java**: JDK 21 hoặc cao hơn
- **Maven**: 3.6+ (để build project)
- **MySQL**: 8.0+ 
- **OS**: Windows/Linux/macOS

## Cài đặt và Chạy

### Bước 1: Chuẩn bị Database

```sql
-- Tạo database
CREATE DATABASE student_management;

-- Import schema và dữ liệu mẫu
mysql -u root -p student_management < database_setup.sql
```

### Bước 2: Cấu hình Database

Cập nhật thông tin kết nối trong file:
`src/main/java/com/university/sms/util/DatabaseConnection.java`

```java
private static final String URL = "jdbc:mysql://localhost:3306/student_management";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

### Bước 3: Build Project

```bash
# Clone repository
git clone <repository-url>
cd PBL4

# Build project
mvn clean package -DskipTests

# Copy dependencies
mvn dependency:copy-dependencies
```

### Bước 4: Chạy Ứng dụng

#### Cách 1: Sử dụng Batch Scripts (Windows)

```bash
# Chạy Server
start-server.bat

# Chạy Client (terminal mới)
start-client.bat
```

#### Cách 2: Chạy thủ công

```bash
# Terminal 1: Chạy Server
java -cp "target/classes;target/dependency/*" com.university.sms.server.ServerMain

# Terminal 2: Chạy Client
java -cp "target/classes;target/dependency/*" com.university.sms.client.UnifiedClientMain
```

## Tài khoản Đăng nhập

### Admin
- **Username**: `admin`
- **Password**: `password`
- **Quyền**: Quản lý toàn bộ hệ thống

### Giảng viên
- **Username**: `teacher1`
- **Password**: `password`
- **Quyền**: Quản lý lớp học, nhập điểm

### Sinh viên
- **Username**: `student1`
- **Password**: `password`
- **Quyền**: Xem thông tin cá nhân, điểm số

## Chức năng Chính

### Admin
- ✅ Quản lý người dùng (CRUD)
- ✅ Quản lý sinh viên
- ✅ Quản lý môn học
- ✅ Duyệt yêu cầu mở lớp
- ✅ Xem báo cáo tổng hợp

### Giảng viên
- ✅ Xem danh sách sinh viên
- ✅ Nhập và quản lý điểm
- ✅ Xem lịch dạy
- 🔄 Đăng ký mở lớp mới
- ✅ Xem báo cáo lớp học

### Sinh viên
- ✅ Xem thông tin cá nhân
- ✅ Xem thời khóa biểu
- ✅ Xem kết quả học tập
- 🔄 Đăng ký tín chỉ
- ✅ Xem lịch học

*Chú thích: ✅ = Đã hoàn thành, 🔄 = Đang phát triển*

## Công nghệ Sử dụng

- **Java 21**: Ngôn ngữ lập trình chính
- **Java Swing**: Framework GUI
- **FlatLaf**: Modern Look and Feel
- **MySQL**: Database
- **JDBC**: Database connectivity
- **Maven**: Build tool và dependency management
- **Socket Programming**: Client-Server communication
- **Multi-threading**: Xử lý nhiều client đồng thời

## Tính năng Kỹ thuật

### 1. Client-Server Architecture
- Server xử lý nhiều client đồng thời (ThreadPool)
- Giao tiếp qua TCP/IP Socket
- Message-based protocol với Object Serialization

### 2. Security
- Phân quyền theo role (Admin, Teacher, Student)
- Session management
- Kiểm tra quyền ở cả client và server

### 3. Database Design
- Normalized database schema
- Foreign key constraints
- Indexed columns for performance

### 4. Code Organization
- Layered architecture (Presentation, Business, Data)
- DAO pattern
- Service layer pattern
- MVC-like structure

## Cấu trúc GUI

Xem chi tiết trong file: [HUONG-DAN-CAU-TRUC-MOI.md](HUONG-DAN-CAU-TRUC-MOI.md)

## Troubleshooting

### Server không khởi động được
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra port 8888 có bị chiếm dụng không
- Xem log để biết lỗi cụ thể

### Client không kết nối được
- Đảm bảo Server đã chạy
- Kiểm tra địa chỉ IP và port
- Kiểm tra firewall

### Lỗi biên dịch
```bash
# Clean và rebuild
mvn clean compile dependency:copy-dependencies
```

## Đóng góp

Dự án được phát triển bởi Nhóm PBL4

## License

Dự án này được phát triển cho mục đích học tập.

## Liên hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng tạo issue trên repository.

---
*© 2024 Student Management System - PBL4 Team*
