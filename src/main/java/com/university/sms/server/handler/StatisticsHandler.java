package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.server.StudentManagementServer;
import com.university.sms.service.TranscriptService;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến thống kê
 */
public class StatisticsHandler {
  private static final Logger LOGGER = Logger.getLogger(StatisticsHandler.class.getName());

  private User currentUser;
  private final TranscriptService transcriptService;
  private final java.util.function.Supplier<Integer> serverVersionSupplier;

  public StatisticsHandler(User currentUser,
      TranscriptService transcriptService,
      java.util.function.Supplier<Integer> serverVersionSupplier) {
    this.currentUser = currentUser;
    this.transcriptService = transcriptService;
    this.serverVersionSupplier = serverVersionSupplier;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetHonorStudents(Message request) {
    try {
      String facultyCode = request.getData(Constants.KEY_FACULTY_CODE, String.class);

      List<?> honorStudents = transcriptService.getHonorStudents(facultyCode);

      Message response = Message.createSuccessResponse(request.getAction(),
          "Honor students retrieved successfully");
      response.addData(Constants.KEY_HONOR_STUDENTS, honorStudents);
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting honor students", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetFacultyStatistics(Message request) {
    try {
      String facultyCode = request.getData("facultyCode", String.class);
      if (facultyCode == null || facultyCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Faculty code is required");
      }

      Map<String, Object> statistics = transcriptService.getFacultyStatistics(facultyCode);

      Message response = Message.createSuccessResponse(request.getAction(),
          "Faculty statistics retrieved successfully");
      response.addData(Constants.KEY_STATISTICS, statistics);
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting faculty statistics", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetServerStatistics(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
      }

      StudentManagementServer server = StudentManagementServer.getInstance();
      if (server == null) {
        return Message.createErrorResponse(request.getAction(), "Server instance not available");
      }

      StudentManagementServer.ServerStatistics stats = server.getStatistics();

      int serverDbVersion = serverVersionSupplier.get();

      Message response = Message.createSuccessResponse(request.getAction(),
          "Server statistics retrieved successfully");
      response.addData("totalClients", stats.getConnectedClients());
      response.addData("adminClients", stats.getAdminClients());
      response.addData("teacherClients", stats.getTeacherClients());
      response.addData("studentClients", stats.getStudentClients());
      response.addData("serverDbVersion", serverDbVersion);

      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting server statistics", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }
  public Message handleGetGpaTrend(Message request) {
    try {
        String facultyCode = request.getData(Constants.KEY_FACULTY_CODE, String.class);

        // Only admin or teacher allowed
        if (currentUser == null || (currentUser.getRole() != User.UserRole.ADMIN
                && currentUser.getRole() != User.UserRole.TEACHER)) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
        }

        Map<String, Double> trend = transcriptService.getGpaTrendBySemester(facultyCode);

        Message response = Message.createSuccessResponse(request.getAction(),
                "GPA trend retrieved successfully");
        response.addData(Constants.KEY_GPA_TREND, trend);
        return response;
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error getting GPA trend", e);
        return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }
}
