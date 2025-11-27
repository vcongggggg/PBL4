package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;
import com.university.sms.model.User;
import com.university.sms.service.CourseService;

import java.util.List;
import java.util.logging.Level;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến khóa học
 */
public class CourseHandler {
  private static final Logger LOGGER = Logger.getLogger(CourseHandler.class.getName());

  private User currentUser;
  private final Supplier<String> clientSourceSupplier;
  private final DataOriginHelper dataOriginHelper;
  private final CourseService courseService;
  private final CourseRegistrationDAO courseRegistrationDAO;
  private final EnrollmentDAO enrollmentDAO;

  public CourseHandler(User currentUser,
      Supplier<String> clientSourceSupplier,
      DataOriginHelper dataOriginHelper,
      CourseService courseService) {
    this.currentUser = currentUser;
    this.clientSourceSupplier = clientSourceSupplier;
    this.dataOriginHelper = dataOriginHelper;
    this.courseService = courseService;
    this.courseRegistrationDAO = new CourseRegistrationDAO();
    this.enrollmentDAO = new EnrollmentDAO();
  }

  private void touchCourseDataOrigin(int courseId) {
    if (courseId <= 0) {
      return;
    }
    String existingSource = dataOriginHelper.getDataOrigin("course", courseId);
    if (existingSource != null) {
      dataOriginHelper.updateDataOriginTimestamp("course", courseId);
    }
  }

  private void touchCourseRegistrations(String courseCode) {
    if (courseCode == null || courseCode.isEmpty()) {
      return;
    }
    List<CourseRegistration> registrations = courseRegistrationDAO.findByCourse(courseCode);
    for (CourseRegistration registration : registrations) {
      int registrationId = registration.getRegistrationId();
      if (registrationId <= 0) {
        continue;
      }
      String existingSource = dataOriginHelper.getDataOrigin("course_registration", registrationId);
      if (existingSource == null) {
        dataOriginHelper.saveDataOrigin("course_registration", registrationId, getClientSource());
      } else {
        dataOriginHelper.updateDataOriginTimestamp("course_registration", registrationId);
      }
    }
  }

  private void touchCourseEnrollments(String courseCode) {
    if (courseCode == null || courseCode.isEmpty()) {
      return;
    }
    List<Enrollment> enrollments = enrollmentDAO.findByCourseCode(courseCode);
    for (Enrollment enrollment : enrollments) {
      int enrollmentId = enrollment.getEnrollmentId();
      if (enrollmentId <= 0) {
        continue;
      }
      String existingSource = dataOriginHelper.getDataOrigin("enrollment", enrollmentId);
      if (existingSource == null) {
        dataOriginHelper.saveDataOrigin("enrollment", enrollmentId, getClientSource());
      } else {
        dataOriginHelper.updateDataOriginTimestamp("enrollment", enrollmentId);
      }
    }
  }

  private String getClientSource() {
    return clientSourceSupplier != null ? clientSourceSupplier.get() : "UNKNOWN";
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetAllCourses(Message request) {
    try {
      LOGGER.info("Đang lấy danh sách tất cả khóa học...");
      var courses = courseService.getAllCourses();
      LOGGER.info("Tìm thấy " + courses.size() + " khóa học");
      String responseAction = request.getAction();
      Message response = Message.createSuccessResponse(responseAction, "Lấy danh sách khóa học thành công");
      response.addData(Constants.KEY_COURSES, courses);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách tất cả khóa học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
    }
  }

  public Message handleGetCourseInfo(Message request) {
    String courseCode = request.getData("courseCode", String.class);
    if (courseCode == null || courseCode.isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_INVALID_DATA);
    }

    var course = courseService.getCourseByCode(courseCode);
    if (course != null) {
      Message response = Message.createSuccessResponse(Constants.ACTION_GET_COURSE_INFO,
          "Lấy thông tin khóa học thành công");
      response.addData(Constants.KEY_COURSE, course);
      return response;
    }

    return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_COURSE_NOT_FOUND);
  }

  public Message handleAddCourse(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_UNAUTHORIZED);
    }

    com.university.sms.model.Course course = request.getData(Constants.KEY_COURSE,
        com.university.sms.model.Course.class);
    if (course == null) {
      return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_INVALID_DATA);
    }

    boolean ok = courseService.addCourse(course);
    if (ok) {
      // Chỉ lưu source khi admin thêm mới
      if (currentUser.getRole() == User.UserRole.ADMIN) {
        dataOriginHelper.saveDataOrigin("course", course.getCourseId(), getClientSource());
      }
      return Message.createSuccessResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_SUCCESS);
    }
    return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_DATABASE_ERROR);
  }

  public Message handleUpdateCourse(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_UNAUTHORIZED);
    }

    com.university.sms.model.Course course = request.getData(Constants.KEY_COURSE,
        com.university.sms.model.Course.class);
    if (course == null || course.getCourseId() <= 0) {
      return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_INVALID_DATA);
    }

    boolean ok = courseService.updateCourse(course);
    if (ok) {
      // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
      String existingSource = dataOriginHelper.getDataOrigin("course", course.getCourseId());
      if (existingSource != null) {
        dataOriginHelper.updateDataOriginTimestamp("course", course.getCourseId());
      }
      return Message.createSuccessResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_SUCCESS);
    }
    return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_DATABASE_ERROR);
  }

  public Message handleDeleteCourse(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_UNAUTHORIZED);
    }

    String courseCode = request.getData("courseCode", String.class);
    if (courseCode == null || courseCode.isEmpty()) {
      return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_INVALID_DATA);
    }

    com.university.sms.model.Course course = courseService.getCourseByCode(courseCode);
    if (course == null) {
      return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, "Không tìm thấy lớp học phần");
    }

    if (course.getCourseStatus() == com.university.sms.model.Course.CourseStatus.COMPLETED) {
      return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
          "Không thể hủy lớp học phần đã hoàn thành. Lớp đã kết thúc và cần lưu lịch sử.");
    }

    if (course.getCourseStatus() == com.university.sms.model.Course.CourseStatus.CANCELLED) {
      return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
          "Lớp học phần đã bị hủy trước đó.");
    }

    if (course.getCourseId() > 0) {
      String existingSource = dataOriginHelper.getDataOrigin("course", course.getCourseId());
      // Chỉ update timestamp nếu đã có source, không tạo mới source khi xóa
      // Không kiểm tra source cụ thể, chỉ cần có source là update timestamp
      if (existingSource != null) {
        dataOriginHelper.updateDataOriginTimestamp("course", course.getCourseId());
      }
    }

    boolean ok = courseService.deleteCourse(courseCode);
    if (ok) {
      LOGGER.info("Đã xóa/hủy khóa học thành công: " + courseCode + " bởi " + currentUser.getUsername());
      return Message.createSuccessResponse(Constants.ACTION_DELETE_COURSE, "Hủy lớp học phần thành công");
    }
    return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
        "Không thể hủy/xóa lớp học phần. Vui lòng thử lại.");
  }

  public Message handleOpenCourseRegistration(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
    }

    Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
    if (courseId == null) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_INVALID_DATA);
    }

    try {
      boolean opened = courseService.openRegistration(courseId);
      if (opened) {
        touchCourseDataOrigin(courseId);
        return Message.createSuccessResponse(request.getAction(), "Đã mở đăng ký cho lớp học phần.");
      }
      return Message.createErrorResponse(request.getAction(),
          "Lớp học phần đang trong trạng thái không thể mở đăng ký.");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi mở đăng ký khóa học", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleCloseCourseRegistration(Message request) {
    if (currentUser.getRole() != User.UserRole.ADMIN) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
    }

    Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
    if (courseId == null) {
      return Message.createErrorResponse(request.getAction(), Constants.MSG_INVALID_DATA);
    }

    Course course = courseService.getCourseById(courseId);
    if (course == null) {
      return Message.createErrorResponse(request.getAction(), "Không tìm thấy lớp học phần");
    }

    try {
      CourseService.RegistrationClosureResult result = courseService.closeRegistration(courseId);
      touchCourseDataOrigin(courseId);
      String courseCode = course.getCourseCode();
      if (courseCode != null && !courseCode.isEmpty()) {
        // Always ping course registrations so clients pick up status changes
        touchCourseRegistrations(courseCode);
        if (result.getFinalStatus() == Course.CourseStatus.ONGOING) {
          touchCourseEnrollments(courseCode);
        }
      }
      Message response = Message.createSuccessResponse(request.getAction(),
          result.getMessage() != null ? result.getMessage() : Constants.MSG_SUCCESS);
      response.addData("registrations", result.getRegistrations());
      response.addData("enrollments", result.getEnrollments());
      response.addData(Constants.KEY_STATUS, result.getFinalStatus());
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi đóng đăng ký khóa học", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }
}
