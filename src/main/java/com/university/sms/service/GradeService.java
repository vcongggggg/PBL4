package com.university.sms.service;

import com.university.sms.dao.GradeDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.model.Grade;
import com.university.sms.model.Grade.GradeType;
import com.university.sms.model.Enrollment;
import com.university.sms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class cho quản lý điểm
 * Business logic cho grades
 */
public class GradeService {
    private static final Logger LOGGER = Logger.getLogger(GradeService.class.getName());

    private GradeDAO gradeDAO;
    private EnrollmentDAO enrollmentDAO;

    public GradeService() {
        this.gradeDAO = new GradeDAO();
        this.enrollmentDAO = new EnrollmentDAO();
    }

    public boolean addGrade(Grade grade) throws Exception {
        // Validate
        validateGrade(grade);

        Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(
                grade.getStudentCode(), grade.getCourseCode());
        if (enrollment == null) {
            throw new IllegalArgumentException("Enrollment không tồn tại");
        }

        boolean result = gradeDAO.save(grade);

        if (result) {
            LOGGER.info("Added grade successfully: " + grade.getGradeName());
        }

        return result;
    }

    public boolean updateGrade(Grade grade) throws Exception {
        validateGrade(grade);

        Grade existingGrade = gradeDAO.findById(grade.getGradeId());
        if (existingGrade == null) {
            throw new IllegalArgumentException("Grade không tồn tại");
        }

        return gradeDAO.updateGrade(grade);
    }

    public boolean deleteGrade(int gradeId) throws Exception {
        Grade grade = gradeDAO.findById(gradeId);
        if (grade == null) {
            throw new IllegalArgumentException("Grade không tồn tại");
        }

        return gradeDAO.deleteGrade(gradeId);
    }

    public Grade getGradeById(int gradeId) throws Exception {
        return gradeDAO.findById(gradeId);
    }

    public List<Grade> getGradesByEnrollment(String studentCode, String courseCode) throws Exception {
        if (studentCode == null || studentCode.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code and course code are required");
        }
        return gradeDAO.getGradesByStudentAndCourse(studentCode, courseCode);
    }

    public List<Grade> getGradesByStudentAndCourse(String studentCode, String courseCode) throws Exception {
        if (studentCode == null || studentCode.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code and course code are required");
        }
        return gradeDAO.getGradesByStudentAndCourse(studentCode, courseCode);
    }

    public List<Grade> getGradesByType(String studentCode, String courseCode, GradeType gradeType) throws Exception {
        if (studentCode == null || studentCode.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code and course code are required");
        }
        return gradeDAO.getGradesByType(studentCode, courseCode, gradeType);
    }

    public List<Grade> getGradesByCourse(String courseCode) throws Exception {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code is required");
        }
        return gradeDAO.getGradesByCourse(courseCode);
    }

    public List<Grade> getGradesByStudent(String studentCode) throws Exception {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code is required");
        }
        return gradeDAO.getGradesByStudent(studentCode);
    }

    public BigDecimal calculateFinalGrade(String studentCode, String courseCode) throws Exception {
        List<Grade> grades = gradeDAO.getGradesByStudentAndCourse(studentCode, courseCode);

        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal finalScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Grade grade : grades) {
            if (grade.getScore() != null && grade.getMaxScore() != null && grade.getWeight() != null) {
                BigDecimal componentScore = grade.getScore()
                        .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP)
                        .multiply(grade.getWeight())
                        .multiply(new BigDecimal("10"));

                finalScore = finalScore.add(componentScore);
                totalWeight = totalWeight.add(grade.getWeight());
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0 &&
                totalWeight.compareTo(BigDecimal.ONE) != 0) {
            finalScore = finalScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean finalizeCourseGrade(String studentCode, String courseCode) throws Exception {
        if (studentCode == null || studentCode.trim().isEmpty() ||
                courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code and course code are required");
        }

        try (Connection conn = DatabaseConnection.getConnection();
                CallableStatement stmt = conn.prepareCall("{CALL CalculateFinalGradeByCode(?, ?)}")) {

            stmt.setString(1, studentCode);
            stmt.setString(2, courseCode);
            stmt.execute();

            LOGGER.info("Finalized grade for student_code: " + studentCode + ", course_code: " + courseCode);
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finalizing course grade", e);
            throw new Exception("Lỗi khi tính điểm tổng kết: " + e.getMessage());
        }
    }

    public int batchFinalizeCourseGrades(List<Enrollment> enrollments) throws Exception {
        int successCount = 0;

        for (Enrollment enrollment : enrollments) {
            try {
                if (finalizeCourseGrade(enrollment.getStudentCode(), enrollment.getCourseCode())) {
                    successCount++;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to finalize grade for student_code: " +
                        enrollment.getStudentCode() + ", course_code: " + enrollment.getCourseCode(), e);
            }
        }

        return successCount;
    }

    public GradeStatistics getStudentGradeStatistics(String studentCode) throws Exception {
        List<Grade> grades = gradeDAO.getGradesByStudent(studentCode);

        GradeStatistics stats = new GradeStatistics();
        stats.totalGrades = grades.size();

        if (grades.isEmpty()) {
            return stats;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        int assignmentCount = 0;
        int quizCount = 0;
        int midtermCount = 0;
        int finalCount = 0;
        int projectCount = 0;

        for (Grade grade : grades) {
            totalScore = totalScore.add(grade.getPercentage());

            switch (grade.getGradeType()) {
                case ASSIGNMENT:
                    assignmentCount++;
                    break;
                case QUIZ:
                    quizCount++;
                    break;
                case MIDTERM:
                    midtermCount++;
                    break;
                case FINAL:
                    finalCount++;
                    break;
                case PROJECT:
                    projectCount++;
                    break;
            }
        }

        stats.averagePercentage = totalScore.divide(new BigDecimal(grades.size()), 2, RoundingMode.HALF_UP);
        stats.assignmentCount = assignmentCount;
        stats.quizCount = quizCount;
        stats.midtermCount = midtermCount;
        stats.finalCount = finalCount;
        stats.projectCount = projectCount;

        return stats;
    }

    private void validateGrade(Grade grade) throws IllegalArgumentException {
        if (grade.getStudentCode() == null || grade.getStudentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Student code không được để trống");
        }

        if (grade.getCourseCode() == null || grade.getCourseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Course code không được để trống");
        }

        if (grade.getGradeType() == null) {
            throw new IllegalArgumentException("Loại điểm không được để trống");
        }

        if (grade.getGradeName() == null || grade.getGradeName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên điểm không được để trống");
        }

        if (grade.getScore() == null) {
            throw new IllegalArgumentException("Điểm không được để trống");
        }

        if (grade.getMaxScore() == null || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm tối đa phải lớn hơn 0");
        }

        if (grade.getScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm không được âm");
        }

        if (grade.getScore().compareTo(grade.getMaxScore()) > 0) {
            throw new IllegalArgumentException("Điểm không được vượt quá điểm tối đa");
        }

        if (grade.getWeight() == null || grade.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Trọng số phải lớn hơn 0");
        }

        if (grade.getWeight().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Trọng số không được vượt quá 1.0");
        }
    }

    public static class GradeStatistics {
        public int totalGrades;
        public BigDecimal averagePercentage;
        public int assignmentCount;
        public int quizCount;
        public int midtermCount;
        public int finalCount;
        public int projectCount;

        public GradeStatistics() {
            this.averagePercentage = BigDecimal.ZERO;
        }
    }
}
