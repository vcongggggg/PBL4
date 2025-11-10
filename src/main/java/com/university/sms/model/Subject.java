package com.university.sms.model;

import java.sql.Timestamp;

/**
 * Model class cho bảng subjects
 * ✅ REFACTORED: Dùng faculty_code, prerequisite_subject_code làm FK
 * (client-safe)
 */
public class Subject implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int subjectId;

    // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
    private String subjectCode; // UNIQUE identifier for subject
    private String subjectName;
    private int credits;
    private String facultyCode; // FK to faculties.faculty_code
    private String prerequisiteSubjectCode; // FK to subjects.subject_code

    private String description;
    private boolean isRequired;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Faculty information (from join)
    private String facultyName;
    // Prerequisite subject information (from join)
    private String prerequisiteSubjectName;

    // Constructors
    public Subject() {
        this.credits = 3;
        this.isRequired = true;
    }

    public Subject(String subjectCode, String subjectName, int credits, String facultyCode) {
        this();
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.facultyCode = facultyCode;
    }

    // Getters and Setters
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
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

    public String getFacultyCode() {
        return facultyCode;
    }

    public void setFacultyCode(String facultyCode) {
        this.facultyCode = facultyCode;
    }

    public String getPrerequisiteSubjectCode() {
        return prerequisiteSubjectCode;
    }

    public void setPrerequisiteSubjectCode(String prerequisiteSubjectCode) {
        this.prerequisiteSubjectCode = prerequisiteSubjectCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
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

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getPrerequisiteSubjectName() {
        return prerequisiteSubjectName;
    }

    public void setPrerequisiteSubjectName(String prerequisiteSubjectName) {
        this.prerequisiteSubjectName = prerequisiteSubjectName;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "subjectId=" + subjectId +
                ", subjectCode='" + subjectCode + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", credits=" + credits +
                ", facultyName='" + facultyName + '\'' +
                '}';
    }
}
