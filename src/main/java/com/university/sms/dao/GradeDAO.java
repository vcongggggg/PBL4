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
 * ✅ REFACTORED: Dùng student_code, course_code thay vì enrollment_id
 */
public class GradeDAO {
    private static final Logger LOGGER = Logger.getLogger(GradeDAO.class.getName());

    /**
     * ✅ REFACTORED: Lưu grade (insert nếu chưa có ID, update nếu đã có ID)
     */
    public boolean save(Grade grade) {
        try {
            if (grade.getGradeId() > 0) {
                // Check if exists
                Grade existing = findById(grade.getGradeId());
                if (existing != null) {
                    // Update existing grade
                    return updateGrade(grade);
                }
            }
            // Insert new grade (có thể với ID từ CSV)
            return addGradeWithId(grade);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving grade", e);
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Thêm grade mới với ID cụ thể (cho CSV import) - dùng codes
     */
    private boolean addGradeWithId(Grade grade) throws SQLException {
        String sql = grade.getGradeId() > 0
                ? "INSERT IGNORE INTO grades (grade_id, student_code, course_code, grade_type, grade_name, score, max_score, weight, grade_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT IGNORE INTO grades (student_code, course_code, grade_type, grade_name, score, max_score, weight, grade_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (grade.getGradeId() > 0) {
                stmt.setInt(paramIndex++, grade.getGradeId());
            }

            stmt.setString(paramIndex++, grade.getStudentCode());
            stmt.setString(paramIndex++, grade.getCourseCode());
            stmt.setString(paramIndex++, grade.getGradeType().name().toLowerCase());
            stmt.setString(paramIndex++, grade.getGradeName());
            stmt.setBigDecimal(paramIndex++, grade.getScore());
            stmt.setBigDecimal(paramIndex++, grade.getMaxScore());
            stmt.setBigDecimal(paramIndex++, grade.getWeight());
            stmt.setDate(paramIndex++, grade.getGradeDate());
            stmt.setString(paramIndex++, grade.getNotes());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0 && grade.getGradeId() == 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        grade.setGradeId(generatedKeys.getInt(1));
                    }
                }
            }

            LOGGER.info("Grade processed: " + grade.getGradeName() + " for student_code=" + grade.getStudentCode()
                    + ", course_code=" + grade.getCourseCode() + " (inserted=" + (affectedRows > 0) + ")");
            return true; // Always return true for INSERT IGNORE
        }
    }

    /**
     * ✅ DEPRECATED: Thêm điểm mới (dùng enrollment_id)
     */
    @Deprecated
    public boolean addGrade(Grade grade) throws SQLException {
        String sql = "INSERT INTO grades (student_code, course_code, grade_type, grade_name, score, max_score, weight, grade_date, notes) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, grade.getStudentCode());
            stmt.setString(2, grade.getCourseCode());
            stmt.setString(3, grade.getGradeType().name().toLowerCase());
            stmt.setString(4, grade.getGradeName());
            stmt.setBigDecimal(5, grade.getScore());
            stmt.setBigDecimal(6, grade.getMaxScore());
            stmt.setBigDecimal(7, grade.getWeight());
            stmt.setDate(8, grade.getGradeDate());
            stmt.setString(9, grade.getNotes());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        grade.setGradeId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info("Added grade: " + grade.getGradeName() + " for student_code=" + grade.getStudentCode()
                        + ", course_code=" + grade.getCourseCode());
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
                "JOIN students s ON g.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON g.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
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
     * ✅ REFACTORED: Lấy tất cả điểm theo student_code và course_code
     */
    public List<Grade> getGradesByStudentAndCourse(String studentCode, String courseCode) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                "c.course_code, sub.subject_name " +
                "FROM grades g " +
                "JOIN students s ON g.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON g.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE g.student_code = ? AND g.course_code = ? " +
                "ORDER BY g.grade_date DESC, g.grade_id DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, courseCode);

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
     * ✅ REFACTORED: Lấy điểm theo loại (assignment, midterm, final...)
     */
    public List<Grade> getGradesByType(String studentCode, String courseCode, GradeType gradeType) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                "c.course_code, sub.subject_name " +
                "FROM grades g " +
                "JOIN students s ON g.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON g.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE g.student_code = ? AND g.course_code = ? AND g.grade_type = ? " +
                "ORDER BY g.grade_date DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, courseCode);
            stmt.setString(3, gradeType.name().toLowerCase());

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
     * ✅ REFACTORED: Lấy tất cả điểm của một khóa học (cho giảng viên)
     */
    public List<Grade> getGradesByCourse(String courseCode) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                "c.course_code, sub.subject_name " +
                "FROM grades g " +
                "JOIN students s ON g.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON g.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE g.course_code = ? " +
                "ORDER BY s.student_code, g.grade_date DESC";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);

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
     * ✅ REFACTORED: Lấy tất cả điểm của một sinh viên (cho xem transcript)
     */
    public List<Grade> getGradesByStudent(String studentCode) throws SQLException {
        String sql = "SELECT g.*, s.student_code, CONCAT(u.full_name) as student_name, " +
                "c.course_code, sub.subject_name " +
                "FROM grades g " +
                "JOIN students s ON g.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON g.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE g.student_code = ? " +
                "ORDER BY c.academic_year DESC, c.semester DESC, sub.subject_name";

        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);

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
     * ✅ REFACTORED: Map ResultSet to Grade object
     */
    private Grade mapResultSetToGrade(ResultSet rs) throws SQLException {
        Grade grade = new Grade();

        grade.setGradeId(rs.getInt("grade_id"));
        grade.setStudentCode(rs.getString("student_code"));
        grade.setCourseCode(rs.getString("course_code"));

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
