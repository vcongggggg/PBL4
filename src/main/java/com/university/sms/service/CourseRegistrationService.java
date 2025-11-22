package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.CourseRegistration.RegistrationStatus;
import com.university.sms.model.Student;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class CourseRegistrationService {
    private static final Logger LOGGER = Logger.getLogger(CourseRegistrationService.class.getName());

    private static final int MAX_CREDITS_PER_SEMESTER = 24;

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
            LOGGER.severe("Lỗi khi lấy danh sách đăng ký: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách đăng ký", e);
        }
    }

    public CourseRegistration getRegistrationById(int registrationId) {
        try {
            return registrationDAO.findById(registrationId);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy đăng ký theo ID: " + e.getMessage());
            throw new RuntimeException("Không thể lấy đăng ký", e);
        }
    }

    public List<CourseRegistration> getRegistrationsByStudent(String studentCode) {
        try {
            return registrationDAO.findByStudent(studentCode);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy đăng ký theo sinh viên: " + e.getMessage());
            throw new RuntimeException("Không thể lấy đăng ký của sinh viên", e);
        }
    }

    public List<CourseRegistration> getRegistrationsByCourse(String courseCode) {
        try {
            return registrationDAO.findByCourse(courseCode);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy đăng ký theo khóa học: " + e.getMessage());
            throw new RuntimeException("Không thể lấy đăng ký của khóa học", e);
        }
    }

    public List<CourseRegistration> getPendingRegistrations() {
        try {
            return registrationDAO.findByStatus(RegistrationStatus.PENDING);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy đăng ký đang chờ: " + e.getMessage());
            throw new RuntimeException("Không thể lấy đăng ký đang chờ", e);
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

            if (registrationDAO.hasActiveRegistrationForSubject(studentCode, course.getSubjectCode())) {
                throw new IllegalStateException("Student is already registered for another class of this subject");
            }

            CourseRegistration existingRegistration = registrationDAO.findByStudentAndCourse(studentCode, courseCode);
            if (existingRegistration != null
                    && existingRegistration.getRegistrationStatus() == RegistrationStatus.CANCELLED) {
                existingRegistration.setRegistrationStatus(RegistrationStatus.PENDING);
                existingRegistration.setRegistrationDate(new Timestamp(System.currentTimeMillis()));
                existingRegistration.setCancelDate(null);
                existingRegistration.setNotes(notes);
                boolean updated = registrationDAO.update(existingRegistration);
                if (updated) {
                    LOGGER.info(
                            "Đã kích hoạt lại đăng ký đã hủy cho sinh viên " + studentCode + " khóa học " + courseCode);
                    return true;
                }
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
                LOGGER.info("Sinh viên " + studentCode + " đã đăng ký khóa học " + courseCode);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi đăng ký khóa học: " + e.getMessage());
            throw new RuntimeException("Không thể đăng ký khóa học: " + e.getMessage(), e);
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

            // Kiểm tra course đã đầy chưa trước khi approve (vì database trigger sẽ tạo
            // enrollment)
            CourseDAO courseDAO = new CourseDAO();
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            Course course = courseDAO.findByCourseCode(registration.getCourseCode());
            if (course != null) {
                int currentEnrolledCount = enrollmentDAO.countByCourse(registration.getCourseCode());
                if (currentEnrolledCount >= course.getMaxStudents()) {
                    throw new IllegalStateException("Khóa học đã đầy (" + currentEnrolledCount + "/"
                            + course.getMaxStudents() + "), không thể duyệt đăng ký");
                }
            }

            registration.setRegistrationStatus(RegistrationStatus.APPROVED);
            boolean success = registrationDAO.update(registration);

            if (success) {
                LOGGER.info("Đã duyệt đăng ký: " + registrationId);

            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi duyệt đăng ký: " + e.getMessage());
            throw new RuntimeException("Không thể duyệt đăng ký: " + e.getMessage(), e);
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
                LOGGER.info("Đã từ chối đăng ký: " + registrationId);
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi từ chối đăng ký: " + e.getMessage());
            throw new RuntimeException("Không thể từ chối đăng ký: " + e.getMessage(), e);
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
                LOGGER.info("Sinh viên đã hủy đăng ký: " + registrationId);

            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi hủy đăng ký: " + e.getMessage());
            throw new RuntimeException("Không thể hủy đăng ký: " + e.getMessage(), e);
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
            LOGGER.severe("Lỗi khi kiểm tra đăng ký: " + e.getMessage());
            return new RegistrationValidation(false, "Validation error: " + e.getMessage());
        }
    }

    public int getStudentCredits(String studentCode, String academicYear, int semester) {
        try {
            return registrationDAO.getTotalCredits(studentCode, academicYear, semester);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy tín chỉ sinh viên: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy thống kê: " + e.getMessage());
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
