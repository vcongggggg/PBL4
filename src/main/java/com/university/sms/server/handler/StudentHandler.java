package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.ClassDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.FacultyDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.dao.UserDAO;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Student;
import com.university.sms.model.User;
import com.university.sms.service.CourseRegistrationService;
import com.university.sms.service.StudentService;
import com.university.sms.service.TranscriptService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến sinh viên
 */
public class StudentHandler {
  private static final Logger LOGGER = Logger.getLogger(StudentHandler.class.getName());

  private final StudentService studentService;
  private final CourseRegistrationService registrationService;
  private final TranscriptService transcriptService;
  private User currentUser;
  private final Supplier<String> clientSourceSupplier;
  private final DataOriginHelper dataOriginHelper;

  public StudentHandler(StudentService studentService,
      CourseRegistrationService registrationService,
      TranscriptService transcriptService,
      User currentUser,
      Supplier<String> clientSourceSupplier,
      DataOriginHelper dataOriginHelper) {
    this.studentService = studentService;
    this.registrationService = registrationService;
    this.transcriptService = transcriptService;
    this.currentUser = currentUser;
    this.clientSourceSupplier = clientSourceSupplier;
    this.dataOriginHelper = dataOriginHelper;
  }

  private String getClientSource() {
    return clientSourceSupplier != null ? clientSourceSupplier.get() : "UNKNOWN";
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetStudentInfo(Message request) {
    if (currentUser.getRole() == User.UserRole.STUDENT) {
      var student = studentService.findByUsername(currentUser.getUsername());
      if (student != null) {
        Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
            "Lấy thông tin thành công");
        response.addData(Constants.KEY_STUDENT, student);
        return response;
      }
    } else if (currentUser.getRole() == User.UserRole.ADMIN ||
        currentUser.getRole() == User.UserRole.TEACHER) {
      String studentCode = request.getData("studentCode", String.class);
      if (studentCode != null && !studentCode.isEmpty()) {
        StudentDAO studentDAO = new StudentDAO();
        Student student = studentDAO.findByStudentCode(studentCode);
        if (student != null) {
          Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
              "Lấy thông tin thành công");
          response.addData(Constants.KEY_STUDENT, student);
          return response;
        }
      }
    }

    return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_INFO, Constants.MSG_STUDENT_NOT_FOUND);
  }

  public Message handleGetAllStudents(Message request) {
    if (currentUser == null || (currentUser.getRole() != User.UserRole.ADMIN
        && currentUser.getRole() != User.UserRole.TEACHER)) {
      return Message.createErrorResponse(Constants.ACTION_GET_ALL_STUDENTS, Constants.MSG_UNAUTHORIZED);
    }

    try {
      var students = studentService.getAllStudents();
      Message response = Message.createSuccessResponse(request.getAction(), "Lấy danh sách thành công");
      response.addData(Constants.KEY_STUDENTS, students);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách tất cả sinh viên", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
    }
  }

  public Message handleGetStudentsByClass(Message request) {
    if (currentUser == null || (currentUser.getRole() != User.UserRole.ADMIN
        && currentUser.getRole() != User.UserRole.TEACHER)) {
      return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, Constants.MSG_UNAUTHORIZED);
    }

    try {
      String classCode = request.getData(Constants.KEY_CLASS_CODE, String.class);
      if (classCode == null || classCode.trim().isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, "Thiếu mã lớp");
      }

      List<Student> students = studentService.getStudentsByClass(classCode.trim());
      Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS,
          "Lấy danh sách sinh viên thành công");
      response.addData(Constants.KEY_STUDENTS, students);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách sinh viên theo lớp", e);
      return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, "Lỗi server: " + e.getMessage());
    }
  }

  public Message handleSearchStudents(Message request) {
    if (currentUser == null || (currentUser.getRole() != User.UserRole.ADMIN
        && currentUser.getRole() != User.UserRole.TEACHER)) {
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, Constants.MSG_UNAUTHORIZED);
    }

    String keyword = request.getData(Constants.KEY_SEARCH_KEYWORD, String.class);
    if (keyword == null || keyword.trim().isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, Constants.MSG_INVALID_DATA);
    }

    var students = studentService.searchStudents(keyword);
    Message response = Message.createSuccessResponse(Constants.ACTION_SEARCH_STUDENTS, "Tìm kiếm thành công");
    response.addData(Constants.KEY_STUDENTS, students);
    return response;
  }

  public Message handleGetMyRegistrations(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      if (studentCode == null || studentCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code is required");
      }

      List<CourseRegistration> registrations = registrationService.getRegistrationsByStudent(studentCode);
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_REGISTRATIONS, registrations);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy student's registrations: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleRegisterCourse(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);
      String notes = request.getData(Constants.KEY_NOTE, String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(),
            "Student code and course code are required");
      }

      boolean success = registrationService.registerCourse(studentCode, courseCode, notes);

      if (success) {
        CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
        List<CourseRegistration> registrations = registrationDAO.findByStudent(studentCode);
        CourseRegistration registration = registrations.stream()
            .filter(r -> courseCode.equals(r.getCourseCode()))
            .findFirst()
            .orElse(null);
        // Chỉ admin mới lưu source khi thêm mới, student register không lưu source
        // Nếu đã có source thì chỉ update timestamp
        if (registration != null && registration.getRegistrationId() > 0) {
          String existingSource = dataOriginHelper.getDataOrigin("course_registration",
              registration.getRegistrationId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("course_registration", registration.getRegistrationId());
          }
        }
        return Message.createSuccessResponse(request.getAction(), "Registration submitted successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to submit registration");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đăng ký course: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleCancelRegistration(Message request) {
    try {
      int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
      String studentCode = request.getData("studentCode", String.class);

      if (studentCode == null || studentCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code is required");
      }

      CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
      CourseRegistration registrationBeforeCancel = registrationDAO.findById(registrationId);
      if (registrationBeforeCancel != null && registrationBeforeCancel.getRegistrationId() > 0) {
        String existingSource = dataOriginHelper.getDataOrigin("course_registration",
            registrationBeforeCancel.getRegistrationId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("course_registration",
              registrationBeforeCancel.getRegistrationId());
        }
      }

      boolean success = registrationService.cancelRegistration(registrationId, studentCode);

      if (success) {
        return Message.createSuccessResponse(request.getAction(), "Registration cancelled successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to cancel registration");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi hủy đăng ký khóa học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetCompletedSubjectCodes(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      if ((studentCode == null || studentCode.isEmpty()) && currentUser != null
          && currentUser.getRole() == User.UserRole.STUDENT) {
        var student = studentService.findByUsername(currentUser.getUsername());
        if (student != null) {
          studentCode = student.getStudentCode();
        }
      }

      if (studentCode == null || studentCode.trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code is required");
      }

      List<String> subjectCodes = transcriptService.getCompletedSubjectCodes(studentCode.trim());
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_SUBJECT_CODES, subjectCodes);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách môn đã hoàn thành", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleAddStudent(Message request) {
    if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_UNAUTHORIZED);
    }

    Student student = request.getData(Constants.KEY_STUDENT, Student.class);
    if (student == null) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_INVALID_DATA);
    }

    if (student.getStudentCode() == null || student.getStudentCode().trim().isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Thiếu mã sinh viên");
    }
    if (student.getFacultyCode() == null || student.getFacultyCode().trim().isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Thiếu mã khoa");
    }

    FacultyDAO facultyDAO = new FacultyDAO();
    if (facultyDAO.findByCode(student.getFacultyCode()) == null) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
          "Mã khoa không tồn tại: " + student.getFacultyCode());
    }

    if (student.getClassCode() != null && !student.getClassCode().trim().isEmpty()) {
      ClassDAO classDAO = new ClassDAO();
      if (classDAO.findByCode(student.getClassCode()) == null) {
        return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
            "Mã lớp không tồn tại: " + student.getClassCode());
      }
    }

    try {
      UserDAO userDAO = new UserDAO();
      boolean userOk = true;
      String username = student.getUsername();
      if (username == null || username.isEmpty()) {
        username = student.getStudentCode();
        student.setUsername(username);
      }

      if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
        if (!isValidEmailFormat(student.getEmail().trim())) {
          return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
              "Email không hợp lệ. Email phải có định dạng: example@domain.com");
        }
      }

      String normalizedStudentPhone = null;
      if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
        normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
        if (!isValidPhoneFormat(normalizedStudentPhone)) {
          return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
              "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
        }
      }

      if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
        User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
        if (existingUserByEmail != null) {
          return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
              "Email đã được sử dụng bởi user khác: " + student.getEmail());
        }
      }

      if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
        User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
        if (existingUserByPhone != null) {
          return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
              "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
        }
      }

      User byUsername = userDAO.findByUsername(username);
      if (byUsername != null) {
        if (byUsername.getRole() != User.UserRole.STUDENT) {
          return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
              "Username đã tồn tại với vai trò khác: " + username);
        }
      } else {
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
          dataOriginHelper.saveDataOrigin("user", u.getUserId(), getClientSource());
        }
      }
      if (!userOk) {
        return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
            "Không thể tạo tài khoản người dùng. Username có thể đã tồn tại.");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi chuẩn bị user cho sinh viên: " + student.getStudentCode(), e);
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
          "Lỗi khi tạo tài khoản: " + e.getMessage());
    }

    StudentDAO studentDAO = new StudentDAO();
    if (studentDAO.findByStudentCode(student.getStudentCode()) != null) {
      return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
          "Mã sinh viên đã tồn tại: " + student.getStudentCode());
    }

    boolean ok = studentService.addStudent(student);
    if (ok) {
      dataOriginHelper.saveDataOrigin("student", student.getStudentId(), getClientSource());
      LOGGER.info("Đã thêm sinh viên thành công: " + student.getStudentCode()
          + " bởi " + (currentUser != null ? currentUser.getUsername() : "system"));
      return Message.createSuccessResponse(Constants.ACTION_ADD_STUDENT, "Thêm sinh viên thành công");
    }
    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Không thể thêm sinh viên. Vui lòng thử lại.");
  }

  public Message handleUpdateStudent(Message request) {
    Student student = request.getData(Constants.KEY_STUDENT, Student.class);
    if (student == null || student.getStudentId() <= 0) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_INVALID_DATA);
    }

    if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
      if (!isValidEmailFormat(student.getEmail().trim())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Email không hợp lệ. Email phải có định dạng: example@domain.com");
      }
    }

    String normalizedStudentPhone = null;
    if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
      normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
      if (!isValidPhoneFormat(normalizedStudentPhone)) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
      }
    }

    StudentDAO studentDAO = new StudentDAO();
    Student currentStudent = studentDAO.findById(student.getStudentId());
    if (currentStudent == null) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Không tìm thấy sinh viên");
    }

    if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
      UserDAO userDAO = new UserDAO();
      User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
      if (existingUserByEmail != null && currentStudent.getUsername() != null
          && !existingUserByEmail.getUsername().equals(currentStudent.getUsername())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Email đã được sử dụng bởi user khác: " + student.getEmail());
      }
    }

    String currentPhone = currentStudent.getPhone();
    String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
    boolean phoneChanged = normalizedStudentPhone != null
        && !normalizedStudentPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

    if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty() && phoneChanged) {
      UserDAO userDAO = new UserDAO();
      User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
      if (existingUserByPhone != null && currentStudent.getUsername() != null
          && !existingUserByPhone.getUsername().equals(currentStudent.getUsername())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
      }
    }

    if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
      student.setPhone(phoneChanged ? normalizedStudentPhone : currentPhone);
    }

    if (currentUser != null && currentUser.getRole() == User.UserRole.STUDENT) {
      return handleStudentSelfUpdate(student);
    } else if (currentUser != null
        && (currentUser.getRole() == User.UserRole.ADMIN || currentUser.getRole() == User.UserRole.TEACHER)) {
      boolean ok = studentService.updateStudent(student);
      if (ok) {
        // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
        String existingSource = dataOriginHelper.getDataOrigin("student", student.getStudentId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("student", student.getStudentId());
        }
        return Message.createSuccessResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_SUCCESS);
      }
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_DATABASE_ERROR);
    } else {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_UNAUTHORIZED);
    }
  }

  public Message handleDeleteStudent(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa sinh viên");
      }

      String studentCode = request.getData("studentCode", String.class);
      if (studentCode == null || studentCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code is required");
      }

      StudentDAO studentDAO = new StudentDAO();
      Student student = studentDAO.findByStudentCode(studentCode);
      if (student == null) {
        return Message.createErrorResponse(request.getAction(), "Không tìm thấy sinh viên");
      }

      String existingSource = dataOriginHelper.getDataOrigin("student", student.getStudentId());

      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      List<Enrollment> enrollments = enrollmentDAO.findByStudentCode(studentCode);
      int enrollmentsUpdated = 0;
      for (Enrollment enrollment : enrollments) {
        try {
          String enrollmentSource = dataOriginHelper.getDataOrigin("enrollment", enrollment.getEnrollmentId());

          if (enrollment.getEnrollmentStatus() != Enrollment.EnrollmentStatus.COMPLETED) {
            if (enrollmentDAO.updateEnrollmentStatus(enrollment.getEnrollmentId(),
                Enrollment.EnrollmentStatus.DROPPED)) {
              enrollmentsUpdated++;
              // Chỉ update timestamp nếu đã có source, không tạo mới source
              if (enrollmentSource != null) {
                dataOriginHelper.updateDataOriginTimestamp("enrollment", enrollment.getEnrollmentId());
              }
              LOGGER.info("Đã cập nhật enrollment ID: " + enrollment.getEnrollmentId()
                  + " của sinh viên " + studentCode
                  + " từ status: " + enrollment.getEnrollmentStatus().name()
                  + " → DROPPED (Thôi học)" + (enrollmentSource != null ? ", source: " + enrollmentSource : ""));
            } else {
              LOGGER.warning("Không thể cập nhật enrollment ID: " + enrollment.getEnrollmentId()
                  + " của sinh viên " + studentCode);
            }
          }
        } catch (Exception e) {
          LOGGER.log(Level.WARNING,
              "Lỗi khi cập nhật enrollment ID: " + enrollment.getEnrollmentId(), e);
        }
      }
      if (enrollmentsUpdated > 0) {
        LOGGER.info("Đã cập nhật " + enrollmentsUpdated + " enrollments của sinh viên " + studentCode);
      }

      CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
      List<CourseRegistration> registrations = registrationDAO.findByStudent(studentCode);
      List<CourseRegistration> cancelledRegistrations = new ArrayList<>();
      int registrationsCancelled = 0;
      for (CourseRegistration registration : registrations) {
        try {
          if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.PENDING) {
            boolean cancelled = registrationDAO.cancel(registration.getRegistrationId());
            if (cancelled) {
              String registrationSource = dataOriginHelper.getDataOrigin("course_registration",
                  registration.getRegistrationId());
              if (registrationSource != null) {
                dataOriginHelper.updateDataOriginTimestamp("course_registration", registration.getRegistrationId());
              }
              registration.setRegistrationStatus(CourseRegistration.RegistrationStatus.CANCELLED);
              registration.setCancelDate(new Timestamp(System.currentTimeMillis()));
              cancelledRegistrations.add(registration);
              registrationsCancelled++;
              LOGGER.info("Đã hủy (reject) course registration ID: " + registration.getRegistrationId()
                  + " của sinh viên " + studentCode
                  + " (từ PENDING → CANCELLED do student bị SUSPENDED)");
            } else {
              LOGGER.warning("Không thể hủy course registration ID: " + registration.getRegistrationId()
                  + " của sinh viên " + studentCode);
            }
          }
        } catch (Exception e) {
          LOGGER.log(Level.WARNING,
              "Lỗi khi hủy course registration ID: " + registration.getRegistrationId(), e);
        }
      }
      if (registrationsCancelled > 0) {
        LOGGER.info("Đã hủy (reject) " + registrationsCancelled + " course registrations PENDING của sinh viên "
            + studentCode);
      }

      UserDAO userDAO = new UserDAO();
      boolean userDeactivated = userDAO.deactivateUser(student.getUsername());

      boolean statusUpdated = studentDAO.updateStudentStatus(student.getStudentId(),
          Student.StudentStatus.SUSPENDED);

      if (userDeactivated && statusUpdated) {
        // Chỉ update timestamp nếu đã có source, không tạo mới source khi xóa
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("student", student.getStudentId());
        }

        User user = userDAO.findByUsername(student.getUsername());
        if (user != null) {
          String userSource = dataOriginHelper.getDataOrigin("user", user.getUserId());
          // Chỉ update timestamp nếu đã có source, không tạo mới source khi xóa
          if (userSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("user", user.getUserId());
          }
          // Không tạo source mới khi xóa
        }

        LOGGER.info("Đã vô hiệu hóa sinh viên: " + student.getStudentCode()
            + " bởi " + (currentUser != null ? currentUser.getUsername() : "system"));

        Student updatedStudent = studentDAO.findByStudentCode(studentCode);

        Message response = Message.createSuccessResponse(request.getAction(),
            "Đã vô hiệu hóa sinh viên thành công");
        if (updatedStudent != null) {
          response.addData(Constants.KEY_STUDENT, updatedStudent);
        }
        if (!cancelledRegistrations.isEmpty()) {
          response.addData(Constants.KEY_REGISTRATIONS, cancelledRegistrations);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể vô hiệu hóa sinh viên");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi vô hiệu hóa sinh viên", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetTranscript(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      if (studentCode == null || studentCode.isEmpty()) {
        if (currentUser != null
            && "STUDENT".equalsIgnoreCase(currentUser.getRole().toString())) {
          StudentDAO studentDAO = new StudentDAO();
          Student student = studentDAO.findByUsername(currentUser.getUsername());
          if (student != null) {
            studentCode = student.getStudentCode();
          }
        }
      }

      if (studentCode == null || studentCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Student code is required");
      }

      var transcript = transcriptService.generateTranscript(studentCode);

      if (transcript == null) {
        return Message.createErrorResponse(request.getAction(), "Cannot generate transcript");
      }

      Message response = Message.createSuccessResponse(request.getAction(), "Transcript retrieved successfully");
      response.addData(Constants.KEY_TRANSCRIPT, transcript);
      response.addData(Constants.KEY_CUMULATIVE_GPA, transcript.getCumulativeGPA());
      response.addData(Constants.KEY_ACADEMIC_RANK, transcript.getAcademicRank());
      response.addData(Constants.KEY_TOTAL_CREDITS, transcript.getTotalCreditsEarned());
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy học bạ", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetSemesterTranscript(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String academicYear = request.getData(Constants.KEY_ACADEMIC_YEAR, String.class);
      Integer semester = request.getData(Constants.KEY_SEMESTER, Integer.class);

      if (studentCode == null || studentCode.isEmpty() || academicYear == null || semester == null) {
        return Message.createErrorResponse(request.getAction(),
            "Student code, academic year and semester are required");
      }

      var semesterRecord = transcriptService.getSemesterTranscript(studentCode, academicYear, semester);

      if (semesterRecord == null) {
        return Message.createErrorResponse(request.getAction(), "Cannot generate semester transcript");
      }

      Message response = Message.createSuccessResponse(request.getAction(),
          "Semester transcript retrieved successfully");
      response.addData(Constants.KEY_SEMESTER_RECORDS, semesterRecord);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy học bạ học kỳ", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  private Message handleStudentSelfUpdate(Student student) {
    StudentDAO studentDAO = new StudentDAO();
    if (student.getStudentCode() == null || student.getStudentCode().isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Student code is required");
    }

    Student currentStudent = studentDAO.findByStudentCode(student.getStudentCode());
    if (currentStudent == null) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Không tìm thấy thông tin sinh viên");
    }

    if (currentUser == null || currentStudent.getUsername() == null
        || !currentStudent.getUsername().equals(currentUser.getUsername())) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
          "Bạn chỉ có thể cập nhật thông tin của chính mình");
    }

    if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
      if (!isValidEmailFormat(student.getEmail().trim())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Email không hợp lệ. Email phải có định dạng: example@domain.com");
      }
    }

    String normalizedStudentPhone = null;
    if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
      normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
      if (!isValidPhoneFormat(normalizedStudentPhone)) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
      }
    }

    if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
      UserDAO userDAO = new UserDAO();
      User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
      if (existingUserByEmail != null && currentStudent.getUsername() != null
          && !existingUserByEmail.getUsername().equals(currentStudent.getUsername())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Email đã được sử dụng bởi user khác: " + student.getEmail());
      }
    }

    String currentPhone = currentStudent.getPhone();
    String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
    boolean phoneChanged = normalizedStudentPhone != null
        && !normalizedStudentPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

    if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty() && phoneChanged) {
      UserDAO userDAO = new UserDAO();
      User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
      if (existingUserByPhone != null && currentStudent.getUsername() != null
          && !existingUserByPhone.getUsername().equals(currentStudent.getUsername())) {
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
            "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
      }
    }

    currentStudent.setEmail(student.getEmail());
    currentStudent.setPhone(phoneChanged ? normalizedStudentPhone : currentPhone);
    currentStudent.setEmergencyContact(student.getEmergencyContact());
    currentStudent.setEmergencyPhone(student.getEmergencyPhone());

    boolean ok = studentService.updateStudent(currentStudent);
    if (ok) {
      // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
      String existingSource = dataOriginHelper.getDataOrigin("student", currentStudent.getStudentId());
      if (existingSource != null) {
        dataOriginHelper.updateDataOriginTimestamp("student", currentStudent.getStudentId());
      }
      return Message.createSuccessResponse(Constants.ACTION_UPDATE_STUDENT, "Cập nhật thông tin thành công");
    }
    return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Lỗi khi cập nhật thông tin");
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
}
