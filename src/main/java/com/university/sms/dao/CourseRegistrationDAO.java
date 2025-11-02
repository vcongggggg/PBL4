package com.university.sms.dao;

import com.university.sms.model.CourseRegistration;
import com.university.sms.model.CourseRegistration.RegistrationStatus;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for managing course registrations
 */
public class CourseRegistrationDAO {
    private static final Logger LOGGER = Logger.getLogger(CourseRegistrationDAO.class.getName());

    /**
     * Find all registrations
     */
    public List<CourseRegistration> findAll() {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                     "s.student_code, u.full_name as student_name, " +
                     "c.course_code, sub.subject_name, sub.credits, " +
                     "u2.full_name as teacher_name, " +
                     "c.schedule_day, c.schedule_time, c.room " +
                     "FROM course_registrations cr " +
                     "JOIN students s ON cr.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "JOIN users u2 ON c.teacher_id = u2.user_id " +
                     "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding all registrations: " + e.getMessage());
            e.printStackTrace();
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
                     "JOIN students s ON cr.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "JOIN users u2 ON c.teacher_id = u2.user_id " +
                     "WHERE cr.registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, registrationId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractRegistrationFromResultSet(rs);
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding registration by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find registrations by student
     */
    public List<CourseRegistration> findByStudent(int studentId) {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                     "s.student_code, u.full_name as student_name, " +
                     "c.course_code, sub.subject_name, sub.credits, " +
                     "u2.full_name as teacher_name, " +
                     "c.schedule_day, c.schedule_time, c.room " +
                     "FROM course_registrations cr " +
                     "JOIN students s ON cr.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "JOIN users u2 ON c.teacher_id = u2.user_id " +
                     "WHERE cr.student_id = ? " +
                     "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding registrations by student: " + e.getMessage());
            e.printStackTrace();
        }

        return registrations;
    }

    /**
     * Find registrations by course
     */
    public List<CourseRegistration> findByCourse(int courseId) {
        List<CourseRegistration> registrations = new ArrayList<>();
        String sql = "SELECT cr.*, " +
                     "s.student_code, u.full_name as student_name, " +
                     "c.course_code, sub.subject_name, sub.credits, " +
                     "u2.full_name as teacher_name, " +
                     "c.schedule_day, c.schedule_time, c.room " +
                     "FROM course_registrations cr " +
                     "JOIN students s ON cr.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "JOIN users u2 ON c.teacher_id = u2.user_id " +
                     "WHERE cr.course_id = ? " +
                     "ORDER BY cr.registration_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                registrations.add(extractRegistrationFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding registrations by course: " + e.getMessage());
            e.printStackTrace();
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
                     "JOIN students s ON cr.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "JOIN users u2 ON c.teacher_id = u2.user_id " +
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
            LOGGER.severe("Error finding registrations by status: " + e.getMessage());
            e.printStackTrace();
        }

        return registrations;
    }

    /**
     * Insert new registration
     */
    public boolean insert(CourseRegistration registration) {
        String sql = "INSERT INTO course_registrations " +
                     "(student_id, course_id, registration_status, notes) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, registration.getStudentId());
            pstmt.setInt(2, registration.getCourseId());
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
            LOGGER.severe("Error inserting registration: " + e.getMessage());
            e.printStackTrace();
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
            LOGGER.severe("Error updating registration: " + e.getMessage());
            e.printStackTrace();
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
            LOGGER.severe("Error cancelling registration: " + e.getMessage());
            e.printStackTrace();
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
            LOGGER.severe("Error deleting registration: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if student already registered for a course
     */
    public boolean isAlreadyRegistered(int studentId, int courseId) {
        String sql = "SELECT COUNT(*) FROM course_registrations " +
                     "WHERE student_id = ? AND course_id = ? " +
                     "AND registration_status != 'CANCELLED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOGGER.severe("Error checking registration: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if student can register (check schedule conflict)
     */
    public boolean hasScheduleConflict(int studentId, int courseId) {
        String sql = "SELECT COUNT(*) FROM course_registrations cr1 " +
                     "JOIN courses c1 ON cr1.course_id = c1.course_id " +
                     "JOIN courses c2 ON c2.course_id = ? " +
                     "WHERE cr1.student_id = ? " +
                     "AND cr1.registration_status = 'APPROVED' " +
                     "AND c1.schedule_day = c2.schedule_day " +
                     "AND c1.schedule_time = c2.schedule_time " +
                     "AND c1.course_id != c2.course_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            pstmt.setInt(2, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOGGER.severe("Error checking schedule conflict: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total registered credits for student in a semester
     */
    public int getTotalCredits(int studentId, String academicYear, int semester) {
        String sql = "SELECT SUM(sub.credits) " +
                     "FROM course_registrations cr " +
                     "JOIN courses c ON cr.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE cr.student_id = ? " +
                     "AND c.academic_year = ? " +
                     "AND c.semester = ? " +
                     "AND cr.registration_status = 'APPROVED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            pstmt.setString(2, academicYear);
            pstmt.setInt(3, semester);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            LOGGER.severe("Error getting total credits: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Extract CourseRegistration from ResultSet
     */
    private CourseRegistration extractRegistrationFromResultSet(ResultSet rs) throws SQLException {
        CourseRegistration registration = new CourseRegistration();

        registration.setRegistrationId(rs.getInt("registration_id"));
        registration.setStudentId(rs.getInt("student_id"));
        registration.setCourseId(rs.getInt("course_id"));
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





