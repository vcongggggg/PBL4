package com.university.sms.model;

import java.sql.Timestamp;

/**
 * Model class cho bảng departments
 */
public class Department implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int departmentId;
    private String departmentCode;
    private String departmentName;
    private String description;
    private Integer headTeacherId;
    private Timestamp createdAt;

    // Teacher information (from join)
    private String headTeacherName;

    // Constructors
    public Department() {}

    public Department(String departmentCode, String departmentName, String description) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.description = description;
    }

    // Getters and Setters
    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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
        return "Department{" +
                "departmentId=" + departmentId +
                ", departmentCode='" + departmentCode + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", headTeacherName='" + headTeacherName + '\'' +
                '}';
    }
}
