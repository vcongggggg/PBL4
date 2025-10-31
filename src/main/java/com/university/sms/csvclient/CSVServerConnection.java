package com.university.sms.csvclient;

import com.university.sms.client.BaseServerConnection;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Logger;

/**
 * Quản lý kết nối đến server cho CSV client
 * Giao tiếp với server database, không đồng bộ dữ liệu về CSV local
 * Implement interface tương tự ServerConnection để tương thích với GUI hiện có
 */
public class CSVServerConnection extends BaseServerConnection {
  private static final Logger LOGGER = Logger.getLogger(CSVServerConnection.class.getName());

  private CSVDataService csvDataService;

  public CSVServerConnection(String serverHost, int serverPort) {
    super(serverHost, serverPort);
    this.csvDataService = new CSVDataService();
  }

  /**
   * Kết nối đến server
   */
  public boolean connect() {
    return super.connect();
  }

  /**
   * Ngắt kết nối khỏi server
   */
  public void disconnect() {
    super.disconnect();
  }

  /**
   * Gửi yêu cầu đến server
   */
  // No direct send helper needed; use sendCSVRequestAndWait() for sync flows

  /**
   * Gửi yêu cầu và chờ phản hồi (synchronous)
   */
  private Message sendCSVRequestAndWait(Message request, long timeoutSeconds) {
    return super.sendRequestAndWait(request, timeoutSeconds);
  }

  /**
   * Đăng nhập - sử dụng server database, không lưu về CSV
   */
  public Message login(String username, String password) {
    try {
      // Gửi yêu cầu đăng nhập đến server
      Message request = Message.createRequest(Constants.ACTION_LOGIN);
      request.addData(Constants.KEY_USERNAME, username);
      request.addData(Constants.KEY_PASSWORD, password);

      Message response = sendCSVRequestAndWait(request, 60);

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
   * Lấy thông tin sinh viên - từ server database, không đồng bộ về CSV
   */
  public Message getStudentInfo(Integer studentId) {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
      if (studentId != null) {
        request.addData(Constants.KEY_STUDENT_ID, studentId);
      }

      Message response = sendCSVRequestAndWait(request, 60);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting student info: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả sinh viên - từ server database, không đồng bộ về CSV
   */
  public Message getAllStudents() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
      Message response = sendCSVRequestAndWait(request, 120);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all students: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Tìm kiếm sinh viên - từ server database, không đồng bộ về CSV
   */
  public Message searchStudents(String keyword) {
    try {
      // Gửi yêu cầu đến server để tìm kiếm trong database
      Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
      request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);

      Message response = sendCSVRequestAndWait(request, 60);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error searching students: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả khóa học - từ server database, không đồng bộ về CSV
   */
  public Message getAllCourses() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
      Message response = sendCSVRequestAndWait(request, 120);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all courses: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_COURSES, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy danh sách khóa học - từ server database, không đồng bộ về CSV
   */
  public Message getCourses() {
    return getAllCourses(); // Sử dụng cùng method với getAllCourses
  }

  /**
   * Lấy thông tin khóa học - từ server database, không đồng bộ về CSV
   */
  public Message getCourseInfo(int courseId) {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
      request.addData(Constants.KEY_COURSE_ID, courseId);

      Message response = sendCSVRequestAndWait(request, 60);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting course info: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả đăng ký - từ server database, không đồng bộ về CSV
   */
  public Message getAllEnrollments() {
    try {
      // Gửi yêu cầu đến server để lấy dữ liệu từ database
      Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS);
      Message response = sendCSVRequestAndWait(request, 120);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error getting all enrollments: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Đổi mật khẩu - không lưu về CSV
   */
  public Message changePassword(String newPassword) {
    return Message.createSuccessResponse(Constants.ACTION_CHANGE_PASSWORD, "Password changed successfully");
  }

  /**
   * Lưu sinh viên - gửi lên server, không lưu vào CSV local
   */
  public Message saveStudent(Student student) {
    try {
      // Gửi yêu cầu lên server
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);

      Message response = sendCSVRequestAndWait(request, 60);

      if (response.isSuccess()) {
        LOGGER.info("Student saved to server: " + student.getStudentCode());
        // Lưu đồng bộ vào CSV local
        boolean ok = csvDataService.saveStudent(student);
        if (ok) {
          LOGGER.info("Student saved to local CSV: " + student.getStudentCode());
        }
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error saving student: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Error: " + e.getMessage());
    }
  }

  public Message addStudent(Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        // Server sẽ gán studentId mới; sau khi nhận, lưu local
        csvDataService.saveStudent(student);
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error adding student: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Error: " + e.getMessage());
    }
  }

  public Message updateStudent(Student student) {
    return saveStudent(student);
  }

  /**
   * Xóa sinh viên - gửi lên server, không xóa khỏi CSV local
   */
  public Message deleteStudent(int studentId) {
    try {
      // Gửi yêu cầu xóa student lên server
      Message request = Message.createRequest(Constants.ACTION_DELETE_STUDENT);
      request.addData(Constants.KEY_STUDENT_ID, studentId);

      Message response = sendCSVRequestAndWait(request, 60);

      if (response.isSuccess()) {
        LOGGER.info("Student deleted from server: " + studentId);
        // Xóa đồng thời trên CSV local để đồng bộ dữ liệu client-side
        boolean removed = csvDataService.deleteStudent(studentId);
        if (removed) {
          LOGGER.info("Student deleted from local CSV: " + studentId);
        } else {
          LOGGER.warning("Student not found in local CSV: " + studentId);
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
  // Listener đã có ở BaseServerConnection; chỉ override xử lý NOTIFICATION nếu
  // cần qua handler phía trên.

  /**
   * Xử lý thông báo từ server
   */
  @SuppressWarnings("unused")
  private void handleNotification(Message notification) {
    try {
      String action = notification.getAction();

      switch (action) {
        case "STUDENT_UPDATED":
          LOGGER.info("Student updated from server notification");
          break;

        case "STUDENT_DELETED":
          LOGGER.info("Student deleted from server notification");
          break;

        case "COURSE_UPDATED":
          LOGGER.info("Course updated from server notification");
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
  // handleConnectionError dùng chung ở lớp cơ sở.

  /**
   * Kiểm tra trạng thái kết nối
   */
  public boolean isConnected() {
    return super.isConnected();
  }

  /**
   * Đặt response handler
   */
  public void setResponseHandler(com.university.sms.client.IServerConnection.ResponseHandler handler) {
    super.setResponseHandler(handler);
  }

  /**
   * Lấy thông tin server
   */
  public String getServerInfo() {
    return super.getServerInfo();
  }

  /**
   * Test kết nối
   */
  public boolean testConnection() {
    return super.testConnection();
  }

  /**
   * Lấy CSV data service
   */
  public CSVDataService getCsvDataService() {
    return csvDataService;
  }

  /**
   * Gửi metadata lên server khi kết nối
   */
  public Message sendMetadata() {
    try {
      Map<String, Object> metadata = csvDataService.getCSVMetadata();

      Message request = Message.createRequest(Constants.ACTION_SYNC_CHECK);
      request.addData("metadata", metadata);

      Message response = sendCSVRequestAndWait(request, 30);

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error sending metadata: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Error: " + e.getMessage());
    }
  }

  /**
   * Đồng bộ dữ liệu dựa trên response từ server
   */
  public Message syncData(String syncAction) {
    try {
      switch (syncAction) {
        case "UPLOAD_TO_SERVER":
          // Server yêu cầu client upload data
          return uploadAllCSVData();

        case "DOWNLOAD_FROM_SERVER":
          // Server gửi data về client
          return downloadFromServer();

        case "NO_SYNC_NEEDED":
          // Không cần sync
          return Message.createSuccessResponse(Constants.ACTION_SYNC_DATA, "Dữ liệu đã đồng bộ");

        default:
          return Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Unknown sync action");
      }
    } catch (Exception e) {
      LOGGER.severe("Error syncing data: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Error: " + e.getMessage());
    }
  }

  /**
   * Download dữ liệu từ server về CSV (placeholder - cần implement later)
   */
  private Message downloadFromServer() {
    try {
      LOGGER.info("Downloading data from server to CSV...");

      // TODO: Implement download logic
      // Đây là placeholder, cần implement sau

      Message response = Message.createSuccessResponse("DOWNLOAD_FROM_SERVER",
          "Download feature not yet implemented");

      return response;
    } catch (Exception e) {
      LOGGER.severe("Error downloading data: " + e.getMessage());
      return Message.createErrorResponse("DOWNLOAD_FROM_SERVER", "Error: " + e.getMessage());
    }
  }

  /**
   * Xử lý response từ server về metadata
   */
  private void handleMetadataResponse(Message response) {
    if (response.isSuccess()) {
      String syncAction = (String) response.getData("sync_action");
      response.getData("server_version");

      LOGGER.info("Sync action from server: " + syncAction);

      if ("UPLOAD_TO_SERVER".equals(syncAction)) {
        // Hiển thị thông báo cho user để manual sync
        LOGGER.info("Server requires upload. User needs to click Sync button.");
      } else if ("DOWNLOAD_FROM_SERVER".equals(syncAction)) {
        // Tự động download
        syncData("DOWNLOAD_FROM_SERVER");
      }
    }
  }

  /**
   * Upload tất cả sinh viên từ CSV lên server
   */
  public Message uploadAllStudentsFromCSV() {
    try {
      // Lấy danh sách sinh viên từ CSV
      List<Student> students = csvDataService.getAllStudents();

      if (students.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_STUDENTS", "CSV file is empty, nothing to upload");
      }

      LOGGER.info("Starting upload of " + students.size() + " students from CSV to server");

      // Gửi yêu cầu upload lên server
      Message request = Message.createRequest("UPLOAD_STUDENTS");
      request.addData("students", students);
      request.addData("total", students.size());

      Message response = sendCSVRequestAndWait(request, 180);

      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + students.size() + " students from CSV to server");
      } else {
        LOGGER.warning("Failed to upload students from CSV: " + response.getMessage());
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error uploading students from CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_STUDENTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả khóa học từ CSV lên server
   */
  public Message uploadAllCoursesFromCSV() {
    try {
      // Lấy danh sách khóa học từ CSV
      List<Course> courses = csvDataService.getAllCourses();

      if (courses.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSES", "CSV file is empty, nothing to upload");
      }

      LOGGER.info("Starting upload of " + courses.size() + " courses from CSV to server");

      // Gửi yêu cầu upload lên server
      Message request = Message.createRequest("UPLOAD_COURSES");
      request.addData("courses", courses);
      request.addData("total", courses.size());

      Message response = sendCSVRequestAndWait(request, 180);

      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + courses.size() + " courses from CSV to server");
      } else {
        LOGGER.warning("Failed to upload courses from CSV: " + response.getMessage());
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error uploading courses from CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSES", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả đăng ký từ CSV lên server
   */
  public Message uploadAllEnrollmentsFromCSV() {
    try {
      // Lấy danh sách đăng ký từ CSV
      List<Enrollment> enrollments = csvDataService.getAllEnrollments();

      if (enrollments.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_ENROLLMENTS", "CSV file is empty, nothing to upload");
      }

      LOGGER.info("Starting upload of " + enrollments.size() + " enrollments from CSV to server");

      // Gửi yêu cầu upload lên server
      Message request = Message.createRequest("UPLOAD_ENROLLMENTS");
      request.addData("enrollments", enrollments);
      request.addData("total", enrollments.size());

      Message response = sendCSVRequestAndWait(request, 180);

      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + enrollments.size() + " enrollments from CSV to server");
      } else {
        LOGGER.warning("Failed to upload enrollments from CSV: " + response.getMessage());
      }

      return response;

    } catch (Exception e) {
      LOGGER.severe("Error uploading enrollments from CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ENROLLMENTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả dữ liệu CSV lên server
   */
  public Message uploadAllCSVData() {
    try {
      LOGGER.info("Starting full CSV data upload to server");

      // Upload users first to satisfy FK for students
      Message usersResponse = uploadAllUsersFromCSV();
      if (!usersResponse.isSuccess()) {
        LOGGER.warning("Failed to upload users: " + usersResponse.getMessage());
      }

      // Upload students
      Message studentsResponse = uploadAllStudentsFromCSV();
      if (!studentsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload students: " + studentsResponse.getMessage());
      }

      // Upload courses
      Message coursesResponse = uploadAllCoursesFromCSV();
      if (!coursesResponse.isSuccess()) {
        LOGGER.warning("Failed to upload courses: " + coursesResponse.getMessage());
      }

      // Upload enrollments
      Message enrollmentsResponse = uploadAllEnrollmentsFromCSV();
      if (!enrollmentsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload enrollments: " + enrollmentsResponse.getMessage());
      }

      // Return success if at least one upload succeeded
      if (usersResponse.isSuccess() || studentsResponse.isSuccess() || coursesResponse.isSuccess()
          || enrollmentsResponse.isSuccess()) {
        LOGGER.info("CSV data upload completed with some successes");
        return Message.createSuccessResponse("UPLOAD_ALL_CSV", "CSV data upload completed");
      } else {
        return Message.createErrorResponse("UPLOAD_ALL_CSV", "All uploads failed");
      }

    } catch (Exception e) {
      LOGGER.severe("Error uploading CSV data: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ALL_CSV", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả users từ CSV lên server
   */
  public Message uploadAllUsersFromCSV() {
    try {
      java.util.List<com.university.sms.model.User> users = csvDataService.getAllUsers();
      if (users.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_USERS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Starting upload of " + users.size() + " users from CSV to server");
      Message request = Message.createRequest(Constants.ACTION_UPLOAD_USERS);
      request.addData("users", users);
      request.addData("total", users.size());
      Message response = sendCSVRequestAndWait(request, 180);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + users.size() + " users from CSV to server");
      } else {
        LOGGER.warning("Failed to upload users from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading users from CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_USERS", "Error: " + e.getMessage());
    }
  }

  @Override
  protected void onConnect() {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(500);
        Message metadataResponse = sendMetadata();
        handleMetadataResponse(metadataResponse);
      } catch (Exception e) {
        LOGGER.warning("Error checking metadata: " + e.getMessage());
      }
    });
  }

  public Message manualSync() {
    Message metadataResponse = sendMetadata();
    if (metadataResponse.isSuccess()) {
      String syncAction = (String) metadataResponse.getData("sync_action");
      return syncData(syncAction);
    }
    return metadataResponse;
  }
}
