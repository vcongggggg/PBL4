package com.university.sms.dao;

import com.university.sms.model.Enrollment;
import com.university.sms.model.Grade;
import com.university.sms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Enrollment
 * ✅ REFACTORED: Dùng student_code, course_code thay vì student_id, course_id
 */
public class EnrollmentDAO {
    private static final Logger LOGGER = Logger.getLogger(EnrollmentDAO.class.getName());

    /**
     * Lưu enrollment (insert nếu chưa có ID, update nếu đã có ID)
     */
    public boolean save(Enrollment enrollment) {
        if (enrollment.getEnrollmentId() > 0) {
            Enrollment existing = findById(enrollment.getEnrollmentId());
            if (existing != null) {
                return true;
            }
        }

        Integer existingId = findEnrollmentIdByKeys(enrollment.getStudentCode(), enrollment.getCourseCode());
        if (existingId != null) {
            enrollment.setEnrollmentId(existingId);
            return true;
        }

        boolean result = addEnrollmentWithId(enrollment);
        return result;
    }

    /**
     * Tìm enrollment ID by codes (không join) - for duplicate check
     */
    private Integer findEnrollmentIdByKeys(String studentCode, String courseCode) {
        String sql = "SELECT enrollment_id FROM enrollments WHERE student_code = ? AND course_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, courseCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enrollment_id");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                    "Error checking enrollment existence: student_code=" + studentCode + ", course_code=" + courseCode,
                    e);
        }

        return null;
    }

    /**
     * Thêm enrollment mới với ID cụ thể (cho CSV import)
     */
    private boolean addEnrollmentWithId(Enrollment enrollment) {
        String sql = enrollment.getEnrollmentId() > 0
                ? "INSERT IGNORE INTO enrollments (enrollment_id, student_code, course_code, enrollment_status, final_grade, letter_grade, grade_points) VALUES (?, ?, ?, ?, ?, ?, ?)"
                : "INSERT IGNORE INTO enrollments (student_code, course_code, enrollment_status, final_grade, letter_grade, grade_points) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (enrollment.getEnrollmentId() > 0) {
                stmt.setInt(paramIndex++, enrollment.getEnrollmentId());
            }

            stmt.setString(paramIndex++, enrollment.getStudentCode());
            stmt.setString(paramIndex++, enrollment.getCourseCode());
            stmt.setString(paramIndex++, enrollment.getEnrollmentStatus().name().toLowerCase());

            if (enrollment.getFinalGrade() != null) {
                stmt.setBigDecimal(paramIndex++, enrollment.getFinalGrade());
            } else {
                stmt.setNull(paramIndex++, Types.DECIMAL);
            }

            if (enrollment.getLetterGrade() != null && !enrollment.getLetterGrade().isEmpty()) {
                stmt.setString(paramIndex++, enrollment.getLetterGrade());
            } else {
                stmt.setString(paramIndex++, "");
            }

            if (enrollment.getGradePoints() != null) {
                stmt.setBigDecimal(paramIndex++, enrollment.getGradePoints());
            } else {
                stmt.setBigDecimal(paramIndex++, BigDecimal.ZERO);
            }

            int result = stmt.executeUpdate();

            // INSERT IGNORE returns 0 if duplicate, but that's OK for CSV import
            if (enrollment.getEnrollmentId() == 0 && result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        enrollment.setEnrollmentId(rs.getInt(1));
                    }
                }
            }

            LOGGER.info("Enrollment processed: Student " + enrollment.getStudentCode() +
                    " -> Course " + enrollment.getCourseCode() + " (inserted=" + (result > 0) + ")");
            return true; // Always return true for INSERT IGNORE

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding enrollment", e);
            return false;
        }
    }

    /**
     * @deprecated Thêm đăng ký mới (chỉ student_id, course_id)
     */
    @Deprecated
    public boolean addEnrollment(Enrollment enrollment) {
        String sql = "INSERT INTO enrollments (student_code, course_code) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, enrollment.getStudentCode());
            stmt.setString(2, enrollment.getCourseCode());

            int result = stmt.executeUpdate();

            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        enrollment.setEnrollmentId(rs.getInt(1));
                    }
                }
                LOGGER.info("Enrollment added successfully: Student " + enrollment.getStudentCode() +
                        " -> Course " + enrollment.getCourseCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding enrollment", e);
        }

        return false;
    }

    /**
     * Lấy đăng ký theo ID
     */
    public Enrollment findById(int enrollmentId) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                "c.course_code, sub.subject_name, sub.credits " +
                "FROM enrollments e " +
                "JOIN students s ON e.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE e.enrollment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEnrollment(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollment by ID", e);
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Lấy đăng ký theo student_code
     */
    public List<Enrollment> findByStudentCode(String studentCode) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                "c.course_code, sub.subject_name, sub.credits " +
                "FROM enrollments e " +
                "JOIN students s ON e.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE e.student_code = ? ORDER BY e.enrollment_date DESC";

        List<Enrollment> enrollments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollments by student code: " + studentCode, e);
        }

        return enrollments;
    }

    /**
     * ✅ REFACTORED: Lấy đăng ký theo course_code
     */
    public List<Enrollment> findByCourseCode(String courseCode) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                "c.course_code, sub.subject_name, sub.credits " +
                "FROM enrollments e " +
                "JOIN students s ON e.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE e.course_code = ? ORDER BY s.student_code";

        List<Enrollment> enrollments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding enrollments by course code: " + courseCode, e);
        }

        return enrollments;
    }

    /**
     * ✅ REFACTORED: Tìm đăng ký cụ thể
     */
    public Enrollment findByStudentAndCourse(String studentCode, String courseCode) {
        String sql = "SELECT e.*, s.student_code, u.full_name AS student_name, " +
                "c.course_code, sub.subject_name, sub.credits " +
                "FROM enrollments e " +
                "JOIN students s ON e.student_code = s.student_code " +
                "JOIN users u ON s.username = u.username " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "WHERE e.student_code = ? AND e.course_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, courseCode);

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
     * Tự động cập nhật trạng thái enrollment dựa trên số lượng điểm
     * - Nếu đủ 3 cột điểm (ASSIGNMENT, MIDTERM, FINAL) → COMPLETED (Kết thúc học
     * phần)
     * - Nếu chưa đủ → giữ nguyên status hiện tại
     */
    public boolean updateEnrollmentStatusBasedOnGrades(int enrollmentId, String studentCode, String courseCode) {
        try {
            GradeDAO gradeDAO = new GradeDAO();
            List<Grade> grades = gradeDAO.getGradesByStudentAndCourse(studentCode, courseCode);

            boolean hasAssignment = false;
            boolean hasMidterm = false;
            boolean hasFinal = false;

            for (Grade grade : grades) {
                if (grade.getGradeType() == Grade.GradeType.ASSIGNMENT) {
                    hasAssignment = true;
                } else if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                    hasMidterm = true;
                } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                    hasFinal = true;
                }
            }

            // Nếu đủ 3 cột điểm → cập nhật status = COMPLETED
            if (hasAssignment && hasMidterm && hasFinal) {
                return updateEnrollmentStatus(enrollmentId, Enrollment.EnrollmentStatus.COMPLETED);
            }

            // Nếu chưa đủ, không cập nhật (giữ nguyên status hiện tại)
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi khi kiểm tra và cập nhật enrollment status dựa trên grades: " + enrollmentId,
                    e);
            return false;
        }
    }

    /**
     * Cập nhật điểm cuối kỳ
     */
    public boolean updateFinalGrade(int enrollmentId, BigDecimal finalGrade, String letterGrade,
            BigDecimal gradePoints) {
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

    public int countByCourse(String courseCode) {
        // Đếm các enrollment có status 'enrolled', 'completed', 'failed' (không đếm
        // 'dropped')
        String sql = "SELECT COUNT(*) AS total FROM enrollments WHERE course_code = ? " +
                "AND enrollment_status IN ('enrolled', 'completed', 'failed')";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting enrollments for course: " + courseCode, e);
        }

        return 0;
    }

    /**
     * ✅ REFACTORED: Map ResultSet to Enrollment object
     */
    private Enrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
        enrollment.setStudentCode(rs.getString("student_code"));
        enrollment.setCourseCode(rs.getString("course_code"));
        enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));

        String status = rs.getString("enrollment_status");
        if (status != null) {
            enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
        }

        enrollment.setFinalGrade(rs.getBigDecimal("final_grade"));
        enrollment.setLetterGrade(rs.getString("letter_grade"));
        enrollment.setGradePoints(rs.getBigDecimal("grade_points"));

        // Related information
        enrollment.setStudentCode(rs.getString("student_code"));
        enrollment.setStudentName(rs.getString("student_name"));
        enrollment.setCourseCode(rs.getString("course_code"));
        enrollment.setSubjectName(rs.getString("subject_name"));
        enrollment.setCredits(rs.getInt("credits"));

        return enrollment;
    }
}
