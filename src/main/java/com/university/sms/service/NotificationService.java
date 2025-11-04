package com.university.sms.service;

import com.university.sms.dao.NotificationDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Notification;
import com.university.sms.model.Notification.TargetType;
import com.university.sms.model.Notification.Priority;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class cho quản lý thông báo
 * Business logic cho notifications
 */
public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    
    private NotificationDAO notificationDAO;
    private StudentDAO studentDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Tạo thông báo mới với validation
     */
    public boolean createNotification(Notification notification) throws Exception {
        // Validate
        validateNotification(notification);
        
        boolean result = notificationDAO.createNotification(notification);
        
        if (result) {
            LOGGER.info("Created notification: " + notification.getTitle() + 
                       " from sender_id: " + notification.getSenderId());
        }
        
        return result;
    }

    /**
     * Gửi thông báo đến tất cả sinh viên
     */
    public boolean sendNotificationToAll(Notification notification) throws Exception {
        notification.setTargetType(TargetType.ALL);
        notification.setTargetId(null);
        
        return createNotification(notification);
    }

    /**
     * Gửi thông báo đến một sinh viên cụ thể
     */
    public boolean sendNotificationToStudent(int studentId, Notification notification) throws Exception {
        // Kiểm tra student exists
        if (studentDAO.findById(studentId) == null) {
            throw new IllegalArgumentException("Sinh viên không tồn tại");
        }

        notification.setTargetType(TargetType.STUDENT);
        notification.setTargetId(studentId);
        
        return createNotification(notification);
    }

    /**
     * Gửi thông báo đến một lớp
     */
    public boolean sendNotificationToClass(int classId, Notification notification) throws Exception {
        notification.setTargetType(TargetType.CLASS);
        notification.setTargetId(classId);
        
        return createNotification(notification);
    }

    /**
     * Gửi thông báo đến một khoa
     */
    public boolean sendNotificationToFaculty(int facultyId, Notification notification) throws Exception {
        notification.setTargetType(TargetType.FACULTY);
        notification.setTargetId(facultyId);
        
        return createNotification(notification);
    }

    /**
     * Cập nhật thông báo
     */
    public boolean updateNotification(Notification notification) throws Exception {
        validateNotification(notification);
        
        Notification existing = notificationDAO.findById(notification.getNotificationId());
        if (existing == null) {
            throw new IllegalArgumentException("Thông báo không tồn tại");
        }

        return notificationDAO.updateNotification(notification);
    }

    /**
     * Xóa thông báo
     */
    public boolean deleteNotification(int notificationId) throws Exception {
        Notification notification = notificationDAO.findById(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("Thông báo không tồn tại");
        }

        return notificationDAO.deleteNotification(notificationId);
    }

    /**
     * Đánh dấu thông báo đã đọc
     */
    public boolean markAsRead(int notificationId) throws Exception {
        return notificationDAO.markAsRead(notificationId);
    }

    /**
     * Đánh dấu tất cả thông báo của user đã đọc
     */
    public boolean markAllAsReadForUser(int userId) throws Exception {
        return notificationDAO.markAllAsReadForUser(userId);
    }

    /**
     * Lấy thông báo theo ID
     */
    public Notification getNotificationById(int notificationId) throws Exception {
        return notificationDAO.findById(notificationId);
    }

    /**
     * Lấy tất cả thông báo của user
     */
    public List<Notification> getNotificationsByUser(int userId) throws Exception {
        return notificationDAO.getNotificationsByUser(userId);
    }

    /**
     * Lấy thông báo chưa đọc của user
     */
    public List<Notification> getUnreadNotifications(int userId) throws Exception {
        return notificationDAO.getUnreadNotificationsByUser(userId);
    }

    /**
     * Lấy tất cả thông báo (cho admin)
     */
    public List<Notification> getAllNotifications() throws Exception {
        return notificationDAO.getAllNotifications();
    }

    /**
     * Lấy thông báo theo lớp
     */
    public List<Notification> getNotificationsByClass(int classId) throws Exception {
        return notificationDAO.getNotificationsByClass(classId);
    }

    /**
     * Lấy thông báo theo khoa
     */
    public List<Notification> getNotificationsByFaculty(int facultyId) throws Exception {
        return notificationDAO.getNotificationsByFaculty(facultyId);
    }

    /**
     * Đếm số thông báo chưa đọc của user
     */
    public int countUnreadNotifications(int userId) throws Exception {
        return notificationDAO.countUnreadByUser(userId);
    }

    /**
     * Lấy thống kê thông báo
     */
    public NotificationStatistics getStatistics() throws Exception {
        NotificationStatistics stats = new NotificationStatistics();
        
        List<Notification> allNotifications = notificationDAO.getAllNotifications();
        stats.totalNotifications = allNotifications.size();
        
        for (Notification notif : allNotifications) {
            if (!notif.isRead()) {
                stats.unreadCount++;
            }
            
            switch (notif.getPriority()) {
                case URGENT:
                    stats.urgentCount++;
                    break;
                case HIGH:
                    stats.highPriorityCount++;
                    break;
                case MEDIUM:
                    stats.mediumPriorityCount++;
                    break;
                case LOW:
                    stats.lowPriorityCount++;
                    break;
            }
            
            switch (notif.getTargetType()) {
                case ALL:
                    stats.allTargetCount++;
                    break;
                case FACULTY:
                    stats.facultyTargetCount++;
                    break;
                case CLASS:
                    stats.classTargetCount++;
                    break;
                case STUDENT:
                    stats.studentTargetCount++;
                    break;
            }
        }
        
        return stats;
    }

    /**
     * Validate notification data
     */
    private void validateNotification(Notification notification) throws IllegalArgumentException {
        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        }

        if (notification.getTitle().length() > 200) {
            throw new IllegalArgumentException("Tiêu đề không được vượt quá 200 ký tự");
        }

        if (notification.getContent() == null || notification.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung không được để trống");
        }

        if (notification.getSenderId() <= 0) {
            throw new IllegalArgumentException("Sender ID không hợp lệ");
        }

        if (notification.getTargetType() == null) {
            throw new IllegalArgumentException("Target type không được để trống");
        }

        // Validate targetId based on targetType
        if (notification.getTargetType() != TargetType.ALL && notification.getTargetId() == null) {
            throw new IllegalArgumentException("Target ID không được để trống khi target type không phải ALL");
        }

        if (notification.getPriority() == null) {
            throw new IllegalArgumentException("Priority không được để trống");
        }
    }

    /**
     * Inner class cho thống kê thông báo
     */
    public static class NotificationStatistics {
        public int totalNotifications;
        public int unreadCount;
        public int urgentCount;
        public int highPriorityCount;
        public int mediumPriorityCount;
        public int lowPriorityCount;
        public int allTargetCount;
        public int facultyTargetCount;
        public int classTargetCount;
        public int studentTargetCount;

        public NotificationStatistics() {
            // Initialize all to 0
        }
    }
}

