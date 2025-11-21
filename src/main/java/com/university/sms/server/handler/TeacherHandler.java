package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.UserDAO;
import com.university.sms.model.Course;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Grade;
import com.university.sms.model.User;
import com.university.sms.service.ClassOpeningRequestService;
import com.university.sms.service.CourseRegistrationService;
import com.university.sms.service.GradeService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến giáo viên
 */
public class TeacherHandler {
  private static final Logger LOGGER = Logger.getLogger(TeacherHandler.class.getName());

  private final ClassOpeningRequestService classRequestService;
  private final CourseRegistrationService registrationService;
  private final GradeService gradeService;
  private User currentUser;
  private final String clientSource;
  private final DataOriginHelper dataOriginHelper;

  public TeacherHandler(ClassOpeningRequestService classRequestService,
      CourseRegistrationService registrationService,
      GradeService gradeService,
      User currentUser,
      String clientSource,
      DataOriginHelper dataOriginHelper) {
    this.classRequestService = classRequestService;
    this.registrationService = registrationService;
    this.gradeService = gradeService;
    this.currentUser = currentUser;
    this.clientSource = clientSource;
    this.dataOriginHelper = dataOriginHelper;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetMyClassRequests(Message request) {
    try {
      String teacherUsername = request.getData("teacherUsername", String.class);
      if ((teacherUsername == null || teacherUsername.isEmpty()) && currentUser != null
          && currentUser.getRole() == User.UserRole.TEACHER) {
        teacherUsername = currentUser.getUsername();
      }
      if (teacherUsername == null || teacherUsername.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Teacher username is required");
      }
      List<ClassOpeningRequest> requests = classRequestService.getRequestsByTeacher(teacherUsername);
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_CLASS_REQUESTS, requests);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy teacher's class requests: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetPendingClassRequests(Message request) {
    try {
      List<ClassOpeningRequest> requests = classRequestService.getPendingRequests();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_CLASS_REQUESTS, requests);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy pending class requests: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleSubmitClassRequest(Message request) {
    try {
      ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
      boolean success = classRequestService.submitRequest(classRequest);

      if (success) {
        if (classRequest != null && classRequest.getRequestId() > 0) {
          dataOriginHelper.saveDataOrigin("class_opening_request", classRequest.getRequestId(), clientSource);
        }
        return Message.createSuccessResponse(request.getAction(), "Request submitted successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to submit request");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi gửi class request: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleUpdateClassRequest(Message request) {
    try {
      ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
      boolean success = classRequestService.updateRequest(classRequest);

      if (success) {
        if (classRequest != null && classRequest.getRequestId() > 0) {
          dataOriginHelper.saveDataOrigin("class_opening_request", classRequest.getRequestId(), clientSource);
        }
        return Message.createSuccessResponse(request.getAction(), "Request updated successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to update request");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi cập nhật class request: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleCancelClassRequest(Message request) {
    try {
      int requestId = (Integer) request.getData(Constants.KEY_REQUEST_ID);
      String teacherUsername = request.getData("teacherUsername", String.class);
      if ((teacherUsername == null || teacherUsername.isEmpty()) && currentUser != null
          && currentUser.getRole() == User.UserRole.TEACHER) {
        teacherUsername = currentUser.getUsername();
      }
      if (teacherUsername == null || teacherUsername.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Teacher username is required");
      }
      ClassOpeningRequest classRequest = classRequestService.getRequestById(requestId);
      if (classRequest != null && classRequest.getRequestId() > 0) {
        String existingSource = dataOriginHelper.getDataOrigin("class_opening_request", classRequest.getRequestId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("class_opening_request", classRequest.getRequestId());
        }
      }

      boolean success = classRequestService.cancelRequest(requestId, teacherUsername);

      if (success) {
        return Message.createSuccessResponse(request.getAction(), "Request cancelled successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to cancel request");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi hủy class request: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetCoursesByTeacher(Message request) {
    try {
      String teacherUsername = request.getData("teacherUsername", String.class);
      if ((teacherUsername == null || teacherUsername.isEmpty()) && currentUser != null
          && currentUser.getRole() == User.UserRole.TEACHER) {
        teacherUsername = currentUser.getUsername();
      }

      if (teacherUsername == null || teacherUsername.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Teacher username is required");
      }

      UserDAO userDAO = new UserDAO();
      User teacher = userDAO.findByUsername(teacherUsername);
      if (teacher == null || teacher.getRole() != User.UserRole.TEACHER) {
        return Message.createErrorResponse(request.getAction(), "Teacher not found");
      }

      CourseDAO courseDAO = new CourseDAO();
      List<Course> courses = courseDAO.findByTeacherUsername(teacher.getUsername());

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + courses.size() + " courses");
      response.addData(Constants.KEY_COURSES, courses);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting courses by teacher", e);
      return Message.createErrorResponse(request.getAction(),
          "Error retrieving courses: " + e.getMessage());
    }
  }

  public Message handleGetEnrollmentsByCourse(Message request) {
    try {
      String courseCode = request.getData("courseCode", String.class);
      if (courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Course code is required");
      }

      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      List<Enrollment> enrollments = enrollmentDAO.findByCourseCode(courseCode);

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + enrollments.size() + " enrollments");
      response.addData("enrollments", enrollments);

      LOGGER.info("Retrieved " + enrollments.size() + " enrollments for course " + courseCode);
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy enrollments theo khóa học", e);
      return Message.createErrorResponse(request.getAction(),
          "Error retrieving enrollments: " + e.getMessage());
    }
  }

  public Message handleGetStudentGrades(Message request) {
    try {
      String studentCode = request.getData("studentCode", String.class);
      String courseCode = request.getData("courseCode", String.class);

      if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(),
            "Student code and course code are required");
      }

      EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
      Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(studentCode, courseCode);

      if (enrollment == null) {
        return Message.createErrorResponse(request.getAction(), "Enrollment not found");
      }

      List<Grade> grades = gradeService.getGradesByStudentAndCourse(studentCode, courseCode);

      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_GRADES, grades);
      response.addData(Constants.KEY_ENROLLMENT, enrollment);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy điểm sinh viên: " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleAddGrade(Message request) {
    try {
      Grade grade = request.getData(Constants.KEY_GRADE, Grade.class);
      if (grade == null) {
        return Message.createErrorResponse(request.getAction(), "Grade data is required");
      }

      boolean result = gradeService.addGrade(grade);

      if (result) {
        Grade savedGrade = gradeService.getGradesByStudentAndCourse(
            grade.getStudentCode(), grade.getCourseCode())
            .stream()
            .filter(g -> g.getGradeType() == grade.getGradeType() &&
                g.getGradeName() != null &&
                g.getGradeName().equals(grade.getGradeName()))
            .findFirst()
            .orElse(grade);

        if (savedGrade != null && savedGrade.getGradeId() > 0) {
          dataOriginHelper.saveDataOrigin("grade", savedGrade.getGradeId(), clientSource);
        }

        Message response = Message.createSuccessResponse(request.getAction(), "Thêm điểm thành công");
        response.addData(Constants.KEY_GRADE, savedGrade);
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Thêm điểm thất bại");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi thêm điểm", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUpdateGrade(Message request) {
    try {
      Grade grade = request.getData(Constants.KEY_GRADE, Grade.class);
      if (grade == null) {
        return Message.createErrorResponse(request.getAction(), "Grade data is required");
      }

      boolean result = gradeService.updateGrade(grade);

      if (result) {
        Grade updatedGrade = gradeService.getGradeById(grade.getGradeId());
        if (updatedGrade != null && updatedGrade.getGradeId() > 0) {
          dataOriginHelper.saveDataOrigin("grade", updatedGrade.getGradeId(), clientSource);
        } else if (grade.getGradeId() > 0) {
          dataOriginHelper.saveDataOrigin("grade", grade.getGradeId(), clientSource);
        }
        Message response = Message.createSuccessResponse(request.getAction(), "Cập nhật điểm thành công");
        if (updatedGrade != null) {
          response.addData(Constants.KEY_GRADE, updatedGrade);
        } else {
          response.addData(Constants.KEY_GRADE, grade);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Cập nhật điểm thất bại");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật điểm", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleDeleteGrade(Message request) {
    try {
      Integer gradeId = request.getData(Constants.KEY_GRADE_ID, Integer.class);
      if (gradeId == null || gradeId <= 0) {
        return Message.createErrorResponse(request.getAction(), "Grade ID is required");
      }

      Grade deletedGrade = gradeService.getGradeById(gradeId);
      if (deletedGrade != null && deletedGrade.getGradeId() > 0) {
        String existingSource = dataOriginHelper.getDataOrigin("grade", deletedGrade.getGradeId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("grade", deletedGrade.getGradeId());
        }
      }
      boolean result = gradeService.deleteGrade(gradeId);

      if (result) {
        Message response = Message.createSuccessResponse(request.getAction(), "Xóa điểm thành công");
        if (deletedGrade != null) {
          response.addData(Constants.KEY_GRADE, deletedGrade);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Xóa điểm thất bại");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi xóa điểm", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetGrades(Message request) {
    try {
      String studentCode = request.getData(Constants.KEY_STUDENT_CODE, String.class);
      String courseCode = request.getData(Constants.KEY_COURSE_CODE, String.class);
      Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);

      List<Grade> grades;

      if (studentCode != null && courseCode != null) {
        grades = gradeService.getGradesByStudentAndCourse(studentCode, courseCode);
      } else if (enrollmentId != null) {
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
        if (enrollment != null) {
          grades = gradeService.getGradesByStudentAndCourse(
              enrollment.getStudentCode(), enrollment.getCourseCode());
        } else {
          return Message.createErrorResponse(request.getAction(), "Enrollment không tồn tại");
        }
      } else if (studentCode != null) {
        grades = gradeService.getGradesByStudent(studentCode);
      } else if (courseCode != null) {
        grades = gradeService.getGradesByCourse(courseCode);
      } else {
        return Message.createErrorResponse(request.getAction(),
            "student_code/course_code hoặc enrollment_id is required");
      }

      Message response = Message.createSuccessResponse(request.getAction(),
          "Lấy danh sách điểm thành công");
      response.addData(Constants.KEY_GRADES, grades);
      return response;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách điểm", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleCalculateFinalGrade(Message request) {
    try {
      String studentCode = request.getData(Constants.KEY_STUDENT_CODE, String.class);
      String courseCode = request.getData(Constants.KEY_COURSE_CODE, String.class);
      Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);

      if (studentCode == null || courseCode == null) {
        if (enrollmentId == null) {
          return Message.createErrorResponse(request.getAction(),
              "Student code and course code (or enrollment ID) are required");
        }

        EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
        if (enrollment == null) {
          return Message.createErrorResponse(request.getAction(), "Enrollment không tồn tại");
        }

        studentCode = enrollment.getStudentCode();
        courseCode = enrollment.getCourseCode();
      }

      boolean result = gradeService.finalizeCourseGrade(studentCode, courseCode);

      if (result) {
        return Message.createSuccessResponse(request.getAction(), "Tính điểm tổng kết thành công");
      } else {
        return Message.createErrorResponse(request.getAction(), "Tính điểm tổng kết thất bại");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Lỗi khi tính điểm tổng kết", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleGetPendingRegistrations(Message request) {
    try {
      List<CourseRegistration> registrations = registrationService.getPendingRegistrations();
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_REGISTRATIONS, registrations);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy pending registrations: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleGetCourseRegistrations(Message request) {
    try {
      String courseCode = request.getData("courseCode", String.class);
      if (courseCode == null || courseCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Course code is required");
      }
      List<CourseRegistration> registrations = registrationService.getRegistrationsByCourse(courseCode);
      Message response = Message.createSuccessResponse(request.getAction(), "Success");
      response.addData(Constants.KEY_REGISTRATIONS, registrations);
      return response;
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi lấy course registrations: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleApproveRegistration(Message request) {
    try {
      int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
      boolean success = registrationService.approveRegistration(registrationId);

      if (success) {
        CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
        CourseRegistration registration = registrationDAO.findById(registrationId);

        if (registration != null) {
          dataOriginHelper.saveDataOrigin("course_registration", registration.getRegistrationId(), clientSource);
        }

        Message response = Message.createSuccessResponse(request.getAction(),
            "Registration approved successfully");
        if (registration != null) {
          response.addData(Constants.KEY_REGISTRATION, registration);
        }
        return response;
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to approve registration");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi duyệt registration: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }

  public Message handleRejectRegistration(Message request) {
    try {
      int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
      String reason = (String) request.getData(Constants.KEY_REASON);

      boolean success = registrationService.rejectRegistration(registrationId, reason);

      if (success) {
        CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
        CourseRegistration registration = registrationDAO.findById(registrationId);
        if (registration != null) {
          dataOriginHelper.saveDataOrigin("course_registration", registration.getRegistrationId(), clientSource);
        }

        return Message.createSuccessResponse(request.getAction(), "Registration rejected successfully");
      } else {
        return Message.createErrorResponse(request.getAction(), "Failed to reject registration");
      }
    } catch (Exception e) {
      LOGGER.severe("Lỗi khi từ chối registration: " + e.getMessage());
      return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
    }
  }
}
