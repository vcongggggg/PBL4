package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model cho yêu cầu mở lớp từ giảng viên
 */
public class ClassOpeningRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int requestId;
    private int teacherId;
    private int subjectId;
    private String academicYear;
    private int semester;
    private String scheduleDay;
    private String scheduleTime;
    private String room;
    private int maxStudents;
    private String reason;
    private RequestStatus requestStatus;
    private String adminNote;
    private Integer approvedBy;
    private Timestamp requestDate;
    private Timestamp decisionDate;
    private Timestamp createdAt;

    // Related information (from joins)
    private String teacherName;
    private String subjectName;
    private String subjectCode;
    private int credits;
    private String approverName;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    // Constructors
    public ClassOpeningRequest() {
        this.requestStatus = RequestStatus.PENDING;
        this.maxStudents = 50;
    }

    public ClassOpeningRequest(int teacherId, int subjectId, String academicYear, int semester) {
        this();
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    // Getters and Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public Timestamp getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(Timestamp decisionDate) {
        this.decisionDate = decisionDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Related information getters/setters
    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    @Override
    public String toString() {
        return "ClassOpeningRequest{" +
                "requestId=" + requestId +
                ", teacherId=" + teacherId +
                ", subjectName='" + subjectName + '\'' +
                ", academicYear='" + academicYear + '\'' +
                ", semester=" + semester +
                ", requestStatus=" + requestStatus +
                '}';
    }
}





