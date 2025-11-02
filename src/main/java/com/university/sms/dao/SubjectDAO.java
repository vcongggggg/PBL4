package com.university.sms.dao;

import com.university.sms.model.Subject;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO class for Subject model
 */
public class SubjectDAO {
    private static final Logger LOGGER = Logger.getLogger(SubjectDAO.class.getName());

    /**
     * Get all subjects
     */
    public List<Subject> findAll() {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.*, f.faculty_name, " +
                    "ps.subject_name as prerequisite_name " +
                    "FROM subjects s " +
                    "LEFT JOIN faculties f ON s.faculty_id = f.faculty_id " +
                    "LEFT JOIN subjects ps ON s.prerequisite_subject_id = ps.subject_id " +
                    "ORDER BY s.subject_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }

            LOGGER.info("Found " + subjects.size() + " subjects");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all subjects", e);
        }

        return subjects;
    }

    /**
     * Get subject by ID
     */
    public Subject findById(int subjectId) {
        String sql = "SELECT s.*, f.faculty_name, " +
                    "ps.subject_name as prerequisite_name " +
                    "FROM subjects s " +
                    "LEFT JOIN faculties f ON s.faculty_id = f.faculty_id " +
                    "LEFT JOIN subjects ps ON s.prerequisite_subject_id = ps.subject_id " +
                    "WHERE s.subject_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, subjectId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToSubject(rs);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding subject by ID: " + subjectId, e);
        }

        return null;
    }

    /**
     * Get subject by code
     */
    public Subject findByCode(String subjectCode) {
        String sql = "SELECT s.*, f.faculty_name, " +
                    "ps.subject_name as prerequisite_name " +
                    "FROM subjects s " +
                    "LEFT JOIN faculties f ON s.faculty_id = f.faculty_id " +
                    "LEFT JOIN subjects ps ON s.prerequisite_subject_id = ps.subject_id " +
                    "WHERE s.subject_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, subjectCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToSubject(rs);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding subject by code: " + subjectCode, e);
        }

        return null;
    }

    /**
     * Get subjects by faculty
     */
    public List<Subject> findByFaculty(int facultyId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.*, f.faculty_name, " +
                    "ps.subject_name as prerequisite_name " +
                    "FROM subjects s " +
                    "LEFT JOIN faculties f ON s.faculty_id = f.faculty_id " +
                    "LEFT JOIN subjects ps ON s.prerequisite_subject_id = ps.subject_id " +
                    "WHERE s.faculty_id = ? " +
                    "ORDER BY s.subject_code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, facultyId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding subjects by faculty: " + facultyId, e);
        }

        return subjects;
    }

    /**
     * Search subjects by keyword
     */
    public List<Subject> search(String keyword) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.*, f.faculty_name, " +
                    "ps.subject_name as prerequisite_name " +
                    "FROM subjects s " +
                    "LEFT JOIN faculties f ON s.faculty_id = f.faculty_id " +
                    "LEFT JOIN subjects ps ON s.prerequisite_subject_id = ps.subject_id " +
                    "WHERE s.subject_code LIKE ? OR s.subject_name LIKE ? " +
                    "ORDER BY s.subject_code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching subjects with keyword: " + keyword, e);
        }

        return subjects;
    }

    /**
     * Insert new subject
     */
    public boolean insert(Subject subject) {
        String sql = "INSERT INTO subjects (subject_code, subject_name, credits, faculty_id, " +
                    "prerequisite_subject_id, description, is_required) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, subject.getSubjectCode());
            pstmt.setString(2, subject.getSubjectName());
            pstmt.setInt(3, subject.getCredits());
            pstmt.setInt(4, subject.getFacultyId());
            
            if (subject.getPrerequisiteSubjectId() != null) {
                pstmt.setInt(5, subject.getPrerequisiteSubjectId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, subject.getDescription());
            pstmt.setBoolean(7, subject.isRequired());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    subject.setSubjectId(rs.getInt(1));
                }
                LOGGER.info("Inserted subject: " + subject.getSubjectCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting subject", e);
        }

        return false;
    }

    /**
     * Update subject
     */
    public boolean update(Subject subject) {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, credits = ?, " +
                    "faculty_id = ?, prerequisite_subject_id = ?, description = ?, " +
                    "is_required = ? WHERE subject_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, subject.getSubjectCode());
            pstmt.setString(2, subject.getSubjectName());
            pstmt.setInt(3, subject.getCredits());
            pstmt.setInt(4, subject.getFacultyId());
            
            if (subject.getPrerequisiteSubjectId() != null) {
                pstmt.setInt(5, subject.getPrerequisiteSubjectId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, subject.getDescription());
            pstmt.setBoolean(7, subject.isRequired());
            pstmt.setInt(8, subject.getSubjectId());

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                LOGGER.info("Updated subject: " + subject.getSubjectCode());
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating subject", e);
        }

        return false;
    }

    /**
     * Delete subject
     */
    public boolean delete(int subjectId) {
        String sql = "DELETE FROM subjects WHERE subject_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, subjectId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                LOGGER.info("Deleted subject ID: " + subjectId);
                return true;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting subject: " + subjectId, e);
        }

        return false;
    }

    /**
     * Map ResultSet to Subject object
     */
    private Subject mapResultSetToSubject(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        
        subject.setSubjectId(rs.getInt("subject_id"));
        subject.setSubjectCode(rs.getString("subject_code"));
        subject.setSubjectName(rs.getString("subject_name"));
        subject.setCredits(rs.getInt("credits"));
        subject.setFacultyId(rs.getInt("faculty_id"));
        
        int prerequisiteId = rs.getInt("prerequisite_subject_id");
        if (!rs.wasNull()) {
            subject.setPrerequisiteSubjectId(prerequisiteId);
        }
        
        subject.setDescription(rs.getString("description"));
        subject.setRequired(rs.getBoolean("is_required"));
        subject.setCreatedAt(rs.getTimestamp("created_at"));
        
        // Set joined data
        subject.setFacultyName(rs.getString("faculty_name"));
        subject.setPrerequisiteSubjectName(rs.getString("prerequisite_name"));
        
        return subject;
    }
}

