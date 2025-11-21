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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến đồng bộ dữ liệu (sync / upload /
 * download).
 */
public class SyncHandler {
  private static final Logger LOGGER = Logger.getLogger(SyncHandler.class.getName());

  private final StudentService studentService;
  private final CourseService courseService;
  private final ClassOpeningRequestService classRequestService;
  private final NotificationService notificationService;
  private final DataOriginHelper dataOriginHelper;
  private final Supplier<String> clientSourceSupplier;
  private final Consumer<String> clientSourceUpdater;

  private User currentUser;

  public SyncHandler(StudentService studentService,
      CourseService courseService,
      ClassOpeningRequestService classRequestService,
      NotificationService notificationService,
      DataOriginHelper dataOriginHelper,
      Supplier<String> clientSourceSupplier,
      Consumer<String> clientSourceUpdater) {
    this.studentService = studentService;
    this.courseService = courseService;
    this.classRequestService = classRequestService;
    this.notificationService = notificationService;
    this.dataOriginHelper = dataOriginHelper;
    this.clientSourceSupplier = clientSourceSupplier;
    this.clientSourceUpdater = clientSourceUpdater;
  }

  public void updateCurrentUser(User user) {
    this.currentUser = user;
  }

  private String getClientSource() {
    String source = clientSourceSupplier.get();
    return source != null ? source : "UNKNOWN";
  }

  private void setClientSource(String source) {
    clientSourceUpdater.accept(source);
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
        setClientSource(clientDbType.trim().toUpperCase());
      }

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

      String currentSource = getClientSource();
      String clientSourceKey = currentSource.toLowerCase() + "_version";
      int clientSourceVersion = 0;
      if (serverMetadata.containsKey(clientSourceKey)) {
        Object versionObj = serverMetadata.get(clientSourceKey);
        if (versionObj instanceof Number) {
          clientSourceVersion = ((Number) versionObj).intValue();
        }
      }

      String syncAction;
      if (currentSource != null && !"REGULAR".equals(currentSource) && !"UNKNOWN".equals(currentSource)) {
        if (clientVersion == 0) {
          syncAction = "UPLOAD_TO_SERVER";
        } else if (clientSourceVersion > clientVersion) {
          syncAction = "DOWNLOAD_FROM_SERVER";
        } else if (clientVersion > clientSourceVersion) {
          syncAction = "UPLOAD_TO_SERVER";
        } else {
          syncAction = "NO_SYNC_NEEDED";
        }
      } else {
        syncAction = "UPLOAD_TO_SERVER";
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
      String source = getClientSource();
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
      String source = getClientSource();

      for (User u : users) {
        try {
          boolean facultyOk = true;
          if (u.getFacultyCode() != null && !u.getFacultyCode().trim().isEmpty()) {
            com.university.sms.model.Faculty existingFaculty = facultyDAO
                .findByCode(u.getFacultyCode().trim());
            if (existingFaculty == null) {
              facultyOk = false;
              LOGGER.warning(
                  "Faculty code not found: " + u.getFacultyCode() + " for user " + u.getUsername());
            }
          }

          User existing = userDAO.findByUsername(u.getUsername());
          if (existing == null) {
            u.setUserId(0);
            if (facultyOk && userDAO.addUser(u)) {
              dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning(
                  "Failed to save user: " + u.getUsername() +
                      (facultyOk ? " (check email/phone uniqueness)" : " (faculty_code not found)"));
            }
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
      String source = getClientSource();

      for (com.university.sms.model.Faculty f : faculties) {
        try {
          com.university.sms.model.Faculty existing = facultyDAO.findByCode(f.getFacultyCode());
          if (existing == null) {
            f.setFacultyId(0);
            if (facultyDAO.addFaculty(f)) {
              dataOriginHelper.saveDataOrigin("faculty", f.getFacultyId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning(
                  "Failed to save faculty: " + f.getFacultyCode() + " - " + f.getFacultyName());
            }
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
      String source = getClientSource();

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
              User u = new User();
              u.setUsername(c.getTeacherUsername());
              u.setPassword("password");
              u.setFullName(c.getTeacherUsername());
              u.setEmail(c.getTeacherUsername() + "@csv-teacher.edu.vn");
              u.setRole(User.UserRole.TEACHER);
              userOk = userDAO.addUser(u);
              if (userOk) {
                dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
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
      String source = getClientSource();

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
              return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
                  "Email không hợp lệ. Email phải có định dạng: example@domain.com");
            }
          }

          String normalizedStudentPhone = null;
          if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
            normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
            if (!isValidPhoneFormat(normalizedStudentPhone)) {
              return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
                  "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
            }
          }

          if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
            if (existingUserByEmail != null) {
              return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
                  "Email đã được sử dụng bởi user khác: " + student.getEmail());
            }
          }

          if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
            User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
            if (existingUserByPhone != null) {
              return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
                  "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
            }
          }

          User byUsername = userDAO.findByUsername(username);
          if (byUsername == null) {
            User u = new User();
            u.setUsername(username);
            u.setPassword("password");
            u.setFullName(student.getFullName());
            u.setEmail(student.getEmail());
            u.setPhone(normalizedStudentPhone);
            u.setAddress(student.getAddress());
            u.setRole(User.UserRole.STUDENT);
            userOk = userDAO.addUser(u);
            if (userOk) {
              dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
            }
          }

          boolean classOk = true;
          if (student.getClassCode() != null && !student.getClassCode().trim().isEmpty()) {
            com.university.sms.model.Class classObj = classDAO.findByCode(student.getClassCode());
            if (classObj == null) {
              classOk = false;
              LOGGER.warning("Class code not found: " + student.getClassCode());
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
      String source = getClientSource();

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
                  "Prerequisite subject code not found: " + s.getPrerequisiteSubjectCode());
            }
          }

          Subject existing = subjectDAO.findByCode(s.getSubjectCode());
          if (existing == null) {
            s.setSubjectId(0);
            if (facultyOk && prerequisiteOk && subjectDAO.save(s)) {
              dataOriginHelper.saveDataOrigin("subject", s.getSubjectId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning("Failed to save subject: " + s.getSubjectCode() + " - " + s.getSubjectName());
            }
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

      LOGGER.info("Uploading " + courses.size() + " courses from client");

      int successCount = 0;
      int failCount = 0;
      CourseDAO courseDAO = new CourseDAO();
      SubjectDAO subjectDAO = new SubjectDAO();
      ClassDAO classDAO = new ClassDAO();
      UserDAO userDAO = new UserDAO();
      String source = getClientSource();

      for (Course course : courses) {
        try {
          boolean subjectOk = true;
          if (course.getSubjectCode() != null && !course.getSubjectCode().isEmpty()) {
            Subject subject = subjectDAO.findByCode(course.getSubjectCode());
            if (subject == null) {
              subjectOk = false;
              LOGGER.warning(
                  "Subject code not found: " + course.getSubjectCode() + " for course " + course.getCourseCode());
            }
          }

          boolean classOk = true;
          if (course.getClassCode() != null && !course.getClassCode().isEmpty()) {
            com.university.sms.model.Class cls = classDAO.findByCode(course.getClassCode());
            if (cls == null) {
              classOk = false;
              LOGGER.warning(
                  "Class code not found: " + course.getClassCode() + " for course " + course.getCourseCode());
            }
          }

          boolean userOk = true;
          if (course.getTeacherUsername() != null && !course.getTeacherUsername().isEmpty()) {
            User existingUser = userDAO.findByUsername(course.getTeacherUsername());
            if (existingUser == null) {
              String facultyCode = null;
              if (course.getSubjectCode() != null && !course.getSubjectCode().isEmpty()) {
                Subject subject = subjectDAO.findByCode(course.getSubjectCode());
                if (subject != null) {
                  facultyCode = subject.getFacultyCode();
                }
              }

              User u = new User();
              u.setUsername(course.getTeacherUsername());
              u.setPassword("password");
              u.setFullName(course.getTeacherName() != null ? course.getTeacherName()
                  : course.getTeacherUsername());
              u.setEmail(course.getTeacherUsername() + "@csv-teacher.edu.vn");
              u.setRole(User.UserRole.TEACHER);
              u.setFacultyCode(facultyCode);
              userOk = userDAO.addUser(u);
              if (userOk) {
                dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
              }
            }
          }

          Course existing = courseDAO.findByCourseCode(course.getCourseCode());
          if (existing == null) {
            if (subjectOk && classOk && userOk && courseDAO.addCourse(course)) {
              if (course.getCourseId() > 0) {
                dataOriginHelper.saveDataOrigin("course", course.getCourseId(), source);
                successCount++;
              } else {
                failCount++;
                LOGGER.warning("Failed to save course: " + course.getCourseCode() + " - ID not set");
              }
            } else {
              failCount++;
              LOGGER.warning("Failed to save course: " + course.getCourseCode());
            }
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
      String source = getClientSource();

      for (Enrollment e : enrollments) {
        try {
          boolean studentOk = true;
          if (e.getStudentCode() != null && !e.getStudentCode().trim().isEmpty()) {
            Student student = studentDAO.findByStudentCode(e.getStudentCode().trim());
            if (student == null) {
              studentOk = false;
              LOGGER.warning("Student code not found: " + e.getStudentCode() + " for enrollment");
            }
          }

          boolean courseOk = true;
          if (e.getCourseCode() != null && !e.getCourseCode().trim().isEmpty()) {
            Course course = courseDAO.findByCourseCode(e.getCourseCode().trim());
            if (course == null) {
              courseOk = false;
              LOGGER.warning("Course code not found: " + e.getCourseCode() + " for enrollment");
            }
          }

          if (!studentOk || !courseOk) {
            failCount++;
            LOGGER.warning("Failed to save enrollment: studentCode=" + e.getStudentCode() +
                ", courseCode=" + e.getCourseCode() + " (FK validation failed)");
            continue;
          }

          boolean exists = false;
          try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement checkStmt = conn.prepareStatement(
                  "SELECT COUNT(*) FROM enrollments WHERE student_code = ? AND course_code = ?")) {
            checkStmt.setString(1, e.getStudentCode());
            checkStmt.setString(2, e.getCourseCode());
            try (ResultSet rs = checkStmt.executeQuery()) {
              if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
                LOGGER.info("Enrollment already exists, using existing: student="
                    + e.getStudentCode() + ", course=" + e.getCourseCode());
              }
            }
          } catch (Exception checkEx) {
            LOGGER.warning("Error checking enrollment duplicate: " + checkEx.getMessage());
          }

          if (!exists) {
            e.setEnrollmentId(0);
            boolean ok = enrollmentDAO.save(e);
            if (ok && e.getEnrollmentId() > 0) {
              dataOriginHelper.saveDataOrigin("enrollment", e.getEnrollmentId(), source);
              successCount++;
            } else {
              failCount++;
              LOGGER.warning("Failed to save enrollment: studentCode=" + e.getStudentCode() +
                  ", courseCode=" + e.getCourseCode());
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi lưu đăng ký học phần", ex);
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
      String source = getClientSource();

      for (Grade g : grades) {
        try {
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
      String source = getClientSource();

      for (ClassOpeningRequest r : requests) {
        try {
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
      String source = getClientSource();

      for (CourseRegistration r : registrations) {
        try {
          boolean exists = false;
          try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement checkStmt = conn.prepareStatement(
                  "SELECT COUNT(*) FROM course_registrations WHERE student_code = ? AND course_code = ?")) {
            checkStmt.setString(1, r.getStudentCode());
            checkStmt.setString(2, r.getCourseCode());
            try (ResultSet rs = checkStmt.executeQuery()) {
              if (rs.next() && rs.getInt(1) > 0) {
                exists = true;
                LOGGER.info("CourseRegistration already exists, skipping: student="
                    + r.getStudentCode() +
                    ", course=" + r.getCourseCode());
              }
            }
          } catch (Exception checkEx) {
            LOGGER.warning("Error checking course registration duplicate: " + checkEx.getMessage());
          }

          if (!exists) {
            r.setRegistrationId(0);
            if (dao.save(r)) {
              dataOriginHelper.saveDataOrigin("course_registration", r.getRegistrationId(), source);
              successCount++;
            } else {
              failCount++;
            }
          }
        } catch (Exception ex) {
          failCount++;
          LOGGER.log(Level.SEVERE, "Lỗi khi tải lên course registration", ex);
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
      String source = getClientSource();

      for (Notification notification : notifications) {
        try {
          String senderUsername = notification.getSenderUsername();
          boolean userOk = true;
          if (senderUsername != null && !senderUsername.isEmpty()) {
            User existingUser = userDAO.findByUsername(senderUsername);
            if (existingUser == null) {
              User u = new User();
              u.setUsername(senderUsername);
              u.setPassword("password");
              u.setFullName(notification.getSenderName() != null ? notification.getSenderName()
                  : senderUsername);
              u.setEmail(senderUsername + "@csv-admin.edu.vn");
              u.setRole(User.UserRole.ADMIN);
              userOk = userDAO.addUser(u);
              if (userOk) {
                dataOriginHelper.saveDataOrigin("user", u.getUserId(), source);
              }
            }
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
