package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model class cho bảng notifications (thông báo)
 * Hỗ trợ gửi thông báo đến: tất cả, khoa, lớp, sinh viên cụ thể
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private int notificationId;
    private String title;
    private String content;
    private int senderId;
    private TargetType targetType;
    private Integer targetId;
    private Priority priority;
    private boolean isRead;
    private Timestamp createdAt;
    private Timestamp expiresAt;

    // Related information (from joins)
    private String senderName;
    private String senderRole;
    private String targetName;

    /**
     * Loại đối tượng nhận thông báo
     */
    public enum TargetType {
        ALL("Tất cả"),
        FACULTY("Khoa"),
        CLASS("Lớp"),
        STUDENT("Sinh viên");

        private final String displayName;

        TargetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Mức độ ưu tiên
     */
    public enum Priority {
        LOW("Thấp"),
        MEDIUM("Trung bình"),
        HIGH("Cao"),
        URGENT("Khẩn cấp");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Loại thông báo
     */
    public enum NotificationType {
        GENERAL,
        COURSE_UPDATE,
        GRADE_UPDATE,
        ENROLLMENT_CONFIRMED,
        COURSE_CANCELLED,
        REGISTRATION_OPENED,
        REGISTRATION_APPROVED,
        REGISTRATION_REJECTED,
        CLASS_REQUEST_APPROVED,
        CLASS_REQUEST_REJECTED,
        SYSTEM_MAINTENANCE
    }

    // Constructors
    public Notification() {
        this.priority = Priority.MEDIUM;
        this.isRead = false;
        this.targetType = TargetType.ALL;
    }

    public Notification(String title, String content, int senderId) {
        this();
        this.title = title;
        this.content = content;
        this.senderId = senderId;
    }

    // Getters and Setters
    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Related information getters/setters
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    /**
     * Kiểm tra thông báo đã hết hạn chưa
     * @return true nếu đã hết hạn
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt.getTime();
    }

    /**
     * Lấy icon theo priority
     * @return Tên icon
     */
    public String getPriorityIcon() {
        switch (priority) {
            case URGENT:
                return "⚠️";
            case HIGH:
                return "🔴";
            case MEDIUM:
                return "🟡";
            case LOW:
            default:
                return "🔵";
        }
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", title='" + title + '\'' +
                ", senderId=" + senderId +
                ", targetType=" + targetType +
                ", priority=" + priority +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}

