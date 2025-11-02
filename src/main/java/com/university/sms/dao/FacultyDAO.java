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
                    "LEFT JOIN users u ON f.head_teacher_id = u.user_id " +
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
                    "LEFT JOIN users u ON f.head_teacher_id = u.user_id " +
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
                    "LEFT JOIN users u ON f.head_teacher_id = u.user_id " +
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
     * Thêm khoa mới
     */
    public boolean addFaculty(Faculty faculty) {
        String sql = "INSERT INTO faculties (faculty_code, faculty_name, description, head_teacher_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, faculty.getFacultyCode());
            stmt.setString(2, faculty.getFacultyName());
            stmt.setString(3, faculty.getDescription());
            
            if (faculty.getHeadTeacherId() != null) {
                stmt.setInt(4, faculty.getHeadTeacherId());
            } else {
                stmt.setNull(4, Types.INTEGER);
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
        String sql = "UPDATE faculties SET faculty_name = ?, description = ?, head_teacher_id = ? WHERE faculty_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, faculty.getFacultyName());
            stmt.setString(2, faculty.getDescription());
            
            if (faculty.getHeadTeacherId() != null) {
                stmt.setInt(3, faculty.getHeadTeacherId());
            } else {
                stmt.setNull(3, Types.INTEGER);
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
        
        int headTeacherId = rs.getInt("head_teacher_id");
        if (!rs.wasNull()) {
            faculty.setHeadTeacherId(headTeacherId);
        }
        
        faculty.setCreatedAt(rs.getTimestamp("created_at"));
        faculty.setHeadTeacherName(rs.getString("head_teacher_name"));
        
        return faculty;
    }
}



