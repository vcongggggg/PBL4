package com.university.sms.service;

import com.university.sms.dao.StudentDAO;
import com.university.sms.model.Student;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service xử lý các thao tác liên quan đến sinh viên
 */
public class StudentService {
    private static final Logger LOGGER = Logger.getLogger(StudentService.class.getName());
    
    private StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
    }

    /**
     * Thêm sinh viên mới
     */
    public boolean addStudent(Student student) {
        if (student == null) {
            LOGGER.warning("Cannot add student: Student object is null");
            return false;
        }

        // Validate required fields
        if (student.getUserId() <= 0 ||
            student.getStudentCode() == null || student.getStudentCode().trim().isEmpty() ||
            student.getDepartmentId() <= 0 ||
            student.getAdmissionYear() <= 0) {
            
            LOGGER.warning("Cannot add student: Missing required fields");
            return false;
        }

        // Check if student code already exists
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

    /**
     * Lấy thông tin sinh viên theo ID
     */
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

    /**
     * Lấy thông tin sinh viên theo mã sinh viên
     */
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
     * Lấy thông tin sinh viên theo user ID
     */
    public Student getStudentByUserId(int userId) {
        if (userId <= 0) {
            return null;
        }

        try {
            return studentDAO.findByUserId(userId);
        } catch (Exception e) {
            LOGGER.severe("Error getting student by user ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy danh sách sinh viên theo lớp
     */
    public List<Student> getStudentsByClass(int classId) {
        if (classId <= 0) {
            return List.of();
        }

        try {
            return studentDAO.findByClassId(classId);
        } catch (Exception e) {
            LOGGER.severe("Error getting students by class: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy danh sách sinh viên theo khoa
     */
    public List<Student> getStudentsByDepartment(int departmentId) {
        if (departmentId <= 0) {
            return List.of();
        }

        try {
            return studentDAO.findByDepartmentId(departmentId);
        } catch (Exception e) {
            LOGGER.severe("Error getting students by department: " + e.getMessage());
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
            // Nếu keyword rỗng, trả về tất cả sinh viên
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
     * Cập nhật trạng thái sinh viên
     */
    public boolean updateStudentStatus(int studentId, Student.StudentStatus status) {
        if (studentId <= 0 || status == null) {
            LOGGER.warning("Cannot update student status: Invalid input");
            return false;
        }

        try {
            boolean success = studentDAO.updateStudentStatus(studentId, status);
            if (success) {
                LOGGER.info("Student status updated successfully: " + studentId + " -> " + status);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating student status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật GPA và tổng tín chỉ
     */
    public boolean updateGpaAndCredits(int studentId, BigDecimal gpa, int totalCredits) {
        if (studentId <= 0 || gpa == null || gpa.compareTo(BigDecimal.ZERO) < 0 || 
            gpa.compareTo(new BigDecimal("4.0")) > 0 || totalCredits < 0) {
            LOGGER.warning("Cannot update GPA and credits: Invalid input");
            return false;
        }

        try {
            boolean success = studentDAO.updateGpaAndCredits(studentId, gpa, totalCredits);
            if (success) {
                LOGGER.info("Student GPA and credits updated successfully: " + studentId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Error updating student GPA and credits: " + e.getMessage());
            return false;
        }
    }

    /**
     * Chuyển lớp cho sinh viên
     */
    public boolean transferStudent(int studentId, int newClassId) {
        if (studentId <= 0 || newClassId <= 0) {
            LOGGER.warning("Cannot transfer student: Invalid input");
            return false;
        }

        try {
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                LOGGER.warning("Cannot transfer student: Student not found - " + studentId);
                return false;
            }

            student.setClassId(newClassId);
            boolean success = studentDAO.updateStudent(student);
            
            if (success) {
                LOGGER.info("Student transferred successfully: " + studentId + " -> Class " + newClassId);
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
     * Kiểm tra xem sinh viên có hoạt động không
     */
    public boolean isStudentActive(int studentId) {
        try {
            Student student = studentDAO.findById(studentId);
            return student != null && student.getStudentStatus() == Student.StudentStatus.ACTIVE;
        } catch (Exception e) {
            LOGGER.severe("Error checking student active status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thống kê sinh viên theo khoa
     */
    public StudentStatistics getStudentStatistics(int departmentId) {
        try {
            List<Student> students = studentDAO.findByDepartmentId(departmentId);
            
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
     * Validate student data
     */
    private boolean isValidStudentCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return false;
        }
        
        // Student code format: SV + year + sequential number (e.g., SV2024001)
        String codeRegex = "^SV\\d{4}\\d{3}$";
        return studentCode.matches(codeRegex);
    }

    /**
     * Generate student code
     */
    public String generateStudentCode(int admissionYear, int departmentId) {
        // This is a simple implementation - in a real system, you'd want to ensure uniqueness
        int sequence = 1;
        String code;
        
        do {
            code = String.format("SV%d%03d", admissionYear, sequence);
            sequence++;
        } while (studentExists(code) && sequence <= 999);
        
        return sequence <= 999 ? code : null;
    }

    /**
     * Inner class for student statistics
     */
    public static class StudentStatistics {
        private int totalStudents;
        private int activeStudents;
        private int graduatedStudents;
        private int suspendedStudents;
        private int droppedStudents;

        // Getters and setters
        public int getTotalStudents() { return totalStudents; }
        public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
        
        public int getActiveStudents() { return activeStudents; }
        public void setActiveStudents(int activeStudents) { this.activeStudents = activeStudents; }
        
        public int getGraduatedStudents() { return graduatedStudents; }
        public void setGraduatedStudents(int graduatedStudents) { this.graduatedStudents = graduatedStudents; }
        
        public int getSuspendedStudents() { return suspendedStudents; }
        public void setSuspendedStudents(int suspendedStudents) { this.suspendedStudents = suspendedStudents; }
        
        public int getDroppedStudents() { return droppedStudents; }
        public void setDroppedStudents(int droppedStudents) { this.droppedStudents = droppedStudents; }
    }
}
