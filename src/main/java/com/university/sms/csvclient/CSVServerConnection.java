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
      LOGGER.severe("Error during login: " + e.getMessage());
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
      LOGGER.severe("Error getting student info: " + e.getMessage());
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
      LOGGER.severe("Error getting all students: " + e.getMessage());
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
      LOGGER.severe("Error searching students: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, "Error: " + e.getMessage());
    }
  }

  /**
   * Thêm sinh viên
   */
  @Override
  public Message addStudent(Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        // Lưu vào CSV local sau khi server trả về thành công
        csvDataService.saveStudent(student);
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error adding student: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Error: " + e.getMessage());
    }
  }

  /**
   * Cập nhật sinh viên
   */
  @Override
  public Message updateStudent(Student student) {
    try {
      Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
      request.addData(Constants.KEY_STUDENT, student);
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        // Lưu vào CSV local sau khi server trả về thành công
        csvDataService.saveStudent(student);
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error updating student: " + e.getMessage());
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
      if (response.isSuccess()) {
        csvDataService.deleteStudent(studentCode);
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error deleting student: " + e.getMessage());
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
      LOGGER.severe("Error getting all courses: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_COURSES, "Error: " + e.getMessage());
    }
  }

  /**
   * Lấy danh sách khóa học
   */
  @Override
  public Message getCourses() {
    return getAllCourses();
  }

  /**
   * Lấy thông tin khóa học
   */
  @Override
  public Message getCourseInfo(int courseId) {
    try {
      Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
      request.addData(Constants.KEY_COURSE_ID, courseId);
      return sendCSVRequestAndWait(request, 60);
    } catch (Exception e) {
      LOGGER.severe("Error getting course info: " + e.getMessage());
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
      LOGGER.severe("Error getting server statistics: " + e.getMessage());
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
          return uploadAllCSVData();
        case "DOWNLOAD_FROM_SERVER":
          return downloadFromServer();
        case "NO_SYNC_NEEDED":
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
   * Download dữ liệu từ server về CSV
   */
  private Message downloadFromServer() {
    try {
      LOGGER.info("Downloading data from server to CSV...");

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
          for (User u : users) {
            if (csvDataService.saveUser(u)) {
              usersSaved++;
              saved++;
            }
          }
        }
        if (faculties != null) {
          for (Faculty f : faculties) {
            if (csvDataService.saveFaculty(f)) {
              facultiesSaved++;
              saved++;
            }
          }
        }
        if (subjects != null) {
          for (Subject s : subjects) {
            if (csvDataService.saveSubject(s)) {
              subjectsSaved++;
              saved++;
            }
          }
        }
        if (classes != null) {
          for (com.university.sms.model.Class c : classes) {
            if (csvDataService.saveClass(c)) {
              classesSaved++;
              saved++;
            }
          }
        }
        if (courses != null) {
          for (Course c : courses) {
            if (csvDataService.saveCourse(c)) {
              coursesSaved++;
              saved++;
            }
          }
        }
        if (students != null) {
          LOGGER.info("Saving " + students.size() + " students to CSV...");
          try {
            if (csvDataService.saveAllStudents(students)) {
              studentsSaved = students.size();
              saved += students.size();
              LOGGER.info("Successfully saved all " + students.size() + " students to CSV");
            } else {
              LOGGER.warning("Failed to save students to CSV");
            }
          } catch (Exception e) {
            LOGGER.severe("Error saving students: " + e.getMessage());
            e.printStackTrace();
          }
        }
        if (enrollments != null) {
          for (Enrollment e : enrollments) {
            if (csvDataService.saveEnrollment(e)) {
              enrollmentsSaved++;
              saved++;
            }
          }
        }
        if (grades != null) {
          for (Grade g : grades) {
            if (csvDataService.saveGrade(g)) {
              gradesSaved++;
              saved++;
            }
          }
        }
        if (notifications != null) {
          for (Notification n : notifications) {
            if (csvDataService.saveNotification(n)) {
              notificationsSaved++;
              saved++;
            }
          }
        }
        if (classOpeningRequests != null) {
          for (ClassOpeningRequest r : classOpeningRequests) {
            if (csvDataService.saveClassOpeningRequest(r)) {
              classOpeningRequestsSaved++;
              saved++;
            }
          }
        }
        if (courseRegistrations != null) {
          for (CourseRegistration r : courseRegistrations) {
            if (csvDataService.saveCourseRegistration(r)) {
              courseRegistrationsSaved++;
              saved++;
            }
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

      LOGGER.info("Downloaded and saved " + saved + " CSV records - " + detailMessage);
      return Message.createSuccessResponse(Constants.ACTION_DOWNLOAD_DATA,
          "Downloaded and saved " + saved + " records. " + detailMessage);
    } catch (Exception e) {
      LOGGER.severe("Error downloading data: " + e.getMessage());
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
      LOGGER.info("Starting full CSV data upload to server");

      // Upload theo thứ tự để đảm bảo foreign key constraints
      Message usersResponse = uploadAllUsersFromCSV();
      if (!usersResponse.isSuccess()) {
        LOGGER.warning("Failed to upload users: " + usersResponse.getMessage());
      }

      Message facultiesResponse = uploadAllFacultiesFromCSV();
      if (!facultiesResponse.isSuccess()) {
        LOGGER.warning("Failed to upload faculties: " + facultiesResponse.getMessage());
      }

      Message subjectsResponse = uploadAllSubjectsFromCSV();
      if (!subjectsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload subjects: " + subjectsResponse.getMessage());
      }

      Message classesResponse = uploadAllClassesFromCSV();
      if (!classesResponse.isSuccess()) {
        LOGGER.warning("Failed to upload classes: " + classesResponse.getMessage());
      }

      Message studentsResponse = uploadAllStudentsFromCSV();
      if (!studentsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload students: " + studentsResponse.getMessage());
      }

      Message coursesResponse = uploadAllCoursesFromCSV();
      if (!coursesResponse.isSuccess()) {
        LOGGER.warning("Failed to upload courses: " + coursesResponse.getMessage());
      }

      Message enrollmentsResponse = uploadAllEnrollmentsFromCSV();
      if (!enrollmentsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload enrollments: " + enrollmentsResponse.getMessage());
      }

      Message gradesResponse = uploadAllGradesFromCSV();
      if (!gradesResponse.isSuccess()) {
        LOGGER.warning("Failed to upload grades: " + gradesResponse.getMessage());
      }

      Message notificationsResponse = uploadAllNotificationsFromCSV();
      if (!notificationsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload notifications: " + notificationsResponse.getMessage());
      }

      Message classOpeningRequestsResponse = uploadAllClassOpeningRequestsFromCSV();
      if (!classOpeningRequestsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload class opening requests: " + classOpeningRequestsResponse.getMessage());
      }

      Message courseRegistrationsResponse = uploadAllCourseRegistrationsFromCSV();
      if (!courseRegistrationsResponse.isSuccess()) {
        LOGGER.warning("Failed to upload course registrations: " + courseRegistrationsResponse.getMessage());
      }

      // Sau khi upload thành công, cập nhật version client = version server
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

      LOGGER.info("CSV data upload completed");
      return Message.createSuccessResponse("UPLOAD_ALL_CSV", "CSV data upload completed");
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
      List<User> users = csvDataService.getAllUsers();
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

  /**
   * Upload tất cả students từ CSV lên server
   */
  public Message uploadAllStudentsFromCSV() {
    try {
      List<Student> students = csvDataService.getAllStudents();
      if (students.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_STUDENTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Starting upload of " + students.size() + " students from CSV to server");
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
   * Upload tất cả courses từ CSV lên server
   */
  public Message uploadAllCoursesFromCSV() {
    try {
      List<Course> courses = csvDataService.getAllCourses();
      if (courses.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_COURSES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Starting upload of " + courses.size() + " courses from CSV to server");
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
   * Upload tất cả enrollments từ CSV lên server
   */
  public Message uploadAllEnrollmentsFromCSV() {
    try {
      List<Enrollment> enrollments = csvDataService.getAllEnrollments();
      if (enrollments.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_ENROLLMENTS", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Starting upload of " + enrollments.size() + " enrollments from CSV to server");
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
   * Upload tất cả faculties từ CSV lên server
   */
  public Message uploadAllFacultiesFromCSV() {
    try {
      List<Faculty> faculties = csvDataService.getAllFaculties();
      if (faculties.isEmpty()) {
        return Message.createSuccessResponse("UPLOAD_FACULTIES", "CSV file is empty, nothing to upload");
      }
      LOGGER.info("Starting upload of " + faculties.size() + " faculties from CSV to server");
      Message request = Message.createRequest("UPLOAD_FACULTIES");
      request.addData("faculties", faculties);
      request.addData("total", faculties.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + faculties.size() + " faculties from CSV to server");
      } else {
        LOGGER.warning("Failed to upload faculties from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading faculties from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + classes.size() + " classes from CSV to server");
      Message request = Message.createRequest("UPLOAD_CLASSES");
      request.addData("classes", classes);
      request.addData("total", classes.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + classes.size() + " classes from CSV to server");
      } else {
        LOGGER.warning("Failed to upload classes from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading classes from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + subjects.size() + " subjects from CSV to server");
      Message request = Message.createRequest("UPLOAD_SUBJECTS");
      request.addData("subjects", subjects);
      request.addData("total", subjects.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + subjects.size() + " subjects from CSV to server");
      } else {
        LOGGER.warning("Failed to upload subjects from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading subjects from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + grades.size() + " grades from CSV to server");
      Message request = Message.createRequest("UPLOAD_GRADES");
      request.addData("grades", grades);
      request.addData("total", grades.size());
      Message response = sendCSVRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + grades.size() + " grades from CSV to server");
      } else {
        LOGGER.warning("Failed to upload grades from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading grades from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + notifications.size() + " notifications from CSV to server");
      Message request = Message.createRequest("UPLOAD_NOTIFICATIONS");
      request.addData("notifications", notifications);
      request.addData("total", notifications.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + notifications.size() + " notifications from CSV to server");
      } else {
        LOGGER.warning("Failed to upload notifications from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading notifications from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + requests.size() + " class opening requests from CSV to server");
      Message request = Message.createRequest("UPLOAD_CLASS_OPENING_REQUESTS");
      request.addData("requests", requests);
      request.addData("total", requests.size());
      Message response = sendCSVRequestAndWait(request, 60);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + requests.size() + " class opening requests from CSV to server");
      } else {
        LOGGER.warning("Failed to upload class opening requests from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading class opening requests from CSV: " + e.getMessage());
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
      LOGGER.info("Starting upload of " + registrations.size() + " course registrations from CSV to server");
      Message request = Message.createRequest("UPLOAD_COURSE_REGISTRATIONS");
      request.addData("registrations", registrations);
      request.addData("total", registrations.size());
      Message response = sendCSVRequestAndWait(request, 120);
      if (response.isSuccess()) {
        LOGGER.info("Successfully uploaded " + registrations.size() + " course registrations from CSV to server");
      } else {
        LOGGER.warning("Failed to upload course registrations from CSV: " + response.getMessage());
      }
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error uploading course registrations from CSV: " + e.getMessage());
      return Message.createErrorResponse("UPLOAD_COURSE_REGISTRATIONS", "Error: " + e.getMessage());
    }
  }

  /**
   * Sync tự động khi kết nối
   */
  @Override
  protected void onConnect() {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(500); // Đợi một chút để connection ổn định
        Message metadataResponse = sendMetadata();
        if (metadataResponse.isSuccess()) {
          String syncAction = (String) metadataResponse.getData("sync_action");
          LOGGER.info("Auto sync action: " + syncAction);
          if ("DOWNLOAD_FROM_SERVER".equals(syncAction)) {
            // Tự động download nếu server có version mới hơn
            syncData("DOWNLOAD_FROM_SERVER");
          } else if ("UPLOAD_TO_SERVER".equals(syncAction)) {
            // Tự động upload nếu client có version mới hơn hoặc version rỗng
            syncData("UPLOAD_TO_SERVER");
          }
        }
      } catch (Exception e) {
        LOGGER.warning("Error during auto sync: " + e.getMessage());
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
