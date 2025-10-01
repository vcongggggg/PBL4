package com.university.sms.client.gui;

import com.university.sms.client.ServerConnection;
import com.university.sms.common.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog đổi mật khẩu
 */
public class ChangePasswordDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private ServerConnection serverConnection;
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton changeButton;
    private JButton cancelButton;

    public ChangePasswordDialog(Frame parent, ServerConnection serverConnection) {
        super(parent, "Đổi mật khẩu", true);
        this.serverConnection = serverConnection;
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        currentPasswordField = new JPasswordField(20);
        newPasswordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        
        changeButton = new JButton("Đổi mật khẩu");
        cancelButton = new JButton("Hủy");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Current password
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Mật khẩu hiện tại:"), gbc);
        gbc.gridx = 1;
        formPanel.add(currentPasswordField, gbc);
        
        // New password
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Mật khẩu mới:"), gbc);
        gbc.gridx = 1;
        formPanel.add(newPasswordField, gbc);
        
        // Confirm password
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
        gbc.gridx = 1;
        formPanel.add(confirmPasswordField, gbc);
        
        add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(changeButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        changeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void changePassword() {
        String currentPassword = new String(currentPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Validate input
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có ít nhất 6 ký tự", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Send change password request
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.changePassword(newPassword);
            }
            
            @Override
            protected void done() {
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


