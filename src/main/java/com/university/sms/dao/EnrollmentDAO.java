package com.university.sms.dao;

import com.university.sms.model.Enrollment;
import com.university.sms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Enrollment
 */
public class EnrollmentDAO {
    private static final Logger LOGGER = Logger.getLogger(EnrollmentDAO.class.getName());

    /**
     * Thêm đăng ký mới
     */
    public boolean addEnrollment(Enrollment enrollment) {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, enrollment.getStudentId());
            stmt.setInt(2, enrollment.getCourseId());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        enrollment.setEnrollmentId(rs.getInt(1));
                    }
                }
                LOGGER.info("Enrollment added successfully: Student " + enrollment.getStudentId() + 
                           " -> Course " + enrollment.getCourseId());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding enrollment", e);
        }
        
        return false;
    }

    /**
     * Lấy đăng ký theo sinh viên
     */
    public List<Enrollment> findByStudentId(int studentId) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                    "c.course_code, sub.subject_name, sub.credits " +
                    "FROM enrollments e " +
                    "JOIN students s ON e.student_id = s.student_id " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN courses c ON e.course_id = c.course_id " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "WHERE e.student_id = ? ORDER BY e.enrollment_date DESC";
        
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollments by student ID: " + studentId, e);
        }
        
        return enrollments;
    }

    /**
     * Lấy đăng ký theo khóa học
     */
    public List<Enrollment> findByCourseId(int courseId) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                    "c.course_code, sub.subject_name, sub.credits " +
                    "FROM enrollments e " +
                    "JOIN students s ON e.student_id = s.student_id " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN courses c ON e.course_id = c.course_id " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "WHERE e.course_id = ? ORDER BY s.student_code";
        
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollments by course ID: " + courseId, e);
        }
        
        return enrollments;
    }

    /**
     * Tìm đăng ký cụ thể
     */
    public Enrollment findByStudentAndCourse(int studentId, int courseId) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                    "c.course_code, sub.subject_name, sub.credits " +
                    "FROM enrollments e " +
                    "JOIN students s ON e.student_id = s.student_id " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN courses c ON e.course_id = c.course_id " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "WHERE e.student_id = ? AND e.course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEnrollment(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollment by student and course", e);
        }
        
        return null;
    }

    /**
     * Cập nhật trạng thái đăng ký
     */
    public boolean updateEnrollmentStatus(int enrollmentId, Enrollment.EnrollmentStatus status) {
        String sql = "UPDATE enrollments SET enrollment_status = ? WHERE enrollment_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, enrollmentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Enrollment status updated successfully: " + enrollmentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating enrollment status: " + enrollmentId, e);
        }
        
        return false;
    }

    /**
     * Cập nhật điểm cuối kỳ
     */
    public boolean updateFinalGrade(int enrollmentId, BigDecimal finalGrade, String letterGrade, BigDecimal gradePoints) {
        String sql = "UPDATE enrollments SET final_grade = ?, letter_grade = ?, grade_points = ? WHERE enrollment_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, finalGrade);
            stmt.setString(2, letterGrade);
            stmt.setBigDecimal(3, gradePoints);
            stmt.setInt(4, enrollmentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Final grade updated successfully: " + enrollmentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating final grade: " + enrollmentId, e);
        }
        
        return false;
    }

    /**
     * Cập nhật tỷ lệ điểm danh
     */
    public boolean updateAttendanceRate(int enrollmentId, BigDecimal attendanceRate) {
        String sql = "UPDATE enrollments SET attendance_rate = ? WHERE enrollment_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, attendanceRate);
            stmt.setInt(2, enrollmentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Attendance rate updated successfully: " + enrollmentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating attendance rate: " + enrollmentId, e);
        }
        
        return false;
    }

    /**
     * Xóa đăng ký
     */
    public boolean deleteEnrollment(int enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, enrollmentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Enrollment deleted successfully: " + enrollmentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting enrollment: " + enrollmentId, e);
        }
        
        return false;
    }

    /**
     * Map ResultSet to Enrollment object
     */
    private Enrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
        enrollment.setStudentId(rs.getInt("student_id"));
        enrollment.setCourseId(rs.getInt("course_id"));
        enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));
        
        String status = rs.getString("enrollment_status");
        if (status != null) {
            enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
        }
        
        enrollment.setFinalGrade(rs.getBigDecimal("final_grade"));
        enrollment.setLetterGrade(rs.getString("letter_grade"));
        enrollment.setGradePoints(rs.getBigDecimal("grade_points"));
        enrollment.setAttendanceRate(rs.getBigDecimal("attendance_rate"));
        
        // Related information
        enrollment.setStudentCode(rs.getString("student_code"));
        enrollment.setStudentName(rs.getString("student_name"));
        enrollment.setCourseCode(rs.getString("course_code"));
        enrollment.setSubjectName(rs.getString("subject_name"));
        enrollment.setCredits(rs.getInt("credits"));
        
        return enrollment;
    }
}


