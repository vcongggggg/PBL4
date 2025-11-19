package com.university.sms.dao;

import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.ClassOpeningRequest.RequestStatus;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for managing class opening requests
 * ✅ REFACTORED: Dùng teacher_username, subject_code, approved_by_username thay
 * vì IDs
 */
public class ClassOpeningRequestDAO {
    private static final Logger LOGGER = Logger.getLogger(ClassOpeningRequestDAO.class.getName());

    /**
     * ✅ REFACTORED: Find all class opening requests
     */
    public List<ClassOpeningRequest> findAll() {
        List<ClassOpeningRequest> requests = new ArrayList<>();
        String sql = "SELECT cor.*, " +
                "u.full_name as teacher_name, " +
                "s.subject_name, s.subject_code, s.credits, " +
                "u2.full_name as approver_name " +
                "FROM class_opening_requests cor " +
                "JOIN users u ON cor.teacher_username = u.username " +
                "JOIN subjects s ON cor.subject_code = s.subject_code " +
                "LEFT JOIN users u2 ON cor.approved_by_username = u2.username " +
                "ORDER BY cor.request_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                requests.add(extractRequestFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi tìm tất cả yêu cầu: " + e.getMessage());
        }

        return requests;
    }

    /**
     * Find request by ID
     */
    public ClassOpeningRequest findById(int requestId) {
        String sql = "SELECT cor.*, " +
                "u.full_name as teacher_name, " +
                "s.subject_name, s.subject_code, s.credits, " +
                "u2.full_name as approver_name " +
                "FROM class_opening_requests cor " +
                "JOIN users u ON cor.teacher_username = u.username " +
                "JOIN subjects s ON cor.subject_code = s.subject_code " +
                "LEFT JOIN users u2 ON cor.approved_by_username = u2.username " +
                "WHERE cor.request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractRequestFromResultSet(rs);
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi tìm yêu cầu theo ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * ✅ REFACTORED: Find requests by teacher (dùng username)
     */
    public List<ClassOpeningRequest> findByTeacher(String teacherUsername) {
        List<ClassOpeningRequest> requests = new ArrayList<>();
        String sql = "SELECT cor.*, " +
                "u.full_name as teacher_name, " +
                "s.subject_name, s.subject_code, s.credits, " +
                "u2.full_name as approver_name " +
                "FROM class_opening_requests cor " +
                "JOIN users u ON cor.teacher_username = u.username " +
                "JOIN subjects s ON cor.subject_code = s.subject_code " +
                "LEFT JOIN users u2 ON cor.approved_by_username = u2.username " +
                "WHERE cor.teacher_username = ? " +
                "ORDER BY cor.request_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherUsername);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(extractRequestFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi tìm yêu cầu theo giáo viên: " + e.getMessage());
        }

        return requests;
    }

    /**
     * Find request by approved course code
     */
    public ClassOpeningRequest findByApprovedCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            LOGGER.warning("findByApprovedCourseCode: courseCode là null hoặc rỗng");
            return null;
        }

        String sql = "SELECT cor.*, " +
                "u.full_name as teacher_name, " +
                "s.subject_name, s.subject_code, s.credits, " +
                "u2.full_name as approver_name " +
                "FROM class_opening_requests cor " +
                "JOIN users u ON cor.teacher_username = u.username " +
                "JOIN subjects s ON cor.subject_code = s.subject_code " +
                "LEFT JOIN users u2 ON cor.approved_by_username = u2.username " +
                "WHERE cor.approved_course_code = ? " +
                "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ClassOpeningRequest request = extractRequestFromResultSet(rs);
                LOGGER.info("Tìm thấy request: requestId=" + request.getRequestId() +
                        ", approvedCourseCode=" + request.getApprovedCourseCode() +
                        ", status=" + request.getRequestStatus());
                return request;
            } else {
                LOGGER.info("Không tìm thấy request với approved_course_code = '" + courseCode + "'");
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi tìm request theo approved_course_code: " + e.getMessage());
        }

        return null;
    }

    /**
     * Find requests by status
     */
    public List<ClassOpeningRequest> findByStatus(RequestStatus status) {
        List<ClassOpeningRequest> requests = new ArrayList<>();
        String sql = "SELECT cor.*, " +
                "u.full_name as teacher_name, " +
                "s.subject_name, s.subject_code, s.credits, " +
                "u2.full_name as approver_name " +
                "FROM class_opening_requests cor " +
                "JOIN users u ON cor.teacher_username = u.username " +
                "JOIN subjects s ON cor.subject_code = s.subject_code " +
                "LEFT JOIN users u2 ON cor.approved_by_username = u2.username " +
                "WHERE cor.request_status = ? " +
                "ORDER BY cor.request_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(extractRequestFromResultSet(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi tìm yêu cầu theo trạng thái: " + e.getMessage());
        }

        return requests;
    }

    /**
     * ✅ REFACTORED: Insert new request
     */
    public boolean insert(ClassOpeningRequest request) {
        String sql = "INSERT INTO class_opening_requests " +
                "(teacher_username, subject_code, academic_year, semester, schedule_day, " +
                "schedule_time, room, max_students, reason, request_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, request.getTeacherUsername());
            pstmt.setString(2, request.getSubjectCode());
            pstmt.setString(3, request.getAcademicYear());
            pstmt.setInt(4, request.getSemester());
            pstmt.setString(5, request.getScheduleDay());
            pstmt.setString(6, request.getScheduleTime());
            pstmt.setString(7, request.getRoom());
            pstmt.setInt(8, request.getMaxStudents());
            pstmt.setString(9, request.getReason());
            pstmt.setString(10, request.getRequestStatus().name());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    request.setRequestId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi thêm yêu cầu: " + e.getMessage());
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Update request
     */
    public boolean update(ClassOpeningRequest request) {
        String sql = "UPDATE class_opening_requests SET " +
                "subject_code = ?, academic_year = ?, semester = ?, " +
                "schedule_day = ?, schedule_time = ?, room = ?, " +
                "max_students = ?, reason = ?, request_status = ?, " +
                "admin_note = ?, approved_by_username = ?, approved_course_code = ?, " +
                "decision_date = ? " +
                "WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, request.getSubjectCode());
            pstmt.setString(2, request.getAcademicYear());
            pstmt.setInt(3, request.getSemester());
            pstmt.setString(4, request.getScheduleDay());
            pstmt.setString(5, request.getScheduleTime());
            pstmt.setString(6, request.getRoom());
            pstmt.setInt(7, request.getMaxStudents());
            pstmt.setString(8, request.getReason());
            pstmt.setString(9, request.getRequestStatus().name());
            pstmt.setString(10, request.getAdminNote());

            if (request.getApprovedByUsername() != null && !request.getApprovedByUsername().isEmpty()) {
                pstmt.setString(11, request.getApprovedByUsername());
            } else {
                pstmt.setNull(11, Types.VARCHAR);
            }

            if (request.getApprovedCourseCode() != null && !request.getApprovedCourseCode().isEmpty()) {
                pstmt.setString(12, request.getApprovedCourseCode());
            } else {
                pstmt.setNull(12, Types.VARCHAR);
            }

            pstmt.setTimestamp(13, request.getDecisionDate());
            pstmt.setInt(14, request.getRequestId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi cập nhật yêu cầu: " + e.getMessage());
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Approve request
     */
    public boolean approve(int requestId, String approverUsername, String note, String approvedCourseCode) {
        String sql = "UPDATE class_opening_requests SET " +
                "request_status = 'APPROVED', " +
                "admin_note = ?, " +
                "approved_by_username = ?, " +
                "approved_course_code = ?, " +
                "decision_date = NOW() " +
                "WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note);
            pstmt.setString(2, approverUsername);
            pstmt.setString(3, approvedCourseCode);
            pstmt.setInt(4, requestId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi duyệt yêu cầu: " + e.getMessage());
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Reject request
     * Nếu request đã được APPROVED (có approved_course_code), vẫn giữ lại
     * approved_course_code
     * để tracking course đã được tạo, nhưng chuyển status thành REJECTED
     * 
     * @param approverUsername Nếu là "SYSTEM" hoặc null, sẽ không update
     *                         approved_by_username
     *                         (giữ nguyên giá trị hiện tại để tránh foreign key
     *                         constraint)
     */
    public boolean reject(int requestId, String approverUsername, String reason) {
        // Nếu approverUsername là "SYSTEM" hoặc null, không update approved_by_username
        // (giữ nguyên giá trị hiện tại để tránh foreign key constraint)
        boolean updateApprover = approverUsername != null &&
                !approverUsername.trim().equalsIgnoreCase("SYSTEM") &&
                !approverUsername.trim().isEmpty();

        String sql;
        if (updateApprover) {
            sql = "UPDATE class_opening_requests SET " +
                    "request_status = 'REJECTED', " +
                    "admin_note = ?, " +
                    "approved_by_username = ?, " +
                    "decision_date = NOW() " +
                    "WHERE request_id = ?";
        } else {
            sql = "UPDATE class_opening_requests SET " +
                    "request_status = 'REJECTED', " +
                    "admin_note = ?, " +
                    "decision_date = NOW() " +
                    "WHERE request_id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reason);
            if (updateApprover) {
                pstmt.setString(2, approverUsername);
                pstmt.setInt(3, requestId);
            } else {
                pstmt.setInt(2, requestId);
            }

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                LOGGER.info("Đã reject yêu cầu mở lớp: requestId=" + requestId +
                        (updateApprover ? ", approver=" + approverUsername : ", tự động reject (SYSTEM)"));
                return true;
            } else {
                LOGGER.warning("Không tìm thấy yêu cầu để reject: requestId=" + requestId);
                return false;
            }

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi reject yêu cầu: " + e.getMessage());
        }

        return false;
    }

    /**
     * Delete request
     */
    public boolean delete(int requestId) {
        String sql = "DELETE FROM class_opening_requests WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.severe("Lỗi khi xóa yêu cầu: " + e.getMessage());
        }

        return false;
    }

    /**
     * ✅ REFACTORED: Extract ClassOpeningRequest from ResultSet
     */
    private ClassOpeningRequest extractRequestFromResultSet(ResultSet rs) throws SQLException {
        ClassOpeningRequest request = new ClassOpeningRequest();

        request.setRequestId(rs.getInt("request_id"));
        request.setTeacherUsername(rs.getString("teacher_username"));
        request.setSubjectCode(rs.getString("subject_code"));
        request.setAcademicYear(rs.getString("academic_year"));
        request.setSemester(rs.getInt("semester"));
        request.setScheduleDay(rs.getString("schedule_day"));
        request.setScheduleTime(rs.getString("schedule_time"));
        request.setRoom(rs.getString("room"));
        request.setMaxStudents(rs.getInt("max_students"));
        request.setReason(rs.getString("reason"));
        request.setRequestStatus(RequestStatus.valueOf(rs.getString("request_status")));
        request.setAdminNote(rs.getString("admin_note"));

        String approvedByUsername = rs.getString("approved_by_username");
        if (!rs.wasNull()) {
            request.setApprovedByUsername(approvedByUsername);
        }

        String approvedCourseCode = rs.getString("approved_course_code");
        if (!rs.wasNull()) {
            request.setApprovedCourseCode(approvedCourseCode);
        }

        request.setRequestDate(rs.getTimestamp("request_date"));
        request.setDecisionDate(rs.getTimestamp("decision_date"));
        request.setCreatedAt(rs.getTimestamp("created_at"));

        // Related information
        request.setTeacherName(rs.getString("teacher_name"));
        request.setSubjectName(rs.getString("subject_name"));
        request.setSubjectCode(rs.getString("subject_code"));
        request.setCredits(rs.getInt("credits"));
        request.setApproverName(rs.getString("approver_name"));

        return request;
    }
}
