package com.university.sms.dao;

import com.university.sms.model.Notification;
import com.university.sms.model.Notification.TargetType;
import com.university.sms.model.Notification.Priority;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO class cho bảng notifications
 * Quản lý CRUD operations cho thông báo
 */
public class NotificationDAO {
    private static final Logger LOGGER = Logger.getLogger(NotificationDAO.class.getName());

    /**
     * Tạo thông báo mới
     */
    public boolean createNotification(Notification notification) throws SQLException {
        String sql = "INSERT INTO notifications (title, content, sender_id, target_type, target_id, priority, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, notification.getTitle());
            stmt.setString(2, notification.getContent());
            stmt.setInt(3, notification.getSenderId());
            stmt.setString(4, notification.getTargetType().name().toLowerCase());
            
            if (notification.getTargetId() != null) {
                stmt.setInt(5, notification.getTargetId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setString(6, notification.getPriority().name().toLowerCase());
            
            if (notification.getExpiresAt() != null) {
                stmt.setTimestamp(7, notification.getExpiresAt());
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        notification.setNotificationId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info("Created notification: " + notification.getTitle());
                return true;
            }

            return false;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating notification", e);
            throw e;
        }
    }

    /**
     * Cập nhật thông báo
     */
    public boolean updateNotification(Notification notification) throws SQLException {
        String sql = "UPDATE notifications SET title = ?, content = ?, target_type = ?, " +
                     "target_id = ?, priority = ?, expires_at = ? WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notification.getTitle());
            stmt.setString(2, notification.getContent());
            stmt.setString(3, notification.getTargetType().name().toLowerCase());
            
            if (notification.getTargetId() != null) {
                stmt.setInt(4, notification.getTargetId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            stmt.setString(5, notification.getPriority().name().toLowerCase());
            
            if (notification.getExpiresAt() != null) {
                stmt.setTimestamp(6, notification.getExpiresAt());
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            
            stmt.setInt(7, notification.getNotificationId());

            int affectedRows = stmt.executeUpdate();
            LOGGER.info("Updated notification_id: " + notification.getNotificationId());
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating notification", e);
            throw e;
        }
    }

    /**
     * Xóa thông báo
     */
    public boolean deleteNotification(int notificationId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);
            int affectedRows = stmt.executeUpdate();
            LOGGER.info("Deleted notification_id: " + notificationId);
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting notification", e);
            throw e;
        }
    }

    /**
     * Đánh dấu thông báo đã đọc
     */
    public boolean markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking notification as read", e);
            throw e;
        }
    }

    /**
     * Đánh dấu tất cả thông báo của user đã đọc
     */
    public boolean markAllAsReadForUser(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE " +
                     "WHERE (target_type = 'student' AND target_id = ?) " +
                     "OR target_type = 'all'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int affectedRows = stmt.executeUpdate();
            LOGGER.info("Marked all notifications as read for user_id: " + userId);
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking all notifications as read", e);
            throw e;
        }
    }

    /**
     * Lấy thông báo theo ID
     */
    public Notification findById(int notificationId) throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "WHERE n.notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToNotification(rs);
                }
            }

            return null;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding notification by ID", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả thông báo của một user (student)
     * Bao gồm: thông báo cho cá nhân, cho lớp, cho khoa, và cho tất cả
     */
    public List<Notification> getNotificationsByUser(int userId) throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "WHERE (n.target_type = 'all') " +
                     "   OR (n.target_type = 'student' AND n.target_id = ?) " +
                     "   OR (n.target_type = 'class' AND n.target_id IN (" +
                     "      SELECT s.class_id FROM students s WHERE s.user_id = ?" +
                     "   )) " +
                     "   OR (n.target_type = 'faculty' AND n.target_id IN (" +
                     "      SELECT c.faculty_id FROM students s " +
                     "      JOIN classes c ON s.class_id = c.class_id " +
                     "      WHERE s.user_id = ?" +
                     "   )) " +
                     "ORDER BY n.priority DESC, n.created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }

            return notifications;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting notifications by user", e);
            throw e;
        }
    }

    /**
     * Lấy thông báo chưa đọc của user
     */
    public List<Notification> getUnreadNotificationsByUser(int userId) throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "WHERE n.is_read = FALSE AND (" +
                     "   (n.target_type = 'all') " +
                     "   OR (n.target_type = 'student' AND n.target_id = ?) " +
                     "   OR (n.target_type = 'class' AND n.target_id IN (" +
                     "      SELECT s.class_id FROM students s WHERE s.user_id = ?" +
                     "   )) " +
                     "   OR (n.target_type = 'faculty' AND n.target_id IN (" +
                     "      SELECT c.faculty_id FROM students s " +
                     "      JOIN classes c ON s.class_id = c.class_id " +
                     "      WHERE s.user_id = ?" +
                     "   )) " +
                     ") " +
                     "ORDER BY n.priority DESC, n.created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }

            return notifications;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting unread notifications", e);
            throw e;
        }
    }

    /**
     * Lấy tất cả thông báo (cho admin)
     */
    public List<Notification> getAllNotifications() throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "ORDER BY n.created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }

            return notifications;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all notifications", e);
            throw e;
        }
    }

    /**
     * Lấy thông báo theo lớp
     */
    public List<Notification> getNotificationsByClass(int classId) throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "WHERE n.target_type = 'class' AND n.target_id = ? " +
                     "ORDER BY n.created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }

            return notifications;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting notifications by class", e);
            throw e;
        }
    }

    /**
     * Lấy thông báo theo faculty
     */
    public List<Notification> getNotificationsByFaculty(int facultyId) throws SQLException {
        String sql = "SELECT n.*, u.full_name as sender_name, u.role as sender_role " +
                     "FROM notifications n " +
                     "JOIN users u ON n.sender_id = u.user_id " +
                     "WHERE n.target_type = 'faculty' AND n.target_id = ? " +
                     "ORDER BY n.created_at DESC";

        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, facultyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }

            return notifications;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting notifications by faculty", e);
            throw e;
        }
    }

    /**
     * Đếm số thông báo chưa đọc của user
     */
    public int countUnreadByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as unread_count " +
                     "FROM notifications n " +
                     "WHERE n.is_read = FALSE AND (" +
                     "   (n.target_type = 'all') " +
                     "   OR (n.target_type = 'student' AND n.target_id = ?) " +
                     "   OR (n.target_type = 'class' AND n.target_id IN (" +
                     "      SELECT s.class_id FROM students s WHERE s.user_id = ?" +
                     "   )) " +
                     "   OR (n.target_type = 'faculty' AND n.target_id IN (" +
                     "      SELECT c.faculty_id FROM students s " +
                     "      JOIN classes c ON s.class_id = c.class_id " +
                     "      WHERE s.user_id = ?" +
                     "   )) " +
                     ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("unread_count");
                }
            }

            return 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting unread notifications", e);
            throw e;
        }
    }

    /**
     * Map ResultSet to Notification object
     */
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();

        notification.setNotificationId(rs.getInt("notification_id"));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setSenderId(rs.getInt("sender_id"));
        
        // Convert string to enum
        String targetTypeStr = rs.getString("target_type").toUpperCase();
        notification.setTargetType(TargetType.valueOf(targetTypeStr));
        
        notification.setTargetId((Integer) rs.getObject("target_id"));
        
        String priorityStr = rs.getString("priority").toUpperCase();
        notification.setPriority(Priority.valueOf(priorityStr));
        
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(rs.getTimestamp("created_at"));
        notification.setExpiresAt(rs.getTimestamp("expires_at"));

        // Related information
        notification.setSenderName(rs.getString("sender_name"));
        notification.setSenderRole(rs.getString("sender_role"));

        return notification;
    }
}

