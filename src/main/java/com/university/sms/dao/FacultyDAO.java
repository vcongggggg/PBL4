package com.university.sms.dao;

import com.university.sms.model.Faculty;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Faculty
 */
public class FacultyDAO {
    private static final Logger LOGGER = Logger.getLogger(FacultyDAO.class.getName());

    /**
     * Lấy tất cả khoa
     */
    public List<Faculty> findAll() {
        String sql = "SELECT f.*, u.full_name AS head_teacher_name " +
                "FROM faculties f " +
                "LEFT JOIN users u ON f.head_teacher_username = u.username " +
                "ORDER BY f.faculty_name";

        List<Faculty> faculties = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                faculties.add(mapResultSetToFaculty(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all faculties", e);
        }

        return faculties;
    }

    /**
     * Tìm khoa theo ID
     */
    public Faculty findById(int facultyId) {
        String sql = "SELECT f.*, u.full_name AS head_teacher_name " +
                "FROM faculties f " +
                "LEFT JOIN users u ON f.head_teacher_username = u.username " +
                "WHERE f.faculty_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facultyId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFaculty(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding faculty by ID: " + facultyId, e);
        }

        return null;
    }

    /**
     * Tìm khoa theo mã khoa
     */
    public Faculty findByCode(String facultyCode) {
        String sql = "SELECT f.*, u.full_name AS head_teacher_name " +
                "FROM faculties f " +
                "LEFT JOIN users u ON f.head_teacher_username = u.username " +
                "WHERE f.faculty_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, facultyCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFaculty(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding faculty by code: " + facultyCode, e);
        }

        return null;
    }

    /**
     * Lưu khoa (insert nếu chưa có ID, update nếu đã có ID)
     */
    public boolean save(Faculty faculty) {
        if (faculty.getFacultyId() > 0) {
            // Check if exists
            Faculty existing = findById(faculty.getFacultyId());
            if (existing != null) {
                // Update existing faculty
                return updateFaculty(faculty);
            }
        }
        // Insert new faculty (có thể với ID từ CSV)
        return addFacultyWithId(faculty);
    }

    /**
     * Thêm khoa mới với ID cụ thể (cho CSV import)
     */
    private boolean addFacultyWithId(Faculty faculty) {
        String sql = faculty.getFacultyId() > 0
                ? "INSERT INTO faculties (faculty_id, faculty_code, faculty_name, description, head_teacher_username) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO faculties (faculty_code, faculty_name, description, head_teacher_username) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (faculty.getFacultyId() > 0) {
                stmt.setInt(paramIndex++, faculty.getFacultyId());
            }

            stmt.setString(paramIndex++, faculty.getFacultyCode());
            stmt.setString(paramIndex++, faculty.getFacultyName());
            stmt.setString(paramIndex++, faculty.getDescription());

            if (faculty.getHeadTeacherUsername() != null && !faculty.getHeadTeacherUsername().isEmpty()) {
                stmt.setString(paramIndex++, faculty.getHeadTeacherUsername());
            } else {
                stmt.setNull(paramIndex++, Types.VARCHAR);
            }

            int result = stmt.executeUpdate();

            if (result > 0) {
                if (faculty.getFacultyId() == 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            faculty.setFacultyId(rs.getInt(1));
                        }
                    }
                }
                LOGGER.info("Faculty added successfully: " + faculty.getFacultyCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding faculty: " + faculty.getFacultyCode(), e);
        }

        return false;
    }

    /**
     * Thêm khoa mới
     */
    public boolean addFaculty(Faculty faculty) {
        String sql = "INSERT INTO faculties (faculty_code, faculty_name, description, head_teacher_username) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, faculty.getFacultyCode());
            stmt.setString(2, faculty.getFacultyName());
            stmt.setString(3, faculty.getDescription());

            if (faculty.getHeadTeacherUsername() != null && !faculty.getHeadTeacherUsername().isEmpty()) {
                stmt.setString(4, faculty.getHeadTeacherUsername());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            int result = stmt.executeUpdate();

            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        faculty.setFacultyId(rs.getInt(1));
                    }
                }
                LOGGER.info("Faculty added successfully: " + faculty.getFacultyCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding faculty: " + faculty.getFacultyCode(), e);
        }

        return false;
    }

    /**
     * Cập nhật khoa
     */
    public boolean updateFaculty(Faculty faculty) {
        String sql = "UPDATE faculties SET faculty_name = ?, description = ?, head_teacher_username = ? WHERE faculty_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, faculty.getFacultyName());
            stmt.setString(2, faculty.getDescription());

            if (faculty.getHeadTeacherUsername() != null && !faculty.getHeadTeacherUsername().isEmpty()) {
                stmt.setString(3, faculty.getHeadTeacherUsername());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            stmt.setInt(4, faculty.getFacultyId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Faculty updated successfully: " + faculty.getFacultyId());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating faculty: " + faculty.getFacultyId(), e);
        }

        return false;
    }

    /**
     * Xóa khoa
     */
    public boolean deleteFaculty(int facultyId) {
        String sql = "DELETE FROM faculties WHERE faculty_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facultyId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Faculty deleted successfully: " + facultyId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting faculty: " + facultyId, e);
        }

        return false;
    }

    /**
     * Map ResultSet to Faculty object
     */
    private Faculty mapResultSetToFaculty(ResultSet rs) throws SQLException {
        Faculty faculty = new Faculty();
        faculty.setFacultyId(rs.getInt("faculty_id"));
        faculty.setFacultyCode(rs.getString("faculty_code"));
        faculty.setFacultyName(rs.getString("faculty_name"));
        faculty.setDescription(rs.getString("description"));

        String headTeacherUsername = rs.getString("head_teacher_username");
        if (headTeacherUsername != null) {
            faculty.setHeadTeacherUsername(headTeacherUsername);
        }

        faculty.setCreatedAt(rs.getTimestamp("created_at"));
        faculty.setHeadTeacherName(rs.getString("head_teacher_name"));

        return faculty;
    }
}
