package com.university.sms.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model class cho bảng students
 */
public class Student implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int studentId;
    private int userId;
    private String studentCode;
    private Integer classId;
    private int departmentId;
    private int admissionYear;
    private StudentStatus studentStatus;
    private BigDecimal gpa;
    private int totalCredits;
    private Date birthDate;
    private Gender gender;
    private String citizenId;
    private String emergencyContact;
    private String emergencyPhone;
    private Timestamp createdAt;

    // User information (from join)
    private String fullName;
    private String email;
    private String phone;
    private String address;

    public enum StudentStatus {
        ACTIVE, SUSPENDED, GRADUATED, DROPPED
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    // Constructors
    public Student() {
        this.gpa = BigDecimal.ZERO;
        this.totalCredits = 0;
        this.studentStatus = StudentStatus.ACTIVE;
    }

    public Student(int userId, String studentCode, int departmentId, int admissionYear) {
        this();
        this.userId = userId;
        this.studentCode = studentCode;
        this.departmentId = departmentId;
        this.admissionYear = admissionYear;
    }

    // Getters and Setters
    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(int admissionYear) {
        this.admissionYear = admissionYear;
    }

    public StudentStatus getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(StudentStatus studentStatus) {
        this.studentStatus = studentStatus;
    }

    public BigDecimal getGpa() {
        return gpa;
    }

    public void setGpa(BigDecimal gpa) {
        this.gpa = gpa;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(int totalCredits) {
        this.totalCredits = totalCredits;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", studentCode='" + studentCode + '\'' +
                ", fullName='" + fullName + '\'' +
                ", gpa=" + gpa +
                ", totalCredits=" + totalCredits +
                ", studentStatus=" + studentStatus +
                '}';
    }
}
