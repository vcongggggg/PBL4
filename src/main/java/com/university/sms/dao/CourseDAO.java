package com.university.sms.dao;

import com.university.sms.model.Course;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Course
 * ✅ REFACTORED: Dùng subject_code, teacher_username, class_code thay vì IDs
 */
public class CourseDAO {
    private static final Logger LOGGER = Logger.getLogger(CourseDAO.class.getName());

    /**
     * ✅ REFACTORED: Lấy tất cả khóa học
     */
    public List<Course> findAll() {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name, dor.source AS data_source " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "LEFT JOIN data_origin dor ON dor.entity_type = 'course' AND dor.entity_id = c.course_id " +
                "ORDER BY CASE WHEN dor.source = 'CSV' THEN 0 ELSE 1 END, COALESCE(dor.source,'ZZZ'), c.academic_year DESC, c.semester DESC, c.course_code";

        List<Course> courses = new ArrayList<>();

        LOGGER.info("Starting findAll() query: " + sql);
        long startTime = System.currentTimeMillis();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            LOGGER.info("Query executed, processing results...");
            int count = 0;
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
                count++;
            }

            long endTime = System.currentTimeMillis();
            LOGGER.info("Query completed. Found " + count + " courses in " + (endTime - startTime) + "ms");

        } catch (SQLException e) {
            long endTime = System.currentTimeMillis();
            LOGGER.log(Level.SEVERE, "Error finding all courses after " + (endTime - startTime) + "ms", e);
        }

        return courses;
    }

    /**
     * Tìm khóa học theo ID
     */
    public Course findById(int courseId) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding course by ID: " + courseId, e);
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Tìm khóa học theo mã khóa học
     */
    public Course findByCourseCode(String courseCode) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.course_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding course by code: " + courseCode, e);
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Tìm khóa học theo giáo viên (dùng username)
     */
    public List<Course> findByTeacherUsername(String teacherUsername) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.teacher_username = ? " +
                "AND c.course_status = 'ongoing' " +
                "ORDER BY c.academic_year DESC, c.semester DESC";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacherUsername);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by teacher username: " + teacherUsername, e);
        }

        return courses;
    }

    /**
     * Tìm tất cả khóa học của giáo viên (không lọc theo status)
     */
    public List<Course> findAllByTeacherUsername(String teacherUsername) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.teacher_username = ? " +
                "ORDER BY c.academic_year DESC, c.semester DESC";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacherUsername);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all courses by teacher username: " + teacherUsername, e);
        }

        return courses;
    }

    /**
     * ✅ REFACTORED: Tìm khóa học theo lớp (dùng class_code)
     */
    public List<Course> findByClassCode(String classCode) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.class_code = ? ORDER BY c.academic_year DESC, c.semester DESC";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, classCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by class code: " + classCode, e);
        }

        return courses;
    }

    /**
     * Tìm khóa học theo năm học và học kỳ
     */
    public List<Course> findByAcademicYearAndSemester(String academicYear, int semester) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.academic_year = ? AND c.semester = ? ORDER BY c.course_code";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, academicYear);
            stmt.setInt(2, semester);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by academic year and semester", e);
        }

        return courses;
    }

    /**
     * ✅ REFACTORED: Tìm khóa học theo môn học (dùng subject_code)
     */
    public List<Course> findBySubjectCode(String subjectCode) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.subject_code = ? ORDER BY c.academic_year DESC, c.semester DESC";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, subjectCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by subject code: " + subjectCode, e);
        }

        return courses;
    }

    /**
     * ✅ REFACTORED: Thêm khóa học mới
     */
    public boolean addCourse(Course course) {
        String sql = "INSERT INTO courses (course_code, subject_code, teacher_username, class_code, " +
                "academic_year, semester, schedule_day, schedule_time, room, max_students, current_students, " +
                "registration_status, course_status, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, course.getCourseCode());
            stmt.setString(2, course.getSubjectCode());
            stmt.setString(3, course.getTeacherUsername());

            if (course.getClassCode() != null && !course.getClassCode().isEmpty()) {
                stmt.setString(4, course.getClassCode());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.setString(5, course.getAcademicYear());
            stmt.setInt(6, course.getSemester());
            stmt.setString(7, course.getScheduleDay());
            stmt.setString(8, course.getScheduleTime());
            stmt.setString(9, course.getRoom());
            stmt.setInt(10, course.getMaxStudents());
            stmt.setInt(11, course.getCurrentStudents());
            stmt.setString(12, course.getRegistrationStatus() != null
                    ? course.getRegistrationStatus().name().toLowerCase()
                    : Course.RegistrationStatus.LOCKED.name().toLowerCase());
            stmt.setString(13, course.getCourseStatus() != null
                    ? course.getCourseStatus().name().toLowerCase()
                    : Course.CourseStatus.PLANNING.name().toLowerCase());
            stmt.setDate(14, course.getStartDate());
            stmt.setDate(15, course.getEndDate());

            int result = stmt.executeUpdate();

            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        course.setCourseId(rs.getInt(1));
                    }
                }
                LOGGER.info("Course added successfully: " + course.getCourseCode());
                return true;
            }

        } catch (SQLException e) {
            // Check if error is due to missing registration_status column
            if (e.getMessage() != null && e.getMessage().contains("registration_status")) {
                LOGGER.warning("registration_status column not found, attempting to add it...");
                try {
                    // Try to add the column
                    String alterSql = "ALTER TABLE courses ADD COLUMN registration_status ENUM('locked', 'open', 'closed') DEFAULT 'locked' AFTER current_students";
                    try (Connection conn = DatabaseConnection.getConnection();
                            Statement alterStmt = conn.createStatement()) {
                        alterStmt.executeUpdate(alterSql);
                        LOGGER.info("Successfully added registration_status column to courses table");
                        // Retry the insert
                        return addCourse(course);
                    }
                } catch (SQLException alterEx) {
                    // If column already exists, that's OK - just retry the insert
                    if (alterEx.getMessage() != null &&
                            (alterEx.getMessage().contains("Duplicate column name") ||
                                    alterEx.getMessage().contains("already exists"))) {
                        LOGGER.info("registration_status column already exists, retrying insert...");
                        // Retry the insert
                        return addCourse(course);
                    }
                    LOGGER.log(Level.SEVERE, "Failed to add registration_status column: " + alterEx.getMessage(),
                            alterEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Error adding course: " + course.getCourseCode(), e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Lưu course (insert nếu chưa có ID, update nếu đã có ID) - for
     * CSV import
     */
    public boolean save(Course course) {
        if (course.getCourseId() > 0) {
            // Check if exists
            Course existing = findById(course.getCourseId());
            if (existing != null) {
                // Update existing course
                return updateCourse(course);
            }
        }
        // Insert new course (có thể với ID từ CSV)
        return insertWithId(course);
    }

    /**
     * ✅ REFACTORED: Insert course with specific ID (for CSV import)
     */
    private boolean insertWithId(Course course) {
        String sql = course.getCourseId() > 0
                ? "INSERT INTO courses (course_id, course_code, subject_code, teacher_username, class_code, " +
                        "academic_year, semester, schedule_day, schedule_time, room, max_students, " +
                        "start_date, end_date, course_status, registration_status, current_students) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO courses (course_code, subject_code, teacher_username, class_code, " +
                        "academic_year, semester, schedule_day, schedule_time, room, max_students, " +
                        "start_date, end_date, course_status, registration_status, current_students) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (course.getCourseId() > 0) {
                stmt.setInt(paramIndex++, course.getCourseId());
            }

            stmt.setString(paramIndex++, course.getCourseCode());
            stmt.setString(paramIndex++, course.getSubjectCode());
            stmt.setString(paramIndex++, course.getTeacherUsername());

            if (course.getClassCode() != null && !course.getClassCode().isEmpty()) {
                stmt.setString(paramIndex++, course.getClassCode());
            } else {
                stmt.setNull(paramIndex++, Types.VARCHAR);
            }

            stmt.setString(paramIndex++, course.getAcademicYear());
            stmt.setInt(paramIndex++, course.getSemester());
            stmt.setString(paramIndex++, course.getScheduleDay());
            stmt.setString(paramIndex++, course.getScheduleTime());
            stmt.setString(paramIndex++, course.getRoom());
            stmt.setInt(paramIndex++, course.getMaxStudents());
            stmt.setDate(paramIndex++, course.getStartDate());
            stmt.setDate(paramIndex++, course.getEndDate());
            stmt.setString(paramIndex++,
                    course.getCourseStatus() != null ? course.getCourseStatus().name().toLowerCase()
                            : Course.CourseStatus.PLANNING.name().toLowerCase());
            stmt.setString(paramIndex++,
                    course.getRegistrationStatus() != null ? course.getRegistrationStatus().name().toLowerCase()
                            : Course.RegistrationStatus.LOCKED.name().toLowerCase());
            stmt.setInt(paramIndex++, course.getCurrentStudents());

            int result = stmt.executeUpdate();

            if (result > 0) {
                if (course.getCourseId() == 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            course.setCourseId(rs.getInt(1));
                        }
                    }
                }
                LOGGER.info("Course inserted successfully with ID: " + course.getCourseId());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting course with ID: " + course.getCourseCode(), e);
        }

        return false;
    }

    /**
     * Thêm mới hoặc cập nhật nếu đã tồn tại theo course_code
     */
    public boolean addOrUpdate(Course course) {
        try {
            Course existing = findByCourseCode(course.getCourseCode());
            if (existing == null) {
                return addCourse(course);
            }
            // Gán id rồi cập nhật các trường mutable cơ bản (lịch học, phòng, max_students,
            // ngày...)
            course.setCourseId(existing.getCourseId());
            return updateCourse(course);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error addOrUpdate course: " + course.getCourseCode(), e);
            return false;
        }
    }

    /**
     * Cập nhật khóa học
     */
    public boolean updateCourse(Course course) {
        String sql = "UPDATE courses SET schedule_day = ?, schedule_time = ?, room = ?, " +
                "max_students = ?, start_date = ?, end_date = ? WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, course.getScheduleDay());
            stmt.setString(2, course.getScheduleTime());
            stmt.setString(3, course.getRoom());
            stmt.setInt(4, course.getMaxStudents());
            stmt.setDate(5, course.getStartDate());
            stmt.setDate(6, course.getEndDate());
            stmt.setInt(7, course.getCourseId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Course updated successfully: " + course.getCourseId());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course: " + course.getCourseId(), e);
        }

        return false;
    }

    /**
     * Cập nhật trạng thái khóa học
     */
    public boolean updateCourseStatus(int courseId, Course.CourseStatus status) {
        String sql = "UPDATE courses SET course_status = ? WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, courseId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Course status updated successfully: " + courseId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course status: " + courseId, e);
        }

        return false;
    }

    public boolean updateRegistrationStatus(int courseId, Course.RegistrationStatus status) {
        String sql = "UPDATE courses SET registration_status = ? WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, courseId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Course registration status updated successfully: " + courseId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course registration status: " + courseId, e);
        }

        return false;
    }

    /**
     * Cập nhật số lượng sinh viên hiện tại
     */
    public boolean updateCurrentStudents(int courseId, int currentStudents) {
        String sql = "UPDATE courses SET current_students = ? WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentStudents);
            stmt.setInt(2, courseId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Course current students updated: " + courseId + " -> " + currentStudents);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course current students: " + courseId, e);
        }

        return false;
    }

    /**
     * Xóa khóa học
     */
    public boolean deleteCourse(int courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Course deleted successfully: " + courseId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting course: " + courseId, e);
        }

        return false;
    }

    /**
     * Tìm kiếm khóa học
     */
    public List<Course> searchCourses(String keyword) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.course_code LIKE ? OR sub.subject_name LIKE ? OR u.full_name LIKE ? " +
                "ORDER BY c.academic_year DESC, c.semester DESC";

        List<Course> courses = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching courses with keyword: " + keyword, e);
        }

        return courses;
    }

    /**
     * ✅ REFACTORED: Map ResultSet to Course object
     */
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setSubjectCode(rs.getString("subject_code"));
        course.setTeacherUsername(rs.getString("teacher_username"));

        String classCode = rs.getString("class_code");
        if (!rs.wasNull()) {
            course.setClassCode(classCode);
        }

        course.setAcademicYear(rs.getString("academic_year"));
        course.setSemester(rs.getInt("semester"));
        course.setScheduleDay(rs.getString("schedule_day"));
        course.setScheduleTime(rs.getString("schedule_time"));
        course.setRoom(rs.getString("room"));
        course.setMaxStudents(rs.getInt("max_students"));
        course.setCurrentStudents(rs.getInt("current_students"));

        String status = rs.getString("course_status");
        if (status != null) {
            course.setCourseStatus(Course.CourseStatus.valueOf(status.toUpperCase()));
        }
        String registrationStatus = rs.getString("registration_status");
        if (registrationStatus != null) {
            course.setRegistrationStatus(Course.RegistrationStatus.valueOf(registrationStatus.toUpperCase()));
        }

        course.setStartDate(rs.getDate("start_date"));
        course.setEndDate(rs.getDate("end_date"));
        course.setCreatedAt(rs.getTimestamp("created_at"));

        // Related information
        course.setSubjectName(rs.getString("subject_name"));
        course.setSubjectCode(rs.getString("subject_code"));
        course.setCredits(rs.getInt("credits"));
        course.setTeacherName(rs.getString("teacher_name"));
        course.setClassName(rs.getString("class_name"));

        return course;
    }

    /**
     * Lấy tổng số lượng khóa học
     */
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM courses";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total course count", e);
        }

        return 0;
    }

    /**
     * ✅ REFACTORED: Tìm khóa học theo môn học và học kỳ (dùng subject_code)
     */
    public List<Course> findBySubjectAndSemester(String subjectCode, String academicYear, int semester) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "WHERE c.subject_code = ? AND c.academic_year = ? AND c.semester = ? " +
                "ORDER BY c.course_code";

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, subjectCode);
            stmt.setString(2, academicYear);
            stmt.setInt(3, semester);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by subject and semester", e);
        }

        return courses;
    }
}
