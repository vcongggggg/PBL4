package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.model.Course;

import java.util.List;
import java.util.logging.Logger;

/**
 * Service xử lý các thao tác liên quan đến khóa học
 */
public class CourseService {
    private static final Logger LOGGER = Logger.getLogger(CourseService.class.getName());
    
    private CourseDAO courseDAO;

    public CourseService() {
        this.courseDAO = new CourseDAO();
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
     * Lấy khóa học theo giáo viên
     */
    public List<Course> getCoursesByTeacher(int teacherId) {
        if (teacherId <= 0) {
            return List.of();
        }

        try {
            return courseDAO.findByTeacherId(teacherId);
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
     * Lấy khóa học theo môn học
     */
    public List<Course> getCoursesBySubject(int subjectId) {
        if (subjectId <= 0) {
            return List.of();
        }

        try {
            return courseDAO.findBySubjectId(subjectId);
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

        // Validate required fields
        if (course.getCourseCode() == null || course.getCourseCode().trim().isEmpty() ||
            course.getSubjectId() <= 0 ||
            course.getTeacherId() <= 0 ||
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
     * Cập nhật trạng thái khóa học
     */
    public boolean updateCourseStatus(int courseId, Course.CourseStatus status) {
        if (courseId <= 0 || status == null) {
            LOGGER.warning("Cannot update course status: Invalid input");
            return false;
        }

        try {
            boolean success = courseDAO.updateCourseStatus(courseId, status);
            if (success) {
                LOGGER.info("Course status updated successfully: " + courseId + " -> " + status);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating course status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa khóa học
     */
    public boolean deleteCourse(int courseId) {
        if (courseId <= 0) {
            LOGGER.warning("Cannot delete course: Invalid course ID");
            return false;
        }

        try {
            Course course = courseDAO.findById(courseId);
            if (course == null) {
                LOGGER.warning("Cannot delete course: Course not found - " + courseId);
                return false;
            }

            // Check if course has enrollments
            if (course.getCurrentStudents() > 0) {
                LOGGER.warning("Cannot delete course: Course has enrolled students - " + courseId);
                return false;
            }

            boolean success = courseDAO.deleteCourse(courseId);
            if (success) {
                LOGGER.info("Course deleted successfully: " + courseId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error deleting course: " + e.getMessage());
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

    /**
     * Kiểm tra khóa học có thể đăng ký không
     */
    public boolean canEnrollInCourse(int courseId) {
        try {
            Course course = courseDAO.findById(courseId);
            if (course == null) {
                return false;
            }

            // Check if course is in planning or ongoing status
            if (course.getCourseStatus() != Course.CourseStatus.PLANNING &&
                course.getCourseStatus() != Course.CourseStatus.ONGOING) {
                return false;
            }

            // Check if course has available slots
            return course.getCurrentStudents() < course.getMaxStudents();
        } catch (Exception e) {
            LOGGER.severe("Error checking course enrollment availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tăng số lượng sinh viên hiện tại
     */
    public boolean incrementCurrentStudents(int courseId) {
        if (courseId <= 0) {
            return false;
        }

        try {
            Course course = courseDAO.findById(courseId);
            if (course == null || course.getCurrentStudents() >= course.getMaxStudents()) {
                return false;
            }

            return courseDAO.updateCurrentStudents(courseId, course.getCurrentStudents() + 1);
        } catch (Exception e) {
            LOGGER.severe("Error incrementing current students: " + e.getMessage());
            return false;
        }
    }

    /**
     * Giảm số lượng sinh viên hiện tại
     */
    public boolean decrementCurrentStudents(int courseId) {
        if (courseId <= 0) {
            return false;
        }

        try {
            Course course = courseDAO.findById(courseId);
            if (course == null || course.getCurrentStudents() <= 0) {
                return false;
            }

            return courseDAO.updateCurrentStudents(courseId, course.getCurrentStudents() - 1);
        } catch (Exception e) {
            LOGGER.severe("Error decrementing current students: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thống kê khóa học
     */
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

    /**
     * Validate course code format
     */
    private boolean isValidCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return false;
        }
        
        // Course code format: SUBJECT_YEAR_SEMESTER (e.g., CNTT101_2024_1)
        String codeRegex = "^[A-Z]+\\d+_\\d{4}_\\d+$";
        return courseCode.matches(codeRegex);
    }

    /**
     * Inner class for course statistics
     */
    public static class CourseStatistics {
        private int totalCourses;
        private int planningCourses;
        private int ongoingCourses;
        private int completedCourses;
        private int cancelledCourses;
        private int totalEnrollments;

        // Getters and setters
        public int getTotalCourses() { return totalCourses; }
        public void setTotalCourses(int totalCourses) { this.totalCourses = totalCourses; }
        
        public int getPlanningCourses() { return planningCourses; }
        public void setPlanningCourses(int planningCourses) { this.planningCourses = planningCourses; }
        
        public int getOngoingCourses() { return ongoingCourses; }
        public void setOngoingCourses(int ongoingCourses) { this.ongoingCourses = ongoingCourses; }
        
        public int getCompletedCourses() { return completedCourses; }
        public void setCompletedCourses(int completedCourses) { this.completedCourses = completedCourses; }
        
        public int getCancelledCourses() { return cancelledCourses; }
        public void setCancelledCourses(int cancelledCourses) { this.cancelledCourses = cancelledCourses; }
        
        public int getTotalEnrollments() { return totalEnrollments; }
        public void setTotalEnrollments(int totalEnrollments) { this.totalEnrollments = totalEnrollments; }
    }
}
