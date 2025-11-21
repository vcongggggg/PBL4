package com.university.sms.service;

import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.util.DatabaseConnection;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.Student;
import com.university.sms.model.Transcript;
import com.university.sms.model.Transcript.CourseRecord;
import com.university.sms.model.Transcript.SemesterRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer cho Transcript (Học bạ)
 */
public class TranscriptService {
    private static final Logger LOGGER = Logger.getLogger(TranscriptService.class.getName());

    private final StudentDAO studentDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final CourseDAO courseDAO;

    public TranscriptService() {
        this.studentDAO = new StudentDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.courseDAO = new CourseDAO();
    }

    /**
     * Tạo học bạ đầy đủ cho sinh viên
     */
    public Transcript generateTranscript(String studentCode) {
        Transcript transcript = new Transcript();

        try {
            // Get student info
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                LOGGER.warning("Không tìm thấy sinh viên: " + studentCode);
                return null;
            }

            transcript.setStudentId(student.getStudentId());
            transcript.setStudentCode(student.getStudentCode());
            transcript.setStudentName(student.getFullName());
            transcript.setFacultyName(student.getFacultyName());
            transcript.setClassName(student.getClassName());

            // Get all enrollments
            List<Enrollment> enrollments = enrollmentDAO.findByStudentCode(studentCode);

            // Group by semester
            Map<String, SemesterRecord> semesterMap = new HashMap<>();

            int totalCompleted = 0;
            int totalFailed = 0;
            int totalInProgress = 0;
            int totalCreditsRegistered = 0;

            for (Enrollment enrollment : enrollments) {
                Course course = courseDAO.findByCourseCode(enrollment.getCourseCode());
                if (course == null)
                    continue;

                // Create semester key
                String semesterKey = course.getAcademicYear() + "-" + course.getSemester();

                // Get or create semester record
                SemesterRecord semesterRecord = semesterMap.get(semesterKey);
                if (semesterRecord == null) {
                    semesterRecord = new SemesterRecord(course.getAcademicYear(), course.getSemester());
                    semesterMap.put(semesterKey, semesterRecord);
                }

                // Add course record
                CourseRecord courseRecord = new CourseRecord(enrollment, course);
                semesterRecord.addCourse(courseRecord);

                // Update statistics
                totalCreditsRegistered += course.getCredits();

                if (enrollment.getEnrollmentStatus() == Enrollment.EnrollmentStatus.COMPLETED) {
                    totalCompleted++;
                } else if (enrollment.getEnrollmentStatus() == Enrollment.EnrollmentStatus.FAILED) {
                    totalFailed++;
                } else if (enrollment.getEnrollmentStatus() == Enrollment.EnrollmentStatus.ENROLLED) {
                    totalInProgress++;
                }
            }

            // Sort semesters and calculate GPA
            List<SemesterRecord> sortedSemesters = new ArrayList<>(semesterMap.values());
            sortedSemesters.sort((s1, s2) -> {
                int yearCompare = s1.getAcademicYear().compareTo(s2.getAcademicYear());
                if (yearCompare != 0)
                    return yearCompare;
                return Integer.compare(s1.getSemester(), s2.getSemester());
            });

            for (SemesterRecord semester : sortedSemesters) {
                semester.calculateSemesterGPA();
                transcript.addSemesterRecord(semester);
            }

            // Set statistics
            transcript.setTotalCoursesCompleted(totalCompleted);
            transcript.setTotalCoursesFailed(totalFailed);
            transcript.setTotalCoursesInProgress(totalInProgress);
            transcript.setTotalCreditsRegistered(totalCreditsRegistered);

            // Calculate overall GPA
            transcript.calculateGPA();

            // Get latest semester GPA
            if (!sortedSemesters.isEmpty()) {
                SemesterRecord latestSemester = sortedSemesters.get(sortedSemesters.size() - 1);
                transcript.setSemesterGPA(latestSemester.getSemesterGPA());
            }

            LOGGER.info("Đã tạo học bạ cho sinh viên " + studentCode
                    + " - GPA: " + transcript.getCumulativeGPA()
                    + " - Xếp loại: " + transcript.getAcademicRank());

            return transcript;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tạo học bạ cho sinh viên " + studentCode, e);
            return null;
        }
    }

    /**
     * Lấy học bạ theo học kỳ cụ thể
     */
    public SemesterRecord getSemesterTranscript(String studentCode, String academicYear, int semester) {
        try {
            Transcript fullTranscript = generateTranscript(studentCode);
            if (fullTranscript == null)
                return null;

            for (SemesterRecord record : fullTranscript.getSemesterRecords()) {
                if (record.getAcademicYear().equals(academicYear)
                        && record.getSemester() == semester) {
                    return record;
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy học bạ học kỳ", e);
        }

        return null;
    }

    /**
     * Tính GPA dự kiến nếu thêm điểm mới
     */
    public Map<String, Object> calculateProjectedGPA(String studentCode, int newCredits, double newGradePoints) {
        Map<String, Object> result = new HashMap<>();

        try {
            Transcript transcript = generateTranscript(studentCode);
            if (transcript == null) {
                result.put("success", false);
                result.put("message", "Cannot load transcript");
                return result;
            }

            double currentGPA = transcript.getCumulativeGPA().doubleValue();
            int currentCredits = transcript.getTotalCreditsEarned();

            // Calculate projected GPA
            double totalWeightedPoints = (currentGPA * currentCredits) + (newGradePoints * newCredits);
            double totalCredits = currentCredits + newCredits;
            double projectedGPA = totalWeightedPoints / totalCredits;

            result.put("success", true);
            result.put("currentGPA", currentGPA);
            result.put("projectedGPA", Math.round(projectedGPA * 100.0) / 100.0);
            result.put("currentCredits", currentCredits);
            result.put("projectedCredits", (int) totalCredits);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính GPA dự kiến", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Lấy danh sách sinh viên xuất sắc (GPA >= 3.6)
     */
    public List<Transcript> getHonorStudents(String facultyCode) {
        List<Transcript> honorStudents = new ArrayList<>();

        try {
            List<Student> students;
            // If facultyCode is null or empty, get all students
            if (facultyCode == null || facultyCode.isEmpty()) {
                students = studentDAO.findAll();
            } else {
                students = studentDAO.findByFacultyCode(facultyCode);
            }

            for (Student student : students) {
                Transcript transcript = generateTranscript(student.getStudentCode());
                if (transcript != null
                        && transcript.getCumulativeGPA() != null
                        && transcript.getCumulativeGPA().doubleValue() >= 3.6) {
                    honorStudents.add(transcript);
                }
            }

            // Sort by GPA descending
            honorStudents.sort((t1, t2) -> {
                if (t1.getCumulativeGPA() == null && t2.getCumulativeGPA() == null)
                    return 0;
                if (t1.getCumulativeGPA() == null)
                    return 1;
                if (t2.getCumulativeGPA() == null)
                    return -1;
                return t2.getCumulativeGPA().compareTo(t1.getCumulativeGPA());
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách sinh viên xuất sắc", e);
        }

        return honorStudents;
    }

    /**
     * Tạo báo cáo thống kê điểm toàn khoa
     */
    public Map<String, Object> getFacultyStatistics(String facultyCode) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<Student> students = studentDAO.findByFacultyCode(facultyCode);

            int totalStudents = students.size();
            int excellentCount = 0; // >= 3.6
            int goodCount = 0; // >= 3.2
            int fairCount = 0; // >= 2.5
            int averageCount = 0; // >= 2.0
            int poorCount = 0; // < 2.0

            double totalGPA = 0;

            for (Student student : students) {
                Transcript transcript = generateTranscript(student.getStudentCode());
                if (transcript != null) {
                    double gpa = transcript.getCumulativeGPA().doubleValue();
                    totalGPA += gpa;

                    if (gpa >= 3.6)
                        excellentCount++;
                    else if (gpa >= 3.2)
                        goodCount++;
                    else if (gpa >= 2.5)
                        fairCount++;
                    else if (gpa >= 2.0)
                        averageCount++;
                    else if (gpa > 0)
                        poorCount++;
                }
            }

            stats.put("totalStudents", totalStudents);
            stats.put("averageGPA", totalStudents > 0 ? Math.round(totalGPA / totalStudents * 100.0) / 100.0 : 0);
            stats.put("excellentCount", excellentCount);
            stats.put("goodCount", goodCount);
            stats.put("fairCount", fairCount);
            stats.put("averageCount", averageCount);
            stats.put("poorCount", poorCount);

            // Percentages
            if (totalStudents > 0) {
                stats.put("excellentPercent", Math.round(excellentCount * 100.0 / totalStudents));
                stats.put("goodPercent", Math.round(goodCount * 100.0 / totalStudents));
                stats.put("fairPercent", Math.round(fairCount * 100.0 / totalStudents));
                stats.put("averagePercent", Math.round(averageCount * 100.0 / totalStudents));
                stats.put("poorPercent", Math.round(poorCount * 100.0 / totalStudents));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính thống kê khoa", e);
        }

        return stats;
    }

    /**
    /**
     * Lấy xu hướng GPA trung bình theo học kỳ cho toàn trường hoặc theo khoa
     */
    public Map<String, Double> getGpaTrendBySemester(String facultyCode) {
        Map<String, Double> trend = new LinkedHashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.academic_year, c.semester, AVG(e.grade_points) AS avg_gpa ")
                .append("FROM enrollments e ")
                .append("JOIN courses c ON e.course_code = c.course_code ")
                .append("JOIN students s ON e.student_code = s.student_code ")
                .append("WHERE e.enrollment_status = 'completed' ")
                .append("AND e.grade_points IS NOT NULL ");

        if (facultyCode != null && !facultyCode.isBlank()) {
            sql.append("AND s.faculty_code = ? ");
        }

        sql.append("GROUP BY c.academic_year, c.semester ")
                .append("ORDER BY c.academic_year, c.semester");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (facultyCode != null && !facultyCode.isBlank()) {
                stmt.setString(1, facultyCode);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String academicYear = rs.getString("academic_year");
                    int semester = rs.getInt("semester");
                    java.math.BigDecimal avg = rs.getBigDecimal("avg_gpa");
                    if (avg == null) {
                        continue;
                    }
                    double average = avg.doubleValue();
                    String key = academicYear + "-" + semester;
                    trend.put(key, Math.round(average * 100.0) / 100.0);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating GPA trend by semester", e);
        }

        return trend;
    }

    /**
     * Lấy danh sách mã môn học đã hoàn thành của sinh viên
     */
    public List<String> getCompletedSubjectCodes(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return enrollmentDAO.findCompletedSubjectCodes(studentCode.trim());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách môn đã hoàn thành cho sinh viên " + studentCode, e);
            return Collections.emptyList();
        }
    }
    }
}
