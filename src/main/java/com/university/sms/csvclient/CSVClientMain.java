package com.university.sms.csvclient;

import com.formdev.flatlaf.FlatLightLaf;
import com.university.sms.csvclient.gui.CSVLoginFrame;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Main class để khởi động CSV client
 * Sử dụng CSVLoginFrame với CSVServerConnection
 */
public class CSVClientMain {
  private static final Logger LOGGER = Logger.getLogger(CSVClientMain.class.getName());

  public static void main(String[] args) {
    LOGGER.info("Starting CSV Student Management System Client...");

    // Set system properties for better UI
    System.setProperty("java.awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    // Set look and feel
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(new FlatLightLaf());

        // Update UI defaults
        UIManager.put("Button.arc", 5);
        UIManager.put("Component.arc", 5);
        UIManager.put("TextComponent.arc", 5);

      } catch (Exception e) {
        LOGGER.warning("Could not set FlatLaf look and feel: " + e.getMessage());
        try {
          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ex) {
          LOGGER.severe("Could not set system look and feel: " + ex.getMessage());
        }
      }

      // Create and show CSV login frame
      try {
        CSVLoginFrame loginFrame = new CSVLoginFrame();
        loginFrame.setVisible(true);

        LOGGER.info("CSV Client application started successfully");

      } catch (Exception e) {
        LOGGER.severe("Error starting CSV client application: " + e.getMessage());
        e.printStackTrace();

        JOptionPane.showMessageDialog(null,
            "Lỗi khởi động ứng dụng CSV: " + e.getMessage(),
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);

        System.exit(1);
      }
    });
  }
}