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

    /**
     * Thêm điểm mới với validation
     */
    public boolean addGrade(Grade grade) throws Exception {
        // Validate
        validateGrade(grade);
        
        // Kiểm tra enrollment exists
        Enrollment enrollment = enrollmentDAO.findById(grade.getEnrollmentId());
        if (enrollment == null) {
            throw new IllegalArgumentException("Enrollment không tồn tại");
        }

        // Thêm grade
        boolean result = gradeDAO.addGrade(grade);
        
        if (result) {
            LOGGER.info("Added grade successfully: " + grade.getGradeName());
        }
        
        return result;
    }

    /**
     * Cập nhật điểm với validation
     */
    public boolean updateGrade(Grade grade) throws Exception {
        // Validate
        validateGrade(grade);
        
        // Kiểm tra grade exists
        Grade existingGrade = gradeDAO.findById(grade.getGradeId());
        if (existingGrade == null) {
            throw new IllegalArgumentException("Grade không tồn tại");
        }

        return gradeDAO.updateGrade(grade);
    }

    /**
     * Xóa điểm
     */
    public boolean deleteGrade(int gradeId) throws Exception {
        Grade grade = gradeDAO.findById(gradeId);
        if (grade == null) {
            throw new IllegalArgumentException("Grade không tồn tại");
        }

        return gradeDAO.deleteGrade(gradeId);
    }

    /**
     * Lấy điểm theo ID
     */
    public Grade getGradeById(int gradeId) throws Exception {
        return gradeDAO.findById(gradeId);
    }

    /**
     * Lấy tất cả điểm của một enrollment
     */
    public List<Grade> getGradesByEnrollment(int enrollmentId) throws Exception {
        return gradeDAO.getGradesByEnrollment(enrollmentId);
    }

    /**
     * Lấy điểm của sinh viên trong một môn
     */
    public List<Grade> getGradesByStudentAndCourse(int studentId, int courseId) throws Exception {
        return gradeDAO.getGradesByStudentAndCourse(studentId, courseId);
    }

    /**
     * Lấy điểm theo loại
     */
    public List<Grade> getGradesByType(int enrollmentId, GradeType gradeType) throws Exception {
        return gradeDAO.getGradesByType(enrollmentId, gradeType);
    }

    /**
     * Lấy tất cả điểm của một course (cho giảng viên)
     */
    public List<Grade> getGradesByCourse(int courseId) throws Exception {
        return gradeDAO.getGradesByCourse(courseId);
    }

    /**
     * Lấy tất cả điểm của một sinh viên
     */
    public List<Grade> getGradesByStudent(int studentId) throws Exception {
        return gradeDAO.getGradesByStudent(studentId);
    }

    /**
     * Tính điểm tổng kết từ các điểm thành phần
     * Công thức: Σ(score/max_score * weight * 10)
     */
    public BigDecimal calculateFinalGrade(int enrollmentId) throws Exception {
        List<Grade> grades = gradeDAO.getGradesByEnrollment(enrollmentId);
        
        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal finalScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Grade grade : grades) {
            if (grade.getScore() != null && grade.getMaxScore() != null && grade.getWeight() != null) {
                // Component score = (score / max_score) * weight * 10
                BigDecimal componentScore = grade.getScore()
                    .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP)
                    .multiply(grade.getWeight())
                    .multiply(new BigDecimal("10"));
                
                finalScore = finalScore.add(componentScore);
                totalWeight = totalWeight.add(grade.getWeight());
            }
        }

        // Normalize nếu tổng trọng số != 1.0
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0 && 
            totalWeight.compareTo(BigDecimal.ONE) != 0) {
            finalScore = finalScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tự động tính và cập nhật điểm tổng kết vào enrollment
     * Sử dụng stored procedure CalculateFinalGrade
     */
    public boolean finalizeCourseGrade(int enrollmentId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL CalculateFinalGrade(?)}")) {
            
            stmt.setInt(1, enrollmentId);
            stmt.execute();
            
            LOGGER.info("Finalized grade for enrollment_id: " + enrollmentId);
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finalizing course grade", e);
            throw new Exception("Lỗi khi tính điểm tổng kết: " + e.getMessage());
        }
    }

    /**
     * Tính và cập nhật điểm cho nhiều enrollments (batch)
     */
    public int batchFinalizeCourseGrades(List<Integer> enrollmentIds) throws Exception {
        int successCount = 0;
        
        for (Integer enrollmentId : enrollmentIds) {
            try {
                if (finalizeCourseGrade(enrollmentId)) {
                    successCount++;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to finalize grade for enrollment_id: " + enrollmentId, e);
            }
        }
        
        return successCount;
    }

    /**
     * Lấy thống kê điểm của một sinh viên
     */
    public GradeStatistics getStudentGradeStatistics(int studentId) throws Exception {
        List<Grade> grades = gradeDAO.getGradesByStudent(studentId);
        
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

    /**
     * Validate grade data
     */
    private void validateGrade(Grade grade) throws IllegalArgumentException {
        if (grade.getEnrollmentId() <= 0) {
            throw new IllegalArgumentException("Enrollment ID không hợp lệ");
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

    /**
     * Inner class cho thống kê điểm
     */
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

