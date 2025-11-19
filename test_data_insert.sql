USE student_management_system;

-- ===============================================
-- Students + Course Registrations (50 sinh viên)
-- Lớp 23CNTT_1: A01-A25
-- Lớp 23CNTT_2: A26-A50
-- ===============================================
INSERT IGNORE INTO faculties (faculty_code, faculty_name, description) VALUES
('CNTT', 'Công nghệ thông tin', 'Khoa CNTT');

-- ===============================================
-- 2. Thêm giáo viên gv01
-- ===============================================
INSERT IGNORE INTO users (username, password, email, full_name, role, faculty_code) VALUES
('gv01', 'password', 'gv01@university.edu.vn', 'Giáo viên GV01', 'teacher', 'CNTT');

-- ===============================================
-- 3. Thêm lớp 23CNTT_1 và 23CNTT_2
-- ===============================================
INSERT IGNORE INTO classes (class_code, class_name, faculty_code, teacher_username, academic_year, semester, max_students) VALUES
('23CNTT_1', 'Lớp 23 CNTT_1', 'CNTT', 'gv01', '2024-2025', 1, 50),
('23CNTT_2', 'Lớp 23 CNTT_2', 'CNTT', 'gv01', '2024-2025', 1, 50);

-- ===============================================
-- 4. Thêm môn học NMN
-- ===============================================
INSERT IGNORE INTO subjects (subject_code, subject_name, credits, faculty_code, is_required) VALUES
('NMN', 'Nhập môn lập trình', 3, 'CNTT', TRUE);

-- ===============================================
-- 5. Thêm khóa học NMN_2425S1 do gv01 dạy
-- ===============================================
INSERT IGNORE INTO courses 
(course_code, subject_code, teacher_username, class_code, academic_year, semester, schedule_day, schedule_time, room, max_students, registration_status) 
VALUES
('NMN_2425S1', 'NMN', 'gv01', NULL, '2024-2025', 1, 'Thứ 2, Thứ 4', 'Tiết 2-3 (08:00-10:00)', 'Room A101', 50, 'open');
-- Lớp 23CNTT_1
INSERT IGNORE INTO users (username, password, email, full_name, role) VALUES
('A01','password','A01@university.edu.vn','Sinh viên A01','student'),
('A02','password','A02@university.edu.vn','Sinh viên A02','student'),
('A03','password','A03@university.edu.vn','Sinh viên A03','student'),
('A04','password','A04@university.edu.vn','Sinh viên A04','student'),
('A05','password','A05@university.edu.vn','Sinh viên A05','student'),
('A06','password','A06@university.edu.vn','Sinh viên A06','student'),
('A07','password','A07@university.edu.vn','Sinh viên A07','student'),
('A08','password','A08@university.edu.vn','Sinh viên A08','student'),
('A09','password','A09@university.edu.vn','Sinh viên A09','student'),
('A10','password','A10@university.edu.vn','Sinh viên A10','student'),
('A11','password','A11@university.edu.vn','Sinh viên A11','student'),
('A12','password','A12@university.edu.vn','Sinh viên A12','student'),
('A13','password','A13@university.edu.vn','Sinh viên A13','student'),
('A14','password','A14@university.edu.vn','Sinh viên A14','student'),
('A15','password','A15@university.edu.vn','Sinh viên A15','student'),
('A16','password','A16@university.edu.vn','Sinh viên A16','student'),
('A17','password','A17@university.edu.vn','Sinh viên A17','student'),
('A18','password','A18@university.edu.vn','Sinh viên A18','student'),
('A19','password','A19@university.edu.vn','Sinh viên A19','student'),
('A20','password','A20@university.edu.vn','Sinh viên A20','student'),
('A21','password','A21@university.edu.vn','Sinh viên A21','student'),
('A22','password','A22@university.edu.vn','Sinh viên A22','student'),
('A23','password','A23@university.edu.vn','Sinh viên A23','student'),
('A24','password','A24@university.edu.vn','Sinh viên A24','student'),
('A25','password','A25@university.edu.vn','Sinh viên A25','student');

INSERT IGNORE INTO students (username, student_code, class_code, faculty_code, admission_year, student_status) VALUES
('A01','A01','23CNTT_1','CNTT',2023,'active'),
('A02','A02','23CNTT_1','CNTT',2023,'active'),
('A03','A03','23CNTT_1','CNTT',2023,'active'),
('A04','A04','23CNTT_1','CNTT',2023,'active'),
('A05','A05','23CNTT_1','CNTT',2023,'active'),
('A06','A06','23CNTT_1','CNTT',2023,'active'),
('A07','A07','23CNTT_1','CNTT',2023,'active'),
('A08','A08','23CNTT_1','CNTT',2023,'active'),
('A09','A09','23CNTT_1','CNTT',2023,'active'),
('A10','A10','23CNTT_1','CNTT',2023,'active'),
('A11','A11','23CNTT_1','CNTT',2023,'active'),
('A12','A12','23CNTT_1','CNTT',2023,'active'),
('A13','A13','23CNTT_1','CNTT',2023,'active'),
('A14','A14','23CNTT_1','CNTT',2023,'active'),
('A15','A15','23CNTT_1','CNTT',2023,'active'),
('A16','A16','23CNTT_1','CNTT',2023,'active'),
('A17','A17','23CNTT_1','CNTT',2023,'active'),
('A18','A18','23CNTT_1','CNTT',2023,'active'),
('A19','A19','23CNTT_1','CNTT',2023,'active'),
('A20','A20','23CNTT_1','CNTT',2023,'active'),
('A21','A21','23CNTT_1','CNTT',2023,'active'),
('A22','A22','23CNTT_1','CNTT',2023,'active'),
('A23','A23','23CNTT_1','CNTT',2023,'active'),
('A24','A24','23CNTT_1','CNTT',2023,'active'),
('A25','A25','23CNTT_1','CNTT',2023,'active');

-- Lớp 23CNTT_2: A26-A50
INSERT IGNORE INTO users (username, password, email, full_name, role) VALUES
('A26','password','A26@university.edu.vn','Sinh viên A26','student'),
('A27','password','A27@university.edu.vn','Sinh viên A27','student'),
('A28','password','A28@university.edu.vn','Sinh viên A28','student'),
('A29','password','A29@university.edu.vn','Sinh viên A29','student'),
('A30','password','A30@university.edu.vn','Sinh viên A30','student'),
('A31','password','A31@university.edu.vn','Sinh viên A31','student'),
('A32','password','A32@university.edu.vn','Sinh viên A32','student'),
('A33','password','A33@university.edu.vn','Sinh viên A33','student'),
('A34','password','A34@university.edu.vn','Sinh viên A34','student'),
('A35','password','A35@university.edu.vn','Sinh viên A35','student'),
('A36','password','A36@university.edu.vn','Sinh viên A36','student'),
('A37','password','A37@university.edu.vn','Sinh viên A37','student'),
('A38','password','A38@university.edu.vn','Sinh viên A38','student'),
('A39','password','A39@university.edu.vn','Sinh viên A39','student'),
('A40','password','A40@university.edu.vn','Sinh viên A40','student'),
('A41','password','A41@university.edu.vn','Sinh viên A41','student'),
('A42','password','A42@university.edu.vn','Sinh viên A42','student'),
('A43','password','A43@university.edu.vn','Sinh viên A43','student'),
('A44','password','A44@university.edu.vn','Sinh viên A44','student'),
('A45','password','A45@university.edu.vn','Sinh viên A45','student'),
('A46','password','A46@university.edu.vn','Sinh viên A46','student'),
('A47','password','A47@university.edu.vn','Sinh viên A47','student'),
('A48','password','A48@university.edu.vn','Sinh viên A48','student'),
('A49','password','A49@university.edu.vn','Sinh viên A49','student'),
('A50','password','A50@university.edu.vn','Sinh viên A50','student');

INSERT IGNORE INTO students (username, student_code, class_code, faculty_code, admission_year, student_status) VALUES
('A26','A26','23CNTT_2','CNTT',2023,'active'),
('A27','A27','23CNTT_2','CNTT',2023,'active'),
('A28','A28','23CNTT_2','CNTT',2023,'active'),
('A29','A29','23CNTT_2','CNTT',2023,'active'),
('A30','A30','23CNTT_2','CNTT',2023,'active'),
('A31','A31','23CNTT_2','CNTT',2023,'active'),
('A32','A32','23CNTT_2','CNTT',2023,'active'),
('A33','A33','23CNTT_2','CNTT',2023,'active'),
('A34','A34','23CNTT_2','CNTT',2023,'active'),
('A35','A35','23CNTT_2','CNTT',2023,'active'),
('A36','A36','23CNTT_2','CNTT',2023,'active'),
('A37','A37','23CNTT_2','CNTT',2023,'active'),
('A38','A38','23CNTT_2','CNTT',2023,'active'),
('A39','A39','23CNTT_2','CNTT',2023,'active'),
('A40','A40','23CNTT_2','CNTT',2023,'active'),
('A41','A41','23CNTT_2','CNTT',2023,'active'),
('A42','A42','23CNTT_2','CNTT',2023,'active'),
('A43','A43','23CNTT_2','CNTT',2023,'active'),
('A44','A44','23CNTT_2','CNTT',2023,'active'),
('A45','A45','23CNTT_2','CNTT',2023,'active'),
('A46','A46','23CNTT_2','CNTT',2023,'active'),
('A47','A47','23CNTT_2','CNTT',2023,'active'),
('A48','A48','23CNTT_2','CNTT',2023,'active'),
('A49','A49','23CNTT_2','CNTT',2023,'active'),
('A50','A50','23CNTT_2','CNTT',2023,'active');

-- Course registrations (status = PENDING)
INSERT IGNORE INTO course_registrations (student_code, course_code, registration_status) VALUES
('A01','NMN_2425S1','PENDING'),
('A02','NMN_2425S1','PENDING'),
('A03','NMN_2425S1','PENDING'),
('A04','NMN_2425S1','PENDING'),
('A05','NMN_2425S1','PENDING'),
('A06','NMN_2425S1','PENDING'),
('A07','NMN_2425S1','PENDING'),
('A08','NMN_2425S1','PENDING'),
('A09','NMN_2425S1','PENDING'),
('A10','NMN_2425S1','PENDING'),
('A11','NMN_2425S1','PENDING'),
('A12','NMN_2425S1','PENDING'),
('A13','NMN_2425S1','PENDING'),
('A14','NMN_2425S1','PENDING'),
('A15','NMN_2425S1','PENDING'),
('A16','NMN_2425S1','PENDING'),
('A17','NMN_2425S1','PENDING'),
('A18','NMN_2425S1','PENDING'),
('A19','NMN_2425S1','PENDING'),
('A20','NMN_2425S1','PENDING'),
('A21','NMN_2425S1','PENDING'),
('A22','NMN_2425S1','PENDING'),
('A23','NMN_2425S1','PENDING'),
('A24','NMN_2425S1','PENDING'),
('A25','NMN_2425S1','PENDING'),
('A26','NMN_2425S1','PENDING'),
('A27','NMN_2425S1','PENDING'),
('A28','NMN_2425S1','PENDING'),
('A29','NMN_2425S1','PENDING'),
('A30','NMN_2425S1','PENDING'),
('A31','NMN_2425S1','PENDING'),
('A32','NMN_2425S1','PENDING'),
('A33','NMN_2425S1','PENDING'),
('A34','NMN_2425S1','PENDING'),
('A35','NMN_2425S1','PENDING'),
('A36','NMN_2425S1','PENDING'),
('A37','NMN_2425S1','PENDING'),
('A38','NMN_2425S1','PENDING'),
('A39','NMN_2425S1','PENDING'),
('A40','NMN_2425S1','PENDING'),
('A41','NMN_2425S1','PENDING'),
('A42','NMN_2425S1','PENDING'),
('A43','NMN_2425S1','PENDING'),
('A44','NMN_2425S1','PENDING'),
('A45','NMN_2425S1','PENDING'),
('A46','NMN_2425S1','PENDING'),
('A47','NMN_2425S1','PENDING'),
('A48','NMN_2425S1','PENDING'),
('A49','NMN_2425S1','PENDING'),
('A50','NMN_2425S1','PENDING');
