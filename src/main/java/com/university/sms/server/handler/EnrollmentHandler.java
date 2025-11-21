package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.model.User;
import com.university.sms.service.CourseService;
import com.university.sms.service.StudentService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến đăng ký học phần
 */
public class EnrollmentHandler {
  private static final Logger LOGGER = Logger.getLogger(EnrollmentHandler.class.getName());

  private User currentUser;
  private final String clientSource;
  private final DataOriginHelper dataOriginHelper;
  private final StudentService studentService;
  private final CourseService courseService;

  public EnrollmentHandler(User currentUser,
      String clientSource,
      DataOriginHelper dataOriginHelper,
      StudentService studentService,
      CourseService courseService) {
    this.currentUser = currentUser;
    this.clientSource = clientSource;
    this.dataOriginHelper = dataOriginHelper;
    this.studentService = studentService;
    this.courseService = courseService;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetEnrollments(Message request) {
    try {
      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

      if (currentUser.getRole() == User.UserRole.STUDENT) {
        var me = studentService.findByUsername(currentUser.getUsername());
        if (me == null) {
          return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS,
              Constants.MSG_STUDENT_NOT_FOUND);
        }
        var list = enrollmentDAO.findByStudentCode(me.getStudentCode());
        Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
        resp.addData(Constants.KEY_ENROLLMENTS, list);
        return resp;
      }

      String studentCode = request.getData("studentCode", String.class);
      if (studentCode == null || studentCode.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_INVALID_DATA);
      }
      var list = enrollmentDAO.findByStudentCode(studentCode);
      Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
      resp.addData(Constants.KEY_ENROLLMENTS, list);
      return resp;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy danh sách đăng ký học phần: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SERVER_ERROR);
    }
  }

  public Message handleEnrollCourse(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_INVALID_DATA);
      }

      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(
          studentCode, courseCode);
      boolean ok;
      com.university.sms.model.Enrollment enrollment = null;
      if (existing == null) {
        com.university.sms.model.Enrollment e = new com.university.sms.model.Enrollment();
        e.setStudentCode(studentCode);
        e.setCourseCode(courseCode);
        ok = enrollmentDAO.save(e);
        if (ok) {
          courseService.incrementCurrentStudents(courseCode);
          dataOriginHelper.saveDataOrigin("enrollment", e.getEnrollmentId(), clientSource);
          enrollment = enrollmentDAO.findByStudentAndCourse(studentCode, courseCode);
        }
      } else {
        ok = true;
        enrollment = existing;
      }
      if (ok) {
        Message response = Message.createSuccessResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SUCCESS);
        if (enrollment != null) {
          response.addData(Constants.KEY_ENROLLMENT, enrollment);
        }
        return response;
      }
      return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_DATABASE_ERROR);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi đăng ký khóa học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SERVER_ERROR);
    }
  }

  public Message handleDropCourse(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
      }

      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(
          studentCode, courseCode);
      if (existing == null) {
        return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
      }

      com.university.sms.model.Enrollment deletedEnrollment = existing;
      String existingSource = dataOriginHelper.getDataOrigin("enrollment", existing.getEnrollmentId());
      if ("CSV".equals(existingSource)) {
        dataOriginHelper.updateDataOriginTimestamp("enrollment", existing.getEnrollmentId());
      }
      boolean ok = enrollmentDAO.deleteEnrollment(existing.getEnrollmentId());
      if (ok) {
        courseService.decrementCurrentStudents(courseCode);
        Message response = Message.createSuccessResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_SUCCESS);
        response.addData(Constants.KEY_ENROLLMENT, deletedEnrollment);
        return response;
      }
      return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_DATABASE_ERROR);
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi hủy đăng ký khóa học: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_SERVER_ERROR);
    }
  }
}
