package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model cho đăng ký học phần của sinh viên
 * ✅ REFACTORED: Dùng student_code và course_code làm FK (client-safe)
 */
public class CourseRegistration implements Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int registrationId;

    // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
    private String studentCode; // FK to students.student_code
    private String courseCode; // FK to courses.course_code

    // Registration data
    private Timestamp registrationDate;
    private RegistrationStatus registrationStatus;
    private Timestamp cancelDate;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Related information (from joins)
    private String studentName;
    private String subjectName;
    private String teacherName;
    private String scheduleDay;
    private String scheduleTime;
    private String room;
    private int credits;

    public enum RegistrationStatus {
        PENDING, APPROVED, CANCELLED
    }

    // Constructors
    public CourseRegistration() {
        this.registrationStatus = RegistrationStatus.PENDING;
    }

    public CourseRegistration(String studentCode, String courseCode) {
        this();
        this.studentCode = studentCode;
        this.courseCode = courseCode;
    }

    // Getters and Setters
    public int getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public Timestamp getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Timestamp registrationDate) {
        this.registrationDate = registrationDate;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public Timestamp getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(Timestamp cancelDate) {
        this.cancelDate = cancelDate;
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

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    @Override
    public String toString() {
        return "CourseRegistration{" +
                "registrationId=" + registrationId +
                ", studentCode='" + studentCode + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", registrationStatus=" + registrationStatus +
                '}';
    }
}
