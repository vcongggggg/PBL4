package com.university.sms.csvclient;

import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
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
  private static final String STUDENTS_FILE = "students.csv";
  private static final String COURSES_FILE = "courses.csv";
  private static final String ENROLLMENTS_FILE = "enrollments.csv";
  private static final String USERS_FILE = "users.csv";

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
      // Tạo file students.csv trống
      Path studentsFile = dataDir.resolve(STUDENTS_FILE);
      if (!Files.exists(studentsFile)) {
        createEmptyCSVFile(studentsFile,
            "studentId,userId,studentCode,classId,facultyId,admissionYear,studentStatus,gpa,totalCredits,birthDate,gender,citizenId,emergencyContact,emergencyPhone,createdAt,fullName,email,phone,address");
      }

      // Tạo file courses.csv trống
      Path coursesFile = dataDir.resolve(COURSES_FILE);
      if (!Files.exists(coursesFile)) {
        createEmptyCSVFile(coursesFile,
            "courseId,courseCode,subjectId,teacherId,academicYear,semester,credits,maxStudents,currentStudents,courseStatus,createdAt");
      }

      // Tạo file enrollments.csv trống
      Path enrollmentsFile = dataDir.resolve(ENROLLMENTS_FILE);
      if (!Files.exists(enrollmentsFile)) {
        createEmptyCSVFile(enrollmentsFile,
            "enrollmentId,studentId,courseId,enrollmentDate,enrollmentStatus,finalGrade,attendanceRate,createdAt");
      }

      // Tạo file users.csv trống
      Path usersFile = dataDir.resolve(USERS_FILE);
      if (!Files.exists(usersFile)) {
        createEmptyCSVFile(usersFile, "userId,username,password,fullName,email,phone,address,role,isActive,createdAt");
      }

    } catch (IOException e) {
      LOGGER.severe("Error creating empty CSV files: " + e.getMessage());
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
      while ((line = reader.readLine()) != null) {
        Enrollment enrollment = parseEnrollmentFromCSV(line);
        if (enrollment != null) {
          enrollments.add(enrollment);
        }
      }
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
      student.setStudentId(Integer.parseInt(fields[0]));
      student.setUserId(Integer.parseInt(fields[1]));
      student.setStudentCode(fields[2]);
      student.setClassId(fields[3].isEmpty() ? null : Integer.parseInt(fields[3]));
      student.setFacultyId(Integer.parseInt(fields[4]));
      student.setAdmissionYear(Integer.parseInt(fields[5]));
      student.setStudentStatus(Student.StudentStatus.valueOf(fields[6]));
      student.setGpa(new java.math.BigDecimal(fields[7]));
      student.setTotalCredits(Integer.parseInt(fields[8]));
      student.setBirthDate(java.sql.Date.valueOf(fields[9]));
      student.setGender(Student.Gender.valueOf(fields[10]));
      student.setCitizenId(fields[11]);
      student.setEmergencyContact(fields[12]);
      student.setEmergencyPhone(fields[13]);
      student.setCreatedAt(java.sql.Timestamp.valueOf(fields[14]));
      student.setFullName(fields[15]);
      student.setEmail(fields[16]);
      student.setPhone(fields[17]);
      student.setAddress(fields[18]);

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
      if (fields.length < 11)
        return null;

      Course course = new Course();
      course.setCourseId(Integer.parseInt(fields[0]));
      course.setCourseCode(fields[1]);
      course.setSubjectId(Integer.parseInt(fields[2]));
      course.setTeacherId(Integer.parseInt(fields[3]));
      course.setAcademicYear(fields[4]); // String, không phải int
      course.setSemester(Integer.parseInt(fields[5]));
      course.setCredits(Integer.parseInt(fields[6]));
      course.setMaxStudents(Integer.parseInt(fields[7]));
      course.setCurrentStudents(Integer.parseInt(fields[8]));
      course.setCourseStatus(Course.CourseStatus.valueOf(fields[9]));
      course.setCreatedAt(java.sql.Timestamp.valueOf(fields[10]));

      return course;
    } catch (Exception e) {
      LOGGER.warning("Error parsing course from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Parse đăng ký từ dòng CSV
   */
  private Enrollment parseEnrollmentFromCSV(String line) {
    try {
      String[] fields = line.split(",");
      if (fields.length < 7)
        return null;

      Enrollment enrollment = new Enrollment();
      enrollment.setEnrollmentId(Integer.parseInt(fields[0]));
      enrollment.setStudentId(Integer.parseInt(fields[1]));
      enrollment.setCourseId(Integer.parseInt(fields[2]));
      enrollment.setEnrollmentDate(java.sql.Timestamp.valueOf(fields[3]));
      enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.valueOf(fields[4]));
      enrollment.setFinalGrade(fields[5].isEmpty() ? null : new java.math.BigDecimal(fields[5]));
      enrollment.setAttendanceRate(fields[6].isEmpty() ? null : new java.math.BigDecimal(fields[6]));

      return enrollment;
    } catch (Exception e) {
      LOGGER.warning("Error parsing enrollment from CSV line: " + line + " - " + e.getMessage());
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
      user.setUserId(Integer.parseInt(fields[0]));
      user.setUsername(fields[1]);
      user.setPassword(fields[2]);
      user.setFullName(fields[3]);
      user.setEmail(fields[4]);
      user.setPhone(fields[5]);
      user.setAddress(fields[6]);
      user.setRole(User.UserRole.valueOf(fields[7]));
      user.setActive(Boolean.parseBoolean(fields[8]));
      user.setCreatedAt(java.sql.Timestamp.valueOf(fields[9]));

      return user;
    } catch (Exception e) {
      LOGGER.warning("Error parsing user from CSV line: " + line + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * Ghi danh sách sinh viên vào CSV
   */
  private boolean writeStudentsToCSV(List<Student> students) {
    Path file = dataDir.resolve(STUDENTS_FILE);

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      // Header
      writer.println(
          "studentId,userId,studentCode,classId,facultyId,admissionYear,studentStatus,gpa,totalCredits,birthDate,gender,citizenId,emergencyContact,emergencyPhone,createdAt,fullName,email,phone,address");

      // Data
      for (Student student : students) {
        writer.println(String.format("%d,%d,%s,%s,%d,%d,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            student.getStudentId(),
            student.getUserId(),
            student.getStudentCode(),
            student.getClassId() != null ? student.getClassId() : "",
            student.getFacultyId(),
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
      // Header
      writer.println(
          "courseId,courseCode,subjectId,teacherId,academicYear,semester,credits,maxStudents,currentStudents,courseStatus,createdAt");

      // Data
      for (Course course : courses) {
        writer.println(String.format("%d,%s,%d,%d,%s,%d,%d,%d,%d,%s,%s",
            course.getCourseId(),
            course.getCourseCode(),
            course.getSubjectId(),
            course.getTeacherId(),
            course.getAcademicYear(),
            course.getSemester(),
            course.getCredits(),
            course.getMaxStudents(),
            course.getCurrentStudents(),
            course.getCourseStatus(),
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
      // Header
      writer.println(
          "enrollmentId,studentId,courseId,enrollmentDate,enrollmentStatus,finalGrade,attendanceRate");

      // Data
      for (Enrollment enrollment : enrollments) {
        writer.println(String.format("%d,%d,%d,%s,%s,%s,%s",
            enrollment.getEnrollmentId(),
            enrollment.getStudentId(),
            enrollment.getCourseId(),
            enrollment.getEnrollmentDate(),
            enrollment.getEnrollmentStatus(),
            enrollment.getFinalGrade(),
            enrollment.getAttendanceRate()));
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
}
