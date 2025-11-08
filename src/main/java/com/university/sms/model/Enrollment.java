package com.university.sms.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model class cho bảng enrollments
 * ✅ REFACTORED: Dùng student_code và course_code làm FK (client-safe)
 */
public class Enrollment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int enrollmentId;

    // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
    private String studentCode; // FK to students.student_code
    private String courseCode; // FK to courses.course_code

    // ⚠️ DEPRECATED: Giữ lại để backward compatibility, nhưng không dùng làm FK
    @Deprecated
    private int studentId; // Legacy field, use studentCode instead
    @Deprecated
    private int courseId; // Legacy field, use courseCode instead

    // Enrollment data
    private Timestamp enrollmentDate;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal regularGrade; // Điểm bài tập
    private BigDecimal midtermGrade; // Điểm giữa kỳ (GK)
    private BigDecimal finalExamGrade; // Điểm cuối kỳ (CK)
    private BigDecimal finalGrade; // Điểm tổng kết
    private String letterGrade;
    private BigDecimal gradePoints;
    private BigDecimal attendanceRate;

    // Related information (from joins)
    private String studentName;
    private String subjectName;
    private int credits;

    public enum EnrollmentStatus {
        ENROLLED, COMPLETED, DROPPED, FAILED
    }

    // Constructors
    public Enrollment() {
        this.enrollmentStatus = EnrollmentStatus.ENROLLED;
        this.attendanceRate = BigDecimal.ZERO;
    }

    public Enrollment(String studentCode, String courseCode) {
        this();
        this.studentCode = studentCode;
        this.courseCode = courseCode;
    }

    // Legacy constructor (deprecated)
    @Deprecated
    public Enrollment(int studentId, int courseId) {
        this();
        this.studentId = studentId;
        this.courseId = courseId;
    }

    // Getters and Setters
    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public Timestamp getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(Timestamp enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public EnrollmentStatus getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public BigDecimal getRegularGrade() {
        return regularGrade;
    }

    public void setRegularGrade(BigDecimal regularGrade) {
        this.regularGrade = regularGrade;
    }

    public BigDecimal getMidtermGrade() {
        return midtermGrade;
    }

    public void setMidtermGrade(BigDecimal midtermGrade) {
        this.midtermGrade = midtermGrade;
    }

    public BigDecimal getFinalExamGrade() {
        return finalExamGrade;
    }

    public void setFinalExamGrade(BigDecimal finalExamGrade) {
        this.finalExamGrade = finalExamGrade;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public BigDecimal getGradePoints() {
        return gradePoints;
    }

    public void setGradePoints(BigDecimal gradePoints) {
        this.gradePoints = gradePoints;
    }

    public BigDecimal getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(BigDecimal attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "enrollmentId=" + enrollmentId +
                ", studentCode='" + studentCode + '\'' +
                ", studentName='" + studentName + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", finalGrade=" + finalGrade +
                ", letterGrade='" + letterGrade + '\'' +
                ", enrollmentStatus=" + enrollmentStatus +
                '}';
    }
}
