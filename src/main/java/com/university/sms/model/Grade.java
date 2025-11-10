package com.university.sms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model class cho bảng grades (điểm chi tiết)
 * Lưu trữ các loại điểm: thường xuyên, giữa kỳ, cuối kỳ, đồ án
 * ✅ REFACTORED: Dùng composite FK (student_code, course_code)
 */
public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int gradeId;

    // ✅ NEW: Composite foreign key (client-safe)
    private String studentCode; // FK to enrollments(student_code, course_code)
    private String courseCode; // FK to enrollments(student_code, course_code)

    // Grade data
    private GradeType gradeType;
    private String gradeName;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private Date gradeDate;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Related information (from joins)
    private String studentName;
    private String subjectName;

    /**
     * Loại điểm
     */
    public enum GradeType {
        ASSIGNMENT("Thường xuyên"),
        QUIZ("Kiểm tra"),
        MIDTERM("Giữa kỳ"),
        FINAL("Cuối kỳ"),
        PROJECT("Đồ án");

        private final String displayName;

        GradeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Constructors
    public Grade() {
        this.score = BigDecimal.ZERO;
        this.maxScore = new BigDecimal("10.00");
        this.weight = new BigDecimal("1.00");
    }

    public Grade(String studentCode, String courseCode, GradeType gradeType, String gradeName) {
        this();
        this.studentCode = studentCode;
        this.courseCode = courseCode;
        this.gradeType = gradeType;
        this.gradeName = gradeName;
    }

    // Getters and Setters
    public int getGradeId() {
        return gradeId;
    }

    public void setGradeId(int gradeId) {
        this.gradeId = gradeId;
    }

    public GradeType getGradeType() {
        return gradeType;
    }

    public void setGradeType(GradeType gradeType) {
        this.gradeType = gradeType;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Date getGradeDate() {
        return gradeDate;
    }

    public void setGradeDate(Date gradeDate) {
        this.gradeDate = gradeDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Related information getters/setters
    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    /**
     * Tính điểm quy đổi theo trọng số
     * 
     * @return Điểm đã quy đổi
     */
    public BigDecimal getWeightedScore() {
        if (score == null || maxScore == null || weight == null) {
            return BigDecimal.ZERO;
        }

        // weighted_score = (score / max_score) * weight * 10
        return score.divide(maxScore, 4, java.math.RoundingMode.HALF_UP)
                .multiply(weight)
                .multiply(new BigDecimal("10"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Tính điểm phần trăm
     * 
     * @return Điểm phần trăm (0-100)
     */
    public BigDecimal getPercentage() {
        if (score == null || maxScore == null) {
            return BigDecimal.ZERO;
        }

        return score.divide(maxScore, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Grade{" +
                "gradeId=" + gradeId +
                ", studentCode='" + studentCode + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", gradeType=" + gradeType +
                ", gradeName='" + gradeName + '\'' +
                ", score=" + score +
                ", maxScore=" + maxScore +
                ", weight=" + weight +
                ", gradeDate=" + gradeDate +
                '}';
    }
}
