package com.university.sms.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.university.sms.client.gui.LoginFrame;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Main hợp nhất cho cả Regular Client và CSV Client
 * Sử dụng tham số dòng lệnh: "regular" (mặc định) hoặc "csv"
 */
public class UnifiedClientMain {
  private static final Logger LOGGER = Logger.getLogger(UnifiedClientMain.class.getName());

  public static void main(String[] args) {
    String mode = (args != null && args.length > 0) ? args[0] : "regular";

    // Chọn factory theo mode
    LoginFrame.ConnectionFactory factory = switch (mode.toLowerCase()) {
      case "csv" -> new LoginFrame.CsvConnectionFactory();
      case "access" -> new LoginFrame.CsvConnectionFactory();
      case "pg" -> new LoginFrame.RegularConnectionFactory();
      default -> new LoginFrame.RegularConnectionFactory();
    };

    LOGGER.info("Starting Client in mode: " + mode);

    // Tối ưu UI
    System.setProperty("java.awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(new FlatLightLaf());
        UIManager.put("Button.arc", 5);
        UIManager.put("Component.arc", 5);
        UIManager.put("TextComponent.arc", 5);

        LoginFrame loginFrame = new LoginFrame(factory);
        loginFrame.setVisible(true);
      } catch (Exception e) {
        LOGGER.severe("Error starting unified client: " + e.getMessage());
        JOptionPane.showMessageDialog(null,
            "Lỗi khởi động ứng dụng: " + e.getMessage(),
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        System.exit(1);
      }
    });
  }
}
