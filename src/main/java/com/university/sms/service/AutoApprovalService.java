package com.university.sms.service;

import com.university.sms.dao.*;
import com.university.sms.model.*;
import com.university.sms.model.CourseRegistration.RegistrationStatus;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for auto-approval of course registrations when period closes
 */
public class AutoApprovalService {
    private static final Logger LOGGER = Logger.getLogger(AutoApprovalService.class.getName());
    
    private static final double MIN_REGISTRATION_RATE = 0.5; // 50%
    
    private final RegistrationPeriodDAO periodDAO;
    private final CourseRegistrationDAO registrationDAO;
    private final CourseDAO courseDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final NotificationService notificationService;
    private final CourseService courseService;

    public AutoApprovalService() {
        this.periodDAO = new RegistrationPeriodDAO();
        this.registrationDAO = new CourseRegistrationDAO();
        this.courseDAO = new CourseDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.notificationService = new NotificationService();
        this.courseService = new CourseService();
    }

    /**
     * Close registration period and trigger auto-approval
     */
    public ClosePeriodResult closePeriod(int periodId, int closedByUserId) {
        LOGGER.info("Starting to close registration period: " + periodId);
        
        RegistrationPeriod period = periodDAO.findById(periodId);
        if (period == null) {
            throw new IllegalArgumentException("Period not found");
        }
        
        if (period.getStatus() != RegistrationPeriod.PeriodStatus.OPEN) {
            throw new IllegalStateException("Period is not open");
        }
        
        // Update status to PROCESSING
        period.setStatus(RegistrationPeriod.PeriodStatus.PROCESSING);
        period.setClosedBy(closedByUserId);
        periodDAO.update(period);
        
        ClosePeriodResult result = new ClosePeriodResult();
        result.setPeriodId(periodId);
        result.setStartTime(new Timestamp(System.currentTimeMillis()));
        
        try {
            // Step 1: Auto-approve pending registrations
            LOGGER.info("Step 1: Processing auto-approval...");
            AutoApprovalResult approvalResult = autoApprovePendingRegistrations(periodId);
            result.setApprovedCount(approvalResult.getApprovedCount());
            result.setRejectedCount(approvalResult.getRejectedCount());
            result.setErrorCount(approvalResult.getErrorCount());
            
            // Step 2: Cancel courses with low registration
            LOGGER.info("Step 2: Cancelling courses with low registration...");
            CourseCancellationResult cancellationResult = cancelLowRegistrationCourses(
                period.getAcademicYear(), period.getSemester(), periodId);
            result.setCancelledCoursesCount(cancellationResult.getCancelledCount());
            
            // Step 3: Update period status to COMPLETED
            period.setStatus(RegistrationPeriod.PeriodStatus.COMPLETED);
            periodDAO.update(period);
            
            result.setEndTime(new Timestamp(System.currentTimeMillis()));
            result.setSuccess(true);
            
            LOGGER.info("Successfully closed period: " + periodId + 
                       ", Approved: " + result.getApprovedCount() +
                       ", Rejected: " + result.getRejectedCount() +
                       ", Cancelled Courses: " + result.getCancelledCoursesCount());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing period: " + periodId, e);
            
            // Rollback status
            period.setStatus(RegistrationPeriod.PeriodStatus.OPEN);
            periodDAO.update(period);
            
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Auto-approve pending registrations
     */
    private AutoApprovalResult autoApprovePendingRegistrations(int periodId) {
        AutoApprovalResult result = new AutoApprovalResult();
        
        // Get all pending registrations
        List<CourseRegistration> pendingRegistrations = registrationDAO.findByStatus(RegistrationStatus.PENDING);
        
        LOGGER.info("Found " + pendingRegistrations.size() + " pending registrations");
        
        for (CourseRegistration registration : pendingRegistrations) {
            try {
                if (shouldAutoApprove(registration)) {
                    // Approve registration
                    registration.setRegistrationStatus(RegistrationStatus.APPROVED);
                    boolean updated = registrationDAO.update(registration);
                    
                    if (updated) {
                        // Create enrollment
                        Enrollment enrollment = new Enrollment(
                            registration.getStudentId(), 
                            registration.getCourseId());
                        boolean enrolled = enrollmentDAO.addEnrollment(enrollment);
                        
                        if (enrolled) {
                            // Increment current students
                            courseService.incrementCurrentStudents(registration.getCourseId());
                            
                            // Log approval
                            logAutoApproval(periodId, registration.getRegistrationId(), 
                                "APPROVED", "Auto-approved by system");
                            
                            // Send notification
                            Notification notification = new Notification(
                                "Đăng ký được duyệt",
                                "Đăng ký môn " + registration.getSubjectName() + 
                                " đã được tự động duyệt!",
                                1 // System sender
                            );
                            notification.setPriority(Notification.Priority.HIGH);
                            try {
                                notificationService.sendNotificationToStudent(
                                    registration.getStudentId(), notification);
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Failed to send notification", ex);
                            }
                            
                            result.incrementApproved();
                        } else {
                            result.incrementError();
                            LOGGER.warning("Failed to create enrollment for registration: " + 
                                registration.getRegistrationId());
                        }
                    }
                } else {
                    // Reject registration
                    registration.setRegistrationStatus(RegistrationStatus.CANCELLED);
                    registration.setNotes("Tự động từ chối: không đáp ứng điều kiện tự động duyệt");
                    registrationDAO.update(registration);
                    
                    // Log rejection
                    logAutoApproval(periodId, registration.getRegistrationId(), 
                        "REJECTED", "Not eligible for auto-approval");
                    
                    // Send notification
                    Notification notification = new Notification(
                        "Đăng ký không được duyệt",
                        "Đăng ký môn " + registration.getSubjectName() + 
                        " không được duyệt. Vui lòng liên hệ phòng đào tạo.",
                        1 // System sender
                    );
                    notification.setPriority(Notification.Priority.HIGH);
                    try {
                        notificationService.sendNotificationToStudent(
                            registration.getStudentId(), notification);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to send notification", ex);
                    }
                    
                    result.incrementRejected();
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing registration: " + 
                    registration.getRegistrationId(), e);
                
                logAutoApproval(periodId, registration.getRegistrationId(), 
                    "ERROR", "Error: " + e.getMessage());
                
                result.incrementError();
            }
        }
        
        return result;
    }

    /**
     * Check if registration should be auto-approved
     */
    private boolean shouldAutoApprove(CourseRegistration registration) {
        try {
            Course course = courseDAO.findById(registration.getCourseId());
            if (course == null) {
                return false;
            }
            
            // Check 1: Course not full
            if (course.getCurrentStudents() >= course.getMaxStudents()) {
                LOGGER.info("Course full: " + course.getCourseCode());
                return false;
            }
            
            // Check 2: No schedule conflict
            if (registrationDAO.hasScheduleConflict(
                    registration.getStudentId(), registration.getCourseId())) {
                LOGGER.info("Schedule conflict for student: " + registration.getStudentId());
                return false;
            }
            
            // Check 3: Not already registered
            if (registrationDAO.isAlreadyRegistered(
                    registration.getStudentId(), registration.getCourseId())) {
                LOGGER.info("Already registered: " + registration.getStudentId());
                return false;
            }
            
            // Check 4: Credit limit (24 credits per semester)
            int currentCredits = registrationDAO.getTotalCredits(
                registration.getStudentId(),
                course.getAcademicYear(),
                course.getSemester()
            );
            
            if (currentCredits + course.getCredits() > 24) {
                LOGGER.info("Exceeds credit limit for student: " + registration.getStudentId());
                return false;
            }
            
            // All checks passed
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in shouldAutoApprove", e);
            return false;
        }
    }

    /**
     * Cancel courses with less than 50% registration
     */
    private CourseCancellationResult cancelLowRegistrationCourses(
            String academicYear, int semester, int periodId) {
        
        CourseCancellationResult result = new CourseCancellationResult();
        
        try {
            List<Course> courses = courseDAO.findByAcademicYearAndSemester(academicYear, semester);
            
            for (Course course : courses) {
                double registrationRate = (double) course.getCurrentStudents() / course.getMaxStudents();
                
                if (registrationRate < MIN_REGISTRATION_RATE) {
                    // Cancel course
                    course.setCourseStatus(Course.CourseStatus.CANCELLED);
                    
                    String reason = String.format(
                        "Lớp bị hủy do không đủ sinh viên đăng ký (%.0f%% < %.0f%%)",
                        registrationRate * 100, MIN_REGISTRATION_RATE * 100);
                    
                    // Update course status to CANCELLED
                    courseDAO.updateCourseStatus(course.getCourseId(), Course.CourseStatus.CANCELLED);
                    
                    // Log cancellation
                    logCourseCancellation(course.getCourseId(), periodId, reason,
                        course.getCurrentStudents(), course.getMaxStudents(), registrationRate);
                    
                    // Notify enrolled students
                    List<Enrollment> enrollments = enrollmentDAO.findByCourseId(course.getCourseId());
                    for (Enrollment enrollment : enrollments) {
                        Notification notification = new Notification(
                            "Lớp học bị hủy",
                            "Lớp " + course.getCourseCode() + " - " + course.getSubjectName() +
                            " đã bị hủy do không đủ sinh viên đăng ký. " +
                            "Vui lòng chọn lớp khác hoặc liên hệ phòng đào tạo.",
                            1 // System sender
                        );
                        notification.setPriority(Notification.Priority.URGENT);
                        try {
                            notificationService.sendNotificationToStudent(
                                enrollment.getStudentId(), notification);
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Failed to send notification to student: " + 
                                enrollment.getStudentId(), ex);
                        }
                        
                        // Update enrollment status to DROPPED
                        enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.DROPPED);
                        enrollmentDAO.updateEnrollmentStatus(
                            enrollment.getEnrollmentId(), 
                            Enrollment.EnrollmentStatus.DROPPED);
                    }
                    
                    result.incrementCancelled();
                    
                    LOGGER.info("Cancelled course: " + course.getCourseCode() + 
                               " (Registration rate: " + String.format("%.2f%%", registrationRate * 100) + ")");
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling low registration courses", e);
        }
        
        return result;
    }

    /**
     * Log auto-approval action to database
     */
    private void logAutoApproval(int periodId, int registrationId, String action, String reason) {
        try {
            String sql = "INSERT INTO auto_approval_log (period_id, registration_id, action, reason) VALUES (?, ?, ?, ?)";
            try (Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, periodId);
                pstmt.setInt(2, registrationId);
                pstmt.setString(3, action);
                pstmt.setString(4, reason);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error logging auto-approval", e);
        }
    }

    /**
     * Log course cancellation to database
     */
    private void logCourseCancellation(int courseId, int periodId, String reason,
                                      int registeredStudents, int maxStudents, double rate) {
        try {
            String sql = "INSERT INTO cancelled_courses_log " +
                        "(course_id, period_id, reason, registered_students, max_students, cancellation_rate) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, courseId);
                pstmt.setInt(2, periodId);
                pstmt.setString(3, reason);
                pstmt.setInt(4, registeredStudents);
                pstmt.setInt(5, maxStudents);
                pstmt.setDouble(6, rate);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error logging course cancellation", e);
        }
    }

    // Inner classes for results
    public static class ClosePeriodResult {
        private int periodId;
        private boolean success;
        private String errorMessage;
        private int approvedCount;
        private int rejectedCount;
        private int errorCount;
        private int cancelledCoursesCount;
        private Timestamp startTime;
        private Timestamp endTime;

        // Getters and Setters
        public int getPeriodId() { return periodId; }
        public void setPeriodId(int periodId) { this.periodId = periodId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public int getApprovedCount() { return approvedCount; }
        public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
        
        public int getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public int getCancelledCoursesCount() { return cancelledCoursesCount; }
        public void setCancelledCoursesCount(int cancelledCoursesCount) { 
            this.cancelledCoursesCount = cancelledCoursesCount; 
        }
        
        public Timestamp getStartTime() { return startTime; }
        public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
        
        public Timestamp getEndTime() { return endTime; }
        public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    }

    private static class AutoApprovalResult {
        private int approvedCount = 0;
        private int rejectedCount = 0;
        private int errorCount = 0;

        public void incrementApproved() { approvedCount++; }
        public void incrementRejected() { rejectedCount++; }
        public void incrementError() { errorCount++; }

        public int getApprovedCount() { return approvedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getErrorCount() { return errorCount; }
    }

    private static class CourseCancellationResult {
        private int cancelledCount = 0;

        public void incrementCancelled() { cancelledCount++; }
        public int getCancelledCount() { return cancelledCount; }
    }
}

