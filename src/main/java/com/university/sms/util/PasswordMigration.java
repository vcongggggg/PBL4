package com.university.sms.util;

import com.university.sms.dao.UserDAO;
import com.university.sms.model.User;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migration script để hash lại tất cả password hiện có trong database
 * Chạy script này một lần duy nhất sau khi implement password hashing
 * 
 * Usage:
 *   java -cp "target/classes;target/dependency/*" com.university.sms.util.PasswordMigration
 */
public class PasswordMigration {
    private static final Logger LOGGER = Logger.getLogger(PasswordMigration.class.getName());

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("Password Migration Script");
        System.out.println("Hashing all passwords in database...");
        System.out.println("==========================================\n");

        try {
            migrateAllPasswords();
            System.out.println("\n✅ Migration completed successfully!");
            System.out.println("All passwords have been hashed using BCrypt.");
            System.out.println("Users can now login with their existing passwords.");
        } catch (Exception e) {
            System.err.println("\n❌ Migration failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Hash lại tất cả password trong database
     */
    public static void migrateAllPasswords() {
        String selectSql = "SELECT user_id, username, password FROM users";
        int totalUsers = 0;
        int migratedCount = 0;
        int alreadyHashedCount = 0;
        int errorCount = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Đọc tất cả users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {

                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE user_id = ?"
                );

                while (rs.next()) {
                    totalUsers++;
                    int userId = rs.getInt("user_id");
                    String username = rs.getString("username");
                    String storedPassword = rs.getString("password");

                    if (storedPassword == null || storedPassword.trim().isEmpty()) {
                        System.out.println("⚠️  Skipping user (no password): " + username);
                        continue;
                    }

                    // 2. Kiểm tra xem đã hash chưa (tránh hash lại)
                    if (PasswordUtil.isHashed(storedPassword)) {
                        alreadyHashedCount++;
                        System.out.println("⏭️  Already hashed: " + username);
                        continue;
                    }

                    try {
                        // 3. Hash password
                        String hashedPassword = PasswordUtil.hashPassword(storedPassword);

                        // 4. Update vào database
                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setInt(2, userId);
                        updateStmt.executeUpdate();

                        migratedCount++;
                        System.out.println("✅ Hashed password for: " + username + " (ID: " + userId + ")");

                    } catch (Exception e) {
                        errorCount++;
                        System.err.println("❌ Error hashing password for " + username + ": " + e.getMessage());
                        LOGGER.log(Level.SEVERE, "Error hashing password for user: " + username, e);
                    }
                }

                // 5. Commit transaction
                if (errorCount == 0) {
                    conn.commit();
                    System.out.println("\n📊 Migration Summary:");
                    System.out.println("   Total users: " + totalUsers);
                    System.out.println("   Migrated: " + migratedCount);
                    System.out.println("   Already hashed: " + alreadyHashedCount);
                    System.out.println("   Errors: " + errorCount);
                } else {
                    conn.rollback();
                    System.err.println("\n⚠️  Transaction rolled back due to errors!");
                    throw new RuntimeException("Migration failed with " + errorCount + " errors");
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during migration", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during migration", e);
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test migration với một user cụ thể (dùng để test trước khi migrate tất cả)
     */
    public static void testMigration(String testUsername) {
        System.out.println("Testing migration for user: " + testUsername);
        
        UserDAO userDAO = new UserDAO();
        User user = userDAO.findByUsername(testUsername);
        
        if (user == null) {
            System.err.println("User not found: " + testUsername);
            return;
        }

        String oldPassword = user.getPassword();
        System.out.println("Current password (first 20 chars): " + 
            (oldPassword != null && oldPassword.length() > 20 ? oldPassword.substring(0, 20) + "..." : oldPassword));

        if (PasswordUtil.isHashed(oldPassword)) {
            System.out.println("✅ Password is already hashed");
            return;
        }

        // Test hash
        String hashedPassword = PasswordUtil.hashPassword(oldPassword);
        System.out.println("Hashed password (first 30 chars): " + hashedPassword.substring(0, 30) + "...");

        // Test verify
        boolean verified = PasswordUtil.verifyPassword(oldPassword, hashedPassword);
        System.out.println("Verification test: " + (verified ? "✅ PASSED" : "❌ FAILED"));

        if (verified) {
            System.out.println("✅ Test successful! Ready to migrate all passwords.");
        } else {
            System.err.println("❌ Test failed! Do not proceed with migration.");
        }
    }
}




