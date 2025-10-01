package com.university.sms.model;

import java.sql.Timestamp;

/**
 * Model class cho bảng subjects
 */
public class Subject implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private int departmentId;
    private Integer prerequisiteSubjectId;
    private String description;
    private boolean isRequired;
    private Timestamp createdAt;

    // Department information (from join)
    private String departmentName;
    // Prerequisite subject information (from join)
    private String prerequisiteSubjectName;

    // Constructors
    public Subject() {
        this.credits = 3;
        this.isRequired = true;
    }

    public Subject(String subjectCode, String subjectName, int credits, int departmentId) {
        this();
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.departmentId = departmentId;
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

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getPrerequisiteSubjectId() {
        return prerequisiteSubjectId;
    }

    public void setPrerequisiteSubjectId(Integer prerequisiteSubjectId) {
        this.prerequisiteSubjectId = prerequisiteSubjectId;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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
                ", departmentName='" + departmentName + '\'' +
                '}';
    }
}
