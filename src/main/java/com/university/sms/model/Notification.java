package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model class cho bảng notifications (thông báo)
 * Hỗ trợ gửi thông báo đến: tất cả, khoa, lớp, sinh viên cụ thể
 * ✅ REFACTORED: Dùng sender_username, target_code làm FK (client-safe)
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    // Primary key
    private int notificationId;

    // ✅ NEW: Foreign keys dùng codes (KHÔNG bị conflict giữa clients)
    private String title;
    private String content;
    private String senderUsername; // FK to users.username
    private TargetType targetType;
    private String targetCode; // FK to faculty_code/class_code/student_code depending on targetType

    // ⚠️ DEPRECATED: Giữ lại để backward compatibility
    @Deprecated
    private int senderId; // Legacy field, use senderUsername instead
    @Deprecated
    private Integer targetId; // Legacy field, use targetCode instead

    private Priority priority;
    private boolean isRead;
    private Timestamp createdAt;
    private Timestamp updatedAt;
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

    // Constructors
    public Notification() {
        this.priority = Priority.MEDIUM;
        this.isRead = false;
        this.targetType = TargetType.ALL;
    }

    public Notification(String title, String content, String senderUsername) {
        this();
        this.title = title;
        this.content = content;
        this.senderUsername = senderUsername;
    }

    // Legacy constructor (deprecated)
    @Deprecated
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

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    // Deprecated getters/setters (keep for backward compat)
    @Deprecated
    public int getSenderId() {
        return senderId;
    }

    @Deprecated
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    @Deprecated
    public Integer getTargetId() {
        return targetId;
    }

    @Deprecated
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

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
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
     * 
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
     * 
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
