package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model cho bảng classes (lớp hành chính)
 * Note: Đặt tên Class (trùng với Java keyword) - cẩn thận khi import
 * ✅ REFACTORED: Dùng faculty_code, teacher_username làm FK (client-safe)
 */
public class Class implements Serializable {
  private static final long serialVersionUID = 1L;

  // Primary key
  private int classId;

  // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
  private String classCode; // UNIQUE identifier for class
  private String className;
  private String facultyCode; // FK to faculties.faculty_code
  private String teacherUsername; // FK to users.username (teacher)

  // ⚠️ DEPRECATED: Giữ lại để backward compatibility
  @Deprecated
  private int facultyId; // Legacy field, use facultyCode instead
  @Deprecated
  private Integer teacherId; // Legacy field, use teacherUsername instead

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

  // Legacy constructor (deprecated)
  @Deprecated
  public Class(int classId, String classCode, String className, int facultyId,
      Integer teacherId, String academicYear, int semester, Integer maxStudents) {
    this.classId = classId;
    this.classCode = classCode;
    this.className = className;
    this.facultyId = facultyId;
    this.teacherId = teacherId;
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

  // Deprecated getters/setters (keep for backward compat)
  @Deprecated
  public int getFacultyId() {
    return facultyId;
  }

  @Deprecated
  public void setFacultyId(int facultyId) {
    this.facultyId = facultyId;
  }

  @Deprecated
  public Integer getTeacherId() {
    return teacherId;
  }

  @Deprecated
  public void setTeacherId(Integer teacherId) {
    this.teacherId = teacherId;
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
        ", facultyId=" + facultyId +
        ", teacherId=" + teacherId +
        ", academicYear='" + academicYear + '\'' +
        ", semester=" + semester +
        ", maxStudents=" + maxStudents +
        ", createdAt=" + createdAt +
        '}';
  }
}
