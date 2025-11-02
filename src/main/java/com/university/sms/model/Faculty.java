package com.university.sms.model;

import java.sql.Timestamp;

/**
 * Model class cho bảng faculties
 */
public class Faculty implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int facultyId;
    private String facultyCode;
    private String facultyName;
    private String description;
    private Integer headTeacherId;
    private Timestamp createdAt;

    // Teacher information (from join)
    private String headTeacherName;

    // Constructors
    public Faculty() {}

    public Faculty(String facultyCode, String facultyName, String description) {
        this.facultyCode = facultyCode;
        this.facultyName = facultyName;
        this.description = description;
    }

    // Getters and Setters
    public int getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(int facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyCode() {
        return facultyCode;
    }

    public void setFacultyCode(String facultyCode) {
        this.facultyCode = facultyCode;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getHeadTeacherId() {
        return headTeacherId;
    }

    public void setHeadTeacherId(Integer headTeacherId) {
        this.headTeacherId = headTeacherId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getHeadTeacherName() {
        return headTeacherName;
    }

    public void setHeadTeacherName(String headTeacherName) {
        this.headTeacherName = headTeacherName;
    }

    @Override
    public String toString() {
        return "Faculty{" +
                "facultyId=" + facultyId +
                ", facultyCode='" + facultyCode + '\'' +
                ", facultyName='" + facultyName + '\'' +
                ", headTeacherName='" + headTeacherName + '\'' +
                '}';
    }
}

