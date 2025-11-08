package com.university.sms.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model class cho bảng courses
 * ✅ REFACTORED: Dùng subject_code, teacher_username, class_code làm FK
 * (client-safe)
 */
public class Course implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int courseId;

    // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
    private String courseCode; // UNIQUE identifier for course
    private String subjectCode; // FK to subjects.subject_code
    private String teacherUsername; // FK to users.username (teacher)
    private String classCode; // FK to classes.class_code

    // ⚠️ DEPRECATED: Giữ lại để backward compatibility
    @Deprecated
    private int subjectId; // Legacy field, use subjectCode instead
    @Deprecated
    private int teacherId; // Legacy field, use teacherUsername instead
    @Deprecated
    private Integer classId; // Legacy field, use classCode instead

    // Course data
    private String academicYear;
    private int semester;
    private String scheduleDay;
    private String scheduleTime;
    private String room;
    private int maxStudents;
    private int currentStudents;
    private CourseStatus courseStatus;
    private Date startDate;
    private Date endDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String weeks; // e.g., "1-16" or "1-8,10-16"

    // Related information (from joins)
    private String subjectName;
    private int credits;
    private String teacherName;
    private String className;

    public enum CourseStatus {
        PLANNING, ONGOING, COMPLETED, CANCELLED
    }

    // Constructors
    public Course() {
        this.maxStudents = 50;
        this.currentStudents = 0;
        this.courseStatus = CourseStatus.PLANNING;
    }

    public Course(String courseCode, String subjectCode, String teacherUsername, String academicYear, int semester) {
        this();
        this.courseCode = courseCode;
        this.subjectCode = subjectCode;
        this.teacherUsername = teacherUsername;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    // Legacy constructor (deprecated)
    @Deprecated
    public Course(String courseCode, int subjectId, int teacherId, String academicYear, int semester) {
        this();
        this.courseCode = courseCode;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTeacherUsername() {
        return teacherUsername;
    }

    public void setTeacherUsername(String teacherUsername) {
        this.teacherUsername = teacherUsername;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    // Deprecated getters/setters (keep for backward compat)
    @Deprecated
    public int getSubjectId() {
        return subjectId;
    }

    @Deprecated
    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    @Deprecated
    public int getTeacherId() {
        return teacherId;
    }

    @Deprecated
    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    @Deprecated
    public Integer getClassId() {
        return classId;
    }

    @Deprecated
    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getScheduleDay() {
        return scheduleDay;
    }

    public void setScheduleDay(String scheduleDay) {
        this.scheduleDay = scheduleDay;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(int maxStudents) {
        this.maxStudents = maxStudents;
    }

    public int getCurrentStudents() {
        return currentStudents;
    }

    public void setCurrentStudents(int currentStudents) {
        this.currentStudents = currentStudents;
    }

    public CourseStatus getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(CourseStatus courseStatus) {
        this.courseStatus = courseStatus;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getWeeks() {
        return weeks;
    }

    public void setWeeks(String weeks) {
        this.weeks = weeks;
    }

    // Alias methods for compatibility
    public String getCourseName() {
        return subjectName;
    }

    public void setCourseName(String courseName) {
        this.subjectName = courseName;
    }

    public int getCurrentEnrollment() {
        return currentStudents;
    }

    public void setCurrentEnrollment(int currentEnrollment) {
        this.currentStudents = currentEnrollment;
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId=" + courseId +
                ", courseCode='" + courseCode + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", academicYear='" + academicYear + '\'' +
                ", semester=" + semester +
                ", currentStudents=" + currentStudents +
                ", maxStudents=" + maxStudents +
                '}';
    }
}
