-- ===============================================
-- HỆ THỐNG QUẢN LÝ SINH VIÊN - POSTGRESQL SCHEMA
-- CHỈ LƯU CÁC BẢNG GIỐNG CSV CLIENT
-- ===============================================

-- Tạo database (chạy riêng với quyền superuser)
-- CREATE DATABASE student_management_system;
-- \c student_management_system;

-- ===============================================
-- TẠO ENUM TYPES
-- ===============================================

CREATE TYPE user_role AS ENUM ('admin', 'teacher', 'student');
CREATE TYPE student_status_type AS ENUM ('active', 'suspended', 'graduated', 'dropped');
CREATE TYPE gender_type AS ENUM ('male', 'female', 'other');
CREATE TYPE registration_status_type AS ENUM ('locked', 'open', 'closed');
CREATE TYPE course_status_type AS ENUM ('planning', 'ongoing', 'completed', 'cancelled');
CREATE TYPE enrollment_status_type AS ENUM ('enrolled', 'completed', 'dropped', 'failed');
CREATE TYPE grade_type_enum AS ENUM ('assignment', 'quiz', 'midterm', 'final', 'project');
CREATE TYPE notification_target_type AS ENUM ('all', 'faculty', 'class', 'student');
CREATE TYPE notification_priority_type AS ENUM ('low', 'medium', 'high', 'urgent');
CREATE TYPE request_status_type AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE registration_status_reg_type AS ENUM ('PENDING', 'APPROVED', 'CANCELLED');

-- ===============================================
-- 1. BẢNG KHOA (FACULTIES)
-- ===============================================
CREATE TABLE faculties (
    faculty_id SERIAL PRIMARY KEY,
    faculty_code VARCHAR(10) UNIQUE NOT NULL,
    faculty_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===============================================
-- 2. BẢNG NGƯỜI DÙNG (USERS)
-- ===============================================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role user_role NOT NULL,
    phone VARCHAR(20) UNIQUE,
    address TEXT,
    faculty_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 3. BẢNG LỚP HỌC (CLASSES)
-- ===============================================
CREATE TABLE classes (
    class_id SERIAL PRIMARY KEY,
    class_code VARCHAR(20) UNIQUE NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    faculty_code VARCHAR(10) NOT NULL,
    teacher_username VARCHAR(50),
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    max_students INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 4. BẢNG SINH VIÊN (STUDENTS)
-- ===============================================
CREATE TABLE students (
    student_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    student_code VARCHAR(20) UNIQUE NOT NULL,
    class_code VARCHAR(20),
    faculty_code VARCHAR(10) NOT NULL,
    admission_year INT NOT NULL,
    student_status student_status_type DEFAULT 'active',
    gpa DECIMAL(3,2) DEFAULT 0.00,
    total_credits INT DEFAULT 0,
    birth_date DATE,
    gender gender_type,
    citizen_id VARCHAR(20) UNIQUE,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 5. BẢNG MÔN HỌC (SUBJECTS)
-- ===============================================
CREATE TABLE subjects (
    subject_id SERIAL PRIMARY KEY,
    subject_code VARCHAR(20) UNIQUE NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    credits INT NOT NULL DEFAULT 3,
    faculty_code VARCHAR(10) NOT NULL,
    prerequisite_subject_code VARCHAR(20),
    description TEXT,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 6. BẢNG KHÓA HỌC (COURSES)
-- ===============================================
CREATE TABLE courses (
    course_id SERIAL PRIMARY KEY,
    course_code VARCHAR(30) UNIQUE NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    teacher_username VARCHAR(50) NOT NULL,
    class_code VARCHAR(20),
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    schedule_day VARCHAR(20),
    schedule_time VARCHAR(50),
    room VARCHAR(20),
    max_students INT DEFAULT 50,
    current_students INT DEFAULT 0,
    registration_status registration_status_type DEFAULT 'locked',
    course_status course_status_type DEFAULT 'planning',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 7. BẢNG ĐĂNG KÝ MÔN HỌC (ENROLLMENTS)
-- ===============================================
CREATE TABLE enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enrollment_status enrollment_status_type DEFAULT 'enrolled',
    final_grade DECIMAL(4,2) DEFAULT 0.00,
    letter_grade VARCHAR(2) DEFAULT '',
    grade_points DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_code, course_code)
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 8. BẢNG ĐIỂM SỐ CHI TIẾT (GRADES)
-- ===============================================
CREATE TABLE grades (
    grade_id SERIAL PRIMARY KEY,
    student_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    grade_type grade_type_enum NOT NULL,
    grade_name VARCHAR(100),
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(3,2) DEFAULT 1.00,
    grade_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 9. BẢNG THÔNG BÁO (NOTIFICATIONS)
-- ===============================================
CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    sender_username VARCHAR(50) NOT NULL,
    target_type notification_target_type NOT NULL,
    target_code VARCHAR(50),
    priority notification_priority_type DEFAULT 'medium',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL DEFAULT NULL
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 10. BẢNG YÊU CẦU MỞ LỚP (CLASS_OPENING_REQUESTS)
-- ===============================================
CREATE TABLE class_opening_requests (
    request_id SERIAL PRIMARY KEY,
    teacher_username VARCHAR(50) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester INT NOT NULL,
    schedule_day VARCHAR(50),
    schedule_time VARCHAR(50),
    room VARCHAR(20),
    max_students INT DEFAULT 50,
    reason TEXT,
    request_status request_status_type DEFAULT 'PENDING',
    admin_note TEXT,
    approved_by_username VARCHAR(50),
    approved_course_code VARCHAR(30),
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decision_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 11. BẢNG ĐĂNG KÝ HỌC PHẦN (COURSE_REGISTRATIONS)
-- ===============================================
CREATE TABLE course_registrations (
    registration_id SERIAL PRIMARY KEY,
    student_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registration_status registration_status_reg_type DEFAULT 'APPROVED',
    cancel_date TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_code, course_code)
    -- Không có FOREIGN KEY: PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
);

-- ===============================================
-- 12. BẢNG VERSION TRACKING CHO POSTGRES CLIENT
-- ===============================================
CREATE TABLE postgres_client_version (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===============================================
-- INDEXES
-- ===============================================

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_students_username ON students(username);
CREATE INDEX idx_students_class ON students(class_code);
CREATE INDEX idx_students_faculty ON students(faculty_code);
CREATE INDEX idx_students_status ON students(student_status);
CREATE INDEX idx_courses_teacher ON courses(teacher_username);
CREATE INDEX idx_courses_subject ON courses(subject_code);
CREATE INDEX idx_courses_class ON courses(class_code);
CREATE INDEX idx_courses_year_semester ON courses(academic_year, semester);
CREATE INDEX idx_enrollments_student ON enrollments(student_code);
CREATE INDEX idx_enrollments_course ON enrollments(course_code);
CREATE INDEX idx_enrollments_status ON enrollments(enrollment_status);
CREATE INDEX idx_grades_student_course ON grades(student_code, course_code);
CREATE INDEX idx_grades_type ON grades(grade_type);
CREATE INDEX idx_requests_teacher ON class_opening_requests(teacher_username);
CREATE INDEX idx_requests_status ON class_opening_requests(request_status);
CREATE INDEX idx_requests_subject ON class_opening_requests(subject_code);
CREATE INDEX idx_registrations_student ON course_registrations(student_code);
CREATE INDEX idx_registrations_course ON course_registrations(course_code);
CREATE INDEX idx_registrations_status ON course_registrations(registration_status);

-- ===============================================
-- TRIGGERS CẬP NHẬT updated_at TỰ ĐỘNG
-- ===============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_grades_updated_at BEFORE UPDATE ON grades
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_class_opening_requests_updated_at BEFORE UPDATE ON class_opening_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_course_registrations_updated_at BEFORE UPDATE ON course_registrations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ===============================================
-- TRIGGERS CẬP NHẬT SỐ LƯỢNG SINH VIÊN
-- ===============================================

CREATE OR REPLACE FUNCTION update_course_student_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        UPDATE courses 
        SET current_students = (
            SELECT COUNT(*) 
            FROM enrollments 
            WHERE course_code = NEW.course_code
            AND enrollment_status IN ('enrolled', 'completed', 'failed')
        )
        WHERE course_code = NEW.course_code;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE courses 
        SET current_students = (
            SELECT COUNT(*) 
            FROM enrollments 
            WHERE course_code = OLD.course_code
            AND enrollment_status IN ('enrolled', 'completed', 'failed')
        )
        WHERE course_code = OLD.course_code;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_enrollment_insert
    AFTER INSERT ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_course_student_count();

CREATE TRIGGER tr_enrollment_update
    AFTER UPDATE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_course_student_count();

CREATE TRIGGER tr_enrollment_delete
    AFTER DELETE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_course_student_count();

-- Không có triggers tự động tạo/xóa enrollment vì PostgreSQL client chỉ là nơi lưu trữ dữ liệu đơn giản
-- Các triggers này sẽ được xử lý ở server side nếu cần

-- ===============================================
-- DỮ LIỆU MẪU
-- ===============================================

-- Admin
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('admin', 'password', 'admin@university.edu.vn', 'Quản trị viên hệ thống', 'admin', '0123456789', 'Trường Đại học ABC')
ON CONFLICT (username) DO NOTHING;

-- Faculties
INSERT INTO faculties (faculty_code, faculty_name, description) VALUES
('CNTT', 'Công nghệ thông tin', 'Khoa CNTT')
ON CONFLICT (faculty_code) DO NOTHING;

-- Teacher
INSERT INTO users (username, password, email, full_name, role, faculty_code) VALUES
('gv01', 'password', 'gv01@university.edu.vn', 'Giáo viên GV01', 'teacher', 'CNTT')
ON CONFLICT (username) DO NOTHING;

-- Classes
INSERT INTO classes (class_code, class_name, faculty_code, teacher_username, academic_year, semester, max_students) VALUES
('23CNTT_1', 'Lớp 23 CNTT_1', 'CNTT', 'gv01', '2024-2025', 1, 50),
('23CNTT_2', 'Lớp 23 CNTT_2', 'CNTT', 'gv01', '2024-2025', 1, 50)
ON CONFLICT (class_code) DO NOTHING;

-- Subjects
INSERT INTO subjects (subject_code, subject_name, credits, faculty_code, is_required) VALUES
('NMN', 'Nhập môn lập trình', 3, 'CNTT', TRUE)
ON CONFLICT (subject_code) DO NOTHING;

-- Courses
INSERT INTO courses 
(course_code, subject_code, teacher_username, class_code, academic_year, semester, schedule_day, schedule_time, room, max_students, registration_status) 
VALUES
('NMN_2425S1', 'NMN', 'gv01', NULL, '2024-2025', 1, 'Thứ 2, Thứ 4', 'Tiết 2-3 (08:00-10:00)', 'Room A101', 50, 'open')
ON CONFLICT (course_code) DO NOTHING;


-- ===============================================
-- TEST DATA: STUDENTS C01-C50
-- ===============================================

-- Lớp 23CNTT_1: C01-C25
INSERT INTO users (username, password, email, full_name, role) VALUES
('C01','password','C01@university.edu.vn','Sinh viên C01','student'),
('C02','password','C02@university.edu.vn','Sinh viên C02','student'),
('C03','password','C03@university.edu.vn','Sinh viên C03','student'),
('C04','password','C04@university.edu.vn','Sinh viên C04','student'),
('C05','password','C05@university.edu.vn','Sinh viên C05','student'),
('C06','password','C06@university.edu.vn','Sinh viên C06','student'),
('C07','password','C07@university.edu.vn','Sinh viên C07','student'),
('C08','password','C08@university.edu.vn','Sinh viên C08','student'),
('C09','password','C09@university.edu.vn','Sinh viên C09','student'),
('C10','password','C10@university.edu.vn','Sinh viên C10','student'),
('C11','password','C11@university.edu.vn','Sinh viên C11','student'),
('C12','password','C12@university.edu.vn','Sinh viên C12','student'),
('C13','password','C13@university.edu.vn','Sinh viên C13','student'),
('C14','password','C14@university.edu.vn','Sinh viên C14','student'),
('C15','password','C15@university.edu.vn','Sinh viên C15','student'),
('C16','password','C16@university.edu.vn','Sinh viên C16','student'),
('C17','password','C17@university.edu.vn','Sinh viên C17','student'),
('C18','password','C18@university.edu.vn','Sinh viên C18','student'),
('C19','password','C19@university.edu.vn','Sinh viên C19','student'),
('C20','password','C20@university.edu.vn','Sinh viên C20','student'),
('C21','password','C21@university.edu.vn','Sinh viên C21','student'),
('C22','password','C22@university.edu.vn','Sinh viên C22','student'),
('C23','password','C23@university.edu.vn','Sinh viên C23','student'),
('C24','password','C24@university.edu.vn','Sinh viên C24','student'),
('C25','password','C25@university.edu.vn','Sinh viên C25','student')
ON CONFLICT (username) DO NOTHING;

INSERT INTO students (username, student_code, class_code, faculty_code, admission_year, student_status) VALUES
('C01','C01','23CNTT_1','CNTT',2023,'active'),
('C02','C02','23CNTT_1','CNTT',2023,'active'),
('C03','C03','23CNTT_1','CNTT',2023,'active'),
('C04','C04','23CNTT_1','CNTT',2023,'active'),
('C05','C05','23CNTT_1','CNTT',2023,'active'),
('C06','C06','23CNTT_1','CNTT',2023,'active'),
('C07','C07','23CNTT_1','CNTT',2023,'active'),
('C08','C08','23CNTT_1','CNTT',2023,'active'),
('C09','C09','23CNTT_1','CNTT',2023,'active'),
('C10','C10','23CNTT_1','CNTT',2023,'active'),
('C11','C11','23CNTT_1','CNTT',2023,'active'),
('C12','C12','23CNTT_1','CNTT',2023,'active'),
('C13','C13','23CNTT_1','CNTT',2023,'active'),
('C14','C14','23CNTT_1','CNTT',2023,'active'),
('C15','C15','23CNTT_1','CNTT',2023,'active'),
('C16','C16','23CNTT_1','CNTT',2023,'active'),
('C17','C17','23CNTT_1','CNTT',2023,'active'),
('C18','C18','23CNTT_1','CNTT',2023,'active'),
('C19','C19','23CNTT_1','CNTT',2023,'active'),
('C20','C20','23CNTT_1','CNTT',2023,'active'),
('C21','C21','23CNTT_1','CNTT',2023,'active'),
('C22','C22','23CNTT_1','CNTT',2023,'active'),
('C23','C23','23CNTT_1','CNTT',2023,'active'),
('C24','C24','23CNTT_1','CNTT',2023,'active'),
('C25','C25','23CNTT_1','CNTT',2023,'active')
ON CONFLICT (student_code) DO NOTHING;

-- Lớp 23CNTT_2: C26-C50
INSERT INTO users (username, password, email, full_name, role) VALUES
('C26','password','C26@university.edu.vn','Sinh viên C26','student'),
('C27','password','C27@university.edu.vn','Sinh viên C27','student'),
('C28','password','C28@university.edu.vn','Sinh viên C28','student'),
('C29','password','C29@university.edu.vn','Sinh viên C29','student'),
('C30','password','C30@university.edu.vn','Sinh viên C30','student'),
('C31','password','C31@university.edu.vn','Sinh viên C31','student'),
('C32','password','C32@university.edu.vn','Sinh viên C32','student'),
('C33','password','C33@university.edu.vn','Sinh viên C33','student'),
('C34','password','C34@university.edu.vn','Sinh viên C34','student'),
('C35','password','C35@university.edu.vn','Sinh viên C35','student'),
('C36','password','C36@university.edu.vn','Sinh viên C36','student'),
('C37','password','C37@university.edu.vn','Sinh viên C37','student'),
('C38','password','C38@university.edu.vn','Sinh viên C38','student'),
('C39','password','C39@university.edu.vn','Sinh viên C39','student'),
('C40','password','C40@university.edu.vn','Sinh viên C40','student'),
('C41','password','C41@university.edu.vn','Sinh viên C41','student'),
('C42','password','C42@university.edu.vn','Sinh viên C42','student'),
('C43','password','C43@university.edu.vn','Sinh viên C43','student'),
('C44','password','C44@university.edu.vn','Sinh viên C44','student'),
('C45','password','C45@university.edu.vn','Sinh viên C45','student'),
('C46','password','C46@university.edu.vn','Sinh viên C46','student'),
('C47','password','C47@university.edu.vn','Sinh viên C47','student'),
('C48','password','C48@university.edu.vn','Sinh viên C48','student'),
('C49','password','C49@university.edu.vn','Sinh viên C49','student'),
('C50','password','C50@university.edu.vn','Sinh viên C50','student')
ON CONFLICT (username) DO NOTHING;

INSERT INTO students (username, student_code, class_code, faculty_code, admission_year, student_status) VALUES
('C26','C26','23CNTT_2','CNTT',2023,'active'),
('C27','C27','23CNTT_2','CNTT',2023,'active'),
('C28','C28','23CNTT_2','CNTT',2023,'active'),
('C29','C29','23CNTT_2','CNTT',2023,'active'),
('C30','C30','23CNTT_2','CNTT',2023,'active'),
('C31','C31','23CNTT_2','CNTT',2023,'active'),
('C32','C32','23CNTT_2','CNTT',2023,'active'),
('C33','C33','23CNTT_2','CNTT',2023,'active'),
('C34','C34','23CNTT_2','CNTT',2023,'active'),
('C35','C35','23CNTT_2','CNTT',2023,'active'),
('C36','C36','23CNTT_2','CNTT',2023,'active'),
('C37','C37','23CNTT_2','CNTT',2023,'active'),
('C38','C38','23CNTT_2','CNTT',2023,'active'),
('C39','C39','23CNTT_2','CNTT',2023,'active'),
('C40','C40','23CNTT_2','CNTT',2023,'active'),
('C41','C41','23CNTT_2','CNTT',2023,'active'),
('C42','C42','23CNTT_2','CNTT',2023,'active'),
('C43','C43','23CNTT_2','CNTT',2023,'active'),
('C44','C44','23CNTT_2','CNTT',2023,'active'),
('C45','C45','23CNTT_2','CNTT',2023,'active'),
('C46','C46','23CNTT_2','CNTT',2023,'active'),
('C47','C47','23CNTT_2','CNTT',2023,'active'),
('C48','C48','23CNTT_2','CNTT',2023,'active'),
('C49','C49','23CNTT_2','CNTT',2023,'active'),
('C50','C50','23CNTT_2','CNTT',2023,'active')
ON CONFLICT (student_code) DO NOTHING;

-- ===============================================
-- TEST DATA: COURSE REGISTRATIONS (A01-A50 + C01-C50)
-- ===============================================


-- Course registrations cho C01-C50
INSERT INTO course_registrations (student_code, course_code, registration_status) VALUES
('C01','NMN_2425S1','PENDING'),
('C02','NMN_2425S1','PENDING'),
('C03','NMN_2425S1','PENDING'),
('C04','NMN_2425S1','PENDING'),
('C05','NMN_2425S1','PENDING'),
('C06','NMN_2425S1','PENDING'),
('C07','NMN_2425S1','PENDING'),
('C08','NMN_2425S1','PENDING'),
('C09','NMN_2425S1','PENDING'),
('C10','NMN_2425S1','PENDING'),
('C11','NMN_2425S1','PENDING'),
('C12','NMN_2425S1','PENDING'),
('C13','NMN_2425S1','PENDING'),
('C14','NMN_2425S1','PENDING'),
('C15','NMN_2425S1','PENDING'),
('C16','NMN_2425S1','PENDING'),
('C17','NMN_2425S1','PENDING'),
('C18','NMN_2425S1','PENDING'),
('C19','NMN_2425S1','PENDING'),
('C20','NMN_2425S1','PENDING'),
('C21','NMN_2425S1','PENDING'),
('C22','NMN_2425S1','PENDING'),
('C23','NMN_2425S1','PENDING'),
('C24','NMN_2425S1','PENDING'),
('C25','NMN_2425S1','PENDING'),
('C26','NMN_2425S1','PENDING'),
('C27','NMN_2425S1','PENDING'),
('C28','NMN_2425S1','PENDING'),
('C29','NMN_2425S1','PENDING'),
('C30','NMN_2425S1','PENDING'),
('C31','NMN_2425S1','PENDING'),
('C32','NMN_2425S1','PENDING'),
('C33','NMN_2425S1','PENDING'),
('C34','NMN_2425S1','PENDING'),
('C35','NMN_2425S1','PENDING'),
('C36','NMN_2425S1','PENDING'),
('C37','NMN_2425S1','PENDING'),
('C38','NMN_2425S1','PENDING'),
('C39','NMN_2425S1','PENDING'),
('C40','NMN_2425S1','PENDING'),
('C41','NMN_2425S1','PENDING'),
('C42','NMN_2425S1','PENDING'),
('C43','NMN_2425S1','PENDING'),
('C44','NMN_2425S1','PENDING'),
('C45','NMN_2425S1','PENDING'),
('C46','NMN_2425S1','PENDING'),
('C47','NMN_2425S1','PENDING'),
('C48','NMN_2425S1','PENDING'),
('C49','NMN_2425S1','PENDING'),
('C50','NMN_2425S1','PENDING')
ON CONFLICT (student_code, course_code) DO NOTHING;

COMMIT;
