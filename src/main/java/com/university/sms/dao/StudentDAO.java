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
 * ✅ REFACTORED: Dùng username, class_code, faculty_code thay vì IDs
 */
public class StudentDAO {
    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    /**
     * ✅ REFACTORED: Thêm sinh viên mới (dùng codes)
     */
    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (username, student_code, class_code, faculty_code, admission_year, " +
                "birth_date, gender, citizen_id, emergency_contact, emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, student.getUsername());
            stmt.setString(2, student.getStudentCode());
            if (student.getClassCode() != null && !student.getClassCode().isEmpty()) {
                stmt.setString(3, student.getClassCode());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            stmt.setString(4, student.getFacultyCode());
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
     * ✅ REFACTORED: Lưu student (insert or update)
     */
    public boolean save(Student student) {
        if (student.getStudentId() > 0) {
            // Check if exists
            Student existing = findById(student.getStudentId());
            if (existing != null) {
                return updateStudent(student);
            }
        }
        // Insert new student with ID from CSV
        return insertWithId(student);
    }

    /**
     * ✅ NEW: Insert student with specific ID (for CSV import)
     */
    private boolean insertWithId(Student student) {
        String sql = student.getStudentId() > 0
                ? "INSERT INTO students (student_id, username, student_code, class_code, faculty_code, admission_year, birth_date, gender, citizen_id, emergency_contact, emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO students (username, student_code, class_code, faculty_code, admission_year, birth_date, gender, citizen_id, emergency_contact, emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (student.getStudentId() > 0) {
                stmt.setInt(paramIndex++, student.getStudentId());
            }

            stmt.setString(paramIndex++, student.getUsername());
            stmt.setString(paramIndex++, student.getStudentCode());
            if (student.getClassCode() != null && !student.getClassCode().isEmpty()) {
                stmt.setString(paramIndex++, student.getClassCode());
            } else {
                stmt.setNull(paramIndex++, Types.VARCHAR);
            }
            stmt.setString(paramIndex++, student.getFacultyCode());
            stmt.setInt(paramIndex++, student.getAdmissionYear());
            stmt.setDate(paramIndex++, student.getBirthDate());
            if (student.getGender() != null) {
                stmt.setString(paramIndex++, student.getGender().name().toLowerCase());
            } else {
                stmt.setNull(paramIndex++, Types.VARCHAR);
            }
            stmt.setString(paramIndex++, student.getCitizenId());
            stmt.setString(paramIndex++, student.getEmergencyContact());
            stmt.setString(paramIndex++, student.getEmergencyPhone());

            int result = stmt.executeUpdate();

            if (result > 0) {
                if (student.getStudentId() == 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            student.setStudentId(rs.getInt(1));
                        }
                    }
                }
                LOGGER.info("Student inserted successfully: " + student.getStudentCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting student: " + student.getStudentCode(), e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Thêm mới hoặc cập nhật nếu đã tồn tại theo student_code
     */
    public boolean addOrUpdate(Student student) {
        try {
            Student existing = findByStudentCode(student.getStudentCode());
            if (existing == null) {
                return addStudent(student);
            }
            // Gán id để cập nhật các trường mutable cơ bản
            student.setStudentId(existing.getStudentId());
            return updateStudent(student);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error addOrUpdate student: " + student.getStudentCode(), e);
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Tìm sinh viên theo username
     */
    public Student findByUsername(String username) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "WHERE s.username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by username: " + username, e);
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Tìm sinh viên theo ID
     */
    public Student findById(int studentId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
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
     * ✅ REFACTORED: Tìm sinh viên theo mã sinh viên
     */
    public Student findByStudentCode(String studentCode) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
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
     * ✅ REFACTORED: Lấy danh sách sinh viên theo lớp (dùng class_code)
     */
    public List<Student> findByClassCode(String classCode) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "WHERE s.class_code = ? ORDER BY s.student_code";

        List<Student> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, classCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by class code: " + classCode, e);
        }

        return students;
    }

    /**
     * ✅ REFACTORED: Lấy danh sách sinh viên theo khoa (dùng faculty_code)
     */
    public List<Student> findByFacultyCode(String facultyCode) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "WHERE s.faculty_code = ? ORDER BY s.student_code";

        List<Student> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, facultyCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by faculty code: " + facultyCode, e);
        }

        return students;
    }

    /**
     * ✅ REFACTORED: Lấy tất cả sinh viên (chỉ active)
     */
    public List<Student> findAll() {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, u.is_active, f.faculty_name, c.class_name "
                +
                ", dor.source AS data_source " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "LEFT JOIN data_origin dor ON dor.entity_type = 'student' AND dor.entity_id = s.student_id " +
                "WHERE u.is_active = TRUE " +
                "ORDER BY CASE WHEN dor.source = 'CSV' THEN 0 ELSE 1 END, COALESCE(dor.source,'ZZZ'), s.student_code";

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
     * ✅ REFACTORED: Lấy tất cả sinh viên (bao gồm cả đã vô hiệu hóa)
     */
    public List<Student> findAllIncludeInactive() {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, u.is_active, f.faculty_name, c.class_name "
                +
                ", dor.source AS data_source " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "LEFT JOIN data_origin dor ON dor.entity_type = 'student' AND dor.entity_id = s.student_id " +
                "ORDER BY u.is_active DESC, CASE WHEN dor.source = 'CSV' THEN 0 ELSE 1 END, COALESCE(dor.source,'ZZZ'), s.student_code";

        List<Student> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all students (include inactive)", e);
        }

        return students;
    }

    /**
     * ✅ REFACTORED: Tìm kiếm sinh viên theo từ khóa
     */
    public List<Student> searchStudents(String keyword) {
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, f.faculty_name, c.class_name " +
                "FROM students s " +
                "JOIN users u ON s.username = u.username " +
                "JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
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
     * ✅ REFACTORED: Cập nhật thông tin sinh viên
     */
    public boolean updateStudent(Student student) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Determine active status based on student status (only ACTIVE students keep
            // account active)
            boolean shouldBeActive = student.getStudentStatus() == null
                    || student.getStudentStatus() == Student.StudentStatus.ACTIVE;

            // Update user information first (full_name, email, phone, is_active)
            String userSql = "UPDATE users SET full_name = ?, email = ?, phone = ?, is_active = ? WHERE username = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setString(1, student.getFullName());
                userStmt.setString(2, student.getEmail());
                userStmt.setString(3, student.getPhone());
                userStmt.setBoolean(4, shouldBeActive);
                userStmt.setString(5, student.getUsername());
                userStmt.executeUpdate();
            }

            // Update student information
            String studentSql = "UPDATE students SET class_code = ?, admission_year = ?, birth_date = ?, " +
                    "gender = ?, citizen_id = ?, emergency_contact = ?, emergency_phone = ?, " +
                    "student_status = ? WHERE student_id = ?";

            try (PreparedStatement studentStmt = conn.prepareStatement(studentSql)) {
                if (student.getClassCode() != null && !student.getClassCode().isEmpty()) {
                    studentStmt.setString(1, student.getClassCode());
                } else {
                    studentStmt.setNull(1, Types.VARCHAR);
                }
                studentStmt.setInt(2, student.getAdmissionYear());
                studentStmt.setDate(3, student.getBirthDate());

                if (student.getGender() != null) {
                    studentStmt.setString(4, student.getGender().name().toLowerCase());
                } else {
                    studentStmt.setNull(4, Types.VARCHAR);
                }

                studentStmt.setString(5, student.getCitizenId());
                studentStmt.setString(6, student.getEmergencyContact());
                studentStmt.setString(7, student.getEmergencyPhone());

                if (student.getStudentStatus() != null) {
                    studentStmt.setString(8, student.getStudentStatus().name().toLowerCase());
                } else {
                    studentStmt.setNull(8, Types.VARCHAR);
                }

                studentStmt.setInt(9, student.getStudentId());
                studentStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Student updated successfully: " + student.getStudentCode());
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student: " + student.getStudentId(), e);
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }

        return false;
    }

    /**
     * Xóa sinh viên theo ID
     */
    public boolean deleteStudent(int studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            int result = stmt.executeUpdate();
            if (result > 0) {
                LOGGER.info("Student deleted successfully: " + studentId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting student: " + studentId, e);
        }
        return false;
    }

    /**
     * Cập nhật trạng thái sinh viên
     */
    public boolean updateStudentStatus(int studentId, Student.StudentStatus status) {
        String studentSql = "UPDATE students SET student_status = ? WHERE student_id = ?";
        String selectUsernameSql = "SELECT username FROM students WHERE student_id = ?";
        String userSql = "UPDATE users SET is_active = ? WHERE username = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(studentSql)) {
                stmt.setString(1, status.name().toLowerCase());
                stmt.setInt(2, studentId);
                int result = stmt.executeUpdate();
                if (result == 0) {
                    conn.rollback();
                    return false;
                }
            }

            String username = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectUsernameSql)) {
                selectStmt.setInt(1, studentId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("username");
                    }
                }
            }

            if (username != null && !username.isEmpty()) {
                boolean shouldBeActive = status == Student.StudentStatus.ACTIVE;
                try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                    userStmt.setBoolean(1, shouldBeActive);
                    userStmt.setString(2, username);
                    userStmt.executeUpdate();
                }
            }

            conn.commit();
            LOGGER.info("Student status updated successfully: " + studentId + " -> " + status);
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student status: " + studentId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
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
     * ✅ REFACTORED: Map ResultSet to Student object
     */
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getInt("student_id"));
        student.setUsername(rs.getString("username"));
        student.setStudentCode(rs.getString("student_code"));

        String classCode = rs.getString("class_code");
        if (!rs.wasNull()) {
            student.setClassCode(classCode);
        }

        student.setFacultyCode(rs.getString("faculty_code"));
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

        // Check if is_active column exists in result set
        try {
            student.setActive(rs.getBoolean("is_active"));
        } catch (SQLException e) {
            // If column doesn't exist, default to true
            student.setActive(true);
        }

        // Faculty and Class information
        student.setFacultyName(rs.getString("faculty_name"));
        student.setClassName(rs.getString("class_name"));

        return student;
    }

    /**
     * Lấy tổng số lượng sinh viên
     */
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM students";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total student count", e);
        }

        return 0;
    }
}
