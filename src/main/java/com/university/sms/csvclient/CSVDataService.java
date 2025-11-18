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
 * Version = timestamp (seconds since epoch) để so sánh với server
 */
public class CSVDataService {
  private static final Logger LOGGER = Logger.getLogger(CSVDataService.class.getName());

  private static final String DATA_DIR = "data/csv";

  // CSV Files
  private static final String USERS_FILE = "users.csv";
  private static final String FACULTIES_FILE = "faculties.csv";
  private static final String CLASSES_FILE = "classes.csv";
  private static final String STUDENTS_FILE = "students.csv";
  private static final String SUBJECTS_FILE = "subjects.csv";
  private static final String COURSES_FILE = "courses.csv";
  private static final String ENROLLMENTS_FILE = "enrollments.csv";
  private static final String GRADES_FILE = "grades.csv";
  private static final String CLASS_OPENING_REQUESTS_FILE = "class_opening_requests.csv";
  private static final String COURSE_REGISTRATIONS_FILE = "course_registrations.csv";
  private static final String NOTIFICATIONS_FILE = "notifications.csv";
  private static final String VERSION_FILE = ".version";

  private Path dataDir;
  private int version = 0; // Version = timestamp (seconds since epoch)
  private boolean skipVersionIncrement = false; // Flag để tạm thời disable version increment khi download

  public CSVDataService() {
    this.dataDir = Paths.get(DATA_DIR);
    initializeDataDirectory();
    loadVersion();
  }

  /**
   * Khởi tạo thư mục dữ liệu
   */
  private void initializeDataDirectory() {
    try {
      if (!Files.exists(dataDir)) {
        Files.createDirectories(dataDir);
      }
      createSampleDataIfNotExists();
    } catch (IOException e) {
      // Ignore
    }
  }

  /**
   * Tạo dữ liệu mẫu nếu file chưa tồn tại
   */
  private void createSampleDataIfNotExists() {
    try {
      createEmptyFileIfNotExists(USERS_FILE,
          "userId,username,password,fullName,email,phone,address,role,isActive,createdAt,facultyCode");
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
      // Ignore
    }
  }

  /**
   * Tạo file CSV trống nếu chưa tồn tại
   */
  private void createEmptyFileIfNotExists(String filename, String header) throws IOException {
    Path file = dataDir.resolve(filename);
    if (!Files.exists(file)) {
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
        writer.println(header);
      }
    }
  }

  // ==================== VERSION MANAGEMENT ====================

  /**
   * Lấy version hiện tại (timestamp in seconds since epoch)
   */
  public int getVersion() {
    return version;
  }

  /**
   * Set version (dùng khi download từ server)
   */
  public void setVersion(int version) {
    this.version = version;
    saveVersionToFile();
  }

  /**
   * Tăng version khi có thay đổi
   * Version = current timestamp (seconds since epoch)
   */
  public void incrementVersion() {
    if (!skipVersionIncrement) {
      this.version = (int) (System.currentTimeMillis() / 1000);
      saveVersionToFile();
    }
  }

  /**
   * Set flag để tạm thời disable version increment (dùng khi download từ server)
   */
  public void setSkipVersionIncrement(boolean skip) {
    this.skipVersionIncrement = skip;
  }

  /**
   * Đọc version từ file
   */
  private void loadVersion() {
    try {
      Path versionFile = dataDir.resolve(VERSION_FILE);
      if (Files.exists(versionFile)) {
        String content = Files.readString(versionFile).trim();
        if (content != null && !content.isEmpty()) {
          try {
            version = Integer.parseInt(content);
          } catch (NumberFormatException e) {
            version = (int) (System.currentTimeMillis() / 1000);
            saveVersionToFile();
          }
        } else {
          version = 0; // Version rỗng = chưa sync lần nào
        }
      } else {
        version = 0; // Version rỗng = chưa sync lần nào
      }
    } catch (IOException e) {
      version = 0;
    }
  }

  /**
   * Lưu version vào file
   */
  private void saveVersionToFile() {
    try {
      Path versionFile = dataDir.resolve(VERSION_FILE);
      Files.write(versionFile, String.valueOf(version).getBytes());
    } catch (IOException e) {
      // Ignore
    }
  }

  /**
   * Lấy metadata của CSV local
   */
  public Map<String, Object> getCSVMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("database_type", "CSV");
    metadata.put("db_version", version); // Version = timestamp (seconds since epoch)
    metadata.put("student_count", getAllStudents().size());
    metadata.put("course_count", getAllCourses().size());
    metadata.put("enrollment_count", getAllEnrollments().size());
    metadata.put("faculty_count", getAllFaculties().size());
    metadata.put("class_count", getAllClasses().size());
    metadata.put("subject_count", getAllSubjects().size());
    int totalRecords = getAllStudents().size() + getAllCourses().size() + getAllEnrollments().size() +
        getAllFaculties().size() + getAllClasses().size() + getAllSubjects().size();
    metadata.put("total_records", totalRecords);
    return metadata;
  }

  // ==================== STUDENTS METHODS ====================

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
      // Ignore
    }
    return students;
  }

  /**
   * Lưu tất cả students cùng lúc (dùng khi download từ server)
   * Không tăng version nếu skipVersionIncrement = true
   */
  public boolean saveAllStudents(List<Student> students) {
    if (students == null || students.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeStudentsToCSV(students);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== USERS METHODS ====================

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
      // Ignore
    }
    return users;
  }

  public boolean saveAllUsers(List<User> users) {
    if (users == null || users.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeUsersToCSV(users);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      // Handle createdAt - có thể là null hoặc timestamp
      if (fields.length > 9 && fields[9].trim() != null && !fields[9].trim().isEmpty()
          && !fields[9].trim().equals("null")) {
        try {
          user.setCreatedAt(java.sql.Timestamp.valueOf(fields[9].trim()));
        } catch (Exception e) {
          user.setCreatedAt(null);
        }
      } else {
        user.setCreatedAt(null);
      }
      // Handle facultyCode (field 10, optional)
      if (fields.length > 10 && fields[10].trim() != null && !fields[10].trim().isEmpty()) {
        user.setFacultyCode(fields[10].trim());
      } else {
        user.setFacultyCode(null);
      }
      return user;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean writeUsersToCSV(List<User> users) {
    Path file = dataDir.resolve(USERS_FILE);
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println("userId,username,password,fullName,email,phone,address,role,isActive,createdAt,facultyCode");
      for (User user : users) {
        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            user.getUserId(),
            user.getUsername(),
            user.getPassword(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone() != null ? user.getPhone() : "",
            user.getAddress() != null ? user.getAddress() : "",
            user.getRole(),
            user.isActive(),
            user.getCreatedAt() != null ? user.getCreatedAt() : "null",
            user.getFacultyCode() != null ? user.getFacultyCode() : ""));
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  // ==================== COURSES METHODS ====================

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
      // Ignore
    }
    return courses;
  }

  public boolean saveAllCourses(List<Course> courses) {
    if (courses == null || courses.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeCoursesToCSV(courses);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== ENROLLMENTS METHODS ====================

  public List<Enrollment> getAllEnrollments() {
    List<Enrollment> enrollments = new ArrayList<>();
    Path file = dataDir.resolve(ENROLLMENTS_FILE);
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        Enrollment enrollment = parseEnrollmentFromCSV(line);
        if (enrollment != null) {
          enrollments.add(enrollment);
        }
      }
    } catch (IOException e) {
      // Ignore
    }
    return enrollments;
  }

  public boolean saveAllEnrollments(List<Enrollment> enrollments) {
    if (enrollments == null || enrollments.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeEnrollmentsToCSV(enrollments);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

  private Enrollment parseEnrollmentFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      // File CSV có 9 cột (theo database schema):
      // enrollmentId,studentCode,courseCode,enrollmentDate,enrollmentStatus,finalGrade,letterGrade,gradePoints,createdAt
      // Lưu ý: File CSV cũ có thể có attendanceRate thay vì gradePoints, nhưng code
      // sẽ đọc field[7] như gradePoints
      if (fields.length < 8)
        return null;
      Enrollment enrollment = new Enrollment();
      enrollment.setEnrollmentId(Integer.parseInt(fields[0].trim()));
      enrollment.setStudentCode(fields[1].trim());
      enrollment.setCourseCode(fields[2].trim());
      enrollment.setEnrollmentDate(fields[3].trim().isEmpty() ? null : java.sql.Timestamp.valueOf(fields[3].trim()));

      // Xử lý enrollmentStatus - có thể null hoặc lowercase
      String statusStr = fields[4].trim();
      if (!statusStr.isEmpty()) {
        try {
          enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
          enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.ENROLLED); // Mặc định
        }
      } else {
        enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.ENROLLED); // Mặc định
      }

      enrollment.setFinalGrade(fields[5].trim().isEmpty() ? null : new java.math.BigDecimal(fields[5].trim()));
      enrollment.setLetterGrade(fields[6].trim().isEmpty() ? null : fields[6].trim());

      // Xử lý gradePoints (field 7) - theo database schema (grade_points)
      if (fields.length > 7 && !fields[7].trim().isEmpty()) {
        enrollment.setGradePoints(new java.math.BigDecimal(fields[7].trim()));
      } else {
        enrollment.setGradePoints(java.math.BigDecimal.ZERO);
      }

      return enrollment;
    } catch (Exception e) {
      LOGGER.warning("Lỗi khi parse enrollment từ CSV: " + e.getMessage() + " - Line: " + line);
      return null;
    }
  }

  private boolean writeEnrollmentsToCSV(List<Enrollment> enrollments) {
    Path file = dataDir.resolve(ENROLLMENTS_FILE);
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header khớp với database schema: enrollment_id, student_code, course_code,
      // enrollment_date,
      // enrollment_status, final_grade, letter_grade, grade_points, created_at
      writer.println(
          "enrollmentId,studentCode,courseCode,enrollmentDate,enrollmentStatus,finalGrade,letterGrade,gradePoints,createdAt");
      for (Enrollment enrollment : enrollments) {
        // Xử lý enrollmentDate - có thể null
        String enrollmentDateStr = "";
        if (enrollment.getEnrollmentDate() != null) {
          enrollmentDateStr = enrollment.getEnrollmentDate().toString();
        }

        // Xử lý enrollmentStatus - có thể null
        String statusStr = "";
        if (enrollment.getEnrollmentStatus() != null) {
          statusStr = enrollment.getEnrollmentStatus().name().toLowerCase(); // lowercase để khớp với file hiện tại
        } else {
          statusStr = "enrolled"; // Mặc định
        }

        // Xử lý gradePoints - có thể null (theo database schema)
        String gradePointsStr = "";
        if (enrollment.getGradePoints() != null) {
          gradePointsStr = enrollment.getGradePoints().toString();
        } else {
          gradePointsStr = "0.00";
        }

        // Xử lý createdAt - Enrollment model không có createdAt, dùng enrollmentDate
        String createdAtStr = enrollmentDateStr; // Dùng enrollmentDate làm createdAt

        writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
            enrollment.getEnrollmentId(),
            enrollment.getStudentCode() != null ? enrollment.getStudentCode() : "",
            enrollment.getCourseCode() != null ? enrollment.getCourseCode() : "",
            enrollmentDateStr,
            statusStr,
            enrollment.getFinalGrade() != null ? enrollment.getFinalGrade() : "",
            enrollment.getLetterGrade() != null ? enrollment.getLetterGrade() : "",
            gradePointsStr,
            createdAtStr));
      }
      LOGGER.info("Đã ghi " + enrollments.size() + " enrollments vào file CSV");
      return true;
    } catch (IOException e) {
      LOGGER.severe("Lỗi khi ghi enrollments vào CSV: " + e.getMessage());
      LOGGER.log(java.util.logging.Level.SEVERE, "Chi tiết lỗi", e);
      return false;
    } catch (Exception e) {
      LOGGER.severe("Lỗi không mong đợi khi ghi enrollments vào CSV: " + e.getMessage());
      LOGGER.log(java.util.logging.Level.SEVERE, "Chi tiết lỗi", e);
      return false;
    }
  }

  // ==================== FACULTIES METHODS ====================

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
      // Ignore
    }
    return faculties;
  }

  public boolean saveAllFaculties(List<Faculty> faculties) {
    if (faculties == null || faculties.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeFacultiesToCSV(faculties);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== CLASSES METHODS ====================

  public List<Class> getAllClasses() {
    List<Class> classes = new ArrayList<>();
    Path file = dataDir.resolve(CLASSES_FILE);
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        Class clazz = parseClassFromCSV(line);
        if (clazz != null) {
          classes.add(clazz);
        }
      }
    } catch (IOException e) {
      // Ignore
    }
    return classes;
  }

  public boolean saveAllClasses(List<Class> classes) {
    if (classes == null || classes.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeClassesToCSV(classes);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

  private Class parseClassFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 9)
        return null;
      Class clazz = new Class();
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
      return null;
    }
  }

  private boolean writeClassesToCSV(List<Class> classes) {
    Path file = dataDir.resolve(CLASSES_FILE);
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      writer.println(
          "classId,classCode,className,facultyCode,teacherUsername,academicYear,semester,maxStudents,createdAt");
      for (Class c : classes) {
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
      return false;
    }
  }

  // ==================== SUBJECTS METHODS ====================

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
      // Ignore
    }
    return subjects;
  }

  public boolean saveAllSubjects(List<Subject> subjects) {
    if (subjects == null || subjects.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeSubjectsToCSV(subjects);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== GRADES METHODS ====================

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
      // Ignore
    }
    return grades;
  }

  public boolean saveAllGrades(List<Grade> grades) {
    if (grades == null || grades.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeGradesToCSV(grades);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== CLASS OPENING REQUESTS METHODS ====================

  public List<ClassOpeningRequest> getAllClassOpeningRequests() {
    List<ClassOpeningRequest> requests = new ArrayList<>();
    Path file = dataDir.resolve(CLASS_OPENING_REQUESTS_FILE);
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line = reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        ClassOpeningRequest request = parseClassOpeningRequestFromCSV(line);
        if (request != null) {
          requests.add(request);
        }
      }
    } catch (IOException e) {
      // Ignore
    }
    return requests;
  }

  public boolean saveAllClassOpeningRequests(List<ClassOpeningRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeClassOpeningRequestsToCSV(requests);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== COURSE REGISTRATIONS METHODS ====================

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
      // Ignore
    }
    return registrations;
  }

  public boolean saveAllCourseRegistrations(List<CourseRegistration> registrations) {
    if (registrations == null || registrations.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeCourseRegistrationsToCSV(registrations);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== NOTIFICATIONS METHODS ====================

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
      // Ignore
    }
    return notifications;
  }

  public boolean saveAllNotifications(List<Notification> notifications) {
    if (notifications == null || notifications.isEmpty()) {
      return true; // Không có gì để lưu
    }
    boolean result = writeNotificationsToCSV(notifications);
    if (result && !skipVersionIncrement) {
      incrementVersion();
    }
    return result;
  }

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
      return null;
    }
  }

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
      return false;
    }
  }

  // ==================== UTILITY METHODS ====================

  public Path getDataDirectory() {
    return dataDir;
  }
}
