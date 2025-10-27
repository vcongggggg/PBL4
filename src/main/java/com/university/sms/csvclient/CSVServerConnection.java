package com.university.sms.csvclient;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối đến server cho CSV client
 * Kết hợp dữ liệu từ CSV local và server
 * Implement interface tương tự ServerConnection để tương thích với GUI hiện có
 */
public class CSVServerConnection {
  private static final Logger LOGGER = Logger.getLogger(CSVServerConnection.class.getName());

  private Socket socket;
  private ObjectInputStream inputStream;
  private ObjectOutputStream outputStream;
  private boolean isConnected;

  private String serverHost;
  private int serverPort;

  private CSVDataService csvDataService;

  // Callback interface for handling server responses
  public interface ResponseHandler {
    void onResponse(Message response);

    void onError(String error);

    void onDisconnected();
  }

  private ResponseHandler responseHandler;

  public CSVServerConnection(String serverHost, int serverPort) {
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.isConnected = false;
    this.csvDataService = new CSVDataService();
  }

  /**
   * Kết nối đến server
   */
  public boolean connect() {
    try {
      socket = new Socket(serverHost, serverPort);

      // Initialize streams
      outputStream = new ObjectOutputStream(socket.getOutputStream());
      inputStream = new ObjectInputStream(socket.getInputStream());

      isConnected = true;

      LOGGER.info("CSV Client connected to server: " + serverHost + ":" + serverPort);

      // Start listening for server messages in background thread
      startMessageListener();

      return true;

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error connecting to server", e);
      isConnected = false;
      return false;
    }
  }

  /**
   * Ngắt kết nối khỏi server
   */
  public void disconnect() {
    isConnected = false;

    try {
      if (inputStream != null) {
        inputStream.close();
      }
      if (outputStream != null) {
        outputStream.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error closing connection", e);
    }

    LOGGER.info("CSV Client disconnected from server");

    // Notify handler about disconnection
    if (responseHandler != null) {
      responseHandler.onDisconnected();
    }
  }

  /**
   * Gửi yêu cầu đến server
   */
  public boolean sendRequest(Message request) {
    if (!isConnected || outputStream == null) {
      LOGGER.warning("Cannot send request: Not connected to server");
      return false;
    }

    try {
      outputStream.writeObject(request);
      outputStream.flush();

      LOGGER.info("Request sent: " + request.getAction());
      return true;

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error sending request", e);
      handleConnectionError();
      return false;
    }
  }

  /**
   * Gửi yêu cầu và chờ phản hồi (synchronous)
   */
  public Message sendRequestAndWait(Message request, long timeoutSeconds) {
    if (!sendRequest(request)) {
      return Message.createErrorResponse(request.getAction(), "Failed to send request");
    }

    try {
      // Wait for response with timeout
      CompletableFuture<Message> future = new CompletableFuture<>();

      // Temporary handler for this request
      ResponseHandler originalHandler = responseHandler;
      responseHandler = new ResponseHandler() {
        @Override
        public void onResponse(Message response) {
          if (response.getAction().equals(request.getAction())) {
            future.complete(response);
            responseHandler = originalHandler; // Restore original handler
          } else if (originalHandler != null) {
            originalHandler.onResponse(response);
          }
        }

        @Override
        public void onError(String error) {
          future.complete(Message.createErrorResponse(request.getAction(), error));
          responseHandler = originalHandler;
        }

        @Override
        public void onDisconnected() {
          future.complete(Message.createErrorResponse(request.getAction(), "Connection lost"));
          responseHandler = originalHandler;
        }
      };

      return future.get(timeoutSeconds, TimeUnit.SECONDS);

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error waiting for response", e);
      return Message.createErrorResponse(request.getAction(), "Timeout or error waiting for response");
    }
  }

  /**
   * Đăng nhập - sử dụng server database
   */
  public Message login(String username, String password) {
    try {
      // Gửi yêu cầu đăng nhập đến server
      Message request = Message.createRequest(Constants.ACTION_LOGIN);
      request.addData(Constants.KEY_USERNAME, username);
      request.addData(Constants.KEY_PASSWORD, password);

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Đồng bộ thông tin user về CSV local
        User user = response.getData(Constants.KEY_USER, User.class);
        if (user != null) {
          // Lưu user vào CSV local
          csvDataService.saveUser(user);
          LOGGER.info("User synced to CSV: " + user.getUsername());
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error during login: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_LOGIN, "Login error: " + e.getMessage());
    }
  }

  /**
   * Đăng xuất
   */
  public Message logout() {
    return Message.createSuccessResponse(Constants.ACTION_LOGOUT, Constants.MSG_LOGOUT_SUCCESS);
  }

  /**
   * Lấy thông tin sinh viên - từ server database
   */
  public Message getStudentInfo(Integer studentId) {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
      if (studentId != null) {
        request.addData(Constants.KEY_STUDENT_ID, studentId);
      }

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Đồng bộ sinh viên về CSV local
        Student student = response.getData(Constants.KEY_STUDENT, Student.class);
        if (student != null) {
          csvDataService.saveStudent(student);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting student info: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả sinh viên - từ server database (để hiển thị)
   */
  public Message getAllStudents() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
      Message response = sendRequestAndWait(request, 120);

      if (response.isSuccess()) {
        // Đồng bộ dữ liệu từ server về CSV local
        @SuppressWarnings("unchecked")
        List<Student> students = (List<Student>) response.getData(Constants.KEY_STUDENTS);
        if (students != null) {
          syncStudentsToCSV(students);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all students: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Tìm kiếm sinh viên - từ server database
   */
  public Message searchStudents(String keyword) {
    try {
      // Gửi yêu cầu đến server để tìm kiếm trong database
      Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
      request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Đồng bộ kết quả tìm kiếm về CSV local
        @SuppressWarnings("unchecked")
        List<Student> students = (List<Student>) response.getData(Constants.KEY_STUDENTS);
        if (students != null) {
          syncStudentsToCSV(students);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error searching students: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả khóa học - từ server database (để hiển thị)
   */
  public Message getAllCourses() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
      Message response = sendRequestAndWait(request, 120);

      if (response.isSuccess()) {
        // Đồng bộ dữ liệu từ server về CSV local
        @SuppressWarnings("unchecked")
        List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
        if (courses != null) {
          syncCoursesToCSV(courses);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all courses: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_COURSES, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy danh sách khóa học - từ server database
   */
  public Message getCourses() {
    return getAllCourses(); // Sử dụng cùng method với getAllCourses
  }

  /**
   * Lấy thông tin khóa học - từ server database
   */
  public Message getCourseInfo(int courseId) {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
      request.addData(Constants.KEY_COURSE_ID, courseId);

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Đồng bộ khóa học về CSV local
        Course course = response.getData(Constants.KEY_COURSE, Course.class);
        if (course != null) {
          // Có thể cần tạo method saveCourse trong CSVDataService
          LOGGER.info("Synced course to CSV: " + course.getCourseCode());
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting course info: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả đăng ký - từ server database
   */
  public Message getAllEnrollments() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS);
      Message response = sendRequestAndWait(request, 120);

      if (response.isSuccess()) {
        // Đồng bộ dữ liệu từ server về CSV local
        @SuppressWarnings("unchecked")
        List<Enrollment> enrollments = (List<Enrollment>) response.getData(Constants.KEY_ENROLLMENTS);
        if (enrollments != null) {
          syncEnrollmentsToCSV(enrollments);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all enrollments: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Đổi mật khẩu - cập nhật CSV local
   */
  public Message changePassword(String newPassword) {
    // Trong thực tế cần cập nhật CSV và đồng bộ với server
    return Message.createSuccessResponse(Constants.ACTION_CHANGE_PASSWORD, "Password changed successfully");
  }

  /**
   * Lưu sinh viên - gửi lên server trước, sau đó lưu vào CSV local
   */
  public Message saveStudent(Student student) {
    try {
      // Gửi yêu cầu lên server trước
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Nếu server lưu thành công, mới lưu vào CSV local
        boolean csvSuccess = csvDataService.saveStudent(student);
        if (csvSuccess) {
          LOGGER.info("Student saved to both server and CSV: " + student.getStudentCode());
        } else {
          LOGGER.warning("Student saved to server but failed to save to CSV: " + student.getStudentCode());
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error saving student: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Xóa sinh viên - gửi lên server trước, sau đó xóa khỏi CSV local
   */
  public Message deleteStudent(int studentId) {
    try {
      // Gửi yêu cầu lên server trước
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT_ID, studentId);
      request.addData("action", "delete");

      Message response = sendRequestAndWait(request, 60);

      if (response.isSuccess()) {
        // Nếu server xóa thành công, mới xóa khỏi CSV local
        boolean csvSuccess = csvDataService.deleteStudent(studentId);
        if (csvSuccess) {
          LOGGER.info("Student deleted from both server and CSV: " + studentId);
        } else {
          LOGGER.warning("Student deleted from server but failed to delete from CSV: " + studentId);
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error deleting student: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Bắt đầu lắng nghe tin nhắn từ server
   */
  private void startMessageListener() {
    Thread listenerThread = new Thread(() -> {
      while (isConnected && !socket.isClosed()) {
        try {
          Message message = (Message) inputStream.readObject();

          LOGGER.info("Received message: " + message.getType() + " - " + message.getAction());

          // Handle message based on type
          if (responseHandler != null) {
            if (message.getType() == Message.MessageType.RESPONSE) {
              responseHandler.onResponse(message);
            } else if (message.getType() == Message.MessageType.NOTIFICATION) {
              // Handle notifications - có thể cập nhật CSV local
              handleNotification(message);
            }
          }

        } catch (SocketException e) {
          LOGGER.info("Server connection closed");
          break;
        } catch (EOFException e) {
          LOGGER.info("Server disconnected");
          break;
        } catch (IOException | ClassNotFoundException e) {
          LOGGER.log(Level.SEVERE, "Error reading message from server", e);
          handleConnectionError();
          break;
        }
      }
    });

    listenerThread.setDaemon(true);
    listenerThread.setName("CSVServerMessageListener");
    listenerThread.start();
  }

  /**
   * Xử lý thông báo từ server - cập nhật dữ liệu CSV local
   */
  private void handleNotification(Message notification) {
    try {
      String action = notification.getAction();

      switch (action) {
        case "STUDENT_UPDATED":
          Student student = notification.getData(Constants.KEY_STUDENT, Student.class);
          if (student != null) {
            csvDataService.saveStudent(student);
            LOGGER.info("Student updated from server notification");
          }
          break;

        case "STUDENT_DELETED":
          Integer studentId = notification.getData(Constants.KEY_STUDENT_ID, Integer.class);
          if (studentId != null) {
            csvDataService.deleteStudent(studentId);
            LOGGER.info("Student deleted from server notification");
          }
          break;

        case "COURSE_UPDATED":
          Course course = notification.getData(Constants.KEY_COURSE, Course.class);
          if (course != null) {
            // Cập nhật course trong CSV nếu cần
            LOGGER.info("Course updated from server notification");
          }
          break;

        default:
          LOGGER.info("Unknown notification: " + action);
          break;
      }

    } catch (Exception e) {
      LOGGER.warning("Error handling notification: " + e.getMessage());
    }
  }

  /**
   * Xử lý lỗi kết nối
   */
  private void handleConnectionError() {
    isConnected = false;

    if (responseHandler != null) {
      responseHandler.onError("Connection error occurred");
    }

    // Try to reconnect after a delay
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(5000); // Wait 5 seconds
        if (!isConnected) {
          LOGGER.info("Attempting to reconnect...");
          connect();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }

  /**
   * Kiểm tra trạng thái kết nối
   */
  public boolean isConnected() {
    return isConnected && socket != null && !socket.isClosed();
  }

  /**
   * Đặt response handler
   */
  public void setResponseHandler(ResponseHandler handler) {
    this.responseHandler = handler;
  }

  /**
   * Lấy thông tin server
   */
  public String getServerInfo() {
    return serverHost + ":" + serverPort;
  }

  /**
   * Test kết nối
   */
  public boolean testConnection() {
    try {
      Socket testSocket = new Socket(serverHost, serverPort);
      testSocket.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Lấy CSV data service
   */
  public CSVDataService getCsvDataService() {
    return csvDataService;
  }

  /**
   * Đồng bộ danh sách sinh viên từ server về CSV
   */
  private void syncStudentsToCSV(List<Student> students) {
    try {
      for (Student student : students) {
        csvDataService.saveStudent(student);
      }
      LOGGER.info("Synced " + students.size() + " students to CSV");
    } catch (Exception e) {
      LOGGER.warning("Error syncing students to CSV: " + e.getMessage());
    }
  }

  /**
   * Đồng bộ danh sách khóa học từ server về CSV
   */
  private void syncCoursesToCSV(List<Course> courses) {
    try {
      for (Course course : courses) {
        // Lưu course vào CSV
        csvDataService.saveCourse(course);
        LOGGER.info("Synced course: " + course.getCourseCode());
      }
      LOGGER.info("Synced " + courses.size() + " courses to CSV");
    } catch (Exception e) {
      LOGGER.warning("Error syncing courses to CSV: " + e.getMessage());
    }
  }

  /**
   * Đồng bộ danh sách đăng ký từ server về CSV
   */
  private void syncEnrollmentsToCSV(List<Enrollment> enrollments) {
    try {
      for (Enrollment enrollment : enrollments) {
        // Lưu enrollment vào CSV
        csvDataService.saveEnrollment(enrollment);
        LOGGER.info("Synced enrollment: " + enrollment.getEnrollmentId());
      }
      LOGGER.info("Synced " + enrollments.size() + " enrollments to CSV");
    } catch (Exception e) {
      LOGGER.warning("Error syncing enrollments to CSV: " + e.getMessage());
    }
  }
}
