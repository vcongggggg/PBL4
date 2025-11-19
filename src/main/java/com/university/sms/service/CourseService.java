package com.university.sms.service;

import com.university.sms.dao.ClassOpeningRequestDAO;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.CourseRegistrationDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.GradeDAO;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Enrollment;

import java.util.List;
import java.util.logging.Level;
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
    private final ClassOpeningRequestDAO classOpeningRequestDAO;

    public CourseService() {
        this.courseDAO = new CourseDAO();
        this.courseRegistrationDAO = new CourseRegistrationDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.classOpeningRequestDAO = new ClassOpeningRequestDAO();
    }

    /**
     * Lấy tất cả khóa học
     */
    public List<Course> getAllCourses() {
        try {
            return courseDAO.findAll();
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách tất cả khóa học: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy khóa học theo ID: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy khóa học theo mã: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy danh sách khóa học theo giáo viên: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy danh sách khóa học theo năm học: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy danh sách khóa học theo môn học: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Thêm khóa học mới
     */
    public boolean addCourse(Course course) {
        if (course == null) {
            LOGGER.warning("Không thể thêm khóa học: Đối tượng khóa học là null");
            return false;
        }

        // ✅ REFACTORED: Validate required fields (dùng codes thay vì IDs)
        if (course.getCourseCode() == null || course.getCourseCode().trim().isEmpty() ||
                course.getSubjectCode() == null || course.getSubjectCode().trim().isEmpty() ||
                course.getTeacherUsername() == null || course.getTeacherUsername().trim().isEmpty() ||
                course.getAcademicYear() == null || course.getAcademicYear().trim().isEmpty() ||
                course.getSemester() <= 0) {

            LOGGER.warning("Không thể thêm khóa học: Thiếu các trường bắt buộc");
            return false;
        }

        // Check if course code already exists
        Course existingCourse = courseDAO.findByCourseCode(course.getCourseCode());
        if (existingCourse != null) {
            LOGGER.warning("Không thể thêm khóa học: Mã khóa học đã tồn tại - " + course.getCourseCode());
            return false;
        }

        try {
            boolean success = courseDAO.addCourse(course);
            if (success) {
                LOGGER.info("Đã thêm khóa học thành công: " + course.getCourseCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi thêm khóa học: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật khóa học
     */
    public boolean updateCourse(Course course) {
        if (course == null || course.getCourseId() <= 0) {
            LOGGER.warning("Không thể cập nhật khóa học: Dữ liệu khóa học không hợp lệ");
            return false;
        }

        try {
            boolean success = courseDAO.updateCourse(course);
            if (success) {
                LOGGER.info("Đã cập nhật khóa học thành công: " + course.getCourseCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi cập nhật khóa học: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ REFACTORED: Cập nhật trạng thái khóa học (dùng courseCode)
     */
    public boolean updateCourseStatus(String courseCode, Course.CourseStatus status) {
        if (courseCode == null || courseCode.trim().isEmpty() || status == null) {
            LOGGER.warning("Không thể cập nhật trạng thái khóa học: Dữ liệu đầu vào không hợp lệ");
            return false;
        }

        try {
            // Get course to get courseId
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                LOGGER.warning("Không thể cập nhật trạng thái khóa học: Không tìm thấy khóa học - " + courseCode);
                return false;
            }

            boolean success = courseDAO.updateCourseStatus(course.getCourseId(), status);
            if (success) {
                LOGGER.info("Đã cập nhật trạng thái khóa học thành công: " + courseCode + " -> " + status);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi cập nhật trạng thái khóa học: " + e.getMessage());
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
            LOGGER.warning("Không thể xóa/hủy khóa học: Mã khóa học không hợp lệ");
            return false;
        }

        try {
            Course course = courseDAO.findByCourseCode(courseCode);
            if (course == null) {
                LOGGER.warning("Không thể xóa/hủy khóa học: Không tìm thấy khóa học - " + courseCode);
                return false;
            }

            // Business Rule 1: Không cho xóa/hủy lớp đã hoàn thành (completed) - cần lưu
            // lịch sử
            if (course.getCourseStatus() == Course.CourseStatus.COMPLETED) {
                LOGGER.warning("Không thể xóa/hủy khóa học: Khóa học đã hoàn thành - " + courseCode);
                return false;
            }

            // Business Rule 2: Không cho xóa/hủy lớp đã bị hủy (cancelled)
            if (course.getCourseStatus() == Course.CourseStatus.CANCELLED) {
                LOGGER.warning("Không thể xóa/hủy khóa học: Khóa học đã bị hủy - " + courseCode);
                return false;
            }

            LOGGER.info(
                    "Bắt đầu xử lý xóa/hủy khóa học: " + courseCode + " (Trạng thái: "
                            + course.getCourseStatus()
                            + ", Số sinh viên hiện tại: " + course.getCurrentStudents() + ")");

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
                LOGGER.info("Khóa học không có dữ liệu liên quan, tiến hành xóa hoàn toàn: " + courseCode);

                // Xóa course registrations (nếu có)
                int registrationsDeleted = 0;
                for (com.university.sms.model.CourseRegistration registration : registrations) {
                    if (registrationDAO.delete(registration.getRegistrationId())) {
                        registrationsDeleted++;
                    }
                }
                if (registrationsDeleted > 0) {
                    LOGGER.info("Đã xóa " + registrationsDeleted + " đăng ký khóa học cho khóa học "
                            + course.getCourseCode());
                }

                // Xóa course hoàn toàn
                boolean success = courseDAO.deleteCourse(course.getCourseId());
                if (success) {
                    LOGGER.info("Đã xóa hoàn toàn khóa học: " + courseCode);
                } else {
                    LOGGER.warning("Không thể xóa khóa học: " + courseCode);
                }
                return success;
            } else {
                // Trường hợp 2: Hủy và giữ lại dữ liệu (đã có dữ liệu hoặc ONGOING)
                LOGGER.info("Khóa học có dữ liệu liên quan hoặc đang diễn ra, tiến hành hủy: " + courseCode);

                // Tự động hủy các course registrations đang PENDING
                int registrationsCancelled = 0;
                for (com.university.sms.model.CourseRegistration registration : registrations) {
                    // Chỉ hủy các registration đang PENDING
                    if (registration
                            .getRegistrationStatus() == com.university.sms.model.CourseRegistration.RegistrationStatus.PENDING) {
                        if (registrationDAO.cancel(registration.getRegistrationId())) {
                            registrationsCancelled++;
                            LOGGER.info("Đã tự động hủy đăng ký khóa học " + registration.getRegistrationId()
                                    + " cho khóa học " + courseCode);
                        }
                    }
                }
                if (registrationsCancelled > 0) {
                    LOGGER.info("Đã hủy " + registrationsCancelled + " đăng ký khóa học đang chờ cho khóa học "
                            + course.getCourseCode());
                }

                // Chuyển course status thành CANCELLED (không xóa dữ liệu)
                boolean success = courseDAO.updateCourseStatus(course.getCourseId(), Course.CourseStatus.CANCELLED);
                if (success) {
                    LOGGER.info("Đã hủy khóa học thành công: " + courseCode +
                            " (Đã hủy " + registrationsCancelled + " đăng ký đang chờ, " +
                            "giữ lại " + enrollments.size() + " ghi danh, " +
                            grades.size() + " điểm)");
                } else {
                    LOGGER.warning("Không thể hủy khóa học: " + courseCode);
                }
                return success;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa/hủy khóa học: " + e.getMessage(), e);
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
            LOGGER.severe("Lỗi khi tìm kiếm khóa học: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi kiểm tra sự tồn tại của khóa học: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi kiểm tra khả năng đăng ký khóa học: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi tăng số sinh viên hiện tại: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi giảm số sinh viên hiện tại: " + e.getMessage());
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
            LOGGER.severe("Lỗi khi lấy thống kê khóa học: " + e.getMessage());
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

        // Đếm số đăng ký đang hoạt động (PENDING và APPROVED, không tính CANCELLED)
        long activeRegistrations = registrations.stream()
                .filter(r -> r.getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED)
                .count();

        // Đóng đợt đăng ký
        courseDAO.updateRegistrationStatus(courseId, Course.RegistrationStatus.CLOSED);

        if (activeRegistrations >= MIN_STUDENTS_TO_START) {
            // Nếu đủ >= 30 sinh viên: approve tất cả đăng ký PENDING và tạo enrollment
            // Bước 1: Approve tất cả đăng ký PENDING -> APPROVED
            for (CourseRegistration registration : registrations) {
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
                    continue;
                }
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.PENDING) {
                    registration.setRegistrationStatus(CourseRegistration.RegistrationStatus.APPROVED);
                    courseRegistrationDAO.update(registration);
                }
            }

            // Bước 2: Tạo enrollment với status ENROLLED cho tất cả đăng ký đã APPROVED
            // Reload danh sách registrations để lấy status mới nhất sau khi approve
            registrations = courseRegistrationDAO.findByCourse(course.getCourseCode());
            if (registrations == null) {
                registrations = List.of();
            }

            for (CourseRegistration registration : registrations) {
                // Chỉ xử lý các registration đã APPROVED (bao gồm cả PENDING đã được approve ở
                // trên)
                if (registration.getRegistrationStatus() != CourseRegistration.RegistrationStatus.APPROVED) {
                    continue;
                }
                // Kiểm tra xem đã có enrollment chưa
                Enrollment existing = enrollmentDAO.findByStudentAndCourse(
                        registration.getStudentCode(), registration.getCourseCode());
                if (existing == null) {
                    // Tạo enrollment mới với status ENROLLED
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

            // Tự động reject yêu cầu mở lớp nếu lớp không mở thành công
            LOGGER.info("Bắt đầu tìm và reject yêu cầu mở lớp cho lớp " + course.getCourseCode() +
                    " (courseId: " + courseId + ")");
            try {
                ClassOpeningRequest request = classOpeningRequestDAO.findByApprovedCourseCode(course.getCourseCode());
                if (request == null) {
                    LOGGER.warning("Không tìm thấy yêu cầu mở lớp cho lớp " + course.getCourseCode() +
                            " - có thể lớp này không được tạo từ yêu cầu mở lớp");
                } else {
                    LOGGER.info("Tìm thấy yêu cầu mở lớp: requestId=" + request.getRequestId() +
                            ", status=" + request.getRequestStatus() +
                            ", approvedCourseCode=" + request.getApprovedCourseCode());

                    if (request.getRequestStatus() != ClassOpeningRequest.RequestStatus.APPROVED) {
                        LOGGER.warning("Yêu cầu mở lớp " + request.getRequestId() +
                                " cho lớp " + course.getCourseCode() +
                                " không ở trạng thái ĐÃ DUYỆT (trạng thái hiện tại: " + request.getRequestStatus() +
                                ") - bỏ qua tự động hủy");
                    } else {
                        String rejectReason = "Lớp không mở thành công do không đủ " + MIN_STUDENTS_TO_START
                                + " sinh viên đăng ký.";
                        LOGGER.info("Đang reject yêu cầu mở lớp " + request.getRequestId() +
                                " với lý do: " + rejectReason);

                        boolean rejected = classOpeningRequestDAO.reject(request.getRequestId(), "SYSTEM",
                                rejectReason);
                        if (rejected) {
                            LOGGER.info("✅ Đã tự động hủy yêu cầu mở lớp " + request.getRequestId() +
                                    " vì lớp " + course.getCourseCode()
                                    + " bị hủy do không đủ sinh viên");
                        } else {
                            LOGGER.severe("❌ Không thể tự động hủy yêu cầu mở lớp " + request.getRequestId() +
                                    " cho lớp " + course.getCourseCode() + " - reject() trả về false");
                        }
                    }
                }
            } catch (Exception e) {
                // Log error nhưng không throw để không ảnh hưởng đến flow chính
                LOGGER.log(Level.SEVERE,
                        "Lỗi khi tự động hủy yêu cầu mở lớp cho lớp " + course.getCourseCode() + ": " + e.getMessage(),
                        e);
            }

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
            LOGGER.severe("Lỗi khi lấy tổng số khóa học: " + e.getMessage());
            return 0;
        }
    }
}
