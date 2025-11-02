package com.university.sms.service;

import com.university.sms.dao.ClassOpeningRequestDAO;
import com.university.sms.dao.CourseDAO;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.ClassOpeningRequest.RequestStatus;
import com.university.sms.model.Course;

import java.util.List;
import java.util.logging.Logger;

/**
 * Service for managing class opening requests
 */
public class ClassOpeningRequestService {
    private static final Logger LOGGER = Logger.getLogger(ClassOpeningRequestService.class.getName());
    
    private final ClassOpeningRequestDAO requestDAO;
    private final CourseDAO courseDAO;

    public ClassOpeningRequestService() {
        this.requestDAO = new ClassOpeningRequestDAO();
        this.courseDAO = new CourseDAO();
    }

    /**
     * Get all class opening requests
     */
    public List<ClassOpeningRequest> getAllRequests() {
        try {
            return requestDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Error getting all requests: " + e.getMessage());
            throw new RuntimeException("Failed to get requests", e);
        }
    }

    /**
     * Get request by ID
     */
    public ClassOpeningRequest getRequestById(int requestId) {
        try {
            return requestDAO.findById(requestId);
        } catch (Exception e) {
            LOGGER.severe("Error getting request by ID: " + e.getMessage());
            throw new RuntimeException("Failed to get request", e);
        }
    }

    /**
     * Get requests by teacher
     */
    public List<ClassOpeningRequest> getRequestsByTeacher(int teacherId) {
        try {
            return requestDAO.findByTeacher(teacherId);
        } catch (Exception e) {
            LOGGER.severe("Error getting requests by teacher: " + e.getMessage());
            throw new RuntimeException("Failed to get teacher requests", e);
        }
    }

    /**
     * Get pending requests (for admin)
     */
    public List<ClassOpeningRequest> getPendingRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Error getting pending requests: " + e.getMessage());
            throw new RuntimeException("Failed to get pending requests", e);
        }
    }

    /**
     * Get approved requests
     */
    public List<ClassOpeningRequest> getApprovedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.APPROVED);
        } catch (Exception e) {
            LOGGER.severe("Error getting approved requests: " + e.getMessage());
            throw new RuntimeException("Failed to get approved requests", e);
        }
    }

    /**
     * Get rejected requests
     */
    public List<ClassOpeningRequest> getRejectedRequests() {
        try {
            return requestDAO.findByStatus(RequestStatus.REJECTED);
        } catch (Exception e) {
            LOGGER.severe("Error getting rejected requests: " + e.getMessage());
            throw new RuntimeException("Failed to get rejected requests", e);
        }
    }

    /**
     * Submit a new class opening request (Teacher)
     */
    public boolean submitRequest(ClassOpeningRequest request) {
        try {
            // Validate request
            validateRequest(request);
            
            // Set initial status
            request.setRequestStatus(RequestStatus.PENDING);
            
            // Insert to database
            boolean success = requestDAO.insert(request);
            
            if (success) {
                LOGGER.info("Request submitted successfully by teacher ID: " + request.getTeacherId());
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.severe("Error submitting request: " + e.getMessage());
            throw new RuntimeException("Failed to submit request: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing request (Teacher - only if PENDING)
     */
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

    /**
     * Approve request (Admin)
     */
    public boolean approveRequest(int requestId, int adminId, String note) {
        try {
            // Get request details
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Request not found");
            }
            
            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Can only approve PENDING requests");
            }
            
            // Create corresponding course
            Course course = createCourseFromRequest(request);
            boolean courseCreated = courseDAO.addCourse(course);
            
            if (!courseCreated) {
                throw new RuntimeException("Failed to create course for approved request");
            }
            
            // Approve the request with the created course ID
            boolean success = requestDAO.approve(requestId, adminId, note, course.getCourseId());
            
            if (success) {
                LOGGER.info("Request approved by admin ID: " + adminId + ", Course ID: " + course.getCourseId());
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.severe("Error approving request: " + e.getMessage());
            throw new RuntimeException("Failed to approve request: " + e.getMessage(), e);
        }
    }

    /**
     * Reject request (Admin)
     */
    public boolean rejectRequest(int requestId, int adminId, String reason) {
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
            boolean success = requestDAO.reject(requestId, adminId, reason);
            
            if (success) {
                LOGGER.info("Request rejected by admin ID: " + adminId);
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.severe("Error rejecting request: " + e.getMessage());
            throw new RuntimeException("Failed to reject request: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel request (Teacher - only if PENDING)
     */
    public boolean cancelRequest(int requestId, int teacherId) {
        try {
            // Check if request exists and belongs to teacher
            ClassOpeningRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new IllegalArgumentException("Request not found");
            }
            
            if (request.getTeacherId() != teacherId) {
                throw new SecurityException("Cannot cancel request from another teacher");
            }
            
            if (request.getRequestStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException("Can only cancel PENDING requests");
            }
            
            // Delete the request
            boolean success = requestDAO.delete(requestId);
            
            if (success) {
                LOGGER.info("Request cancelled by teacher ID: " + teacherId);
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.severe("Error cancelling request: " + e.getMessage());
            throw new RuntimeException("Failed to cancel request: " + e.getMessage(), e);
        }
    }

    /**
     * Validate request data
     */
    private void validateRequest(ClassOpeningRequest request) {
        if (request.getTeacherId() <= 0) {
            throw new IllegalArgumentException("Teacher ID is required");
        }
        
        if (request.getSubjectId() <= 0) {
            throw new IllegalArgumentException("Subject ID is required");
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

    /**
     * Create a Course from approved ClassOpeningRequest
     */
    private Course createCourseFromRequest(ClassOpeningRequest request) {
        Course course = new Course();
        
        // Generate course code
        String courseCode = generateCourseCode(request);
        course.setCourseCode(courseCode);
        
        // Set basic info from request
        course.setSubjectId(request.getSubjectId());
        course.setTeacherId(request.getTeacherId());
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

    /**
     * Generate unique course code
     * Format: [SUBJECT_CODE]_[YEAR]_[SEM]_[SEQUENCE]
     * Example: CS101_2024_1_01
     */
    private String generateCourseCode(ClassOpeningRequest request) {
        // Get all courses for this subject, year, and semester
        List<Course> existingCourses = courseDAO.findBySubjectAndSemester(
            request.getSubjectId(),
            request.getAcademicYear(),
            request.getSemester()
        );
        
        // Find the next sequence number
        int nextSequence = existingCourses.size() + 1;
        
        // Get subject code from request (assuming it's available)
        String subjectCode = request.getSubjectCode();
        if (subjectCode == null || subjectCode.isEmpty()) {
            subjectCode = "SUBJ" + request.getSubjectId();
        }
        
        // Format: SUBJ_YEAR_SEM_SEQ
        return String.format("%s_%s_%d_%02d",
            subjectCode,
            request.getAcademicYear(),
            request.getSemester(),
            nextSequence
        );
    }

    /**
     * Get request statistics (for admin dashboard)
     */
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

    /**
     * Inner class for statistics
     */
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

        public int getPending() { return pending; }
        public int getApproved() { return approved; }
        public int getRejected() { return rejected; }
        public int getTotal() { return total; }
    }
}



