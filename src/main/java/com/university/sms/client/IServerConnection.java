package com.university.sms.client;

import com.university.sms.common.Message;

/**
 * Interface chuẩn hóa kết nối client-server để GUI phụ thuộc vào interface này.
 */
public interface IServerConnection {

  interface ResponseHandler {
    void onResponse(Message response);

    void onError(String error);

    void onDisconnected();
  }

  boolean connect();

  void disconnect();

  boolean isConnected();

  void setResponseHandler(ResponseHandler handler);

  String getServerInfo();

  boolean testConnection();

  // Auth
  Message login(String username, String password);

  Message logout();

  // Student APIs
  Message getStudentInfo(Integer studentId);

  Message getAllStudents();

  Message searchStudents(String keyword);

  Message deleteStudent(String studentCode);

  Message addStudent(com.university.sms.model.Student student);

  Message updateStudent(com.university.sms.model.Student student);

  // Course APIs
  Message getAllCourses();

  Message getCourseInfo(int courseId);

  // User profile
  Message changePassword(String newPassword);

  // Generic request sender for custom actions
  Message sendRequest(Message request);

  // Server statistics (admin only)
  Message getServerStatistics();
}
