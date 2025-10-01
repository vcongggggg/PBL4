package com.university.sms.dao;

import com.university.sms.model.Student;
import com.university.sms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Student
 */
public class StudentDAO {
    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    /**
     * Thêm sinh viên mới
     */
    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (user_id, student_code, class_id, department_id, admission_year, " +
                    "birth_date, gender, citizen_id, emergency_contact, emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, student.getUserId());
            stmt.setString(2, student.getStudentCode());
            if (student.getClassId() != null) {
                stmt.setInt(3, student.getClassId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setInt(4, student.getDepartmentId());
            stmt.setInt(5, student.getAdmissionYear());
            stmt.setDate(6, student.getBirthDate());
            if (student.getGender() != null) {
                stmt.setString(7, student.getGender().name().toLowerCase());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            stmt.setString(8, student.getCitizenId());
            stmt.setString(9, student.getEmergencyContact());
            stmt.setString(10, student.getEmergencyPhone());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        student.setStudentId(rs.getInt(1));
                    }
                }
                LOGGER.info("Student added successfully: " + student.getStudentCode());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding student: " + student.getStudentCode(), e);
        }
        
        return false;
    }

    /**
     * Tìm sinh viên theo ID
     */
    public Student findById(int studentId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.student_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by ID: " + studentId, e);
        }
        
        return null;
    }

    /**
     * Tìm sinh viên theo mã sinh viên
     */
    public Student findByStudentCode(String studentCode) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.student_code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, studentCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by code: " + studentCode, e);
        }
        
        return null;
    }

    /**
     * Tìm sinh viên theo user ID
     */
    public Student findByUserId(int userId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by user ID: " + userId, e);
        }
        
        return null;
    }

    /**
     * Lấy danh sách sinh viên theo lớp
     */
    public List<Student> findByClassId(int classId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.class_id = ? ORDER BY s.student_code";
        
        List<Student> students = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, classId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by class ID: " + classId, e);
        }
        
        return students;
    }

    /**
     * Lấy danh sách sinh viên theo khoa
     */
    public List<Student> findByDepartmentId(int departmentId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.department_id = ? ORDER BY s.student_code";
        
        List<Student> students = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, departmentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by department ID: " + departmentId, e);
        }
        
        return students;
    }

    /**
     * Lấy tất cả sinh viên
     */
    public List<Student> findAll() {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "ORDER BY s.student_code";
        
        List<Student> students = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all students", e);
        }
        
        return students;
    }

    /**
     * Tìm kiếm sinh viên theo từ khóa
     */
    public List<Student> searchStudents(String keyword) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, d.department_name, c.class_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN departments d ON s.department_id = d.department_id " +
                    "LEFT JOIN classes c ON s.class_id = c.class_id " +
                    "WHERE s.student_code LIKE ? OR u.full_name LIKE ? OR u.email LIKE ? " +
                    "ORDER BY s.student_code";
        
        List<Student> students = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching students with keyword: " + keyword, e);
        }
        
        return students;
    }

    /**
     * Cập nhật thông tin sinh viên
     */
    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET class_id = ?, birth_date = ?, gender = ?, citizen_id = ?, " +
                    "emergency_contact = ?, emergency_phone = ? WHERE student_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (student.getClassId() != null) {
                stmt.setInt(1, student.getClassId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setDate(2, student.getBirthDate());
            if (student.getGender() != null) {
                stmt.setString(3, student.getGender().name().toLowerCase());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            stmt.setString(4, student.getCitizenId());
            stmt.setString(5, student.getEmergencyContact());
            stmt.setString(6, student.getEmergencyPhone());
            stmt.setInt(7, student.getStudentId());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Student updated successfully: " + student.getStudentCode());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student: " + student.getStudentId(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật trạng thái sinh viên
     */
    public boolean updateStudentStatus(int studentId, Student.StudentStatus status) {
        String sql = "UPDATE students SET student_status = ? WHERE student_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, studentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Student status updated successfully: " + studentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student status: " + studentId, e);
        }
        
        return false;
    }

    /**
     * Cập nhật GPA và tổng tín chỉ
     */
    public boolean updateGpaAndCredits(int studentId, BigDecimal gpa, int totalCredits) {
        String sql = "UPDATE students SET gpa = ?, total_credits = ? WHERE student_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, gpa);
            stmt.setInt(2, totalCredits);
            stmt.setInt(3, studentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Student GPA and credits updated successfully: " + studentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student GPA and credits: " + studentId, e);
        }
        
        return false;
    }

    /**
     * Map ResultSet to Student object
     */
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getInt("student_id"));
        student.setUserId(rs.getInt("user_id"));
        student.setStudentCode(rs.getString("student_code"));
        
        int classId = rs.getInt("class_id");
        if (!rs.wasNull()) {
            student.setClassId(classId);
        }
        
        student.setDepartmentId(rs.getInt("department_id"));
        student.setAdmissionYear(rs.getInt("admission_year"));
        
        String status = rs.getString("student_status");
        if (status != null) {
            student.setStudentStatus(Student.StudentStatus.valueOf(status.toUpperCase()));
        }
        
        student.setGpa(rs.getBigDecimal("gpa"));
        student.setTotalCredits(rs.getInt("total_credits"));
        student.setBirthDate(rs.getDate("birth_date"));
        
        String gender = rs.getString("gender");
        if (gender != null) {
            student.setGender(Student.Gender.valueOf(gender.toUpperCase()));
        }
        
        student.setCitizenId(rs.getString("citizen_id"));
        student.setEmergencyContact(rs.getString("emergency_contact"));
        student.setEmergencyPhone(rs.getString("emergency_phone"));
        student.setCreatedAt(rs.getTimestamp("created_at"));
        
        // User information
        student.setFullName(rs.getString("full_name"));
        student.setEmail(rs.getString("email"));
        student.setPhone(rs.getString("phone"));
        student.setAddress(rs.getString("address"));
        
        return student;
    }
}
