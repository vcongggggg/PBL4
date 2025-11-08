package com.university.sms.csvclient;

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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service để quản lý dữ liệu CSV
 */
public class CSVDataService {
  private static final Logger LOGGER = Logger.getLogger(CSVDataService.class.getName());

  private static final String DATA_DIR = "data/csv";

  // ============================================
  // CSV FILES - Mirror complete database structure
  // CSV Client can work OFFLINE, sync to server when connected
  // ============================================

  // Core master data
  private static final String USERS_FILE = "users.csv";
  private static final String FACULTIES_FILE = "faculties.csv";
  private static final String CLASSES_FILE = "classes.csv";
  private static final String STUDENTS_FILE = "students.csv";
  private static final String SUBJECTS_FILE = "subjects.csv";

  // Transactional data
  private static final String COURSES_FILE = "courses.csv";
  private static final String ENROLLMENTS_FILE = "enrollments.csv";
  private static final String GRADES_FILE = "grades.csv";

  // Business workflow data
  private static final String CLASS_OPENING_REQUESTS_FILE = "class_opening_requests.csv";
  private static final String COURSE_REGISTRATIONS_FILE = "course_registrations.csv";

  // Communication data
  private static final String NOTIFICATIONS_FILE = "notifications.csv";

  // NOTE: System audit tables NOT included (server-only):
  // - login_history, data_origin, student_status_log
  // These are runtime/audit data managed by server

  private Path dataDir;
  private int dbVersion = 1; // Tăng version mỗi khi có thay đổi

  public CSVDataService() {
    this.dataDir = Paths.get(DATA_DIR);
    initializeDataDirectory();
    loadVersion(); // Load version khi khởi tạo
  }

  /**
   * Khởi tạo thư mục dữ liệu
   */
  private void initializeDataDirectory() {
    try {
      if (!Files.exists(dataDir)) {
        Files.createDirectories(dataDir);
        LOGGER.info("Created data directory: " + dataDir);
      }

      // Tạo file CSV mẫu nếu chưa tồn tại
      createSampleDataIfNotExists();

    } catch (IOException e) {
      LOGGER.severe("Error creating data directory: " + e.getMessage());
    }
  }

  /**
   * Tạo dữ liệu mẫu nếu file chưa tồn tại
   */
  private void createSampleDataIfNotExists() {
    try {
      // Core data files
      createEmptyFileIfNotExists(USERS_FILE,
          "userId,username,password,fullName,email,phone,address,role,isActive,createdAt");

      createEmptyFileIfNotExists(FACULTIES_FILE,
          "facultyId,facultyCode,facultyName,description,headTeacherUsername,createdAt");

      createEmptyFileIfNotExists(CLASSES_FILE,
          "classId,classCode,className,facultyCode,teacherUsername,academicYear,semester,maxStudents,createdAt");

      createEmptyFileIfNotExists(STUDENTS_FILE,
          "studentId,username,studentCode,classCode,facultyCode,admissionYear,studentStatus,gpa,totalCredits,birthDate,gender,citizenId,emergencyContact,emergencyPhone,createdAt,fullName,email,phone,address");

      createEmptyFileIfNotExists(SUBJECTS_FILE,
          "subjectId,subjectCode,subjectName,credits,facultyCode,prerequisiteSubjectCode,description,isRequired,createdAt");

      createEmptyFileIfNotExists(COURSES_FILE,
          "courseId,courseCode,subjectCode,teacherUsername,classCode,academicYear,semester,scheduleDay,scheduleTime,room,maxStudents,currentStudents,courseStatus,startDate,endDate,createdAt");

      createEmptyFileIfNotExists(ENROLLMENTS_FILE,
          "enrollmentId,studentCode,courseCode,enrollmentDate,enrollmentStatus,finalGrade,letterGrade,gradePoints");

      createEmptyFileIfNotExists(GRADES_FILE,
          "gradeId,studentCode,courseCode,gradeType,gradeName,score,maxScore,weight,gradeDate,notes,createdAt");

      createEmptyFileIfNotExists(CLASS_OPENING_REQUESTS_FILE,
          "requestId,teacherUsername,subjectCode,academicYear,semester,scheduleDay,scheduleTime,room,maxStudents,reason,requestStatus,adminNote,approvedByUsername,approvedCourseCode,requestDate,decisionDate,createdAt,updatedAt");

      createEmptyFileIfNotExists(COURSE_REGISTRATIONS_FILE,
          "registrationId,studentCode,courseCode,registrationDate,registrationStatus,cancelDate,notes,createdAt");

      createEmptyFileIfNotExists(NOTIFICATIONS_FILE,
          "notificationId,title,content,senderUsername,targetType,targetCode,priority,isRead,createdAt,expiresAt");

    } catch (IOException e) {
      LOGGER.severe("Error creating empty CSV files: " + e.getMessage());
    }
  }

  /**
   * Tạo file CSV trống nếu chưa tồn tại
   */
  private void createEmptyFileIfNotExists(String filename, String header) throws IOException {
    Path file = dataDir.resolve(filename);
    if (!Files.exists(file)) {
      createEmptyCSVFile(file, header);
      LOGGER.info("Created empty CSV file: " + filename);
    }
  }

  /**
   * Tạo file CSV trống với header
   */
  private void createEmptyCSVFile(Path file, String header) throws IOException {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(header);
    }
  }

  /**
   * Tạo file students.csv mẫu
   */
  private void createSampleStudentsFile(Path file) throws IOException {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println(
          "studentId,userId,studentCode,classId,facultyId,admissionYear,studentStatus,gpa,totalCredits,birthDate,gender,citizenId,emergencyContact,emergencyPhone,createdAt,fullName,email,phone,address");

      // Sample data
      writer.println(
          "1,1,SV001,1,1,2024,ACTIVE,3.5,120,2000-01-15,MALE,123456789,John Doe,0123456789,2024-01-01 00:00:00,Nguyễn Văn A,nguyenvana@email.com,0123456789,123 Đường ABC");
      writer.println(
          "2,2,SV002,1,1,2024,ACTIVE,3.2,115,2000-03-20,FEMALE,987654321,Jane Smith,0987654321,2024-01-01 00:00:00,Trần Thị B,tranthib@email.com,0987654321,456 Đường XYZ");
      writer.println(
          "3,3,SV003,2,2,2023,ACTIVE,3.8,130,1999-07-10,MALE,456789123,Bob Johnson,0456789123,2024-01-01 00:00:00,Lê Văn C,levanc@email.com,0456789123,789 Đường DEF");
    }
  }

  /**
   * Tạo file courses.csv mẫu
   */
  private void createSampleCoursesFile(Path file) throws IOException {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println(
          "courseId,courseCode,subjectId,teacherId,academicYear,semester,credits,maxStudents,currentStudents,courseStatus,createdAt");

      // Sample data
      writer.println("1,CS101,1,1,2024,1,3,50,25,ONGOING,2024-01-01 00:00:00");
      writer.println("2,CS102,1,2,2024,1,3,40,20,ONGOING,2024-01-01 00:00:00");
      writer.println("3,CS201,2,1,2024,2,4,45,22,ONGOING,2024-01-01 00:00:00");
    }
  }

  /**
   * Tạo file enrollments.csv mẫu
   */
  private void createSampleEnrollmentsFile(Path file) throws IOException {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println(
          "enrollmentId,studentId,courseId,enrollmentDate,enrollmentStatus,finalGrade,attendanceRate,createdAt");

      // Sample data
      writer.println("1,1,1,2024-01-15 00:00:00,ENROLLED,8.5,95.0,2024-01-15 00:00:00");
      writer.println("2,1,2,2024-01-15 00:00:00,ENROLLED,7.8,90.0,2024-01-15 00:00:00");
      writer.println("3,2,1,2024-01-15 00:00:00,ENROLLED,9.0,98.0,2024-01-15 00:00:00");
      writer.println("4,3,3,2024-01-15 00:00:00,ENROLLED,8.2,92.0,2024-01-15 00:00:00");
    }
  }

  /**
   * Tạo file users.csv mẫu
   */
  private void createSampleUsersFile(Path file) throws IOException {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println("userId,username,password,fullName,email,phone,address,role,isActive,createdAt");

      // Sample data (password is hashed with BCrypt)
      writer.println(
          "1,admin,$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi,Admin User,admin@email.com,0123456789,Admin Address,ADMIN,true,2024-01-01 00:00:00");
      writer.println(
          "2,teacher1,$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi,Giảng viên 1,teacher1@email.com,0987654321,Teacher Address,TEACHER,true,2024-01-01 00:00:00");
      writer.println(
          "3,student1,$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi,Sinh viên 1,student1@email.com,0456789123,Student Address,STUDENT,true,2024-01-01 00:00:00");
    }
  }

  /**
   * Đọc tất cả sinh viên từ CSV
   */
  public List<Student> getAllStudents() {
    List<Student> students = new ArrayList<>();
    Path file = dataDir.resolve(STUDENTS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Student student = parseStudentFromCSV(line);
        if (student != null) {
          students.add(student);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading students from CSV: " + e.getMessage());
    }

    return students;
  }

  /**
   * Đọc tất cả khóa học từ CSV
   */
  public List<Course> getAllCourses() {
    List<Course> courses = new ArrayList<>();
    Path file = dataDir.resolve(COURSES_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Course course = parseCourseFromCSV(line);
        if (course != null) {
          courses.add(course);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading courses from CSV: " + e.getMessage());
    }

    return courses;
  }

  /**
   * Đọc tất cả đăng ký từ CSV
   */
  public List<Enrollment> getAllEnrollments() {
    List<Enrollment> enrollments = new ArrayList<>();
    Path file = dataDir.resolve(ENROLLMENTS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      int lineNum = 1;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        Enrollment enrollment = parseEnrollmentFromCSV(line);
        if (enrollment != null) {
          enrollments.add(enrollment);
          LOGGER.info("DEBUG: Parsed enrollment #" + lineNum + " -> ID=" + enrollment.getEnrollmentId() +
              ", studentCode=" + enrollment.getStudentCode() + ", courseCode=" + enrollment.getCourseCode());
        } else {
          LOGGER.warning("DEBUG: Failed to parse enrollment line #" + lineNum + ": " + line);
        }
      }
      LOGGER.info("DEBUG: Total enrollments parsed from CSV: " + enrollments.size());
    } catch (IOException e) {
      LOGGER.severe("Error reading enrollments from CSV: " + e.getMessage());
    }

    return enrollments;
  }

  /**
   * Đọc tất cả người dùng từ CSV
   */
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    Path file = dataDir.resolve(USERS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        User user = parseUserFromCSV(line);
        if (user != null) {
          users.add(user);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading users from CSV: " + e.getMessage());
    }

    return users;
  }

  /**
   * Tìm kiếm sinh viên theo từ khóa
   */
  public List<Student> searchStudents(String keyword) {
    List<Student> allStudents = getAllStudents();
    List<Student> results = new ArrayList<>();

    String lowerKeyword = keyword.toLowerCase();
    for (Student student : allStudents) {
      if (student.getFullName() != null && student.getFullName().toLowerCase().contains(lowerKeyword) ||
          student.getStudentCode() != null && student.getStudentCode().toLowerCase().contains(lowerKeyword) ||
          student.getEmail() != null && student.getEmail().toLowerCase().contains(lowerKeyword)) {
        results.add(student);
      }
    }

    return results;
  }

  /**
   * Lưu sinh viên vào CSV
   */
  public boolean saveStudent(Student student) {
    List<Student> students = getAllStudents();

    // Tìm và cập nhật sinh viên hiện có hoặc thêm mới
    boolean found = false;
    for (int i = 0; i < students.size(); i++) {
      if (students.get(i).getStudentId() == student.getStudentId()) {
        students.set(i, student);
        found = true;
        break;
      }
    }

    if (!found) {
      // Tạo ID mới nếu là sinh viên mới
      if (student.getStudentId() == 0) {
        int maxId = students.stream().mapToInt(Student::getStudentId).max().orElse(0);
        student.setStudentId(maxId + 1);
      }
      students.add(student);
    }

    boolean result = writeStudentsToCSV(students);
    if (result) {
      incrementVersion(); // Tăng version khi có thay đổi
    }
    return result;
  }

  /**
   * Xóa sinh viên khỏi CSV
   */
  public boolean deleteStudent(int studentId) {
    List<Student> students = getAllStudents();
    students.removeIf(student -> student.getStudentId() == studentId);
    boolean result = writeStudentsToCSV(students);
    if (result) {
      incrementVersion(); // Tăng version khi có thay đổi
    }
    return result;
  }

  /**
   * Parse sinh viên từ dòng CSV
   */
  private Student parseStudentFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 19)
        return null;

      Student student = new Student();
      student.setStudentId(Integer.parseInt(fields[0].trim()));
      student.setUsername(fields[1].trim());
      student.setStudentCode(fields[2].trim());
      student.setClassCode(fields[3].trim().isEmpty() ? null : fields[3].trim());
      student.setFacultyCode(fields[4].trim());
      student.setAdmissionYear(Integer.parseInt(fields[5].trim()));
      student.setStudentStatus(Student.StudentStatus.valueOf(fields[6].trim().toUpperCase()));
      student.setGpa(new java.math.BigDecimal(fields[7].trim()));
      student.setTotalCredits(Integer.parseInt(fields[8].trim()));
      student.setBirthDate(java.sql.Date.valueOf(fields[9].trim()));
      student.setGender(Student.Gender.valueOf(fields[10].trim().toUpperCase()));
      student.setCitizenId(fields[11].trim());
      student.setEmergencyContact(fields[12].trim());
      student.setEmergencyPhone(fields[13].trim());
      student.setCreatedAt(java.sql.Timestamp.valueOf(fields[14].trim()));
      student.setFullName(fields[15].trim());
      student.setEmail(fields[16].trim());
      student.setPhone(fields[17].trim());
      student.setAddress(fields[18].trim());

      return student;
    } catch (Exception e) {
      LOGGER.warning("Error parsing student from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Parse khóa học từ dòng CSV
   */
  private Course parseCourseFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 16)
        return null;

      Course course = new Course();
      course.setCourseId(Integer.parseInt(fields[0].trim()));
      course.setCourseCode(fields[1].trim());
      course.setSubjectCode(fields[2].trim());
      course.setTeacherUsername(fields[3].trim());
      course.setClassCode(fields[4].trim().isEmpty() ? null : fields[4].trim());
      course.setAcademicYear(fields[5].trim());
      course.setSemester(Integer.parseInt(fields[6].trim()));
      course.setScheduleDay(fields[7].trim().isEmpty() ? null : fields[7].trim());
      course.setScheduleTime(fields[8].trim().isEmpty() ? null : fields[8].trim());
      course.setRoom(fields[9].trim().isEmpty() ? null : fields[9].trim());
      course.setMaxStudents(Integer.parseInt(fields[10].trim()));
      course.setCurrentStudents(Integer.parseInt(fields[11].trim()));
      course.setCourseStatus(Course.CourseStatus.valueOf(fields[12].trim().toUpperCase()));
      course.setStartDate(fields[13].trim().isEmpty() ? null : java.sql.Date.valueOf(fields[13].trim()));
      course.setEndDate(fields[14].trim().isEmpty() ? null : java.sql.Date.valueOf(fields[14].trim()));
      course.setCreatedAt(java.sql.Timestamp.valueOf(fields[15].trim()));

      return course;
    } catch (Exception e) {
      LOGGER.warning("Error parsing course from CSV line: " + line + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parse đăng ký từ dòng CSV
   */
  private Enrollment parseEnrollmentFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 8)
        return null;

      Enrollment enrollment = new Enrollment();
      enrollment.setEnrollmentId(Integer.parseInt(fields[0].trim()));
      enrollment.setStudentCode(fields[1].trim());
      enrollment.setCourseCode(fields[2].trim());
      enrollment.setEnrollmentDate(fields[3].trim().isEmpty() ? null : java.sql.Timestamp.valueOf(fields[3].trim()));
      enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(fields[4].trim().toUpperCase()));
      enrollment.setFinalGrade(fields[5].trim().isEmpty() ? null : new java.math.BigDecimal(fields[5].trim()));
      enrollment.setLetterGrade(fields[6].trim().isEmpty() ? null : fields[6].trim());
      enrollment.setGradePoints(
          fields[7].trim().isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(fields[7].trim()));

      return enrollment;
    } catch (Exception e) {
      LOGGER.warning("Error parsing enrollment from CSV line: " + line + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parse người dùng từ dòng CSV
   */
  private User parseUserFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 10)
        return null;

      User user = new User();
      user.setUserId(Integer.parseInt(fields[0].trim()));
      user.setUsername(fields[1].trim());
      user.setPassword(fields[2].trim());
      user.setFullName(fields[3].trim());
      user.setEmail(fields[4].trim());
      user.setPhone(fields[5].trim());
      user.setAddress(fields[6].trim());
      user.setRole(User.UserRole.valueOf(fields[7].trim().toUpperCase()));
      user.setActive(Boolean.parseBoolean(fields[8].trim()));
      user.setCreatedAt(java.sql.Timestamp.valueOf(fields[9].trim()));

      return user;
    } catch (Exception e) {
      LOGGER.warning("Error parsing user from CSV line: " + line + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Ghi danh sách sinh viên vào CSV
   */
  private boolean writeStudentsToCSV(List<Student> students) {
    Path file = dataDir.resolve(STUDENTS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "studentId,username,studentCode,classCode,facultyCode,admissionYear,studentStatus,gpa,totalCredits,birthDate,gender,citizenId,emergencyContact,emergencyPhone,createdAt,fullName,email,phone,address");

      for (Student student : students) {
        writer.println(String.format("%d,%s,%s,%s,%s,%d,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            student.getStudentId(),
            student.getUsername() != null ? student.getUsername() : "",
            student.getStudentCode(),
            student.getClassCode() != null ? student.getClassCode() : "",
            student.getFacultyCode(),
            student.getAdmissionYear(),
            student.getStudentStatus(),
            student.getGpa(),
            student.getTotalCredits(),
            student.getBirthDate(),
            student.getGender(),
            student.getCitizenId(),
            student.getEmergencyContact(),
            student.getEmergencyPhone(),
            student.getCreatedAt(),
            student.getFullName(),
            student.getEmail(),
            student.getPhone(),
            student.getAddress()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing students to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Lưu user vào CSV
   */
  public boolean saveUser(User user) {
    List<User> users = getAllUsers();

    // Tìm và cập nhật user hiện có hoặc thêm mới
    boolean found = false;
    for (int i = 0; i < users.size(); i++) {
      if (users.get(i).getUserId() == user.getUserId()) {
        users.set(i, user);
        found = true;
        break;
      }
    }

    if (!found) {
      // Tạo ID mới nếu là user mới
      if (user.getUserId() == 0) {
        int maxId = users.stream().mapToInt(User::getUserId).max().orElse(0);
        user.setUserId(maxId + 1);
      }
      users.add(user);
    }

    return writeUsersToCSV(users);
  }

  /**
   * Ghi danh sách users vào CSV
   */
  private boolean writeUsersToCSV(List<User> users) {
    Path file = dataDir.resolve(USERS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println("userId,username,password,fullName,email,phone,address,role,isActive,createdAt");

      // Data
      for (User user : users) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            user.getUserId(),
            user.getUsername(),
            user.getPassword(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getAddress(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing users to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Lưu course vào CSV
   */
  public boolean saveCourse(Course course) {
    List<Course> courses = getAllCourses();

    // Tìm và cập nhật course hiện có hoặc thêm mới
    boolean found = false;
    for (int i = 0; i < courses.size(); i++) {
      if (courses.get(i).getCourseId() == course.getCourseId()) {
        courses.set(i, course);
        found = true;
        break;
      }
    }

    if (!found) {
      // Tạo ID mới nếu là course mới
      if (course.getCourseId() == 0) {
        int maxId = courses.stream().mapToInt(Course::getCourseId).max().orElse(0);
        course.setCourseId(maxId + 1);
      }
      courses.add(course);
    }

    boolean result = writeCoursesToCSV(courses);
    if (result) {
      incrementVersion(); // Tăng version khi có thay đổi
    }
    return result;
  }

  /**
   * Ghi danh sách courses vào CSV
   */
  private boolean writeCoursesToCSV(List<Course> courses) {
    Path file = dataDir.resolve(COURSES_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "courseId,courseCode,subjectCode,teacherUsername,classCode,academicYear,semester,scheduleDay,scheduleTime,room,maxStudents,currentStudents,courseStatus,startDate,endDate,createdAt");

      for (Course course : courses) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%d,%s,%s,%s,%d,%d,%s,%s,%s,%s",
            course.getCourseId(),
            course.getCourseCode(),
            course.getSubjectCode(),
            course.getTeacherUsername(),
            course.getClassCode() != null ? course.getClassCode() : "",
            course.getAcademicYear(),
            course.getSemester(),
            course.getScheduleDay() != null ? course.getScheduleDay() : "",
            course.getScheduleTime() != null ? course.getScheduleTime() : "",
            course.getRoom() != null ? course.getRoom() : "",
            course.getMaxStudents(),
            course.getCurrentStudents(),
            course.getCourseStatus(),
            course.getStartDate() != null ? course.getStartDate() : "",
            course.getEndDate() != null ? course.getEndDate() : "",
            course.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing courses to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Lưu enrollment vào CSV
   */
  public boolean saveEnrollment(Enrollment enrollment) {
    List<Enrollment> enrollments = getAllEnrollments();

    // Tìm và cập nhật enrollment hiện có hoặc thêm mới
    boolean found = false;
    for (int i = 0; i < enrollments.size(); i++) {
      if (enrollments.get(i).getEnrollmentId() == enrollment.getEnrollmentId()) {
        enrollments.set(i, enrollment);
        found = true;
        break;
      }
    }

    if (!found) {
      // Tạo ID mới nếu là enrollment mới
      if (enrollment.getEnrollmentId() == 0) {
        int maxId = enrollments.stream().mapToInt(Enrollment::getEnrollmentId).max().orElse(0);
        enrollment.setEnrollmentId(maxId + 1);
      }
      enrollments.add(enrollment);
    }

    boolean result = writeEnrollmentsToCSV(enrollments);
    if (result) {
      incrementVersion(); // Tăng version khi có thay đổi
    }
    return result;
  }

  /**
   * Ghi danh sách enrollments vào CSV
   */
  private boolean writeEnrollmentsToCSV(List<Enrollment> enrollments) {
    Path file = dataDir.resolve(ENROLLMENTS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "enrollmentId,studentCode,courseCode,enrollmentDate,enrollmentStatus,finalGrade,letterGrade,gradePoints");

      for (Enrollment enrollment : enrollments) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s",
            enrollment.getEnrollmentId(),
            enrollment.getStudentCode(),
            enrollment.getCourseCode(),
            enrollment.getEnrollmentDate(),
            enrollment.getEnrollmentStatus(),
            enrollment.getFinalGrade() != null ? enrollment.getFinalGrade() : "",
            enrollment.getLetterGrade() != null ? enrollment.getLetterGrade() : "",
            enrollment.getGradePoints() != null ? enrollment.getGradePoints() : ""));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing enrollments to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Lấy đường dẫn thư mục dữ liệu
   */
  public Path getDataDirectory() {
    return dataDir;
  }

  /**
   * Lấy metadata của CSV local
   */
  public Map<String, Object> getCSVMetadata() {
    Map<String, Object> metadata = new HashMap<>();

    List<Student> students = getAllStudents();
    List<Course> courses = getAllCourses();
    List<Enrollment> enrollments = getAllEnrollments();

    // Identify this client/source type for server-side provenance tagging
    metadata.put("database_type", "CSV");
    metadata.put("db_version", dbVersion);
    metadata.put("student_count", students.size());
    metadata.put("course_count", courses.size());
    metadata.put("enrollment_count", enrollments.size());
    metadata.put("total_records", students.size() + courses.size() + enrollments.size());

    return metadata;
  }

  /**
   * Tăng version khi có thay đổi
   */
  public void incrementVersion() {
    this.dbVersion++;
    saveVersionToFile();
  }

  /**
   * Lưu version vào file
   */
  private void saveVersionToFile() {
    try {
      Path versionFile = dataDir.resolve(".version");
      Files.write(versionFile, String.valueOf(dbVersion).getBytes());
      LOGGER.info("Saved version to file: " + dbVersion);
    } catch (IOException e) {
      LOGGER.warning("Could not save version file: " + e.getMessage());
    }
  }

  /**
   * Đọc version từ file
   */
  private void loadVersion() {
    try {
      Path versionFile = dataDir.resolve(".version");
      if (Files.exists(versionFile)) {
        String content = Files.readString(versionFile).trim();
        dbVersion = Integer.parseInt(content);
        LOGGER.info("Loaded version from file: " + dbVersion);
      } else {
        // Tạo file version mới
        dbVersion = 1;
        saveVersionToFile();
      }
    } catch (IOException e) {
      LOGGER.warning("Could not load version file: " + e.getMessage());
      dbVersion = 1;
    }
  }

  /**
   * Set version (dùng khi sync từ server)
   */
  public void setVersion(int version) {
    this.dbVersion = version;
    saveVersionToFile();
  }

  /**
   * Get current version
   */
  public int getVersion() {
    return dbVersion;
  }

  // ==================== FACULTIES METHODS ====================

  /**
   * Đọc tất cả faculties từ CSV
   */
  public List<Faculty> getAllFaculties() {
    List<Faculty> faculties = new ArrayList<>();
    Path file = dataDir.resolve(FACULTIES_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Faculty faculty = parseFacultyFromCSV(line);
        if (faculty != null) {
          faculties.add(faculty);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading faculties from CSV: " + e.getMessage());
    }

    return faculties;
  }

  /**
   * Parse faculty từ dòng CSV
   */
  private Faculty parseFacultyFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 6)
        return null;

      Faculty faculty = new Faculty();
      faculty.setFacultyId(Integer.parseInt(fields[0].trim()));
      faculty.setFacultyCode(fields[1].trim());
      faculty.setFacultyName(fields[2].trim());
      faculty.setDescription(fields[3].trim().isEmpty() ? null : fields[3].trim());
      faculty.setHeadTeacherUsername(fields[4].trim().isEmpty() ? null : fields[4].trim());
      faculty.setCreatedAt(java.sql.Timestamp.valueOf(fields[5].trim()));

      return faculty;
    } catch (Exception e) {
      LOGGER.warning("Error parsing faculty from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Lưu faculty vào CSV
   */
  public boolean saveFaculty(Faculty faculty) {
    List<Faculty> faculties = getAllFaculties();

    boolean found = false;
    for (int i = 0; i < faculties.size(); i++) {
      if (faculties.get(i).getFacultyId() == faculty.getFacultyId()) {
        faculties.set(i, faculty);
        found = true;
        break;
      }
    }

    if (!found) {
      if (faculty.getFacultyId() == 0) {
        int maxId = faculties.stream().mapToInt(Faculty::getFacultyId).max().orElse(0);
        faculty.setFacultyId(maxId + 1);
      }
      faculties.add(faculty);
    }

    boolean result = writeFacultiesToCSV(faculties);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách faculties vào CSV
   */
  private boolean writeFacultiesToCSV(List<Faculty> faculties) {
    Path file = dataDir.resolve(FACULTIES_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println("facultyId,facultyCode,facultyName,description,headTeacherUsername,createdAt");

      for (Faculty f : faculties) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s",
            f.getFacultyId(),
            f.getFacultyCode(),
            f.getFacultyName(),
            f.getDescription() != null ? f.getDescription() : "",
            f.getHeadTeacherUsername() != null ? f.getHeadTeacherUsername() : "",
            f.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing faculties to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa faculty khỏi CSV
   */
  public boolean deleteFaculty(int facultyId) {
    List<Faculty> faculties = getAllFaculties();
    faculties.removeIf(f -> f.getFacultyId() == facultyId);
    boolean result = writeFacultiesToCSV(faculties);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== SUBJECTS METHODS ====================

  /**
   * Đọc tất cả subjects từ CSV
   */
  public List<Subject> getAllSubjects() {
    List<Subject> subjects = new ArrayList<>();
    Path file = dataDir.resolve(SUBJECTS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Subject subject = parseSubjectFromCSV(line);
        if (subject != null) {
          subjects.add(subject);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading subjects from CSV: " + e.getMessage());
    }

    return subjects;
  }

  /**
   * Parse subject từ dòng CSV
   */
  private Subject parseSubjectFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 9)
        return null;

      Subject subject = new Subject();
      subject.setSubjectId(Integer.parseInt(fields[0].trim()));
      subject.setSubjectCode(fields[1].trim());
      subject.setSubjectName(fields[2].trim());
      subject.setCredits(Integer.parseInt(fields[3].trim()));
      subject.setFacultyCode(fields[4].trim());
      subject.setPrerequisiteSubjectCode(fields[5].trim().isEmpty() ? null : fields[5].trim());
      subject.setDescription(fields[6].trim().isEmpty() ? null : fields[6].trim());
      subject.setRequired(Boolean.parseBoolean(fields[7].trim()));
      subject.setCreatedAt(java.sql.Timestamp.valueOf(fields[8].trim()));

      return subject;
    } catch (Exception e) {
      LOGGER.warning("Error parsing subject from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Lưu subject vào CSV
   */
  public boolean saveSubject(Subject subject) {
    List<Subject> subjects = getAllSubjects();

    boolean found = false;
    for (int i = 0; i < subjects.size(); i++) {
      if (subjects.get(i).getSubjectId() == subject.getSubjectId()) {
        subjects.set(i, subject);
        found = true;
        break;
      }
    }

    if (!found) {
      if (subject.getSubjectId() == 0) {
        int maxId = subjects.stream().mapToInt(Subject::getSubjectId).max().orElse(0);
        subject.setSubjectId(maxId + 1);
      }
      subjects.add(subject);
    }

    boolean result = writeSubjectsToCSV(subjects);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách subjects vào CSV
   */
  private boolean writeSubjectsToCSV(List<Subject> subjects) {
    Path file = dataDir.resolve(SUBJECTS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "subjectId,subjectCode,subjectName,credits,facultyCode,prerequisiteSubjectCode,description,isRequired,createdAt");

      for (Subject s : subjects) {
        writer.println(String.format("%d,%s,%s,%d,%s,%s,%s,%s,%s",
            s.getSubjectId(),
            s.getSubjectCode(),
            s.getSubjectName(),
            s.getCredits(),
            s.getFacultyCode(),
            s.getPrerequisiteSubjectCode() != null ? s.getPrerequisiteSubjectCode() : "",
            s.getDescription() != null ? s.getDescription() : "",
            s.isRequired(),
            s.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing subjects to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa subject khỏi CSV
   */
  public boolean deleteSubject(int subjectId) {
    List<Subject> subjects = getAllSubjects();
    subjects.removeIf(s -> s.getSubjectId() == subjectId);
    boolean result = writeSubjectsToCSV(subjects);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== CLASSES METHODS ====================

  /**
   * Đọc tất cả classes từ CSV
   */
  public List<com.university.sms.model.Class> getAllClasses() {
    List<com.university.sms.model.Class> classes = new ArrayList<>();
    Path file = dataDir.resolve(CLASSES_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      int lineNum = 1;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        if (line.trim().isEmpty()) {
          continue; // Skip empty lines
        }
        com.university.sms.model.Class clazz = parseClassFromCSV(line);
        if (clazz != null) {
          classes.add(clazz);
        } else {
          LOGGER.warning("Failed to parse class from CSV line #" + lineNum + ": " + line);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading classes from CSV: " + e.getMessage());
    }

    return classes;
  }

  /**
   * Parse class từ dòng CSV
   */
  private com.university.sms.model.Class parseClassFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 9)
        return null;

      com.university.sms.model.Class clazz = new com.university.sms.model.Class();
      clazz.setClassId(Integer.parseInt(fields[0].trim()));
      clazz.setClassCode(fields[1].trim());
      clazz.setClassName(fields[2].trim());
      clazz.setFacultyCode(fields[3].trim());
      clazz.setTeacherUsername(fields[4].trim().isEmpty() ? null : fields[4].trim());
      clazz.setAcademicYear(fields[5].trim());
      clazz.setSemester(Integer.parseInt(fields[6].trim()));
      clazz.setMaxStudents(fields[7].trim().isEmpty() ? null : Integer.parseInt(fields[7].trim()));
      clazz.setCreatedAt(java.sql.Timestamp.valueOf(fields[8].trim()));

      return clazz;
    } catch (Exception e) {
      LOGGER.warning("Error parsing class from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Lưu class vào CSV
   */
  public boolean saveClass(com.university.sms.model.Class clazz) {
    List<com.university.sms.model.Class> classes = getAllClasses();

    boolean found = false;
    for (int i = 0; i < classes.size(); i++) {
      if (classes.get(i).getClassId() == clazz.getClassId()) {
        classes.set(i, clazz);
        found = true;
        break;
      }
    }

    if (!found) {
      if (clazz.getClassId() == 0) {
        int maxId = classes.stream().mapToInt(com.university.sms.model.Class::getClassId).max().orElse(0);
        clazz.setClassId(maxId + 1);
      }
      classes.add(clazz);
    }

    boolean result = writeClassesToCSV(classes);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách classes vào CSV
   */
  private boolean writeClassesToCSV(List<com.university.sms.model.Class> classes) {
    Path file = dataDir.resolve(CLASSES_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "classId,classCode,className,facultyCode,teacherUsername,academicYear,semester,maxStudents,createdAt");

      for (com.university.sms.model.Class c : classes) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%d,%s,%s",
            c.getClassId(),
            c.getClassCode(),
            c.getClassName(),
            c.getFacultyCode(),
            c.getTeacherUsername() != null ? c.getTeacherUsername() : "",
            c.getAcademicYear(),
            c.getSemester(),
            c.getMaxStudents() != null ? c.getMaxStudents() : "",
            c.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing classes to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa class khỏi CSV
   */
  public boolean deleteClass(int classId) {
    List<com.university.sms.model.Class> classes = getAllClasses();
    classes.removeIf(c -> c.getClassId() == classId);
    boolean result = writeClassesToCSV(classes);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== GRADES METHODS ====================

  /**
   * Đọc tất cả grades từ CSV
   */
  public List<Grade> getAllGrades() {
    List<Grade> grades = new ArrayList<>();
    Path file = dataDir.resolve(GRADES_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Grade grade = parseGradeFromCSV(line);
        if (grade != null) {
          grades.add(grade);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading grades from CSV: " + e.getMessage());
    }

    return grades;
  }

  /**
   * Parse grade từ dòng CSV
   */
  private Grade parseGradeFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 11)
        return null;

      Grade grade = new Grade();
      grade.setGradeId(Integer.parseInt(fields[0].trim()));
      grade.setStudentCode(fields[1].trim());
      grade.setCourseCode(fields[2].trim());
      grade.setGradeType(Grade.GradeType.valueOf(fields[3].trim().toUpperCase()));
      grade.setGradeName(fields[4].trim().isEmpty() ? null : fields[4].trim());
      grade.setScore(fields[5].trim().isEmpty() ? null : new java.math.BigDecimal(fields[5].trim()));
      grade.setMaxScore(fields[6].trim().isEmpty() ? null : new java.math.BigDecimal(fields[6].trim()));
      grade.setWeight(fields[7].trim().isEmpty() ? null : new java.math.BigDecimal(fields[7].trim()));
      grade.setGradeDate(fields[8].trim().isEmpty() ? null : java.sql.Date.valueOf(fields[8].trim()));
      grade.setNotes(fields[9].trim().isEmpty() ? null : fields[9].trim());
      grade.setCreatedAt(java.sql.Timestamp.valueOf(fields[10].trim()));

      return grade;
    } catch (Exception e) {
      LOGGER.warning("Error parsing grade from CSV line: " + line + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Lưu grade vào CSV
   */
  public boolean saveGrade(Grade grade) {
    List<Grade> grades = getAllGrades();

    boolean found = false;
    for (int i = 0; i < grades.size(); i++) {
      if (grades.get(i).getGradeId() == grade.getGradeId()) {
        grades.set(i, grade);
        found = true;
        break;
      }
    }

    if (!found) {
      if (grade.getGradeId() == 0) {
        int maxId = grades.stream().mapToInt(Grade::getGradeId).max().orElse(0);
        grade.setGradeId(maxId + 1);
      }
      grades.add(grade);
    }

    boolean result = writeGradesToCSV(grades);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách grades vào CSV
   */
  private boolean writeGradesToCSV(List<Grade> grades) {
    Path file = dataDir.resolve(GRADES_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "gradeId,studentCode,courseCode,gradeType,gradeName,score,maxScore,weight,gradeDate,notes,createdAt");

      for (Grade g : grades) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            g.getGradeId(),
            g.getStudentCode(),
            g.getCourseCode(),
            g.getGradeType() != null ? g.getGradeType().name().toLowerCase() : "",
            g.getGradeName() != null ? g.getGradeName() : "",
            g.getScore() != null ? g.getScore() : "",
            g.getMaxScore() != null ? g.getMaxScore() : "",
            g.getWeight() != null ? g.getWeight() : "",
            g.getGradeDate() != null ? g.getGradeDate() : "",
            g.getNotes() != null ? g.getNotes() : "",
            g.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing grades to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa grade khỏi CSV
   */
  public boolean deleteGrade(int gradeId) {
    List<Grade> grades = getAllGrades();
    grades.removeIf(g -> g.getGradeId() == gradeId);
    boolean result = writeGradesToCSV(grades);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== CLASS OPENING REQUESTS METHODS ====================

  /**
   * Đọc tất cả class opening requests từ CSV
   */
  public List<ClassOpeningRequest> getAllClassOpeningRequests() {
    List<ClassOpeningRequest> requests = new ArrayList<>();
    Path file = dataDir.resolve(CLASS_OPENING_REQUESTS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      int lineNum = 1;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        if (line.trim().isEmpty()) {
          continue; // Skip empty lines
        }
        ClassOpeningRequest request = parseClassOpeningRequestFromCSV(line);
        if (request != null) {
          requests.add(request);
        } else {
          LOGGER.warning("Failed to parse class opening request from CSV line #" + lineNum + ": " + line);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading class opening requests from CSV: " + e.getMessage());
    }

    return requests;
  }

  /**
   * Parse class opening request từ dòng CSV
   */
  private ClassOpeningRequest parseClassOpeningRequestFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 16)
        return null;

      ClassOpeningRequest request = new ClassOpeningRequest();
      request.setRequestId(Integer.parseInt(fields[0].trim()));
      request.setTeacherUsername(fields[1].trim());
      request.setSubjectCode(fields[2].trim());
      request.setAcademicYear(fields[3].trim());
      request.setSemester(Integer.parseInt(fields[4].trim()));
      request.setScheduleDay(fields[5].trim().isEmpty() ? null : fields[5].trim());
      request.setScheduleTime(fields[6].trim().isEmpty() ? null : fields[6].trim());
      request.setRoom(fields[7].trim().isEmpty() ? null : fields[7].trim());
      request.setMaxStudents(fields[8].trim().isEmpty() ? 50 : Integer.parseInt(fields[8].trim()));
      request.setReason(fields[9].trim().isEmpty() ? null : fields[9].trim());
      request.setRequestStatus(fields[10].trim().isEmpty() ? null
          : ClassOpeningRequest.RequestStatus.valueOf(fields[10].trim().toUpperCase()));
      request.setAdminNote(fields[11].trim().isEmpty() ? null : fields[11].trim());
      request.setApprovedByUsername(fields[12].trim().isEmpty() ? null : fields[12].trim());
      request.setApprovedCourseCode(fields[13].trim().isEmpty() ? null : fields[13].trim());
      request.setRequestDate(fields[14].trim().isEmpty() ? null : java.sql.Timestamp.valueOf(fields[14].trim()));
      request.setDecisionDate(
          fields.length > 15 && !fields[15].trim().isEmpty() ? java.sql.Timestamp.valueOf(fields[15].trim()) : null);
      request.setCreatedAt(
          fields.length > 16 && !fields[16].trim().isEmpty() ? java.sql.Timestamp.valueOf(fields[16].trim()) : null);
      request.setUpdatedAt(
          fields.length > 17 && !fields[17].trim().isEmpty() ? java.sql.Timestamp.valueOf(fields[17].trim()) : null);

      return request;
    } catch (Exception e) {
      LOGGER.warning("Error parsing class opening request from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Lưu class opening request vào CSV
   */
  public boolean saveClassOpeningRequest(ClassOpeningRequest request) {
    List<ClassOpeningRequest> requests = getAllClassOpeningRequests();

    boolean found = false;
    for (int i = 0; i < requests.size(); i++) {
      if (requests.get(i).getRequestId() == request.getRequestId()) {
        requests.set(i, request);
        found = true;
        break;
      }
    }

    if (!found) {
      if (request.getRequestId() == 0) {
        int maxId = requests.stream().mapToInt(ClassOpeningRequest::getRequestId).max().orElse(0);
        request.setRequestId(maxId + 1);
      }
      requests.add(request);
    }

    boolean result = writeClassOpeningRequestsToCSV(requests);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách class opening requests vào CSV
   */
  private boolean writeClassOpeningRequestsToCSV(List<ClassOpeningRequest> requests) {
    Path file = dataDir.resolve(CLASS_OPENING_REQUESTS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "requestId,teacherUsername,subjectCode,academicYear,semester,scheduleDay,scheduleTime,room,maxStudents,reason,requestStatus,adminNote,approvedByUsername,approvedCourseCode,requestDate,decisionDate,createdAt,updatedAt");

      for (ClassOpeningRequest r : requests) {
        writer.println(String.format("%d,%s,%s,%s,%d,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            r.getRequestId(),
            r.getTeacherUsername() != null ? r.getTeacherUsername() : "",
            r.getSubjectCode(),
            r.getAcademicYear(),
            r.getSemester(),
            r.getScheduleDay() != null ? r.getScheduleDay() : "",
            r.getScheduleTime() != null ? r.getScheduleTime() : "",
            r.getRoom() != null ? r.getRoom() : "",
            r.getMaxStudents(),
            r.getReason() != null ? r.getReason() : "",
            r.getRequestStatus() != null ? r.getRequestStatus().name() : "",
            r.getAdminNote() != null ? r.getAdminNote() : "",
            r.getApprovedByUsername() != null ? r.getApprovedByUsername() : "",
            r.getApprovedCourseCode() != null ? r.getApprovedCourseCode() : "",
            r.getRequestDate() != null ? r.getRequestDate() : "",
            r.getDecisionDate() != null ? r.getDecisionDate() : "",
            r.getCreatedAt() != null ? r.getCreatedAt() : "",
            r.getUpdatedAt() != null ? r.getUpdatedAt() : ""));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing class opening requests to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa class opening request khỏi CSV
   */
  public boolean deleteClassOpeningRequest(int requestId) {
    List<ClassOpeningRequest> requests = getAllClassOpeningRequests();
    requests.removeIf(r -> r.getRequestId() == requestId);
    boolean result = writeClassOpeningRequestsToCSV(requests);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== COURSE REGISTRATIONS METHODS ====================

  /**
   * Đọc tất cả course registrations từ CSV
   */
  public List<CourseRegistration> getAllCourseRegistrations() {
    List<CourseRegistration> registrations = new ArrayList<>();
    Path file = dataDir.resolve(COURSE_REGISTRATIONS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        CourseRegistration registration = parseCourseRegistrationFromCSV(line);
        if (registration != null) {
          registrations.add(registration);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading course registrations from CSV: " + e.getMessage());
    }

    return registrations;
  }

  /**
   * Parse course registration từ dòng CSV
   */
  private CourseRegistration parseCourseRegistrationFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 8)
        return null;

      CourseRegistration registration = new CourseRegistration();
      registration.setRegistrationId(Integer.parseInt(fields[0].trim()));
      registration.setStudentCode(fields[1].trim());
      registration.setCourseCode(fields[2].trim());
      registration.setRegistrationDate(java.sql.Timestamp.valueOf(fields[3].trim()));
      registration.setRegistrationStatus(
          fields[4].trim().isEmpty() ? null
              : CourseRegistration.RegistrationStatus.valueOf(fields[4].trim().toUpperCase()));
      registration.setCancelDate(fields[5].trim().isEmpty() ? null : java.sql.Timestamp.valueOf(fields[5].trim()));
      registration.setNotes(fields[6].trim().isEmpty() ? null : fields[6].trim());
      registration.setCreatedAt(java.sql.Timestamp.valueOf(fields[7].trim()));

      return registration;
    } catch (Exception e) {
      LOGGER.warning("Error parsing course registration from CSV line: " + line + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Lưu course registration vào CSV
   */
  public boolean saveCourseRegistration(CourseRegistration registration) {
    List<CourseRegistration> registrations = getAllCourseRegistrations();

    boolean found = false;
    for (int i = 0; i < registrations.size(); i++) {
      if (registrations.get(i).getRegistrationId() == registration.getRegistrationId()) {
        registrations.set(i, registration);
        found = true;
        break;
      }
    }

    if (!found) {
      if (registration.getRegistrationId() == 0) {
        int maxId = registrations.stream().mapToInt(CourseRegistration::getRegistrationId).max().orElse(0);
        registration.setRegistrationId(maxId + 1);
      }
      registrations.add(registration);
    }

    boolean result = writeCourseRegistrationsToCSV(registrations);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách course registrations vào CSV
   */
  private boolean writeCourseRegistrationsToCSV(List<CourseRegistration> registrations) {
    Path file = dataDir.resolve(COURSE_REGISTRATIONS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "registrationId,studentCode,courseCode,registrationDate,registrationStatus,cancelDate,notes,createdAt");

      for (CourseRegistration r : registrations) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s",
            r.getRegistrationId(),
            r.getStudentCode(),
            r.getCourseCode(),
            r.getRegistrationDate(),
            r.getRegistrationStatus() != null ? r.getRegistrationStatus().name() : "",
            r.getCancelDate() != null ? r.getCancelDate() : "",
            r.getNotes() != null ? r.getNotes() : "",
            r.getCreatedAt()));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing course registrations to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa course registration khỏi CSV
   */
  public boolean deleteCourseRegistration(int registrationId) {
    List<CourseRegistration> registrations = getAllCourseRegistrations();
    registrations.removeIf(r -> r.getRegistrationId() == registrationId);
    boolean result = writeCourseRegistrationsToCSV(registrations);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  // ==================== NOTIFICATIONS METHODS ====================

  /**
   * Đọc tất cả notifications từ CSV
   */
  public List<Notification> getAllNotifications() {
    List<Notification> notifications = new ArrayList<>();
    Path file = dataDir.resolve(NOTIFICATIONS_FILE);

    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Notification notification = parseNotificationFromCSV(line);
        if (notification != null) {
          notifications.add(notification);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Error reading notifications from CSV: " + e.getMessage());
    }

    return notifications;
  }

  /**
   * Parse notification từ dòng CSV
   */
  private Notification parseNotificationFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 10)
        return null;

      Notification notification = new Notification();
      notification.setNotificationId(Integer.parseInt(fields[0].trim()));
      notification.setTitle(fields[1].trim());
      notification.setContent(fields[2].trim());
      notification.setSenderUsername(fields[3].trim());
      notification.setTargetType(Notification.TargetType.valueOf(fields[4].trim().toUpperCase()));
      notification.setTargetCode(fields[5].trim().isEmpty() ? null : fields[5].trim());
      notification.setPriority(Notification.Priority.valueOf(fields[6].trim().toUpperCase()));
      notification.setRead(Boolean.parseBoolean(fields[7].trim()));
      notification.setCreatedAt(java.sql.Timestamp.valueOf(fields[8].trim()));
      notification.setExpiresAt(fields[9].trim().isEmpty() ? null : java.sql.Timestamp.valueOf(fields[9].trim()));

      return notification;
    } catch (Exception e) {
      LOGGER.warning("Error parsing notification from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Lưu notification vào CSV
   */
  public boolean saveNotification(Notification notification) {
    List<Notification> notifications = getAllNotifications();

    boolean found = false;
    for (int i = 0; i < notifications.size(); i++) {
      if (notifications.get(i).getNotificationId() == notification.getNotificationId()) {
        notifications.set(i, notification);
        found = true;
        break;
      }
    }

    if (!found) {
      if (notification.getNotificationId() == 0) {
        int maxId = notifications.stream().mapToInt(Notification::getNotificationId).max().orElse(0);
        notification.setNotificationId(maxId + 1);
      }
      notifications.add(notification);
    }

    boolean result = writeNotificationsToCSV(notifications);
    if (result) {
      incrementVersion();
    }
    return result;
  }

  /**
   * Ghi danh sách notifications vào CSV
   */
  private boolean writeNotificationsToCSV(List<Notification> notifications) {
    Path file = dataDir.resolve(NOTIFICATIONS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "notificationId,title,content,senderUsername,targetType,targetCode,priority,isRead,createdAt,expiresAt");

      for (Notification n : notifications) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            n.getNotificationId(),
            n.getTitle(),
            n.getContent(),
            n.getSenderUsername() != null ? n.getSenderUsername() : "",
            n.getTargetType() != null ? n.getTargetType().name() : "",
            n.getTargetCode() != null ? n.getTargetCode() : "",
            n.getPriority() != null ? n.getPriority().name() : "",
            n.isRead(),
            n.getCreatedAt(),
            n.getExpiresAt() != null ? n.getExpiresAt() : ""));
      }

      return true;
    } catch (IOException e) {
      LOGGER.severe("Error writing notifications to CSV: " + e.getMessage());
      return false;
    }
  }

  /**
   * Xóa notification khỏi CSV
   */
  public boolean deleteNotification(int notificationId) {
    List<Notification> notifications = getAllNotifications();
    notifications.removeIf(n -> n.getNotificationId() == notificationId);
    boolean result = writeNotificationsToCSV(notifications);
    if (result) {
      incrementVersion();
    }
    return result;
  }
}
