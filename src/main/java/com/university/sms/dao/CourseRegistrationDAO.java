package com.university.sms.dao;

import com.university.sms.model.CourseRegistration;
import com.university.sms.model.CourseRegistration.RegistrationStatus;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for managing course registrations
 * ✅ REFACTORED: Dùng student_code, course_code thay vì student_id, course_id
 */
public class CourseRegistrationDAO {
    private static final Logger LOGGER = Logger.getLogger(CourseRegistrationDAO.class.getName());

    /**
     * ✅ REFACTORED: Find all registrations
     */
    public List<CourseRegistration> findAll() {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                "s.student_code, u.full_name as student_name, " +
                "c.course_code, sub.subject_name, sub.credits, " +
                "u2.full_name as teacher_name, " +
                "c.schedule_day, c.schedule_time, c.room " +
                "FROM course_registrations cr " +
                "JOIN students s ON cr.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u2 ON c.teacher_username = u2.username " +
                "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách đăng ký học phần", e);
        }

        return registrations;
    }

    /**
     * Find registration by ID
     */
    public CourseRegistration findById(int registrationId) {
        String sql = "SELECT cr.*, " +
                "s.student_code, u.full_name as student_name, " +
                "c.course_code, sub.subject_name, sub.credits, " +
                "u2.full_name as teacher_name, " +
                "c.schedule_day, c.schedule_time, c.room " +
                "FROM course_registrations cr " +
                "JOIN students s ON cr.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u2 ON c.teacher_username = u2.username " +
                "WHERE cr.registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, registrationId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractRegistrationFromResultSet(rs);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm đăng ký theo ID", e);
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Find registrations by student (dùng student_code)
     */
    public List<CourseRegistration> findByStudent(String studentCode) {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                "s.student_code, u.full_name as student_name, " +
                "c.course_code, sub.subject_name, sub.credits, " +
                "u2.full_name as teacher_name, " +
                "c.schedule_day, c.schedule_time, c.room " +
                "FROM course_registrations cr " +
                "JOIN students s ON cr.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u2 ON c.teacher_username = u2.username " +
                "WHERE cr.student_code = ? " +
                "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm đăng ký theo sinh viên", e);
        }

        return registrations;
    }

    /**
     * ✅ REFACTORED: Find registrations by course (dùng course_code)
     */
    public List<CourseRegistration> findByCourse(String courseCode) {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                "s.student_code, u.full_name as student_name, " +
                "c.course_code, sub.subject_name, sub.credits, " +
                "u2.full_name as teacher_name, " +
                "c.schedule_day, c.schedule_time, c.room " +
                "FROM course_registrations cr " +
                "JOIN students s ON cr.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u2 ON c.teacher_username = u2.username " +
                "WHERE cr.course_code = ? " +
                "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm đăng ký theo lớp học phần", e);
        }

        return registrations;
    }

    /**
     * Find registrations by status
     */
    public List<CourseRegistration> findByStatus(RegistrationStatus status) {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                "s.student_code, u.full_name as student_name, " +
                "c.course_code, sub.subject_name, sub.credits, " +
                "u2.full_name as teacher_name, " +
                "c.schedule_day, c.schedule_time, c.room " +
                "FROM course_registrations cr " +
                "JOIN students s ON cr.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u2 ON c.teacher_username = u2.username " +
                "WHERE cr.registration_status = ? " +
                "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm đăng ký theo trạng thái", e);
        }

        return registrations;
    }

    /**
     * ✅ REFACTORED: Lưu registration (insert nếu chưa có ID, update nếu đã có ID)
     */
    public boolean save(CourseRegistration registration) {
        if (registration.getRegistrationId() > 0) {
            // Check if exists
            CourseRegistration existing = findById(registration.getRegistrationId());
            if (existing != null) {
                // Update existing registration
                return update(registration);
            }
        }
        // Insert new registration (có thể với ID từ CSV)
        return insertWithId(registration);
    }

    /**
     * ✅ REFACTORED: Insert registration with specific ID (for CSV import)
     */
    private boolean insertWithId(CourseRegistration registration) {
        String sql = registration.getRegistrationId() > 0
                ? "INSERT IGNORE INTO course_registrations (registration_id, student_code, course_code, registration_status, notes, created_at) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT IGNORE INTO course_registrations (student_code, course_code, registration_status, notes) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (registration.getRegistrationId() > 0) {
                pstmt.setInt(paramIndex++, registration.getRegistrationId());
            }

            pstmt.setString(paramIndex++, registration.getStudentCode());
            pstmt.setString(paramIndex++, registration.getCourseCode());
            pstmt.setString(paramIndex++, registration.getRegistrationStatus().name());
            pstmt.setString(paramIndex++, registration.getNotes());

            if (registration.getRegistrationId() > 0 && registration.getCreatedAt() != null) {
                pstmt.setTimestamp(paramIndex++, registration.getCreatedAt());
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0 && registration.getRegistrationId() == 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    registration.setRegistrationId(generatedKeys.getInt(1));
                }
            }

            LOGGER.info("Đã xử lý đăng ký: SV " + registration.getStudentCode() +
                    " -> Lớp " + registration.getCourseCode() + " (đã chèn=" + (affectedRows > 0) + ")");
            return true; // Always return true for INSERT IGNORE

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn đăng ký học phần", e);
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Insert new registration
     */
    public boolean insert(CourseRegistration registration) {
        String sql = "INSERT INTO course_registrations " +
                "(student_code, course_code, registration_status, notes) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, registration.getStudentCode());
            pstmt.setString(2, registration.getCourseCode());
            pstmt.setString(3, registration.getRegistrationStatus().name());
            pstmt.setString(4, registration.getNotes());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    registration.setRegistrationId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tạo đăng ký học phần", e);
        }

        return false;
    }

    /**
     * Update registration
     */
    public boolean update(CourseRegistration registration) {
        String sql = "UPDATE course_registrations SET " +
                "registration_status = ?, " +
                "notes = ?, " +
                "cancel_date = ? " +
                "WHERE registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, registration.getRegistrationStatus().name());
            pstmt.setString(2, registration.getNotes());
            pstmt.setTimestamp(3, registration.getCancelDate());
            pstmt.setInt(4, registration.getRegistrationId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật đăng ký học phần", e);
        }

        return false;
    }

    /**
     * Cancel registration
     */
    public boolean cancel(int registrationId) {
        String sql = "UPDATE course_registrations SET " +
                "registration_status = 'CANCELLED', " +
                "cancel_date = NOW() " +
                "WHERE registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, registrationId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi hủy đăng ký học phần", e);
        }

        return false;
    }

    /**
     * Delete registration
     */
    public boolean delete(int registrationId) {
        String sql = "DELETE FROM course_registrations WHERE registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, registrationId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa đăng ký học phần", e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Check if student already registered for a course
     */
    public boolean isAlreadyRegistered(String studentCode, String courseCode) {
        String sql = "SELECT COUNT(*) FROM course_registrations " +
                "WHERE student_code = ? AND course_code = ? " +
                "AND registration_status != 'CANCELLED'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentCode);
            pstmt.setString(2, courseCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra đăng ký trùng", e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Check if student can register (check schedule conflict)
     */
    public boolean hasScheduleConflict(String studentCode, String courseCode) {
        String sql = "SELECT COUNT(*) FROM course_registrations cr1 " +
                "JOIN courses c1 ON cr1.course_code = c1.course_code " +
                "JOIN courses c2 ON c2.course_code = ? " +
                "WHERE cr1.student_code = ? " +
                "AND cr1.registration_status = 'APPROVED' " +
                "AND c1.schedule_day = c2.schedule_day " +
                "AND c1.schedule_time = c2.schedule_time " +
                "AND c1.course_code != c2.course_code";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            pstmt.setString(2, studentCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra xung đột lịch học phần", e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Get total registered credits for student in a semester
     */
    public int getTotalCredits(String studentCode, String academicYear, int semester) {
        String sql = "SELECT SUM(sub.credits) " +
                "FROM course_registrations cr " +
                "JOIN courses c ON cr.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE cr.student_code = ? " +
                "AND c.academic_year = ? " +
                "AND c.semester = ? " +
                "AND cr.registration_status = 'APPROVED'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentCode);
            pstmt.setString(2, academicYear);
            pstmt.setInt(3, semester);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy tổng tín chỉ đã đăng ký", e);
        }

        return 0;
    }

    /**
     * ✅ REFACTORED: Extract CourseRegistration from ResultSet
     */
    private CourseRegistration extractRegistrationFromResultSet(ResultSet rs) throws SQLException {
        CourseRegistration registration = new CourseRegistration();

        registration.setRegistrationId(rs.getInt("registration_id"));
        registration.setStudentCode(rs.getString("student_code"));
        registration.setCourseCode(rs.getString("course_code"));
        registration.setRegistrationDate(rs.getTimestamp("registration_date"));
        registration.setRegistrationStatus(RegistrationStatus.valueOf(rs.getString("registration_status")));
        registration.setCancelDate(rs.getTimestamp("cancel_date"));
        registration.setNotes(rs.getString("notes"));

        // Related information
        registration.setStudentCode(rs.getString("student_code"));
        registration.setStudentName(rs.getString("student_name"));
        registration.setCourseCode(rs.getString("course_code"));
        registration.setSubjectName(rs.getString("subject_name"));
        registration.setCredits(rs.getInt("credits"));
        registration.setTeacherName(rs.getString("teacher_name"));
        registration.setScheduleDay(rs.getString("schedule_day"));
        registration.setScheduleTime(rs.getString("schedule_time"));
        registration.setRoom(rs.getString("room"));

        return registration;
    }
}
