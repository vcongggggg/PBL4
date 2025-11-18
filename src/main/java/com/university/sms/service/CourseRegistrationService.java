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

    public List<CourseRegistration> getAllRegistrations() {
        try {
            return registrationDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Error getting all registrations: " + e.getMessage());
            throw new RuntimeException("Failed to get registrations", e);
        }
    }

    public CourseRegistration getRegistrationById(int registrationId) {
        try {
            return registrationDAO.findById(registrationId);
        } catch (Exception e) {
            LOGGER.severe("Error getting registration by ID: " + e.getMessage());
            throw new RuntimeException("Failed to get registration", e);
        }
    }

    public List<CourseRegistration> getRegistrationsByStudent(String studentCode) {
        try {
            return registrationDAO.findByStudent(studentCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting registrations by student: " + e.getMessage());
            throw new RuntimeException("Failed to get student registrations", e);
        }
    }

    public List<CourseRegistration> getRegistrationsByCourse(String courseCode) {
        try {
            return registrationDAO.findByCourse(courseCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting registrations by course: " + e.getMessage());
            throw new RuntimeException("Failed to get course registrations", e);
        }
    }

    public List<CourseRegistration> getPendingRegistrations() {
        try {
            return registrationDAO.findByStatus(RegistrationStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Error getting pending registrations: " + e.getMessage());
            throw new RuntimeException("Failed to get pending registrations", e);
        }
    }

    public boolean registerCourse(String studentCode, String courseCode, String notes) {
        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                throw new IllegalArgumentException("Student not found");
            }

            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                throw new IllegalArgumentException("Course not found");
            }

            if (course.getCourseStatus() != Course.CourseStatus.PLANNING) {
                throw new IllegalStateException("Course is not open for registration");
            }

            if (course.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
                throw new IllegalStateException("Registration period is not open for this course");
            }

            if (registrationDAO.isAlreadyRegistered(studentCode, courseCode)) {
                throw new IllegalStateException("Student is already registered for this course");
            }

            if (course.getCurrentStudents() >= course.getMaxStudents()) {
                throw new IllegalStateException("Course is full");
            }

            if (registrationDAO.hasScheduleConflict(studentCode, courseCode)) {
                throw new IllegalStateException("Schedule conflict with another registered course");
            }
            int currentCredits = registrationDAO.getTotalCredits(
                    studentCode,
                    course.getAcademicYear(),
                    course.getSemester());

            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > MAX_CREDITS_PER_SEMESTER) {
                throw new IllegalStateException(
                        String.format("Exceeds maximum credits per semester (%d). Current: %d, Course: %d",
                                MAX_CREDITS_PER_SEMESTER, currentCredits, courseCredits));
            }

            CourseRegistration registration = new CourseRegistration();
            registration.setStudentCode(studentCode);
            registration.setCourseCode(courseCode);
            registration.setRegistrationStatus(RegistrationStatus.PENDING);
            registration.setNotes(notes);

            boolean success = registrationDAO.insert(registration);

            if (success) {
                LOGGER.info("Student " + studentCode + " registered for course " + courseCode);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error registering course: " + e.getMessage());
            throw new RuntimeException("Failed to register course: " + e.getMessage(), e);
        }
    }

    public boolean approveRegistration(int registrationId) {
        try {
            CourseRegistration registration = registrationDAO.findById(registrationId);
            if (registration == null) {
                throw new IllegalArgumentException("Registration not found");
            }

            if (registration.getRegistrationStatus() != RegistrationStatus.PENDING) {
                throw new IllegalStateException("Can only approve PENDING registrations");
            }

            registration.setRegistrationStatus(RegistrationStatus.APPROVED);
            boolean success = registrationDAO.update(registration);

            if (success) {
                LOGGER.info("Registration approved: " + registrationId);

            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error approving registration: " + e.getMessage());
            throw new RuntimeException("Failed to approve registration: " + e.getMessage(), e);
        }
    }

    public boolean rejectRegistration(int registrationId, String reason) {
        try {
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

    public boolean cancelRegistration(int registrationId, String studentCode) {
        try {
            CourseRegistration registration = registrationDAO.findById(registrationId);
            if (registration == null) {
                throw new IllegalArgumentException("Registration not found");
            }

            if (registration.getStudentCode() == null || !registration.getStudentCode().equals(studentCode)) {
                throw new SecurityException("Cannot cancel registration from another student");
            }

            if (registration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
                throw new IllegalStateException("Registration is already cancelled");
            }

            boolean success = registrationDAO.cancel(registrationId);

            if (success) {
                LOGGER.info("Registration cancelled by student: " + registrationId);

            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error cancelling registration: " + e.getMessage());
            throw new RuntimeException("Failed to cancel registration: " + e.getMessage(), e);
        }
    }

    public RegistrationValidation validateRegistration(String studentCode, String courseCode) {
        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                return new RegistrationValidation(false, "Student not found");
            }

            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                return new RegistrationValidation(false, "Course not found");
            }

            if (course.getCourseStatus() != Course.CourseStatus.PLANNING) {
                return new RegistrationValidation(false, "Course is not open for registration");
            }

            if (course.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
                return new RegistrationValidation(false, "Registration period is closed");
            }

            if (registrationDAO.isAlreadyRegistered(studentCode, courseCode)) {
                return new RegistrationValidation(false, "Already registered for this course");
            }

            if (course.getCurrentStudents() >= course.getMaxStudents()) {
                return new RegistrationValidation(false, "Course is full");
            }

            if (registrationDAO.hasScheduleConflict(studentCode, courseCode)) {
                return new RegistrationValidation(false, "Schedule conflict detected");
            }
            int currentCredits = registrationDAO.getTotalCredits(
                    studentCode,
                    course.getAcademicYear(),
                    course.getSemester());

            int courseCredits = course.getCredits();
            if (currentCredits + courseCredits > MAX_CREDITS_PER_SEMESTER) {
                return new RegistrationValidation(false,
                        String.format("Exceeds max credits (%d). Current: %d, Course: %d",
                                MAX_CREDITS_PER_SEMESTER, currentCredits, courseCredits));
            }

            return new RegistrationValidation(true, "Can register");

        } catch (Exception e) {
            LOGGER.severe("Error validating registration: " + e.getMessage());
            return new RegistrationValidation(false, "Validation error: " + e.getMessage());
        }
    }

    public int getStudentCredits(String studentCode, String academicYear, int semester) {
        try {
            return registrationDAO.getTotalCredits(studentCode, academicYear, semester);
        } catch (Exception e) {
            LOGGER.severe("Error getting student credits: " + e.getMessage());
            return 0;
        }
    }

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

    public static class RegistrationValidation {
        private final boolean valid;
        private final String message;

        public RegistrationValidation(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

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

        public int getPending() {
            return pending;
        }

        public int getApproved() {
            return approved;
        }

        public int getRejected() {
            return rejected;
        }

        public int getCancelled() {
            return cancelled;
        }

        public int getTotal() {
            return total;
        }
    }
}
