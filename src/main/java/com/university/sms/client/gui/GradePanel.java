package com.university.sms.client.gui;

import com.university.sms.client.ServerConnection;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Panel quản lý điểm số
 */
public class GradePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private User currentUser;
    private ServerConnection serverConnection;
    private boolean isReadOnly;

    public GradePanel(User currentUser, ServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;
        
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        JLabel label = new JLabel("Chức năng quản lý điểm số sẽ được phát triển trong phiên bản sau", JLabel.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }

    public void refreshData() {
        // Implementation will be added later
    }
}


