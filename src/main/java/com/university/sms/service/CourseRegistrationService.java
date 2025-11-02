package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.CourseRegistration.RegistrationStatus;
import com.university.sms.model.Student;

import java.util.List;
import java.util.logging.Logger;

/**
 * Service for managing course registrations
 */
public class CourseRegistrationService {
    private static final Logger LOGGER = Logger.getLogger(CourseRegistrationService.class.getName());
    
    private static final int MAX_CREDITS_PER_SEMESTER = 24;
    private static final int MIN_CREDITS_PER_SEMESTER = 12;
    
    private final CourseRegistrationDAO registrationDAO;
    private final CourseDAO courseDAO;
    private final StudentDAO studentDAO;

    public CourseRegistrationService() {
        this.registrationDAO = new CourseRegistrationDAO();
        this.courseDAO = new CourseDAO();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Get all registrations
     */
    public List<CourseRegistration> getAllRegistrations() {
        try {
            return registrationDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Error getting all registrations: " + e.getMessage());
            throw new RuntimeException("Failed to get registrations", e);
        }
    }

    /**
     * Get registration by ID
     */
    public CourseRegistration getRegistrationById(int registrationId) {
        try {
            return registrationDAO.findById(registrationId);
        } catch (Exception e) {
            LOGGER.severe("Error getting registration by ID: " + e.getMessage());
            throw new RuntimeException("Failed to get registration", e);
        }
    }

    /**
     * Get registrations by student
     */
    public List<CourseRegistration> getRegistrationsByStudent(int studentId) {
        try {
            return registrationDAO.findByStudent(studentId);
        } catch (Exception e) {
            LOGGER.severe("Error getting registrations by student: " + e.getMessage());
            throw new RuntimeException("Failed to get student registrations", e);
        }
    }

    /**
     * Get registrations by course
     */
    public List<CourseRegistration> getRegistrationsByCourse(int courseId) {
        try {
            return registrationDAO.findByCourse(courseId);
        } catch (Exception e) {
            LOGGER.severe("Error getting registrations by course: " + e.getMessage());
            throw new RuntimeException("Failed to get course registrations", e);
        }
    }

    /**
     * Get pending registrations (for admin approval)
     */
    public List<CourseRegistration> getPendingRegistrations() {
        try {
            return registrationDAO.findByStatus(RegistrationStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Error getting pending registrations: " + e.getMessage());
            throw new RuntimeException("Failed to get pending registrations", e);
        }
    }

    /**
     * Register student for a course
     */
    public boolean registerCourse(int studentId, int courseId, String notes) {
        try {
            // Validate student exists
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                throw new IllegalArgumentException("Student not found");
            }

            // Validate course exists
            Course course = courseDAO.findById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("Course not found");
            }

            // Note: Course model doesn't have status field, skip active check
            // if (!"ACTIVE".equals(course.getStatus())) {
            //     throw new IllegalStateException("Course is not active for registration");
            // }

            // Check if already registered
            if (registrationDAO.isAlreadyRegistered(studentId, courseId)) {
                throw new IllegalStateException("Student is already registered for this course");
            }

            // Check course capacity
            if (course.getCurrentStudents() >= course.getMaxStudents()) {
                throw new IllegalStateException("Course is full");
            }

            // Check schedule conflict
            if (registrationDAO.hasScheduleConflict(studentId, courseId)) {
                throw new IllegalStateException("Schedule conflict with another registered course");
            }

            // Check credit limit
            int currentCredits = registrationDAO.getTotalCredits(
                studentId,
                course.getAcademicYear(),
                course.getSemester()
            );
            
            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > MAX_CREDITS_PER_SEMESTER) {
                throw new IllegalStateException(
                    String.format("Exceeds maximum credits per semester (%d). Current: %d, Course: %d",
                        MAX_CREDITS_PER_SEMESTER, currentCredits, courseCredits)
                );
            }

            // Create registration
            CourseRegistration registration = new CourseRegistration();
            registration.setStudentId(studentId);
            registration.setCourseId(courseId);
            registration.setRegistrationStatus(RegistrationStatus.PENDING);
            registration.setNotes(notes);

            // Insert registration
            boolean success = registrationDAO.insert(registration);

            if (success) {
                LOGGER.info("Student " + studentId + " registered for course " + courseId);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error registering course: " + e.getMessage());
            throw new RuntimeException("Failed to register course: " + e.getMessage(), e);
        }
    }

    /**
     * Approve registration (Admin or automatic)
     */
    public boolean approveRegistration(int registrationId) {
        try {
            // Get registration
            CourseRegistration registration = registrationDAO.findById(registrationId);
            if (registration == null) {
                throw new IllegalArgumentException("Registration not found");
            }

            if (registration.getRegistrationStatus() != RegistrationStatus.PENDING) {
                throw new IllegalStateException("Can only approve PENDING registrations");
            }

            // Update status to APPROVED
            registration.setRegistrationStatus(RegistrationStatus.APPROVED);
            boolean success = registrationDAO.update(registration);

            if (success) {
                LOGGER.info("Registration approved: " + registrationId);
                
                // Note: Enrollment is automatically created by database trigger
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error approving registration: " + e.getMessage());
            throw new RuntimeException("Failed to approve registration: " + e.getMessage(), e);
        }
    }

    /**
     * Reject registration (Admin)
     */
    public boolean rejectRegistration(int registrationId, String reason) {
        try {
            // Get registration
            CourseRegistration registration = registrationDAO.findById(registrationId);
            if (registration == null) {
                throw new IllegalArgumentException("Registration not found");
            }

            if (registration.getRegistrationStatus() != RegistrationStatus.PENDING) {
                throw new IllegalStateException("Can only reject PENDING registrations");
            }

            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required");
            }

            // Update status to CANCELLED (rejecting a registration = cancelling it)
            registration.setRegistrationStatus(RegistrationStatus.CANCELLED);
            registration.setNotes(reason);
            boolean success = registrationDAO.update(registration);

            if (success) {
                LOGGER.info("Registration rejected: " + registrationId);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error rejecting registration: " + e.getMessage());
            throw new RuntimeException("Failed to reject registration: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel registration (Student)
     */
    public boolean cancelRegistration(int registrationId, int studentId) {
        try {
            // Get registration
            CourseRegistration registration = registrationDAO.findById(registrationId);
            if (registration == null) {
                throw new IllegalArgumentException("Registration not found");
            }

            // Check ownership
            if (registration.getStudentId() != studentId) {
                throw new SecurityException("Cannot cancel registration from another student");
            }

            // Can cancel if PENDING or APPROVED
            if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
                throw new IllegalStateException("Registration is already cancelled");
            }

            // Cancel registration
            boolean success = registrationDAO.cancel(registrationId);

            if (success) {
                LOGGER.info("Registration cancelled by student: " + registrationId);
                
                // Note: Enrollment is automatically deleted by database trigger if it was APPROVED
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error cancelling registration: " + e.getMessage());
            throw new RuntimeException("Failed to cancel registration: " + e.getMessage(), e);
        }
    }

    /**
     * Check if student can register for a course
     */
    public RegistrationValidation validateRegistration(int studentId, int courseId) {
        try {
            // Check student exists
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                return new RegistrationValidation(false, "Student not found");
            }

            // Check course exists
            Course course = courseDAO.findById(courseId);
            if (course == null) {
                return new RegistrationValidation(false, "Course not found");
            }

            // Note: Course model doesn't have status field
            // if (!"ACTIVE".equals(course.getStatus())) {
            //     return new RegistrationValidation(false, "Course is not active");
            // }

            // Check if already registered
            if (registrationDAO.isAlreadyRegistered(studentId, courseId)) {
                return new RegistrationValidation(false, "Already registered for this course");
            }

            // Check course capacity
            if (course.getCurrentStudents() >= course.getMaxStudents()) {
                return new RegistrationValidation(false, "Course is full");
            }

            // Check schedule conflict
            if (registrationDAO.hasScheduleConflict(studentId, courseId)) {
                return new RegistrationValidation(false, "Schedule conflict detected");
            }

            // Check credit limit
            int currentCredits = registrationDAO.getTotalCredits(
                studentId,
                course.getAcademicYear(),
                course.getSemester()
            );
            
            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > MAX_CREDITS_PER_SEMESTER) {
                return new RegistrationValidation(false,
                    String.format("Exceeds max credits (%d). Current: %d, Course: %d",
                        MAX_CREDITS_PER_SEMESTER, currentCredits, courseCredits)
                );
            }

            // All checks passed
            return new RegistrationValidation(true, "Can register");

        } catch (Exception e) {
            LOGGER.severe("Error validating registration: " + e.getMessage());
            return new RegistrationValidation(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Get student's registered credits for a semester
     */
    public int getStudentCredits(int studentId, String academicYear, int semester) {
        try {
            return registrationDAO.getTotalCredits(studentId, academicYear, semester);
        } catch (Exception e) {
            LOGGER.severe("Error getting student credits: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get registration statistics
     */
    public RegistrationStatistics getStatistics() {
        try {
            List<CourseRegistration> all = registrationDAO.findAll();
            
            int pending = 0;
            int approved = 0;
            int rejected = 0;
            int cancelled = 0;
            
            for (CourseRegistration reg : all) {
                switch (reg.getRegistrationStatus()) {
                    case PENDING:
                        pending++;
                        break;
                    case APPROVED:
                        approved++;
                        break;
                    case CANCELLED:
                        // CANCELLED includes both student-cancelled and admin-rejected
                        cancelled++;
                        break;
                    default:
                        cancelled++;
                        break;
                }
            }
            
            return new RegistrationStatistics(pending, approved, rejected, cancelled, all.size());
            
        } catch (Exception e) {
            LOGGER.severe("Error getting statistics: " + e.getMessage());
            return new RegistrationStatistics(0, 0, 0, 0, 0);
        }
    }

    /**
     * Inner class for validation result
     */
    public static class RegistrationValidation {
        private final boolean valid;
        private final String message;

        public RegistrationValidation(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * Inner class for statistics
     */
    public static class RegistrationStatistics {
        private final int pending;
        private final int approved;
        private final int rejected;
        private final int cancelled;
        private final int total;

        public RegistrationStatistics(int pending, int approved, int rejected, int cancelled, int total) {
            this.pending = pending;
            this.approved = approved;
            this.rejected = rejected;
            this.cancelled = cancelled;
            this.total = total;
        }

        public int getPending() { return pending; }
        public int getApproved() { return approved; }
        public int getRejected() { return rejected; }
        public int getCancelled() { return cancelled; }
        public int getTotal() { return total; }
    }
}



