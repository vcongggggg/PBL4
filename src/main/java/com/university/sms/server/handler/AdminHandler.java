package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.FacultyDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.dao.UserDAO;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Student;
import com.university.sms.model.User;
import com.university.sms.service.ClassOpeningRequestService;
import com.university.sms.service.CourseRegistrationService;
import com.university.sms.service.StudentService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến quản trị viên
 */
public class AdminHandler {
  private static final Logger LOGGER = Logger.getLogger(AdminHandler.class.getName());

  private User currentUser;
  private final String clientSource;
  private final DataOriginHelper dataOriginHelper;
  private final StudentService studentService;
  private final ClassOpeningRequestService classRequestService;
  private final CourseRegistrationService registrationService;

  public AdminHandler(User currentUser,
      String clientSource,
      DataOriginHelper dataOriginHelper,
      StudentService studentService,
      ClassOpeningRequestService classRequestService,
      CourseRegistrationService registrationService) {
    this.currentUser = currentUser;
    this.clientSource = clientSource;
    this.dataOriginHelper = dataOriginHelper;
    this.studentService = studentService;
    this.classRequestService = classRequestService;
    this.registrationService = registrationService;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleAddTeacher(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền thêm giảng viên");
      }

      String username = request.getData("username", String.class);
      String password = request.getData("password", String.class);
      String fullName = request.getData("fullName", String.class);
      String email = request.getData("email", String.class);
      String phone = request.getData("phone", String.class);
      String address = request.getData("address", String.class);
      String facultyCode = request.getData("facultyCode", String.class);

      if (username == null || username.trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu tên đăng nhập");
      }
      if (password == null || password.trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu mật khẩu");
      }
      if (fullName == null || fullName.trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu họ tên");
      }

      if (password.length() < 6) {
        return Message.createErrorResponse(request.getAction(),
            "Mật khẩu phải có ít nhất 6 ký tự");
      }

      if (email != null && !email.trim().isEmpty()) {
        if (!isValidEmailFormat(email.trim())) {
          return Message.createErrorResponse(request.getAction(),
              "Email không hợp lệ. Email phải có định dạng: example@domain.com");
        }
      }

      UserDAO userDAO = new UserDAO();
      User existingUser = userDAO.findByUsername(username);
      if (existingUser != null) {
        return Message.createErrorResponse(request.getAction(),
            "Tên đăng nhập đã tồn tại: " + username);
      }

      if (email != null && !email.trim().isEmpty()) {
        User existingUserByEmail = userDAO.findByEmail(email.trim());
        if (existingUserByEmail != null) {
          return Message.createErrorResponse(request.getAction(),
              "Email đã được sử dụng bởi user khác: " + email);
        }
      }

      String normalizedPhone = null;
      if (phone != null && !phone.trim().isEmpty()) {
        normalizedPhone = normalizePhoneNumber(phone.trim());
        if (!isValidPhoneFormat(normalizedPhone)) {
          return Message.createErrorResponse(request.getAction(),
              "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
        }
      }

      if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
        User existingUserByPhone = userDAO.findByPhone(normalizedPhone);
        if (existingUserByPhone != null) {
          return Message.createErrorResponse(request.getAction(),
              "Số điện thoại đã được sử dụng bởi user khác: " + normalizedPhone);
        }
      }

      if (facultyCode != null && !facultyCode.trim().isEmpty()) {
        FacultyDAO facultyDAO = new FacultyDAO();
        com.university.sms.model.Faculty faculty = facultyDAO.findByCode(facultyCode.trim());
        if (faculty == null) {
          return Message.createErrorResponse(request.getAction(),
              "Mã khoa không tồn tại: " + facultyCode);
        }
      }

      User newTeacher = new User();
      newTeacher.setUsername(username.trim());
      newTeacher.setPassword(password);
      newTeacher.setFullName(fullName.trim());
      newTeacher.setEmail(email != null ? email.trim() : null);
      newTeacher.setPhone(normalizedPhone);
      newTeacher.setAddress(address != null ? address.trim() : null);
      newTeacher.setFacultyCode(facultyCode != null && !facultyCode.trim().isEmpty() ? facultyCode.trim() : null);
      newTeacher.setRole(User.UserRole.TEACHER);
      newTeacher.setActive(true);

      boolean success = userDAO.addUser(newTeacher);

      if (success) {
        // Chỉ lưu source khi admin thêm mới (đã kiểm tra role ở đầu method)
        if (newTeacher.getUserId() > 0) {
          dataOriginHelper.saveDataOrigin("user", newTeacher.getUserId(), clientSource);
        }
        LOGGER.info("Teacher added: " + username + " by " + currentUser.getUsername());
        return Message.createSuccessResponse(request.getAction(), "Thêm giảng viên thành công");
      } else {
        return Message.createErrorResponse(request.getAction(),
            "Không thể thêm giảng viên. Tên đăng nhập có thể đã tồn tại.");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error adding teacher", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetAllTeachers(Message request) {
    try {
      UserDAO userDAO = new UserDAO();
      List<User> teachers = userDAO.findByRole(User.UserRole.TEACHER);

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + teachers.size() + " teachers");
      response.addData("teachers", teachers);

      LOGGER.info("Retrieved " + teachers.size() + " teachers");
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting teachers", e);
      return Message.createErrorResponse(request.getAction(),
          "Error retrieving teachers: " + e.getMessage());
    }
  }

  public Message handleSearchTeachers(Message request) {
    try {
      String keyword = request.getData("keyword", String.class);
      if (keyword == null || keyword.trim().isEmpty()) {
        return handleGetAllTeachers(request);
      }

      UserDAO userDAO = new UserDAO();
      List<User> allTeachers = userDAO.findByRole(User.UserRole.TEACHER);
      List<User> filteredTeachers = new ArrayList<>();

      String lowerKeyword = keyword.toLowerCase();
      for (User teacher : allTeachers) {
        if ((teacher.getFullName() != null && teacher.getFullName().toLowerCase().contains(lowerKeyword)) ||
            (teacher.getUsername() != null && teacher.getUsername().toLowerCase().contains(lowerKeyword)) ||
            (teacher.getEmail() != null && teacher.getEmail().toLowerCase().contains(lowerKeyword))) {
          filteredTeachers.add(teacher);
        }
      }

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + filteredTeachers.size() + " teachers");
      response.addData("teachers", filteredTeachers);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error searching teachers", e);
      return Message.createErrorResponse(request.getAction(),
          "Error searching teachers: " + e.getMessage());
    }
  }

  public Message handleUpdateTeacher(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền cập nhật giảng viên");
      }

      Integer userId = request.getData("userId", Integer.class);
      if (userId == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu ID giảng viên");
      }

      UserDAO userDAO = new UserDAO();
      User teacher = userDAO.findById(userId);
      if (teacher == null || teacher.getRole() != User.UserRole.TEACHER) {
        return Message.createErrorResponse(request.getAction(), "Không tìm thấy giảng viên");
      }

      String fullName = request.getData("fullName", String.class);
      String email = request.getData("email", String.class);
      String phone = request.getData("phone", String.class);
      String address = request.getData("address", String.class);
      String password = request.getData("password", String.class);
      String facultyCode = request.getData("facultyCode", String.class);

      if (facultyCode != null && !facultyCode.trim().isEmpty()) {
        FacultyDAO facultyDAO = new FacultyDAO();
        com.university.sms.model.Faculty faculty = facultyDAO.findByCode(facultyCode.trim());
        if (faculty == null) {
          return Message.createErrorResponse(request.getAction(),
              "Mã khoa không tồn tại: " + facultyCode);
        }
      }

      if (email != null && !email.trim().isEmpty()) {
        if (!isValidEmailFormat(email.trim())) {
          return Message.createErrorResponse(request.getAction(),
              "Email không hợp lệ. Email phải có định dạng: example@domain.com");
        }
      }

      if (email != null && !email.trim().isEmpty()) {
        User existingUserByEmail = userDAO.findByEmail(email.trim());
        if (existingUserByEmail != null && existingUserByEmail.getUserId() != teacher.getUserId()) {
          return Message.createErrorResponse(request.getAction(),
              "Email đã được sử dụng bởi user khác: " + email);
        }
      }

      String normalizedPhone = null;
      if (phone != null && !phone.trim().isEmpty()) {
        normalizedPhone = normalizePhoneNumber(phone.trim());
      }

      String currentPhone = teacher.getPhone();
      String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
      boolean phoneChanged = normalizedPhone != null
          && !normalizedPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

      if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
        if (phoneChanged && !isValidPhoneFormat(normalizedPhone)) {
          return Message.createErrorResponse(request.getAction(),
              "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
        }
      }

      if (normalizedPhone != null && !normalizedPhone.isEmpty() && phoneChanged) {
        User existingUserByPhone = userDAO.findByPhone(normalizedPhone);
        if (existingUserByPhone != null && existingUserByPhone.getUserId() != teacher.getUserId()) {
          return Message.createErrorResponse(request.getAction(),
              "Số điện thoại đã được sử dụng bởi user khác: " + normalizedPhone);
        }
      }

      if (fullName != null)
        teacher.setFullName(fullName);
      if (email != null)
        teacher.setEmail(email);
      if (phone != null) {
        teacher.setPhone(phoneChanged ? normalizedPhone : currentPhone);
      }
      if (address != null)
        teacher.setAddress(address);
      if (facultyCode != null) {
        teacher.setFacultyCode(facultyCode.trim().isEmpty() ? null : facultyCode.trim());
      }

      boolean success = userDAO.updateUser(teacher);

      if (password != null && !password.isEmpty()) {
        userDAO.changePassword(teacher.getUsername(), password);
      }

      if (success) {
        // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
        if (teacher.getUserId() > 0) {
          String existingSource = dataOriginHelper.getDataOrigin("user", teacher.getUserId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("user", teacher.getUserId());
          }
        }
        LOGGER.info("Teacher updated: " + teacher.getUsername() + " by " + currentUser.getUsername());
        return Message.createSuccessResponse(request.getAction(), "Cập nhật giảng viên thành công");
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể cập nhật giảng viên");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error updating teacher", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleDeleteTeacher(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa giảng viên");
      }

      Integer userId = request.getData("userId", Integer.class);
      if (userId == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu ID giảng viên");
      }

      UserDAO userDAO = new UserDAO();
      User teacher = userDAO.findById(userId);
      if (teacher == null || teacher.getRole() != User.UserRole.TEACHER) {
        return Message.createErrorResponse(request.getAction(), "Không tìm thấy giảng viên");
      }

      String teacherUsername = teacher.getUsername();

      CourseDAO courseDAO = new CourseDAO();
      List<Course> teacherCourses = courseDAO.findByTeacherUsername(teacherUsername);

      List<ClassOpeningRequest> teacherRequests = classRequestService.getRequestsByTeacher(teacherUsername);

      if (!teacherCourses.isEmpty()) {
        String courseCodes = teacherCourses.stream()
            .map(Course::getCourseCode)
            .collect(java.util.stream.Collectors.joining(", "));
        String errorMsg = "Không thể vô hiệu hóa giảng viên vì vẫn còn lớp đang dạy (trạng thái ongoing). "
            + "Các lớp chưa kết thúc: " + courseCodes;
        return Message.createErrorResponse(request.getAction(), errorMsg);
      }

      List<Course> allTeacherCourses = courseDAO.findAllByTeacherUsername(teacherUsername);
      List<Course> planningCourses = allTeacherCourses.stream()
          .filter(course -> course.getCourseStatus() == Course.CourseStatus.PLANNING)
          .collect(java.util.stream.Collectors.toList());

      int cancelledCourses = 0;
      for (Course planningCourse : planningCourses) {
        try {
          boolean updated = courseDAO.updateCourseStatus(planningCourse.getCourseId(),
              Course.CourseStatus.CANCELLED);
          if (updated) {
            cancelledCourses++;
            LOGGER.info("Auto-cancelled planning course " + planningCourse.getCourseCode()
                + " for teacher " + teacherUsername);
          }
        } catch (Exception ex) {
          LOGGER.log(Level.WARNING,
              "Error auto-cancelling planning course " + planningCourse.getCourseCode()
                  + " for teacher " + teacherUsername,
              ex);
        }
      }

      List<ClassOpeningRequest> pendingRequests = teacherRequests.stream()
          .filter(req -> req.getRequestStatus() == ClassOpeningRequest.RequestStatus.PENDING)
          .collect(java.util.stream.Collectors.toList());

      int rejectedRequests = 0;
      for (ClassOpeningRequest pending : pendingRequests) {
        try {
          boolean rejected = classRequestService.rejectRequest(
              pending.getRequestId(),
              currentUser.getUsername(),
              "Tự động từ chối do vô hiệu hóa giảng viên");
          if (rejected) {
            rejectedRequests++;
            LOGGER.info("Auto-rejected class opening request " + pending.getRequestId()
                + " for teacher " + teacherUsername);
          }
        } catch (Exception ex) {
          LOGGER.log(Level.WARNING,
              "Error auto-rejecting class opening request " + pending.getRequestId()
                  + " for teacher " + teacherUsername,
              ex);
        }
      }

      if (teacher.getUserId() > 0) {
        String existingSource = dataOriginHelper.getDataOrigin("user", teacher.getUserId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("user", teacher.getUserId());
        }
      }

      boolean success = userDAO.deactivateUser(teacherUsername);

      if (success) {
        LOGGER.info("Teacher deactivated: " + teacherUsername + " by " + currentUser.getUsername());
        String successMessage = "Vô hiệu hóa giảng viên thành công";
        if (cancelledCourses > 0) {
          successMessage += ". Đã tự động hủy " + cancelledCourses + " lớp đang ở trạng thái planning.";
        }
        if (rejectedRequests > 0) {
          successMessage += " Đã tự động từ chối " + rejectedRequests + " yêu cầu mở lớp đang chờ.";
        }
        return Message.createSuccessResponse(request.getAction(), successMessage);
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể vô hiệu hóa giảng viên");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error deactivating teacher", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleActivateUser(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền kích hoạt người dùng");
      }

      Integer userId = request.getData("userId", Integer.class);
      if (userId == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu ID người dùng");
      }

      UserDAO userDAO = new UserDAO();
      User user = userDAO.findById(userId);
      if (user == null) {
        return Message.createErrorResponse(request.getAction(), "Không tìm thấy người dùng");
      }

      boolean success = userDAO.activateUser(userId);

      if (success) {
        User activatedUser = userDAO.findById(userId);
        if (activatedUser != null) {
          // Khi activate: chỉ update timestamp nếu đã có source, không tạo mới source
          String existingSource = dataOriginHelper.getDataOrigin("user", activatedUser.getUserId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("user", activatedUser.getUserId());
          }
        }

        String userType = user.getRole() == User.UserRole.TEACHER ? "giảng viên"
            : user.getRole() == User.UserRole.STUDENT ? "sinh viên" : "người dùng";
        LOGGER.info("User activated: " + userId + " (" + user.getUsername() + ", " + userType + ") by "
            + currentUser.getUsername());

        Message response = Message.createSuccessResponse(request.getAction(),
            "Kích hoạt " + userType + " thành công");
        response.addData("user", activatedUser);

        if (user.getRole() == User.UserRole.STUDENT) {
          StudentDAO studentDAO = new StudentDAO();
          Student student = studentDAO.findByUsername(user.getUsername());
          if (student != null) {
            studentDAO.updateStudentStatus(student.getStudentId(), Student.StudentStatus.ACTIVE);
            // Khi activate: chỉ update timestamp nếu đã có source, không tạo mới source
            String existingSource = dataOriginHelper.getDataOrigin("student", student.getStudentId());
            if (existingSource != null) {
              dataOriginHelper.updateDataOriginTimestamp("student", student.getStudentId());
            }
            student = studentDAO.findByUsername(user.getUsername());
            if (student != null) {
              LOGGER.info("Student status updated to ACTIVE: " + student.getStudentCode());
              student.setStudentStatus(Student.StudentStatus.ACTIVE);
              response.addData(Constants.KEY_STUDENT, student);
            }
          }
        }

        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể kích hoạt người dùng");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error activating user", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetAllTeachersIncludeInactive(Message request) {
    try {
      UserDAO userDAO = new UserDAO();
      List<User> teachers = userDAO.findByRoleIncludeInactive(User.UserRole.TEACHER);

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + teachers.size() + " teachers (include inactive)");
      response.addData("teachers", teachers);

      LOGGER.info("Retrieved " + teachers.size() + " teachers (include inactive)");
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting teachers (include inactive)", e);
      return Message.createErrorResponse(request.getAction(),
          "Error retrieving teachers: " + e.getMessage());
    }
  }

  public Message handleGetAllStudentsIncludeInactive(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
    }

    try {
      StudentDAO studentDAO = new StudentDAO();
      List<Student> students = studentDAO.findAllIncludeInactive();
      Message response = Message.createSuccessResponse(request.getAction(), "Lấy danh sách thành công");
      response.addData(Constants.KEY_STUDENTS, students);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách tất cả sinh viên (bao gồm không hoạt động): " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
    }
  }

  public Message handleGetStudentsPaged(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
    }

    try {
      Integer page = request.getData(Constants.KEY_PAGE, Integer.class);
      Integer pageSize = request.getData(Constants.KEY_PAGE_SIZE, Integer.class);
      Boolean includeInactive = request.getData(Constants.KEY_INCLUDE_INACTIVE, Boolean.class);

      if (page == null || page < 1) {
        page = 1;
      }
      if (pageSize == null || pageSize <= 0) {
        pageSize = 200;
      }

      boolean includeInactiveFlag = includeInactive != null && includeInactive;

      StudentService.StudentPageResult result = studentService.getStudentsPaged(page, pageSize,
          includeInactiveFlag);

      Message response = Message.createSuccessResponse(request.getAction(), Constants.MSG_SUCCESS);
      response.addData(Constants.KEY_STUDENTS, result.getStudents());
      response.addData(Constants.KEY_TOTAL, result.getTotal());
      response.addData(Constants.KEY_PAGE, result.getPage());
      response.addData(Constants.KEY_PAGE_SIZE, result.getPageSize());
      response.addData(Constants.KEY_INCLUDE_INACTIVE, result.isIncludeInactive());
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách sinh viên phân trang", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
    }
  }

  public Message handleApproveClassRequest(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ Admin mới có quyền duyệt yêu cầu");
      }

      Integer requestId = request.getData(Constants.KEY_REQUEST_ID, Integer.class);
      if (requestId == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu thông tin request ID");
      }

      String adminUsername = currentUser.getUsername();
      String note = request.getData(Constants.KEY_NOTE, String.class);

      boolean success = classRequestService.approveRequest(requestId, adminUsername, note);

      if (success) {
        LOGGER.info("Admin " + currentUser.getUsername() + " approved request " + requestId);
        ClassOpeningRequest updatedRequest = classRequestService.getRequestById(requestId);
        // Khi approve: chỉ update timestamp nếu đã có source, không tạo mới source
        if (updatedRequest != null && updatedRequest.getRequestId() > 0) {
          String existingSource = dataOriginHelper.getDataOrigin("class_opening_request",
              updatedRequest.getRequestId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("class_opening_request", updatedRequest.getRequestId());
          }
        }
        Message response = Message.createSuccessResponse(request.getAction(), "Đã duyệt yêu cầu thành công");
        if (updatedRequest != null) {
          response.addData("request", updatedRequest);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể duyệt yêu cầu");
      }
    } catch (IllegalStateException e) {
      LOGGER.warning("Cannot approve request: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), e.getMessage());
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi duyệt yêu cầu mở lớp: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleRejectClassRequest(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ Admin mới có quyền từ chối yêu cầu");
      }

      Integer requestId = request.getData(Constants.KEY_REQUEST_ID, Integer.class);
      if (requestId == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu thông tin request ID");
      }

      String adminUsername = currentUser.getUsername();
      String reason = request.getData(Constants.KEY_REASON, String.class);

      if (reason == null || reason.trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Vui lòng nhập lý do từ chối");
      }

      boolean success = classRequestService.rejectRequest(requestId, adminUsername, reason);

      if (success) {
        LOGGER.info("Admin " + currentUser.getUsername() + " rejected request " + requestId);
        ClassOpeningRequest updatedRequest = classRequestService.getRequestById(requestId);
        // Khi reject: chỉ update timestamp nếu đã có source, không tạo mới source
        if (updatedRequest != null && updatedRequest.getRequestId() > 0) {
          String existingSource = dataOriginHelper.getDataOrigin("class_opening_request",
              updatedRequest.getRequestId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("class_opening_request", updatedRequest.getRequestId());
          }
        }
        Message response = Message.createSuccessResponse(request.getAction(), "Đã từ chối yêu cầu");
        if (updatedRequest != null) {
          response.addData("request", updatedRequest);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Không thể từ chối yêu cầu");
      }
    } catch (IllegalStateException e) {
      LOGGER.warning("Cannot reject request: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), e.getMessage());
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi từ chối yêu cầu mở lớp: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetAllClassRequests(Message request) {
    try {
      List<ClassOpeningRequest> requests = classRequestService.getAllRequests();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_CLASS_REQUESTS, requests);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy all class requests: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetClassRequestById(Message request) {
    try {
      int requestId = (Integer) request.getData(Constants.KEY_REQUEST_ID);
      ClassOpeningRequest classRequest = classRequestService.getRequestById(requestId);

      if (classRequest == null) {
        return Message.createErrorResponse(request.getAction(), "Request not found");
      }

      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_CLASS_REQUEST, classRequest);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy class request by ID: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetClassRequestStats(Message request) {
    try {
      ClassOpeningRequestService.RequestStatistics stats = classRequestService.getStatistics();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_STATISTICS, stats);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy class request stats: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetAllRegistrations(Message request) {
    try {
      List<CourseRegistration> registrations = registrationService.getAllRegistrations();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_REGISTRATIONS, registrations);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy all registrations: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetRegistrationById(Message request) {
    try {
      int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
      CourseRegistration registration = registrationService.getRegistrationById(registrationId);

      if (registration == null) {
        return Message.createErrorResponse(request.getAction(), "Registration not found");
      }

      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_REGISTRATION, registration);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy registration by ID: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleValidateRegistration(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(),
            "Student code and course code are required");
      }

      CourseRegistrationService.RegistrationValidation validation = registrationService
          .validateRegistration(studentCode, courseCode);

      Message response = Message.createSuccessResponse(request.getAction(), validation.getMessage());
      response.addData("valid", validation.isValid());
      response.addData("message", validation.getMessage());
      return response;
    } catch (Exception e) {
      LOGGER.severe("Error validating registration: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetStudentCredits(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String academicYear = request.getData(Constants.KEY_ACADEMIC_YEAR, String.class);
      Integer semester = request.getData(Constants.KEY_SEMESTER, Integer.class);

      if (studentCode == null || studentCode.isEmpty() || academicYear == null || semester == null) {
        return Message.createErrorResponse(request.getAction(),
            "Student code, academic year, and semester are required");
      }

      int credits = registrationService.getStudentCredits(studentCode, academicYear, semester);

      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData("credits", credits);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy student credits: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetRegistrationStats(Message request) {
    try {
      CourseRegistrationService.RegistrationStatistics stats = registrationService.getStatistics();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_STATISTICS, stats);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy registration stats: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
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
}
