package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.TimetableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer cho Timetable
 */
public class TimetableService {
    private static final Logger LOGGER = Logger.getLogger(TimetableService.class.getName());

    private final CourseDAO courseDAO;
    private final EnrollmentDAO enrollmentDAO;

    // Predefined colors for different subjects/faculties
    private static final String[] COLORS = {
            "#FFB3BA", "#FFDFBA", "#FFFFBA", "#BAFFC9", "#BAE1FF",
            "#FFB3E6", "#E6B3FF", "#D4A5A5", "#A5D4D4", "#D4D4A5"
    };

    public TimetableService() {
        this.courseDAO = new CourseDAO();
        this.enrollmentDAO = new EnrollmentDAO();
    }

    /**
     * Lấy thời khóa biểu của sinh viên
     */
    public List<TimetableEntry> getStudentTimetable(int studentId) {
        List<TimetableEntry> timetable = new ArrayList<>();

        try {
            // Get all enrollments of student
            List<Enrollment> enrollments = enrollmentDAO.findByStudentId(studentId);
            LOGGER.info("TimetableService: Found " + enrollments.size() + " enrollments for student " + studentId);

            Map<String, String> colorMap = new HashMap<>();
            int colorIndex = 0;
            int filteredByEnrollmentStatus = 0;
            int filteredByCourseStatus = 0;
            int filteredBySchedule = 0;

            for (Enrollment enrollment : enrollments) {
                // Only show enrolled courses
                if (enrollment.getEnrollmentStatus() == Enrollment.EnrollmentStatus.ENROLLED) {
                    Course course = courseDAO.findById(enrollment.getCourseId());
                    if (course != null) {
                        // Only show ONGOING courses
                        if (course.getCourseStatus() == Course.CourseStatus.ONGOING) {
                            TimetableEntry entry = new TimetableEntry(course);

                            // Check if schedule is valid
                            if (entry.getDayOfWeek() == null) {
                                LOGGER.warning("  Course " + course.getCourseCode()
                                        + " - Filtered: Invalid schedule (dayOfWeek is null), scheduleDay: "
                                        + course.getScheduleDay());
                                filteredBySchedule++;
                                continue;
                            }

                            // Assign color based on subject
                            String subjectKey = course.getSubjectName();
                            if (!colorMap.containsKey(subjectKey)) {
                                colorMap.put(subjectKey, COLORS[colorIndex % COLORS.length]);
                                colorIndex++;
                            }
                            entry.setColor(colorMap.get(subjectKey));

                            timetable.add(entry);
                        } else {
                            filteredByCourseStatus++;
                        }
                    }
                } else {
                    filteredByEnrollmentStatus++;
                }
            }

            LOGGER.info("TimetableService Summary for student " + studentId + ":");
            LOGGER.info("  Total enrollments: " + enrollments.size());
            LOGGER.info("  Added to timetable: " + timetable.size());
            if (filteredByEnrollmentStatus > 0) {
                LOGGER.info("  Filtered by enrollment status: " + filteredByEnrollmentStatus);
            }
            if (filteredByCourseStatus > 0) {
                LOGGER.info("  Filtered by course status: " + filteredByCourseStatus);
            }
            if (filteredBySchedule > 0) {
                LOGGER.info("  Filtered by invalid schedule: " + filteredBySchedule);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading student timetable", e);
        }

        return timetable;
    }

    /**
     * Lấy thời khóa biểu của giảng viên
     * Note: CourseDAO.findByTeacherId() đã filter chỉ lấy ONGOING
     */
    public List<TimetableEntry> getTeacherTimetable(int teacherId) {
        List<TimetableEntry> timetable = new ArrayList<>();

        try {
            List<Course> courses = courseDAO.findByTeacherId(teacherId);
            LOGGER.info("TimetableService: Found " + courses.size() + " active courses for teacher " + teacherId);

            Map<String, String> colorMap = new HashMap<>();
            int colorIndex = 0;
            int filteredBySchedule = 0;

            for (Course course : courses) {
                TimetableEntry entry = new TimetableEntry(course);

                // Check if schedule is valid
                if (entry.getDayOfWeek() == null) {
                    LOGGER.warning("  Course " + course.getCourseCode()
                            + " - Filtered: Invalid schedule (dayOfWeek is null), scheduleDay: "
                            + course.getScheduleDay());
                    filteredBySchedule++;
                    continue;
                }

                // Assign color
                String subjectKey = course.getSubjectName();
                if (!colorMap.containsKey(subjectKey)) {
                    colorMap.put(subjectKey, COLORS[colorIndex % COLORS.length]);
                    colorIndex++;
                }
                entry.setColor(colorMap.get(subjectKey));

                timetable.add(entry);
            }

            LOGGER.info("TimetableService Summary for teacher " + teacherId + ":");
            LOGGER.info("  Total active courses: " + courses.size());
            LOGGER.info("  Added to timetable: " + timetable.size());
            LOGGER.info("  Filtered by invalid schedule: " + filteredBySchedule);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading teacher timetable", e);
        }

        return timetable;
    }

    /**
     * Lấy thời khóa biểu của một lớp
     */
    public List<TimetableEntry> getClassTimetable(int classId) {
        List<TimetableEntry> timetable = new ArrayList<>();

        try {
            List<Course> courses = courseDAO.findByClassId(classId);

            Map<String, String> colorMap = new HashMap<>();
            int colorIndex = 0;

            for (Course course : courses) {
                if (course.getCourseStatus() == Course.CourseStatus.ONGOING) {
                    TimetableEntry entry = new TimetableEntry(course);

                    // Assign color
                    String subjectKey = course.getSubjectName();
                    if (!colorMap.containsKey(subjectKey)) {
                        colorMap.put(subjectKey, COLORS[colorIndex % COLORS.length]);
                        colorIndex++;
                    }
                    entry.setColor(colorMap.get(subjectKey));

                    timetable.add(entry);
                }
            }

            LOGGER.info("Loaded timetable for class " + classId + ": " + timetable.size() + " entries");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading class timetable", e);
        }

        return timetable;
    }

    /**
     * Kiểm tra xung đột lịch
     */
    public List<TimetableEntry> findConflicts(List<TimetableEntry> timetable) {
        List<TimetableEntry> conflicts = new ArrayList<>();

        for (int i = 0; i < timetable.size(); i++) {
            for (int j = i + 1; j < timetable.size(); j++) {
                TimetableEntry entry1 = timetable.get(i);
                TimetableEntry entry2 = timetable.get(j);

                if (entry1.conflictsWith(entry2)) {
                    if (!conflicts.contains(entry1)) {
                        conflicts.add(entry1);
                    }
                    if (!conflicts.contains(entry2)) {
                        conflicts.add(entry2);
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * Validate lịch học trước khi đăng ký
     */
    public boolean validateSchedule(int studentId, int newCourseId) {
        try {
            // Get current timetable
            List<TimetableEntry> currentTimetable = getStudentTimetable(studentId);

            // Get new course
            Course newCourse = courseDAO.findById(newCourseId);
            if (newCourse == null) {
                return false;
            }

            TimetableEntry newEntry = new TimetableEntry(newCourse);

            // Check conflicts
            for (TimetableEntry existing : currentTimetable) {
                if (existing.conflictsWith(newEntry)) {
                    LOGGER.warning("Schedule conflict detected: " + existing.getSubjectName()
                            + " conflicts with " + newEntry.getSubjectName());
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating schedule", e);
            return false;
        }
    }
}
