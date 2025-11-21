package com.university.sms.csvclient;

import com.university.sms.client.BaseServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Faculty;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Notification;
import com.university.sms.model.Subject;
import com.university.sms.model.Grade;
import com.university.sms.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối đến server cho CSV client
 * Tự động sync dữ liệu dựa trên version timestamp
 */
public class CSVServerConnection extends BaseServerConnection {
  private static final Logger LOGGER = Logger.getLogger(CSVServerConnection.class.getName());

  private CSVDataService csvDataService;

  public CSVServerConnection(String serverHost, int serverPort) {
    super(serverHost, serverPort);
    this.csvDataService = new CSVDataService();
  }

  /**
   * Gửi yêu cầu và chờ phản hồi (synchronous)
   */
  private Message sendCSVRequestAndWait(Message request, long timeoutSeconds) {
    return super.sendRequestAndWait(request, timeoutSeconds);
  }

  /**
   * Đăng nhập
   */
  @Override
  public Message login(String username, String password) {
    try {
      Message request = Message.createRequest(Constants.ACTION_LOGIN);
      request.addData(Constants.KEY_USERNAME, username);
      request.addData(Constants.KEY_PASSWORD, password);
      Message response = sendCSVRequestAndWait(request, 60);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đăng nhập: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_LOGIN, "Login error: " + e.getMessage());
    }
  }

  /**
   * Đăng xuất
   */
  @Override
  public Message logout() {
    return Message.createSuccessResponse(Constants.ACTION_LOGOUT, Constants.MSG_LOGOUT_SUCCESS);
  }

  /**
   * Lấy thông tin sinh viên
   */
  @Override
  public Message getStudentInfo(Integer studentId) {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
      if (studentId != null) {
        request.addData(Constants.KEY_STUDENT_ID, studentId);
      }
      return sendCSVRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy thông tin sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả sinh viên
   */
  @Override
  public Message getAllStudents() {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
      return sendCSVRequestAndWait(request, 120);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách tất cả sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Tìm kiếm sinh viên
   */
  @Override
  public Message searchStudents(String keyword) {
    try {
      Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
      request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);
      return sendCSVRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tìm kiếm sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Thêm sinh viên
   * Chỉ gọi server, không cập nhật CSV local
   * CSV local sẽ được cập nhật qua sync check và download
   */
  @Override
  public Message addStudent(Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      Message response = sendCSVRequestAndWait(request, 60);
      // Không cập nhật CSV local ở đây
      // CSV local sẽ được cập nhật qua sync check và download từ server
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi thêm sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Cập nhật sinh viên
   * Chỉ gọi server, không cập nhật CSV local
   * CSV local sẽ được cập nhật qua sync check và download
   */
  @Override
  public Message updateStudent(Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      Message response = sendCSVRequestAndWait(request, 60);
      // Không cập nhật CSV local ở đây
      // CSV local sẽ được cập nhật qua sync check và download từ server
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi cập nhật sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Xóa sinh viên
   */
  @Override
  public Message deleteStudent(String studentCode) {
    try {
      Message request = Message.createRequest(Constants.ACTION_DELETE_STUDENT);
      request.addData("studentCode", studentCode);
      Message response = sendCSVRequestAndWait(request, 60);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xóa sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_DELETE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả khóa học
   */
  @Override
  public Message getAllCourses() {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
      return sendCSVRequestAndWait(request, 120);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách tất cả khóa học: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_COURSES, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy danh sách khóa học
   */
  @Override
  public Message getCourseInfo(int courseId) {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
      request.addData(Constants.KEY_COURSE_ID, courseId);
      return sendCSVRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy thông tin khóa học: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, "Error: " + e.getMessage());
    }
  }

  /**
   * Đổi mật khẩu
   */
  @Override
  public Message changePassword(String newPassword) {
    return Message.createSuccessResponse(Constants.ACTION_CHANGE_PASSWORD, "Password changed successfully");
  }

  /**
   * Generic sendRequest implementation
   */
  @Override
  public Message sendRequest(Message request) {
    return sendCSVRequestAndWait(request, 60);
  }

  /**
   * Lấy thống kê server (admin only)
   */
  @Override
  public Message getServerStatistics() {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_SERVER_STATISTICS);
      return sendCSVRequestAndWait(request, 30);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy thống kê server: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_SERVER_STATISTICS, "Error: " + e.getMessage());
    }
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
      // Tăng timeout lên 120 giây vì getServerMetadata() thực hiện nhiều query
      Message response = sendCSVRequestAndWait(request, 120);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi gửi metadata: " + e.getMessage());
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
          return uploadAllCSVData();
        case "DOWNLOAD_FROM_SERVER":
          return downloadFromServer();
        case "NO_SYNC_NEEDED":
          return Message.createSuccessResponse(Constants.ACTION_SYNC_DATA, "Dữ liệu đã đồng bộ");
        default:
          return Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Unknown sync action");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đồng bộ dữ liệu: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Error: " + e.getMessage());
    }
  }

  /**
   * Download dữ liệu từ server về CSV
   */
  private Message downloadFromServer() {
    try {
      LOGGER.info("Đang tải dữ liệu từ server về CSV...");

      // 1) Gửi request tải dữ liệu
      Message request = Message.createRequest(Constants.ACTION_DOWNLOAD_DATA);
      Message response = sendCSVRequestAndWait(request, 180);
      if (!response.isSuccess()) {
        return response;
      }

      // 2) Lấy version server để set lại sau khi ghi xong
      Object serverVersionObj = response.getData("client_source_version");
      Integer serverVersion = null;
      if (serverVersionObj instanceof Integer) {
        serverVersion = (Integer) serverVersionObj;
      } else if (serverVersionObj instanceof Long) {
        serverVersion = ((Long) serverVersionObj).intValue();
      } else if (serverVersionObj instanceof String) {
        try {
          serverVersion = Integer.parseInt((String) serverVersionObj);
        } catch (NumberFormatException ignore) {
        }
      }

      // 3) Đọc danh sách dữ liệu
      @SuppressWarnings("unchecked")
      List<Student> students = (List<Student>) response.getData("students");
      @SuppressWarnings("unchecked")
      List<Course> courses = (List<Course>) response.getData("courses");
      @SuppressWarnings("unchecked")
      List<Enrollment> enrollments = (List<Enrollment>) response.getData("enrollments");
      @SuppressWarnings("unchecked")
      List<Faculty> faculties = (List<Faculty>) response.getData("faculties");
      @SuppressWarnings("unchecked")
      List<com.university.sms.model.Class> classes = (List<com.university.sms.model.Class>) response.getData("classes");
      @SuppressWarnings("unchecked")
      List<Subject> subjects = (List<Subject>) response.getData("subjects");
      @SuppressWarnings("unchecked")
      List<User> users = (List<User>) response.getData("users");
      @SuppressWarnings("unchecked")
      List<Grade> grades = (List<Grade>) response.getData("grades");
      @SuppressWarnings("unchecked")
      List<Notification> notifications = (List<Notification>) response.getData("notifications");
      @SuppressWarnings("unchecked")
      List<ClassOpeningRequest> classOpeningRequests = (List<ClassOpeningRequest>) response
          .getData("classOpeningRequests");
      @SuppressWarnings("unchecked")
      List<CourseRegistration> courseRegistrations = (List<CourseRegistration>) response.getData("courseRegistrations");

      // 4) Ghi dữ liệu vào CSV local không tăng version giữa chừng
      csvDataService.setSkipVersionIncrement(true);
      int saved = 0;
      int usersSaved = 0, facultiesSaved = 0, subjectsSaved = 0, classesSaved = 0;
      int coursesSaved = 0, studentsSaved = 0, enrollmentsSaved = 0, gradesSaved = 0;
      int notificationsSaved = 0, classOpeningRequestsSaved = 0, courseRegistrationsSaved = 0;

      try {
        if (users != null) {
          LOGGER.info("Đang lưu " + users.size() + " người dùng vào CSV...");
          try {
            if (csvDataService.saveAllUsers(users)) {
              usersSaved = users.size();
              saved += users.size();
              LOGGER.info("Đã lưu thành công " + users.size() + " người dùng vào CSV");
            } else {
              LOGGER.warning("Không thể lưu người dùng vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu người dùng", e);
          }
        }
        if (faculties != null) {
          LOGGER.info("Đang lưu " + faculties.size() + " khoa vào CSV...");
          try {
            if (csvDataService.saveAllFaculties(faculties)) {
              facultiesSaved = faculties.size();
              saved += faculties.size();
              LOGGER.info("Đã lưu thành công " + faculties.size() + " khoa vào CSV");
            } else {
              LOGGER.warning("Không thể lưu khoa vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu khoa", e);
          }
        }
        if (subjects != null) {
          LOGGER.info("Đang lưu " + subjects.size() + " môn học vào CSV...");
          try {
            if (csvDataService.saveAllSubjects(subjects)) {
              subjectsSaved = subjects.size();
              saved += subjects.size();
              LOGGER.info("Đã lưu thành công " + subjects.size() + " môn học vào CSV");
            } else {
              LOGGER.warning("Không thể lưu môn học vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu môn học", e);
          }
        }
        if (classes != null) {
          LOGGER.info("Đang lưu " + classes.size() + " lớp vào CSV...");
          try {
            if (csvDataService.saveAllClasses(classes)) {
              classesSaved = classes.size();
              saved += classes.size();
              LOGGER.info("Đã lưu thành công " + classes.size() + " lớp vào CSV");
            } else {
              LOGGER.warning("Không thể lưu lớp vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu lớp", e);
          }
        }
        if (courses != null) {
          LOGGER.info("Đang lưu " + courses.size() + " khóa học vào CSV...");
          try {
            if (csvDataService.saveAllCourses(courses)) {
              coursesSaved = courses.size();
              saved += courses.size();
              LOGGER.info("Đã lưu thành công " + courses.size() + " khóa học vào CSV");
            } else {
              LOGGER.warning("Không thể lưu khóa học vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu khóa học", e);
          }
        }
        if (students != null) {
          LOGGER.info("Đang lưu " + students.size() + " sinh viên vào CSV...");
          try {
            if (csvDataService.saveAllStudents(students)) {
              studentsSaved = students.size();
              saved += students.size();
              LOGGER.info("Đã lưu thành công " + students.size() + " sinh viên vào CSV");
            } else {
              LOGGER.warning("Không thể lưu sinh viên vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu sinh viên", e);
          }
        }
        if (enrollments != null) {
          LOGGER.info("Đang lưu " + enrollments.size() + " đăng ký học phần vào CSV...");
          try {
            if (csvDataService.saveAllEnrollments(enrollments)) {
              enrollmentsSaved = enrollments.size();
              saved += enrollments.size();
              LOGGER.info("Đã lưu thành công " + enrollments.size() + " đăng ký học phần vào CSV");
            } else {
              LOGGER.warning("Không thể lưu đăng ký học phần vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký học phần", e);
          }
        }
        if (grades != null) {
          LOGGER.info("Đang lưu " + grades.size() + " điểm vào CSV...");
          try {
            if (csvDataService.saveAllGrades(grades)) {
              gradesSaved = grades.size();
              saved += grades.size();
              LOGGER.info("Đã lưu thành công " + grades.size() + " điểm vào CSV");
            } else {
              LOGGER.warning("Không thể lưu điểm vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu điểm", e);
          }
        }
        if (notifications != null) {
          LOGGER.info("Đang lưu " + notifications.size() + " thông báo vào CSV...");
          try {
            if (csvDataService.saveAllNotifications(notifications)) {
              notificationsSaved = notifications.size();
              saved += notifications.size();
              LOGGER.info("Đã lưu thành công " + notifications.size() + " thông báo vào CSV");
            } else {
              LOGGER.warning("Không thể lưu thông báo vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu thông báo", e);
          }
        }
        if (classOpeningRequests != null) {
          LOGGER.info("Đang lưu " + classOpeningRequests.size() + " yêu cầu mở lớp vào CSV...");
          try {
            if (csvDataService.saveAllClassOpeningRequests(classOpeningRequests)) {
              classOpeningRequestsSaved = classOpeningRequests.size();
              saved += classOpeningRequests.size();
              LOGGER.info("Đã lưu thành công " + classOpeningRequests.size() + " yêu cầu mở lớp vào CSV");
            } else {
              LOGGER.warning("Không thể lưu yêu cầu mở lớp vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu yêu cầu mở lớp", e);
          }
        }
        if (courseRegistrations != null) {
          LOGGER.info("Đang lưu " + courseRegistrations.size() + " đăng ký khóa học vào CSV...");
          try {
            if (csvDataService.saveAllCourseRegistrations(courseRegistrations)) {
              courseRegistrationsSaved = courseRegistrations.size();
              saved += courseRegistrations.size();
              LOGGER.info("Đã lưu thành công " + courseRegistrations.size() + " đăng ký khóa học vào CSV");
            } else {
              LOGGER.warning("Không thể lưu đăng ký khóa học vào CSV");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký khóa học", e);
          }
        }
      } finally {
        // 5) Khôi phục và set version = serverVersion sau cùng
        csvDataService.setSkipVersionIncrement(false);
        if (serverVersion != null) {
          csvDataService.setVersion(serverVersion);
        } else {
          // Fallback: nếu không lấy được serverVersion, vẫn tăng version 1 lần
          csvDataService.incrementVersion();
        }
      }

      String detailMessage = String.format(
          "Downloaded: %d users, %d faculties, %d subjects, %d classes, %d courses, %d students, %d enrollments, %d grades, %d notifications, %d requests, %d registrations (Total: %d records)",
          usersSaved, facultiesSaved, subjectsSaved, classesSaved, coursesSaved, studentsSaved,
          enrollmentsSaved, gradesSaved, notificationsSaved, classOpeningRequestsSaved,
          courseRegistrationsSaved, saved);

      LOGGER.info("Đã tải và lưu " + saved + " bản ghi CSV - " + detailMessage);
      return Message.createSuccessResponse(Constants.ACTION_DOWNLOAD_DATA,
          "Downloaded and saved " + saved + " records. " + detailMessage);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải dữ liệu: " + e.getMessage());
      // Đảm bảo reset flag ngay cả khi có lỗi
      csvDataService.setSkipVersionIncrement(false);
      return Message.createErrorResponse(Constants.ACTION_DOWNLOAD_DATA, "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả dữ liệu CSV lên server
   */
  public Message uploadAllCSVData() {
    try {
      LOGGER.info("Bắt đầu tải lên toàn bộ dữ liệu CSV lên server");

      // Upload theo thứ tự để đảm bảo foreign key constraints
      // Faculties trước vì users có FK đến faculties (faculty_code)
      Message facultiesResponse = uploadAllFacultiesFromCSV();
      if (!facultiesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên khoa: " + facultiesResponse.getMessage());
      }

      Message usersResponse = uploadAllUsersFromCSV();
      if (!usersResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên người dùng: " + usersResponse.getMessage());
      }

      Message subjectsResponse = uploadAllSubjectsFromCSV();
      if (!subjectsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên môn học: " + subjectsResponse.getMessage());
      }

      Message classesResponse = uploadAllClassesFromCSV();
      if (!classesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên lớp: " + classesResponse.getMessage());
      }

      Message studentsResponse = uploadAllStudentsFromCSV();
      if (!studentsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên sinh viên: " + studentsResponse.getMessage());
      }

      Message coursesResponse = uploadAllCoursesFromCSV();
      if (!coursesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên khóa học: " + coursesResponse.getMessage());
      }

      Message enrollmentsResponse = uploadAllEnrollmentsFromCSV();
      if (!enrollmentsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên đăng ký học phần: " + enrollmentsResponse.getMessage());
      }

      Message gradesResponse = uploadAllGradesFromCSV();
      if (!gradesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên điểm: " + gradesResponse.getMessage());
      }

      Message notificationsResponse = uploadAllNotificationsFromCSV();
      if (!notificationsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên thông báo: " + notificationsResponse.getMessage());
      }

      Message classOpeningRequestsResponse = uploadAllClassOpeningRequestsFromCSV();
      if (!classOpeningRequestsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên yêu cầu mở lớp: " + classOpeningRequestsResponse.getMessage());
      }

      Message courseRegistrationsResponse = uploadAllCourseRegistrationsFromCSV();
      if (!courseRegistrationsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên đăng ký khóa học: " + courseRegistrationsResponse.getMessage());
      }

      // Sau khi upload thành công, cập nhật version client = version server
      // Đợi một chút để server cập nhật version xong
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      Message metadataResponse = sendMetadata();
      if (metadataResponse.isSuccess()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> serverMetadata = (Map<String, Object>) metadataResponse.getData("server_metadata");
        if (serverMetadata != null) {
          Object csvVersionObj = serverMetadata.get("csv_version");
          if (csvVersionObj instanceof Integer) {
            csvDataService.setVersion((Integer) csvVersionObj);
          } else if (csvVersionObj instanceof Long) {
            csvDataService.setVersion(((Long) csvVersionObj).intValue());
          }
        }
      }

      LOGGER.info("Hoàn tất tải lên dữ liệu CSV");
      return Message.createSuccessResponse("UPLOAD_ALL_CSV", "CSV data upload completed");
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên dữ liệu CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ALL_CSV", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả users từ CSV lên server
   */
  public Message uploadAllUsersFromCSV() {
    try {
      List<User> users = csvDataService.getAllUsers();
      if (users.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_USERS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + users.size() + " người dùng từ CSV lên server");
      Message request = Message.createRequest(Constants.ACTION_UPLOAD_USERS);
      request.addData("users", users);
      request.addData("total", users.size());
      Message response = sendCSVRequestAndWait(request, 180);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + users.size() + " người dùng từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên người dùng từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên người dùng từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_USERS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả students từ CSV lên server
   */
  public Message uploadAllStudentsFromCSV() {
    try {
      List<Student> students = csvDataService.getAllStudents();
      if (students.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_STUDENTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + students.size() + " sinh viên từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_STUDENTS");
      request.addData("students", students);
      request.addData("total", students.size());
      Message response = sendCSVRequestAndWait(request, 180);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + students.size() + " sinh viên từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên sinh viên từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên sinh viên từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_STUDENTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả courses từ CSV lên server
   */
  public Message uploadAllCoursesFromCSV() {
    try {
      List<Course> courses = csvDataService.getAllCourses();
      if (courses.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + courses.size() + " khóa học từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_COURSES");
      request.addData("courses", courses);
      request.addData("total", courses.size());
      Message response = sendCSVRequestAndWait(request, 180);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + courses.size() + " khóa học từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên khóa học từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên khóa học từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSES", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả enrollments từ CSV lên server
   */
  public Message uploadAllEnrollmentsFromCSV() {
    try {
      List<Enrollment> enrollments = csvDataService.getAllEnrollments();
      if (enrollments.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_ENROLLMENTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + enrollments.size() + " đăng ký học phần từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_ENROLLMENTS");
      request.addData("enrollments", enrollments);
      request.addData("total", enrollments.size());
      Message response = sendCSVRequestAndWait(request, 180);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + enrollments.size() + " đăng ký học phần từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên đăng ký học phần từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên đăng ký học phần từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ENROLLMENTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả faculties từ CSV lên server
   */
  public Message uploadAllFacultiesFromCSV() {
    try {
      List<Faculty> faculties = csvDataService.getAllFaculties();
      if (faculties.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_FACULTIES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + faculties.size() + " khoa từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_FACULTIES");
      request.addData("faculties", faculties);
      request.addData("total", faculties.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + faculties.size() + " khoa từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên khoa từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên khoa từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_FACULTIES", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả classes từ CSV lên server
   */
  public Message uploadAllClassesFromCSV() {
    try {
      List<com.university.sms.model.Class> classes = csvDataService.getAllClasses();
      if (classes.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_CLASSES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + classes.size() + " lớp từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_CLASSES");
      request.addData("classes", classes);
      request.addData("total", classes.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + classes.size() + " lớp từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên lớp từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên lớp từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_CLASSES", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả subjects từ CSV lên server
   */
  public Message uploadAllSubjectsFromCSV() {
    try {
      List<Subject> subjects = csvDataService.getAllSubjects();
      if (subjects.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_SUBJECTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + subjects.size() + " môn học từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_SUBJECTS");
      request.addData("subjects", subjects);
      request.addData("total", subjects.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + subjects.size() + " môn học từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên môn học từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên môn học từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_SUBJECTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả grades từ CSV lên server
   */
  public Message uploadAllGradesFromCSV() {
    try {
      List<Grade> grades = csvDataService.getAllGrades();
      if (grades.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_GRADES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + grades.size() + " điểm từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_GRADES");
      request.addData("grades", grades);
      request.addData("total", grades.size());
      Message response = sendCSVRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + grades.size() + " điểm từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên điểm từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên điểm từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_GRADES", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả notifications từ CSV lên server
   */
  public Message uploadAllNotificationsFromCSV() {
    try {
      List<Notification> notifications = csvDataService.getAllNotifications();
      if (notifications.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_NOTIFICATIONS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + notifications.size() + " thông báo từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_NOTIFICATIONS");
      request.addData("notifications", notifications);
      request.addData("total", notifications.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + notifications.size() + " thông báo từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên thông báo từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên thông báo từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_NOTIFICATIONS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả class opening requests từ CSV lên server
   */
  public Message uploadAllClassOpeningRequestsFromCSV() {
    try {
      List<ClassOpeningRequest> requests = csvDataService.getAllClassOpeningRequests();
      if (requests.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_CLASS_OPENING_REQUESTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + requests.size() + " yêu cầu mở lớp từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_CLASS_OPENING_REQUESTS");
      request.addData("requests", requests);
      request.addData("total", requests.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + requests.size() + " yêu cầu mở lớp từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên yêu cầu mở lớp từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên yêu cầu mở lớp từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_CLASS_OPENING_REQUESTS", "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả course registrations từ CSV lên server
   */
  public Message uploadAllCourseRegistrationsFromCSV() {
    try {
      List<CourseRegistration> registrations = csvDataService.getAllCourseRegistrations();
      if (registrations.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSE_REGISTRATIONS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Bắt đầu tải lên " + registrations.size() + " đăng ký khóa học từ CSV lên server");
      Message request = Message.createRequest("UPLOAD_COURSE_REGISTRATIONS");
      request.addData("registrations", registrations);
      request.addData("total", registrations.size());
      Message response = sendCSVRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + registrations.size() + " đăng ký khóa học từ CSV lên server");
      } else {
        LOGGER.warning("Không thể tải lên đăng ký khóa học từ CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên đăng ký khóa học từ CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSE_REGISTRATIONS", "Error: " + e.getMessage());
    }
  }

  /**
   * Sync tự động khi kết nối
   * Chỉ tự động download (không cần quyền), không tự động upload (cần quyền
   * admin)
   */
  @Override
  protected void onConnect() {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(500); // Đợi một chút để connection ổn định
        Message metadataResponse = sendMetadata();
        if (metadataResponse.isSuccess()) {
          String syncAction = (String) metadataResponse.getData("sync_action");
          LOGGER.info("Hành động đồng bộ tự động: " + syncAction);
          if ("DOWNLOAD_FROM_SERVER".equals(syncAction)) {
            // Tự động download nếu server có version mới hơn (không cần quyền)
            syncData("DOWNLOAD_FROM_SERVER");
          } else if ("UPLOAD_TO_SERVER".equals(syncAction)) {
            // Không tự động upload - cần đăng nhập admin trước
            // Upload sẽ được thực hiện thủ công từ GUI sau khi admin đăng nhập
            LOGGER.info("Cần đăng nhập với quyền admin để upload dữ liệu CSV");
          }
        }
      } catch (Exception e) {
        LOGGER.warning("Lỗi khi đồng bộ tự động: " + e.getMessage());
      }
    });
  }

  /**
   * Manual sync (gọi từ GUI)
   */
  public Message manualSync() {
    Message metadataResponse = sendMetadata();
    if (metadataResponse.isSuccess()) {
      String syncAction = (String) metadataResponse.getData("sync_action");
      return syncData(syncAction);
    }
    return metadataResponse;
  }
}
