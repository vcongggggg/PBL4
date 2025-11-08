package com.university.sms.service;

import com.university.sms.dao.NotificationDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Notification;
import com.university.sms.model.Notification.TargetType;

import java.util.List;
import java.util.logging.Logger;

public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private NotificationDAO notificationDAO;
    private StudentDAO studentDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
        this.studentDAO = new StudentDAO();
    }

    public boolean createNotification(Notification notification) throws Exception {
        // Validate
        validateNotification(notification);

        boolean result = notificationDAO.createNotification(notification);

        if (result) {
            LOGGER.info("Created notification: " + notification.getTitle() +
                    " from sender_code: " + notification.getSenderUsername());
        }

        return result;
    }

    public boolean sendNotificationToAll(Notification notification) throws Exception {
        notification.setTargetType(TargetType.ALL);
        notification.setTargetCode(null);

        return createNotification(notification);
    }

    public boolean sendNotificationToStudent(String studentCode, Notification notification) throws Exception {
        // Kiểm tra student exists
        if (studentDAO.findByStudentCode(studentCode) == null) {
            throw new IllegalArgumentException("Sinh viên không tồn tại");
        }

        notification.setTargetType(TargetType.STUDENT);
        notification.setTargetCode(studentCode);

        return createNotification(notification);
    }

    public boolean sendNotificationToClass(String classCode, Notification notification) throws Exception {
        notification.setTargetType(TargetType.CLASS);
        notification.setTargetCode(classCode);

        return createNotification(notification);
    }

    public boolean sendNotificationToFaculty(String facultyCode, Notification notification) throws Exception {
        notification.setTargetType(TargetType.FACULTY);
        notification.setTargetCode(facultyCode);

        return createNotification(notification);
    }

    public boolean updateNotification(Notification notification) throws Exception {
        validateNotification(notification);

        Notification existing = notificationDAO.findById(notification.getNotificationId());
        if (existing == null) {
            throw new IllegalArgumentException("Thông báo không tồn tại");
        }

        return notificationDAO.updateNotification(notification);
    }

    public boolean deleteNotification(int notificationId) throws Exception {
        Notification notification = notificationDAO.findById(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("Thông báo không tồn tại");
        }

        return notificationDAO.deleteNotification(notificationId);
    }

    public boolean markAsRead(int notificationId) throws Exception {
        return notificationDAO.markAsRead(notificationId);
    }

    public boolean markAllAsReadForUser(String username) throws Exception {
        return notificationDAO.markAllAsReadForUser(username);
    }

    public Notification getNotificationById(int notificationId) throws Exception {
        return notificationDAO.findById(notificationId);
    }

    public List<Notification> getNotificationsByUser(String username) throws Exception {
        return notificationDAO.getNotificationsByUser(username);
    }

    public List<Notification> getUnreadNotifications(String username) throws Exception {
        return notificationDAO.getUnreadNotificationsByUser(username);
    }

    public List<Notification> getAllNotifications() throws Exception {
        return notificationDAO.getAllNotifications();
    }

    public List<Notification> getNotificationsByClass(String classCode) throws Exception {
        return notificationDAO.getNotificationsByClass(classCode);
    }

    public List<Notification> getNotificationsByFaculty(String facultyCode) throws Exception {
        return notificationDAO.getNotificationsByFaculty(facultyCode);
    }

    public int countUnreadNotifications(String username) throws Exception {
        return notificationDAO.countUnreadByUser(username);
    }

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

        if (notification.getSenderUsername() == null || notification.getSenderUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Sender username không hợp lệ");
        }

        if (notification.getTargetType() == null) {
            throw new IllegalArgumentException("Target type không được để trống");
        }

        // Validate targetCode based on targetType
        if (notification.getTargetType() != TargetType.ALL && notification.getTargetCode() == null) {
            throw new IllegalArgumentException("Target code không được để trống khi target type không phải ALL");
        }

        if (notification.getPriority() == null) {
            throw new IllegalArgumentException("Priority không được để trống");
        }
    }

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
        }
    }
}
