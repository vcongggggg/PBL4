package com.university.sms.service;

import com.university.sms.dao.UserDAO;
import com.university.sms.model.User;

import java.util.logging.Logger;

/**
 * Service xử lý xác thực và quản lý người dùng
 */
public class AuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    
    private UserDAO userDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Xác thực người dùng
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            LOGGER.warning("Authentication failed: Empty username or password");
            return null;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                LOGGER.info("User authenticated successfully: " + username);
                return user;
            } else {
                LOGGER.warning("Authentication failed for user: " + username);
                return null;
            }
        } catch (Exception e) {
            LOGGER.severe("Error during authentication: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tạo tài khoản người dùng mới
     */
    public boolean createUser(User user) {
        if (user == null) {
            return false;
        }

        // Validate required fields
        if (user.getUsername() == null || user.getUsername().trim().isEmpty() ||
            user.getPassword() == null || user.getPassword().trim().isEmpty() ||
            user.getEmail() == null || user.getEmail().trim().isEmpty() ||
            user.getFullName() == null || user.getFullName().trim().isEmpty() ||
            user.getRole() == null) {
            
            LOGGER.warning("Cannot create user: Missing required fields");
            return false;
        }

        // Check if username already exists
        User existingUser = userDAO.findByUsername(user.getUsername());
        if (existingUser != null) {
            LOGGER.warning("Cannot create user: Username already exists - " + user.getUsername());
            return false;
        }

        try {
            boolean success = userDAO.addUser(user);
            if (success) {
                LOGGER.info("User created successfully: " + user.getUsername());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error creating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tìm người dùng theo ID
     */
    public User findUserById(int userId) {
        try {
            return userDAO.findById(userId);
        } catch (Exception e) {
            LOGGER.severe("Error finding user by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tìm người dùng theo username
     */
    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        try {
            return userDAO.findByUsername(username);
        } catch (Exception e) {
            LOGGER.severe("Error finding user by username: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cập nhật thông tin người dùng
     */
    public boolean updateUser(User user) {
        if (user == null || user.getUserId() <= 0) {
            return false;
        }

        try {
            boolean success = userDAO.updateUser(user);
            if (success) {
                LOGGER.info("User updated successfully: " + user.getUserId());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thay đổi mật khẩu
     */
    public boolean changePassword(int userId, String newPassword) {
        if (userId <= 0 || newPassword == null || newPassword.trim().isEmpty()) {
            LOGGER.warning("Cannot change password: Invalid input");
            return false;
        }

        // Validate password strength
        if (newPassword.length() < 6) {
            LOGGER.warning("Cannot change password: Password too short");
            return false;
        }

        try {
            boolean success = userDAO.changePassword(userId, newPassword);
            if (success) {
                LOGGER.info("Password changed successfully for user ID: " + userId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error changing password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vô hiệu hóa tài khoản
     */
    public boolean deactivateUser(int userId) {
        if (userId <= 0) {
            return false;
        }

        try {
            boolean success = userDAO.deactivateUser(userId);
            if (success) {
                LOGGER.info("User deactivated successfully: " + userId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error deactivating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ghi log đăng nhập thành công
     */
    public void logLogin(int userId, String ipAddress, String userAgent, String status) {
        try {
            userDAO.logLogin(userId, ipAddress, userAgent, status);
        } catch (Exception e) {
            LOGGER.warning("Error logging user login: " + e.getMessage());
        }
    }

    /**
     * Ghi log đăng nhập thất bại
     */
    public void logFailedLogin(String username, String ipAddress) {
        try {
            // Try to find user ID for logging
            User user = userDAO.findByUsername(username);
            if (user != null) {
                userDAO.logLogin(user.getUserId(), ipAddress, "Java Client", "failed");
            }
        } catch (Exception e) {
            LOGGER.warning("Error logging failed login: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra quyền truy cập
     */
    public boolean hasPermission(User user, String action) {
        if (user == null || action == null) {
            return false;
        }

        // Admin có tất cả quyền
        if (user.getRole() == User.UserRole.ADMIN) {
            return true;
        }

        // Giáo viên có quyền xem và quản lý sinh viên, khóa học
        if (user.getRole() == User.UserRole.TEACHER) {
            return action.contains("GET_") || 
                   action.contains("SEARCH_") ||
                   action.contains("UPDATE_GRADE") ||
                   action.contains("MARK_ATTENDANCE");
        }

        // Sinh viên chỉ có quyền xem thông tin của mình
        if (user.getRole() == User.UserRole.STUDENT) {
            return action.contains("GET_STUDENT_") ||
                   action.contains("GET_COURSE") ||
                   action.contains("GET_ENROLLMENT") ||
                   action.contains("GET_GRADE");
        }

        return false;
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    /**
     * Validate username format
     */
    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Username should be 3-20 characters, alphanumeric and underscore only
        String usernameRegex = "^[a-zA-Z0-9_]{3,20}$";
        return username.matches(usernameRegex);
    }
}


