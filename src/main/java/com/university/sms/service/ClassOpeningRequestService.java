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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            LOGGER.severe("Lỗi khi lấy tất cả yêu cầu: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách yêu cầu", e);
        }
    }

    public ClassOpeningRequest getRequestById(int requestId) {
        try {
            return requestDAO.findById(requestId);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy yêu cầu theo ID: " + e.getMessage());
            throw new RuntimeException("Không thể lấy yêu cầu", e);
        }
    }

    public List<ClassOpeningRequest> getRequestsByTeacher(String teacherUsername) {
        try {
            return requestDAO.findByTeacher(teacherUsername);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy yêu cầu theo giáo viên: " + e.getMessage());
            throw new RuntimeException("Không thể lấy yêu cầu của giáo viên", e);
        }
    }

    public List<ClassOpeningRequest> getPendingRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy yêu cầu đang chờ: " + e.getMessage());
            throw new RuntimeException("Không thể lấy yêu cầu đang chờ", e);
        }
    }

    public List<ClassOpeningRequest> getApprovedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.APPROVED);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy yêu cầu đã duyệt: " + e.getMessage());
            throw new RuntimeException("Không thể lấy yêu cầu đã duyệt", e);
        }
    }

    public List<ClassOpeningRequest> getRejectedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.REJECTED);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy yêu cầu bị từ chối: " + e.getMessage());
            throw new RuntimeException("Không thể lấy yêu cầu bị từ chối", e);
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
                LOGGER.info("Yêu cầu được gửi thành công bởi giáo viên: " + request.getTeacherUsername());
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi gửi yêu cầu: " + e.getMessage());
            throw new RuntimeException("Không thể gửi yêu cầu: " + e.getMessage(), e);
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
                LOGGER.info("Yêu cầu được cập nhật thành công: " + request.getRequestId());
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi cập nhật yêu cầu: " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật yêu cầu: " + e.getMessage(), e);
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

            // Kiểm tra trùng lịch với các lớp đã có của giảng viên (kiểm tra lại khi
            // approve)
            // Vì có thể lịch của giáo viên đã thay đổi từ lúc gửi yêu cầu đến lúc approve
            checkScheduleConflict(request);

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
                                "Đã hoàn tác việc tạo lớp do duyệt thất bại: " + createdCourse.getCourseCode());
                    } catch (Exception rollbackEx) {
                        LOGGER.severe("Không thể hoàn tác việc tạo lớp: " + rollbackEx.getMessage());
                    }
                }
                throw new RuntimeException("Không thể duyệt yêu cầu sau khi tạo lớp");
            }

            LOGGER.info(
                    "Yêu cầu được duyệt bởi admin: " + adminUsername + ", Mã lớp: " + course.getCourseCode());

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
                LOGGER.info("Đã gửi thông báo cho giáo viên: " + request.getTeacherUsername());
            } catch (Exception e) {
                // Log error but don't fail the approval
                LOGGER.warning("Không thể gửi thông báo cho giáo viên: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            // Rollback: Delete the course if any error occurred
            if (createdCourse != null && createdCourse.getCourseId() > 0) {
                try {
                    courseDAO.deleteCourse(createdCourse.getCourseId());
                    LOGGER.warning("Đã hoàn tác việc tạo lớp do lỗi: " + createdCourse.getCourseCode());
                } catch (Exception rollbackEx) {
                    LOGGER.severe("Không thể hoàn tác việc tạo lớp: " + rollbackEx.getMessage());
                }
            }
            LOGGER.severe("Lỗi khi duyệt yêu cầu: " + e.getMessage());
            throw new RuntimeException("Không thể duyệt yêu cầu: " + e.getMessage(), e);
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
                LOGGER.info("Yêu cầu bị từ chối bởi admin: " + adminUsername);

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
                    LOGGER.info("Đã gửi thông báo cho giáo viên: " + request.getTeacherUsername());
                } catch (Exception e) {
                    // Log error but don't fail the rejection
                    LOGGER.warning("Không thể gửi thông báo cho giáo viên: " + e.getMessage());
                }
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi từ chối yêu cầu: " + e.getMessage());
            throw new RuntimeException("Không thể từ chối yêu cầu: " + e.getMessage(), e);
        }
    }

    public boolean cancelRequest(int requestId, String teacherUsername) {
        try {
            // Check if request exists and belongs to teacher
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Không tìm thấy yêu cầu");
            }

            // Kiểm tra yêu cầu thuộc về giáo viên này
            if (request.getTeacherUsername() == null ||
                    !request.getTeacherUsername().equals(teacherUsername)) {
                throw new SecurityException("Bạn chỉ có thể hủy yêu cầu của chính mình");
            }

            // Chỉ cho phép hủy yêu cầu đang chờ duyệt (PENDING)
            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Chỉ có thể hủy yêu cầu đang chờ duyệt");
            }

            // Delete the request
            boolean success = requestDAO.delete(requestId);

            if (success) {
                LOGGER.info("Request cancelled by teacher: " + teacherUsername + ", requestId: " + requestId);
            }

            return success;

        } catch (SecurityException | IllegalStateException | IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            LOGGER.severe("Error cancelling request: " + e.getMessage());
            throw new RuntimeException("Lỗi khi hủy yêu cầu: " + e.getMessage(), e);
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

        // Kiểm tra khoa: Giảng viên chỉ có thể mở lớp thuộc khoa của mình
        com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
        com.university.sms.model.User teacher = userDAO.findByUsername(request.getTeacherUsername());
        if (teacher == null) {
            throw new IllegalArgumentException("Giảng viên không tồn tại");
        }

        String teacherFacultyCode = teacher.getFacultyCode();
        if (teacherFacultyCode == null || teacherFacultyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Giảng viên chưa được gán vào khoa. Vui lòng liên hệ admin.");
        }

        // Kiểm tra môn học thuộc khoa của giảng viên
        com.university.sms.dao.SubjectDAO subjectDAO = new com.university.sms.dao.SubjectDAO();
        com.university.sms.model.Subject subject = subjectDAO.findByCode(request.getSubjectCode());
        if (subject == null) {
            throw new IllegalArgumentException("Môn học không tồn tại");
        }

        if (!teacherFacultyCode.equals(subject.getFacultyCode())) {
            throw new IllegalArgumentException(
                    "Bạn chỉ có thể mở lớp cho các môn học thuộc khoa của mình. Môn học này thuộc khoa khác.");
        }

        // Kiểm tra trùng lịch với các lớp đã có của giảng viên
        checkScheduleConflict(request);
    }

    /**
     * Kiểm tra trùng lịch với các lớp đã có của giảng viên và các request đã được
     * approve
     */
    private void checkScheduleConflict(ClassOpeningRequest request) {
        try {
            // Parse schedule time để lấy periods
            String scheduleTime = request.getScheduleTime();
            int startPeriod = parseStartPeriod(scheduleTime);
            int endPeriod = parseEndPeriod(scheduleTime);

            if (startPeriod <= 0 || endPeriod <= 0) {
                return; // Không parse được, bỏ qua kiểm tra
            }

            // Parse schedule day để xử lý nhiều ngày (ví dụ: "Thứ 2, Thứ 4")
            List<String> requestDays = parseScheduleDays(request.getScheduleDay());

            // 1. Kiểm tra với các lớp học phần đã có của giảng viên
            List<Course> teacherCourses = courseDAO.findByTeacherUsername(request.getTeacherUsername());
            if (teacherCourses != null && !teacherCourses.isEmpty()) {
                for (Course course : teacherCourses) {
                    // Chỉ kiểm tra các lớp cùng năm học, học kỳ và đang diễn ra hoặc đang mở đăng
                    // ký
                    if (!course.getAcademicYear().equals(request.getAcademicYear())
                            || course.getSemester() != request.getSemester()) {
                        continue;
                    }

                    if (course.getCourseStatus() != Course.CourseStatus.ONGOING
                            && course.getCourseStatus() != Course.CourseStatus.PLANNING) {
                        continue;
                    }

                    // Kiểm tra trùng ngày và thời gian
                    if (hasScheduleConflict(requestDays, startPeriod, endPeriod,
                            course.getScheduleDay(), course.getScheduleTime())) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Lịch học bị trùng với lớp %s (%s - %s). Vui lòng chọn lịch khác.",
                                        course.getCourseCode(),
                                        course.getScheduleDay(),
                                        course.getScheduleTime()));
                    }
                }
            }

            // 2. Kiểm tra với các request đã được APPROVED khác của giáo viên (cùng năm
            // học, học kỳ)
            // Vì có thể có nhiều request được approve cùng lúc
            // LƯU Ý: Chỉ kiểm tra với APPROVED requests, bỏ qua REJECTED và PENDING
            // (cho phép gửi yêu cầu mới nếu trùng lịch với yêu cầu đã bị hủy)
            List<ClassOpeningRequest> teacherRequests = requestDAO.findByTeacher(request.getTeacherUsername());
            if (teacherRequests != null && !teacherRequests.isEmpty()) {
                for (ClassOpeningRequest otherRequest : teacherRequests) {
                    if (otherRequest.getRequestId() == request.getRequestId()) {
                        continue;
                    }

                    if (otherRequest.getRequestStatus() == RequestStatus.REJECTED) {
                        continue;
                    }

                    if (!otherRequest.getAcademicYear().equals(request.getAcademicYear())
                            || otherRequest.getSemester() != request.getSemester()) {
                        continue;
                    }

                    int otherStartPeriod = parseStartPeriod(otherRequest.getScheduleTime());
                    int otherEndPeriod = parseEndPeriod(otherRequest.getScheduleTime());

                    if (otherStartPeriod > 0 && otherEndPeriod > 0) {
                        List<String> otherDays = parseScheduleDays(otherRequest.getScheduleDay());
                        if (hasScheduleConflict(requestDays, startPeriod, endPeriod,
                                otherDays, otherStartPeriod, otherEndPeriod)) {
                            String statusLabel = otherRequest.getRequestStatus() == RequestStatus.APPROVED
                                    ? "đã được duyệt"
                                    : "đang chờ duyệt";
                            throw new IllegalArgumentException(
                                    String.format(
                                            "Lịch học bị trùng với yêu cầu %s khác (%s - %s). Vui lòng chọn lịch khác.",
                                            statusLabel,
                                            otherRequest.getScheduleDay(),
                                            otherRequest.getScheduleTime()));
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi kiểm tra xung đột lịch: " + e.getMessage());
            // Không throw exception nếu có lỗi khi kiểm tra, chỉ log warning
        }
    }

    /**
     * Parse schedule day để xử lý nhiều ngày (ví dụ: "Thứ 2, Thứ 4" hoặc "Monday,
     * Wednesday")
     */
    private List<String> parseScheduleDays(String scheduleDay) {
        List<String> days = new java.util.ArrayList<>();
        if (scheduleDay == null || scheduleDay.trim().isEmpty()) {
            return days;
        }

        Matcher matcher = Pattern.compile("(Thứ\\s*\\d)", Pattern.CASE_INSENSITIVE).matcher(scheduleDay);
        if (matcher.find()) {
            days.add(matcher.group(1).trim());
            return days;
        }

        String normalized = scheduleDay.replaceAll("[/\\-]", ",");
        String[] parts = normalized.split(",");
        for (String part : parts) {
            String day = part.trim();
            if (!day.isEmpty()) {
                days.add(day);
                break;
            }
        }

        if (days.isEmpty()) {
            days.add(scheduleDay.trim());
        }

        return days;
    }

    /**
     * Kiểm tra xung đột lịch giữa 2 schedule
     */
    private boolean hasScheduleConflict(List<String> days1, int startPeriod1, int endPeriod1,
            String scheduleDay2, String scheduleTime2) {
        List<String> days2 = parseScheduleDays(scheduleDay2);
        int startPeriod2 = parseStartPeriod(scheduleTime2);
        int endPeriod2 = parseEndPeriod(scheduleTime2);

        if (startPeriod2 <= 0 || endPeriod2 <= 0) {
            return false; // Không parse được, không có xung đột
        }

        return hasScheduleConflict(days1, startPeriod1, endPeriod1, days2, startPeriod2, endPeriod2);
    }

    /**
     * Kiểm tra xung đột lịch giữa 2 schedule (overloaded)
     */
    private boolean hasScheduleConflict(List<String> days1, int startPeriod1, int endPeriod1,
            List<String> days2, int startPeriod2, int endPeriod2) {
        // Kiểm tra có ngày trùng không
        boolean hasCommonDay = false;
        for (String day1 : days1) {
            for (String day2 : days2) {
                if (day1.equals(day2)) {
                    hasCommonDay = true;
                    break;
                }
            }
            if (hasCommonDay) {
                break;
            }
        }

        if (!hasCommonDay) {
            return false; // Không có ngày trùng, không xung đột
        }

        // Kiểm tra xung đột thời gian (periods)
        // Overlap xảy ra khi: startPeriod1 <= endPeriod2 && endPeriod1 >= startPeriod2
        return startPeriod1 <= endPeriod2 && endPeriod1 >= startPeriod2;
    }

    /**
     * Parse start period từ schedule time string
     */
    private int parseStartPeriod(String scheduleTime) {
        if (scheduleTime == null || scheduleTime.trim().isEmpty()) {
            return 0;
        }

        try {
            // Format: "Tiết 1-3 (07:00-09:30)" hoặc "1-3"
            String periodStr = scheduleTime.trim();
            if (periodStr.toLowerCase().contains("tiết")) {
                periodStr = periodStr.split("(?i)tiết")[1].trim().split("\\(")[0].trim();
            }

            String[] parts = periodStr.split("-");
            if (parts.length >= 1) {
                return Integer.parseInt(parts[0].trim());
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }

    /**
     * Parse end period từ schedule time string
     */
    private int parseEndPeriod(String scheduleTime) {
        if (scheduleTime == null || scheduleTime.trim().isEmpty()) {
            return 0;
        }

        try {
            // Format: "Tiết 1-3 (07:00-09:30)" hoặc "1-3"
            String periodStr = scheduleTime.trim();
            if (periodStr.toLowerCase().contains("tiết")) {
                periodStr = periodStr.split("(?i)tiết")[1].trim().split("\\(")[0].trim();
            }

            String[] parts = periodStr.split("-");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1].trim());
            } else if (parts.length == 1) {
                // Nếu chỉ có 1 period, start = end
                return Integer.parseInt(parts[0].trim());
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
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
        course.setRegistrationStatus(Course.RegistrationStatus.LOCKED);
        course.setCourseStatus(Course.CourseStatus.PLANNING);

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

        String academicYearTag = buildAcademicYearTag(request.getAcademicYear());
        do {
            // New compact format: {SUBJECT}-{YYYY}S{semester}-{sequence}
            courseCode = String.format("%s-%sS%d-%02d",
                    subjectCode,
                    academicYearTag,
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

    private String buildAcademicYearTag(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            return "0000";
        }
        String[] parts = academicYear.split("-");
        if (parts.length == 2) {
            String start = parts[0].trim();
            String end = parts[1].trim();
            String startSuffix = start.length() >= 2 ? start.substring(start.length() - 2) : start;
            String endSuffix = end.length() >= 2 ? end.substring(end.length() - 2) : end;
            return startSuffix + endSuffix;
        }
        String digits = academicYear.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return digits.substring(digits.length() - 4);
        }
        return digits.isEmpty() ? "0000" : digits;
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
            LOGGER.severe("Lỗi khi lấy thống kê: " + e.getMessage());
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
