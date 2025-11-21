package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.StudentDAO;
import com.university.sms.dao.UserDAO;
import com.university.sms.model.Student;
import com.university.sms.model.User;
import com.university.sms.service.TimetableService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến thời khóa biểu
 */
public class TimetableHandler {
  private static final Logger LOGGER = Logger.getLogger(TimetableHandler.class.getName());

  private User currentUser;
  private final TimetableService timetableService;

  public TimetableHandler(User currentUser, TimetableService timetableService) {
    this.currentUser = currentUser;
    this.timetableService = timetableService;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetTimetable(Message request) {
    try {
      Integer userId = request.getData(Constants.KEY_USER_ID, Integer.class);
      String userRole = request.getData(Constants.KEY_USER_ROLE, String.class);

      if (userId == null) {
        userId = currentUser != null ? currentUser.getUserId() : null;
        userRole = currentUser != null ? currentUser.getRole().toString() : null;
      }

      if (userId == null || userRole == null) {
        return Message.createErrorResponse(request.getAction(), "User information is required");
      }

      List<?> timetable = null;

      if ("STUDENT".equalsIgnoreCase(userRole)) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(userId);
        if (user != null) {
          StudentDAO studentDAO = new StudentDAO();
          Student student = studentDAO.findByUsername(user.getUsername());
          if (student != null) {
            timetable = timetableService.getStudentTimetable(student.getStudentCode());
          }
        }
      } else if ("TEACHER".equalsIgnoreCase(userRole)) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(userId);
        if (user != null) {
          timetable = timetableService.getTeacherTimetable(user.getUsername());
        }
      }

      Message response = Message.createSuccessResponse(request.getAction(), "Timetable retrieved successfully");
      response.addData(Constants.KEY_TIMETABLE, timetable);
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting timetable", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleValidateSchedule(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code and course code are required");
      }

      boolean isValid = timetableService.validateSchedule(studentCode, courseCode);

      if (isValid) {
        return Message.createSuccessResponse(request.getAction(), "No schedule conflict");
      } else {
        return Message.createErrorResponse(request.getAction(), "Schedule conflict detected");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error validating schedule", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }
}
