package com.university.sms.csvclient;

import com.university.sms.common.Message;

/**
 * Adapter để CSVServerConnection có thể hoạt động với MainFrame hiện có
 * Chuyển đổi giữa CSVServerConnection và ServerConnection interface
 */
public class CSVServerConnectionAdapter extends com.university.sms.client.ServerConnection {

  private CSVServerConnection csvConnection;

  public CSVServerConnectionAdapter(CSVServerConnection csvConnection) {
    super("localhost", 8888); // Dummy values, không sử dụng
    this.csvConnection = csvConnection;
  }

  @Override
  public boolean connect() {
    return csvConnection.connect();
  }

  @Override
  public void disconnect() {
    csvConnection.disconnect();
  }

  @Override
  public boolean isConnected() {
    return csvConnection.isConnected();
  }

  @Override
  public Message login(String username, String password) {
    return csvConnection.login(username, password);
  }

  @Override
  public Message logout() {
    return csvConnection.logout();
  }

  @Override
  public Message getStudentInfo(Integer studentId) {
    return csvConnection.getStudentInfo(studentId);
  }

  @Override
  public Message getAllStudents() {
    return csvConnection.getAllStudents();
  }

  @Override
  public Message searchStudents(String keyword) {
    return csvConnection.searchStudents(keyword);
  }

  @Override
  public Message getAllCourses() {
    return csvConnection.getAllCourses();
  }

  @Override
  public Message getCourses() {
    return csvConnection.getCourses();
  }

  @Override
  public Message getCourseInfo(int courseId) {
    return csvConnection.getCourseInfo(courseId);
  }

  @Override
  public Message changePassword(String newPassword) {
    return csvConnection.changePassword(newPassword);
  }

  @Override
  public void setResponseHandler(com.university.sms.client.ServerConnection.ResponseHandler handler) {
    // Chuyển đổi handler từ ServerConnection sang CSVServerConnection
    csvConnection.setResponseHandler(new CSVServerConnection.ResponseHandler() {
      @Override
      public void onResponse(Message response) {
        handler.onResponse(response);
      }

      @Override
      public void onError(String error) {
        handler.onError(error);
      }

      @Override
      public void onDisconnected() {
        handler.onDisconnected();
      }
    });
  }

  @Override
  public String getServerInfo() {
    return csvConnection.getServerInfo();
  }

  @Override
  public boolean testConnection() {
    return csvConnection.testConnection();
  }

  // Thêm các method mới cho CSV client
  public Message saveStudent(com.university.sms.model.Student student) {
    return csvConnection.saveStudent(student);
  }

  public Message deleteStudent(int studentId) {
    return csvConnection.deleteStudent(studentId);
  }

  public Message getAllEnrollments() {
    return csvConnection.getAllEnrollments();
  }

  public CSVDataService getCsvDataService() {
    return csvConnection.getCsvDataService();
  }
}
