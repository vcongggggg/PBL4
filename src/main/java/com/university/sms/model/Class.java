package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Class implements Serializable {
  private static final long serialVersionUID = 1L;

  private int classId;

  private String classCode;
  private String className;
  private String facultyCode;
  private String teacherUsername; // FK to users.username (teacher)

  // Class data
  private String academicYear;
  private int semester;
  private Integer maxStudents;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  // Thông tin join (không lưu trong DB)
  private String facultyName;
  private String teacherName;

  public Class() {
  }

  public Class(String classCode, String className, String facultyCode,
      String teacherUsername, String academicYear, int semester, Integer maxStudents) {
    this.classCode = classCode;
    this.className = className;
    this.facultyCode = facultyCode;
    this.teacherUsername = teacherUsername;
    this.academicYear = academicYear;
    this.semester = semester;
    this.maxStudents = maxStudents;
  }

  // Getters and Setters
  public int getClassId() {
    return classId;
  }

  public void setClassId(int classId) {
    this.classId = classId;
  }

  public String getClassCode() {
    return classCode;
  }

  public void setClassCode(String classCode) {
    this.classCode = classCode;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getFacultyCode() {
    return facultyCode;
  }

  public void setFacultyCode(String facultyCode) {
    this.facultyCode = facultyCode;
  }

  public String getTeacherUsername() {
    return teacherUsername;
  }

  public void setTeacherUsername(String teacherUsername) {
    this.teacherUsername = teacherUsername;
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

  public Integer getMaxStudents() {
    return maxStudents;
  }

  public void setMaxStudents(Integer maxStudents) {
    this.maxStudents = maxStudents;
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

  public String getTeacherName() {
    return teacherName;
  }

  public void setTeacherName(String teacherName) {
    this.teacherName = teacherName;
  }

  @Override
  public String toString() {
    return "Class{" +
        "classId=" + classId +
        ", classCode='" + classCode + '\'' +
        ", className='" + className + '\'' +
        ", facultyCode=" + facultyCode +
        ", teacherUsername=" + teacherUsername +
        ", academicYear='" + academicYear + '\'' +
        ", semester=" + semester +
        ", maxStudents=" + maxStudents +
        ", createdAt=" + createdAt +
        '}';
  }
}
