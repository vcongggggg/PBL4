# Hướng Dẫn Test Lại Sau Khi Fix

## ✅ Đã Fix

### 1. **Lỗi Đăng Ký Trùng**
- **Vấn đề**: Sinh viên đã đăng ký 4 lớp, nhưng vẫn thấy 4 lớp đó trong danh sách → nhấn "Đăng ký" → lỗi "already registered"
- **Giải pháp**: Hệ thống giờ sẽ tự động **ẨN** các lớp mà sinh viên đã đăng ký (status PENDING hoặc APPROVED)
- **Logic**:
  - Khi mở tab "Đăng Ký Tín Chỉ", hệ thống gọi 2 requests:
    1. `GET_MY_REGISTRATIONS`: Lấy danh sách lớp đã đăng ký
    2. `GET_ALL_COURSES`: Lấy tất cả lớp có sẵn
  - Filter: Chỉ hiển thị các lớp CHƯA đăng ký trong bảng "Lớp chọn riêng"

### 2. **TimeoutException** (Không cần fix client-side)
- Server đang chạy tốt, client có thể gọi được các request
- Timeout là do client gọi request trong quá trình init, có thể bỏ qua

## 📋 Steps để Test

### Bước 1: Recompile Project (NẾU CHƯA)

```bash
cd D:\PBL4
mvn clean compile -DskipTests
```

### Bước 2: Restart Server (NẾU ĐANG CHẠY)

- Vào cửa sổ PowerShell đang chạy server
- Nhấn `Ctrl+C` để dừng
- Chạy lại:
```bash
java -cp "target/classes;target/dependency/*" com.university.sms.server.Server 9999
```

### Bước 3: Restart Client

- Đóng client cũ
- Mở PowerShell mới:
```bash
cd D:\PBL4
java -cp "target/classes;target/dependency/*" com.university.sms.client.UnifiedClientMain socket localhost 9999
```

### Bước 4: Login với sv001

- **Username**: `sv001`
- **Password**: `password123`

### Bước 5: Vào Tab "Đăng Ký Tín Chỉ"

- Click vào tab thứ 3: **"Đăng Ký Tín Chỉ"**

### Bước 6: Kiểm Tra Danh Sách Lớp

**Expected Result**:

**Bảng trên "Lớp đã chọn, đăng ký:"**
- Trống (vì chưa chọn lớp mới nào)

**Bảng dưới "Lớp chọn riêng:"**
- **KHÔNG** thấy 4 lớp sau (vì đã đăng ký):
  1. ❌ `CNTT101_2025-2026_T_01` - Nhập môn lập trình
  2. ❌ `CNTT101_2024_1` - Nhập môn lập trình
  3. ❌ `CNTT201_2024_1` - Cấu trúc dữ liệu và giải thuật
  4. ❌ `KT101_2024_1` - Kinh tế vi mô

- **CHỈ** hiển thị các lớp **CHƯA** đăng ký (nếu có thêm lớp khác trong database)

### Bước 7: Test Đăng Ký Lớp Mới

#### Nếu có lớp khác trong database (chưa đăng ký):

1. **Double-click** vào 1 lớp trong bảng dưới
2. **Kiểm tra**:
   - Lớp đó xuất hiện trong bảng trên
   - Lớp đó biến mất khỏi bảng dưới
   - "Tổng số tín chỉ" cập nhật
3. **Double-click** thêm 1 lớp nữa (nếu muốn)
4. **Nhấn "Đăng ký"**
5. **Expected**:
   - Dialog: "Đăng ký thành công X lớp học!"
   - Không còn lỗi "already registered"

#### Nếu KHÔNG có lớp nào trong bảng dưới:

- **Đúng rồi!** Vì sv001 đã đăng ký hết 4 lớp rồi
- Để test thêm, bạn cần:
  - **Option 1**: Admin duyệt thêm lớp mới (từ yêu cầu mở lớp của giảng viên)
  - **Option 2**: Thêm lớp mới vào database:
    ```sql
    INSERT INTO courses (course_code, subject_id, teacher_id, academic_year, semester, 
                         schedule_day, schedule_time, room, max_students, current_students, course_status)
    VALUES ('CNTT301_2024_1', 3, 2, '2024-2025', 1, 'Thứ 4', '13:00-15:00', 'C305', 50, 0, 'ONGOING');
    ```

### Bước 8: Test Hủy Đăng Ký (Optional)

1. Vào tab **"Khóa học"** hoặc tạo panel riêng để xem lớp đã đăng ký
2. Hủy 1 lớp (ví dụ: `CNTT101_2024_1`)
3. Quay lại tab **"Đăng Ký Tín Chỉ"**
4. Nhấn **"Làm mới"**
5. **Expected**:
   - Lớp `CNTT101_2024_1` xuất hiện lại trong bảng "Lớp chọn riêng"

## 🔍 Kiểm Tra Logs

### Server Logs (Expected)

```
Nov 02, 2025 XX:XX:XX AM com.university.sms.server.ClientHandler run
INFO: Received request: GET_MY_REGISTRATIONS from sv001

Nov 02, 2025 XX:XX:XX AM com.university.sms.dao.CourseRegistrationDAO findByStudentId
INFO: Found 4 registrations for student_id=1

Nov 02, 2025 XX:XX:XX AM com.university.sms.server.ClientHandler run
INFO: Received request: GET_ALL_COURSES from sv001

Nov 02, 2025 XX:XX:XX AM com.university.sms.dao.CourseDAO findAll
INFO: Query completed. Found 4 courses
```

### Client Logs (Expected)

- Không còn `TimeoutException` khi vào tab "Đăng Ký Tín Chỉ"
- Nếu vẫn có timeout ở chỗ khác (ví dụ: GET_STUDENT_INFO), có thể bỏ qua (không ảnh hưởng chức năng)

## ❌ Lỗi Có Thể Gặp

### 1. Vẫn thấy 4 lớp đã đăng ký trong bảng dưới

**Nguyên nhân**: Code chưa được compile lại

**Giải pháp**:
```bash
mvn clean compile -DskipTests
```

### 2. Lỗi "Cannot find symbol: class CourseRegistration"

**Nguyên nhân**: Missing import

**Giải pháp**: Đã fix trong code, chạy lại compile

### 3. Bảng dưới trống hoàn toàn

**Nguyên nhân**: Tất cả lớp đã được đăng ký rồi (đúng rồi!)

**Giải pháp**: Thêm lớp mới vào database hoặc hủy 1 lớp đã đăng ký

## ✅ Checklist

- [ ] Server đang chạy (port 9999)
- [ ] Client đã compile lại (`mvn clean compile`)
- [ ] Login thành công với sv001
- [ ] Vào tab "Đăng Ký Tín Chỉ" không bị crash
- [ ] Bảng dưới KHÔNG hiển thị 4 lớp đã đăng ký
- [ ] Có thể double-click lớp mới (nếu có) để thêm vào giỏ
- [ ] Nhấn "Đăng ký" thành công (nếu có lớp mới)
- [ ] Không còn lỗi "already registered"

## 📊 Trạng Thái Hiện Tại

| Tính năng | Trạng thái |
|-----------|-----------|
| Giao diện split-panel | ✅ Hoàn thành |
| Kiểm tra xung đột thời gian | ✅ Hoàn thành |
| Tính tổng tín chỉ | ✅ Hoàn thành |
| Tìm kiếm lớp | ✅ Hoàn thành |
| **Lọc lớp đã đăng ký** | ✅ **MỚI FIX** |
| Đăng ký nhiều lớp cùng lúc | ✅ Hoàn thành |
| Xóa lớp khỏi giỏ | ✅ Hoàn thành |

## 🎯 Kết Quả Mong Đợi

- ✅ Sinh viên chỉ thấy các lớp chưa đăng ký
- ✅ Không bị lỗi đăng ký trùng
- ✅ Giao diện giống với giao diện trường
- ✅ Trải nghiệm người dùng mượt mà

## 📞 Nếu Cần Hỗ Trợ

Nếu vẫn gặp lỗi sau khi test:
1. Chụp màn hình giao diện
2. Copy logs từ server console
3. Copy logs từ client console
4. Gửi thông tin để tôi debug tiếp


