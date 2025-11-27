package com.university.sms.dao;

import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho bảng classes
 * ✅ REFACTORED: Dùng faculty_code, teacher_username thay vì IDs
 */
public class ClassDAO {
  private static final Logger LOGGER = Logger.getLogger(ClassDAO.class.getName());

  /**
   * Lưu class (insert nếu chưa có ID, update nếu đã có ID)
   */
  public boolean save(com.university.sms.model.Class classEntity) {
    if (classEntity.getClassId() > 0) {
      // Check if exists
      com.university.sms.model.Class existing = findById(classEntity.getClassId());
      if (existing != null) {
        // Update existing class
        return update(classEntity);
      }
    }
    // Insert new class (có thể với ID từ CSV)
    return insertWithId(classEntity);
  }

  /**
   * ✅ REFACTORED: Insert class with specific ID (for CSV import)
   */
  private boolean insertWithId(com.university.sms.model.Class classEntity) {
    String sql = classEntity.getClassId() > 0
        ? "INSERT INTO classes (class_id, class_code, class_name, faculty_code, teacher_username, academic_year, semester, max_students) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        : "INSERT INTO classes (class_code, class_name, faculty_code, teacher_username, academic_year, semester, max_students) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      int paramIndex = 1;
      if (classEntity.getClassId() > 0) {
        stmt.setInt(paramIndex++, classEntity.getClassId());
      }

      stmt.setString(paramIndex++, classEntity.getClassCode());
      stmt.setString(paramIndex++, classEntity.getClassName());
      stmt.setString(paramIndex++, classEntity.getFacultyCode());

      if (classEntity.getTeacherUsername() != null && !classEntity.getTeacherUsername().isEmpty()) {
        stmt.setString(paramIndex++, classEntity.getTeacherUsername());
      } else {
        stmt.setNull(paramIndex++, Types.VARCHAR);
      }

      stmt.setString(paramIndex++, classEntity.getAcademicYear());
      stmt.setInt(paramIndex++, classEntity.getSemester());

      if (classEntity.getMaxStudents() != null) {
        stmt.setInt(paramIndex++, classEntity.getMaxStudents());
      } else {
        stmt.setNull(paramIndex++, Types.INTEGER);
      }

      int result = stmt.executeUpdate();

      if (result > 0) {
        if (classEntity.getClassId() == 0) {
          try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
              classEntity.setClassId(rs.getInt(1));
            }
          }
        }
        LOGGER.info("Class inserted successfully: " + classEntity.getClassCode());
        return true;
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error inserting class: " + classEntity.getClassCode(), e);
    }

    return false;
  }

  /**
   * ✅ REFACTORED: Thêm class mới
   */
  public boolean insert(com.university.sms.model.Class classEntity) {
    String sql = "INSERT INTO classes (class_code, class_name, faculty_code, teacher_username, " +
        "academic_year, semester, max_students) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setString(1, classEntity.getClassCode());
      stmt.setString(2, classEntity.getClassName());
      stmt.setString(3, classEntity.getFacultyCode());

      if (classEntity.getTeacherUsername() != null && !classEntity.getTeacherUsername().isEmpty()) {
        stmt.setString(4, classEntity.getTeacherUsername());
      } else {
        stmt.setNull(4, Types.VARCHAR);
      }

      stmt.setString(5, classEntity.getAcademicYear());
      stmt.setInt(6, classEntity.getSemester());

      if (classEntity.getMaxStudents() != null) {
        stmt.setInt(7, classEntity.getMaxStudents());
      } else {
        stmt.setNull(7, Types.INTEGER);
      }

      int result = stmt.executeUpdate();

      if (result > 0) {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
          if (rs.next()) {
            classEntity.setClassId(rs.getInt(1));
          }
        }
        LOGGER.info("Class inserted successfully: " + classEntity.getClassCode());
        return true;
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error inserting class: " + classEntity.getClassCode(), e);
    }

    return false;
  }

  /**
   * ✅ REFACTORED: Cập nhật class
   */
  public boolean update(com.university.sms.model.Class classEntity) {
    String sql = "UPDATE classes SET class_code = ?, class_name = ?, faculty_code = ?, " +
        "teacher_username = ?, academic_year = ?, semester = ?, max_students = ? " +
        "WHERE class_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, classEntity.getClassCode());
      stmt.setString(2, classEntity.getClassName());
      stmt.setString(3, classEntity.getFacultyCode());

      if (classEntity.getTeacherUsername() != null && !classEntity.getTeacherUsername().isEmpty()) {
        stmt.setString(4, classEntity.getTeacherUsername());
      } else {
        stmt.setNull(4, Types.VARCHAR);
      }

      stmt.setString(5, classEntity.getAcademicYear());
      stmt.setInt(6, classEntity.getSemester());

      if (classEntity.getMaxStudents() != null) {
        stmt.setInt(7, classEntity.getMaxStudents());
      } else {
        stmt.setNull(7, Types.INTEGER);
      }

      stmt.setInt(8, classEntity.getClassId());

      int result = stmt.executeUpdate();

      if (result > 0) {
        LOGGER.info("Class updated successfully: " + classEntity.getClassId());
        return true;
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error updating class: " + classEntity.getClassId(), e);
    }

    return false;
  }

  /**
   * ✅ REFACTORED: Lấy tất cả classes
   */
  public List<com.university.sms.model.Class> findAll() {
    String sql = "SELECT c.*, f.faculty_name, u.full_name AS teacher_name " +
        "FROM classes c " +
        "LEFT JOIN faculties f ON c.faculty_code = f.faculty_code " +
        "LEFT JOIN users u ON c.teacher_username = u.username " +
        "ORDER BY c.academic_year DESC, c.semester DESC, c.class_name";

    List<com.university.sms.model.Class> classes = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        classes.add(mapResultSetToClass(rs));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error finding all classes", e);
    }

    return classes;
  }

  /**
   * Lấy danh sách lớp còn trống (chưa đầy)
   * 
   * @param facultyCode Nếu không null, chỉ lấy lớp của khoa này
   * @return Danh sách lớp còn trống
   */
  public List<com.university.sms.model.Class> findAvailableClasses(String facultyCode) {
    String sql = "SELECT c.*, f.faculty_name, u.full_name AS teacher_name, " +
        "COALESCE(COUNT(s.student_id), 0) AS current_student_count " +
        "FROM classes c " +
        "LEFT JOIN faculties f ON c.faculty_code = f.faculty_code " +
        "LEFT JOIN users u ON c.teacher_username = u.username " +
        "LEFT JOIN students s ON c.class_code = s.class_code " +
        (facultyCode != null && !facultyCode.trim().isEmpty() ? "WHERE c.faculty_code = ? " : "") +
        "GROUP BY c.class_id, c.class_code, c.class_name, c.faculty_code, c.teacher_username, " +
        "c.academic_year, c.semester, c.max_students, c.created_at, f.faculty_name, u.full_name " +
        "HAVING (c.max_students IS NULL OR COALESCE(COUNT(s.student_id), 0) < c.max_students) " +
        "ORDER BY c.academic_year DESC, c.semester DESC, c.class_name";

    List<com.university.sms.model.Class> classes = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      if (facultyCode != null && !facultyCode.trim().isEmpty()) {
        stmt.setString(1, facultyCode);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          com.university.sms.model.Class classEntity = mapResultSetToClass(rs);
          classes.add(classEntity);
        }
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error finding available classes", e);
    }

    return classes;
  }

  /**
   * Tìm class theo ID
   */
  public com.university.sms.model.Class findById(int classId) {
    String sql = "SELECT c.*, f.faculty_name, u.full_name AS teacher_name " +
        "FROM classes c " +
        "LEFT JOIN faculties f ON c.faculty_code = f.faculty_code " +
        "LEFT JOIN users u ON c.teacher_username = u.username " +
        "WHERE c.class_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, classId);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapResultSetToClass(rs);
        }
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error finding class by ID: " + classId, e);
    }

    return null;
  }

  /**
   * Tìm class theo class_code
   */
  public com.university.sms.model.Class findByCode(String classCode) {
    String sql = "SELECT c.*, f.faculty_name, u.full_name AS teacher_name " +
        "FROM classes c " +
        "LEFT JOIN faculties f ON c.faculty_code = f.faculty_code " +
        "LEFT JOIN users u ON c.teacher_username = u.username " +
        "WHERE c.class_code = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, classCode);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapResultSetToClass(rs);
        }
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error finding class by code: " + classCode, e);
    }

    return null;
  }

  /**
   * Xóa class
   */
  public boolean delete(int classId) {
    String sql = "DELETE FROM classes WHERE class_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, classId);

      int result = stmt.executeUpdate();

      if (result > 0) {
        LOGGER.info("Class deleted successfully: " + classId);
        return true;
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error deleting class: " + classId, e);
    }

    return false;
  }

  /**
   * ✅ REFACTORED: Map ResultSet to Class object
   */
  private com.university.sms.model.Class mapResultSetToClass(ResultSet rs) throws SQLException {
    com.university.sms.model.Class classEntity = new com.university.sms.model.Class();
    classEntity.setClassId(rs.getInt("class_id"));
    classEntity.setClassCode(rs.getString("class_code"));
    classEntity.setClassName(rs.getString("class_name"));
    classEntity.setFacultyCode(rs.getString("faculty_code"));

    // Set faculty name if available
    try {
      String facultyName = rs.getString("faculty_name");
      if (facultyName != null) {
        classEntity.setFacultyName(facultyName);
      }
    } catch (SQLException e) {
      // Column may not exist in some queries
    }

    String teacherUsername = rs.getString("teacher_username");
    if (!rs.wasNull()) {
      classEntity.setTeacherUsername(teacherUsername);
    }

    // Set teacher name if available
    try {
      String teacherName = rs.getString("teacher_name");
      if (teacherName != null) {
        classEntity.setTeacherName(teacherName);
      }
    } catch (SQLException e) {
      // Column may not exist in some queries
    }

    classEntity.setAcademicYear(rs.getString("academic_year"));
    classEntity.setSemester(rs.getInt("semester"));

    int maxStudents = rs.getInt("max_students");
    if (!rs.wasNull()) {
      classEntity.setMaxStudents(maxStudents);
    }

    classEntity.setCreatedAt(rs.getTimestamp("created_at"));

    return classEntity;
  }
}
