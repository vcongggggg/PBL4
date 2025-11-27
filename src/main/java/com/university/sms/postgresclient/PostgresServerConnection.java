package com.university.sms.postgresclient;

import com.university.sms.client.BaseServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Faculty;
import com.university.sms.model.Class;
import com.university.sms.model.Subject;
import com.university.sms.model.User;
import com.university.sms.model.Grade;
import com.university.sms.model.Notification;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.CourseRegistration;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PostgreSQL Server Connection - kết nối đến server qua socket để sync
 * Tương tự CSV client nhưng đọc/ghi từ PostgreSQL database thay vì CSV files
 */
public class PostgresServerConnection extends BaseServerConnection {
  private static final Logger LOGGER = Logger.getLogger(PostgresServerConnection.class.getName());

  private PostgresDataService postgresDataService;
  private SyncProgressListener syncProgressListener;

  public interface SyncProgressListener {
    void onSyncStart(String action);

    void onSyncStep(String action, String message);

    void onSyncCompleted(String action, Message result);
  }

  public PostgresServerConnection(String serverHost, int serverPort) {
    super(serverHost, serverPort);
    this.postgresDataService = new PostgresDataService();
  }

  /**
   * Gửi yêu cầu và chờ phản hồi (synchronous)
   */
  private Message sendPostgresRequestAndWait(Message request, long timeoutSeconds) {
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
      Message response = sendPostgresRequestAndWait(request, 60);
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
      return sendPostgresRequestAndWait(request, 60);
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
      return sendPostgresRequestAndWait(request, 120);
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
      return sendPostgresRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tìm kiếm sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, "Error: " + e.getMessage());
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
      return sendPostgresRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xóa sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_DELETE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Thêm sinh viên
   */
  @Override
  public Message addStudent(com.university.sms.model.Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      return sendPostgresRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi thêm sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Cập nhật sinh viên
   */
  @Override
  public Message updateStudent(com.university.sms.model.Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      return sendPostgresRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi cập nhật sinh viên: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy tất cả khóa học
   */
  @Override
  public Message getAllCourses() {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
      return sendPostgresRequestAndWait(request, 120);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách khóa học: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_COURSES, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy thông tin khóa học
   */
  @Override
  public Message getCourseInfo(int courseId) {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
      request.addData(Constants.KEY_COURSE_ID, courseId);
      return sendPostgresRequestAndWait(request, 60);
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
    try {
      Message request = Message.createRequest(Constants.ACTION_CHANGE_PASSWORD);
      request.addData(Constants.KEY_PASSWORD, newPassword);
      return sendPostgresRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đổi mật khẩu: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_CHANGE_PASSWORD, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy thống kê server (admin only)
   */
  @Override
  public Message getServerStatistics() {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_SERVER_STATISTICS);
      return sendPostgresRequestAndWait(request, 30);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy thống kê server: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_SERVER_STATISTICS, "Error: " + e.getMessage());
    }
  }

  /**
   * Generic sendRequest implementation
   */
  @Override
  public Message sendRequest(Message request) {
    return sendPostgresRequestAndWait(request, 60);
  }

  /**
   * Lấy PostgreSQL data service
   */
  public PostgresDataService getPostgresDataService() {
    return postgresDataService;
  }

  public void setSyncProgressListener(SyncProgressListener listener) {
    this.syncProgressListener = listener;
  }

  private void notifySyncStart(String action) {
    if (syncProgressListener != null && action != null && !"NO_SYNC_NEEDED".equals(action)) {
      syncProgressListener.onSyncStart(action);
    }
  }

  private void notifySyncStep(String action, String message) {
    if (syncProgressListener != null && action != null && !"NO_SYNC_NEEDED".equals(action)) {
      syncProgressListener.onSyncStep(action, message);
    }
  }

  private void notifySyncCompleted(String action, Message result) {
    if (syncProgressListener != null && action != null && !"NO_SYNC_NEEDED".equals(action)) {
      syncProgressListener.onSyncCompleted(action, result);
    }
  }

  /**
   * Gửi metadata lên server khi kết nối
   */
  public Message sendMetadata() {
    try {
      Map<String, Object> metadata = postgresDataService.getPostgresMetadata();
      Message request = Message.createRequest(Constants.ACTION_SYNC_CHECK);
      request.addData("metadata", metadata);
      // Tăng timeout lên 120 giây vì getServerMetadata() thực hiện nhiều query
      Message response = sendPostgresRequestAndWait(request, 120);
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
    Message result = null;
    try {
      notifySyncStart(syncAction);
      switch (syncAction) {
        case "UPLOAD_TO_SERVER":
          result = uploadAllPostgresData();
          break;
        case "DOWNLOAD_FROM_SERVER":
          result = downloadFromServer();
          break;
        case "NO_SYNC_NEEDED":
          result = Message.createSuccessResponse(Constants.ACTION_SYNC_DATA, "Dữ liệu đã đồng bộ");
          break;
        default:
          result = Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Unknown sync action");
          break;
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đồng bộ dữ liệu: " + e.getMessage());
      result = Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Error: " + e.getMessage());
    } finally {
      notifySyncCompleted(syncAction, result);
    }
    return result;
  }

  /**
   * Download dữ liệu từ server về PostgreSQL local database
   */
  private Message downloadFromServer() {
    try {
      LOGGER.info("Đang tải dữ liệu từ server về PostgreSQL database...");
      notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang gửi yêu cầu tải dữ liệu từ server...");

      // 1) Gửi request tải dữ liệu
      Message request = Message.createRequest(Constants.ACTION_DOWNLOAD_DATA);
      Message response = sendPostgresRequestAndWait(request, 180);
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
      List<Class> classes = (List<Class>) response.getData("classes");
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

      // Log để debug
      LOGGER.info("Nhận được từ server: " +
          (users != null ? users.size() : 0) + " users, " +
          (faculties != null ? faculties.size() : 0) + " faculties, " +
          (subjects != null ? subjects.size() : 0) + " subjects, " +
          (classes != null ? classes.size() : 0) + " classes, " +
          (students != null ? students.size() : 0) + " students, " +
          (courses != null ? courses.size() : 0) + " courses, " +
          (enrollments != null ? enrollments.size() : 0) + " enrollments, " +
          (grades != null ? grades.size() : 0) + " grades, " +
          (notifications != null ? notifications.size() : 0) + " notifications, " +
          (classOpeningRequests != null ? classOpeningRequests.size() : 0) + " requests, " +
          (courseRegistrations != null ? courseRegistrations.size() : 0) + " registrations");

      // 4) Tắt version increment để không tăng version khi download
      postgresDataService.setSkipVersionIncrement(true);
      int saved = 0;
      int usersSaved = 0, facultiesSaved = 0, subjectsSaved = 0, classesSaved = 0;
      int coursesSaved = 0, studentsSaved = 0, enrollmentsSaved = 0, gradesSaved = 0;
      int notificationsSaved = 0, classOpeningRequestsSaved = 0, courseRegistrationsSaved = 0;

      try {
        // 5) Xóa toàn bộ dữ liệu cũ một lần (TRUNCATE CASCADE tự động xử lý foreign
        // key)
        postgresDataService.truncateAllTables();

        // 6) Ghi dữ liệu mới vào PostgreSQL local database
        // Thứ tự: faculties -> users -> subjects -> classes -> courses -> students ->
        // enrollments -> ...
        if (faculties != null && !faculties.isEmpty()) {
          LOGGER.info("Đang lưu " + faculties.size() + " khoa vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + faculties.size() + " khoa...");
          try {
            if (postgresDataService.saveFaculties(faculties)) {
              facultiesSaved = faculties.size();
              saved += faculties.size();
              LOGGER.info("Đã lưu thành công " + faculties.size() + " khoa vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu khoa vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu khoa", e);
          }
        }
        if (users != null && !users.isEmpty()) {
          LOGGER.info("Đang lưu " + users.size() + " người dùng vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + users.size() + " người dùng...");
          try {
            if (postgresDataService.saveUsers(users)) {
              usersSaved = users.size();
              saved += users.size();
              LOGGER.info("Đã lưu thành công " + users.size() + " người dùng vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu người dùng vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu người dùng", e);
          }
        }
        if (subjects != null && !subjects.isEmpty()) {
          LOGGER.info("Đang lưu " + subjects.size() + " môn học vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + subjects.size() + " môn học...");
          try {
            if (postgresDataService.saveSubjects(subjects)) {
              subjectsSaved = subjects.size();
              saved += subjects.size();
              LOGGER.info("Đã lưu thành công " + subjects.size() + " môn học vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu môn học vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu môn học", e);
          }
        }
        if (classes != null && !classes.isEmpty()) {
          LOGGER.info("Đang lưu " + classes.size() + " lớp vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + classes.size() + " lớp...");
          try {
            if (postgresDataService.saveClasses(classes)) {
              classesSaved = classes.size();
              saved += classes.size();
              LOGGER.info("Đã lưu thành công " + classes.size() + " lớp vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu lớp vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu lớp", e);
          }
        }
        if (students != null && !students.isEmpty()) {
          LOGGER.info("Đang lưu " + students.size() + " sinh viên vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + students.size() + " sinh viên...");
          try {
            if (postgresDataService.saveStudents(students)) {
              studentsSaved = students.size();
              saved += students.size();
              LOGGER.info("Đã lưu thành công " + students.size() + " sinh viên vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu sinh viên vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu sinh viên", e);
          }
        }
        if (courses != null && !courses.isEmpty()) {
          LOGGER.info("Đang lưu " + courses.size() + " khóa học vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + courses.size() + " khóa học...");
          try {
            if (postgresDataService.saveCourses(courses)) {
              coursesSaved = courses.size();
              saved += courses.size();
              LOGGER.info("Đã lưu thành công " + courses.size() + " khóa học vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu khóa học vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu khóa học", e);
          }
        }
        if (enrollments != null && !enrollments.isEmpty()) {
          LOGGER.info("Đang lưu " + enrollments.size() + " đăng ký học phần vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + enrollments.size() + " đăng ký học phần...");
          try {
            if (postgresDataService.saveEnrollments(enrollments)) {
              enrollmentsSaved = enrollments.size();
              saved += enrollments.size();
              LOGGER.info("Đã lưu thành công " + enrollments.size() + " đăng ký học phần vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu đăng ký học phần vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký học phần", e);
          }
        }
        if (grades != null && !grades.isEmpty()) {
          LOGGER.info("Đang lưu " + grades.size() + " điểm vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + grades.size() + " điểm...");
          try {
            if (postgresDataService.saveGrades(grades)) {
              gradesSaved = grades.size();
              saved += grades.size();
              LOGGER.info("Đã lưu thành công " + grades.size() + " điểm vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu điểm vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu điểm", e);
          }
        }
        if (notifications != null && !notifications.isEmpty()) {
          LOGGER.info("Đang lưu " + notifications.size() + " thông báo vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER", "Đang lưu " + notifications.size() + " thông báo...");
          try {
            if (postgresDataService.saveNotifications(notifications)) {
              notificationsSaved = notifications.size();
              saved += notifications.size();
              LOGGER.info("Đã lưu thành công " + notifications.size() + " thông báo vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu thông báo vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu thông báo", e);
          }
        }
        if (classOpeningRequests != null && !classOpeningRequests.isEmpty()) {
          LOGGER.info("Đang lưu " + classOpeningRequests.size() + " yêu cầu mở lớp vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER",
              "Đang lưu " + classOpeningRequests.size() + " yêu cầu mở lớp...");
          try {
            if (postgresDataService.saveClassOpeningRequests(classOpeningRequests)) {
              classOpeningRequestsSaved = classOpeningRequests.size();
              saved += classOpeningRequests.size();
              LOGGER.info("Đã lưu thành công " + classOpeningRequests.size() + " yêu cầu mở lớp vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu yêu cầu mở lớp vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu yêu cầu mở lớp", e);
          }
        }
        if (courseRegistrations != null && !courseRegistrations.isEmpty()) {
          LOGGER.info("Đang lưu " + courseRegistrations.size() + " đăng ký khóa học vào PostgreSQL...");
          notifySyncStep("DOWNLOAD_FROM_SERVER",
              "Đang lưu " + courseRegistrations.size() + " đăng ký khóa học...");
          try {
            if (postgresDataService.saveCourseRegistrations(courseRegistrations)) {
              courseRegistrationsSaved = courseRegistrations.size();
              saved += courseRegistrations.size();
              LOGGER.info("Đã lưu thành công " + courseRegistrations.size() + " đăng ký khóa học vào PostgreSQL");
            } else {
              LOGGER.warning("Không thể lưu đăng ký khóa học vào PostgreSQL");
            }
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký khóa học", e);
          }
        }
      } finally {
        // 6) Khôi phục và set version = serverVersion sau cùng
        postgresDataService.setSkipVersionIncrement(false);
        if (serverVersion != null) {
          postgresDataService.setVersion(serverVersion);
        } else {
          // Fallback: nếu không lấy được serverVersion, vẫn tăng version 1 lần
          postgresDataService.incrementVersion();
        }
      }

      String detailMessage = String.format(
          "Downloaded: %d users, %d faculties, %d subjects, %d classes, %d courses, %d students, %d enrollments, %d grades, %d notifications, %d requests, %d registrations (Total: %d records)",
          usersSaved, facultiesSaved, subjectsSaved, classesSaved, coursesSaved, studentsSaved,
          enrollmentsSaved, gradesSaved, notificationsSaved, classOpeningRequestsSaved,
          courseRegistrationsSaved, saved);

      LOGGER.info("Đã tải và lưu " + saved + " bản ghi PostgreSQL - " + detailMessage);
      return Message.createSuccessResponse(Constants.ACTION_DOWNLOAD_DATA,
          "Downloaded and saved " + saved + " records. " + detailMessage);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải dữ liệu: " + e.getMessage());
      // Đảm bảo reset flag ngay cả khi có lỗi
      postgresDataService.setSkipVersionIncrement(false);
      return Message.createErrorResponse(Constants.ACTION_DOWNLOAD_DATA, "Error: " + e.getMessage());
    }
  }

  /**
   * Upload tất cả dữ liệu PostgreSQL local database lên server
   */
  public Message uploadAllPostgresData() {
    try {
      LOGGER.info("Bắt đầu tải lên toàn bộ dữ liệu PostgreSQL lên server");
      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu khoa...");

      // Upload theo thứ tự để đảm bảo foreign key constraints
      // Faculties trước vì users có FK đến faculties (faculty_code)
      Message facultiesResponse = uploadAllFacultiesFromPostgres();
      if (!facultiesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên khoa: " + facultiesResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu người dùng...");
      Message usersResponse = uploadAllUsersFromPostgres();
      if (!usersResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên người dùng: " + usersResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu môn học...");
      Message subjectsResponse = uploadAllSubjectsFromPostgres();
      if (!subjectsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên môn học: " + subjectsResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu lớp...");
      Message classesResponse = uploadAllClassesFromPostgres();
      if (!classesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên lớp: " + classesResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu sinh viên...");
      Message studentsResponse = uploadAllStudentsFromPostgres();
      if (!studentsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên sinh viên: " + studentsResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload dữ liệu khóa học...");
      Message coursesResponse = uploadAllCoursesFromPostgres();
      if (!coursesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên khóa học: " + coursesResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload đăng ký học phần...");
      Message enrollmentsResponse = uploadAllEnrollmentsFromPostgres();
      if (!enrollmentsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên đăng ký học phần: " + enrollmentsResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload điểm...");
      Message gradesResponse = uploadAllGradesFromPostgres();
      if (!gradesResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên điểm: " + gradesResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload thông báo...");
      Message notificationsResponse = uploadAllNotificationsFromPostgres();
      if (!notificationsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên thông báo: " + notificationsResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload yêu cầu mở lớp...");
      Message classOpeningRequestsResponse = uploadAllClassOpeningRequestsFromPostgres();
      if (!classOpeningRequestsResponse.isSuccess()) {
        LOGGER.warning("Không thể tải lên yêu cầu mở lớp: " + classOpeningRequestsResponse.getMessage());
      }

      notifySyncStep("UPLOAD_TO_SERVER", "Đang upload đăng ký khóa học...");
      Message courseRegistrationsResponse = uploadAllCourseRegistrationsFromPostgres();
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
          Object postgresVersionObj = serverMetadata.get("postgres_version");
          if (postgresVersionObj instanceof Integer) {
            postgresDataService.setVersion((Integer) postgresVersionObj);
          } else if (postgresVersionObj instanceof Long) {
            postgresDataService.setVersion(((Long) postgresVersionObj).intValue());
          }
        }
      }

      LOGGER.info("Hoàn tất tải lên dữ liệu PostgreSQL");
      return Message.createSuccessResponse("UPLOAD_ALL_POSTGRES", "PostgreSQL data upload completed");
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên dữ liệu PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ALL_POSTGRES", "Error: " + e.getMessage());
    }
  }

  // Upload methods - tương tự CSV client
  public Message uploadAllFacultiesFromPostgres() {
    try {
      List<Faculty> faculties = postgresDataService.getAllFaculties();
      if (faculties.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_FACULTIES", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + faculties.size() + " khoa từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_FACULTIES");
      request.addData("faculties", faculties);
      request.addData("total", faculties.size());
      Message response = sendPostgresRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + faculties.size() + " khoa từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên khoa từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên khoa từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_FACULTIES", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllUsersFromPostgres() {
    try {
      List<User> users = postgresDataService.getAllUsers();
      if (users.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_USERS", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + users.size() + " người dùng từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_USERS");
      request.addData("users", users);
      request.addData("total", users.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + users.size() + " người dùng từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên người dùng từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên người dùng từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_USERS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllSubjectsFromPostgres() {
    try {
      List<Subject> subjects = postgresDataService.getAllSubjects();
      if (subjects.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_SUBJECTS", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + subjects.size() + " môn học từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_SUBJECTS");
      request.addData("subjects", subjects);
      request.addData("total", subjects.size());
      Message response = sendPostgresRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + subjects.size() + " môn học từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên môn học từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên môn học từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_SUBJECTS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllClassesFromPostgres() {
    try {
      List<Class> classes = postgresDataService.getAllClasses();
      if (classes.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_CLASSES", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + classes.size() + " lớp từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_CLASSES");
      request.addData("classes", classes);
      request.addData("total", classes.size());
      Message response = sendPostgresRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + classes.size() + " lớp từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên lớp từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên lớp từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_CLASSES", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllStudentsFromPostgres() {
    try {
      List<Student> students = postgresDataService.getAllStudents();
      if (students.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_STUDENTS", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + students.size() + " sinh viên từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_STUDENTS");
      request.addData("students", students);
      request.addData("total", students.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + students.size() + " sinh viên từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên sinh viên từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên sinh viên từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_STUDENTS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllCoursesFromPostgres() {
    try {
      List<Course> courses = postgresDataService.getAllCourses();
      if (courses.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSES", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + courses.size() + " khóa học từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_COURSES");
      request.addData("courses", courses);
      request.addData("total", courses.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + courses.size() + " khóa học từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên khóa học từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên khóa học từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSES", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllEnrollmentsFromPostgres() {
    try {
      List<Enrollment> enrollments = postgresDataService.getAllEnrollments();
      if (enrollments.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_ENROLLMENTS", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + enrollments.size() + " đăng ký học phần từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_ENROLLMENTS");
      request.addData("enrollments", enrollments);
      request.addData("total", enrollments.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + enrollments.size() + " đăng ký học phần từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên đăng ký học phần từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên đăng ký học phần từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_ENROLLMENTS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllGradesFromPostgres() {
    try {
      List<Grade> grades = postgresDataService.getAllGrades();
      if (grades.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_GRADES", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + grades.size() + " điểm từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_GRADES");
      request.addData("grades", grades);
      request.addData("total", grades.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + grades.size() + " điểm từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên điểm từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên điểm từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_GRADES", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllNotificationsFromPostgres() {
    try {
      List<Notification> notifications = postgresDataService.getAllNotifications();
      if (notifications.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_NOTIFICATIONS", "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + notifications.size() + " thông báo từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_NOTIFICATIONS");
      request.addData("notifications", notifications);
      request.addData("total", notifications.size());
      Message response = sendPostgresRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + notifications.size() + " thông báo từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên thông báo từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên thông báo từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_NOTIFICATIONS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllClassOpeningRequestsFromPostgres() {
    try {
      List<ClassOpeningRequest> requests = postgresDataService.getAllClassOpeningRequests();
      if (requests.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_CLASS_OPENING_REQUESTS",
            "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + requests.size() + " yêu cầu mở lớp từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_CLASS_OPENING_REQUESTS");
      request.addData("requests", requests);
      request.addData("total", requests.size());
      Message response = sendPostgresRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + requests.size() + " yêu cầu mở lớp từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên yêu cầu mở lớp từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên yêu cầu mở lớp từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_CLASS_OPENING_REQUESTS", "Error: " + e.getMessage());
    }
  }

  public Message uploadAllCourseRegistrationsFromPostgres() {
    try {
      List<CourseRegistration> registrations = postgresDataService.getAllCourseRegistrations();
      if (registrations.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSE_REGISTRATIONS",
            "PostgreSQL database is empty, nothing to upload");
      }

      LOGGER.info("Bắt đầu tải lên " + registrations.size() + " đăng ký khóa học từ PostgreSQL lên server");
      Message request = Message.createRequest("UPLOAD_COURSE_REGISTRATIONS");
      request.addData("registrations", registrations);
      request.addData("total", registrations.size());
      Message response = sendPostgresRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Đã tải lên thành công " + registrations.size() + " đăng ký khóa học từ PostgreSQL lên server");
      } else {
        LOGGER.warning("Không thể tải lên đăng ký khóa học từ PostgreSQL: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi tải lên đăng ký khóa học từ PostgreSQL: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSE_REGISTRATIONS", "Error: " + e.getMessage());
    }
  }
}
