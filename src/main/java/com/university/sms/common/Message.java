package com.university.sms.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp Message để giao tiếp giữa Client và Server
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MessageType type;
    private String action;
    private Map<String, Object> data;
    private boolean success;
    private String message;
    private long timestamp;

    public enum MessageType {
        REQUEST,    // Yêu cầu từ client
        RESPONSE,   // Phản hồi từ server
        NOTIFICATION // Thông báo từ server
    }

    // Constructors
    public Message() {
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public Message(MessageType type, String action) {
        this();
        this.type = type;
        this.action = action;
    }

    public Message(MessageType type, String action, boolean success, String message) {
        this(type, action);
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getData(String key) {
        return this.data.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = this.data.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    // Factory methods for common message types
    public static Message createRequest(String action) {
        return new Message(MessageType.REQUEST, action);
    }

    public static Message createResponse(String action, boolean success, String message) {
        return new Message(MessageType.RESPONSE, action, success, message);
    }

    public static Message createSuccessResponse(String action, String message) {
        return new Message(MessageType.RESPONSE, action, true, message);
    }

    public static Message createErrorResponse(String action, String message) {
        return new Message(MessageType.RESPONSE, action, false, message);
    }

    public static Message createNotification(String action, String message) {
        return new Message(MessageType.NOTIFICATION, action, true, message);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", action='" + action + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data.size() + " items" +
                '}';
    }
}


