package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.GradeDAO;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;

import java.util.List;
import java.util.logging.Logger;

/**
 * Service xử lý các thao tác liên quan đến khóa học
 */
public class CourseService {
    private static final Logger LOGGER = Logger.getLogger(CourseService.class.getName());
    private static final int MIN_STUDENTS_TO_START = 30;

    private final CourseDAO courseDAO;
    private final CourseRegistrationDAO courseRegistrationDAO;
    private final EnrollmentDAO enrollmentDAO;

    public CourseService() {
        this.courseDAO = new CourseDAO();
        this.courseRegistrationDAO = new CourseRegistrationDAO();
        this.enrollmentDAO = new EnrollmentDAO();
    }

    /**
     * Lấy tất cả khóa học
     */
    public List<Course> getAllCourses() {
        try {
            return courseDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Error getting all courses: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy khóa học theo ID
     */
    public Course getCourseById(int courseId) {
        if (courseId <= 0) {
            return null;
        }

        try {
            return courseDAO.findById(courseId);
        } catch (Exception e) {
            LOGGER.severe("Error getting course by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy khóa học theo mã khóa học
     */
    public Course getCourseByCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return null;
        }

        try {
            return courseDAO.findByCourseCode(courseCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting course by code: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ REFACTORED: Lấy khóa học theo giáo viên (dùng teacherUsername)
     */
    public List<Course> getCoursesByTeacher(String teacherUsername) {
        if (teacherUsername == null || teacherUsername.trim().isEmpty()) {
            return List.of();
        }

        try {
            return courseDAO.findByTeacherUsername(teacherUsername);
        } catch (Exception e) {
            LOGGER.severe("Error getting courses by teacher: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy khóa học theo năm học và học kỳ
     */
    public List<Course> getCoursesByAcademicYear(String academicYear, int semester) {
        if (academicYear == null || academicYear.trim().isEmpty() || semester <= 0) {
            return List.of();
        }

        try {
            return courseDAO.findByAcademicYearAndSemester(academicYear, semester);
        } catch (Exception e) {
            LOGGER.severe("Error getting courses by academic year: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * ✅ REFACTORED: Lấy khóa học theo môn học (dùng subjectCode)
     */
    public List<Course> getCoursesBySubject(String subjectCode) {
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            return List.of();
        }

        try {
            return courseDAO.findBySubjectCode(subjectCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting courses by subject: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Thêm khóa học mới
     */
    public boolean addCourse(Course course) {
        if (course == null) {
            LOGGER.warning("Cannot add course: Course object is null");
            return false;
        }

        // ✅ REFACTORED: Validate required fields (dùng codes thay vì IDs)
        if (course.getCourseCode() == null || course.getCourseCode().trim().isEmpty() ||
                course.getSubjectCode() == null || course.getSubjectCode().trim().isEmpty() ||
                course.getTeacherUsername() == null || course.getTeacherUsername().trim().isEmpty() ||
                course.getAcademicYear() == null || course.getAcademicYear().trim().isEmpty() ||
                course.getSemester() <= 0) {

            LOGGER.warning("Cannot add course: Missing required fields");
            return false;
        }

        // Check if course code already exists
        Course existingCourse = courseDAO.findByCourseCode(course.getCourseCode());
        if (existingCourse != null) {
            LOGGER.warning("Cannot add course: Course code already exists - " + course.getCourseCode());
            return false;
        }

        try {
            boolean success = courseDAO.addCourse(course);
            if (success) {
                LOGGER.info("Course added successfully: " + course.getCourseCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error adding course: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật khóa học
     */
    public boolean updateCourse(Course course) {
        if (course == null || course.getCourseId() <= 0) {
            LOGGER.warning("Cannot update course: Invalid course data");
            return false;
        }

        try {
            boolean success = courseDAO.updateCourse(course);
            if (success) {
                LOGGER.info("Course updated successfully: " + course.getCourseCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating course: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Cập nhật trạng thái khóa học (dùng courseCode)
     */
    public boolean updateCourseStatus(String courseCode, Course.CourseStatus status) {
        if (courseCode == null || courseCode.trim().isEmpty() || status == null) {
            LOGGER.warning("Cannot update course status: Invalid input");
            return false;
        }

        try {
            // Get course to get courseId
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                LOGGER.warning("Cannot update course status: Course not found - " + courseCode);
                return false;
            }

            boolean success = courseDAO.updateCourseStatus(course.getCourseId(), status);
            if (success) {
                LOGGER.info("Course status updated successfully: " + courseCode + " -> " + status);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating course status: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Hủy/Xóa lớp học phần với logic hybrid
     * - Xóa hoàn toàn nếu: PLANNING + chưa có dữ liệu liên quan
     * - Hủy và giữ lại nếu: đã có dữ liệu liên quan hoặc ONGOING
     */
    public boolean deleteCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            LOGGER.warning("Cannot delete/cancel course: Invalid course code");
            return false;
        }

        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                LOGGER.warning("Cannot delete/cancel course: Course not found - " + courseCode);
                return false;
            }

            // Business Rule 1: Không cho xóa/hủy lớp đã hoàn thành (completed) - cần lưu
            // lịch sử
            if (course.getCourseStatus() == Course.CourseStatus.COMPLETED) {
                LOGGER.warning("Cannot delete/cancel course: Course is completed - " + courseCode);
                return false;
            }

            // Business Rule 2: Không cho xóa/hủy lớp đã bị hủy (cancelled)
            if (course.getCourseStatus() == Course.CourseStatus.CANCELLED) {
                LOGGER.warning("Cannot delete/cancel course: Course is already cancelled - " + courseCode);
                return false;
            }

            LOGGER.info(
                    "Starting to process course deletion/cancellation: " + courseCode + " (Status: "
                            + course.getCourseStatus()
                            + ", Current students: " + course.getCurrentStudents() + ")");

            // Kiểm tra dữ liệu liên quan
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            List<com.university.sms.model.Enrollment> enrollments = enrollmentDAO
                    .findByCourseCode(course.getCourseCode());

            CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
            List<com.university.sms.model.CourseRegistration> registrations = registrationDAO
                    .findByCourse(course.getCourseCode());

            GradeDAO gradeDAO = new GradeDAO();
            List<com.university.sms.model.Grade> grades = gradeDAO.getGradesByCourse(courseCode);

            boolean hasRelatedData = !enrollments.isEmpty() || !registrations.isEmpty() || !grades.isEmpty();

            // Quyết định: Xóa hoàn toàn hay Hủy và giữ lại
            if (!hasRelatedData && course.getCourseStatus() == Course.CourseStatus.PLANNING) {
                // Trường hợp 1: Xóa hoàn toàn (PLANNING + chưa có dữ liệu)
                LOGGER.info("Course has no related data, proceeding with full deletion: " + courseCode);

                // Xóa course registrations (nếu có)
                int registrationsDeleted = 0;
                for (com.university.sms.model.CourseRegistration registration : registrations) {
                    if (registrationDAO.delete(registration.getRegistrationId())) {
                        registrationsDeleted++;
                    }
                }
                if (registrationsDeleted > 0) {
                    LOGGER.info("Deleted " + registrationsDeleted + " course registrations for course "
                            + course.getCourseCode());
                }

                // Xóa course hoàn toàn
                boolean success = courseDAO.deleteCourse(course.getCourseId());
                if (success) {
                    LOGGER.info("Course deleted completely: " + courseCode);
                } else {
                    LOGGER.warning("Failed to delete course: " + courseCode);
                }
                return success;
            } else {
                // Trường hợp 2: Hủy và giữ lại dữ liệu (đã có dữ liệu hoặc ONGOING)
                LOGGER.info("Course has related data or is ongoing, proceeding with cancellation: " + courseCode);

                // Tự động hủy các course registrations đang PENDING
                int registrationsCancelled = 0;
                for (com.university.sms.model.CourseRegistration registration : registrations) {
                    // Chỉ hủy các registration đang PENDING
                    if (registration
                            .getRegistrationStatus() == com.university.sms.model.CourseRegistration.RegistrationStatus.PENDING) {
                        if (registrationDAO.cancel(registration.getRegistrationId())) {
                            registrationsCancelled++;
                            LOGGER.info("Auto-cancelled course registration " + registration.getRegistrationId()
                                    + " for course " + courseCode);
                        }
                    }
                }
                if (registrationsCancelled > 0) {
                    LOGGER.info("Cancelled " + registrationsCancelled + " pending course registrations for course "
                            + course.getCourseCode());
                }

                // Chuyển course status thành CANCELLED (không xóa dữ liệu)
                boolean success = courseDAO.updateCourseStatus(course.getCourseId(), Course.CourseStatus.CANCELLED);
                if (success) {
                    LOGGER.info("Course cancelled successfully: " + courseCode +
                            " (Cancelled " + registrationsCancelled + " pending registrations, " +
                            "kept " + enrollments.size() + " enrollments, " +
                            grades.size() + " grades)");
                } else {
                    LOGGER.warning("Failed to cancel course: " + courseCode);
                }
                return success;
            }
        } catch (Exception e) {
            LOGGER.severe("Error deleting/cancelling course: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tìm kiếm khóa học
     */
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        try {
            return courseDAO.searchCourses(keyword.trim());
        } catch (Exception e) {
            LOGGER.severe("Error searching courses: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Kiểm tra khóa học có tồn tại không
     */
    public boolean courseExists(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }

        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            return course != null;
        } catch (Exception e) {
            LOGGER.severe("Error checking course existence: " + e.getMessage());
            return false;
        }
    }

    public boolean canEnrollInCourse(String courseCode) {
        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                return false;
            }

            if (course.getCourseStatus() != Course.CourseStatus.PLANNING &&
                    course.getCourseStatus() != Course.CourseStatus.ONGOING) {
                return false;
            }

            return course.getCurrentStudents() < course.getMaxStudents();
        } catch (Exception e) {
            LOGGER.severe("Error checking course enrollment availability: " + e.getMessage());
            return false;
        }
    }

    public boolean incrementCurrentStudents(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }

        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null || course.getCurrentStudents() >= course.getMaxStudents()) {
                return false;
            }

            return courseDAO.updateCurrentStudents(course.getCourseId(), course.getCurrentStudents() + 1);
        } catch (Exception e) {
            LOGGER.severe("Error incrementing current students: " + e.getMessage());
            return false;
        }
    }

    public boolean decrementCurrentStudents(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }

        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null || course.getCurrentStudents() <= 0) {
                return false;
            }

            return courseDAO.updateCurrentStudents(course.getCourseId(), course.getCurrentStudents() - 1);
        } catch (Exception e) {
            LOGGER.severe("Error decrementing current students: " + e.getMessage());
            return false;
        }
    }

    public CourseStatistics getCourseStatistics(String academicYear, int semester) {
        try {
            List<Course> courses = courseDAO.findByAcademicYearAndSemester(academicYear, semester);

            CourseStatistics stats = new CourseStatistics();
            stats.setTotalCourses(courses.size());

            long planningCount = courses.stream()
                    .filter(c -> c.getCourseStatus() == Course.CourseStatus.PLANNING)
                    .count();
            stats.setPlanningCourses((int) planningCount);

            long ongoingCount = courses.stream()
                    .filter(c -> c.getCourseStatus() == Course.CourseStatus.ONGOING)
                    .count();
            stats.setOngoingCourses((int) ongoingCount);

            long completedCount = courses.stream()
                    .filter(c -> c.getCourseStatus() == Course.CourseStatus.COMPLETED)
                    .count();
            stats.setCompletedCourses((int) completedCount);

            long cancelledCount = courses.stream()
                    .filter(c -> c.getCourseStatus() == Course.CourseStatus.CANCELLED)
                    .count();
            stats.setCancelledCourses((int) cancelledCount);

            int totalEnrollments = courses.stream()
                    .mapToInt(Course::getCurrentStudents)
                    .sum();
            stats.setTotalEnrollments(totalEnrollments);

            return stats;
        } catch (Exception e) {
            LOGGER.severe("Error getting course statistics: " + e.getMessage());
            return new CourseStatistics();
        }
    }

    @SuppressWarnings("unused")
    private boolean isValidCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }

        String codeRegex = "^[A-Z0-9]+-\\d{4}S\\d-\\d{2}$";
        return courseCode.matches(codeRegex);
    }

    public boolean openRegistration(int courseId) {
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }
        if (course.getCourseStatus() != Course.CourseStatus.PLANNING) {
            throw new IllegalStateException("Only courses in planning status can open registration");
        }
        if (course.getRegistrationStatus() == Course.RegistrationStatus.OPEN) {
            return false;
        }
        return courseDAO.updateRegistrationStatus(courseId, Course.RegistrationStatus.OPEN);
    }

    public RegistrationClosureResult closeRegistration(int courseId) {
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }
        if (course.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
            throw new IllegalStateException("Registration must be open before closing");
        }

        List<CourseRegistration> registrations = courseRegistrationDAO.findByCourse(course.getCourseCode());
        if (registrations == null) {
            registrations = List.of();
        }
        long activeRegistrations = registrations.stream()
                .filter(r -> r.getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED)
                .count();

        courseDAO.updateRegistrationStatus(courseId, Course.RegistrationStatus.CLOSED);

        if (activeRegistrations >= MIN_STUDENTS_TO_START) {
            // Approve all pending registrations
            for (CourseRegistration registration : registrations) {
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
                    continue;
                }
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.PENDING) {
                    registration.setRegistrationStatus(CourseRegistration.RegistrationStatus.APPROVED);
                    courseRegistrationDAO.update(registration);
                }
            }

            // Ensure enrollments exist
            for (CourseRegistration registration : registrations) {
                if (registration.getRegistrationStatus() != CourseRegistration.RegistrationStatus.APPROVED) {
                    continue;
                }
                Enrollment existing = enrollmentDAO.findByStudentAndCourse(
                        registration.getStudentCode(), registration.getCourseCode());
                if (existing == null) {
                    Enrollment enrollment = new Enrollment(registration.getStudentCode(), registration.getCourseCode());
                    enrollment.setEnrollmentStatus(Enrollment.EnrollmentStatus.ENROLLED);
                    enrollmentDAO.save(enrollment);
                }
            }

            int totalEnrollments = enrollmentDAO.countByCourse(course.getCourseCode());
            courseDAO.updateCurrentStudents(courseId, totalEnrollments);
            courseDAO.updateCourseStatus(courseId, Course.CourseStatus.ONGOING);

            return new RegistrationClosureResult(true,
                    (int) activeRegistrations,
                    totalEnrollments,
                    Course.CourseStatus.ONGOING,
                    "Đã chốt đăng ký và lớp sẽ diễn ra");
        } else {
            // Cancel all active registrations
            for (CourseRegistration registration : registrations) {
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
                    continue;
                }
                registration.setRegistrationStatus(CourseRegistration.RegistrationStatus.CANCELLED);
                if (registration.getNotes() == null || registration.getNotes().isBlank()) {
                    registration.setNotes("Đợt đăng ký bị hủy do không đủ " + MIN_STUDENTS_TO_START + " sinh viên.");
                }
                courseRegistrationDAO.update(registration);
            }

            courseDAO.updateCourseStatus(courseId, Course.CourseStatus.CANCELLED);
            courseDAO.updateCurrentStudents(courseId, 0);

            return new RegistrationClosureResult(false,
                    (int) activeRegistrations,
                    0,
                    Course.CourseStatus.CANCELLED,
                    "Đã hủy lớp vì không đủ " + MIN_STUDENTS_TO_START + " sinh viên.");
        }
    }

    public static class RegistrationClosureResult {
        private final boolean started;
        private final int registrations;
        private final int enrollments;
        private final Course.CourseStatus finalStatus;
        private final String message;

        public RegistrationClosureResult(boolean started, int registrations, int enrollments,
                Course.CourseStatus finalStatus, String message) {
            this.started = started;
            this.registrations = registrations;
            this.enrollments = enrollments;
            this.finalStatus = finalStatus;
            this.message = message;
        }

        public boolean isStarted() {
            return started;
        }

        public int getRegistrations() {
            return registrations;
        }

        public int getEnrollments() {
            return enrollments;
        }

        public Course.CourseStatus getFinalStatus() {
            return finalStatus;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CourseStatistics {
        private int totalCourses;
        private int planningCourses;
        private int ongoingCourses;
        private int completedCourses;
        private int cancelledCourses;
        private int totalEnrollments;

        public int getTotalCourses() {
            return totalCourses;
        }

        public void setTotalCourses(int totalCourses) {
            this.totalCourses = totalCourses;
        }

        public int getPlanningCourses() {
            return planningCourses;
        }

        public void setPlanningCourses(int planningCourses) {
            this.planningCourses = planningCourses;
        }

        public int getOngoingCourses() {
            return ongoingCourses;
        }

        public void setOngoingCourses(int ongoingCourses) {
            this.ongoingCourses = ongoingCourses;
        }

        public int getCompletedCourses() {
            return completedCourses;
        }

        public void setCompletedCourses(int completedCourses) {
            this.completedCourses = completedCourses;
        }

        public int getCancelledCourses() {
            return cancelledCourses;
        }

        public void setCancelledCourses(int cancelledCourses) {
            this.cancelledCourses = cancelledCourses;
        }

        public int getTotalEnrollments() {
            return totalEnrollments;
        }

        public void setTotalEnrollments(int totalEnrollments) {
            this.totalEnrollments = totalEnrollments;
        }
    }

    public int getTotalCount() {
        try {
            return courseDAO.getTotalCount();
        } catch (Exception e) {
            LOGGER.severe("Error getting total course count: " + e.getMessage());
            return 0;
        }
    }
}
