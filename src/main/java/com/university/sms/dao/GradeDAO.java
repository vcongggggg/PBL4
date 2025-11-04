package com.university.sms.dao;

import com.university.sms.model.Grade;
import com.university.sms.model.Grade.GradeType;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO class cho bảng grades
 * Quản lý CRUD operations cho điểm chi tiết
 */
public class GradeDAO {
    private static final Logger LOGGER = Logger.getLogger(GradeDAO.class.getName());

    /**
     * Thêm điểm mới
     */
    public boolean addGrade(Grade grade) throws SQLException {
        String sql = "INSERT INTO grades (enrollment_id, grade_type, grade_name, score, max_score, weight, grade_date, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, grade.getEnrollmentId());
            stmt.setString(2, grade.getGradeType().name().toLowerCase());
            stmt.setString(3, grade.getGradeName());
            stmt.setBigDecimal(4, grade.getScore());
            stmt.setBigDecimal(5, grade.getMaxScore());
            stmt.setBigDecimal(6, grade.getWeight());
            stmt.setDate(7, grade.getGradeDate());
            stmt.setString(8, grade.getNotes());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        grade.setGradeId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info("Added grade: " + grade.getGradeName() + " for enrollment_id: " + grade.getEnrollmentId());
                return true;
            }

            return false;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding grade", e);
            throw e;
        }
    }

    /**
     * Cập nhật điểm
     */
    public boolean updateGrade(Grade grade) throws SQLException {
        String sql = "UPDATE grades SET grade_type = ?, grade_name = ?, score = ?, max_score = ?, " +
                     "weight = ?, grade_date = ?, notes = ? WHERE grade_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, grade.getGradeType().name().toLowerCase());
            stmt.setString(2, grade.getGradeName());
            stmt.setBigDecimal(3, grade.getScore());
            stmt.setBigDecimal(4, grade.getMaxScore());
            stmt.setBigDecimal(5, grade.getWeight());
            stmt.setDate(6, grade.getGradeDate());
            stmt.setString(7, grade.getNotes());
            stmt.setInt(8, grade.getGradeId());

            int affectedRows = stmt.executeUpdate();
            LOGGER.info("Updated grade_id: " + grade.getGradeId());
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating grade", e);
            throw e;
        }
    }

    /**
     * Xóa điểm
     */
    public boolean deleteGrade(int gradeId) throws SQLException {
        String sql = "DELETE FROM grades WHERE grade_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gradeId);
            int affectedRows = stmt.executeUpdate();
            LOGGER.info("Deleted grade_id: " + gradeId);
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting grade", e);
            throw e;
        }
    }

    /**
     * Lấy điểm theo ID
     */
    public Grade findById(int gradeId) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE g.grade_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gradeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGrade(rs);
                }
            }

            return null;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding grade by ID", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả điểm của một enrollment
     */
    public List<Grade> getGradesByEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE g.enrollment_id = ? " +
                     "ORDER BY g.grade_date DESC, g.grade_id DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }

            return grades;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting grades by enrollment", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả điểm của một sinh viên trong một môn học
     */
    public List<Grade> getGradesByStudentAndCourse(int studentId, int courseId) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE e.student_id = ? AND e.course_id = ? " +
                     "ORDER BY g.grade_date DESC, g.grade_id DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }

            return grades;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting grades by student and course", e);
            throw e;
        }
    }

    /**
     * Lấy điểm theo loại (assignment, midterm, final...)
     */
    public List<Grade> getGradesByType(int enrollmentId, GradeType gradeType) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE g.enrollment_id = ? AND g.grade_type = ? " +
                     "ORDER BY g.grade_date DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);
            stmt.setString(2, gradeType.name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }

            return grades;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting grades by type", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả điểm của một khóa học (cho giảng viên)
     */
    public List<Grade> getGradesByCourse(int courseId) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE e.course_id = ? " +
                     "ORDER BY s.student_code, g.grade_date DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }

            return grades;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting grades by course", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả điểm của một sinh viên (cho xem transcript)
     */
    public List<Grade> getGradesByStudent(int studentId) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                     "c.course_code, sub.subject_name " +
                     "FROM grades g " +
                     "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                     "JOIN students s ON e.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                     "WHERE e.student_id = ? " +
                     "ORDER BY c.academic_year DESC, c.semester DESC, sub.subject_name";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }

            return grades;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting grades by student", e);
            throw e;
        }
    }

    /**
     * Map ResultSet to Grade object
     */
    private Grade mapResultSetToGrade(ResultSet rs) throws SQLException {
        Grade grade = new Grade();

        grade.setGradeId(rs.getInt("grade_id"));
        grade.setEnrollmentId(rs.getInt("enrollment_id"));
        
        // Convert string to enum
        String gradeTypeStr = rs.getString("grade_type").toUpperCase();
        grade.setGradeType(GradeType.valueOf(gradeTypeStr));
        
        grade.setGradeName(rs.getString("grade_name"));
        grade.setScore(rs.getBigDecimal("score"));
        grade.setMaxScore(rs.getBigDecimal("max_score"));
        grade.setWeight(rs.getBigDecimal("weight"));
        grade.setGradeDate(rs.getDate("grade_date"));
        grade.setNotes(rs.getString("notes"));
        grade.setCreatedAt(rs.getTimestamp("created_at"));

        // Related information
        grade.setStudentCode(rs.getString("student_code"));
        grade.setStudentName(rs.getString("student_name"));
        grade.setCourseCode(rs.getString("course_code"));
        grade.setSubjectName(rs.getString("subject_name"));

        return grade;
    }
}

