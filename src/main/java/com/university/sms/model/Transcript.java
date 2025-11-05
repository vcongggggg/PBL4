package com.university.sms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Model cho học bạ/bảng điểm tổng hợp của sinh viên
 */
public class Transcript implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int studentId;
    private String studentCode;
    private String studentName;
    private String facultyName;
    private String className;
    
    // Academic info
    private int totalCreditsEarned;      // Tổng tín chỉ đã đạt
    private int totalCreditsRegistered;  // Tổng tín chỉ đã đăng ký
    private BigDecimal cumulativeGPA;    // GPA tích lũy
    private BigDecimal semesterGPA;      // GPA học kỳ hiện tại
    private String academicRank;         // Xếp loại học tập
    
    // Statistics
    private int totalCoursesCompleted;
    private int totalCoursesFailed;
    private int totalCoursesInProgress;
    
    // Course records grouped by semester
    private List<SemesterRecord> semesterRecords;
    
    public Transcript() {
        this.semesterRecords = new ArrayList<>();
        this.cumulativeGPA = BigDecimal.ZERO;
        this.semesterGPA = BigDecimal.ZERO;
    }
    
    /**
     * Tính toán GPA và xếp loại
     */
    public void calculateGPA() {
        if (semesterRecords.isEmpty()) {
            this.cumulativeGPA = BigDecimal.ZERO;
            this.academicRank = "Chưa có dữ liệu";
            return;
        }
        
        BigDecimal totalWeightedPoints = BigDecimal.ZERO;
        int totalCredits = 0;
        
        for (SemesterRecord semester : semesterRecords) {
            for (CourseRecord course : semester.getCourses()) {
                if (course.getGradePoints() != null && course.getCredits() > 0) {
                    BigDecimal weightedPoint = course.getGradePoints()
                        .multiply(new BigDecimal(course.getCredits()));
                    totalWeightedPoints = totalWeightedPoints.add(weightedPoint);
                    totalCredits += course.getCredits();
                }
            }
        }
        
        if (totalCredits > 0) {
            this.cumulativeGPA = totalWeightedPoints
                .divide(new BigDecimal(totalCredits), 2, RoundingMode.HALF_UP);
            this.totalCreditsEarned = totalCredits;
        }
        
        // Xếp loại học tập
        this.academicRank = calculateAcademicRank(this.cumulativeGPA);
    }
    
    /**
     * Xếp loại học tập dựa trên GPA
     */
    private String calculateAcademicRank(BigDecimal gpa) {
        if (gpa.compareTo(new BigDecimal("3.60")) >= 0) {
            return "Xuất sắc";
        } else if (gpa.compareTo(new BigDecimal("3.20")) >= 0) {
            return "Giỏi";
        } else if (gpa.compareTo(new BigDecimal("2.50")) >= 0) {
            return "Khá";
        } else if (gpa.compareTo(new BigDecimal("2.00")) >= 0) {
            return "Trung bình";
        } else if (gpa.compareTo(BigDecimal.ZERO) > 0) {
            return "Yếu";
        } else {
            return "Chưa đánh giá";
        }
    }
    
    /**
     * Record cho một học kỳ
     */
    public static class SemesterRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String academicYear;
        private int semester;
        private List<CourseRecord> courses;
        private BigDecimal semesterGPA;
        private int creditsEarned;
        
        public SemesterRecord() {
            this.courses = new ArrayList<>();
        }
        
        public SemesterRecord(String academicYear, int semester) {
            this();
            this.academicYear = academicYear;
            this.semester = semester;
        }
        
        public void calculateSemesterGPA() {
            if (courses.isEmpty()) {
                this.semesterGPA = BigDecimal.ZERO;
                return;
            }
            
            BigDecimal totalWeightedPoints = BigDecimal.ZERO;
            int totalCredits = 0;
            int earnedCredits = 0;
            
            for (CourseRecord course : courses) {
                if (course.getGradePoints() != null && course.getCredits() > 0) {
                    BigDecimal weightedPoint = course.getGradePoints()
                        .multiply(new BigDecimal(course.getCredits()));
                    totalWeightedPoints = totalWeightedPoints.add(weightedPoint);
                    totalCredits += course.getCredits();
                    
                    // Only count passed courses
                    if (course.isPassed()) {
                        earnedCredits += course.getCredits();
                    }
                }
            }
            
            if (totalCredits > 0) {
                this.semesterGPA = totalWeightedPoints
                    .divide(new BigDecimal(totalCredits), 2, RoundingMode.HALF_UP);
            }
            this.creditsEarned = earnedCredits;
        }
        
        public String getDisplayName() {
            return String.format("Học kỳ %d - %s", semester, academicYear);
        }
        
        // Getters and Setters
        public String getAcademicYear() { return academicYear; }
        public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
        
        public int getSemester() { return semester; }
        public void setSemester(int semester) { this.semester = semester; }
        
        public List<CourseRecord> getCourses() { return courses; }
        public void setCourses(List<CourseRecord> courses) { this.courses = courses; }
        public void addCourse(CourseRecord course) { this.courses.add(course); }
        
        public BigDecimal getSemesterGPA() { return semesterGPA; }
        public void setSemesterGPA(BigDecimal semesterGPA) { this.semesterGPA = semesterGPA; }
        
        public int getCreditsEarned() { return creditsEarned; }
        public void setCreditsEarned(int creditsEarned) { this.creditsEarned = creditsEarned; }
    }
    
    /**
     * Record cho một môn học
     */
    public static class CourseRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String courseCode;
        private String subjectName;
        private int credits;
        private BigDecimal finalGrade;
        private String letterGrade;
        private BigDecimal gradePoints;
        private String status; // completed, failed, in_progress
        
        public CourseRecord() {}
        
        public CourseRecord(Enrollment enrollment, Course course) {
            this.courseCode = course.getCourseCode();
            this.subjectName = course.getSubjectName();
            this.credits = course.getCredits();
            this.finalGrade = enrollment.getFinalGrade();
            this.letterGrade = enrollment.getLetterGrade();
            this.gradePoints = enrollment.getGradePoints();
            this.status = enrollment.getEnrollmentStatus().name().toLowerCase();
        }
        
        public boolean isPassed() {
            return gradePoints != null && gradePoints.compareTo(BigDecimal.ZERO) > 0 
                && !"failed".equals(status);
        }
        
        // Getters and Setters
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        
        public int getCredits() { return credits; }
        public void setCredits(int credits) { this.credits = credits; }
        
        public BigDecimal getFinalGrade() { return finalGrade; }
        public void setFinalGrade(BigDecimal finalGrade) { this.finalGrade = finalGrade; }
        
        public String getLetterGrade() { return letterGrade; }
        public void setLetterGrade(String letterGrade) { this.letterGrade = letterGrade; }
        
        public BigDecimal getGradePoints() { return gradePoints; }
        public void setGradePoints(BigDecimal gradePoints) { this.gradePoints = gradePoints; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    // Getters and Setters
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public int getTotalCreditsEarned() { return totalCreditsEarned; }
    public void setTotalCreditsEarned(int totalCreditsEarned) { this.totalCreditsEarned = totalCreditsEarned; }
    
    public int getTotalCreditsRegistered() { return totalCreditsRegistered; }
    public void setTotalCreditsRegistered(int totalCreditsRegistered) { this.totalCreditsRegistered = totalCreditsRegistered; }
    
    public BigDecimal getCumulativeGPA() { return cumulativeGPA; }
    public void setCumulativeGPA(BigDecimal cumulativeGPA) { this.cumulativeGPA = cumulativeGPA; }
    
    public BigDecimal getSemesterGPA() { return semesterGPA; }
    public void setSemesterGPA(BigDecimal semesterGPA) { this.semesterGPA = semesterGPA; }
    
    public String getAcademicRank() { return academicRank; }
    public void setAcademicRank(String academicRank) { this.academicRank = academicRank; }
    
    public int getTotalCoursesCompleted() { return totalCoursesCompleted; }
    public void setTotalCoursesCompleted(int totalCoursesCompleted) { this.totalCoursesCompleted = totalCoursesCompleted; }
    
    public int getTotalCoursesFailed() { return totalCoursesFailed; }
    public void setTotalCoursesFailed(int totalCoursesFailed) { this.totalCoursesFailed = totalCoursesFailed; }
    
    public int getTotalCoursesInProgress() { return totalCoursesInProgress; }
    public void setTotalCoursesInProgress(int totalCoursesInProgress) { this.totalCoursesInProgress = totalCoursesInProgress; }
    
    public List<SemesterRecord> getSemesterRecords() { return semesterRecords; }
    public void setSemesterRecords(List<SemesterRecord> semesterRecords) { this.semesterRecords = semesterRecords; }
    public void addSemesterRecord(SemesterRecord record) { this.semesterRecords.add(record); }
}

