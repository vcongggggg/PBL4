package com.university.sms.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.university.sms.client.gui.LoginFrame;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Main class để khởi động ứng dụng client
 */
public class ClientMain {
    private static final Logger LOGGER = Logger.getLogger(ClientMain.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting Student Management System Client...");
        
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
            
            // Create and show login frame
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                
                LOGGER.info("Client application started successfully");
                
            } catch (Exception e) {
                LOGGER.severe("Error starting client application: " + e.getMessage());
                e.printStackTrace();
                
                JOptionPane.showMessageDialog(null, 
                    "Lỗi khởi động ứng dụng: " + e.getMessage(), 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
                
                System.exit(1);
            }
        });
    }
}
