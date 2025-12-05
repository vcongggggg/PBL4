package com.university.sms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.university.sms.util.DatabaseConnection;

/**
 * Đơn giản hóa việc đọc/ghi bảng system_config.
 */
public class SystemConfigDAO {
    private static final Logger LOGGER = Logger.getLogger(SystemConfigDAO.class.getName());

    public String getConfigValue(String key) {
        String sql = "SELECT config_value FROM system_config WHERE config_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đọc system_config với key=" + key, e);
        }
        return null;
    }

    /**
     * Cập nhật hoặc chèn mới một cấu hình.
     */
    public boolean upsertConfigValue(String key, String value, String descriptionIfInsert) {
        String updateSql = "UPDATE system_config SET config_value = ?, description = ? WHERE config_key = ?";
        String insertSql = "INSERT INTO system_config (config_key, config_value, description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Thử update trước
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, value);
                stmt.setString(2, descriptionIfInsert);
                stmt.setString(3, key);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    return true;
                }
            }

            // Nếu chưa tồn tại thì insert
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, key);
                stmt.setString(2, value);
                stmt.setString(3, descriptionIfInsert);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi ghi system_config với key=" + key, e);
            return false;
        }
    }
}


