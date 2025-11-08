-- ===============================================
-- HỆ THỐNG QUẢN LÝ SINH VIÊN - REFACTORED SCHEMA
-- SỬ DỤNG UNIQUE CODES LÀM FOREIGN KEYS
-- TRÁNH CONFLICT GIỮA CÁC CLIENTS
-- ===============================================

-- Tạo database
CREATE DATABASE IF NOT EXISTS student_management_system;
USE student_management_system;

-- ===============================================
-- 1. BẢNG NGƯỜI DÙNG (USERS)
-- ===============================================
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
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
-- 2. BẢNG KHOA (FACULTIES)
-- ===============================================
CREATE TABLE faculties (
    faculty_id INT PRIMARY KEY AUTO_INCREMENT,
    faculty_code VARCHAR(10) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
    faculty_name VARCHAR(100) NOT NULL,
    description TEXT,
    head_teacher_username VARCHAR(50),  -- ✅ CHANGED: username thay vì user_id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (head_teacher_username) REFERENCES users(username) ON UPDATE CASCADE
);

-- ===============================================
-- 3. BẢNG LỚP HỌC (CLASSES)
-- ===============================================
CREATE TABLE classes (
    class_id INT PRIMARY KEY AUTO_INCREMENT,
    class_code VARCHAR(20) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
    class_name VARCHAR(100) NOT NULL,
    faculty_code VARCHAR(10) NOT NULL,  -- ✅ CHANGED: faculty_code thay vì faculty_id
    teacher_username VARCHAR(50),  -- ✅ CHANGED: username thay vì teacher_id
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    max_students INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_code) REFERENCES faculties(faculty_code) ON UPDATE CASCADE,
    FOREIGN KEY (teacher_username) REFERENCES users(username) ON UPDATE CASCADE
);

-- ===============================================
-- 4. BẢNG SINH VIÊN (STUDENTS)
-- ===============================================
CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,  -- ✅ CHANGED: trực tiếp username thay vì user_id
    student_code VARCHAR(20) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
    class_code VARCHAR(20),  -- ✅ CHANGED: class_code thay vì class_id
    faculty_code VARCHAR(10) NOT NULL,  -- ✅ CHANGED: faculty_code thay vì faculty_id
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
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (class_code) REFERENCES classes(class_code) ON UPDATE CASCADE,
    FOREIGN KEY (faculty_code) REFERENCES faculties(faculty_code) ON UPDATE CASCADE
);

-- ===============================================
-- 5. BẢNG MÔN HỌC (SUBJECTS)
-- ===============================================
CREATE TABLE subjects (
    subject_id INT PRIMARY KEY AUTO_INCREMENT,
    subject_code VARCHAR(20) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
    subject_name VARCHAR(100) NOT NULL,
    credits INT NOT NULL DEFAULT 3,
    faculty_code VARCHAR(10) NOT NULL,  -- ✅ CHANGED: faculty_code thay vì faculty_id
    prerequisite_subject_code VARCHAR(20),  -- ✅ CHANGED: subject_code thay vì subject_id
    description TEXT,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_code) REFERENCES faculties(faculty_code) ON UPDATE CASCADE,
    FOREIGN KEY (prerequisite_subject_code) REFERENCES subjects(subject_code) ON UPDATE CASCADE
);

-- ===============================================
-- 6. BẢNG KHÓA HỌC (COURSES)
-- ===============================================
CREATE TABLE courses (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(30) UNIQUE NOT NULL,  -- ✅ DÙNG LÀM FK
    subject_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: subject_code thay vì subject_id
    teacher_username VARCHAR(50) NOT NULL,  -- ✅ CHANGED: username thay vì teacher_id
    class_code VARCHAR(20),  -- ✅ CHANGED: class_code thay vì class_id
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
    FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON UPDATE CASCADE,
    FOREIGN KEY (teacher_username) REFERENCES users(username) ON UPDATE CASCADE,
    FOREIGN KEY (class_code) REFERENCES classes(class_code) ON UPDATE CASCADE
);

-- ===============================================
-- 7. BẢNG ĐĂNG KÝ MÔN HỌC (ENROLLMENTS)
-- ✅ KEY CHANGE: Dùng student_code + course_code
-- ===============================================
CREATE TABLE enrollments (
    enrollment_id INT PRIMARY KEY AUTO_INCREMENT,
    student_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: student_code thay vì student_id
    course_code VARCHAR(30) NOT NULL,  -- ✅ CHANGED: course_code thay vì course_id
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enrollment_status ENUM('enrolled', 'completed', 'dropped', 'failed') DEFAULT 'enrolled',
    final_grade DECIMAL(4,2) DEFAULT 0.00,
    letter_grade VARCHAR(2) DEFAULT '',
    grade_points DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_enrollment (student_code, course_code),  -- ✅ Composite unique key
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ===============================================
-- 8. BẢNG ĐIỂM SỐ CHI TIẾT (GRADES)
-- ✅ KEY CHANGE: Dùng composite (student_code, course_code)
-- ===============================================
CREATE TABLE grades (
    grade_id INT PRIMARY KEY AUTO_INCREMENT,
    student_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: Composite FK
    course_code VARCHAR(30) NOT NULL,  -- ✅ CHANGED: Composite FK
    grade_type ENUM('assignment', 'quiz', 'midterm', 'final', 'project') NOT NULL,
    grade_name VARCHAR(100),
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(3,2) DEFAULT 1.00,
    grade_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_code, course_code) 
        REFERENCES enrollments(student_code, course_code) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ===============================================
-- 9. BẢNG THÔNG BÁO (NOTIFICATIONS)
-- ✅ KEY CHANGE: Dùng username và target_code
-- ===============================================
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    sender_username VARCHAR(50) NOT NULL,  -- ✅ CHANGED: username thay vì sender_id
    target_type ENUM('all', 'faculty', 'class', 'student') NOT NULL,
    target_code VARCHAR(50),  -- ✅ CHANGED: faculty_code/class_code/student_code tùy target_type
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (sender_username) REFERENCES users(username) ON UPDATE CASCADE
    -- Note: Không thể tạo FK động cho target_code vì tùy target_type
);

-- ===============================================
-- 10. BẢNG LỊCH SỬ ĐĂNG NHẬP (LOGIN_HISTORY)
-- ===============================================
CREATE TABLE login_history (
    login_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,  -- ✅ CHANGED: username thay vì user_id
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_status ENUM('success', 'failed') NOT NULL,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ===============================================
-- 11. BẢNG CẤU HÌNH HỆ THỐNG (SYSTEM_CONFIG)
-- ===============================================
CREATE TABLE system_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description TEXT,
    updated_by_username VARCHAR(50),  -- ✅ CHANGED: username thay vì updated_by
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by_username) REFERENCES users(username) ON UPDATE CASCADE
);

-- ===============================================
-- 12. BẢNG GẮN NGUỒN DỮ LIỆU (DATA_ORIGIN)
-- ✅ KEEP: Vẫn dùng entity_id vì chỉ tracking internal
-- ===============================================
CREATE TABLE IF NOT EXISTS data_origin (
    entity_type VARCHAR(32) NOT NULL,
    entity_id INT NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_type, entity_id)
);

-- ===============================================
-- 13. BẢNG YÊU CẦU MỞ LỚP (CLASS_OPENING_REQUESTS)
-- ✅ KEY CHANGE: Dùng username và subject_code
-- ===============================================
CREATE TABLE IF NOT EXISTS class_opening_requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    teacher_username VARCHAR(50) NOT NULL,  -- ✅ CHANGED: username thay vì teacher_id
    subject_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: subject_code thay vì subject_id
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    schedule_day VARCHAR(50),
    schedule_time VARCHAR(50),
    room VARCHAR(20),
    max_students INT DEFAULT 50,
    reason TEXT,
    request_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    admin_note TEXT,
    approved_by_username VARCHAR(50),  -- ✅ CHANGED: username thay vì approved_by
    approved_course_code VARCHAR(30),  -- ✅ CHANGED: course_code thay vì approved_course_id
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decision_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_username) REFERENCES users(username) ON UPDATE CASCADE,
    FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON UPDATE CASCADE,
    FOREIGN KEY (approved_by_username) REFERENCES users(username) ON UPDATE CASCADE,
    FOREIGN KEY (approved_course_code) REFERENCES courses(course_code) ON DELETE SET NULL ON UPDATE CASCADE
);

-- ===============================================
-- 14. BẢNG ĐĂNG KÝ HỌC PHẦN (COURSE_REGISTRATIONS)
-- ✅ KEY CHANGE: Dùng student_code + course_code
-- ===============================================
CREATE TABLE IF NOT EXISTS course_registrations (
    registration_id INT PRIMARY KEY AUTO_INCREMENT,
    student_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: student_code thay vì student_id
    course_code VARCHAR(30) NOT NULL,  -- ✅ CHANGED: course_code thay vì course_id
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registration_status ENUM('PENDING', 'APPROVED', 'CANCELLED') DEFAULT 'APPROVED',
    cancel_date TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_registration (student_code, course_code),
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ===============================================
-- BẢNG LOG TRẠNG THÁI SINH VIÊN
-- ===============================================
CREATE TABLE IF NOT EXISTS student_status_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    student_code VARCHAR(20) NOT NULL,  -- ✅ CHANGED: student_code thay vì student_id
    old_status ENUM('active', 'suspended', 'graduated', 'dropped'),
    new_status ENUM('active', 'suspended', 'graduated', 'dropped'),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON UPDATE CASCADE
);

-- ===============================================
-- INDEXES ĐỂ TỐI ƯU HIỆU SUẤT
-- ===============================================

-- Indexes cho bảng users
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);

-- Indexes cho bảng students
CREATE INDEX idx_students_username ON students(username);
CREATE INDEX idx_students_class ON students(class_code);
CREATE INDEX idx_students_faculty ON students(faculty_code);
CREATE INDEX idx_students_status ON students(student_status);

-- Indexes cho bảng courses
CREATE INDEX idx_courses_teacher ON courses(teacher_username);
CREATE INDEX idx_courses_subject ON courses(subject_code);
CREATE INDEX idx_courses_class ON courses(class_code);
CREATE INDEX idx_courses_year_semester ON courses(academic_year, semester);

-- Indexes cho bảng enrollments
CREATE INDEX idx_enrollments_student ON enrollments(student_code);
CREATE INDEX idx_enrollments_course ON enrollments(course_code);
CREATE INDEX idx_enrollments_status ON enrollments(enrollment_status);

-- Indexes cho bảng grades
CREATE INDEX idx_grades_student_course ON grades(student_code, course_code);
CREATE INDEX idx_grades_type ON grades(grade_type);

-- Indexes cho bảng class_opening_requests
CREATE INDEX idx_requests_teacher ON class_opening_requests(teacher_username);
CREATE INDEX idx_requests_status ON class_opening_requests(request_status);
CREATE INDEX idx_requests_subject ON class_opening_requests(subject_code);
CREATE INDEX idx_requests_year_semester ON class_opening_requests(academic_year, semester);

-- Indexes cho bảng course_registrations  
CREATE INDEX idx_registrations_student ON course_registrations(student_code);
CREATE INDEX idx_registrations_course ON course_registrations(course_code);
CREATE INDEX idx_registrations_status ON course_registrations(registration_status);

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
    f.faculty_name,
    c.class_name,
    s.admission_year,
    s.gpa,
    s.total_credits,
    s.student_status,
    dor.source AS data_source
FROM students s
JOIN users u ON s.username = u.username
JOIN faculties f ON s.faculty_code = f.faculty_code
LEFT JOIN classes c ON s.class_code = c.class_code
LEFT JOIN data_origin dor ON dor.entity_type = 'student' AND dor.entity_id = s.student_id;

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
    co.course_status,
    dor.source AS data_source
FROM courses co
JOIN subjects sub ON co.subject_code = sub.subject_code
JOIN users u ON co.teacher_username = u.username
LEFT JOIN classes cl ON co.class_code = cl.class_code
LEFT JOIN data_origin dor ON dor.entity_type = 'course' AND dor.entity_id = co.course_id;

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
JOIN students s ON e.student_code = s.student_code
JOIN users u ON s.username = u.username
JOIN courses co ON e.course_code = co.course_code
JOIN subjects sub ON co.subject_code = sub.subject_code
WHERE e.enrollment_status = 'completed';

-- ===============================================
-- TRIGGERS CẬP NHẬT SỐ LƯỢNG SINH VIÊN
-- ===============================================

DELIMITER //
CREATE TRIGGER tr_enrollment_insert
AFTER INSERT ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_code = NEW.course_code
        AND enrollment_status = 'enrolled'
    )
    WHERE course_code = NEW.course_code;
END//

CREATE TRIGGER tr_enrollment_update
AFTER UPDATE ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_code = NEW.course_code
        AND enrollment_status = 'enrolled'
    )
    WHERE course_code = NEW.course_code;
END//

CREATE TRIGGER tr_enrollment_delete
AFTER DELETE ON enrollments
FOR EACH ROW
BEGIN
    UPDATE courses 
    SET current_students = (
        SELECT COUNT(*) 
        FROM enrollments 
        WHERE course_code = OLD.course_code
        AND enrollment_status = 'enrolled'
    )
    WHERE course_code = OLD.course_code;
END//

-- Trigger tự động tạo enrollment khi registration approved
CREATE TRIGGER tr_registration_approved
AFTER INSERT ON course_registrations
FOR EACH ROW
BEGIN
    IF NEW.registration_status = 'APPROVED' THEN
        INSERT INTO enrollments (student_code, course_code, enrollment_status)
        VALUES (NEW.student_code, NEW.course_code, 'enrolled')
        ON DUPLICATE KEY UPDATE enrollment_status = 'enrolled';
    END IF;
END//

-- Trigger xóa enrollment khi registration cancelled
CREATE TRIGGER tr_registration_cancelled
AFTER UPDATE ON course_registrations
FOR EACH ROW
BEGIN
    IF NEW.registration_status = 'CANCELLED' AND OLD.registration_status != 'CANCELLED' THEN
        DELETE FROM enrollments 
        WHERE student_code = NEW.student_code AND course_code = NEW.course_code;
    END IF;
END//

DELIMITER ;

-- ===============================================
-- TRIGGERS TĂNG VERSION
-- ===============================================

DELIMITER //
CREATE TRIGGER tr_students_insert_version AFTER INSERT ON students FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_students_update_version AFTER UPDATE ON students FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_students_delete_version AFTER DELETE ON students FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_courses_insert_version AFTER INSERT ON courses FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_courses_update_version AFTER UPDATE ON courses FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_courses_delete_version AFTER DELETE ON courses FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_enrollments_insert_version AFTER INSERT ON enrollments FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_enrollments_update_version AFTER UPDATE ON enrollments FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//

CREATE TRIGGER tr_enrollments_delete_version AFTER DELETE ON enrollments FOR EACH ROW
BEGIN
    UPDATE system_config SET config_value = CAST(config_value AS UNSIGNED) + 1 WHERE config_key = 'db_version';
END//
DELIMITER ;

-- ===============================================
-- STORED PROCEDURES
-- ===============================================

DELIMITER //
CREATE PROCEDURE CalculateStudentGPA(IN p_student_code VARCHAR(20))
BEGIN
    DECLARE v_gpa DECIMAL(3,2);
    DECLARE v_total_credits INT;
    
    SELECT 
        ROUND(SUM(e.grade_points * s.credits) / SUM(s.credits), 2),
        SUM(s.credits)
    INTO v_gpa, v_total_credits
    FROM enrollments e
    JOIN courses c ON e.course_code = c.course_code
    JOIN subjects s ON c.subject_code = s.subject_code
    WHERE e.student_code = p_student_code
    AND e.enrollment_status = 'completed'
    AND e.grade_points IS NOT NULL;
    
    UPDATE students 
    SET gpa = COALESCE(v_gpa, 0.00),
        total_credits = COALESCE(v_total_credits, 0)
    WHERE student_code = p_student_code;
END//

CREATE PROCEDURE CalculateFinalGradeByCode(
    IN p_student_code VARCHAR(20),
    IN p_course_code VARCHAR(30)
)
BEGIN
    DECLARE v_final_score DECIMAL(5,2) DEFAULT 0;
    DECLARE v_letter_grade VARCHAR(2);
    DECLARE v_grade_points DECIMAL(3,2);
    
    SELECT ROUND(SUM(score * weight / max_score * 10), 2)
    INTO v_final_score
    FROM grades 
    WHERE student_code = p_student_code AND course_code = p_course_code;
    
    CASE 
        WHEN v_final_score >= 9.0 THEN SET v_letter_grade = 'A+'; SET v_grade_points = 4.0;
        WHEN v_final_score >= 8.5 THEN SET v_letter_grade = 'A'; SET v_grade_points = 3.7;
        WHEN v_final_score >= 8.0 THEN SET v_letter_grade = 'B+'; SET v_grade_points = 3.3;
        WHEN v_final_score >= 7.0 THEN SET v_letter_grade = 'B'; SET v_grade_points = 3.0;
        WHEN v_final_score >= 6.5 THEN SET v_letter_grade = 'C+'; SET v_grade_points = 2.3;
        WHEN v_final_score >= 5.5 THEN SET v_letter_grade = 'C'; SET v_grade_points = 2.0;
        WHEN v_final_score >= 5.0 THEN SET v_letter_grade = 'D+'; SET v_grade_points = 1.3;
        WHEN v_final_score >= 4.0 THEN SET v_letter_grade = 'D'; SET v_grade_points = 1.0;
        ELSE SET v_letter_grade = 'F'; SET v_grade_points = 0.0;
    END CASE;
    
    UPDATE enrollments 
    SET 
        final_grade = v_final_score,
        letter_grade = v_letter_grade,
        grade_points = v_grade_points,
        enrollment_status = CASE WHEN v_final_score >= 5.0 THEN 'completed' ELSE 'failed' END
    WHERE student_code = p_student_code AND course_code = p_course_code;
END//
DELIMITER ;

-- ===============================================
-- DỮ LIỆU MẪU
-- ===============================================

-- System config
INSERT INTO system_config (config_key, config_value, description) VALUES
('academic_year_current', '2024-2025', 'Năm học hiện tại'),
('semester_current', '1', 'Học kỳ hiện tại'),
('max_credits_per_semester', '24', 'Số tín chỉ tối đa mỗi học kỳ'),
('passing_grade', '5.0', 'Điểm đậu tối thiểu'),
('db_version', '1', 'Database version cho sync mechanism');

-- Admin
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('admin', 'password', 'admin@university.edu.vn', 'Quản trị viên hệ thống', 'admin', '0123456789', 'Trường Đại học ABC');

-- Faculties
INSERT INTO faculties (faculty_code, faculty_name, description) VALUES
('CNTT', 'Công nghệ thông tin', 'Khoa Công nghệ thông tin'),
('KT', 'Kinh tế', 'Khoa Kinh tế'),
('NN', 'Ngoại ngữ', 'Khoa Ngoại ngữ'),
('KHTN', 'Khoa học tự nhiên', 'Khoa Khoa học tự nhiên');

-- Teachers
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('gv001', 'password', 'nguyenvana@university.edu.vn', 'Nguyễn Văn A', 'teacher', '0987654321', 'Hà Nội'),
('gv002', 'password', 'tranthib@university.edu.vn', 'Trần Thị B', 'teacher', '0987654322', 'Hà Nội');

-- Classes
INSERT INTO classes (class_code, class_name, faculty_code, teacher_username, academic_year, semester) VALUES
('CNTT2024A', 'Công nghệ thông tin 2024A', 'CNTT', 'gv001', '2024-2025', 1),
('KT2024A', 'Kinh tế 2024A', 'KT', 'gv002', '2024-2025', 1);

-- Students
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('sv001', 'password', 'sv001@student.university.edu.vn', 'Lê Văn C', 'student', '0123456788', 'Hà Nội'),
('sv002', 'password', 'sv002@student.university.edu.vn', 'Phạm Thị D', 'student', '0123456787', 'Hà Nội');

INSERT INTO students (username, student_code, class_code, faculty_code, admission_year, birth_date, gender, citizen_id) VALUES
('sv001', 'SV2024001', 'CNTT2024A', 'CNTT', 2024, '2002-05-15', 'male', '001202012345'),
('sv002', 'SV2024002', 'KT2024A', 'KT', 2024, '2002-08-20', 'female', '001202054321');

-- Subjects
INSERT INTO subjects (subject_code, subject_name, credits, faculty_code, description) VALUES
('CNTT101', 'Nhập môn lập trình', 3, 'CNTT', 'Môn học cơ bản về lập trình'),
('CNTT201', 'Cấu trúc dữ liệu và giải thuật', 4, 'CNTT', 'Môn học về cấu trúc dữ liệu'),
('KT101', 'Kinh tế vi mô', 3, 'KT', 'Môn học cơ bản về kinh tế vi mô'),
('NN101', 'Tiếng Anh cơ bản', 2, 'NN', 'Môn học tiếng Anh cơ bản');

-- Courses
INSERT INTO courses (course_code, subject_code, teacher_username, class_code, academic_year, semester, schedule_day, schedule_time, room, course_status) VALUES
('CNTT101_2024_1', 'CNTT101', 'gv001', 'CNTT2024A', '2024-2025', 1, 'Thứ 2, Thứ 4', '07:00-09:00', 'A101', 'ongoing'),
('KT101_2024_1', 'KT101', 'gv002', 'KT2024A', '2024-2025', 1, 'Thứ 3, Thứ 5', '09:00-11:00', 'B201', 'ongoing'),
('CNTT201_2024_1', 'CNTT201', 'gv001', 'CNTT2024A', '2024-2025', 1, 'Thứ 3, Thứ 6', '13:00-17:00', 'C305', 'ongoing');

COMMIT;

-- ===============================================
-- MIGRATION NOTES
-- ===============================================
/*
✅ THAY ĐỔI CHÍNH:

1. ENROLLMENTS:
   - student_id → student_code
   - course_id → course_code
   
2. COURSE_REGISTRATIONS:
   - student_id → student_code
   - course_id → course_code
   
3. GRADES:
   - enrollment_id → (student_code, course_code) composite FK
   
4. CLASS_OPENING_REQUESTS:
   - teacher_id → teacher_username
   - subject_id → subject_code
   - approved_by → approved_by_username
   - approved_course_id → approved_course_code
   
5. NOTIFICATIONS:
   - sender_id → sender_username
   - target_id → target_code
   
6. STUDENTS:
   - user_id → username
   - class_id → class_code
   - faculty_id → faculty_code
   
7. COURSES:
   - subject_id → subject_code
   - teacher_id → teacher_username
   - class_id → class_code
   
8. CLASSES:
   - faculty_id → faculty_code
   - teacher_id → teacher_username
   
9. FACULTIES:
   - head_teacher_id → head_teacher_username
   
10. SUBJECTS:
    - faculty_id → faculty_code
    - prerequisite_subject_id → prerequisite_subject_code

✅ LỢI ÍCH:
- Không conflict giữa các clients (CSV, POSTGRES, MYSQL)
- student_code, course_code là UNIQUE và STABLE
- Dễ debug và trace data
- Có thể merge data từ nhiều nguồn

✅ TRADE-OFFS:
- JOIN query chậm hơn một chút (VARCHAR vs INT)
- Cần index tốt trên các code columns
- Phải ensure codes không bao giờ thay đổi
*/

