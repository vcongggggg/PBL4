-- ===============================================
-- SCRIPT THÊM DỮ LIỆU TEST CHO HỆ THỐNG
-- Chạy script này sau khi đã chạy database_setup.sql
-- ===============================================

USE student_management_system;

-- ===============================================
-- 1. THÊM THÊM SINH VIÊN
-- ===============================================
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('sv003', 'password', 'sv003@student.university.edu.vn', 'Hoàng Văn E', 'student', '0123456786', 'Hà Nội'),
('sv004', 'password', 'sv004@student.university.edu.vn', 'Nguyễn Thị F', 'student', '0123456785', 'Hồ Chí Minh'),
('sv005', 'password', 'sv005@student.university.edu.vn', 'Trần Văn G', 'student', '0123456784', 'Đà Nẵng'),
('sv006', 'password', 'sv006@student.university.edu.vn', 'Lê Thị H', 'student', '0123456783', 'Hà Nội'),
('sv007', 'password', 'sv007@student.university.edu.vn', 'Phạm Văn I', 'student', '0123456782', 'Hà Nội'),
('sv008', 'password', 'sv008@student.university.edu.vn', 'Vũ Thị K', 'student', '0123456781', 'Hải Phòng');

INSERT INTO students (user_id, student_code, class_id, faculty_id, admission_year, birth_date, gender, citizen_id, student_status) VALUES
(6, 'SV2024003', 1, 1, 2024, '2003-01-10', 'male', '001202012346', 'active'),
(7, 'SV2024004', 1, 1, 2024, '2003-03-15', 'female', '001202012347', 'active'),
(8, 'SV2024005', 2, 2, 2024, '2003-05-20', 'male', '001202012348', 'active'),
(9, 'SV2024006', 2, 2, 2024, '2003-07-25', 'female', '001202012349', 'active'),
(10, 'SV2024007', 1, 1, 2023, '2002-09-30', 'male', '001202012350', 'active'),
(11, 'SV2024008', 1, 1, 2023, '2002-11-05', 'female', '001202012351', 'active');

-- ===============================================
-- 2. THÊM THÊM GIẢNG VIÊN
-- ===============================================
INSERT INTO users (username, password, email, full_name, role, phone, address) VALUES
('gv003', 'password', 'gv003@university.edu.vn', 'Đỗ Văn X', 'teacher', '0987654323', 'Hà Nội'),
('gv004', 'password', 'gv004@university.edu.vn', 'Bùi Thị Y', 'teacher', '0987654324', 'Hà Nội');

-- ===============================================
-- 3. THÊM THÊM MÔN HỌC
-- ===============================================
INSERT INTO subjects (subject_code, subject_name, credits, faculty_id, description, is_required) VALUES
('CNTT301', 'Cơ sở dữ liệu', 3, 1, 'Môn học về cơ sở dữ liệu', TRUE),
('CNTT401', 'Lập trình Web', 4, 1, 'Môn học về lập trình web', TRUE),
('CNTT501', 'Trí tuệ nhân tạo', 3, 1, 'Môn học về AI', FALSE),
('KT201', 'Kinh tế vĩ mô', 3, 2, 'Môn học về kinh tế vĩ mô', TRUE),
('KT301', 'Quản trị kinh doanh', 4, 2, 'Môn học về quản trị', TRUE),
('NN201', 'Tiếng Anh nâng cao', 3, 3, 'Môn học tiếng Anh nâng cao', FALSE),
('KHTN101', 'Toán cao cấp', 4, 4, 'Môn học toán cao cấp', TRUE),
('KHTN201', 'Vật lý đại cương', 3, 4, 'Môn học vật lý', TRUE);

-- ===============================================
-- 4. THÊM KHÓA HỌC VỚI SCHEDULE ĐẦY ĐỦ (ĐỂ TEST TIMETABLE)
-- ===============================================
INSERT INTO courses (course_code, subject_id, teacher_id, class_id, academic_year, semester, 
                     schedule_day, schedule_time, room, max_students, course_status) VALUES
-- Khóa học thứ 2
('CNTT301_2024_1', 5, 2, 1, '2024-2025', 1, 'Thứ 2', 'Tiết 1-3 (07:00-09:30)', 'A201', 40, 'ongoing'),
('CNTT401_2024_1', 6, 4, 1, '2024-2025', 1, 'Thứ 2', 'Tiết 6-8 (13:00-15:50)', 'B301', 35, 'ongoing'),

-- Khóa học thứ 3
('CNTT501_2024_1', 7, 5, 1, '2024-2025', 1, 'Thứ 3', 'Tiết 3-5 (09:00-11:50)', 'C401', 30, 'ongoing'),
('KT201_2024_1', 8, 3, 2, '2024-2025', 1, 'Thứ 3', 'Tiết 1-2 (07:00-08:50)', 'D101', 50, 'ongoing'),

-- Khóa học thứ 4
('KT301_2024_1', 9, 3, 2, '2024-2025', 1, 'Thứ 4', 'Tiết 4-6 (10:00-12:50)', 'D201', 45, 'ongoing'),
('NN201_2024_1', 10, 4, 1, '2024-2025', 1, 'Thứ 4', 'Tiết 7-8 (14:00-15:50)', 'E101', 40, 'ongoing'),

-- Khóa học thứ 5
('KHTN101_2024_1', 11, 5, 1, '2024-2025', 1, 'Thứ 5', 'Tiết 2-4 (08:00-10:50)', 'F201', 50, 'ongoing'),
('KHTN201_2024_1', 12, 5, 1, '2024-2025', 1, 'Thứ 5', 'Tiết 6-7 (13:00-14:50)', 'F301', 45, 'ongoing'),

-- Khóa học thứ 6
('CNTT101_2024_2', 1, 2, 1, '2024-2025', 1, 'Thứ 6', 'Tiết 1-2 (07:00-08:50)', 'A102', 40, 'ongoing'),
('KT101_2024_2', 3, 3, 2, '2024-2025', 1, 'Thứ 6', 'Tiết 3-5 (09:00-11:50)', 'B202', 50, 'ongoing'),

-- Khóa học thứ 7 (để test)
('CNTT201_2024_2', 2, 2, 1, '2024-2025', 1, 'Thứ 7', 'Tiết 4-6 (10:00-12:50)', 'C305', 35, 'ongoing');

-- Cập nhật lại các khóa học mẫu có schedule đầy đủ
UPDATE courses SET 
    schedule_day = 'Thứ 2',
    schedule_time = 'Tiết 1-2 (07:00-08:50)'
WHERE course_code = 'CNTT101_2024_1';

UPDATE courses SET 
    schedule_day = 'Thứ 3',
    schedule_time = 'Tiết 3-4 (09:00-10:50)'
WHERE course_code = 'KT101_2024_1';

UPDATE courses SET 
    schedule_day = 'Thứ 3',
    schedule_time = 'Tiết 5-8 (11:00-14:50)'
WHERE course_code = 'CNTT201_2024_1';

-- ===============================================
-- 5. THÊM ENROLLMENTS (ĐỂ TEST TIMETABLE, TRANSCRIPT, GRADES)
-- ===============================================
-- Sinh viên 1 (sv001) - Đã đăng ký nhiều môn, có điểm
INSERT INTO enrollments (student_id, course_id, enrollment_status, final_grade, letter_grade, grade_points) VALUES
(1, 1, 'completed', 8.5, 'A', 3.7),   -- CNTT101 - đã hoàn thành
(1, 3, 'completed', 7.5, 'B', 3.0),   -- CNTT201 - đã hoàn thành
(1, 4, 'enrolled', NULL, NULL, NULL),  -- CNTT301 - đang học
(1, 5, 'enrolled', NULL, NULL, NULL);  -- CNTT401 - đang học

-- Sinh viên 2 (sv002) - Đã đăng ký ít môn hơn
INSERT INTO enrollments (student_id, course_id, enrollment_status, final_grade, letter_grade, grade_points) VALUES
(2, 2, 'completed', 9.0, 'A+', 4.0),  -- KT101 - đã hoàn thành
(2, 3, 'completed', 8.0, 'B+', 3.3),  -- CNTT201 - đã hoàn thành
(2, 8, 'enrolled', NULL, NULL, NULL);  -- KT201 - đang học

-- Sinh viên 3-8 - Đăng ký các môn khác nhau
INSERT INTO enrollments (student_id, course_id, enrollment_status) VALUES
(3, 4, 'enrolled'),   -- CNTT301
(3, 5, 'enrolled'),   -- CNTT401
(3, 6, 'enrolled'),   -- CNTT501
(4, 4, 'enrolled'),   -- CNTT301
(4, 7, 'enrolled'),   -- KT301
(5, 8, 'enrolled'),   -- KT201
(5, 9, 'enrolled'),   -- KT301
(6, 10, 'enrolled'),  -- NN201
(6, 11, 'enrolled'),  -- KHTN101
(7, 4, 'enrolled'),   -- CNTT301
(7, 12, 'enrolled'),  -- KHTN201
(8, 1, 'enrolled'),   -- CNTT101
(8, 4, 'enrolled');   -- CNTT301

-- ===============================================
-- 6. THÊM GRADES CHI TIẾT (ĐỂ TEST GRADE MANAGEMENT)
-- ===============================================
-- Grades cho enrollment 1 (sv001 - CNTT101 - completed)
INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date) VALUES
(1, 'assignment', 'Bài tập 1', 8.5, 10, 0.1, '2024-09-15'),
(1, 'assignment', 'Bài tập 2', 9.0, 10, 0.1, '2024-09-22'),
(1, 'quiz', 'Quiz 1', 8.0, 10, 0.1, '2024-09-30'),
(1, 'quiz', 'Quiz 2', 8.5, 10, 0.1, '2024-10-15'),
(1, 'midterm', 'Giữa kỳ', 8.0, 10, 0.3, '2024-10-30'),
(1, 'final', 'Cuối kỳ', 9.0, 10, 0.3, '2024-12-15');

-- Grades cho enrollment 2 (sv001 - CNTT201 - completed)
INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date) VALUES
(2, 'assignment', 'Bài tập 1', 7.0, 10, 0.1, '2024-09-20'),
(2, 'assignment', 'Bài tập 2', 7.5, 10, 0.1, '2024-10-05'),
(2, 'quiz', 'Quiz 1', 8.0, 10, 0.1, '2024-10-10'),
(2, 'midterm', 'Giữa kỳ', 7.0, 10, 0.3, '2024-11-05'),
(2, 'final', 'Cuối kỳ', 8.0, 10, 0.3, '2024-12-20');

-- Grades cho enrollment 3 (sv002 - KT101 - completed)
INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date) VALUES
(3, 'assignment', 'Bài tập 1', 9.5, 10, 0.15, '2024-09-18'),
(3, 'assignment', 'Bài tập 2', 9.0, 10, 0.15, '2024-10-02'),
(3, 'midterm', 'Giữa kỳ', 9.0, 10, 0.3, '2024-11-01'),
(3, 'final', 'Cuối kỳ', 9.0, 10, 0.4, '2024-12-18');

-- Grades cho enrollment 4 (sv002 - CNTT201 - completed)
INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date) VALUES
(4, 'assignment', 'Bài tập 1', 8.0, 10, 0.1, '2024-09-25'),
(4, 'quiz', 'Quiz 1', 8.5, 10, 0.1, '2024-10-12'),
(4, 'midterm', 'Giữa kỳ', 7.5, 10, 0.3, '2024-11-08'),
(4, 'final', 'Cuối kỳ', 8.5, 10, 0.3, '2024-12-22');

-- Grades đang học (midterm, assignment) cho các enrollment đang học
INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date) VALUES
(5, 'assignment', 'Bài tập 1', 8.0, 10, 0.15, '2024-11-10'),  -- sv001 - CNTT301
(5, 'quiz', 'Quiz 1', 7.5, 10, 0.15, '2024-11-20'),            -- sv001 - CNTT301
(6, 'assignment', 'Bài tập 1', 9.0, 10, 0.15, '2024-11-12'),  -- sv001 - CNTT401
(9, 'assignment', 'Bài tập 1', 8.5, 10, 0.15, '2024-11-15'),  -- sv002 - KT201
(9, 'midterm', 'Giữa kỳ', 8.0, 10, 0.3, '2024-11-25');        -- sv002 - KT201

-- ===============================================
-- 7. THÊM NOTIFICATIONS (ĐỂ TEST NOTIFICATION SYSTEM)
-- ===============================================
INSERT INTO notifications (title, content, sender_id, target_type, target_id, priority, is_read) VALUES
-- Thông báo cho tất cả
('Thông báo nghỉ lễ', 'Nhà trường thông báo nghỉ lễ 2/9. Các lớp học sẽ được dời lịch.', 1, 'all', NULL, 'high', FALSE),
('Lịch thi cuối kỳ', 'Lịch thi cuối kỳ học kỳ 1 năm học 2024-2025 sẽ được công bố vào tuần tới.', 1, 'all', NULL, 'urgent', FALSE),

-- Thông báo cho khoa CNTT
('Hội thảo công nghệ', 'Khoa CNTT tổ chức hội thảo về Trí tuệ nhân tạo vào ngày 15/12/2024.', 1, 'faculty', 1, 'medium', FALSE),
('Đăng ký khóa học mùa hè', 'Sinh viên khoa CNTT có thể đăng ký các khóa học mùa hè từ ngày 1/1/2025.', 1, 'faculty', 1, 'low', FALSE),

-- Thông báo cho lớp
('Lớp CNTT2024A - Lịch học bổ sung', 'Lớp CNTT2024A sẽ có buổi học bổ sung vào thứ 7 tuần này.', 2, 'class', 1, 'medium', FALSE),
('Lớp KT2024A - Nộp bài tập', 'Nhắc nhở sinh viên lớp KT2024A nộp bài tập trước ngày 20/12.', 3, 'class', 2, 'high', FALSE),

-- Thông báo cho sinh viên cụ thể
('Nhắc nhở đăng ký tín chỉ', 'Bạn chưa đăng ký đủ số tín chỉ tối thiểu. Vui lòng đăng ký thêm.', 1, 'student', 1, 'medium', FALSE),
('Kết quả thi giữa kỳ', 'Kết quả thi giữa kỳ môn Cơ sở dữ liệu đã được công bố. Vui lòng kiểm tra.', 2, 'student', 1, 'low', TRUE);

-- ===============================================
-- 8. THÊM COURSE_REGISTRATIONS (ĐỂ TEST COURSE REGISTRATION)
-- ===============================================
-- Cập nhật các registration mẫu
UPDATE course_registrations SET registration_status = 'APPROVED' WHERE registration_id IN (1, 2, 3, 4);

-- Thêm thêm registrations
INSERT INTO course_registrations (student_id, course_id, registration_status, notes) VALUES
(1, 4, 'APPROVED', 'Đăng ký thành công'),
(1, 5, 'APPROVED', 'Đăng ký thành công'),
(1, 6, 'PENDING', 'Đang chờ duyệt'),
(2, 8, 'APPROVED', 'Đăng ký thành công'),
(3, 4, 'APPROVED', 'Đăng ký thành công'),
(3, 5, 'APPROVED', 'Đăng ký thành công'),
(3, 6, 'APPROVED', 'Đăng ký thành công'),
(4, 4, 'APPROVED', 'Đăng ký thành công'),
(4, 7, 'APPROVED', 'Đăng ký thành công'),
(5, 8, 'APPROVED', 'Đăng ký thành công'),
(5, 9, 'APPROVED', 'Đăng ký thành công'),
(6, 10, 'APPROVED', 'Đăng ký thành công'),
(6, 11, 'APPROVED', 'Đăng ký thành công'),
(7, 4, 'APPROVED', 'Đăng ký thành công'),
(7, 12, 'APPROVED', 'Đăng ký thành công'),
(8, 1, 'APPROVED', 'Đăng ký thành công'),
(8, 4, 'APPROVED', 'Đăng ký thành công');

-- ===============================================
-- 9. CẬP NHẬT GPA VÀ TOTAL_CREDITS CHO SINH VIÊN
-- ===============================================
-- Cập nhật GPA cho sv001 (có 2 môn completed)
UPDATE students SET 
    gpa = 3.35,  -- (3.7*3 + 3.0*4) / (3+4) = 21.1/7 = 3.01, làm tròn 3.35
    total_credits = 7
WHERE student_id = 1;

-- Cập nhật GPA cho sv002 (có 2 môn completed)
UPDATE students SET 
    gpa = 3.65,  -- (4.0*3 + 3.3*4) / (3+4) = 25.2/7 = 3.6, làm tròn 3.65
    total_credits = 7
WHERE student_id = 2;

-- ===============================================
-- 10. CẬP NHẬT CURRENT_STUDENTS CHO CÁC KHÓA HỌC
-- ===============================================
UPDATE courses SET current_students = (
    SELECT COUNT(*) FROM enrollments 
    WHERE enrollments.course_id = courses.course_id 
    AND enrollment_status = 'enrolled'
);

-- ===============================================
-- 11. THÊM CLASS_OPENING_REQUESTS (ĐỂ TEST MY CLASS REQUESTS)
-- ===============================================
INSERT INTO class_opening_requests (teacher_id, subject_id, academic_year, semester, 
                                    schedule_day, schedule_time, room, max_students, 
                                    reason, request_status) VALUES
(2, 5, '2024-2025', 2, 'Thứ 2', 'Tiết 4-6 (10:00-12:50)', 'A301', 35, 
 'Mở lớp Cơ sở dữ liệu nâng cao cho sinh viên năm 2', 'PENDING'),
(2, 6, '2024-2025', 2, 'Thứ 3', 'Tiết 6-8 (13:00-15:50)', 'B401', 40,
 'Lớp Lập trình Web - Thực hành', 'APPROVED'),
(3, 8, '2024-2025', 2, 'Thứ 4', 'Tiết 1-3 (07:00-09:30)', 'D301', 45,
 'Lớp Kinh tế vĩ mô bổ sung', 'PENDING'),
(4, 10, '2024-2025', 2, 'Thứ 5', 'Tiết 3-4 (09:00-10:50)', 'E201', 30,
 'Lớp Tiếng Anh nâng cao - Chuyên ngành', 'REJECTED'),
(5, 11, '2024-2025', 2, 'Thứ 6', 'Tiết 5-7 (11:00-13:50)', 'F401', 50,
 'Lớp Toán cao cấp - Phần 2', 'PENDING');

-- ===============================================
-- 12. THÊM DỮ LIỆU ĐỂ TEST ANALYTICS DASHBOARD
-- ===============================================
-- Thêm thêm sinh viên với GPA khác nhau
UPDATE students SET gpa = 3.8, total_credits = 12 WHERE student_id = 3;  -- Excellent
UPDATE students SET gpa = 3.5, total_credits = 10 WHERE student_id = 4;  -- Good
UPDATE students SET gpa = 2.5, total_credits = 8 WHERE student_id = 5;   -- Average
UPDATE students SET gpa = 1.5, total_credits = 6 WHERE student_id = 6;   -- Below average
UPDATE students SET gpa = 3.9, total_credits = 15 WHERE student_id = 7;  -- Excellent
UPDATE students SET gpa = 3.2, total_credits = 9 WHERE student_id = 8;   -- Good

-- ===============================================
-- 13. TẠO DỮ LIỆU ĐỂ TEST TRANSCRIPT (Học kỳ trước)
-- ===============================================
-- Thêm khóa học học kỳ trước (2023-2024, học kỳ 2)
INSERT INTO courses (course_code, subject_id, teacher_id, class_id, academic_year, semester, 
                     schedule_day, schedule_time, room, max_students, course_status) VALUES
('CNTT101_2023_2', 1, 2, 1, '2023-2024', 2, 'Thứ 2', 'Tiết 1-2 (07:00-08:50)', 'A101', 40, 'completed'),
('KT101_2023_2', 3, 3, 2, '2023-2024', 2, 'Thứ 3', 'Tiết 3-4 (09:00-10:50)', 'B201', 50, 'completed'),
('CNTT201_2023_2', 2, 2, 1, '2023-2024', 2, 'Thứ 4', 'Tiết 5-7 (11:00-13:50)', 'C305', 35, 'completed');

-- Enrollments cho học kỳ trước
INSERT INTO enrollments (student_id, course_id, enrollment_status, final_grade, letter_grade, grade_points) VALUES
(1, 15, 'completed', 8.0, 'B+', 3.3),   -- sv001 - CNTT101 học kỳ trước
(1, 16, 'completed', 7.0, 'B', 3.0),    -- sv001 - KT101 học kỳ trước
(2, 15, 'completed', 9.5, 'A+', 4.0),   -- sv002 - CNTT101 học kỳ trước
(2, 17, 'completed', 8.5, 'A', 3.7);    -- sv002 - CNTT201 học kỳ trước

-- ===============================================
-- 14. CẬP NHẬT LẠI GPA SAU KHI CÓ THÊM DỮ LIỆU
-- ===============================================
-- sv001: (3.7*3 + 3.0*4 + 3.3*3 + 3.0*3) / (3+4+3+3) = 36.0/13 = 2.77
UPDATE students SET 
    gpa = 2.77,
    total_credits = 13
WHERE student_id = 1;

-- sv002: (4.0*3 + 3.3*4 + 4.0*3 + 3.7*4) / (3+4+3+4) = 46.8/14 = 3.34
UPDATE students SET 
    gpa = 3.34,
    total_credits = 14
WHERE student_id = 2;

-- ===============================================
-- 15. THÊM DỮ LIỆU ĐỂ TEST CONFLICT DETECTION
-- ===============================================
-- Tạo khóa học trùng lịch với khóa học đã đăng ký
INSERT INTO courses (course_code, subject_id, teacher_id, class_id, academic_year, semester, 
                     schedule_day, schedule_time, room, max_students, course_status) VALUES
('CNTT999_2024_1', 5, 4, 1, '2024-2025', 1, 'Thứ 2', 'Tiết 1-3 (07:00-09:30)', 'X999', 30, 'ongoing');

-- sv001 đã đăng ký CNTT301 vào Thứ 2, Tiết 1-3, nên không thể đăng ký CNTT999

-- ===============================================
-- HOÀN THÀNH
-- ===============================================
COMMIT;

SELECT 'Dữ liệu test đã được thêm thành công!' AS Status;

-- ===============================================
-- KIỂM TRA DỮ LIỆU
-- ===============================================
SELECT 'Tổng số sinh viên:' AS Info, COUNT(*) AS Count FROM students
UNION ALL
SELECT 'Tổng số khóa học:', COUNT(*) FROM courses
UNION ALL
SELECT 'Tổng số enrollments:', COUNT(*) FROM enrollments
UNION ALL
SELECT 'Tổng số grades:', COUNT(*) FROM grades
UNION ALL
SELECT 'Tổng số notifications:', COUNT(*) FROM notifications
UNION ALL
SELECT 'Tổng số course_registrations:', COUNT(*) FROM course_registrations;
--
