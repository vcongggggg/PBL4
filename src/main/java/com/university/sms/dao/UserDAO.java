package com.university.sms.dao;

import com.university.sms.model.User;
import com.university.sms.util.DatabaseConnection;
import com.university.sms.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho User
 */
public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    /**
     * Thêm user mới
     */
    public boolean addUser(User user) {
        // Validate required fields
        if (user == null) {
            LOGGER.warning("Cannot add user: User object is null");
            return false;
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            LOGGER.warning("Cannot add user: Username is required");
            return false;
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            LOGGER.warning("Cannot add user: Password is required");
            return false;
        }
        if (user.getRole() == null) {
            LOGGER.warning("Cannot add user: Role is required");
            return false;
        }

        // Check if username already exists
        User existingUser = findByUsername(user.getUsername());
        if (existingUser != null) {
            LOGGER.warning("Cannot add user: Username already exists - " + user.getUsername());
            return false;
        }

        String sql = "INSERT INTO users (username, password, email, full_name, role, phone, address, faculty_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername().trim());
            // Hash password trước khi lưu vào database
            String hashedPassword = PasswordUtil.isHashed(user.getPassword()) 
                ? user.getPassword() 
                : PasswordUtil.hashPassword(user.getPassword());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getRole().name().toLowerCase());
            stmt.setString(6, user.getPhone());
            stmt.setString(7, user.getAddress());
            if (user.getFacultyCode() != null && !user.getFacultyCode().trim().isEmpty()) {
                stmt.setString(8, user.getFacultyCode().trim());
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }

            int result = stmt.executeUpdate();

            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
                    }
                }
                LOGGER.info("User added successfully: " + user.getUsername());
                return true;
            }

        } catch (SQLException e) {
            // Check if it's a duplicate key error
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("phone")) {
                    LOGGER.warning(
                            "Cannot add user: Phone already exists (database constraint) - " + user.getPhone());
                } else {
                    LOGGER.warning(
                            "Cannot add user: Username or email already exists (database constraint) - "
                                    + user.getUsername());
                }
            } else {
                LOGGER.log(Level.SEVERE, "Error adding user: " + user.getUsername(), e);
            }
        }

        return false;
    }

    /**
     * Thêm mới hoặc cập nhật theo username. Nếu chưa tồn tại và userId > 0, chèn
     * giữ nguyên user_id.
     */
    public boolean addOrUpdatePreserveId(User user) {
        try {
            User existing = findByUsername(user.getUsername());
            if (existing != null) {
                user.setUserId(existing.getUserId());
                return updateUser(user);
            }

            if (user.getUserId() > 0) {
                String insertWithId = "INSERT INTO users (user_id, username, password, email, full_name, role, phone, address, faculty_code) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DatabaseConnection.getConnection();
                        PreparedStatement stmt = conn.prepareStatement(insertWithId)) {
                    stmt.setInt(1, user.getUserId());
                    stmt.setString(2, user.getUsername());
                    // Hash password trước khi lưu vào database
                    String hashedPassword = PasswordUtil.isHashed(user.getPassword()) 
                        ? user.getPassword() 
                        : PasswordUtil.hashPassword(user.getPassword());
                    stmt.setString(3, hashedPassword);
                    stmt.setString(4, user.getEmail());
                    stmt.setString(5, user.getFullName());
                    stmt.setString(6, user.getRole().name().toLowerCase());
                    stmt.setString(7, user.getPhone());
                    stmt.setString(8, user.getAddress());
                    if (user.getFacultyCode() != null && !user.getFacultyCode().trim().isEmpty()) {
                        stmt.setString(9, user.getFacultyCode().trim());
                    } else {
                        stmt.setNull(9, Types.VARCHAR);
                    }
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        LOGGER.info("User added with explicit ID: " + user.getUserId());
                        return true;
                    }
                }
                return false;
            }

            return addUser(user);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error addOrUpdatePreserveId user: " + user.getUsername(), e);
            return false;
        }
    }

    /**
     * Tìm user theo username và password
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    // Verify password bằng BCrypt
                    if (PasswordUtil.verifyPassword(password, storedPassword)) {
                        User user = mapResultSetToUser(rs);
                        LOGGER.info("User authenticated successfully: " + username);
                        return user;
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error authenticating user: " + username, e);
        }

        return null;
    }

    /**
     * Tìm user theo ID
     */
    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by ID: " + userId, e);
        }

        return null;
    }

    /**
     * Tìm user theo username
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
        }

        return null;
    }

    /**
     * Tìm user theo phone (số điện thoại)
     * 
     * @param phone Số điện thoại cần tìm
     * @return User nếu tìm thấy, null nếu không tìm thấy
     */
    public User findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE phone = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by phone: " + phone, e);
        }

        return null;
    }

    /**
     * Tìm user theo email
     * 
     * @param email Email cần tìm
     * @return User nếu tìm thấy, null nếu không tìm thấy
     */
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by email: " + email, e);
        }

        return null;
    }

    /**
     * Lấy tất cả users theo role
     */
    public List<User> findByRole(User.UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ? AND is_active = TRUE ORDER BY full_name";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding users by role: " + role, e);
        }

        return users;
    }

    /**
     * Lấy danh sách giáo viên chưa chủ nhiệm lớp nào (hoặc đang chủ nhiệm lớp hiện
     * tại)
     */
    public List<User> findAvailableClassTeachers(String classCode, String facultyCode) {
        boolean includeCurrentClass = classCode != null && !classCode.trim().isEmpty();
        boolean filterByFaculty = facultyCode != null && !facultyCode.trim().isEmpty();

        StringBuilder sql = new StringBuilder("SELECT u.* FROM users u WHERE u.role = ? AND u.is_active = TRUE ");

        if (filterByFaculty) {
            sql.append("AND u.faculty_code = ? ");
        }

        if (includeCurrentClass) {
            sql.append("AND (NOT EXISTS (SELECT 1 FROM classes c WHERE c.teacher_username = u.username) ")
                    .append("OR EXISTS (SELECT 1 FROM classes c WHERE c.teacher_username = u.username AND c.class_code = ?)) ");
        } else {
            sql.append("AND NOT EXISTS (SELECT 1 FROM classes c WHERE c.teacher_username = u.username) ");
        }

        sql.append("ORDER BY u.full_name");

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, User.UserRole.TEACHER.name().toLowerCase());
            if (filterByFaculty) {
                stmt.setString(paramIndex++, facultyCode.trim());
            }
            if (includeCurrentClass) {
                stmt.setString(paramIndex++, classCode.trim());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding available class teachers", e);
        }

        return users;
    }

    /**
     * Lấy tất cả users theo role (bao gồm cả đã vô hiệu hóa)
     */
    public List<User> findByRoleIncludeInactive(User.UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY is_active DESC, full_name";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding users by role (include inactive): " + role, e);
        }

        return users;
    }

    /**
     * Kích hoạt lại user
     */
    public boolean activateUser(int userId) {
        String sql = "UPDATE users SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("User activated successfully: " + userId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error activating user: " + userId, e);
        }

        return false;
    }

    /**
     * Cập nhật thông tin user
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ?, address = ?, faculty_code = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getAddress());
            if (user.getFacultyCode() != null && !user.getFacultyCode().trim().isEmpty()) {
                stmt.setString(5, user.getFacultyCode().trim());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            stmt.setInt(6, user.getUserId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("User updated successfully: " + user.getUsername());
                return true;
            }

        } catch (SQLException e) {
            // Check if it's a duplicate key error
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("phone")) {
                    LOGGER.warning(
                            "Cannot update user: Phone already exists (database constraint) - " + user.getPhone());
                } else {
                    LOGGER.warning(
                            "Cannot update user: Email already exists (database constraint) - " + user.getEmail());
                }
            } else {
                LOGGER.log(Level.SEVERE, "Error updating user: " + user.getUserId(), e);
            }
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Thay đổi mật khẩu (dùng username)
     */
    public boolean changePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash password trước khi lưu vào database
            String hashedPassword = PasswordUtil.isHashed(newPassword) 
                ? newPassword 
                : PasswordUtil.hashPassword(newPassword);
            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("Password changed successfully for username: " + username);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error changing password for username: " + username, e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Vô hiệu hóa user (dùng username)
     */
    public boolean deactivateUser(String username) {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            int result = stmt.executeUpdate();

            if (result > 0) {
                LOGGER.info("User deactivated successfully: " + username);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deactivating user: " + username, e);
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Ghi log đăng nhập (dùng username)
     */
    public void logLogin(String username, String ipAddress, String userAgent, String status) {
        String sql = "INSERT INTO login_history (username, ip_address, user_agent, login_status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, ipAddress);
            stmt.setString(3, userAgent);
            stmt.setString(4, status);

            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error logging user login for username: " + username, e);
        }
    }

    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(User.UserRole.valueOf(rs.getString("role").toUpperCase()));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setFacultyCode(rs.getString("faculty_code"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
