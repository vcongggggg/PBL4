package com.university.sms.dao;

import com.university.sms.model.RegistrationPeriod;
import com.university.sms.model.RegistrationPeriod.PeriodStatus;
import com.university.sms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for managing registration periods
 */
public class RegistrationPeriodDAO {
    private static final Logger LOGGER = Logger.getLogger(RegistrationPeriodDAO.class.getName());

    /**
     * Create new registration period
     */
    public boolean insert(RegistrationPeriod period) {
        String sql = "INSERT INTO registration_periods " +
                     "(academic_year, semester, start_date, end_date, status, description, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, period.getAcademicYear());
            pstmt.setInt(2, period.getSemester());
            pstmt.setTimestamp(3, period.getStartDate());
            pstmt.setTimestamp(4, period.getEndDate());
            pstmt.setString(5, period.getStatus().name());
            pstmt.setString(6, period.getDescription());
            pstmt.setObject(7, period.getCreatedBy());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        period.setPeriodId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            LOGGER.severe("Error inserting registration period: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update registration period
     */
    public boolean update(RegistrationPeriod period) {
        String sql = "UPDATE registration_periods SET " +
                     "academic_year = ?, semester = ?, start_date = ?, end_date = ?, " +
                     "status = ?, description = ?, closed_by = ?, updated_at = NOW() " +
                     "WHERE period_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, period.getAcademicYear());
            pstmt.setInt(2, period.getSemester());
            pstmt.setTimestamp(3, period.getStartDate());
            pstmt.setTimestamp(4, period.getEndDate());
            pstmt.setString(5, period.getStatus().name());
            pstmt.setString(6, period.getDescription());
            pstmt.setObject(7, period.getClosedBy());
            pstmt.setInt(8, period.getPeriodId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.severe("Error updating registration period: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Find registration period by ID
     */
    public RegistrationPeriod findById(int periodId) {
        String sql = "SELECT * FROM registration_periods WHERE period_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, periodId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPeriod(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding registration period by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find all registration periods
     */
    public List<RegistrationPeriod> findAll() {
        List<RegistrationPeriod> periods = new ArrayList<>();
        String sql = "SELECT * FROM registration_periods ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                periods.add(mapResultSetToPeriod(rs));
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding all registration periods: " + e.getMessage());
            e.printStackTrace();
        }

        return periods;
    }

    /**
     * Find current active registration period
     */
    public RegistrationPeriod findCurrentPeriod() {
        String sql = "SELECT * FROM registration_periods " +
                     "WHERE status = 'OPEN' AND start_date <= NOW() AND end_date >= NOW() " +
                     "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return mapResultSetToPeriod(rs);
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding current registration period: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find registration period by academic year and semester
     */
    public RegistrationPeriod findByAcademicYearAndSemester(String academicYear, int semester) {
        String sql = "SELECT * FROM registration_periods " +
                     "WHERE academic_year = ? AND semester = ? " +
                     "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, academicYear);
            pstmt.setInt(2, semester);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPeriod(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.severe("Error finding registration period: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Delete registration period
     */
    public boolean delete(int periodId) {
        String sql = "DELETE FROM registration_periods WHERE period_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, periodId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.severe("Error deleting registration period: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Map ResultSet to RegistrationPeriod
     */
    private RegistrationPeriod mapResultSetToPeriod(ResultSet rs) throws SQLException {
        RegistrationPeriod period = new RegistrationPeriod();

        period.setPeriodId(rs.getInt("period_id"));
        period.setAcademicYear(rs.getString("academic_year"));
        period.setSemester(rs.getInt("semester"));
        period.setStartDate(rs.getTimestamp("start_date"));
        period.setEndDate(rs.getTimestamp("end_date"));
        period.setStatus(PeriodStatus.valueOf(rs.getString("status")));
        period.setDescription(rs.getString("description"));
        period.setCreatedAt(rs.getTimestamp("created_at"));
        period.setUpdatedAt(rs.getTimestamp("updated_at"));

        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) {
            period.setCreatedBy(createdBy);
        }

        int closedBy = rs.getInt("closed_by");
        if (!rs.wasNull()) {
            period.setClosedBy(closedBy);
        }

        return period;
    }
}

