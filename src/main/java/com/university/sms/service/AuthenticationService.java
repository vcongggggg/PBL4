package com.university.sms.service;

import com.university.sms.dao.UserDAO;
import com.university.sms.model.User;

import java.util.logging.Logger;

public class AuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());

    private UserDAO userDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Inner class - Kết quả xác thực đăng nhập với thông tin chi tiết
     */
    public static class AuthenticationResult {
        private boolean success;
        private User user;
        private String errorCode;
        private String message;

        // Error codes
        public static final String ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String ERROR_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
        public static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";

        public AuthenticationResult(boolean success, User user, String errorCode, String message) {
            this.success = success;
            this.user = user;
            this.errorCode = errorCode;
            this.message = message;
        }

        // Factory methods
        public static AuthenticationResult success(User user) {
            return new AuthenticationResult(true, user, null, "Đăng nhập thành công");
        }

        public static AuthenticationResult userNotFound() {
            return new AuthenticationResult(false, null, ERROR_USER_NOT_FOUND, "Tên đăng nhập không tồn tại");
        }

        public static AuthenticationResult accountDisabled() {
            return new AuthenticationResult(false, null, ERROR_ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa");
        }

        public static AuthenticationResult invalidPassword() {
            return new AuthenticationResult(false, null, ERROR_INVALID_CREDENTIALS, "Mật khẩu không đúng");
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public User getUser() {
            return user;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Xác thực đăng nhập (phiên bản cũ - deprecated)
     */
    @Deprecated
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
     * Xác thực đăng nhập với thông tin chi tiết lỗi
     */
    public AuthenticationResult authenticateDetailed(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            LOGGER.warning("Authentication failed: Empty username or password");
            return new AuthenticationResult(false, null, "INVALID_INPUT", "Vui lòng nhập đầy đủ thông tin");
        }

        try {
            return userDAO.authenticateDetailed(username, password);
        } catch (Exception e) {
            LOGGER.severe("Error during authentication: " + e.getMessage());
            return new AuthenticationResult(false, null, "ERROR", "Lỗi khi xác thực: " + e.getMessage());
        }
    }

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

    public User findUserById(int userId) {
        try {
            return userDAO.findById(userId);
        } catch (Exception e) {
            LOGGER.severe("Error finding user by ID: " + e.getMessage());
            return null;
        }
    }

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

    public boolean changePassword(String username, String newPassword) {
        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            LOGGER.warning("Cannot change password: Invalid input");
            return false;
        }

        // Validate password strength
        if (newPassword.length() < 6) {
            LOGGER.warning("Cannot change password: Password too short");
            return false;
        }

        try {
            boolean success = userDAO.changePassword(username, newPassword);
            if (success) {
                LOGGER.info("Password changed successfully for username: " + username);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error changing password: " + e.getMessage());
            return false;
        }
    }

    public boolean deactivateUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        try {
            boolean success = userDAO.deactivateUser(username);
            if (success) {
                LOGGER.info("User deactivated successfully: " + username);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error deactivating user: " + e.getMessage());
            return false;
        }
    }

    public void logLogin(String username, String ipAddress, String userAgent, String status) {
        try {
            userDAO.logLogin(username, ipAddress, userAgent, status);
        } catch (Exception e) {
            LOGGER.warning("Error logging user login: " + e.getMessage());
        }
    }

    public void logFailedLogin(String username, String ipAddress) {
        try {
            userDAO.logLogin(username, ipAddress, "Java Client", "failed");
        } catch (Exception e) {
            LOGGER.warning("Error logging failed login: " + e.getMessage());
        }
    }

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

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // Username should be 3-20 characters, alphanumeric and underscore only
        String usernameRegex = "^[a-zA-Z0-9_]{3,20}$";
        return username.matches(usernameRegex);
    }
}
