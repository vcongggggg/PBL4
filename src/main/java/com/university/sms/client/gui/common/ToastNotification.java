package com.university.sms.client.gui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Toast notification - hiển thị thông báo ngắn ở góc màn hình
 * Tự động ẩn sau vài giây
 */
public class ToastNotification extends JWindow {
    private static final long serialVersionUID = 1L;
    
    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 80;
    private static final int MARGIN = 20;
    
    public enum ToastType {
        SUCCESS("✓", new Color(46, 204, 113), Color.WHITE),
        ERROR("✗", new Color(231, 76, 60), Color.WHITE),
        WARNING("⚠", new Color(243, 156, 18), Color.WHITE),
        INFO("ℹ", new Color(52, 152, 219), Color.WHITE);
        
        private final String icon;
        private final Color bgColor;
        private final Color fgColor;
        
        ToastType(String icon, Color bgColor, Color fgColor) {
            this.icon = icon;
            this.bgColor = bgColor;
            this.fgColor = fgColor;
        }
        
        public String getIcon() { return icon; }
        public Color getBgColor() { return bgColor; }
        public Color getFgColor() { return fgColor; }
    }
    
    /**
     * Hiển thị toast notification
     */
    public static void show(Component parent, String message, ToastType type, int durationMs) {
        ToastNotification toast = new ToastNotification(parent, message, type);
        toast.display(durationMs);
    }
    
    /**
     * Shortcut methods
     */
    public static void showSuccess(Component parent, String message) {
        show(parent, message, ToastType.SUCCESS, 3000);
    }
    
    public static void showError(Component parent, String message) {
        show(parent, message, ToastType.ERROR, 4000);
    }
    
    public static void showWarning(Component parent, String message) {
        show(parent, message, ToastType.WARNING, 3500);
    }
    
    public static void showInfo(Component parent, String message) {
        show(parent, message, ToastType.INFO, 3000);
    }
    
    private ToastNotification(Component parent, String message, ToastType type) {
        super(parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        
        JPanel panel = createPanel(message, type);
        add(panel);
        pack();
        
        // Position at bottom-right
        positionToast(parent);
        
        // Add shadow effect (Java 10+)
        try {
            getRootPane().putClientProperty("Window.shadow", Boolean.TRUE);
        } catch (Exception e) {
            // Ignore if not supported
        }
    }
    
    private JPanel createPanel(String message, ToastType type) {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(type.getBgColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(type.getBgColor().darker(), 1),
            new EmptyBorder(15, 20, 15, 20)
        ));
        panel.setPreferredSize(new Dimension(TOAST_WIDTH, TOAST_HEIGHT));
        
        // Icon
        JLabel iconLabel = new JLabel(type.getIcon());
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        iconLabel.setForeground(type.getFgColor());
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(iconLabel, BorderLayout.WEST);
        
        // Message
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setForeground(type.getFgColor());
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(null);
        panel.add(messageArea, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
        closeButton.setForeground(type.getFgColor());
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(30, 30));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private void positionToast(Component parent) {
        if (parent != null) {
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window != null) {
                // Position relative to parent window
                int x = window.getX() + window.getWidth() - TOAST_WIDTH - MARGIN;
                int y = window.getY() + window.getHeight() - TOAST_HEIGHT - MARGIN;
                setLocation(x, y);
                return;
            }
        }
        
        // Position relative to screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - TOAST_WIDTH - MARGIN;
        int y = screenSize.height - TOAST_HEIGHT - MARGIN - 40; // Account for taskbar
        setLocation(x, y);
    }
    
    private void display(int durationMs) {
        // Fade in animation
        setOpacity(0.0f);
        setVisible(true);
        
        Timer fadeInTimer = new Timer(20, null);
        fadeInTimer.addActionListener(new ActionListener() {
            private float opacity = 0.0f;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.05f;
                if (opacity >= 0.95f) {
                    opacity = 0.95f;
                    fadeInTimer.stop();
                    
                    // Schedule fade out
                    Timer hideTimer = new Timer(durationMs, evt -> fadeOut());
                    hideTimer.setRepeats(false);
                    hideTimer.start();
                }
                setOpacity(opacity);
            }
        });
        fadeInTimer.start();
    }
    
    private void fadeOut() {
        Timer fadeOutTimer = new Timer(20, null);
        fadeOutTimer.addActionListener(new ActionListener() {
            private float opacity = 0.95f;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= 0.05f;
                if (opacity <= 0.0f) {
                    opacity = 0.0f;
                    fadeOutTimer.stop();
                    dispose();
                }
                setOpacity(opacity);
            }
        });
        fadeOutTimer.start();
    }
    
    /**
     * Demo/Testing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Toast Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            
            JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
            panel.setBorder(new EmptyBorder(50, 50, 50, 50));
            
            JButton successBtn = new JButton("Show Success Toast");
            successBtn.addActionListener(e -> ToastNotification.showSuccess(frame, "Thao tác thành công!"));
            
            JButton errorBtn = new JButton("Show Error Toast");
            errorBtn.addActionListener(e -> ToastNotification.showError(frame, "Đã xảy ra lỗi! Vui lòng thử lại."));
            
            JButton warningBtn = new JButton("Show Warning Toast");
            warningBtn.addActionListener(e -> ToastNotification.showWarning(frame, "Cảnh báo: Dữ liệu chưa được lưu!"));
            
            JButton infoBtn = new JButton("Show Info Toast");
            infoBtn.addActionListener(e -> ToastNotification.showInfo(frame, "Thông tin: Hệ thống sẽ bảo trì vào lúc 23:00"));
            
            panel.add(successBtn);
            panel.add(errorBtn);
            panel.add(warningBtn);
            panel.add(infoBtn);
            
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}

