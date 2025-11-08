package com.university.sms.service;

import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Student;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

public class StudentService {
    private static final Logger LOGGER = Logger.getLogger(StudentService.class.getName());

    private StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
    }

    public boolean addStudent(Student student) {
        if (student == null) {
            LOGGER.warning("Cannot add student: Student object is null");
            return false;
        }

        if (student.getUsername() == null || student.getUsername().trim().isEmpty() ||
                student.getStudentCode() == null || student.getStudentCode().trim().isEmpty() ||
                student.getFacultyCode() == null || student.getFacultyCode().trim().isEmpty() ||
                student.getAdmissionYear() <= 0) {

            LOGGER.warning("Cannot add student: Missing required fields");
            return false;
        }

        Student existingStudent = studentDAO.findByStudentCode(student.getStudentCode());
        if (existingStudent != null) {
            LOGGER.warning("Cannot add student: Student code already exists - " + student.getStudentCode());
            return false;
        }

        try {
            boolean success = studentDAO.addStudent(student);
            if (success) {
                LOGGER.info("Student added successfully: " + student.getStudentCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error adding student: " + e.getMessage());
            return false;
        }
    }

    public Student getStudentById(int studentId) {
        if (studentId <= 0) {
            return null;
        }

        try {
            return studentDAO.findById(studentId);
        } catch (Exception e) {
            LOGGER.severe("Error getting student by ID: " + e.getMessage());
            return null;
        }
    }

    public Student getStudentByCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return null;
        }

        try {
            return studentDAO.findByStudentCode(studentCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting student by code: " + e.getMessage());
            return null;
        }
    }

    /**
     */
    public Student getStudentByUserId(int userId) {
        if (userId <= 0) {
            return null;
        }

        try {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User user = userDAO.findById(userId);
            if (user == null) {
                return null;
            }
            return studentDAO.findByUsername(user.getUsername());
        } catch (Exception e) {
            LOGGER.severe("Error getting student by user ID: " + e.getMessage());
            return null;
        }
    }

    /**
     */
    public List<Student> getStudentsByClass(String classCode) {
        if (classCode == null || classCode.trim().isEmpty()) {
            return List.of();
        }

        try {
            return studentDAO.findByClassCode(classCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting students by class: " + e.getMessage());
            return List.of();
        }
    }

    /**
     */
    public List<Student> getStudentsByFaculty(String facultyCode) {
        if (facultyCode == null || facultyCode.trim().isEmpty()) {
            return List.of();
        }

        try {
            return studentDAO.findByFacultyCode(facultyCode);
        } catch (Exception e) {
            LOGGER.severe("Error getting students by faculty: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy tất cả sinh viên
     */
    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    /**
     * Tìm kiếm sinh viên theo từ khóa
     */
    public List<Student> searchStudents(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }

        try {
            return studentDAO.searchStudents(keyword.trim());
        } catch (Exception e) {
            LOGGER.severe("Error searching students: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Cập nhật thông tin sinh viên
     */
    public boolean updateStudent(Student student) {
        if (student == null || student.getStudentId() <= 0) {
            LOGGER.warning("Cannot update student: Invalid student data");
            return false;
        }

        try {
            boolean success = studentDAO.updateStudent(student);
            if (success) {
                LOGGER.info("Student updated successfully: " + student.getStudentCode());
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating student: " + e.getMessage());
            return false;
        }
    }

    /**
     */
    public boolean updateStudentStatus(String studentCode, Student.StudentStatus status) {
        if (studentCode == null || studentCode.trim().isEmpty() || status == null) {
            LOGGER.warning("Cannot update student status: Invalid input");
            return false;
        }

        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                LOGGER.warning("Cannot update student status: Student not found - " + studentCode);
                return false;
            }

            boolean success = studentDAO.updateStudentStatus(student.getStudentId(), status);
            if (success) {
                LOGGER.info("Student status updated successfully: " + studentCode + " -> " + status);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating student status: " + e.getMessage());
            return false;
        }
    }

    /**
     */
    public boolean updateGpaAndCredits(String studentCode, BigDecimal gpa, int totalCredits) {
        if (studentCode == null || studentCode.trim().isEmpty() || gpa == null ||
                gpa.compareTo(BigDecimal.ZERO) < 0 ||
                gpa.compareTo(new BigDecimal("4.0")) > 0 || totalCredits < 0) {
            LOGGER.warning("Cannot update GPA and credits: Invalid input");
            return false;
        }

        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                LOGGER.warning("Cannot update GPA and credits: Student not found - " + studentCode);
                return false;
            }

            boolean success = studentDAO.updateGpaAndCredits(student.getStudentId(), gpa, totalCredits);
            if (success) {
                LOGGER.info("Student GPA and credits updated successfully: " + studentCode);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating student GPA and credits: " + e.getMessage());
            return false;
        }
    }

    /**
     */
    public boolean transferStudent(String studentCode, String newClassCode) {
        if (studentCode == null || studentCode.trim().isEmpty() ||
                newClassCode == null || newClassCode.trim().isEmpty()) {
            LOGGER.warning("Cannot transfer student: Invalid input");
            return false;
        }

        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                LOGGER.warning("Cannot transfer student: Student not found - " + studentCode);
                return false;
            }

            student.setClassCode(newClassCode);
            boolean success = studentDAO.updateStudent(student);

            if (success) {
                LOGGER.info("Student transferred successfully: " + studentCode + " -> Class " + newClassCode);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error transferring student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra xem sinh viên có tồn tại không
     */
    public boolean studentExists(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return false;
        }

        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            return student != null;
        } catch (Exception e) {
            LOGGER.severe("Error checking student existence: " + e.getMessage());
            return false;
        }
    }

    /**
     */
    public boolean isStudentActive(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return false;
        }

        try {
            Student student = studentDAO.findByStudentCode(studentCode);
            return student != null && student.getStudentStatus() == Student.StudentStatus.ACTIVE;
        } catch (Exception e) {
            LOGGER.severe("Error checking student active status: " + e.getMessage());
            return false;
        }
    }

    /**
     */
    public StudentStatistics getStudentStatistics(String facultyCode) {
        try {
            List<Student> students = studentDAO.findByFacultyCode(facultyCode);

            StudentStatistics stats = new StudentStatistics();
            stats.setTotalStudents(students.size());

            long activeCount = students.stream()
                    .filter(s -> s.getStudentStatus() == Student.StudentStatus.ACTIVE)
                    .count();
            stats.setActiveStudents((int) activeCount);

            long graduatedCount = students.stream()
                    .filter(s -> s.getStudentStatus() == Student.StudentStatus.GRADUATED)
                    .count();
            stats.setGraduatedStudents((int) graduatedCount);

            long suspendedCount = students.stream()
                    .filter(s -> s.getStudentStatus() == Student.StudentStatus.SUSPENDED)
                    .count();
            stats.setSuspendedStudents((int) suspendedCount);

            long droppedCount = students.stream()
                    .filter(s -> s.getStudentStatus() == Student.StudentStatus.DROPPED)
                    .count();
            stats.setDroppedStudents((int) droppedCount);

            return stats;
        } catch (Exception e) {
            LOGGER.severe("Error getting student statistics: " + e.getMessage());
            return new StudentStatistics();
        }
    }

    /**
     */
    private boolean isValidStudentCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return false;
        }

        String codeRegex = "^SV\\d{4}\\d{3}$";
        return studentCode.matches(codeRegex);
    }

    public String generateStudentCode(int admissionYear, String facultyCode) {
        if (facultyCode == null || facultyCode.trim().isEmpty()) {
            LOGGER.warning("Cannot generate student code: Faculty code is required");
            return null;
        }

        int sequence = 1;
        String code;

        do {
            code = String.format("SV%d%03d", admissionYear, sequence);
            sequence++;
        } while (studentExists(code) && sequence <= 999);

        return sequence <= 999 ? code : null;
    }

    public static class StudentStatistics {
        private int totalStudents;
        private int activeStudents;
        private int graduatedStudents;
        private int suspendedStudents;
        private int droppedStudents;

        public int getTotalStudents() {
            return totalStudents;
        }

        public void setTotalStudents(int totalStudents) {
            this.totalStudents = totalStudents;
        }

        public int getActiveStudents() {
            return activeStudents;
        }

        public void setActiveStudents(int activeStudents) {
            this.activeStudents = activeStudents;
        }

        public int getGraduatedStudents() {
            return graduatedStudents;
        }

        public void setGraduatedStudents(int graduatedStudents) {
            this.graduatedStudents = graduatedStudents;
        }

        public int getSuspendedStudents() {
            return suspendedStudents;
        }

        public void setSuspendedStudents(int suspendedStudents) {
            this.suspendedStudents = suspendedStudents;
        }

        public int getDroppedStudents() {
            return droppedStudents;
        }

        public void setDroppedStudents(int droppedStudents) {
            this.droppedStudents = droppedStudents;
        }
    }

    public int getTotalCount() {
        try {
            return studentDAO.getTotalCount();
        } catch (Exception e) {
            LOGGER.severe("Error getting total student count: " + e.getMessage());
            return 0;
        }
    }
}
