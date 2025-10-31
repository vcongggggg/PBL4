package com.university.sms.client.gui;

import com.university.sms.client.IServerConnection;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Panel quản trị hệ thống (chỉ dành cho Admin)
 */
public class AdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    public AdminPanel(User currentUser, IServerConnection serverConnection) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;

        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Chức năng quản trị hệ thống sẽ được phát triển trong phiên bản sau", JLabel.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }

    public void refreshData() {
        // Implementation will be added later
    }
}
