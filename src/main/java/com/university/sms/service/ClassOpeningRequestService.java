package com.university.sms.service;

import com.university.sms.dao.ClassOpeningRequestDAO;
import com.university.sms.dao.CourseDAO;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.ClassOpeningRequest.RequestStatus;
import com.university.sms.model.Course;
import com.university.sms.model.Notification;
import com.university.sms.model.Notification.Priority;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

public class ClassOpeningRequestService {
    private static final Logger LOGGER = Logger.getLogger(ClassOpeningRequestService.class.getName());

    private final ClassOpeningRequestDAO requestDAO;
    private final CourseDAO courseDAO;
    private final NotificationService notificationService;

    public ClassOpeningRequestService() {
        this.requestDAO = new ClassOpeningRequestDAO();
        this.courseDAO = new CourseDAO();
        this.notificationService = new NotificationService();
    }

    public List<ClassOpeningRequest> getAllRequests() {
        try {
            return requestDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Error getting all requests: " + e.getMessage());
            throw new RuntimeException("Failed to get requests", e);
        }
    }

    public ClassOpeningRequest getRequestById(int requestId) {
        try {
            return requestDAO.findById(requestId);
        } catch (Exception e) {
            LOGGER.severe("Error getting request by ID: " + e.getMessage());
            throw new RuntimeException("Failed to get request", e);
        }
    }

    public List<ClassOpeningRequest> getRequestsByTeacher(String teacherUsername) {
        try {
            return requestDAO.findByTeacher(teacherUsername);
        } catch (Exception e) {
            LOGGER.severe("Error getting requests by teacher: " + e.getMessage());
            throw new RuntimeException("Failed to get teacher requests", e);
        }
    }

    public List<ClassOpeningRequest> getPendingRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Error getting pending requests: " + e.getMessage());
            throw new RuntimeException("Failed to get pending requests", e);
        }
    }

    public List<ClassOpeningRequest> getApprovedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.APPROVED);
        } catch (Exception e) {
            LOGGER.severe("Error getting approved requests: " + e.getMessage());
            throw new RuntimeException("Failed to get approved requests", e);
        }
    }

    public List<ClassOpeningRequest> getRejectedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.REJECTED);
        } catch (Exception e) {
            LOGGER.severe("Error getting rejected requests: " + e.getMessage());
            throw new RuntimeException("Failed to get rejected requests", e);
        }
    }

    public boolean submitRequest(ClassOpeningRequest request) {
        try {
            // Validate request
            validateRequest(request);

            // Set initial status
            request.setRequestStatus(RequestStatus.PENDING);

            // Insert to database
            boolean success = requestDAO.insert(request);

            if (success) {
                LOGGER.info("Request submitted successfully by teacher: " + request.getTeacherUsername());
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error submitting request: " + e.getMessage());
            throw new RuntimeException("Failed to submit request: " + e.getMessage(), e);
        }
    }

    public boolean updateRequest(ClassOpeningRequest request) {
        try {
            // Check if request exists and is PENDING
            ClassOpeningRequest existing = requestDAO.findById(request.getRequestId());
            if (existing == null) {
                throw new IllegalArgumentException("Request not found");
            }

            if (existing.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Cannot update request that is not PENDING");
            }

            // Validate request
            validateRequest(request);

            // Update database
            boolean success = requestDAO.update(request);

            if (success) {
                LOGGER.info("Request updated successfully: " + request.getRequestId());
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error updating request: " + e.getMessage());
            throw new RuntimeException("Failed to update request: " + e.getMessage(), e);
        }
    }

    public boolean approveRequest(int requestId, String adminUsername, String note) {
        Course createdCourse = null;
        try {
            // Get request details
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Request not found");
            }

            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Can only approve PENDING requests");
            }

            // Validate teacher exists and is active
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User teacher = userDAO.findByUsername(request.getTeacherUsername());
            if (teacher == null) {
                throw new IllegalArgumentException("Teacher không tồn tại: " + request.getTeacherUsername());
            }
            if (!teacher.isActive()) {
                throw new IllegalStateException("Không thể duyệt yêu cầu: Giảng viên đã bị vô hiệu hóa");
            }

            // Validate subject exists
            com.university.sms.dao.SubjectDAO subjectDAO = new com.university.sms.dao.SubjectDAO();
            com.university.sms.model.Subject subject = subjectDAO.findByCode(request.getSubjectCode());
            if (subject == null) {
                throw new IllegalArgumentException("Môn học không tồn tại: " + request.getSubjectCode());
            }

            // Create corresponding course
            Course course = createCourseFromRequest(request);

            // Check for duplicate course code before creating (race condition protection)
            Course existingCourse = courseDAO.findByCourseCode(course.getCourseCode());
            if (existingCourse != null) {
                throw new IllegalStateException("Course code đã tồn tại: " + course.getCourseCode() +
                        ". Có thể yêu cầu đã được duyệt bởi admin khác.");
            }

            boolean courseCreated = courseDAO.addCourse(course);
            if (!courseCreated) {
                throw new RuntimeException("Failed to create course for approved request");
            }
            createdCourse = course; // Track created course for rollback

            // Approve the request
            boolean success = requestDAO.approve(requestId, adminUsername, note, course.getCourseCode());

            if (!success) {
                // Rollback: Delete the course if approve request failed
                if (createdCourse != null && createdCourse.getCourseId() > 0) {
                    try {
                        courseDAO.deleteCourse(createdCourse.getCourseId());
                        LOGGER.warning(
                                "Rolled back course creation due to failed approval: " + createdCourse.getCourseCode());
                    } catch (Exception rollbackEx) {
                        LOGGER.severe("Failed to rollback course creation: " + rollbackEx.getMessage());
                    }
                }
                throw new RuntimeException("Failed to approve request after creating course");
            }

            LOGGER.info(
                    "Request approved by admin: " + adminUsername + ", Course Code: " + course.getCourseCode());

            // Gửi notification cho teacher
            try {
                Notification notification = new Notification();
                notification.setTitle("Yêu cầu mở lớp đã được duyệt");
                notification.setContent("Yêu cầu mở lớp của bạn đã được duyệt. Mã lớp học phần: "
                        + course.getCourseCode()
                        + (note != null && !note.trim().isEmpty() ? ". Ghi chú: " + note : ""));
                notification.setSenderUsername(adminUsername);
                notification.setTargetType(Notification.TargetType.STUDENT); // Gửi cho teacher (dùng username)
                notification.setTargetCode(request.getTeacherUsername());
                notification.setPriority(Priority.HIGH);

                // Set expires at 30 days from now
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 30);
                notification.setExpiresAt(new Timestamp(cal.getTimeInMillis()));

                notificationService.createNotification(notification);
                LOGGER.info("Notification sent to teacher: " + request.getTeacherUsername());
            } catch (Exception e) {
                // Log error but don't fail the approval
                LOGGER.warning("Failed to send notification to teacher: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            // Rollback: Delete the course if any error occurred
            if (createdCourse != null && createdCourse.getCourseId() > 0) {
                try {
                    courseDAO.deleteCourse(createdCourse.getCourseId());
                    LOGGER.warning("Rolled back course creation due to error: " + createdCourse.getCourseCode());
                } catch (Exception rollbackEx) {
                    LOGGER.severe("Failed to rollback course creation: " + rollbackEx.getMessage());
                }
            }
            LOGGER.severe("Error approving request: " + e.getMessage());
            throw new RuntimeException("Failed to approve request: " + e.getMessage(), e);
        }
    }

    public boolean rejectRequest(int requestId, String adminUsername, String reason) {
        try {
            // Check if request exists and is PENDING
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Request not found");
            }

            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Can only reject PENDING requests");
            }

            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required");
            }

            // Reject the request
            boolean success = requestDAO.reject(requestId, adminUsername, reason);

            if (success) {
                LOGGER.info("Request rejected by admin: " + adminUsername);

                // Gửi notification cho teacher
                try {
                    Notification notification = new Notification();
                    notification.setTitle("Yêu cầu mở lớp đã bị từ chối");
                    notification.setContent("Yêu cầu mở lớp của bạn đã bị từ chối. Lý do: " + reason);
                    notification.setSenderUsername(adminUsername);
                    notification.setTargetType(Notification.TargetType.STUDENT); // Gửi cho teacher
                    notification.setTargetCode(request.getTeacherUsername());
                    notification.setPriority(Priority.MEDIUM);

                    // Set expires at 30 days from now
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 30);
                    notification.setExpiresAt(new Timestamp(cal.getTimeInMillis()));

                    notificationService.createNotification(notification);
                    LOGGER.info("Notification sent to teacher: " + request.getTeacherUsername());
                } catch (Exception e) {
                    // Log error but don't fail the rejection
                    LOGGER.warning("Failed to send notification to teacher: " + e.getMessage());
                }
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error rejecting request: " + e.getMessage());
            throw new RuntimeException("Failed to reject request: " + e.getMessage(), e);
        }
    }

    public boolean cancelRequest(int requestId, String teacherUsername) {
        try {
            // Check if request exists and belongs to teacher
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Request not found");
            }

            if (request.getTeacherUsername() != teacherUsername) {
                throw new SecurityException("Cannot cancel request from another teacher");
            }

            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Can only cancel PENDING requests");
            }

            // Delete the request
            boolean success = requestDAO.delete(requestId);

            if (success) {
                LOGGER.info("Request cancelled by teacher: " + teacherUsername);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error cancelling request: " + e.getMessage());
            throw new RuntimeException("Failed to cancel request: " + e.getMessage(), e);
        }
    }

    private void validateRequest(ClassOpeningRequest request) {
        if (request.getTeacherUsername() == null || request.getTeacherUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Teacher username is required");
        }

        if (request.getSubjectCode() == null || request.getSubjectCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject code is required");
        }

        if (request.getAcademicYear() == null || request.getAcademicYear().trim().isEmpty()) {
            throw new IllegalArgumentException("Academic year is required");
        }

        if (request.getSemester() < 1 || request.getSemester() > 3) {
            throw new IllegalArgumentException("Semester must be 1, 2, or 3");
        }

        if (request.getScheduleDay() == null || request.getScheduleDay().trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule day is required");
        }

        if (request.getScheduleTime() == null || request.getScheduleTime().trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule time is required");
        }

        if (request.getRoom() == null || request.getRoom().trim().isEmpty()) {
            throw new IllegalArgumentException("Room is required");
        }

        if (request.getMaxStudents() < 1 || request.getMaxStudents() > 100) {
            throw new IllegalArgumentException("Max students must be between 1 and 100");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required");
        }

        if (request.getReason().length() > 500) {
            throw new IllegalArgumentException("Reason is too long (max 500 characters)");
        }
    }

    private Course createCourseFromRequest(ClassOpeningRequest request) {
        Course course = new Course();

        // Generate course code
        String courseCode = generateCourseCode(request);
        course.setCourseCode(courseCode);

        // Set basic info from request
        course.setSubjectCode(request.getSubjectCode());
        course.setTeacherUsername(request.getTeacherUsername());
        course.setAcademicYear(request.getAcademicYear());
        course.setSemester(request.getSemester());

        // Set schedule info
        course.setScheduleDay(request.getScheduleDay());
        course.setScheduleTime(request.getScheduleTime());
        course.setRoom(request.getRoom());

        // Set capacity
        course.setMaxStudents(request.getMaxStudents());
        course.setCurrentStudents(0); // Initially 0 students

        // Note: Course model doesn't have status field
        // course.setStatus("ACTIVE");

        return course;
    }

    private String generateCourseCode(ClassOpeningRequest request) {
        // Validate subject code
        String subjectCode = request.getSubjectCode();
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject code is required to generate course code");
        }

        // Get all courses for this subject, year, and semester
        List<Course> existingCourses = courseDAO.findBySubjectAndSemester(
                subjectCode,
                request.getAcademicYear(),
                request.getSemester());

        // Find the next sequence number (check for duplicates to avoid race condition)
        int nextSequence = existingCourses.size() + 1;
        String courseCode;
        int maxAttempts = 100; // Prevent infinite loop
        int attempts = 0;

        do {
            // Format: SUBJ_YEAR_SEM_SEQ
            courseCode = String.format("%s_%s_%d_%02d",
                    subjectCode,
                    request.getAcademicYear(),
                    request.getSemester(),
                    nextSequence);

            // Check if course code already exists (race condition protection)
            Course existing = courseDAO.findByCourseCode(courseCode);
            if (existing == null) {
                break; // Course code is available
            }

            nextSequence++;
            attempts++;
        } while (attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new RuntimeException("Cannot generate unique course code after " + maxAttempts + " attempts");
        }

        return courseCode;
    }

    public RequestStatistics getStatistics() {
        try {
            List<ClassOpeningRequest> all = requestDAO.findAll();

            int pending = 0;
            int approved = 0;
            int rejected = 0;

            for (ClassOpeningRequest req : all) {
                switch (req.getRequestStatus()) {
                    case PENDING:
                        pending++;
                        break;
                    case APPROVED:
                        approved++;
                        break;
                    case REJECTED:
                        rejected++;
                        break;
                }
            }

            return new RequestStatistics(pending, approved, rejected, all.size());

        } catch (Exception e) {
            LOGGER.severe("Error getting statistics: " + e.getMessage());
            return new RequestStatistics(0, 0, 0, 0);
        }
    }

    public static class RequestStatistics {
        private final int pending;
        private final int approved;
        private final int rejected;
        private final int total;

        public RequestStatistics(int pending, int approved, int rejected, int total) {
            this.pending = pending;
            this.approved = approved;
            this.rejected = rejected;
            this.total = total;
        }

        public int getPending() {
            return pending;
        }

        public int getApproved() {
            return approved;
        }

        public int getRejected() {
            return rejected;
        }

        public int getTotal() {
            return total;
        }
    }
}
