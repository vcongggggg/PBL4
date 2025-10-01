-- ===============================================
-- HỆ THỐNG QUẢN LÝ SINH VIÊN - DATABASE SCHEMA
-- ===============================================

-- Tạo database
CREATE DATABASE IF NOT EXISTS student_management_system;
USE student_management_system;

-- ===============================================
-- 1. BẢNG NGƯỜI DÙNG (USERS)
-- ===============================================
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('admin', 'teacher', 'student') NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- ===============================================
-- 2. BẢNG KHOA (DEPARTMENTS)
-- ===============================================
CREATE TABLE departments (
    department_id INT PRIMARY KEY AUTO_INCREMENT,
    department_code VARCHAR(10) UNIQUE NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    description TEXT,
    head_teacher_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (head_teacher_id) REFERENCES users(user_id)
);

-- ===============================================
-- 3. BẢNG LỚP HỌC (CLASSES)
-- ===============================================
CREATE TABLE classes (
    class_id INT PRIMARY KEY AUTO_INCREMENT,
    class_code VARCHAR(20) UNIQUE NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    teacher_id INT,
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    max_students INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    FOREIGN KEY (teacher_id) REFERENCES users(user_id)
);

-- ===============================================
-- 4. BẢNG SINH VIÊN (STUDENTS)
-- ===============================================
CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE NOT NULL,
    student_code VARCHAR(20) UNIQUE NOT NULL,
    class_id INT,
    department_id INT NOT NULL,
    admission_year INT NOT NULL,
    student_status ENUM('active', 'suspended', 'graduated', 'dropped') DEFAULT 'active',
    gpa DECIMAL(3,2) DEFAULT 0.00,
    total_credits INT DEFAULT 0,
    birth_date DATE,
    gender ENUM('male', 'female', 'other'),
    citizen_id VARCHAR(20) UNIQUE,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(class_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- ===============================================
-- 5. BẢNG MÔN HỌC (SUBJECTS)
-- ===============================================
CREATE TABLE subjects (
    subject_id INT PRIMARY KEY AUTO_INCREMENT,
    subject_code VARCHAR(20) UNIQUE NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    credits INT NOT NULL DEFAULT 3,
    department_id INT NOT NULL,
    prerequisite_subject_id INT,
    description TEXT,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    FOREIGN KEY (prerequisite_subject_id) REFERENCES subjects(subject_id)
);

-- ===============================================
-- 6. BẢNG KHÓA HỌC (COURSES)
-- ===============================================
CREATE TABLE courses (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(30) UNIQUE NOT NULL,
    subject_id INT NOT NULL,
    teacher_id INT NOT NULL,
    class_id INT,
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    schedule_day VARCHAR(20),
    schedule_time VARCHAR(50),
    room VARCHAR(20),
    max_students INT DEFAULT 50,
    current_students INT DEFAULT 0,
    course_status ENUM('planning', 'ongoing', 'completed', 'cancelled') DEFAULT 'planning',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
    FOREIGN KEY (teacher_id) REFERENCES users(user_id),
    FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- ===============================================
-- 7. BẢNG ĐĂNG KÝ MÔN HỌC (ENROLLMENTS)
-- ===============================================
CREATE TABLE enrollments (
    enrollment_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enrollment_status ENUM('enrolled', 'completed', 'dropped', 'failed') DEFAULT 'enrolled',
    final_grade DECIMAL(4,2),
    letter_grade VARCHAR(2),
    grade_points DECIMAL(3,2),
    attendance_rate DECIMAL(5,2) DEFAULT 0.00,
    UNIQUE KEY unique_enrollment (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

-- ===============================================
-- 8. BẢNG ĐIỂM SỐ CHI TIẾT (GRADES)
-- ===============================================
CREATE TABLE grades (
    grade_id INT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id INT NOT NULL,
    grade_type ENUM('assignment', 'quiz', 'midterm', 'final', 'project') NOT NULL,
    grade_name VARCHAR(100),
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(3,2) DEFAULT 1.00,
    grade_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
);

-- ===============================================
-- 9. BẢNG ĐIỂM DANH (ATTENDANCE)
-- ===============================================
CREATE TABLE attendance (
    attendance_id INT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('present', 'absent', 'late', 'excused') NOT NULL,
    notes TEXT,
    recorded_by INT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_attendance (enrollment_id, attendance_date),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- ===============================================
-- 10. BẢNG THÔNG BÁO (NOTIFICATIONS)
-- ===============================================
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    sender_id INT NOT NULL,
    target_type ENUM('all', 'department', 'class', 'student') NOT NULL,
    target_id INT,
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(user_id)
);

-- ===============================================
-- 11. BẢNG LỊCH SỬ ĐĂNG NHẬP (LOGIN_HISTORY)
-- ===============================================
CREATE TABLE login_history (
    login_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_status ENUM('success', 'failed') NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ===============================================
-- 12. BẢNG CẤU HÌNH HỆ THỐNG (SYSTEM_CONFIG)
-- ===============================================
CREATE TABLE system_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description TEXT,
    updated_by INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

-- ===============================================
-- INDEXES ĐỂ TỐI ƯU HIỆU SUẤT
-- ===============================================

-- Indexes cho bảng users
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);

-- Indexes cho bảng students
CREATE INDEX idx_students_code ON students(student_code);
CREATE INDEX idx_students_class ON students(class_id);
CREATE INDEX idx_students_status ON students(student_status);

-- Indexes cho bảng courses
CREATE INDEX idx_courses_teacher ON courses(teacher_id);
CREATE INDEX idx_courses_subject ON courses(subject_id);
CREATE INDEX idx_courses_year_semester ON courses(academic_year, semester);

-- Indexes cho bảng enrollments
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
CREATE INDEX idx_enrollments_status ON enrollments(enrollment_status);

-- Indexes cho bảng grades
CREATE INDEX idx_grades_enrollment ON grades(enrollment_id);
CREATE INDEX idx_grades_type ON grades(grade_type);

-- Indexes cho bảng attendance
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);

-- ===============================================
-- VIEWS ĐỂ TRUY VẤN DỄ DÀNG
-- ===============================================

-- View thông tin sinh viên đầy đủ
CREATE VIEW v_student_info AS
SELECT 
    s.student_id,
    s.student_code,
    u.full_name,
    u.email,
    u.phone,
    d.department_name,
    c.class_name,
    s.admission_year,
    s.gpa,
    s.total_credits,
    s.student_status
FROM students s
JOIN users u ON s.user_id = u.user_id
JOIN departments d ON s.department_id = d.department_id
LEFT JOIN classes c ON s.class_id = c.class_id;

-- View thông tin khóa học
CREATE VIEW v_course_info AS
SELECT 
    co.course_id,
    co.course_code,
    sub.subject_name,
    sub.credits,
    u.full_name AS teacher_name,
    cl.class_name,
    co.academic_year,
    co.semester,
    co.schedule_day,
    co.schedule_time,
    co.room,
    co.current_students,
    co.max_students,
    co.course_status
FROM courses co
JOIN subjects sub ON co.subject_id = sub.subject_id
JOIN users u ON co.teacher_id = u.user_id
LEFT JOIN classes cl ON co.class_id = cl.class_id;

-- View bảng điểm sinh viên
CREATE VIEW v_student_grades AS
SELECT 
    s.student_code,
    u.full_name AS student_name,
    sub.subject_code,
    sub.subject_name,
    e.final_grade,
    e.letter_grade,
    co.academic_year,
    co.semester
FROM enrollments e
JOIN students s ON e.student_id = s.student_id
JOIN users u ON s.user_id = u.user_id
JOIN courses co ON e.course_id = co.course_id
JOIN subjects sub ON co.subject_id = sub.subject_id
WHERE e.enrollment_status = 'completed';

-- ===============================================
-- TRIGGERS ĐỂ TỰ ĐỘNG CẬP NHẬT DỮ LIỆU
-- ===============================================

-- Trigger cập nhật số lượng sinh viên trong khóa học
DELIMITER //
CREATE TRIGGER tr_enrollment_insert
AFTER INSERT ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_id = NEW.course_id 
        AND enrollment_status = 'enrolled'
    )
    WHERE course_id = NEW.course_id;
END//

CREATE TRIGGER tr_enrollment_update
AFTER UPDATE ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_id = NEW.course_id 
        AND enrollment_status = 'enrolled'
    )
    WHERE course_id = NEW.course_id;
END//

CREATE TRIGGER tr_enrollment_delete
AFTER DELETE ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_id = OLD.course_id 
        AND enrollment_status = 'enrolled'
    )
    WHERE course_id = OLD.course_id;
END//
DELIMITER ;

-- ===============================================
-- STORED PROCEDURES CƠ BẢN (CHỈ GIỮ LẠI CÁC PROCEDURE ĐƠN GIẢN)
-- ===============================================

-- 1. Procedure tính GPA cho sinh viên (Tác vụ tính toán đơn giản)
DELIMITER //
CREATE PROCEDURE CalculateStudentGPA(IN p_student_id INT)
BEGIN
    DECLARE v_gpa DECIMAL(3,2);
    DECLARE v_total_credits INT;
    
    -- Tính GPA từ dữ liệu local trên node này
    SELECT 
        ROUND(SUM(e.grade_points * s.credits) / SUM(s.credits), 2),
        SUM(s.credits)
    INTO v_gpa, v_total_credits
    FROM enrollments e
    JOIN courses c ON e.course_id = c.course_id
    JOIN subjects s ON c.subject_id = s.subject_id
    WHERE e.student_id = p_student_id 
    AND e.enrollment_status = 'completed'
    AND e.grade_points IS NOT NULL;
    
    -- Cập nhật trực tiếp trong database
    UPDATE students 
    SET gpa = COALESCE(v_gpa, 0.00),
        total_credits = COALESCE(v_total_credits, 0)
    WHERE student_id = p_student_id;
END//
DELIMITER ;

-- 2. Procedure tính tỷ lệ điểm danh (Tác vụ đơn giản)
DELIMITER //
CREATE PROCEDURE CalculateAttendanceRate(IN p_enrollment_id INT)
BEGIN
    DECLARE v_attendance_rate DECIMAL(5,2);
    
    -- Tính tỷ lệ điểm danh từ dữ liệu local
    SELECT 
        ROUND(
            (SUM(CASE WHEN status IN ('present', 'late') THEN 1 ELSE 0 END) * 100.0) 
            / COUNT(*), 2
        )
    INTO v_attendance_rate
    FROM attendance 
    WHERE enrollment_id = p_enrollment_id;
    
    -- Cập nhật trực tiếp
    UPDATE enrollments 
    SET attendance_rate = COALESCE(v_attendance_rate, 0.00)
    WHERE enrollment_id = p_enrollment_id;
END//
DELIMITER ;

-- 3. Procedure tính điểm cuối kỳ (Logic tính toán cố định)
DELIMITER //
CREATE PROCEDURE CalculateFinalGrade(IN p_enrollment_id INT)
BEGIN
    DECLARE v_final_score DECIMAL(5,2) DEFAULT 0;
    DECLARE v_letter_grade VARCHAR(2);
    DECLARE v_grade_points DECIMAL(3,2);
    
    -- Tính điểm tổng kết từ các thành phần
    SELECT 
        ROUND(SUM(score * weight / max_score * 10), 2)
    INTO v_final_score
    FROM grades 
    WHERE enrollment_id = p_enrollment_id;
    
    -- Logic chuyển đổi điểm (cố định, không thay đổi)
    CASE 
        WHEN v_final_score >= 9.0 THEN 
            SET v_letter_grade = 'A+'; SET v_grade_points = 4.0;
        WHEN v_final_score >= 8.5 THEN 
            SET v_letter_grade = 'A'; SET v_grade_points = 3.7;
        WHEN v_final_score >= 8.0 THEN 
            SET v_letter_grade = 'B+'; SET v_grade_points = 3.3;
        WHEN v_final_score >= 7.0 THEN 
            SET v_letter_grade = 'B'; SET v_grade_points = 3.0;
        WHEN v_final_score >= 6.5 THEN 
            SET v_letter_grade = 'C+'; SET v_grade_points = 2.3;
        WHEN v_final_score >= 5.5 THEN 
            SET v_letter_grade = 'C'; SET v_grade_points = 2.0;
        WHEN v_final_score >= 5.0 THEN 
            SET v_letter_grade = 'D+'; SET v_grade_points = 1.3;
        WHEN v_final_score >= 4.0 THEN 
            SET v_letter_grade = 'D'; SET v_grade_points = 1.0;
        ELSE 
            SET v_letter_grade = 'F'; SET v_grade_points = 0.0;
    END CASE;
    
    -- Cập nhật kết quả
    UPDATE enrollments 
    SET 
        final_grade = v_final_score,
        letter_grade = v_letter_grade,
        grade_points = v_grade_points,
        enrollment_status = CASE 
            WHEN v_final_score >= 5.0 THEN 'completed'
            ELSE 'failed'
        END
    WHERE enrollment_id = p_enrollment_id;
END//
DELIMITER ;

-- 4. Procedure cập nhật trạng thái sinh viên (Tác vụ đơn giản)
DELIMITER //
CREATE PROCEDURE UpdateStudentStatus(
    IN p_student_id INT,
    IN p_new_status ENUM('active', 'suspended', 'graduated', 'dropped')
)
BEGIN
    -- Cập nhật trạng thái đơn giản
    UPDATE students 
    SET student_status = p_new_status,
        updated_at = CURRENT_TIMESTAMP
    WHERE student_id = p_student_id;
    
    -- Log thay đổi (nếu cần thiết)
    INSERT INTO student_status_log (student_id, old_status, new_status, changed_at)
    SELECT p_student_id, 
           (SELECT student_status FROM students WHERE student_id = p_student_id),
           p_new_status,
           CURRENT_TIMESTAMP;
END//
DELIMITER ;

-- 5. Procedure ghi log đăng nhập (Tác vụ đơn giản)
DELIMITER //
CREATE PROCEDURE LogUserLogin(
    IN p_user_id INT,
    IN p_ip_address VARCHAR(45),
    IN p_user_agent TEXT,
    IN p_status ENUM('success', 'failed')
)
BEGIN
    -- Ghi log đơn giản
    INSERT INTO login_history (user_id, ip_address, user_agent, login_status)
    VALUES (p_user_id, p_ip_address, p_user_agent, p_status);
    
    -- Cleanup log cũ (giữ 30 ngày gần nhất)
    DELETE FROM login_history 
    WHERE login_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
END//
DELIMITER ;

-- ===============================================
-- BẢNG HỖ TRỢ CHO CÁC PROCEDURE
-- ===============================================

-- Bảng log thay đổi trạng thái sinh viên
CREATE TABLE IF NOT EXISTS student_status_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    old_status ENUM('active', 'suspended', 'graduated', 'dropped'),
    new_status ENUM('active', 'suspended', 'graduated', 'dropped'),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id)
);

-- ===============================================
-- DỮ LIỆU MẪU ĐỂ TEST HỆ THỐNG
-- ===============================================

-- Thêm cấu hình hệ thống
INSERT INTO system_config (config_key, config_value, description) VALUES
('academic_year_current', '2024-2025', 'Năm học hiện tại'),
('semester_current', '1', 'Học kỳ hiện tại'),
('max_credits_per_semester', '24', 'Số tín chỉ tối đa mỗi học kỳ'),
('min_attendance_rate', '80', 'Tỷ lệ điểm danh tối thiểu (%)'),
('passing_grade', '5.0', 'Điểm đậu tối thiểu');

-- Thêm admin mặc định
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('admin', 'password', 'admin@university.edu.vn', 'Quản trị viên hệ thống', 'admin', '0123456789', 'Trường Đại học ABC');

-- Thêm khoa mẫu
INSERT INTO departments (department_code, department_name, description) VALUES
('CNTT', 'Công nghệ thông tin', 'Khoa Công nghệ thông tin'),
('KT', 'Kinh tế', 'Khoa Kinh tế'),
('NN', 'Ngoại ngữ', 'Khoa Ngoại ngữ'),
('KHTN', 'Khoa học tự nhiên', 'Khoa Khoa học tự nhiên');

-- Thêm giảng viên mẫu
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('gv001', 'password', 'nguyenvana@university.edu.vn', 'Nguyễn Văn A', 'teacher', '0987654321', 'Hà Nội'),
('gv002', 'password', 'tranthib@university.edu.vn', 'Trần Thị B', 'teacher', '0987654322', 'Hà Nội');

-- Thêm lớp học mẫu
INSERT INTO classes (class_code, class_name, department_id, teacher_id, academic_year, semester) VALUES
('CNTT2024A', 'Công nghệ thông tin 2024A', 1, 2, '2024-2025', 1),
('KT2024A', 'Kinh tế 2024A', 2, 3, '2024-2025', 1);

-- Thêm sinh viên mẫu
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('sv001', 'password', 'sv001@student.university.edu.vn', 'Lê Văn C', 'student', '0123456788', 'Hà Nội'),
('sv002', 'password', 'sv002@student.university.edu.vn', 'Phạm Thị D', 'student', '0123456787', 'Hà Nội');

INSERT INTO students (user_id, student_code, class_id, department_id, admission_year, birth_date, gender, citizen_id) VALUES
(4, 'SV2024001', 1, 1, 2024, '2002-05-15', 'male', '001202012345'),
(5, 'SV2024002', 2, 2, 2024, '2002-08-20', 'female', '001202054321');

-- Thêm môn học mẫu
INSERT INTO subjects (subject_code, subject_name, credits, department_id, description) VALUES
('CNTT101', 'Nhập môn lập trình', 3, 1, 'Môn học cơ bản về lập trình'),
('CNTT201', 'Cấu trúc dữ liệu và giải thuật', 4, 1, 'Môn học về cấu trúc dữ liệu'),
('KT101', 'Kinh tế vi mô', 3, 2, 'Môn học cơ bản về kinh tế vi mô'),
('NN101', 'Tiếng Anh cơ bản', 2, 3, 'Môn học tiếng Anh cơ bản');

-- Thêm khóa học mẫu
INSERT INTO courses (course_code, subject_id, teacher_id, class_id, academic_year, semester, schedule_day, schedule_time, room) VALUES
('CNTT101_2024_1', 1, 2, 1, '2024-2025', 1, 'Thứ 2, Thứ 4', '07:00-09:00', 'A101'),
('KT101_2024_1', 3, 3, 2, '2024-2025', 1, 'Thứ 3, Thứ 5', '09:00-11:00', 'B201');

COMMIT;

-- ===============================================
-- HƯỚNG DẪN SỬ DỤNG CÁC PROCEDURE CƠ BẢN
-- ===============================================

/*
-- 1. Tính GPA cho sinh viên
CALL CalculateStudentGPA(1);

-- 2. Tính tỷ lệ điểm danh cho một enrollment
CALL CalculateAttendanceRate(1);

-- 3. Tính điểm cuối kỳ
CALL CalculateFinalGrade(1);

-- 4. Cập nhật trạng thái sinh viên
CALL UpdateStudentStatus(1, 'graduated');

-- 5. Ghi log đăng nhập
CALL LogUserLogin(1, '192.168.1.100', 'Mozilla/5.0...', 'success');

-- LƯU Ý: 
-- - Các procedure này được thiết kế để tích hợp với Java code
-- - Logic phức tạp (tìm kiếm, validation, báo cáo) nên viết trong Java
-- - Chỉ dùng procedure cho tác vụ tính toán đơn giản và cập nhật nhanh
*/

-- ===============================================
-- KẾT THÚC SCRIPT TẠO DATABASE
-- ===============================================
