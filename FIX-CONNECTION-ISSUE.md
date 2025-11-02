# Fix Lỗi "Không thể kết nối đến server"

## Vấn Đề

Server đang chạy trên port **8888**, nhưng client không kết nối được.

## Nguyên Nhân

1. Client chưa compile lại code mới
2. Hoặc đang chạy client cũ với port sai
3. LoginFrame default port đã đúng là 8888, nhưng cần recompile

## Giải Pháp

### Bước 1: Dừng Client Hiện Tại (NẾU ĐANG CHẠY)
- Đóng cửa sổ client hiện tại

### Bước 2: Compile Lại Client
```bash
cd D:\PBL4
mvn clean compile -DskipTests
```

### Bước 3: Chạy Client
**Option 1: Dùng Maven**
```bash
mvn exec:java@client
```

**Option 2: Dùng Java trực tiếp**
```bash
java -cp "target/classes;target/dependency/*" com.university.sms.client.UnifiedClientMain socket localhost 8888
```

### Bước 4: Kiểm Tra Giao Diện Đăng Nhập

Khi cửa sổ login hiện ra, kiểm tra:

**Kết nối Server:**
- Địa chỉ Server: `localhost`
- Port: **8888** (PHẢI LÀ 8888, KHÔNG PHẢI 9999)
- Nhấn nút **"Kết nối"**

**Kết quả mong đợi:**
- ✅ Status: "Đã kết nối" (màu xanh)
- ✅ Nút "Đăng nhập" được kích hoạt

### Bước 5: Đăng Nhập

**Sinh viên:**
- Username: `sv001`
- Password: `password123`

**Giảng viên:**
- Username: `gv001`
- Password: `password123`

**Admin:**
- Username: `admin`
- Password: `admin123`

## Kiểm Tra Server Logs

Sau khi nhấn "Kết nối" trên client, kiểm tra server logs:

**Expected:**
```
Nov 02, 2025 XX:XX:XX AM com.university.sms.server.StudentManagementServer$ClientHandlerThread run
INFO: New client connected from /127.0.0.1:XXXXX

Connected Clients: 1
```

Nếu KHÔNG thấy log này → Client chưa kết nối được.

## Troubleshooting

### 1. Vẫn không kết nối được

**Kiểm tra port trong LoginFrame:**
- Khi cửa sổ login hiện ra, xem field "Port" có là **8888** không?
- Nếu KHÔNG phải 8888 → Sửa thành 8888 rồi nhấn "Kết nối"

### 2. Lỗi "Connection refused"

**Nguyên nhân**: Server chưa chạy hoặc port sai

**Giải pháp**:
```bash
# Kiểm tra server có đang chạy không
netstat -ano | findstr :8888

# Nếu không có kết quả → server chưa chạy
# Start server:
mvn exec:java@server
```

### 3. Lỗi "Address already in use"

**Nguyên nhân**: Port 8888 đang được sử dụng bởi process khác

**Giải pháp**:
```bash
# Tìm process đang dùng port 8888
netstat -ano | findstr :8888

# Output example:
# TCP    0.0.0.0:8888    0.0.0.0:0    LISTENING    12345
#                                                    ↑
#                                                   PID

# Kill process:
taskkill /PID 12345 /F

# Hoặc đổi port server sang 9999 trong ServerMain.java
```

## Nếu Muốn Dùng Port 9999

### Option 1: Đổi port trong code (KHUYÊN DÙNG)

**File**: `src/main/java/com/university/sms/server/ServerMain.java`

```java
public class ServerMain {
    private static final int DEFAULT_PORT = 9999;  // <-- Đổi từ 8888 → 9999
    // ...
}
```

**File**: `src/main/java/com/university/sms/client/gui/common/LoginFrame.java`

```java
private void initializeComponents() {
    // ...
    portField = new JTextField("9999", 8);  // <-- Đổi từ "8888" → "9999"
    // ...
}
```

Sau đó:
```bash
mvn clean compile -DskipTests
```

### Option 2: Chạy server với port khác (TẠM THỜI)

```bash
# Dừng server hiện tại (Ctrl+C)

# Start server với port 9999:
java -cp "target/classes;target/dependency/*" com.university.sms.server.ServerMain 9999

# Start client:
java -cp "target/classes;target/dependency/*" com.university.sms.client.UnifiedClientMain socket localhost 9999
```

## Quick Commands (Copy & Paste)

### Terminal 1 - Server
```powershell
cd D:\PBL4
mvn clean compile -DskipTests
mvn exec:java@server
```

### Terminal 2 - Client
```powershell
cd D:\PBL4
mvn exec:java@client
```

**Trong giao diện login:**
- Địa chỉ Server: `localhost`
- Port: `8888`
- Nhấn "Kết nối"
- Username: `sv001`
- Password: `password123`
- Nhấn "Đăng nhập"

## Status

✅ Server đang chạy: port **8888**
✅ LoginFrame default: port **8888**
⚠️ Cần recompile client nếu vừa thay đổi code
⚠️ Đảm bảo không có firewall chặn port 8888

## Logs để Debug

### Client không connect được

**Check 1**: Port đúng chưa?
```
LoginFrame field: portField = new JTextField("8888", 8);
```

**Check 2**: Server có đang lắng nghe không?
```powershell
netstat -ano | findstr :8888
```

**Check 3**: Client có gửi request không?
- Xem client console có log gì không
- Xem server console có log "New client connected" không

### Client connect được nhưng không đăng nhập được

**Check 1**: Database có dữ liệu không?
```sql
SELECT * FROM users WHERE username = 'sv001';
```

**Check 2**: Server có nhận được LOGIN request không?
```
Nov 02, 2025 XX:XX:XX AM com.university.sms.server.ClientHandler run
INFO: Received request: LOGIN from sv001
```

## Kết Luận

**Nguyên nhân chính**: Client cần recompile và chạy lại với port đúng (8888).

**Fix nhanh nhất**:
1. `mvn clean compile -DskipTests`
2. `mvn exec:java@client`
3. Nhập port: `8888`
4. Nhấn "Kết nối"
5. Đăng nhập với `sv001` / `password123`

✅ Done!


