package com.university.sms.server.handler;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.FacultyDAO;
import com.university.sms.dao.SubjectDAO;
import com.university.sms.model.User;
import com.university.sms.service.SubjectService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler xử lý các action liên quan đến môn học
 */
public class SubjectHandler {
  private static final Logger LOGGER = Logger.getLogger(SubjectHandler.class.getName());

  private User currentUser;
  private final String clientSource;
  private final DataOriginHelper dataOriginHelper;
  private final SubjectService subjectService;

  public SubjectHandler(User currentUser,
      String clientSource,
      DataOriginHelper dataOriginHelper,
      SubjectService subjectService) {
    this.currentUser = currentUser;
    this.clientSource = clientSource;
    this.dataOriginHelper = dataOriginHelper;
    this.subjectService = subjectService;
  }

  public void updateCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Message handleGetSubjects(Message request) {
    try {
      List<com.university.sms.model.Subject> subjects = subjectService.getAllSubjects();

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + subjects.size() + " subjects");
      response.addData(Constants.KEY_SUBJECTS, subjects);

      LOGGER.info("Retrieved " + subjects.size() + " subjects");
      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error getting subjects", e);
      return Message.createErrorResponse(request.getAction(),
          "Error retrieving subjects: " + e.getMessage());
    }
  }

  public Message handleGetAllSubjects(Message request) {
    return handleGetSubjects(request);
  }

  public Message handleSearchSubjects(Message request) {
    try {
      String keyword = request.getData("keyword", String.class);
      if (keyword == null || keyword.trim().isEmpty()) {
        return handleGetAllSubjects(request);
      }

      List<com.university.sms.model.Subject> allSubjects = subjectService.getAllSubjects();
      List<com.university.sms.model.Subject> filteredSubjects = new java.util.ArrayList<>();

      String lowerKeyword = keyword.toLowerCase();
      for (com.university.sms.model.Subject subject : allSubjects) {
        if ((subject.getSubjectName() != null && subject.getSubjectName().toLowerCase().contains(lowerKeyword))
            ||
            (subject.getSubjectCode() != null
                && subject.getSubjectCode().toLowerCase().contains(lowerKeyword))) {
          filteredSubjects.add(subject);
        }
      }

      Message response = Message.createSuccessResponse(request.getAction(),
          "Found " + filteredSubjects.size() + " subjects");
      response.addData("subjects", filteredSubjects);

      return response;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error searching subjects", e);
      return Message.createErrorResponse(request.getAction(),
          "Error searching subjects: " + e.getMessage());
    }
  }

  public Message handleAddSubject(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền thêm môn học");
      }

      com.university.sms.model.Subject subject = request.getData("subject",
          com.university.sms.model.Subject.class);
      if (subject == null) {
        return Message.createErrorResponse(request.getAction(), "Thiếu thông tin môn học");
      }

      if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu mã môn học");
      }
      if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu tên môn học");
      }
      if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu mã khoa");
      }

      FacultyDAO facultyDAO = new FacultyDAO();
      com.university.sms.model.Faculty faculty = facultyDAO.findByCode(subject.getFacultyCode());
      if (faculty == null) {
        return Message.createErrorResponse(request.getAction(),
            "Mã khoa không tồn tại: " + subject.getFacultyCode());
      }

      if (subject.getPrerequisiteSubjectCode() != null
          && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
        com.university.sms.model.Subject prerequisite = subjectService
            .getSubjectByCode(subject.getPrerequisiteSubjectCode());
        if (prerequisite == null) {
          return Message.createErrorResponse(request.getAction(),
              "Môn học tiên quyết không tồn tại: " + subject.getPrerequisiteSubjectCode());
        }
        String circularError = checkCircularPrerequisite(subject.getSubjectCode(),
            subject.getPrerequisiteSubjectCode());
        if (circularError != null) {
          return Message.createErrorResponse(request.getAction(), circularError);
        }
      }

      com.university.sms.model.Subject existing = subjectService.getSubjectByCode(subject.getSubjectCode());
      if (existing != null) {
        return Message.createErrorResponse(request.getAction(),
            "Mã môn học đã tồn tại: " + subject.getSubjectCode());
      }

      boolean success = subjectService.addSubject(subject);
      if (success) {
        // Chỉ lưu source khi admin thêm mới
        if (subject.getSubjectId() > 0 && currentUser.getRole() == User.UserRole.ADMIN) {
          dataOriginHelper.saveDataOrigin("subject", subject.getSubjectId(), clientSource);
        }
        LOGGER.info("Subject added: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
        return Message.createSuccessResponse(request.getAction(), "Thêm môn học thành công");
      } else {
        return Message.createErrorResponse(request.getAction(),
            "Không thể thêm môn học. Vui lòng kiểm tra lại thông tin.");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error adding subject", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleUpdateSubject(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền sửa môn học");
      }

      com.university.sms.model.Subject subject = request.getData("subject",
          com.university.sms.model.Subject.class);
      if (subject == null || subject.getSubjectId() <= 0) {
        return Message.createErrorResponse(request.getAction(), "Thiếu thông tin môn học");
      }

      if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu mã môn học");
      }
      if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu tên môn học");
      }
      if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Thiếu mã khoa");
      }

      FacultyDAO facultyDAO = new FacultyDAO();
      com.university.sms.model.Faculty faculty = facultyDAO.findByCode(subject.getFacultyCode());
      if (faculty == null) {
        return Message.createErrorResponse(request.getAction(),
            "Mã khoa không tồn tại: " + subject.getFacultyCode());
      }

      if (subject.getPrerequisiteSubjectCode() != null
          && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
        com.university.sms.model.Subject prerequisite = subjectService
            .getSubjectByCode(subject.getPrerequisiteSubjectCode());
        if (prerequisite == null) {
          return Message.createErrorResponse(request.getAction(),
              "Môn học tiên quyết không tồn tại: " + subject.getPrerequisiteSubjectCode());
        }
        String circularError = checkCircularPrerequisite(subject.getSubjectCode(),
            subject.getPrerequisiteSubjectCode());
        if (circularError != null) {
          return Message.createErrorResponse(request.getAction(), circularError);
        }
      }

      com.university.sms.model.Subject duplicate = subjectService.getSubjectByCode(subject.getSubjectCode());
      if (duplicate != null && duplicate.getSubjectId() != subject.getSubjectId()) {
        return Message.createErrorResponse(request.getAction(),
            "Mã môn học đã tồn tại: " + subject.getSubjectCode());
      }

      boolean success = subjectService.updateSubject(subject);
      if (success) {
        // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
        if (subject.getSubjectId() > 0) {
          String existingSource = dataOriginHelper.getDataOrigin("subject", subject.getSubjectId());
          if (existingSource != null) {
            dataOriginHelper.updateDataOriginTimestamp("subject", subject.getSubjectId());
          }
        }
        LOGGER.info("Subject updated: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
        return Message.createSuccessResponse(request.getAction(), "Cập nhật môn học thành công");
      } else {
        return Message.createErrorResponse(request.getAction(),
            "Không thể cập nhật môn học. Vui lòng kiểm tra lại thông tin.");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error updating subject", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  public Message handleDeleteSubject(Message request) {
    try {
      if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
        return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa môn học");
      }

      String subjectCode = request.getData("subjectCode", String.class);
      if (subjectCode == null || subjectCode.isEmpty()) {
        return Message.createErrorResponse(request.getAction(), "Subject code không hợp lệ");
      }

      CourseDAO courseDAO = new CourseDAO();
      List<com.university.sms.model.Course> subjectCourses = courseDAO.findBySubjectCode(subjectCode);

      if (!subjectCourses.isEmpty()) {
        String courseCodes = subjectCourses.stream()
            .map(com.university.sms.model.Course::getCourseCode)
            .collect(java.util.stream.Collectors.joining(", "));
        return Message.createErrorResponse(request.getAction(),
            "Không thể xóa môn học. Môn học này đang có " + subjectCourses.size() +
                " lớp học phần: " + courseCodes + ". Vui lòng xóa các lớp học phần trước.");
      }

      SubjectDAO subjectDAO = new SubjectDAO();
      List<com.university.sms.model.Subject> dependentSubjects = subjectDAO.findByPrerequisite(subjectCode);
      if (!dependentSubjects.isEmpty()) {
        String dependentCodes = dependentSubjects.stream()
            .map(com.university.sms.model.Subject::getSubjectCode)
            .collect(java.util.stream.Collectors.joining(", "));
        return Message.createErrorResponse(request.getAction(),
            "Không thể xóa môn học. Có " + dependentSubjects.size() +
                " môn học khác đang dùng môn này làm tiên quyết: " + dependentCodes +
                ". Vui lòng cập nhật hoặc xóa các môn học phụ thuộc trước.");
      }

      com.university.sms.model.Subject subject = subjectDAO.findByCode(subjectCode);
      if (subject != null && subject.getSubjectId() > 0) {
        String existingSource = dataOriginHelper.getDataOrigin("subject", subject.getSubjectId());
        if (existingSource != null) {
          dataOriginHelper.updateDataOriginTimestamp("subject", subject.getSubjectId());
        }
      }

      boolean success = subjectService.deleteSubject(subjectCode);
      if (success) {
        LOGGER.info("Subject deleted: " + subjectCode + " by " + currentUser.getUsername());
        return Message.createSuccessResponse(request.getAction(), "Xóa môn học thành công");
      } else {
        return Message.createErrorResponse(request.getAction(),
            "Không thể xóa môn học");
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error deleting subject", e);
      return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
    }
  }

  private String checkCircularPrerequisite(String subjectCode, String prerequisiteCode) {
    if (subjectCode == null || prerequisiteCode == null || subjectCode.equals(prerequisiteCode)) {
      return "Môn học không thể là môn học tiên quyết của chính nó";
    }

    java.util.Set<String> visited = new java.util.HashSet<>();
    java.util.List<String> path = new java.util.ArrayList<>();

    String currentCode = prerequisiteCode;
    visited.add(subjectCode);
    path.add(subjectCode);

    while (currentCode != null && !currentCode.trim().isEmpty()) {
      if (currentCode.equals(subjectCode)) {
        path.add(currentCode);
        return "Phát hiện vòng lặp tiên quyết: " + String.join(" -> ", path);
      }

      if (visited.contains(currentCode)) {
        path.add(currentCode);
        int loopStart = path.indexOf(currentCode);
        java.util.List<String> loopPath = new java.util.ArrayList<>(path.subList(loopStart, path.size()));
        loopPath.add(currentCode);
        return "Phát hiện vòng lặp tiên quyết: " + String.join(" -> ", loopPath);
      }

      visited.add(currentCode);
      path.add(currentCode);

      com.university.sms.model.Subject currentSubject = subjectService.getSubjectByCode(currentCode);
      if (currentSubject == null) {
        break;
      }

      currentCode = currentSubject.getPrerequisiteSubjectCode();
    }

    return null;
  }
}
