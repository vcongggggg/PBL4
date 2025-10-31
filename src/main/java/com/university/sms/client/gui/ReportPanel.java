package com.university.sms.client.gui;

import com.university.sms.client.IServerConnection;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Panel báo cáo và thống kê
 */
public class ReportPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    public ReportPanel(User currentUser, IServerConnection serverConnection) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;

        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Chức năng báo cáo và thống kê sẽ được phát triển trong phiên bản sau",
                JLabel.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }

    public void refreshData() {
        // Implementation will be added later
    }
}
