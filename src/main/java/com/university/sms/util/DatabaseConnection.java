package com.university.sms.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class để quản lý kết nối database
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    
    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;
    private static String DB_DRIVER;
    
    static {
        loadDatabaseConfig();
    }
    
    /**
     * Tải cấu hình database từ file properties
     */
    private static void loadDatabaseConfig() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            
            if (input == null) {
                LOGGER.severe("Không tìm thấy file database.properties");
                throw new RuntimeException("database.properties file not found");
            }
            
            props.load(input);
            
            DB_URL = props.getProperty("db.url");
            DB_USERNAME = props.getProperty("db.username");
            DB_PASSWORD = props.getProperty("db.password");
            DB_DRIVER = props.getProperty("db.driver");
            
            // Load MySQL driver
            Class.forName(DB_DRIVER);
            
            LOGGER.info("Database configuration loaded successfully");
            
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error loading database configuration", e);
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }
    
    /**
     * Tạo kết nối mới đến database
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            // LOGGER.info("Database connection established successfully"); // Removed spam log
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            throw e;
        }
    }
    
    /**
     * Đóng kết nối database
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                // LOGGER.info("Database connection closed"); // Removed spam log
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
    
    /**
     * Test kết nối database
     * @return true nếu kết nối thành công
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        }
    }
    
    /**
     * Lấy thông tin cấu hình database
     */
    public static void printDatabaseInfo() {
        LOGGER.info("Database URL: " + DB_URL);
        LOGGER.info("Database Username: " + DB_USERNAME);
        LOGGER.info("Database Driver: " + DB_DRIVER);
    }
}
