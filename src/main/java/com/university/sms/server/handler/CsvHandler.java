package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.ClassDAO;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.FacultyDAO;
import com.university.sms.dao.GradeDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.dao.SubjectDAO;
import com.university.sms.dao.UserDAO;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Grade;
import com.university.sms.model.Notification;
import com.university.sms.model.Student;
import com.university.sms.model.Subject;
import com.university.sms.model.User;
import com.university.sms.service.ClassOpeningRequestService;
import com.university.sms.service.CourseService;
import com.university.sms.service.NotificationService;
import com.university.sms.service.StudentService;
import com.university.sms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý đồng bộ cho nguồn CSV (upload/download/sync).
 */
public class CsvHandler {
  private static final Logger LOGGER = Logger.getLogger(CsvHandler.class.getName());
  private static final String SOURCE = "CSV";

  private final StudentService studentService;
  private final CourseService courseService;
  private final ClassOpeningRequestService classRequestService;
  private final NotificationService notificationService;
  private final DataOriginHelper dataOriginHelper;
  private final Consumer<String> clientSourceUpdater;

  private User currentUser;

  public CsvHandler(StudentService studentService,
      CourseService courseService,
      ClassOpeningRequestService classRequestService,
      NotificationService notificationService,
      DataOriginHelper dataOriginHelper,
      Consumer<String> clientSourceUpdater) {
    this.studentService = studentService;
    this.courseService = courseService;
    this.classRequestService = classRequestService;
    this.notificationService = notificationService;
    this.dataOriginHelper = dataOriginHelper;
    this.clientSourceUpdater = clientSourceUpdater;
  }

  public void updateCurrentUser(User user) {
    this.currentUser = user;
  }

  private void setClientSource() {
    if (clientSourceUpdater != null) {
      clientSourceUpdater.accept(SOURCE);
    }
  }

  public Message handleSyncCheck(Message request) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> clientMetadata = (Map<String, Object>) request.getData("metadata", Map.class);

      if (clientMetadata == null) {
        return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Invalid metadata");
      }

      String clientDbType = (String) clientMetadata.get("database_type");
      if (clientDbType != null && !clientDbType.trim().isEmpty()) {
        LOGGER.info("CSV client metadata reported database_type=" + clientDbType);
      }
      setClientSource();

      Object clientVersionObj = clientMetadata.get("db_version");
      int clientVersion = 0;
      if (clientVersionObj != null) {
        if (clientVersionObj instanceof Number) {
          clientVersion = ((Number) clientVersionObj).intValue();
        } else if (clientVersionObj instanceof String) {
          try {
            clientVersion = Integer.parseInt((String) clientVersionObj);
          } catch (NumberFormatException e) {
            clientVersion = 0;
          }
        }
      }

      int clientTotalRecords = 0;
      Object clientTotalRecordsObj = clientMetadata.get("total_records");
      if (clientTotalRecordsObj instanceof Number) {
        clientTotalRecords = ((Number) clientTotalRecordsObj).intValue();
      }

      Map<String, Object> serverMetadata = getServerMetadata();
      int serverVersion = ((Number) serverMetadata.get("db_version")).intValue();

      String currentSource = SOURCE;
      String clientSourceKey = currentSource.toLowerCase() + "_version";
      int clientSourceVersion = 0;
      if (serverMetadata.containsKey(clientSourceKey)) {
        Object versionObj = serverMetadata.get(clientSourceKey);
        if (versionObj instanceof Number) {
          clientSourceVersion = ((Number) versionObj).intValue();
        }
      }

      boolean hasClientVersion = clientVersion > 0;
      boolean hasSourceVersion = clientSourceVersion > 0;
      boolean versionMatches = hasClientVersion
          && ((hasSourceVersion && clientVersion == clientSourceVersion)
              || (!hasSourceVersion && clientVersion == serverVersion));

      String syncAction;
      if (versionMatches) {
        syncAction = "NO_SYNC_NEEDED";
      } else if (currentSource != null && !"REGULAR".equals(currentSource) && !"UNKNOWN".equals(currentSource)) {
        if (!hasClientVersion) {
          syncAction = "UPLOAD_TO_SERVER";
        } else if (clientSourceVersion > clientVersion) {
          syncAction = "DOWNLOAD_FROM_SERVER";
        } else {
          syncAction = "UPLOAD_TO_SERVER";
        }
      } else {
        if (hasClientVersion && clientVersion >= serverVersion) {
          syncAction = "NO_SYNC_NEEDED";
        } else {
          syncAction = "UPLOAD_TO_SERVER";
        }
      }

      Message response = Message.createSuccessResponse(Constants.ACTION_SYNC_CHECK,
          "Sync check completed");
      response.addData("sync_action", syncAction);
      response.addData("server_version", serverVersion);
      response.addData("client_source_version", clientSourceVersion);
      response.addData("server_metadata", serverMetadata);
      response.addData("client_total_records", clientTotalRecords);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý kiểm tra đồng bộ: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleDownloadData(Message request) {
    try {
      setClientSource();
      String source = SOURCE;
      if (source == null || "UNKNOWN".equals(source) || "REGULAR".equals(source)) {
        source = "CSV";
      }

      LOGGER.info("Downloading " + source + " data to client");

      List<Student> students = getStudentsBySource(source);
      List<Course> courses = getCoursesBySource(source);
      List<Enrollment> enrollments = getEnrollmentsBySource(source);
      List<com.university.sms.model.Faculty> faculties = getFacultiesBySource(source);
      List<com.university.sms.model.Class> classes = getClassesBySource(source);
      List<Subject> subjects = getSubjectsBySource(source);
      List<User> users = getUsersBySource(source);
      List<Grade> grades = getGradesBySource(source);
      List<Notification> notifications = getNotificationsBySource(source);
      List<ClassOpeningRequest> classOpeningRequests = getClassOpeningRequestsBySource(source);
      List<CourseRegistration> courseRegistrations = getCourseRegistrationsBySource(source);

      Message response = Message.createSuccessResponse(Constants.ACTION_DOWNLOAD_DATA,
          "Downloaded " + students.size() + " students, " + courses.size() + " courses, " +
              users.size() + " users, " + grades.size() + " grades, " + notifications.size()
              + " notifications, " +
              classOpeningRequests.size() + " requests, " + courseRegistrations.size()
              + " registrations");
      response.addData("students", students);
      response.addData("courses", courses);
      response.addData("enrollments", enrollments);
      response.addData("faculties", faculties);
      response.addData("classes", classes);
      response.addData("subjects", subjects);
      response.addData("users", users);
      response.addData("grades", grades);
      response.addData("notifications", notifications);
      response.addData("classOpeningRequests", classOpeningRequests);
      response.addData("courseRegistrations", courseRegistrations);

      Map<String, Object> serverMetadata = getServerMetadata();
      String versionKey = source.toLowerCase() + "_version";
      response.addData("client_source_version", serverMetadata.get(versionKey));
      response.addData("client_source", source);

      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải dữ liệu: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_DOWNLOAD_DATA, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadUsers(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<User> users = (List<User>) request.getData("users");

      if (users == null || users.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS, "No users to upload");
      }

      LOGGER.info("Uploading " + users.size() + " users from client");

      int successCount = 0;
      int failCount = 0;
      UserDAO userDAO = new UserDAO();
      FacultyDAO facultyDAO = new FacultyDAO();
      String source = SOURCE;

      for (User u : users) {
        try {
          // Kiểm tra email format và uniqueness
          if (u.getEmail() != null && !u.getEmail().trim().isEmpty()) {
            if (!isValidEmailFormat(u.getEmail().trim())) {
              failCount++;
              LOGGER.warning("Không thể upload user: email không hợp lệ - username=" +
                  u.getUsername() + ", email=" + u.getEmail());
              continue;
            }
            User existingUserByEmail = userDAO.findByEmail(u.getEmail().trim());
            if (existingUserByEmail != null && !existingUserByEmail.getUsername().equals(u.getUsername())) {
              failCount++;
              LOGGER.warning("Không thể upload user: email đã được sử dụng - username=" +
                  u.getUsername() + ", email=" + u.getEmail());
              continue;
            }
          }

          // Kiểm tra phone format và uniqueness
          String normalizedPhone = null;
          if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            normalizedPhone = normalizePhoneNumber(u.getPhone().trim());
            if (!isValidPhoneFormat(normalizedPhone)) {
              failCount++;
              LOGGER.warning("Không thể upload user: số điện thoại không hợp lệ - username=" +
                  u.getUsername() + ", phone=" + u.getPhone());
              continue;
            }
            User existingUserByPhone = userDAO.findByPhone(normalizedPhone);
            if (existingUserByPhone != null && !existingUserByPhone.getUsername().equals(u.getUsername())) {
              failCount++;
              LOGGER.warning("Không thể upload user: số điện thoại đã được sử dụng - username=" +
                  u.getUsername() + ", phone=" + normalizedPhone);
              continue;
            }
          }

          boolean facultyOk = true;
          if (u.getFacultyCode() != null && !u.getFacultyCode().trim().isEmpty()) {
            com.university.sms.model.Faculty existingFaculty = facultyDAO
                .findByCode(u.getFacultyCode().trim());
            if (existingFaculty == null) {
              facultyOk = false;
              LOGGER.warning(
                  "Không tìm thấy mã khoa: " + u.getFacultyCode() + " cho user " + u.getUsername());
            }
          }

          User existing = userDAO.findByUsername(u.getUsername());
          if (existing == null) {
            u.setUserId(0);
            if (normalizedPhone != null) {
              u.setPhone(normalizedPhone);
            }
            if (facultyOk && userDAO.addUser(u)) {
              dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
              successCount++;
              LOGGER.info("Đã upload user: username=" + u.getUsername() + ", role=" + u.getRole());
            } else {
              failCount++;
              LOGGER.warning(
                  "Không thể lưu user: " + u.getUsername() +
                      (facultyOk ? " (kiểm tra email/phone uniqueness)" : " (faculty_code not found)"));
            }
          } else {
            LOGGER.info("User đã tồn tại, bỏ qua: " + u.getUsername());
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên người dùng " + u.getUsername(), ex);
        }
      }

      String message = createUploadMessage("users", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_USERS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên người dùng: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadFaculties(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<com.university.sms.model.Faculty> faculties = (List<com.university.sms.model.Faculty>) request
          .getData("faculties");

      if (faculties == null || faculties.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES, "No faculties to upload");
      }

      LOGGER.info("Uploading " + faculties.size() + " faculties from client");

      int successCount = 0;
      int failCount = 0;
      FacultyDAO facultyDAO = new FacultyDAO();
      String source = SOURCE;

      for (com.university.sms.model.Faculty f : faculties) {
        try {
          com.university.sms.model.Faculty existing = facultyDAO.findByCode(f.getFacultyCode());
          if (existing == null) {
            f.setFacultyId(0);
            if (facultyDAO.addFaculty(f)) {
              dataOriginHelper.saveDataOrigin("faculty", f.getFacultyId(), source);
              successCount++;
              LOGGER.info("Đã upload faculty: " + f.getFacultyCode() + " - " + f.getFacultyName());
            } else {
              failCount++;
              LOGGER.warning(
                  "Failed to save faculty: " + f.getFacultyCode() + " - " + f.getFacultyName());
            }
          } else {
            LOGGER.info("Faculty đã tồn tại, bỏ qua: " + f.getFacultyCode() + " - " + f.getFacultyName());
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên khoa " + f.getFacultyCode(), ex);
        }
      }

      String message = createUploadMessage("faculties", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_FACULTIES, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên khoa: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadClasses(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<com.university.sms.model.Class> classes = (List<com.university.sms.model.Class>) request
          .getData("classes");

      if (classes == null || classes.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES, "No classes to upload");
      }

      LOGGER.info("Uploading " + classes.size() + " classes from client");

      int successCount = 0;
      int failCount = 0;
      ClassDAO classDAO = new ClassDAO();
      UserDAO userDAO = new UserDAO();
      FacultyDAO facultyDAO = new FacultyDAO();
      String source = SOURCE;

      for (com.university.sms.model.Class c : classes) {
        try {
          boolean facultyOk = true;
          if (c.getFacultyCode() != null && !c.getFacultyCode().isEmpty()) {
            com.university.sms.model.Faculty existingFaculty = facultyDAO.findByCode(c.getFacultyCode());
            if (existingFaculty == null) {
              facultyOk = false;
              LOGGER.warning("Faculty code not found: " + c.getFacultyCode() + " for class "
                  + c.getClassCode());
            }
          }

          boolean userOk = true;
          if (c.getTeacherUsername() != null && !c.getTeacherUsername().isEmpty()) {
            User existingUser = userDAO.findByUsername(c.getTeacherUsername());
            if (existingUser == null) {
              userOk = false;
              LOGGER.warning("Không thể upload class: teacher không tồn tại - " +
                  "classCode=" + c.getClassCode() + ", teacherUsername=" + c.getTeacherUsername());
            } else {
              // Kiểm tra role của user phải là TEACHER
              if (existingUser.getRole() != User.UserRole.TEACHER) {
                userOk = false;
                LOGGER.warning("Không thể upload class: user không phải là giáo viên - " +
                    "classCode=" + c.getClassCode() + ", teacherUsername=" + c.getTeacherUsername() +
                    ", role=" + existingUser.getRole());
              }
            }
          }

          com.university.sms.model.Class existing = classDAO.findByCode(c.getClassCode());
          if (existing == null) {
            c.setClassId(0);
            if (facultyOk && userOk && classDAO.save(c)) {
              dataOriginHelper.saveDataOrigin("class", c.getClassId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning("Failed to save class: " + c.getClassCode() + " - " + c.getClassName());
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên lớp " + c.getClassCode(), ex);
        }
      }

      String message = createUploadMessage("classes", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_CLASSES, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên lớp: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadStudents(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Student> students = (List<Student>) request
          .getData("students");

      if (students == null || students.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS, "No students to upload");
      }

      LOGGER.info("Uploading " + students.size() + " students from client");

      int successCount = 0;
      int failCount = 0;

      StudentDAO studentDAO = new StudentDAO();
      UserDAO userDAO = new UserDAO();
      ClassDAO classDAO = new ClassDAO();
      FacultyDAO facultyDAO = new FacultyDAO();
      String source = SOURCE;

      for (Student student : students) {
        try {
          String username = student.getUsername();
          boolean userOk = true;
          if (username == null || username.isEmpty()) {
            username = student.getStudentCode();
            student.setUsername(username);
          }

          if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            if (!isValidEmailFormat(student.getEmail().trim())) {
              failCount++;
              LOGGER.warning("Không thể upload student: email không hợp lệ - " +
                  "studentCode=" + student.getStudentCode() + ", email=" + student.getEmail());
              continue;
            }
          }

          String normalizedStudentPhone = null;
          if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
            normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
            if (!isValidPhoneFormat(normalizedStudentPhone)) {
              failCount++;
              LOGGER.warning("Không thể upload student: số điện thoại không hợp lệ - " +
                  "studentCode=" + student.getStudentCode() + ", phone=" + student.getPhone());
              continue;
            }
          }

          if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
            if (existingUserByEmail != null && !existingUserByEmail.getUsername().equals(username)) {
              failCount++;
              LOGGER.warning("Không thể upload student: email đã được sử dụng bởi user khác - " +
                  "studentCode=" + student.getStudentCode() + ", email=" + student.getEmail() +
                  ", existingUser=" + existingUserByEmail.getUsername());
              continue;
            }
          }

          if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
            User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
            if (existingUserByPhone != null && !existingUserByPhone.getUsername().equals(username)) {
              failCount++;
              LOGGER.warning("Không thể upload student: số điện thoại đã được sử dụng bởi user khác - " +
                  "studentCode=" + student.getStudentCode() + ", phone=" + normalizedStudentPhone +
                  ", existingUser=" + existingUserByPhone.getUsername());
              continue;
            }
          }

          User byUsername = userDAO.findByUsername(username);
          if (byUsername == null) {
            userOk = false;
            LOGGER.warning("Không thể upload student: user không tồn tại - " +
                "studentCode=" + student.getStudentCode() + ", username=" + username);
          } else {
            // Kiểm tra role của user phải là STUDENT
            if (byUsername.getRole() != User.UserRole.STUDENT) {
              userOk = false;
              LOGGER.warning("Không thể upload student: user không phải là sinh viên - " +
                  "studentCode=" + student.getStudentCode() + ", username=" + username +
                  ", role=" + byUsername.getRole());
            }
          }

          boolean classOk = true;
          com.university.sms.model.Class classObj = null;
          if (student.getClassCode() != null && !student.getClassCode().trim().isEmpty()) {
            classObj = classDAO.findByCode(student.getClassCode());
            if (classObj == null) {
              classOk = false;
              LOGGER.warning("Không tìm thấy mã lớp: " + student.getClassCode() +
                  " - Sẽ upload sinh viên " + student.getStudentCode() + " không có lớp");
              // Set classCode = null để upload sinh viên không có lớp
              student.setClassCode(null);
            } else {
              // Kiểm tra lớp đã đầy chưa
              if (classObj.getMaxStudents() != null) {
                int currentStudentCount = studentDAO.countByClassCode(student.getClassCode());
                if (currentStudentCount >= classObj.getMaxStudents()) {
                  LOGGER.warning("Lớp đã đầy (" + currentStudentCount + "/" + classObj.getMaxStudents() +
                      "): " + student.getClassCode() + " - Sẽ upload sinh viên " + student.getStudentCode()
                      + " không có lớp");
                  // Set classCode = null để upload sinh viên không có lớp thay vì từ chối
                  student.setClassCode(null);
                }
              }
            }
          }

          boolean facultyOk = true;
          if (student.getFacultyCode() != null && !student.getFacultyCode().trim().isEmpty()) {
            com.university.sms.model.Faculty faculty = facultyDAO.findByCode(student.getFacultyCode());
            if (faculty == null) {
              facultyOk = false;
              LOGGER.warning("Faculty code not found: " + student.getFacultyCode());
            }
          }

          Student existingStudent = studentDAO.findByStudentCode(student.getStudentCode());
          if (existingStudent == null) {
            if (userOk && classOk && facultyOk && studentDAO.addStudent(student)) {
              dataOriginHelper.saveDataOrigin("student", student.getStudentId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning("Failed to save student: " + student.getStudentCode());
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên sinh viên: " + student.getStudentCode(), ex);
        }
      }

      String message = createUploadMessage("students", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_STUDENTS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên sinh viên: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadSubjects(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Subject> subjects = (List<Subject>) request
          .getData("subjects");

      if (subjects == null || subjects.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS, "No subjects to upload");
      }

      LOGGER.info("Uploading " + subjects.size() + " subjects from client");

      int successCount = 0;
      int failCount = 0;
      SubjectDAO subjectDAO = new SubjectDAO();
      FacultyDAO facultyDAO = new FacultyDAO();
      String source = SOURCE;

      for (Subject s : subjects) {
        try {
          boolean facultyOk = true;
          if (s.getFacultyCode() != null && !s.getFacultyCode().isEmpty()) {
            com.university.sms.model.Faculty faculty = facultyDAO.findByCode(s.getFacultyCode());
            if (faculty == null) {
              facultyOk = false;
              LOGGER.warning("Faculty code not found: " + s.getFacultyCode());
            }
          }

          boolean prerequisiteOk = true;
          if (s.getPrerequisiteSubjectCode() != null && !s.getPrerequisiteSubjectCode().isEmpty()) {
            Subject prerequisite = subjectDAO.findByCode(s.getPrerequisiteSubjectCode());
            if (prerequisite == null) {
              prerequisiteOk = false;
              LOGGER.warning(
                  "Không tìm thấy môn học tiên quyết: " + s.getPrerequisiteSubjectCode() +
                      " cho môn học " + s.getSubjectCode());
            } else {
              // Kiểm tra circular dependency: nếu prerequisite có prerequisite là subject
              // hiện tại
              if (prerequisite.getPrerequisiteSubjectCode() != null &&
                  prerequisite.getPrerequisiteSubjectCode().equals(s.getSubjectCode())) {
                prerequisiteOk = false;
                LOGGER.warning("Không thể upload subject: circular dependency - " +
                    s.getSubjectCode() + " requires " + s.getPrerequisiteSubjectCode() +
                    " but " + s.getPrerequisiteSubjectCode() + " requires " + s.getSubjectCode());
              }
            }
          }

          Subject existing = subjectDAO.findByCode(s.getSubjectCode());
          if (existing == null) {
            s.setSubjectId(0);
            if (facultyOk && prerequisiteOk && subjectDAO.save(s)) {
              dataOriginHelper.saveDataOrigin("subject", s.getSubjectId(), source);
              successCount++;
              LOGGER.info("Đã upload subject: " + s.getSubjectCode() + " - " + s.getSubjectName());
            } else {
              failCount++;
              String reason = "";
              if (!facultyOk)
                reason += " (không tìm thấy khoa: " + s.getFacultyCode() + ")";
              if (!prerequisiteOk)
                reason += " (không tìm thấy/circular dependency môn tiên quyết: " +
                    s.getPrerequisiteSubjectCode() + ")";
              LOGGER.warning("Không thể lưu subject: " + s.getSubjectCode() + " - " + s.getSubjectName() + reason);
            }
          } else {
            LOGGER.info("Subject đã tồn tại, bỏ qua: " + s.getSubjectCode() + " - " + s.getSubjectName());
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên môn học " + s.getSubjectCode(), ex);
        }
      }

      String message = createUploadMessage("subjects", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_SUBJECTS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên môn học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadCourses(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Course> courses = (List<Course>) request
          .getData("courses");

      if (courses == null || courses.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES, "No courses to upload");
      }

      LOGGER.info("Đang tải lên " + courses.size() + " khóa học từ client");

      int successCount = 0;
      int failCount = 0;
      CourseDAO courseDAO = new CourseDAO();
      SubjectDAO subjectDAO = new SubjectDAO();
      ClassDAO classDAO = new ClassDAO();
      UserDAO userDAO = new UserDAO();
      String source = SOURCE;

      for (Course course : courses) {
        try {
          boolean subjectOk = true;
          if (course.getSubjectCode() != null && !course.getSubjectCode().isEmpty()) {
            Subject subject = subjectDAO.findByCode(course.getSubjectCode());
            if (subject == null) {
              subjectOk = false;
              LOGGER.warning(
                  "Không tìm thấy mã môn học: " + course.getSubjectCode() + " cho khóa học " + course.getCourseCode());
            }
          }

          boolean classOk = true;
          if (course.getClassCode() != null && !course.getClassCode().isEmpty()) {
            com.university.sms.model.Class cls = classDAO.findByCode(course.getClassCode());
            if (cls == null) {
              classOk = false;
              LOGGER.warning(
                  "Không tìm thấy mã lớp: " + course.getClassCode() + " cho khóa học " + course.getCourseCode());
            }
          }

          boolean userOk = true;
          if (course.getTeacherUsername() != null && !course.getTeacherUsername().isEmpty()) {
            User existingUser = userDAO.findByUsername(course.getTeacherUsername());
            if (existingUser == null) {
              userOk = false;
              LOGGER.warning("Không thể upload course: teacher không tồn tại - " +
                  "courseCode=" + course.getCourseCode() + ", teacherUsername=" + course.getTeacherUsername());
            } else {
              // Kiểm tra role của user phải là TEACHER
              if (existingUser.getRole() != User.UserRole.TEACHER) {
                userOk = false;
                LOGGER.warning("Không thể upload course: user không phải là giáo viên - " +
                    "courseCode=" + course.getCourseCode() + ", teacherUsername=" + course.getTeacherUsername() +
                    ", role=" + existingUser.getRole());
              }
            }
          }

          // Kiểm tra trùng lịch học (schedule_day, schedule_time, room) với các course
          // khác
          boolean scheduleConflict = false;
          if (course.getScheduleDay() != null && !course.getScheduleDay().isEmpty() &&
              course.getScheduleTime() != null && !course.getScheduleTime().isEmpty() &&
              course.getRoom() != null && !course.getRoom().isEmpty()) {
            List<Course> existingCourses = courseDAO.findByScheduleAndRoom(
                course.getScheduleDay(), course.getScheduleTime(), course.getRoom());
            if (!existingCourses.isEmpty()) {
              scheduleConflict = true;
              LOGGER.warning("Trùng lịch học: " + course.getCourseCode() +
                  " - Lịch: " + course.getScheduleDay() + " " + course.getScheduleTime() +
                  ", Phòng: " + course.getRoom());
            }
          }

          // Kiểm tra maxStudents > currentStudents
          boolean capacityOk = true;
          if (course.getMaxStudents() <= 0) {
            capacityOk = false;
            LOGGER.warning("Không thể upload course: maxStudents phải lớn hơn 0 - " +
                "courseCode=" + course.getCourseCode() + ", maxStudents=" + course.getMaxStudents());
          } else if (course.getMaxStudents() <= course.getCurrentStudents()) {
            capacityOk = false;
            LOGGER.warning("Không thể upload course: maxStudents phải lớn hơn currentStudents - " +
                "courseCode=" + course.getCourseCode() +
                ", maxStudents=" + course.getMaxStudents() +
                ", currentStudents=" + course.getCurrentStudents());
          }

          Course existing = courseDAO.findByCourseCode(course.getCourseCode());
          if (existing == null) {
            if (subjectOk && classOk && userOk && !scheduleConflict && capacityOk && courseDAO.addCourse(course)) {
              if (course.getCourseId() > 0) {
                dataOriginHelper.saveDataOrigin("course", course.getCourseId(), source);
                successCount++;
                LOGGER.info("Đã tải lên thành công khóa học: " + course.getCourseCode());
              } else {
                failCount++;
                LOGGER.warning("Không thể lưu khóa học: " + course.getCourseCode() + " - ID chưa được thiết lập");
              }
            } else {
              failCount++;
              String reason = "";
              if (!subjectOk)
                reason += " (không tìm thấy môn học: " + course.getSubjectCode() + ")";
              if (!classOk)
                reason += " (không tìm thấy lớp: " + course.getClassCode() + ")";
              if (!userOk)
                reason += " (không tìm thấy/tạo được người dùng giáo viên: " + course.getTeacherUsername() + ")";
              if (scheduleConflict)
                reason += " (trùng lịch học: " + course.getScheduleDay() + " " + course.getScheduleTime() + ", Phòng: "
                    + course.getRoom() + ")";
              if (!capacityOk)
                reason += " (maxStudents <= currentStudents hoặc maxStudents <= 0)";
              LOGGER.warning("Không thể lưu khóa học: " + course.getCourseCode() + reason);
            }
          } else {
            LOGGER.info("Khóa học đã tồn tại, bỏ qua: " + course.getCourseCode() + " (ID hiện tại: "
                + existing.getCourseId() + ")");
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên khóa học " + course.getCourseCode(), ex);
        }
      }

      String message = createUploadMessage("courses", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_COURSES, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên khóa học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES, "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUploadEnrollments(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Enrollment> enrollments = (List<Enrollment>) request
          .getData("enrollments");

      if (enrollments == null || enrollments.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
            "No enrollments to upload");
      }

      LOGGER.info("Uploading " + enrollments.size() + " enrollments from client");

      int successCount = 0;
      int failCount = 0;
      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      StudentDAO studentDAO = new StudentDAO();
      CourseDAO courseDAO = new CourseDAO();
      CourseRegistrationDAO courseRegistrationDAO = new CourseRegistrationDAO();
      String source = SOURCE;
      // Set để lưu các course cần cập nhật currentStudents sau khi upload xong
      java.util.Set<String> coursesToUpdate = new java.util.HashSet<>();

      for (Enrollment e : enrollments) {
        try {
          boolean studentOk = true;
          if (e.getStudentCode() != null && !e.getStudentCode().trim().isEmpty()) {
            Student student = studentDAO.findByStudentCode(e.getStudentCode().trim());
            if (student == null) {
              studentOk = false;
              LOGGER.warning("Không tìm thấy mã sinh viên: " + e.getStudentCode() + " cho enrollment");
            }
          }

          Course course = null;
          boolean courseOk = true;
          if (e.getCourseCode() != null && !e.getCourseCode().trim().isEmpty()) {
            course = courseDAO.findByCourseCode(e.getCourseCode().trim());
            if (course == null) {
              courseOk = false;
              LOGGER.warning("Không tìm thấy mã khóa học: " + e.getCourseCode() + " cho enrollment");
            }
          }

          if (!studentOk || !courseOk) {
            failCount++;
            LOGGER.warning("Không thể lưu enrollment: studentCode=" + e.getStudentCode() +
                ", courseCode=" + e.getCourseCode() + " (FK validation failed)");
            continue;
          }

          // Kiểm tra xem enrollment status có tính vào currentStudents không
          boolean countsAsEnrolled = (e.getEnrollmentStatus() == Enrollment.EnrollmentStatus.ENROLLED ||
              e.getEnrollmentStatus() == Enrollment.EnrollmentStatus.COMPLETED ||
              e.getEnrollmentStatus() == Enrollment.EnrollmentStatus.FAILED);

          Enrollment existingEnrollment = null;
          if (e.getStudentCode() != null && !e.getStudentCode().trim().isEmpty() &&
              e.getCourseCode() != null && !e.getCourseCode().trim().isEmpty()) {
            existingEnrollment = enrollmentDAO.findByStudentAndCourse(
                e.getStudentCode().trim(), e.getCourseCode().trim());
          }

          if (existingEnrollment != null) {
            // Update enrollment hiện tại
            Enrollment.EnrollmentStatus oldStatus = existingEnrollment.getEnrollmentStatus();
            boolean oldCountsAsEnrolled = (oldStatus == Enrollment.EnrollmentStatus.ENROLLED ||
                oldStatus == Enrollment.EnrollmentStatus.COMPLETED ||
                oldStatus == Enrollment.EnrollmentStatus.FAILED);

            // Nếu status cũ không tính nhưng status mới tính, cần kiểm tra course đã đầy
            // chưa
            if (!oldCountsAsEnrolled && countsAsEnrolled) {
              // Đếm số enrollments hiện tại với status ENROLLED/COMPLETED/FAILED
              int currentEnrolledCount = enrollmentDAO.countByCourse(e.getCourseCode().trim());
              if (currentEnrolledCount >= course.getMaxStudents()) {
                failCount++;
                LOGGER.warning("Không thể cập nhật enrollment: khóa học đã đầy (" +
                    currentEnrolledCount + "/" + course.getMaxStudents() +
                    ") - studentCode=" + e.getStudentCode() + ", courseCode=" + e.getCourseCode());
                continue;
              }
            }

            e.setEnrollmentId(existingEnrollment.getEnrollmentId());
            boolean ok = enrollmentDAO.save(e);
            if (ok) {
              dataOriginHelper.updateDataOriginTimestamp("enrollment", existingEnrollment.getEnrollmentId());
              successCount++;
              coursesToUpdate.add(e.getCourseCode().trim());
              LOGGER.info("Đã cập nhật enrollment: studentCode=" + e.getStudentCode() +
                  ", courseCode=" + e.getCourseCode() + ", status=" + e.getEnrollmentStatus());
            } else {
              failCount++;
              LOGGER.warning("Không thể cập nhật enrollment: studentCode=" + e.getStudentCode() +
                  ", courseCode=" + e.getCourseCode());
            }
            continue;
          }

          // Enrollment mới
          // Nếu status tính vào currentStudents, kiểm tra course đã đầy chưa
          if (countsAsEnrolled) {
            // Đếm số enrollments hiện tại với status ENROLLED/COMPLETED/FAILED
            int currentEnrolledCount = enrollmentDAO.countByCourse(e.getCourseCode().trim());
            if (currentEnrolledCount >= course.getMaxStudents()) {
              failCount++;
              LOGGER.warning("Không thể thêm enrollment: khóa học đã đầy (" +
                  currentEnrolledCount + "/" + course.getMaxStudents() +
                  ") - studentCode=" + e.getStudentCode() + ", courseCode=" + e.getCourseCode());
              continue;
            }

            // Kiểm tra schedule conflict với các enrollment/registration khác của cùng
            // student
            if (courseRegistrationDAO.hasScheduleConflict(e.getStudentCode().trim(), e.getCourseCode().trim())) {
              failCount++;
              LOGGER.warning("Không thể thêm enrollment: xung đột lịch học - studentCode=" +
                  e.getStudentCode() + ", courseCode=" + e.getCourseCode());
              continue;
            }

            // Kiểm tra credits limit (MAX_CREDITS_PER_SEMESTER = 24)
            int currentCredits = courseRegistrationDAO.getTotalCredits(
                e.getStudentCode().trim(),
                course.getAcademicYear(),
                course.getSemester());
            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > 24) {
              failCount++;
              LOGGER.warning("Không thể thêm enrollment: vượt quá số tín chỉ tối đa (24) - " +
                  "Hiện tại: " + currentCredits + ", Khóa học: " + courseCredits +
                  " - studentCode=" + e.getStudentCode() + ", courseCode=" + e.getCourseCode());
              continue;
            }
          }

          e.setEnrollmentId(0);
          boolean ok = enrollmentDAO.save(e);
          if (ok && e.getEnrollmentId() > 0) {
            dataOriginHelper.saveDataOrigin("enrollment", e.getEnrollmentId(), source);
            successCount++;
            if (countsAsEnrolled) {
              coursesToUpdate.add(e.getCourseCode().trim());
            }
            LOGGER.info("Đã thêm enrollment: studentCode=" + e.getStudentCode() +
                ", courseCode=" + e.getCourseCode() + ", status=" + e.getEnrollmentStatus());
          } else {
            failCount++;
            LOGGER.warning("Không thể lưu enrollment: studentCode=" + e.getStudentCode() +
                ", courseCode=" + e.getCourseCode());
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký học phần", ex);
        }
      }

      // Cập nhật currentStudents cho tất cả các course đã được upload enrollments
      for (String courseCode : coursesToUpdate) {
        try {
          int actualCount = enrollmentDAO.countByCourse(courseCode);
          Course course = courseDAO.findByCourseCode(courseCode);
          if (course != null) {
            courseDAO.updateCurrentStudents(course.getCourseId(), actualCount);
            LOGGER.info("Đã cập nhật currentStudents cho khóa học " + courseCode + ": " + actualCount);
          }
        } catch (Exception ex) {
          LOGGER.warning("Lỗi khi cập nhật currentStudents cho khóa học " + courseCode + ": " + ex.getMessage());
        }
      }

      String message = createUploadMessage("enrollments", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_ENROLLMENTS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên đăng ký học phần: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
          "Error: " + e.getMessage());
    }
  }

  public Message handleUploadGrades(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Grade> grades = (List<Grade>) request
          .getData("grades");

      if (grades == null || grades.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES, "No grades to upload");
      }

      LOGGER.info("Uploading " + grades.size() + " grades from client");

      int successCount = 0;
      int failCount = 0;
      GradeDAO gradeDAO = new GradeDAO();
      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      String source = SOURCE;

      for (Grade g : grades) {
        try {
          // Kiểm tra enrollment tồn tại
          Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(
              g.getStudentCode(), g.getCourseCode());
          if (enrollment == null) {
            failCount++;
            LOGGER.warning("Không thể upload grade: enrollment không tồn tại - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode());
            continue;
          }

          // Kiểm tra grade value hợp lệ
          if (g.getScore() == null || g.getMaxScore() == null) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm không được để trống - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode());
            continue;
          }

          if (g.getScore().compareTo(java.math.BigDecimal.ZERO) < 0) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm không được âm - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode() + ", score=" + g.getScore());
            continue;
          }

          if (g.getMaxScore().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm tối đa phải > 0 - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode() + ", maxScore=" + g.getMaxScore());
            continue;
          }

          if (g.getScore().compareTo(new java.math.BigDecimal("10")) > 0) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm không được vượt quá 10 - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode() + ", score=" + g.getScore());
            continue;
          }

          if (g.getMaxScore().compareTo(new java.math.BigDecimal("10")) > 0) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm tối đa không được vượt quá 10 - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode() + ", maxScore=" + g.getMaxScore());
            continue;
          }

          if (g.getScore().compareTo(g.getMaxScore()) > 0) {
            failCount++;
            LOGGER.warning("Không thể upload grade: điểm không được vượt quá điểm tối đa - studentCode=" +
                g.getStudentCode() + ", courseCode=" + g.getCourseCode() +
                ", score=" + g.getScore() + ", maxScore=" + g.getMaxScore());
            continue;
          }

          // Kiểm tra weight hợp lệ (nếu có)
          if (g.getWeight() != null) {
            if (g.getWeight().compareTo(java.math.BigDecimal.ZERO) <= 0 ||
                g.getWeight().compareTo(java.math.BigDecimal.ONE) > 0) {
              failCount++;
              LOGGER.warning("Không thể upload grade: trọng số phải trong khoảng (0.0, 1.0] - studentCode=" +
                  g.getStudentCode() + ", courseCode=" + g.getCourseCode() + ", weight=" + g.getWeight());
              continue;
            }
          }

          boolean exists = false;
          try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement checkStmt = conn.prepareStatement(
                  "SELECT COUNT(*) FROM grades WHERE student_code = ? AND course_code = ? AND grade_type = ? "
                      + "AND (grade_name = ? OR (grade_name IS NULL AND ? IS NULL))")) {
            checkStmt.setString(1, g.getStudentCode());
            checkStmt.setString(2, g.getCourseCode());
            checkStmt.setString(3, g.getGradeType().name().toLowerCase());
            if (g.getGradeName() != null && !g.getGradeName().isEmpty()) {
              checkStmt.setString(4, g.getGradeName());
              checkStmt.setString(5, g.getGradeName());
            } else {
              checkStmt.setNull(4, java.sql.Types.VARCHAR);
              checkStmt.setNull(5, java.sql.Types.VARCHAR);
            }
            try (ResultSet rs = checkStmt.executeQuery()) {
              if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
                LOGGER.info("Grade already exists, skipping: student=" + g.getStudentCode()
                    + ", course=" + g.getCourseCode() + ", type=" + g.getGradeType()
                    + ", name=" + g.getGradeName());
              }
            }
          } catch (Exception checkEx) {
            LOGGER.warning("Error checking grade duplicate: " + checkEx.getMessage());
          }

          if (!exists) {
            g.setGradeId(0);
            if (gradeDAO.save(g)) {
              dataOriginHelper.saveDataOrigin("grade", g.getGradeId(), source);
              successCount++;
            } else {
              failCount++;
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên điểm", ex);
        }
      }

      String message = createUploadMessage("grades", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_GRADES, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên grades: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES, "Error: " + e.getMessage());
    }
  }

  public Message handleUploadClassOpeningRequests(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<ClassOpeningRequest> requests = (List<ClassOpeningRequest>) request
          .getData("requests");

      if (requests == null || requests.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
            "No class opening requests to upload");
      }

      LOGGER.info("Uploading " + requests.size() + " class opening requests from client");

      int successCount = 0;
      int failCount = 0;
      ClassOpeningRequestService service = classRequestService;
      com.university.sms.dao.ClassOpeningRequestDAO requestDAO = new com.university.sms.dao.ClassOpeningRequestDAO();
      String source = SOURCE;

      for (ClassOpeningRequest r : requests) {
        try {
          // Kiểm tra teacher tồn tại
          UserDAO userDAO = new UserDAO();
          User teacher = userDAO.findByUsername(r.getTeacherUsername());
          if (teacher == null) {
            failCount++;
            LOGGER.warning("Không thể upload class opening request: teacher không tồn tại - " +
                "teacherUsername=" + r.getTeacherUsername() + ", subjectCode=" + r.getSubjectCode());
            continue;
          }

          // Kiểm tra subject tồn tại
          SubjectDAO subjectDAO = new SubjectDAO();
          Subject subject = subjectDAO.findByCode(r.getSubjectCode());
          if (subject == null) {
            failCount++;
            LOGGER.warning("Không thể upload class opening request: subject không tồn tại - " +
                "teacherUsername=" + r.getTeacherUsername() + ", subjectCode=" + r.getSubjectCode());
            continue;
          }

          // Kiểm tra schedule conflict với các course/request khác (nếu có schedule)
          if (r.getScheduleDay() != null && !r.getScheduleDay().isEmpty() &&
              r.getScheduleTime() != null && !r.getScheduleTime().isEmpty() &&
              r.getRoom() != null && !r.getRoom().isEmpty()) {
            CourseDAO courseDAO = new CourseDAO();
            List<Course> conflictingCourses = courseDAO.findByScheduleAndRoom(
                r.getScheduleDay(), r.getScheduleTime(), r.getRoom());
            if (!conflictingCourses.isEmpty()) {
              failCount++;
              LOGGER.warning("Không thể upload class opening request: trùng lịch học - " +
                  "teacherUsername=" + r.getTeacherUsername() + ", subjectCode=" + r.getSubjectCode() +
                  ", schedule=" + r.getScheduleDay() + " " + r.getScheduleTime() + ", room=" + r.getRoom());
              continue;
            }
          }

          List<ClassOpeningRequest> existingRequests = requestDAO.findByTeacher(r.getTeacherUsername());
          boolean exists = false;
          for (ClassOpeningRequest existing : existingRequests) {
            if (r.getSubjectCode() != null && r.getSubjectCode().equals(existing.getSubjectCode()) &&
                r.getAcademicYear() != null && r.getAcademicYear().equals(existing.getAcademicYear()) &&
                r.getSemester() == existing.getSemester() &&
                ((r.getScheduleDay() == null && existing.getScheduleDay() == null) ||
                    (r.getScheduleDay() != null
                        && r.getScheduleDay().equals(existing.getScheduleDay())))
                &&
                ((r.getScheduleTime() == null && existing.getScheduleTime() == null) ||
                    (r.getScheduleTime() != null
                        && r.getScheduleTime().equals(existing.getScheduleTime())))) {
              exists = true;
              LOGGER.info(
                  "ClassOpeningRequest already exists, skipping: teacher=" + r.getTeacherUsername() +
                      ", subject=" + r.getSubjectCode() + ", year=" + r.getAcademicYear() +
                      ", semester=" + r.getSemester());
              break;
            }
          }

          if (!exists) {
            r.setRequestId(0);
            if (service.submitRequest(r)) {
              dataOriginHelper.saveDataOrigin("class_opening_request", r.getRequestId(), source);
              successCount++;
            } else {
              failCount++;
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên class opening request", ex);
        }
      }

      String message = createUploadMessage("class opening requests", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên class opening requests: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
          "Error: " + e.getMessage());
    }
  }

  public Message handleUploadCourseRegistrations(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<CourseRegistration> registrations = (List<CourseRegistration>) request
          .getData("registrations");

      if (registrations == null || registrations.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
            "No course registrations to upload");
      }

      LOGGER.info("Uploading " + registrations.size() + " course registrations from client");

      int successCount = 0;
      int failCount = 0;
      CourseRegistrationDAO dao = new CourseRegistrationDAO();
      CourseDAO courseDAO = new CourseDAO();
      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      String source = SOURCE;
      // Set để lưu các course cần cập nhật currentStudents sau khi upload xong
      java.util.Set<String> coursesToUpdate = new java.util.HashSet<>();
      final int MAX_CREDITS_PER_SEMESTER = 24;

      for (CourseRegistration r : registrations) {
        try {
          // Kiểm tra duplicate: nếu đã có registration với BẤT KỲ status nào thì skip
          // (kể cả CANCELLED) để tránh conflict
          if (dao.exists(r.getStudentCode(), r.getCourseCode())) {
            LOGGER.info("CourseRegistration đã tồn tại (duplicate), bỏ qua: student="
                + r.getStudentCode() +
                ", course=" + r.getCourseCode());
            continue;
          }

          Course course = courseDAO.findByCourseCode(r.getCourseCode());
          if (course == null) {
            failCount++;
            LOGGER.warning("Không thể upload course registration: khóa học không tồn tại - " +
                "courseCode=" + r.getCourseCode());
            continue;
          }

          // Kiểm tra khóa học có đang mở đăng ký không
          if (course.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
            failCount++;
            LOGGER.warning("Không thể upload course registration: khóa học không mở đăng ký (status=" +
                course.getRegistrationStatus() + ") - studentCode=" + r.getStudentCode() +
                ", courseCode=" + r.getCourseCode());
            continue;
          }

          // Kiểm tra số lượng registration (PENDING + APPROVED) không vượt quá
          // max_students
          int currentRegistrationsCount = dao.countByCourseAndStatus(r.getCourseCode(),
              CourseRegistration.RegistrationStatus.PENDING,
              CourseRegistration.RegistrationStatus.APPROVED);
          if (currentRegistrationsCount >= course.getMaxStudents()) {
            failCount++;
            LOGGER.warning("Không thể upload course registration: số lượng đăng ký đã đầy (" +
                currentRegistrationsCount + "/" + course.getMaxStudents() +
                ") - studentCode=" + r.getStudentCode() + ", courseCode=" + r.getCourseCode());
            continue;
          }

          // Validation chỉ áp dụng cho APPROVED status
          if (r.getRegistrationStatus() == CourseRegistration.RegistrationStatus.APPROVED) {
            int currentEnrolledCount = enrollmentDAO.countByCourse(r.getCourseCode());
            if (currentEnrolledCount >= course.getMaxStudents()) {
              failCount++;
              LOGGER.warning("Không thể upload course registration với status APPROVED: khóa học đã đầy (" +
                  currentEnrolledCount + "/" + course.getMaxStudents() +
                  ") - studentCode=" + r.getStudentCode() + ", courseCode=" + r.getCourseCode());
              continue;
            }

            if (dao.hasScheduleConflict(r.getStudentCode(), r.getCourseCode())) {
              failCount++;
              LOGGER.warning("Không thể upload course registration: xung đột lịch học - studentCode=" +
                  r.getStudentCode() + ", courseCode=" + r.getCourseCode());
              continue;
            }

            int currentCredits = dao.getTotalCredits(
                r.getStudentCode(),
                course.getAcademicYear(),
                course.getSemester());
            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > MAX_CREDITS_PER_SEMESTER) {
              failCount++;
              LOGGER.warning("Không thể upload course registration: vượt quá số tín chỉ tối đa (" +
                  MAX_CREDITS_PER_SEMESTER + ") - Hiện tại: " + currentCredits + ", Khóa học: " + courseCredits +
                  " - studentCode=" + r.getStudentCode() + ", courseCode=" + r.getCourseCode());
              continue;
            }
          }

          r.setRegistrationId(0);
          if (dao.save(r)) {
            // Kiểm tra xem có thực sự insert được không (INSERT IGNORE có thể không insert
            // nếu duplicate)
            CourseRegistration inserted = dao.findByStudentAndCourse(r.getStudentCode(), r.getCourseCode());
            if (inserted != null) {
              r.setRegistrationId(inserted.getRegistrationId());
              dataOriginHelper.saveDataOrigin("course_registration", r.getRegistrationId(), source);
              successCount++;
              if (r.getRegistrationStatus() == CourseRegistration.RegistrationStatus.APPROVED) {
                coursesToUpdate.add(r.getCourseCode());
              }
              LOGGER.info("Đã upload course registration: studentCode=" + r.getStudentCode() +
                  ", courseCode=" + r.getCourseCode() + ", status=" + r.getRegistrationStatus());
            } else {
              // INSERT IGNORE đã skip do duplicate (có thể do race condition)
              LOGGER.info("CourseRegistration đã tồn tại (duplicate key), bỏ qua: student="
                  + r.getStudentCode() + ", course=" + r.getCourseCode());
            }
          } else {
            failCount++;
            LOGGER.warning("Không thể lưu course registration: studentCode=" + r.getStudentCode() +
                ", courseCode=" + r.getCourseCode());
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên course registration", ex);
        }
      }

      // Cập nhật currentStudents cho tất cả các course có registration status
      // APPROVED
      for (String courseCode : coursesToUpdate) {
        try {
          int actualCount = enrollmentDAO.countByCourse(courseCode);
          Course course = courseDAO.findByCourseCode(courseCode);
          if (course != null) {
            courseDAO.updateCurrentStudents(course.getCourseId(), actualCount);
            LOGGER.info("Đã cập nhật currentStudents cho khóa học " + courseCode + ": " + actualCount);
          }
        } catch (Exception ex) {
          LOGGER.warning("Lỗi khi cập nhật currentStudents cho khóa học " + courseCode + ": " + ex.getMessage());
        }
      }

      String message = createUploadMessage("course registrations", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên course registrations: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
          "Error: " + e.getMessage());
    }
  }

  public Message handleUploadNotifications(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
            "Không có quyền truy cập");
      }

      @SuppressWarnings("unchecked")
      List<Notification> notifications = (List<Notification>) request
          .getData("notifications");

      if (notifications == null || notifications.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
            "No notifications to upload");
      }

      LOGGER.info("Uploading " + notifications.size() + " notifications from client");

      int successCount = 0;
      int failCount = 0;
      NotificationService service = notificationService;
      UserDAO userDAO = new UserDAO();
      String source = SOURCE;

      for (Notification notification : notifications) {
        try {
          String senderUsername = notification.getSenderUsername();
          boolean userOk = true;
          if (senderUsername != null && !senderUsername.isEmpty()) {
            User existingUser = userDAO.findByUsername(senderUsername);
            if (existingUser == null) {
              userOk = false;
              LOGGER.warning("Không thể upload notification: sender không tồn tại - " +
                  "senderUsername=" + senderUsername);
            }
          }

          // Kiểm tra targetType và targetCode hợp lệ
          boolean targetOk = true;
          if (notification.getTargetType() != null && notification.getTargetType() != Notification.TargetType.ALL) {
            String targetCode = notification.getTargetCode();
            if (targetCode == null || targetCode.trim().isEmpty()) {
              targetOk = false;
              LOGGER.warning("Không thể upload notification: targetCode không được để trống khi targetType=" +
                  notification.getTargetType());
            } else {
              switch (notification.getTargetType()) {
                case FACULTY:
                  FacultyDAO facultyDAO = new FacultyDAO();
                  if (facultyDAO.findByCode(targetCode) == null) {
                    targetOk = false;
                    LOGGER.warning("Không thể upload notification: faculty không tồn tại - targetCode=" + targetCode);
                  }
                  break;
                case CLASS:
                  ClassDAO classDAO = new ClassDAO();
                  if (classDAO.findByCode(targetCode) == null) {
                    targetOk = false;
                    LOGGER.warning("Không thể upload notification: class không tồn tại - targetCode=" + targetCode);
                  }
                  break;
                case STUDENT:
                  StudentDAO studentDAO = new StudentDAO();
                  if (studentDAO.findByStudentCode(targetCode) == null) {
                    targetOk = false;
                    LOGGER.warning("Không thể upload notification: student không tồn tại - targetCode=" + targetCode);
                  }
                  break;
                default:
                  break;
              }
            }
          }

          if (!userOk || !targetOk) {
            failCount++;
            continue;
          }

          boolean exists = false;
          try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement checkStmt = conn.prepareStatement(
                  "SELECT COUNT(*) FROM notifications WHERE title = ? AND content = ? AND sender_username = ? AND target_type = ? "
                      + "AND (target_code = ? OR (target_code IS NULL AND ? IS NULL))")) {
            checkStmt.setString(1, notification.getTitle());
            checkStmt.setString(2, notification.getContent());
            checkStmt.setString(3, notification.getSenderUsername());
            checkStmt.setString(4, notification.getTargetType().name().toLowerCase());
            if (notification.getTargetCode() != null && !notification.getTargetCode().isEmpty()) {
              checkStmt.setString(5, notification.getTargetCode());
              checkStmt.setString(6, notification.getTargetCode());
            } else {
              checkStmt.setNull(5, java.sql.Types.VARCHAR);
              checkStmt.setNull(6, java.sql.Types.VARCHAR);
            }
            try (ResultSet rs = checkStmt.executeQuery()) {
              if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
                LOGGER.info("Notification already exists, skipping: title=" + notification.getTitle()
                    + ", sender=" + notification.getSenderUsername());
              }
            }
          } catch (Exception checkEx) {
            LOGGER.warning("Error checking notification duplicate: " + checkEx.getMessage());
          }

          if (!exists && userOk) {
            notification.setNotificationId(0);
            if (service.createNotification(notification)) {
              dataOriginHelper.saveDataOrigin("notification", notification.getNotificationId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning(
                  "Failed to save notification: " + notification.getNotificationId() + " - " + notification.getTitle());
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên thông báo", ex);
        }
      }

      String message = createUploadMessage("notifications", successCount, failCount);
      return Message.createSuccessResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS, message);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi xử lý tải lên notifications: " + e.getMessage());
      return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
          "Error: " + e.getMessage());
    }
  }

  private Map<String, Object> getServerMetadata() {
    Map<String, Object> metadata = new HashMap<>();

    try {
      int studentCount = studentService.getTotalCount();
      int courseCount = courseService.getTotalCount();

      int dbVersion = getServerVersion();

      List<String> sources = getAvailableSources();

      for (String source : sources) {
        int sourceVersion = getDataVersionBySource(source);
        int sourceStudentCount = getDataCountBySource("student", source);
        int sourceCourseCount = getDataCountBySource("course", source);
        int sourceEnrollmentCount = getDataCountBySource("enrollment", source);
        int sourceFacultyCount = getDataCountBySource("faculty", source);
        int sourceClassCount = getDataCountBySource("class", source);
        int sourceSubjectCount = getDataCountBySource("subject", source);
        int sourceTotalRecords = sourceStudentCount + sourceCourseCount + sourceEnrollmentCount +
            sourceFacultyCount + sourceClassCount + sourceSubjectCount;

        String sourceKey = source.toLowerCase();
        metadata.put(sourceKey + "_version", sourceVersion);
        metadata.put(sourceKey + "_student_count", sourceStudentCount);
        metadata.put(sourceKey + "_course_count", sourceCourseCount);
        metadata.put(sourceKey + "_enrollment_count", sourceEnrollmentCount);
        metadata.put(sourceKey + "_faculty_count", sourceFacultyCount);
        metadata.put(sourceKey + "_class_count", sourceClassCount);
        metadata.put(sourceKey + "_subject_count", sourceSubjectCount);
        metadata.put(sourceKey + "_total_records", sourceTotalRecords);
      }

      metadata.put("db_version", dbVersion);
      metadata.put("student_count", studentCount);
      metadata.put("course_count", courseCount);
      metadata.put("total_records", studentCount + courseCount);

    } catch (Exception e) {
      LOGGER.warning("Lỗi khi lấy metadata server: " + e.getMessage());
      LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
      metadata.put("db_version", 1);
      metadata.put("student_count", 0);
      metadata.put("course_count", 0);
      metadata.put("total_records", 0);
    }

    return metadata;
  }

  private List<String> getAvailableSources() {
    List<String> sources = new ArrayList<>();
    String sql = "SELECT DISTINCT source FROM data_origin WHERE source IS NOT NULL ORDER BY source";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        sources.add(rs.getString("source"));
      }
    } catch (Exception e) {
      LOGGER.warning("Lỗi khi lấy danh sách nguồn dữ liệu: " + e.getMessage());
      LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
    }
    return sources;
  }

  private int getDataCountBySource(String entityType, String source) {
    String tableName = getTableName(entityType);
    String entityIdColumn = getEntityIdColumn(entityType);

    String sql = "SELECT COUNT(*) as count FROM " + tableName + " e " +
        "JOIN data_origin dor ON dor.entity_type = ? AND dor.entity_id = e." + entityIdColumn + " " +
        "WHERE dor.source = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, entityType);
      stmt.setString(2, source);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("count");
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Lỗi khi đếm số lượng " + entityType + " với nguồn " + source + ": " + e.getMessage());
      LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
    }
    return 0;
  }

  private int getDataVersionBySource(String source) {
    String sql = "SELECT MAX(UNIX_TIMESTAMP(updated_at)) as last_update FROM data_origin WHERE source = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          long timestamp = rs.getLong("last_update");
          if (!rs.wasNull() && timestamp > 0) {
            return (int) timestamp;
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Lỗi khi lấy timestamp cập nhật cuối cho nguồn " + source + ": " + e.getMessage());
      LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
    }

    int total = getDataCountBySource("student", source) + getDataCountBySource("course", source) +
        getDataCountBySource("enrollment", source) + getDataCountBySource("faculty", source) +
        getDataCountBySource("class", source) + getDataCountBySource("subject", source);
    return total;
  }

  private String getTableName(String entityType) {
    switch (entityType) {
      case "student":
        return "students";
      case "course":
        return "courses";
      case "enrollment":
        return "enrollments";
      case "faculty":
        return "faculties";
      case "class":
        return "classes";
      case "subject":
        return "subjects";
      case "grade":
        return "grades";
      case "notification":
        return "notifications";
      case "class_opening_request":
        return "class_opening_requests";
      case "course_registration":
        return "course_registrations";
      case "user":
        return "users";
      default:
        return entityType + "s";
    }
  }

  private String getEntityIdColumn(String entityType) {
    switch (entityType) {
      case "student":
        return "student_id";
      case "course":
        return "course_id";
      case "enrollment":
        return "enrollment_id";
      case "faculty":
        return "faculty_id";
      case "class":
        return "class_id";
      case "subject":
        return "subject_id";
      case "grade":
        return "grade_id";
      case "notification":
        return "notification_id";
      case "class_opening_request":
        return "request_id";
      case "course_registration":
        return "registration_id";
      case "user":
        return "user_id";
      default:
        return entityType + "_id";
    }
  }

  private int getServerVersion() {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT CAST(config_value AS UNSIGNED) as version FROM system_config WHERE config_key = 'db_version'");
        ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt("version");
      }

      try (PreparedStatement insertStmt = conn.prepareStatement(
          "INSERT INTO system_config (config_key, config_value, description) "
              + "VALUES ('db_version', '1', 'Database version')")) {
        insertStmt.executeUpdate();
      }
      return 1;
    } catch (Exception e) {
      LOGGER.warning("Error getting server version: " + e.getMessage());
      return 1;
    }
  }

  private String createUploadMessage(String entityName, int successCount, int failCount) {
    if (failCount == 0) {
      return String.format("Uploaded %d %s successfully", successCount, entityName);
    } else if (successCount == 0) {
      return String.format("Uploaded 0 %s successfully, %d failed", entityName, failCount);
    } else {
      return String.format("Uploaded %d %s successfully, %d failed", successCount, entityName, failCount);
    }
  }

  private boolean isValidEmailFormat(String email) {
    if (email == null || email.trim().isEmpty()) {
      return false;
    }
    String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    return email.trim().matches(emailRegex);
  }

  private boolean isValidPhoneFormat(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return false;
    }
    String phoneStr = phone.trim();
    String normalized = phoneStr.replaceAll("[^0-9+]", "");
    if (normalized.isEmpty()) {
      return false;
    }
    if (normalized.matches("^0[0-9]{9}$")) {
      return true;
    }
    if (normalized.matches("^\\+84[0-9]{9}$")) {
      return true;
    }
    if (normalized.matches("^[1-9][0-9]{9}$")) {
      return true;
    }
    return false;
  }

  private String normalizePhoneNumber(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return phone;
    }
    return phone.trim().replaceAll("[^0-9+]", "");
  }

  private List<Student> getStudentsBySource(String source) {
    String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, u.is_active, " +
        "f.faculty_name, c.class_name " +
        "FROM students s " +
        "LEFT JOIN users u ON s.username = u.username " +
        "LEFT JOIN faculties f ON s.faculty_code = f.faculty_code " +
        "LEFT JOIN classes c ON s.class_code = c.class_code " +
        "JOIN data_origin dor ON dor.entity_type = 'student' AND dor.entity_id = s.student_id " +
        "WHERE dor.source = ? " +
        "ORDER BY s.student_id";

    List<Student> students = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Student student = new Student();
          int studentId = rs.getInt("student_id");
          student.setStudentId(studentId);
          student.setUsername(rs.getString("username"));
          student.setStudentCode(rs.getString("student_code"));
          String classCode = rs.getString("class_code");
          if (!rs.wasNull()) {
            student.setClassCode(classCode);
          }
          student.setFacultyCode(rs.getString("faculty_code"));
          student.setAdmissionYear(rs.getInt("admission_year"));
          String status = rs.getString("student_status");
          if (status != null) {
            student.setStudentStatus(
                Student.StudentStatus.valueOf(status.toUpperCase()));
          }
          student.setGpa(rs.getBigDecimal("gpa"));
          student.setTotalCredits(rs.getInt("total_credits"));
          student.setBirthDate(rs.getDate("birth_date"));
          String gender = rs.getString("gender");
          if (gender != null) {
            student.setGender(Student.Gender.valueOf(gender.toUpperCase()));
          }
          student.setCitizenId(rs.getString("citizen_id"));
          student.setEmergencyContact(rs.getString("emergency_contact"));
          student.setEmergencyPhone(rs.getString("emergency_phone"));
          student.setCreatedAt(rs.getTimestamp("created_at"));
          student.setFullName(rs.getString("full_name"));
          student.setEmail(rs.getString("email"));
          student.setPhone(rs.getString("phone"));
          student.setAddress(rs.getString("address"));
          try {
            if (rs.getObject("is_active") != null) {
              student.setActive(rs.getBoolean("is_active"));
            } else {
              student.setActive(true);
            }
          } catch (java.sql.SQLException e) {
            student.setActive(true);
          }
          student.setFacultyName(rs.getString("faculty_name"));
          student.setClassName(rs.getString("class_name"));
          students.add(student);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách sinh viên theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }
    return students;
  }

  private List<Course> getCoursesBySource(String source) {
    String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
        "u.full_name AS teacher_name, cl.class_name " +
        "FROM courses c " +
        "JOIN subjects sub ON c.subject_code = sub.subject_code " +
        "JOIN users u ON c.teacher_username = u.username " +
        "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
        "JOIN data_origin dor ON dor.entity_type = 'course' AND dor.entity_id = c.course_id " +
        "WHERE dor.source = ?";

    List<Course> courses = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Course course = new Course();
          course.setCourseId(rs.getInt("course_id"));
          course.setCourseCode(rs.getString("course_code"));
          course.setSubjectCode(rs.getString("subject_code"));
          course.setTeacherUsername(rs.getString("teacher_username"));
          String classCode = rs.getString("class_code");
          if (!rs.wasNull()) {
            course.setClassCode(classCode);
          }
          course.setAcademicYear(rs.getString("academic_year"));
          course.setSemester(rs.getInt("semester"));
          course.setScheduleDay(rs.getString("schedule_day"));
          course.setScheduleTime(rs.getString("schedule_time"));
          course.setRoom(rs.getString("room"));
          course.setMaxStudents(rs.getInt("max_students"));
          course.setCurrentStudents(rs.getInt("current_students"));
          String status = rs.getString("course_status");
          if (status != null) {
            course.setCourseStatus(
                Course.CourseStatus.valueOf(status.toUpperCase()));
          }
          course.setStartDate(rs.getDate("start_date"));
          course.setEndDate(rs.getDate("end_date"));
          course.setSubjectName(rs.getString("subject_name"));
          course.setCredits(rs.getInt("credits"));
          course.setTeacherName(rs.getString("teacher_name"));
          course.setClassName(rs.getString("class_name"));
          courses.add(course);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách khóa học theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return courses;
  }

  private List<Enrollment> getEnrollmentsBySource(String source) {
    String sql = "SELECT e.* FROM enrollments e " +
        "JOIN data_origin dor ON dor.entity_type = 'enrollment' AND dor.entity_id = e.enrollment_id " +
        "WHERE dor.source = ?";

    List<Enrollment> enrollments = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Enrollment enrollment = new Enrollment();
          enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
          enrollment.setStudentCode(rs.getString("student_code"));
          enrollment.setCourseCode(rs.getString("course_code"));
          enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));

          String status = rs.getString("enrollment_status");
          if (status != null && !status.trim().isEmpty()) {
            try {
              enrollment.setEnrollmentStatus(
                  Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
              LOGGER.warning("Enrollment status không hợp lệ: " + status + " cho enrollment ID: "
                  + enrollment.getEnrollmentId() + ", dùng ENROLLED làm mặc định");
              enrollment
                  .setEnrollmentStatus(Enrollment.EnrollmentStatus.ENROLLED);
            }
          } else {
            enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.ENROLLED);
          }

          enrollment.setFinalGrade(rs.getBigDecimal("final_grade"));
          enrollment.setLetterGrade(rs.getString("letter_grade"));
          enrollment.setGradePoints(rs.getBigDecimal("grade_points"));
          enrollments.add(enrollment);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách đăng ký học phần theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return enrollments;
  }

  private List<com.university.sms.model.Faculty> getFacultiesBySource(String source) {
    String sql = "SELECT f.* FROM faculties f " +
        "JOIN data_origin dor ON dor.entity_type = 'faculty' AND dor.entity_id = f.faculty_id " +
        "WHERE dor.source = ?";

    List<com.university.sms.model.Faculty> faculties = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          com.university.sms.model.Faculty faculty = new com.university.sms.model.Faculty();
          faculty.setFacultyId(rs.getInt("faculty_id"));
          faculty.setFacultyCode(rs.getString("faculty_code"));
          faculty.setFacultyName(rs.getString("faculty_name"));
          faculty.setDescription(rs.getString("description"));
          faculties.add(faculty);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách khoa theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return faculties;
  }

  private List<com.university.sms.model.Class> getClassesBySource(String source) {
    String sql = "SELECT c.* FROM classes c " +
        "JOIN data_origin dor ON dor.entity_type = 'class' AND dor.entity_id = c.class_id " +
        "WHERE dor.source = ?";

    List<com.university.sms.model.Class> classes = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          com.university.sms.model.Class classEntity = new com.university.sms.model.Class();
          classEntity.setClassId(rs.getInt("class_id"));
          classEntity.setClassCode(rs.getString("class_code"));
          classEntity.setClassName(rs.getString("class_name"));
          classEntity.setFacultyCode(rs.getString("faculty_code"));
          classEntity.setTeacherUsername(rs.getString("teacher_username"));
          classEntity.setAcademicYear(rs.getString("academic_year"));
          classEntity.setSemester(rs.getInt("semester"));
          classEntity.setMaxStudents(rs.getInt("max_students"));
          classes.add(classEntity);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách lớp theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return classes;
  }

  private List<Subject> getSubjectsBySource(String source) {
    String sql = "SELECT s.*, f.faculty_name FROM subjects s " +
        "LEFT JOIN faculties f ON s.faculty_code = f.faculty_code " +
        "JOIN data_origin dor ON dor.entity_type = 'subject' AND dor.entity_id = s.subject_id " +
        "WHERE dor.source = ?";

    List<Subject> subjects = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Subject subject = new Subject();
          subject.setSubjectId(rs.getInt("subject_id"));
          subject.setSubjectCode(rs.getString("subject_code"));
          subject.setSubjectName(rs.getString("subject_name"));
          subject.setCredits(rs.getInt("credits"));
          subject.setFacultyCode(rs.getString("faculty_code"));
          subject.setPrerequisiteSubjectCode(rs.getString("prerequisite_subject_code"));
          subject.setDescription(rs.getString("description"));
          subject.setRequired(rs.getBoolean("is_required"));
          subject.setFacultyName(rs.getString("faculty_name"));
          subjects.add(subject);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách môn học theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return subjects;
  }

  private List<User> getUsersBySource(String source) {
    String sql = "SELECT u.* FROM users u " +
        "JOIN data_origin dor ON dor.entity_type = 'user' AND dor.entity_id = u.user_id " +
        "WHERE dor.source = ?";

    List<User> users = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          User user = new User();
          user.setUserId(rs.getInt("user_id"));
          user.setUsername(rs.getString("username"));
          user.setPassword(rs.getString("password"));
          user.setFullName(rs.getString("full_name"));
          user.setEmail(rs.getString("email"));
          user.setPhone(rs.getString("phone"));
          user.setAddress(rs.getString("address"));
          String role = rs.getString("role");
          if (role != null) {
            user.setRole(User.UserRole.valueOf(role.toUpperCase()));
          }
          user.setActive(rs.getBoolean("is_active"));
          users.add(user);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách người dùng theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return users;
  }

  private List<Grade> getGradesBySource(String source) {
    String sql = "SELECT g.* FROM grades g " +
        "JOIN data_origin dor ON dor.entity_type = 'grade' AND dor.entity_id = g.grade_id " +
        "WHERE dor.source = ?";

    List<Grade> grades = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Grade grade = new Grade();
          grade.setGradeId(rs.getInt("grade_id"));
          grade.setStudentCode(rs.getString("student_code"));
          grade.setCourseCode(rs.getString("course_code"));
          String gradeType = rs.getString("grade_type");
          if (gradeType != null) {
            grade.setGradeType(Grade.GradeType.valueOf(gradeType.toUpperCase()));
          }
          grade.setGradeName(rs.getString("grade_name"));
          grade.setScore(rs.getBigDecimal("score"));
          grade.setMaxScore(rs.getBigDecimal("max_score"));
          grade.setWeight(rs.getBigDecimal("weight"));
          grade.setNotes(rs.getString("notes"));
          grade.setCreatedAt(rs.getTimestamp("created_at"));
          grades.add(grade);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách điểm theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return grades;
  }

  private List<Notification> getNotificationsBySource(String source) {
    String sql = "SELECT n.* FROM notifications n " +
        "JOIN data_origin dor ON dor.entity_type = 'notification' AND dor.entity_id = n.notification_id " +
        "WHERE dor.source = ?";

    List<Notification> notifications = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Notification notification = new Notification();
          notification.setNotificationId(rs.getInt("notification_id"));
          notification.setTitle(rs.getString("title"));
          notification.setContent(rs.getString("content"));
          notification.setSenderUsername(rs.getString("sender_username"));
          String targetType = rs.getString("target_type");
          if (targetType != null) {
            notification.setTargetType(
                Notification.TargetType.valueOf(targetType.toUpperCase()));
          }
          notification.setTargetCode(rs.getString("target_code"));
          String priority = rs.getString("priority");
          if (priority != null) {
            notification.setPriority(
                Notification.Priority.valueOf(priority.toUpperCase()));
          }
          notification.setRead(rs.getBoolean("is_read"));
          notification.setCreatedAt(rs.getTimestamp("created_at"));
          notification.setExpiresAt(rs.getTimestamp("expires_at"));
          notifications.add(notification);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách thông báo theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return notifications;
  }

  private List<ClassOpeningRequest> getClassOpeningRequestsBySource(String source) {
    String sql = "SELECT cor.* FROM class_opening_requests cor " +
        "JOIN data_origin dor ON dor.entity_type = 'class_opening_request' AND dor.entity_id = cor.request_id "
        +
        "WHERE dor.source = ?";

    List<ClassOpeningRequest> requests = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          ClassOpeningRequest request = new ClassOpeningRequest();
          request.setRequestId(rs.getInt("request_id"));
          request.setTeacherUsername(rs.getString("teacher_username"));
          request.setSubjectCode(rs.getString("subject_code"));
          request.setAcademicYear(rs.getString("academic_year"));
          request.setSemester(rs.getInt("semester"));
          request.setScheduleDay(rs.getString("schedule_day"));
          request.setScheduleTime(rs.getString("schedule_time"));
          request.setRoom(rs.getString("room"));
          request.setMaxStudents(rs.getInt("max_students"));
          request.setReason(rs.getString("reason"));
          String status = rs.getString("request_status");
          if (status != null) {
            request.setRequestStatus(ClassOpeningRequest.RequestStatus
                .valueOf(status.toUpperCase()));
          }
          request.setAdminNote(rs.getString("admin_note"));
          request.setRequestDate(rs.getTimestamp("request_date"));
          request.setDecisionDate(rs.getTimestamp("decision_date"));
          request.setCreatedAt(rs.getTimestamp("created_at"));
          request.setUpdatedAt(rs.getTimestamp("updated_at"));
          requests.add(request);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách yêu cầu mở lớp theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return requests;
  }

  private List<CourseRegistration> getCourseRegistrationsBySource(String source) {
    String sql = "SELECT cr.* FROM course_registrations cr " +
        "JOIN data_origin dor ON dor.entity_type = 'course_registration' AND dor.entity_id = cr.registration_id "
        +
        "WHERE dor.source = ?";

    List<CourseRegistration> registrations = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, source);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          CourseRegistration registration = new CourseRegistration();
          registration.setRegistrationId(rs.getInt("registration_id"));
          registration.setStudentCode(rs.getString("student_code"));
          registration.setCourseCode(rs.getString("course_code"));
          registration.setRegistrationDate(rs.getTimestamp("registration_date"));
          String status = rs.getString("registration_status");
          if (status != null) {
            registration
                .setRegistrationStatus(CourseRegistration.RegistrationStatus
                    .valueOf(status.toUpperCase()));
          }
          registration.setCancelDate(rs.getTimestamp("cancel_date"));
          registration.setNotes(rs.getString("notes"));
          registration.setCreatedAt(rs.getTimestamp("created_at"));
          registrations.add(registration);
        }
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách đăng ký khóa học theo nguồn '" + source + "': " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
    }

    return registrations;
  }
}
