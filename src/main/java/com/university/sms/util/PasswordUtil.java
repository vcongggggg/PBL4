package com.university.sms.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility class để hash và verify password sử dụng BCrypt
 */
public class PasswordUtil {
    private static final Logger LOGGER = Logger.getLogger(PasswordUtil.class.getName());
    
    // BCrypt cost factor (số lần hash, càng cao càng an toàn nhưng chậm hơn)
    // 12 là giá trị cân bằng tốt giữa security và performance
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash password bằng BCrypt
     * 
     * @param plainPassword Password dạng plain text
     * @return BCrypt hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
            LOGGER.fine("Password hashed successfully");
            return hashedPassword;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error hashing password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * Verify password với BCrypt hash
     * 
     * @param plainPassword Password dạng plain text từ user input
     * @param hashedPassword BCrypt hash từ database
     * @return true nếu password match, false nếu không match
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        // Chỉ verify với BCrypt hash
        // Nếu stored password không phải BCrypt hash, return false
        if (!hashedPassword.startsWith("$2a$") && !hashedPassword.startsWith("$2b$") && !hashedPassword.startsWith("$2y$")) {
            LOGGER.warning("Stored password is not a BCrypt hash. Please run migration script to hash all passwords.");
            return false;
        }
        
        try {
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            if (!matches) {
                LOGGER.fine("Password verification failed");
            }
            return matches;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verifying password with BCrypt", e);
            return false;
        }
    }

    /**
     * Kiểm tra xem một string có phải là BCrypt hash không
     * 
     * @param password String cần kiểm tra
     * @return true nếu là BCrypt hash, false nếu không
     */
    public static boolean isHashed(String password) {
        if (password == null || password.length() < 10) {
            return false;
        }
        // BCrypt hash bắt đầu với $2a$, $2b$, hoặc $2y$
        return password.startsWith("$2a$") || 
               password.startsWith("$2b$") || 
               password.startsWith("$2y$");
    }
}

