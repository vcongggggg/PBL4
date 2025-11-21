package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Notification;
import com.university.sms.model.User;
import com.university.sms.service.NotificationService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến thông báo
 */
public class NotificationHandler {
  private static final Logger LOGGER = Logger.getLogger(NotificationHandler.class.getName());

  private User currentUser;
  private final NotificationService notificationService;

  public NotificationHandler(User currentUser, NotificationService notificationService) {
    this.currentUser = currentUser;
    this.notificationService = notificationService;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetNotifications(Message request) {
    try {
      Integer userId = request.getData(Constants.KEY_USER_ID, Integer.class);

      List<Notification> notifications;
      String username = null;

      if (userId != null) {
        com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
        User user = userDAO.findById(userId);
        if (user != null) {
          username = user.getUsername();
        }
      } else if (currentUser != null) {
        username = currentUser.getUsername();
      }

      if (username == null || username.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "User information is required");
      }

      notifications = notificationService.getNotificationsByUser(username);

      int unreadCount = 0;
      for (Notification n : notifications) {
        if (!n.isRead()) {
          unreadCount++;
        }
      }

      Message response = Message.createSuccessResponse(request.getAction(),
          "Lấy danh sách thông báo thành công");
      response.addData(Constants.KEY_NOTIFICATIONS, notifications);
      response.addData(Constants.KEY_UNREAD_COUNT, unreadCount);
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting notifications", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleSendNotification(Message request) {
    try {
      Notification notification = request.getData(Constants.KEY_NOTIFICATION, Notification.class);
      if (notification == null) {
        return Message.createErrorResponse(request.getAction(), "Notification data is required");
      }

      if (notification.getSenderUsername() == null && currentUser != null) {
        notification.setSenderUsername(currentUser.getUsername());
      }

      boolean result = notificationService.createNotification(notification);

      if (result) {
        return Message.createSuccessResponse(request.getAction(), "Gửi thông báo thành công");
      } else {
        return Message.createErrorResponse(request.getAction(), "Gửi thông báo thất bại");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error sending notification", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleMarkNotificationRead(Message request) {
    try {
      Integer notificationId = request.getData(Constants.KEY_NOTIFICATION_ID, Integer.class);

      if (notificationId != null) {
        boolean result = notificationService.markAsRead(notificationId);
        if (result) {
          return Message.createSuccessResponse(request.getAction(), "Đánh dấu đã đọc thành công");
        } else {
          return Message.createErrorResponse(request.getAction(), "Đánh dấu đã đọc thất bại");
        }
      } else if (currentUser != null) {
        boolean result = notificationService.markAllAsReadForUser(currentUser.getUsername());
        if (result) {
          return Message.createSuccessResponse(request.getAction(),
              "Đánh dấu tất cả thông báo đã đọc thành công");
        } else {
          return Message.createErrorResponse(request.getAction(),
              "Đánh dấu thông báo đã đọc thất bại");
        }
      } else {
        return Message.createErrorResponse(request.getAction(), "Notification ID is required");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error marking notification as read", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }
}
