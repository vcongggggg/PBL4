-- ===============================================
-- 1. Thêm giáo viên gv03
-- ===============================================
INSERT INTO faculties (faculty_code, faculty_name) VALUES
('CNTT', 'Công nghệ thông tin')
ON CONFLICT (faculty_code) DO NOTHING;

INSERT INTO users (username, password, email, full_name, role, faculty_code) VALUES
('gv03', 'password', 'gv03@university.edu.vn', 'Giáo viên GV03', 'teacher', 'CNTT')
ON CONFLICT (username) DO NOTHING;

-- ===============================================
-- 2. Thêm môn học mới: LTW (Lập trình web cơ bản)
-- ===============================================
INSERT INTO subjects (subject_code, subject_name, credits, faculty_code, is_required) VALUES
('LTM', 'Lập trình mạng', 3, 'CNTT', TRUE)
ON CONFLICT (subject_code) DO NOTHING;

-- ===============================================
-- 3. Thêm khóa học mới: LTM-2425S1-01 do gv03 dạy
-- ===============================================
INSERT INTO courses 
(course_code, subject_code, teacher_username, class_code, academic_year, semester, schedule_day, schedule_time, room, max_students, registration_status) 
VALUES
('LTM-2425S1-01', 'LTM', 'gv03', NULL, '2024-2025', 1, 'Thứ 3, Thứ 5', 'Tiết 4-5 (10:00-12:00)', 'Room B201', 50, 'open')
ON CONFLICT (course_code) DO NOTHING;

-- ===============================================
-- 4. Thêm 50 sinh viên mới C51-C100
-- ===============================================
INSERT INTO users (username, password, email, full_name, role) VALUES
('C51','password','C51@university.edu.vn','Sinh viên C51','student'),
('C52','password','C52@university.edu.vn','Sinh viên C52','student'),
('C53','password','C53@university.edu.vn','Sinh viên C53','student'),
('C54','password','C54@university.edu.vn','Sinh viên C54','student'),
('C55','password','C55@university.edu.vn','Sinh viên C55','student'),
('C56','password','C56@university.edu.vn','Sinh viên C56','student'),
('C57','password','C57@university.edu.vn','Sinh viên C57','student'),
('C58','password','C58@university.edu.vn','Sinh viên C58','student'),
('C59','password','C59@university.edu.vn','Sinh viên C59','student'),
('C60','password','C60@university.edu.vn','Sinh viên C60','student'),
('C61','password','C61@university.edu.vn','Sinh viên C61','student'),
('C62','password','C62@university.edu.vn','Sinh viên C62','student'),
('C63','password','C63@university.edu.vn','Sinh viên C63','student'),
('C64','password','C64@university.edu.vn','Sinh viên C64','student'),
('C65','password','C65@university.edu.vn','Sinh viên C65','student'),
('C66','password','C66@university.edu.vn','Sinh viên C66','student'),
('C67','password','C67@university.edu.vn','Sinh viên C67','student'),
('C68','password','C68@university.edu.vn','Sinh viên C68','student'),
('C69','password','C69@university.edu.vn','Sinh viên C69','student'),
('C70','password','C70@university.edu.vn','Sinh viên C70','student'),
('C71','password','C71@university.edu.vn','Sinh viên C71','student'),
('C72','password','C72@university.edu.vn','Sinh viên C72','student'),
('C73','password','C73@university.edu.vn','Sinh viên C73','student'),
('C74','password','C74@university.edu.vn','Sinh viên C74','student'),
('C75','password','C75@university.edu.vn','Sinh viên C75','student'),
('C76','password','C76@university.edu.vn','Sinh viên C76','student'),
('C77','password','C77@university.edu.vn','Sinh viên C77','student'),
('C78','password','C78@university.edu.vn','Sinh viên C78','student'),
('C79','password','C79@university.edu.vn','Sinh viên C79','student'),
('C80','password','C80@university.edu.vn','Sinh viên C80','student'),
('C81','password','C81@university.edu.vn','Sinh viên C81','student'),
('C82','password','C82@university.edu.vn','Sinh viên C82','student'),
('C83','password','C83@university.edu.vn','Sinh viên C83','student'),
('C84','password','C84@university.edu.vn','Sinh viên C84','student'),
('C85','password','C85@university.edu.vn','Sinh viên C85','student'),
('C86','password','C86@university.edu.vn','Sinh viên C86','student'),
('C87','password','C87@university.edu.vn','Sinh viên C87','student'),
('C88','password','C88@university.edu.vn','Sinh viên C88','student'),
('C89','password','C89@university.edu.vn','Sinh viên C89','student'),
('C90','password','C90@university.edu.vn','Sinh viên C90','student'),
('C91','password','C91@university.edu.vn','Sinh viên C91','student'),
('C92','password','C92@university.edu.vn','Sinh viên C92','student'),
('C93','password','C93@university.edu.vn','Sinh viên C93','student'),
('C94','password','C94@university.edu.vn','Sinh viên C94','student'),
('C95','password','C95@university.edu.vn','Sinh viên C95','student'),
('C96','password','C96@university.edu.vn','Sinh viên C96','student'),
('C97','password','C97@university.edu.vn','Sinh viên C97','student'),
('C98','password','C98@university.edu.vn','Sinh viên C98','student'),
('C99','password','C99@university.edu.vn','Sinh viên C99','student'),
('C100','password','C100@university.edu.vn','Sinh viên C100','student')
ON CONFLICT (username) DO NOTHING;

INSERT INTO students (username, student_code, faculty_code, admission_year, student_status) VALUES
('C51','C51','CNTT',2023,'active'),
('C52','C52','CNTT',2023,'active'),
('C53','C53','CNTT',2023,'active'),
('C54','C54','CNTT',2023,'active'),
('C55','C55','CNTT',2023,'active'),
('C56','C56','CNTT',2023,'active'),
('C57','C57','CNTT',2023,'active'),
('C58','C58','CNTT',2023,'active'),
('C59','C59','CNTT',2023,'active'),
('C60','C60','CNTT',2023,'active'),
('C61','C61','CNTT',2023,'active'),
('C62','C62','CNTT',2023,'active'),
('C63','C63','CNTT',2023,'active'),
('C64','C64','CNTT',2023,'active'),
('C65','C65','CNTT',2023,'active'),
('C66','C66','CNTT',2023,'active'),
('C67','C67','CNTT',2023,'active'),
('C68','C68','CNTT',2023,'active'),
('C69','C69','CNTT',2023,'active'),
('C70','C70','CNTT',2023,'active'),
('C71','C71','CNTT',2023,'active'),
('C72','C72','CNTT',2023,'active'),
('C73','C73','CNTT',2023,'active'),
('C74','C74','CNTT',2023,'active'),
('C75','C75','CNTT',2023,'active'),
('C76','C76','CNTT',2023,'active'),
('C77','C77','CNTT',2023,'active'),
('C78','C78','CNTT',2023,'active'),
('C79','C79','CNTT',2023,'active'),
('C80','C80','CNTT',2023,'active'),
('C81','C81','CNTT',2023,'active'),
('C82','C82','CNTT',2023,'active'),
('C83','C83','CNTT',2023,'active'),
('C84','C84','CNTT',2023,'active'),
('C85','C85','CNTT',2023,'active'),
('C86','C86','CNTT',2023,'active'),
('C87','C87','CNTT',2023,'active'),
('C88','C88','CNTT',2023,'active'),
('C89','C89','CNTT',2023,'active'),
('C90','C90','CNTT',2023,'active'),
('C91','C91','CNTT',2023,'active'),
('C92','C92','CNTT',2023,'active'),
('C93','C93','CNTT',2023,'active'),
('C94','C94','CNTT',2023,'active'),
('C95','C95','CNTT',2023,'active'),
('C96','C96','CNTT',2023,'active'),
('C97','C97','CNTT',2023,'active'),
('C98','C98','CNTT',2023,'active'),
('C99','C99','CNTT',2023,'active'),
('C100','C100','CNTT',2023,'active')
ON CONFLICT (student_code) DO NOTHING;

-- ===============================================
-- 5. Course registrations for LTM-2425S1-01 (status = PENDING)
-- C51 - C100
-- ===============================================
INSERT INTO course_registrations (student_code, course_code, registration_status) VALUES
('C51','LTM-2425S1-01','PENDING'),
('C52','LTM-2425S1-01','PENDING'),
('C53','LTM-2425S1-01','PENDING'),
('C54','LTM-2425S1-01','PENDING'),
('C55','LTM-2425S1-01','PENDING'),
('C56','LTM-2425S1-01','PENDING'),
('C57','LTM-2425S1-01','PENDING'),
('C58','LTM-2425S1-01','PENDING'),
('C59','LTM-2425S1-01','PENDING'),
('C60','LTM-2425S1-01','PENDING'),
('C61','LTM-2425S1-01','PENDING'),
('C62','LTM-2425S1-01','PENDING'),
('C63','LTM-2425S1-01','PENDING'),
('C64','LTM-2425S1-01','PENDING'),
('C65','LTM-2425S1-01','PENDING'),
('C66','LTM-2425S1-01','PENDING'),
('C67','LTM-2425S1-01','PENDING'),
('C68','LTM-2425S1-01','PENDING'),
('C69','LTM-2425S1-01','PENDING'),
('C70','LTM-2425S1-01','PENDING'),
('C71','LTM-2425S1-01','PENDING'),
('C72','LTM-2425S1-01','PENDING'),
('C73','LTM-2425S1-01','PENDING'),
('C74','LTM-2425S1-01','PENDING'),
('C75','LTM-2425S1-01','PENDING'),
('C76','LTM-2425S1-01','PENDING'),
('C77','LTM-2425S1-01','PENDING'),
('C78','LTM-2425S1-01','PENDING'),
('C79','LTM-2425S1-01','PENDING'),
('C80','LTM-2425S1-01','PENDING'),
('C81','LTM-2425S1-01','PENDING'),
('C82','LTM-2425S1-01','PENDING'),
('C83','LTM-2425S1-01','PENDING'),
('C84','LTM-2425S1-01','PENDING'),
('C85','LTM-2425S1-01','PENDING'),
('C86','LTM-2425S1-01','PENDING'),
('C87','LTM-2425S1-01','PENDING'),
('C88','LTM-2425S1-01','PENDING'),
('C89','LTM-2425S1-01','PENDING'),
('C90','LTM-2425S1-01','PENDING'),
('C91','LTM-2425S1-01','PENDING'),
('C92','LTM-2425S1-01','PENDING'),
('C93','LTM-2425S1-01','PENDING'),
('C94','LTM-2425S1-01','PENDING'),
('C95','LTM-2425S1-01','PENDING'),
('C96','LTM-2425S1-01','PENDING'),
('C97','LTM-2425S1-01','PENDING'),
('C98','LTM-2425S1-01','PENDING'),
('C99','LTM-2425S1-01','PENDING'),
('C100','LTM-2425S1-01','PENDING')
ON CONFLICT (student_code, course_code) DO NOTHING;

-- ===============================================
