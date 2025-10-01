package com.university.sms.dao;

import com.university.sms.model.Course;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho Course
 */
public class CourseDAO {
    private static final Logger LOGGER = Logger.getLogger(CourseDAO.class.getName());

    /**
     * Lấy tất cả khóa học
     */
    public List<Course> findAll() {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "ORDER BY c.academic_year DESC, c.semester DESC, c.course_code";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all courses", e);
        }
        
        return courses;
    }

    /**
     * Tìm khóa học theo ID
     */
    public Course findById(int courseId) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding course by ID: " + courseId, e);
        }
        
        return null;
    }

    /**
     * Tìm khóa học theo mã khóa học
     */
    public Course findByCourseCode(String courseCode) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.course_code = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, courseCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourse(rs);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding course by code: " + courseCode, e);
        }
        
        return null;
    }

    /**
     * Tìm khóa học theo giáo viên
     */
    public List<Course> findByTeacherId(int teacherId) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.teacher_id = ? ORDER BY c.academic_year DESC, c.semester DESC";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, teacherId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by teacher ID: " + teacherId, e);
        }
        
        return courses;
    }

    /**
     * Tìm khóa học theo năm học và học kỳ
     */
    public List<Course> findByAcademicYearAndSemester(String academicYear, int semester) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.academic_year = ? AND c.semester = ? ORDER BY c.course_code";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, academicYear);
            stmt.setInt(2, semester);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by academic year and semester", e);
        }
        
        return courses;
    }

    /**
     * Tìm khóa học theo môn học
     */
    public List<Course> findBySubjectId(int subjectId) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.subject_id = ? ORDER BY c.academic_year DESC, c.semester DESC";
        
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, subjectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding courses by subject ID: " + subjectId, e);
        }
        
        return courses;
    }

    /**
     * Thêm khóa học mới
     */
    public boolean addCourse(Course course) {
        String sql = "INSERT INTO courses (course_code, subject_id, teacher_id, class_id, " +
                    "academic_year, semester, schedule_day, schedule_time, room, max_students, " +
                    "start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, course.getCourseCode());
            stmt.setInt(2, course.getSubjectId());
            stmt.setInt(3, course.getTeacherId());
            
            if (course.getClassId() != null) {
                stmt.setInt(4, course.getClassId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            stmt.setString(5, course.getAcademicYear());
            stmt.setInt(6, course.getSemester());
            stmt.setString(7, course.getScheduleDay());
            stmt.setString(8, course.getScheduleTime());
            stmt.setString(9, course.getRoom());
            stmt.setInt(10, course.getMaxStudents());
            stmt.setDate(11, course.getStartDate());
            stmt.setDate(12, course.getEndDate());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        course.setCourseId(rs.getInt(1));
                    }
                }
                LOGGER.info("Course added successfully: " + course.getCourseCode());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding course: " + course.getCourseCode(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật khóa học
     */
    public boolean updateCourse(Course course) {
        String sql = "UPDATE courses SET schedule_day = ?, schedule_time = ?, room = ?, " +
                    "max_students = ?, start_date = ?, end_date = ? WHERE course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, course.getScheduleDay());
            stmt.setString(2, course.getScheduleTime());
            stmt.setString(3, course.getRoom());
            stmt.setInt(4, course.getMaxStudents());
            stmt.setDate(5, course.getStartDate());
            stmt.setDate(6, course.getEndDate());
            stmt.setInt(7, course.getCourseId());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Course updated successfully: " + course.getCourseId());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course: " + course.getCourseId(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật trạng thái khóa học
     */
    public boolean updateCourseStatus(int courseId, Course.CourseStatus status) {
        String sql = "UPDATE courses SET course_status = ? WHERE course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, courseId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Course status updated successfully: " + courseId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course status: " + courseId, e);
        }
        
        return false;
    }

    /**
     * Cập nhật số lượng sinh viên hiện tại
     */
    public boolean updateCurrentStudents(int courseId, int currentStudents) {
        String sql = "UPDATE courses SET current_students = ? WHERE course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentStudents);
            stmt.setInt(2, courseId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Course current students updated: " + courseId + " -> " + currentStudents);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating course current students: " + courseId, e);
        }
        
        return false;
    }

    /**
     * Xóa khóa học
     */
    public boolean deleteCourse(int courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                LOGGER.info("Course deleted successfully: " + courseId);
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting course: " + courseId, e);
        }
        
        return false;
    }

    /**
     * Tìm kiếm khóa học
     */
    public List<Course> searchCourses(String keyword) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                    "u.full_name AS teacher_name, cl.class_name " +
                    "FROM courses c " +
                    "JOIN subjects sub ON c.subject_id = sub.subject_id " +
                    "JOIN users u ON c.teacher_id = u.user_id " +
                    "LEFT JOIN classes cl ON c.class_id = cl.class_id " +
                    "WHERE c.course_code LIKE ? OR sub.subject_name LIKE ? OR u.full_name LIKE ? " +
                    "ORDER BY c.academic_year DESC, c.semester DESC";
        
        List<Course> courses = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching courses with keyword: " + keyword, e);
        }
        
        return courses;
    }

    /**
     * Map ResultSet to Course object
     */
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setSubjectId(rs.getInt("subject_id"));
        course.setTeacherId(rs.getInt("teacher_id"));
        
        int classId = rs.getInt("class_id");
        if (!rs.wasNull()) {
            course.setClassId(classId);
        }
        
        course.setAcademicYear(rs.getString("academic_year"));
        course.setSemester(rs.getInt("semester"));
        course.setScheduleDay(rs.getString("schedule_day"));
        course.setScheduleTime(rs.getString("schedule_time"));
        course.setRoom(rs.getString("room"));
        course.setMaxStudents(rs.getInt("max_students"));
        course.setCurrentStudents(rs.getInt("current_students"));
        
        String status = rs.getString("course_status");
        if (status != null) {
            course.setCourseStatus(Course.CourseStatus.valueOf(status.toUpperCase()));
        }
        
        course.setStartDate(rs.getDate("start_date"));
        course.setEndDate(rs.getDate("end_date"));
        course.setCreatedAt(rs.getTimestamp("created_at"));
        
        // Related information
        course.setSubjectName(rs.getString("subject_name"));
        course.setSubjectCode(rs.getString("subject_code"));
        course.setCredits(rs.getInt("credits"));
        course.setTeacherName(rs.getString("teacher_name"));
        course.setClassName(rs.getString("class_name"));
        
        return course;
    }
}


