package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Dialog đổi mật khẩu với nút toggle password visibility
 */
public class ChangePasswordDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton toggleCurrentPasswordButton;
    private JButton toggleNewPasswordButton;
    private JButton toggleConfirmPasswordButton;
    private JButton changeButton;
    private JButton cancelButton;
    
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    public ChangePasswordDialog(Frame parent, IServerConnection serverConnection) {
        super(parent, "Đổi mật khẩu", true);
        this.serverConnection = serverConnection;

        // Dùng header tùy chỉnh, bỏ border hệ điều hành
        setUndecorated(true);

        initializeComponents();
        setupLayout();
        setupEventListeners();

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initializeComponents() {
        // Password fields
        currentPasswordField = createStyledPasswordField(20);
        newPasswordField = createStyledPasswordField(20);
        confirmPasswordField = createStyledPasswordField(20);

        // Toggle buttons
        toggleCurrentPasswordButton = createToggleButton();
        toggleNewPasswordButton = createToggleButton();
        toggleConfirmPasswordButton = createToggleButton();

        // Action buttons
        changeButton = new JButton("Đổi mật khẩu");
        changeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        changeButton.setBackground(new Color(46, 204, 113)); // Green
        changeButton.setForeground(Color.WHITE);
        changeButton.setFocusPainted(false);
        changeButton.setBorderPainted(false);
        changeButton.setPreferredSize(new Dimension(120, 35));
        
        cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setPreferredSize(new Dimension(80, 35));
    }

    private JPasswordField createStyledPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setEchoChar('•');
        field.setPreferredSize(new Dimension(250, 32));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 35))); // Right padding for toggle button
        
        // Focus effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
                        BorderFactory.createEmptyBorder(5, 9, 5, 34)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                        BorderFactory.createEmptyBorder(6, 10, 6, 35)));
            }
        });

        return field;
    }

    private JButton createToggleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(30, 30));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Hiện/Ẩn mật khẩu");
        // Load icon mặc định (eye-close - ẩn password)
        loadIconForButton(button, "/icons/eye-close.png", 20, 20);
        return button;
    }

    /**
     * Load icon cho button từ resources
     */
    private void loadIconForButton(JButton button, String iconPath, int width, int height) {
        try {
            InputStream is = getClass().getResourceAsStream(iconPath);
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaled));
                button.setText("");
            } else {
                // Fallback nếu không tìm thấy icon
                button.setText("👁");
            }
        } catch (IOException e) {
            System.err.println("Cannot load icon: " + iconPath);
            button.setText("👁");
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        Color primaryColor = new Color(44, 62, 80); // match sidebar
        Color backgroundColor = new Color(245, 247, 250);
        Color cardBorderColor = new Color(220, 224, 230);

        // Header tùy chỉnh
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(primaryColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setPreferredSize(new Dimension(0, 60));

        JLabel headerTitle = new JLabel("Đổi mật khẩu");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerTitle.setForeground(Color.WHITE);
        headerPanel.add(headerTitle, BorderLayout.WEST);

        JButton closeButton = new JButton("X");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeButton.setForeground(new Color(255, 200, 200));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeButton.setForeground(Color.WHITE);
            }
        });
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        mainPanel.setBackground(backgroundColor);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Current password row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel currentLabel = new JLabel("Mật khẩu hiện tại:");
        currentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(currentLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel currentPasswordPanel = createPasswordFieldPanel(currentPasswordField, toggleCurrentPasswordButton);
        formPanel.add(currentPasswordPanel, gbc);

        // New password row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel newLabel = new JLabel("Mật khẩu mới:");
        newLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(newLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel newPasswordPanel = createPasswordFieldPanel(newPasswordField, toggleNewPasswordButton);
        formPanel.add(newPasswordPanel, gbc);

        // Confirm password row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel confirmLabel = new JLabel("Xác nhận mật khẩu:");
        confirmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel confirmPasswordPanel = createPasswordFieldPanel(confirmPasswordField, toggleConfirmPasswordButton);
        formPanel.add(confirmPasswordPanel, gbc);

        // Card trắng chứa form
        JPanel cardPanel = new JPanel(new BorderLayout(10, 10));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cardBorderColor, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        cardPanel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(cancelButton);
        buttonPanel.add(changeButton);
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // Container gộp header + thân
        JPanel container = new JPanel(new BorderLayout());
        container.add(headerPanel, BorderLayout.NORTH);
        container.add(mainPanel, BorderLayout.CENTER);

        setContentPane(container);

        // Viền ngoài cùng
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));
        }
    }

    private JPanel createPasswordFieldPanel(JPasswordField passwordField, JButton toggleButton) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(Color.WHITE);
        panel.setOpaque(false);
        
        panel.add(passwordField, BorderLayout.CENTER);
        
        // Toggle button positioned on the right
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(toggleButton);
        panel.add(buttonWrapper, BorderLayout.EAST);
        
        return panel;
    }

    private void setupEventListeners() {
        // Toggle current password visibility
        toggleCurrentPasswordButton.addActionListener(e -> {
            isCurrentPasswordVisible = !isCurrentPasswordVisible;
            togglePasswordVisibility(currentPasswordField, toggleCurrentPasswordButton, isCurrentPasswordVisible);
        });

        // Toggle new password visibility
        toggleNewPasswordButton.addActionListener(e -> {
            isNewPasswordVisible = !isNewPasswordVisible;
            togglePasswordVisibility(newPasswordField, toggleNewPasswordButton, isNewPasswordVisible);
        });

        // Toggle confirm password visibility
        toggleConfirmPasswordButton.addActionListener(e -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(confirmPasswordField, toggleConfirmPasswordButton, isConfirmPasswordVisible);
        });

        // Change password button
        changeButton.addActionListener(e -> changePassword());

        // Cancel button
        cancelButton.addActionListener(e -> dispose());

        // Enter key to submit
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke("ENTER");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "submit");
        getRootPane().getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        });

        // Escape key to cancel
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void togglePasswordVisibility(JPasswordField passwordField, JButton toggleButton, boolean isVisible) {
        if (isVisible) {
            passwordField.setEchoChar((char) 0); // Show password
            loadIconForButton(toggleButton, "/icons/eye-open.png", 20, 20);
            toggleButton.setToolTipText("Ẩn mật khẩu");
        } else {
            passwordField.setEchoChar('•'); // Hide password
            loadIconForButton(toggleButton, "/icons/eye-close.png", 20, 20);
            toggleButton.setToolTipText("Hiện mật khẩu");
        }
    }

    private void changePassword() {
        String currentPassword = new String(currentPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Validate input
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "Vui lòng điền đầy đủ thông tin", 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                    "Mật khẩu xác nhận không khớp", 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
            confirmPasswordField.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this, 
                    "Mật khẩu mới phải có ít nhất 6 ký tự", 
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            newPasswordField.requestFocus();
            return;
        }

        // Disable buttons during request
        changeButton.setEnabled(false);
        cancelButton.setEnabled(false);

        // Send change password request
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.changePassword(newPassword);
            }

            @Override
            protected void done() {
                // Re-enable buttons
                changeButton.setEnabled(true);
                cancelButton.setEnabled(true);

                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                                "Đổi mật khẩu thành công",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                                "Đổi mật khẩu thất bại: " + response.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ChangePasswordDialog.this,
                            "Lỗi khi đổi mật khẩu: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
