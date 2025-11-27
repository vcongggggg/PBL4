package com.university.sms.postgresclient;

import com.university.sms.model.Class;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Faculty;
import com.university.sms.model.Grade;
import com.university.sms.model.Notification;
import com.university.sms.model.Student;
import com.university.sms.model.Subject;
import com.university.sms.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service để quản lý dữ liệu PostgreSQL local database
 * Version = timestamp (seconds since epoch) để so sánh với server
 */
public class PostgresDataService {
  private static final Logger LOGGER = Logger.getLogger(PostgresDataService.class.getName());

  private int version = 0; // Version = timestamp (seconds since epoch)
  private boolean skipVersionIncrement = false; // Flag để tạm thời disable version increment khi download

  private static final String VERSION_TABLE = "postgres_client_version";
  private static final String VERSION_KEY = "db_version";

  // Database connection config
  private static String DB_URL;
  private static String DB_USERNAME;
  private static String DB_PASSWORD;
  private static String DB_DRIVER;

  static {
    loadDatabaseConfig();
  }

  /**
   * Tải cấu hình database từ file properties
   */
  private static void loadDatabaseConfig() {
    Properties props = new Properties();
    try (InputStream input = PostgresDataService.class.getClassLoader()
        .getResourceAsStream("postgresql.properties")) {

      if (input == null) {
        LOGGER.warning("Không tìm thấy file postgresql.properties, sử dụng database.properties chung");
        // Fallback: sử dụng DatabaseConnection chung
        try {
          DB_DRIVER = "org.postgresql.Driver";
          java.lang.Class.forName(DB_DRIVER);
          LOGGER.info("Sử dụng DatabaseConnection chung (cần cấu hình PostgreSQL trong database.properties)");
        } catch (ClassNotFoundException e) {
          LOGGER.log(Level.SEVERE, "Error loading PostgreSQL driver", e);
          throw new RuntimeException("Failed to load PostgreSQL driver", e);
        }
        return;
      }

      props.load(input);

      DB_URL = props.getProperty("db.url");
      DB_USERNAME = props.getProperty("db.username");
      DB_PASSWORD = props.getProperty("db.password");
      DB_DRIVER = props.getProperty("db.driver", "org.postgresql.Driver");

      // Load PostgreSQL driver
      java.lang.Class.forName(DB_DRIVER);
      LOGGER.info("PostgreSQL database configuration loaded successfully");

    } catch (IOException | ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Error loading PostgreSQL database configuration", e);
      throw new RuntimeException("Failed to load PostgreSQL database configuration", e);
    }
  }

  /**
   * Tạo kết nối mới đến PostgreSQL database
   */
  private static Connection getConnection() throws SQLException {
    try {
      // Nếu có cấu hình riêng từ postgresql.properties, sử dụng
      if (DB_URL != null && DB_USERNAME != null) {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
      }
      // Ngược lại, sử dụng DatabaseConnection chung
      return com.university.sms.util.DatabaseConnection.getConnection();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to connect to PostgreSQL database", e);
      throw e;
    }
  }

  /**
   * Xóa toàn bộ dữ liệu cũ (dùng khi download từ server)
   * PostgreSQL client không có foreign key constraints, chỉ là nơi lưu trữ dữ
   * liệu đơn giản
   */
  public void truncateAllTables() {
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try (Statement stmt = conn.createStatement()) {
        // Xóa tất cả dữ liệu từ các bảng (không cần quan tâm thứ tự vì không có foreign
        // keys)
        stmt.executeUpdate("TRUNCATE TABLE course_registrations CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE class_opening_requests CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE notifications CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE grades CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE enrollments CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE students CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE courses CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE classes CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE subjects CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE users CASCADE");
        stmt.executeUpdate("TRUNCATE TABLE faculties CASCADE");
        conn.commit();
        LOGGER.info("Đã xóa toàn bộ dữ liệu cũ từ PostgreSQL database");
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi xóa dữ liệu cũ (có thể bảng trống): " + e.getMessage());
    }
  }

  public PostgresDataService() {
    initializeVersionTable();
    loadVersion();
  }

  /**
   * Khởi tạo bảng version nếu chưa tồn tại
   */
  private void initializeVersionTable() {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement()) {
      // Tạo bảng version nếu chưa có
      String sql = "CREATE TABLE IF NOT EXISTS " + VERSION_TABLE + " (" +
          "config_key VARCHAR(50) PRIMARY KEY, " +
          "config_value VARCHAR(255), " +
          "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
          ")";
      stmt.execute(sql);
      LOGGER.info("PostgreSQL version table initialized");
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi khởi tạo bảng version", e);
    }
  }

  // ==================== VERSION MANAGEMENT ====================

  /**
   * Lấy version hiện tại (timestamp in seconds since epoch)
   * Reload từ database để đảm bảo có version mới nhất
   */
  public int getVersion() {
    loadVersion(); // Reload từ database mỗi lần gọi để có version mới nhất
    return version;
  }

  /**
   * Set version (dùng khi download từ server)
   */
  public void setVersion(int version) {
    this.version = version;
    saveVersion();
  }

  /**
   * Tăng version khi có thay đổi
   * Version = current timestamp (seconds since epoch)
   */
  public void incrementVersion() {
    if (!skipVersionIncrement) {
      this.version = (int) (System.currentTimeMillis() / 1000);
      saveVersion();
    }
  }

  /**
   * Set flag để tạm thời disable version increment (dùng khi download từ server)
   */
  public void setSkipVersionIncrement(boolean skip) {
    this.skipVersionIncrement = skip;
  }

  /**
   * Đọc version từ database
   */
  private void loadVersion() {
    try (Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
            "SELECT config_value FROM " + VERSION_TABLE + " WHERE config_key = ?")) {
      pstmt.setString(1, VERSION_KEY);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          String value = rs.getString("config_value");
          if (value != null && !value.trim().isEmpty()) {
            try {
              version = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
              version = (int) (System.currentTimeMillis() / 1000);
              saveVersion();
            }
          } else {
            version = 0; // Version rỗng = chưa sync lần nào
          }
        } else {
          version = 0; // Chưa có version = chưa sync lần nào
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi đọc version", e);
      version = 0;
    }
  }

  /**
   * Lưu version vào database
   */
  private void saveVersion() {
    try (Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO " + VERSION_TABLE + " (config_key, config_value, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = CURRENT_TIMESTAMP")) {
      pstmt.setString(1, VERSION_KEY);
      pstmt.setString(2, String.valueOf(version));
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lưu version", e);
    }
  }

  /**
   * Lấy metadata của PostgreSQL local database
   */
  public Map<String, Object> getPostgresMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("database_type", "POSTGRES");
    metadata.put("db_version", version); // Version = timestamp (seconds since epoch)

    try {
      metadata.put("student_count", countRecords("students"));
      metadata.put("course_count", countRecords("courses"));
      metadata.put("enrollment_count", countRecords("enrollments"));
      metadata.put("faculty_count", countRecords("faculties"));
      metadata.put("class_count", countRecords("classes"));
      metadata.put("subject_count", countRecords("subjects"));
      metadata.put("user_count", countRecords("users"));
      metadata.put("grade_count", countRecords("grades"));
      metadata.put("notification_count", countRecords("notifications"));
      metadata.put("class_opening_request_count", countRecords("class_opening_requests"));
      metadata.put("course_registration_count", countRecords("course_registrations"));

      int totalRecords = (Integer) metadata.get("student_count") +
          (Integer) metadata.get("course_count") +
          (Integer) metadata.get("enrollment_count") +
          (Integer) metadata.get("faculty_count") +
          (Integer) metadata.get("class_count") +
          (Integer) metadata.get("subject_count");
      metadata.put("total_records", totalRecords);
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy metadata", e);
    }

    return metadata;
  }

  /**
   * Đếm số bản ghi trong bảng
   */
  private int countRecords(String tableName) throws SQLException {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + tableName)) {
      if (rs.next()) {
        return rs.getInt("count");
      }
    }
    return 0;
  }

  // ==================== DATA RETRIEVAL METHODS ====================

  /**
   * Lấy tất cả students từ local database
   */
  public List<Student> getAllStudents() {
    List<Student> students = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {
      while (rs.next()) {
        Student student = mapStudentFromResultSet(rs);
        if (student != null) {
          students.add(student);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách students", e);
    }
    return students;
  }

  /**
   * Lấy tất cả courses từ local database
   */
  public List<Course> getAllCourses() {
    List<Course> courses = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT c.*, s.subject_name, s.credits, u.full_name as teacher_name " +
                "FROM courses c " +
                "LEFT JOIN subjects s ON c.subject_code = s.subject_code " +
                "LEFT JOIN users u ON c.teacher_username = u.username")) {
      while (rs.next()) {
        Course course = mapCourseFromResultSet(rs);
        if (course != null) {
          courses.add(course);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách courses", e);
    }
    return courses;
  }

  /**
   * Lấy tất cả enrollments từ local database
   */
  public List<Enrollment> getAllEnrollments() {
    List<Enrollment> enrollments = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM enrollments")) {
      while (rs.next()) {
        Enrollment enrollment = mapEnrollmentFromResultSet(rs);
        if (enrollment != null) {
          enrollments.add(enrollment);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách enrollments", e);
    }
    return enrollments;
  }

  /**
   * Lấy tất cả faculties từ local database
   */
  public List<Faculty> getAllFaculties() {
    List<Faculty> faculties = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM faculties")) {
      while (rs.next()) {
        Faculty faculty = mapFacultyFromResultSet(rs);
        if (faculty != null) {
          faculties.add(faculty);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách faculties", e);
    }
    return faculties;
  }

  /**
   * Lấy tất cả classes từ local database
   */
  public List<Class> getAllClasses() {
    List<Class> classes = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM classes")) {
      while (rs.next()) {
        Class clazz = mapClassFromResultSet(rs);
        if (clazz != null) {
          classes.add(clazz);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách classes", e);
    }
    return classes;
  }

  /**
   * Lấy tất cả subjects từ local database
   */
  public List<Subject> getAllSubjects() {
    List<Subject> subjects = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM subjects")) {
      while (rs.next()) {
        Subject subject = mapSubjectFromResultSet(rs);
        if (subject != null) {
          subjects.add(subject);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách subjects", e);
    }
    return subjects;
  }

  /**
   * Lấy tất cả users từ local database
   */
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
      while (rs.next()) {
        User user = mapUserFromResultSet(rs);
        if (user != null) {
          users.add(user);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách users", e);
    }
    return users;
  }

  /**
   * Lấy tất cả grades từ local database
   */
  public List<Grade> getAllGrades() {
    List<Grade> grades = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM grades")) {
      while (rs.next()) {
        Grade grade = mapGradeFromResultSet(rs);
        if (grade != null) {
          grades.add(grade);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách grades", e);
    }
    return grades;
  }

  /**
   * Lấy tất cả notifications từ local database
   */
  public List<Notification> getAllNotifications() {
    List<Notification> notifications = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM notifications")) {
      while (rs.next()) {
        Notification notification = mapNotificationFromResultSet(rs);
        if (notification != null) {
          notifications.add(notification);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách notifications", e);
    }
    return notifications;
  }

  /**
   * Lấy tất cả class opening requests từ local database
   */
  public List<ClassOpeningRequest> getAllClassOpeningRequests() {
    List<ClassOpeningRequest> requests = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM class_opening_requests")) {
      while (rs.next()) {
        ClassOpeningRequest request = mapClassOpeningRequestFromResultSet(rs);
        if (request != null) {
          requests.add(request);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách class opening requests", e);
    }
    return requests;
  }

  /**
   * Lấy tất cả course registrations từ local database
   */
  public List<CourseRegistration> getAllCourseRegistrations() {
    List<CourseRegistration> registrations = new ArrayList<>();
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM course_registrations")) {
      while (rs.next()) {
        CourseRegistration registration = mapCourseRegistrationFromResultSet(rs);
        if (registration != null) {
          registrations.add(registration);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Lỗi khi lấy danh sách course registrations", e);
    }
    return registrations;
  }

  // ==================== MAPPING METHODS ====================

  private Student mapStudentFromResultSet(ResultSet rs) throws SQLException {
    Student student = new Student();
    student.setStudentId(rs.getInt("student_id"));
    student.setUsername(rs.getString("username"));
    student.setStudentCode(rs.getString("student_code"));
    student.setClassCode(rs.getString("class_code"));
    student.setFacultyCode(rs.getString("faculty_code"));
    student.setAdmissionYear(rs.getInt("admission_year"));
    String statusStr = rs.getString("student_status");
    if (statusStr != null) {
      student.setStudentStatus(Student.StudentStatus.valueOf(statusStr.toUpperCase()));
    }
    double gpaValue = rs.getDouble("gpa");
    if (!rs.wasNull()) {
      student.setGpa(java.math.BigDecimal.valueOf(gpaValue));
    }
    student.setTotalCredits(rs.getInt("total_credits"));
    Timestamp birthDate = rs.getTimestamp("birth_date");
    if (birthDate != null) {
      student.setBirthDate(new java.sql.Date(birthDate.getTime()));
    }
    String genderStr = rs.getString("gender");
    if (genderStr != null) {
      student.setGender(Student.Gender.valueOf(genderStr.toUpperCase()));
    }
    student.setCitizenId(rs.getString("citizen_id"));
    student.setEmergencyContact(rs.getString("emergency_contact"));
    student.setEmergencyPhone(rs.getString("emergency_phone"));
    return student;
  }

  private Course mapCourseFromResultSet(ResultSet rs) throws SQLException {
    Course course = new Course();
    course.setCourseId(rs.getInt("course_id"));
    course.setCourseCode(rs.getString("course_code"));
    course.setSubjectCode(rs.getString("subject_code"));
    course.setTeacherUsername(rs.getString("teacher_username"));
    course.setClassCode(rs.getString("class_code"));
    course.setAcademicYear(rs.getString("academic_year"));
    course.setSemester(rs.getInt("semester"));
    course.setScheduleDay(rs.getString("schedule_day"));
    course.setScheduleTime(rs.getString("schedule_time"));
    course.setRoom(rs.getString("room"));
    course.setMaxStudents(rs.getInt("max_students"));
    course.setCurrentStudents(rs.getInt("current_students"));
    String registrationStatus = rs.getString("registration_status");
    if (registrationStatus != null) {
      course.setRegistrationStatus(Course.RegistrationStatus.valueOf(registrationStatus.toUpperCase()));
    }
    String courseStatus = rs.getString("course_status");
    if (courseStatus != null) {
      course.setCourseStatus(Course.CourseStatus.valueOf(courseStatus.toUpperCase()));
    }
    Timestamp startDate = rs.getTimestamp("start_date");
    if (startDate != null) {
      course.setStartDate(new java.sql.Date(startDate.getTime()));
    }
    Timestamp endDate = rs.getTimestamp("end_date");
    if (endDate != null) {
      course.setEndDate(new java.sql.Date(endDate.getTime()));
    }
    // Set additional info if available
    try {
      course.setSubjectName(rs.getString("subject_name"));
      course.setCredits(rs.getInt("credits"));
      course.setTeacherName(rs.getString("teacher_name"));
    } catch (SQLException e) {
      // Ignore if columns don't exist
    }
    return course;
  }

  private Enrollment mapEnrollmentFromResultSet(ResultSet rs) throws SQLException {
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
    enrollment.setStudentCode(rs.getString("student_code"));
    enrollment.setCourseCode(rs.getString("course_code"));
    Timestamp enrollmentDate = rs.getTimestamp("enrollment_date");
    if (enrollmentDate != null) {
      enrollment.setEnrollmentDate(enrollmentDate);
    }
    String statusStr = rs.getString("enrollment_status");
    if (statusStr != null) {
      enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(statusStr.toUpperCase()));
    }
    double finalGradeValue = rs.getDouble("final_grade");
    if (!rs.wasNull()) {
      enrollment.setFinalGrade(java.math.BigDecimal.valueOf(finalGradeValue));
    }
    enrollment.setLetterGrade(rs.getString("letter_grade"));
    double gradePointsValue = rs.getDouble("grade_points");
    if (!rs.wasNull()) {
      enrollment.setGradePoints(java.math.BigDecimal.valueOf(gradePointsValue));
    }
    return enrollment;
  }

  private Faculty mapFacultyFromResultSet(ResultSet rs) throws SQLException {
    Faculty faculty = new Faculty();
    faculty.setFacultyId(rs.getInt("faculty_id"));
    faculty.setFacultyCode(rs.getString("faculty_code"));
    faculty.setFacultyName(rs.getString("faculty_name"));
    faculty.setDescription(rs.getString("description"));
    return faculty;
  }

  private Class mapClassFromResultSet(ResultSet rs) throws SQLException {
    Class clazz = new Class();
    clazz.setClassId(rs.getInt("class_id"));
    clazz.setClassCode(rs.getString("class_code"));
    clazz.setClassName(rs.getString("class_name"));
    clazz.setFacultyCode(rs.getString("faculty_code"));
    clazz.setTeacherUsername(rs.getString("teacher_username"));
    clazz.setAcademicYear(rs.getString("academic_year"));
    clazz.setSemester(rs.getInt("semester"));
    clazz.setMaxStudents(rs.getInt("max_students"));
    return clazz;
  }

  private Subject mapSubjectFromResultSet(ResultSet rs) throws SQLException {
    Subject subject = new Subject();
    subject.setSubjectId(rs.getInt("subject_id"));
    subject.setSubjectCode(rs.getString("subject_code"));
    subject.setSubjectName(rs.getString("subject_name"));
    subject.setCredits(rs.getInt("credits"));
    subject.setFacultyCode(rs.getString("faculty_code"));
    subject.setPrerequisiteSubjectCode(rs.getString("prerequisite_subject_code"));
    subject.setDescription(rs.getString("description"));
    subject.setRequired(rs.getBoolean("is_required"));
    return subject;
  }

  private User mapUserFromResultSet(ResultSet rs) throws SQLException {
    User user = new User();
    user.setUserId(rs.getInt("user_id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password"));
    user.setEmail(rs.getString("email"));
    user.setFullName(rs.getString("full_name"));
    String role = rs.getString("role");
    if (role != null) {
      user.setRole(User.UserRole.valueOf(role.toUpperCase()));
    }
    user.setPhone(rs.getString("phone"));
    user.setAddress(rs.getString("address"));
    user.setFacultyCode(rs.getString("faculty_code"));
    user.setActive(rs.getBoolean("is_active"));
    return user;
  }

  private Grade mapGradeFromResultSet(ResultSet rs) throws SQLException {
    Grade grade = new Grade();
    grade.setGradeId(rs.getInt("grade_id"));
    grade.setStudentCode(rs.getString("student_code"));
    grade.setCourseCode(rs.getString("course_code"));
    String gradeType = rs.getString("grade_type");
    if (gradeType != null) {
      grade.setGradeType(Grade.GradeType.valueOf(gradeType.toUpperCase()));
    }
    grade.setGradeName(rs.getString("grade_name"));
    double scoreValue = rs.getDouble("score");
    if (!rs.wasNull()) {
      grade.setScore(java.math.BigDecimal.valueOf(scoreValue));
    }
    double maxScoreValue = rs.getDouble("max_score");
    if (!rs.wasNull()) {
      grade.setMaxScore(java.math.BigDecimal.valueOf(maxScoreValue));
    }
    double weightValue = rs.getDouble("weight");
    if (!rs.wasNull()) {
      grade.setWeight(java.math.BigDecimal.valueOf(weightValue));
    }
    Timestamp gradeDate = rs.getTimestamp("grade_date");
    if (gradeDate != null) {
      grade.setGradeDate(new java.sql.Date(gradeDate.getTime()));
    }
    grade.setNotes(rs.getString("notes"));
    return grade;
  }

  private Notification mapNotificationFromResultSet(ResultSet rs) throws SQLException {
    Notification notification = new Notification();
    notification.setNotificationId(rs.getInt("notification_id"));
    notification.setTitle(rs.getString("title"));
    notification.setContent(rs.getString("content"));
    notification.setSenderUsername(rs.getString("sender_username"));
    String targetType = rs.getString("target_type");
    if (targetType != null) {
      notification.setTargetType(Notification.TargetType.valueOf(targetType.toUpperCase()));
    }
    notification.setTargetCode(rs.getString("target_code"));
    String priority = rs.getString("priority");
    if (priority != null) {
      notification.setPriority(Notification.Priority.valueOf(priority.toUpperCase()));
    }
    notification.setRead(rs.getBoolean("is_read"));
    Timestamp createdAt = rs.getTimestamp("created_at");
    if (createdAt != null) {
      notification.setCreatedAt(createdAt);
    }
    Timestamp expiresAt = rs.getTimestamp("expires_at");
    if (expiresAt != null) {
      notification.setExpiresAt(expiresAt);
    }
    return notification;
  }

  private ClassOpeningRequest mapClassOpeningRequestFromResultSet(ResultSet rs) throws SQLException {
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
    String requestStatus = rs.getString("request_status");
    if (requestStatus != null) {
      request.setRequestStatus(ClassOpeningRequest.RequestStatus.valueOf(requestStatus.toUpperCase()));
    }
    request.setAdminNote(rs.getString("admin_note"));
    request.setApprovedByUsername(rs.getString("approved_by_username"));
    request.setApprovedCourseCode(rs.getString("approved_course_code"));
    Timestamp requestDate = rs.getTimestamp("request_date");
    if (requestDate != null) {
      request.setRequestDate(requestDate);
    }
    Timestamp decisionDate = rs.getTimestamp("decision_date");
    if (decisionDate != null) {
      request.setDecisionDate(decisionDate);
    }
    return request;
  }

  private CourseRegistration mapCourseRegistrationFromResultSet(ResultSet rs) throws SQLException {
    CourseRegistration registration = new CourseRegistration();
    registration.setRegistrationId(rs.getInt("registration_id"));
    registration.setStudentCode(rs.getString("student_code"));
    registration.setCourseCode(rs.getString("course_code"));
    Timestamp registrationDate = rs.getTimestamp("registration_date");
    if (registrationDate != null) {
      registration.setRegistrationDate(registrationDate);
    }
    String regStatusStr = rs.getString("registration_status");
    if (regStatusStr != null) {
      registration.setRegistrationStatus(
          CourseRegistration.RegistrationStatus.valueOf(regStatusStr.toUpperCase()));
    }
    Timestamp cancelDate = rs.getTimestamp("cancel_date");
    if (cancelDate != null) {
      registration.setCancelDate(cancelDate);
    }
    registration.setNotes(rs.getString("notes"));
    return registration;
  }

  /**
   * Lưu danh sách entities vào local database (dùng cho download)
   * Logic giống CSVDataService: xóa dữ liệu cũ, insert dữ liệu mới, increment
   * version
   */
  public boolean saveStudents(List<Student> students) {
    if (students == null || students.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        // Chỉ cần insert dữ liệu mới
        // Insert dữ liệu mới
        String sql = "INSERT INTO students (student_id, username, student_code, class_code, faculty_code, " +
            "admission_year, student_status, gpa, total_credits, birth_date, gender, citizen_id, " +
            "emergency_contact, emergency_phone, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS student_status_type), ?, ?, ?, " +
            "CAST(? AS gender_type), ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Student s : students) {
            pstmt.setInt(1, s.getStudentId());
            pstmt.setString(2, s.getUsername());
            pstmt.setString(3, s.getStudentCode());
            pstmt.setString(4, s.getClassCode());
            pstmt.setString(5, s.getFacultyCode());
            pstmt.setInt(6, s.getAdmissionYear());
            pstmt.setString(7, s.getStudentStatus() != null ? s.getStudentStatus().name().toLowerCase() : "active");
            pstmt.setBigDecimal(8, s.getGpa());
            pstmt.setInt(9, s.getTotalCredits());
            pstmt.setDate(10, s.getBirthDate() != null ? new java.sql.Date(s.getBirthDate().getTime()) : null);
            // Xử lý NULL cho gender: CAST(NULL AS gender_type) sẽ trả về NULL
            if (s.getGender() != null) {
              pstmt.setString(11, s.getGender().name().toLowerCase());
            } else {
              pstmt.setNull(11, java.sql.Types.VARCHAR);
            }
            pstmt.setString(12, s.getCitizenId());
            pstmt.setString(13, s.getEmergencyContact());
            pstmt.setString(14, s.getEmergencyPhone());
            pstmt.setTimestamp(15, s.getCreatedAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + students.size() + " students vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu students", e);
      return false;
    }
  }

  public boolean saveCourses(List<Course> courses) {
    if (courses == null || courses.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO courses (course_id, course_code, subject_code, teacher_username, class_code, " +
            "academic_year, semester, schedule_day, schedule_time, room, max_students, current_students, " +
            "registration_status, course_status, start_date, end_date, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS registration_status_type), CAST(? AS course_status_type), ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Course c : courses) {
            pstmt.setInt(1, c.getCourseId());
            pstmt.setString(2, c.getCourseCode());
            pstmt.setString(3, c.getSubjectCode());
            pstmt.setString(4, c.getTeacherUsername());
            pstmt.setString(5, c.getClassCode());
            pstmt.setString(6, c.getAcademicYear());
            pstmt.setInt(7, c.getSemester());
            pstmt.setString(8, c.getScheduleDay());
            pstmt.setString(9, c.getScheduleTime());
            pstmt.setString(10, c.getRoom());
            pstmt.setInt(11, c.getMaxStudents());
            pstmt.setInt(12, c.getCurrentStudents());
            pstmt.setString(13,
                c.getRegistrationStatus() != null ? c.getRegistrationStatus().name().toLowerCase() : "closed");
            pstmt.setString(14, c.getCourseStatus() != null ? c.getCourseStatus().name().toLowerCase() : "upcoming");
            pstmt.setDate(15, c.getStartDate() != null ? new java.sql.Date(c.getStartDate().getTime()) : null);
            pstmt.setDate(16, c.getEndDate() != null ? new java.sql.Date(c.getEndDate().getTime()) : null);
            pstmt.setTimestamp(17, c.getCreatedAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + courses.size() + " courses vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu courses", e);
      return false;
    }
  }

  public boolean saveEnrollments(List<Enrollment> enrollments) {
    if (enrollments == null || enrollments.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO enrollments (enrollment_id, student_code, course_code, enrollment_date, " +
            "enrollment_status, final_grade, letter_grade, grade_points, created_at) " +
            "VALUES (?, ?, ?, ?, CAST(? AS enrollment_status_type), ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Enrollment e : enrollments) {
            pstmt.setInt(1, e.getEnrollmentId());
            pstmt.setString(2, e.getStudentCode());
            pstmt.setString(3, e.getCourseCode());
            pstmt.setTimestamp(4, e.getEnrollmentDate());
            pstmt.setString(5,
                e.getEnrollmentStatus() != null ? e.getEnrollmentStatus().name().toLowerCase() : "enrolled");
            pstmt.setBigDecimal(6, e.getFinalGrade());
            pstmt.setString(7, e.getLetterGrade());
            pstmt.setBigDecimal(8, e.getGradePoints());
            pstmt.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + enrollments.size() + " enrollments vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu enrollments", e);
      return false;
    }
  }

  public boolean saveFaculties(List<Faculty> faculties) {
    if (faculties == null || faculties.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO faculties (faculty_id, faculty_code, faculty_name, description, created_at) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Faculty f : faculties) {
            pstmt.setInt(1, f.getFacultyId());
            pstmt.setString(2, f.getFacultyCode());
            pstmt.setString(3, f.getFacultyName());
            pstmt.setString(4, f.getDescription());
            pstmt.setTimestamp(5, f.getCreatedAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + faculties.size() + " faculties vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu faculties", e);
      return false;
    }
  }

  public boolean saveClasses(List<Class> classes) {
    if (classes == null || classes.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO classes (class_id, class_code, class_name, faculty_code, teacher_username, " +
            "academic_year, semester, max_students, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Class c : classes) {
            pstmt.setInt(1, c.getClassId());
            pstmt.setString(2, c.getClassCode());
            pstmt.setString(3, c.getClassName());
            pstmt.setString(4, c.getFacultyCode());
            pstmt.setString(5, c.getTeacherUsername());
            pstmt.setString(6, c.getAcademicYear());
            pstmt.setInt(7, c.getSemester());
            pstmt.setInt(8, c.getMaxStudents());
            pstmt.setTimestamp(9, c.getCreatedAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + classes.size() + " classes vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu classes", e);
      return false;
    }
  }

  public boolean saveSubjects(List<Subject> subjects) {
    if (subjects == null || subjects.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO subjects (subject_id, subject_code, subject_name, credits, faculty_code, " +
            "prerequisite_subject_code, description, is_required, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Subject s : subjects) {
            pstmt.setInt(1, s.getSubjectId());
            pstmt.setString(2, s.getSubjectCode());
            pstmt.setString(3, s.getSubjectName());
            pstmt.setInt(4, s.getCredits());
            pstmt.setString(5, s.getFacultyCode());
            pstmt.setString(6, s.getPrerequisiteSubjectCode());
            pstmt.setString(7, s.getDescription());
            pstmt.setBoolean(8, s.isRequired());
            pstmt.setTimestamp(9, s.getCreatedAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + subjects.size() + " subjects vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu subjects", e);
      return false;
    }
  }

  public boolean saveUsers(List<User> users) {
    if (users == null || users.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO users (user_id, username, password, email, full_name, role, phone, address, " +
            "faculty_code, created_at, updated_at, is_active) " +
            "VALUES (?, ?, ?, ?, ?, CAST(? AS user_role), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (User u : users) {
            pstmt.setInt(1, u.getUserId());
            pstmt.setString(2, u.getUsername());
            pstmt.setString(3, u.getPassword());
            pstmt.setString(4, u.getEmail());
            pstmt.setString(5, u.getFullName());
            pstmt.setString(6, u.getRole() != null ? u.getRole().name().toLowerCase() : "student");
            pstmt.setString(7, u.getPhone());
            pstmt.setString(8, u.getAddress());
            pstmt.setString(9, u.getFacultyCode());
            pstmt.setTimestamp(10, u.getCreatedAt());
            pstmt.setTimestamp(11, u.getUpdatedAt());
            pstmt.setBoolean(12, u.isActive());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + users.size() + " users vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu users", e);
      return false;
    }
  }

  public boolean saveGrades(List<Grade> grades) {
    if (grades == null || grades.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO grades (grade_id, student_code, course_code, grade_type, grade_name, " +
            "score, max_score, weight, grade_date, notes, created_at, updated_at) " +
            "VALUES (?, ?, ?, CAST(? AS grade_type_enum), ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Grade g : grades) {
            pstmt.setInt(1, g.getGradeId());
            pstmt.setString(2, g.getStudentCode());
            pstmt.setString(3, g.getCourseCode());
            pstmt.setString(4, g.getGradeType() != null ? g.getGradeType().name().toLowerCase() : "assignment");
            pstmt.setString(5, g.getGradeName());
            pstmt.setBigDecimal(6, g.getScore());
            pstmt.setBigDecimal(7, g.getMaxScore());
            pstmt.setBigDecimal(8, g.getWeight());
            pstmt.setDate(9, g.getGradeDate() != null ? new java.sql.Date(g.getGradeDate().getTime()) : null);
            pstmt.setString(10, g.getNotes());
            pstmt.setTimestamp(11, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + grades.size() + " grades vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu grades", e);
      return false;
    }
  }

  public boolean saveNotifications(List<Notification> notifications) {
    if (notifications == null || notifications.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO notifications (notification_id, title, content, sender_username, target_type, " +
            "target_code, priority, is_read, created_at, expires_at) " +
            "VALUES (?, ?, ?, ?, CAST(? AS notification_target_type), ?, CAST(? AS notification_priority_type), ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (Notification n : notifications) {
            pstmt.setInt(1, n.getNotificationId());
            pstmt.setString(2, n.getTitle());
            pstmt.setString(3, n.getContent());
            pstmt.setString(4, n.getSenderUsername());
            pstmt.setString(5, n.getTargetType() != null ? n.getTargetType().name().toLowerCase() : "all");
            pstmt.setString(6, n.getTargetCode());
            pstmt.setString(7, n.getPriority() != null ? n.getPriority().name().toLowerCase() : "normal");
            pstmt.setBoolean(8, n.isRead());
            pstmt.setTimestamp(9, n.getCreatedAt());
            pstmt.setTimestamp(10, n.getExpiresAt());
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + notifications.size() + " notifications vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu notifications", e);
      return false;
    }
  }

  public boolean saveClassOpeningRequests(List<ClassOpeningRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO class_opening_requests (request_id, teacher_username, subject_code, " +
            "academic_year, semester, schedule_day, schedule_time, room, max_students, reason, " +
            "request_status, admin_note, approved_by_username, approved_course_code, request_date, " +
            "decision_date, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS request_status_type), ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (ClassOpeningRequest r : requests) {
            pstmt.setInt(1, r.getRequestId());
            pstmt.setString(2, r.getTeacherUsername());
            pstmt.setString(3, r.getSubjectCode());
            pstmt.setString(4, r.getAcademicYear());
            pstmt.setInt(5, r.getSemester());
            pstmt.setString(6, r.getScheduleDay());
            pstmt.setString(7, r.getScheduleTime());
            pstmt.setString(8, r.getRoom());
            pstmt.setInt(9, r.getMaxStudents());
            pstmt.setString(10, r.getReason());
            pstmt.setString(11, r.getRequestStatus() != null ? r.getRequestStatus().name().toLowerCase() : "pending");
            pstmt.setString(12, r.getAdminNote());
            pstmt.setString(13, r.getApprovedByUsername());
            pstmt.setString(14, r.getApprovedCourseCode());
            pstmt.setTimestamp(15, r.getRequestDate());
            pstmt.setTimestamp(16, r.getDecisionDate());
            pstmt.setTimestamp(17, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(18, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + requests.size() + " class opening requests vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu class opening requests", e);
      return false;
    }
  }

  public boolean saveCourseRegistrations(List<CourseRegistration> registrations) {
    if (registrations == null || registrations.isEmpty()) {
      return true; // Không có gì để lưu
    }
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Không cần xóa nữa vì đã xóa tất cả ở đầu (truncateAllTables)
        String sql = "INSERT INTO course_registrations (registration_id, student_code, course_code, " +
            "registration_date, registration_status, cancel_date, notes, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, CAST(? AS registration_status_reg_type), ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          for (CourseRegistration r : registrations) {
            pstmt.setInt(1, r.getRegistrationId());
            pstmt.setString(2, r.getStudentCode());
            pstmt.setString(3, r.getCourseCode());
            pstmt.setTimestamp(4, r.getRegistrationDate());
            pstmt.setString(5,
                r.getRegistrationStatus() != null ? r.getRegistrationStatus().name().toLowerCase() : "pending");
            pstmt.setTimestamp(6, r.getCancelDate());
            pstmt.setString(7, r.getNotes());
            pstmt.setTimestamp(8, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
        conn.commit();
        if (!skipVersionIncrement) {
          incrementVersion();
        }
        LOGGER.info("Đã lưu " + registrations.size() + " course registrations vào PostgreSQL");
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lưu course registrations", e);
      return false;
    }
  }
}
