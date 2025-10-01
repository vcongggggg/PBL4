package com.university.sms.dao;

import com.university.sms.model.Department;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Department
 */
public class DepartmentDAO {
    private static final Logger LOGGER = Logger.getLogger(DepartmentDAO.class.getName());

    /**
     * Lấy tất cả khoa
     */
    public List<Department> findAll() {
        String sql = "SELECT d.*, u.full_name AS head_teacher_name " +
                    "FROM departments d " +
                    "LEFT JOIN users u ON d.head_teacher_id = u.user_id " +
                    "ORDER BY d.department_name";
        
        List<Department> departments = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(mapResultSetToDepartment(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all departments", e);
        }
        
        return departments;
    }

    /**
     * Tìm khoa theo ID
     */
    public Department findById(int departmentId) {
        String sql = "SELECT d.*, u.full_name AS head_teacher_name " +
                    "FROM departments d " +
                    "LEFT JOIN users u ON d.head_teacher_id = u.user_id " +
                    "WHERE d.department_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, departmentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDepartment(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding department by ID: " + departmentId, e);
        }
        
        return null;
    }

    /**
     * Tìm khoa theo mã khoa
     */
    public Department findByCode(String departmentCode) {
        String sql = "SELECT d.*, u.full_name AS head_teacher_name " +
                    "FROM departments d " +
                    "LEFT JOIN users u ON d.head_teacher_id = u.user_id " +
                    "WHERE d.department_code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, departmentCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDepartment(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding department by code: " + departmentCode, e);
        }
        
        return null;
    }

    /**
     * Thêm khoa mới
     */
    public boolean addDepartment(Department department) {
        String sql = "INSERT INTO departments (department_code, department_name, description, head_teacher_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, department.getDepartmentCode());
            stmt.setString(2, department.getDepartmentName());
            stmt.setString(3, department.getDescription());
            
            if (department.getHeadTeacherId() != null) {
                stmt.setInt(4, department.getHeadTeacherId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        department.setDepartmentId(rs.getInt(1));
                    }
                }
                LOGGER.info("Department added successfully: " + department.getDepartmentCode());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding department: " + department.getDepartmentCode(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật khoa
     */
    public boolean updateDepartment(Department department) {
        String sql = "UPDATE departments SET department_name = ?, description = ?, head_teacher_id = ? WHERE department_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, department.getDepartmentName());
            stmt.setString(2, department.getDescription());
            
            if (department.getHeadTeacherId() != null) {
                stmt.setInt(3, department.getHeadTeacherId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setInt(4, department.getDepartmentId());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Department updated successfully: " + department.getDepartmentId());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating department: " + department.getDepartmentId(), e);
        }
        
        return false;
    }

    /**
     * Xóa khoa
     */
    public boolean deleteDepartment(int departmentId) {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, departmentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Department deleted successfully: " + departmentId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting department: " + departmentId, e);
        }
        
        return false;
    }

    /**
     * Map ResultSet to Department object
     */
    private Department mapResultSetToDepartment(ResultSet rs) throws SQLException {
        Department department = new Department();
        department.setDepartmentId(rs.getInt("department_id"));
        department.setDepartmentCode(rs.getString("department_code"));
        department.setDepartmentName(rs.getString("department_name"));
        department.setDescription(rs.getString("description"));
        
        int headTeacherId = rs.getInt("head_teacher_id");
        if (!rs.wasNull()) {
            department.setHeadTeacherId(headTeacherId);
        }
        
        department.setCreatedAt(rs.getTimestamp("created_at"));
        department.setHeadTeacherName(rs.getString("head_teacher_name"));
        
        return department;
    }
}


