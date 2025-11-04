package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.client.gui.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Giao diện chính cho Sinh viên
 * Chức năng: Xem thời khóa biểu, Lịch học, Kết quả học tập, Đăng ký tín chỉ
 */
public class StudentMainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    private JTabbedPane tabbedPane;
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;

    // Student-specific panels
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private CourseRegistrationPanel registrationPanel;
    private GradePanel gradePanel;
    private NotificationPanel notificationPanel;

    public StudentMainFrame(User user, IServerConnection serverConnection) {
        this.currentUser = user;
        this.serverConnection = serverConnection;

        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupEventListeners();
        
        // Load initial data
        SwingUtilities.invokeLater(() -> refreshAllPanels());
    }

    private void initializeComponents() {
        setTitle("Hệ thống Quản lý Sinh viên - Sinh viên - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create status labels
        userInfoLabel = new JLabel();
        connectionStatusLabel = new JLabel();

        // Create student-specific panels
        createStudentPanels();
    }

    private void createStudentPanels() {
        // Sinh viên xem thông tin cá nhân
        studentPanel = new StudentPanel(currentUser, serverConnection, true);
        tabbedPane.addTab("Thông tin Cá nhân", createIcon("student"), studentPanel, 
                         "Xem thông tin cá nhân của bạn");

        // Sinh viên xem các khóa học đã đăng ký
        coursePanel = new CoursePanel(currentUser, serverConnection, true);
        tabbedPane.addTab("Khóa học", createIcon("course"), coursePanel, 
                         "Xem các khóa học bạn đã đăng ký");

        // Đăng ký tín chỉ (new split-panel design)
        registrationPanel = new CourseRegistrationPanel();
        registrationPanel.setServerConnection(serverConnection);
        // Note: Need to get student_id from database based on user_id
        // For now, assuming user_id == student_id (should be fixed in real implementation)
        registrationPanel.setCurrentUser(currentUser, currentUser.getUserId());
        tabbedPane.addTab("Đăng Ký Tín Chỉ", createIcon("register"), registrationPanel, 
                         "Đăng ký và quản lý tín chỉ");

        // Sinh viên xem kết quả học tập
        gradePanel = new GradePanel(currentUser, serverConnection, true);
        tabbedPane.addTab("Kết quả Học tập", createIcon("grade"), gradePanel, 
                         "Xem điểm số và kết quả học tập của bạn");

        // Thông báo
        notificationPanel = new NotificationPanel(currentUser, serverConnection, true);
        tabbedPane.addTab("Thông báo", createIcon("notification"), notificationPanel, 
                         "Xem thông báo từ giảng viên");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Main content
        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // Toolbar
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.setPreferredSize(new Dimension(0, 25));

        // User info on the left
        userInfoLabel.setText("Người dùng: " + currentUser.getFullName() + " (Sinh viên)");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusPanel.add(userInfoLabel, BorderLayout.WEST);

        // Connection status on the right
        connectionStatusLabel.setText("Trạng thái: Đã kết nối");
        connectionStatusLabel.setForeground(new Color(0, 150, 0));
        connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusPanel.add(connectionStatusLabel, BorderLayout.EAST);

        return statusPanel;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Refresh button
        JButton refreshButton = new JButton("Làm mới", createIcon("refresh"));
        refreshButton.addActionListener(e -> refreshAllPanels());
        toolBar.add(refreshButton);

        toolBar.addSeparator();

        // Course registration button
        JButton registerButton = new JButton("Đăng ký tín chỉ", createIcon("add"));
        registerButton.addActionListener(e -> showCourseRegistrationDialog());
        toolBar.add(registerButton);

        toolBar.addSeparator();

        // Change password button
        JButton changePasswordButton = new JButton("Đổi mật khẩu", createIcon("password"));
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
        toolBar.add(changePasswordButton);

        toolBar.addSeparator();

        // Logout button
        JButton logoutButton = new JButton("Đăng xuất", createIcon("logout"));
        logoutButton.addActionListener(e -> logout());
        toolBar.add(logoutButton);

        return toolBar;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("Tệp");
        fileMenu.setMnemonic('T');

        JMenuItem refreshMenuItem = new JMenuItem("Làm mới", createIcon("refresh"));
        refreshMenuItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshMenuItem.addActionListener(e -> refreshAllPanels());
        fileMenu.add(refreshMenuItem);

        fileMenu.addSeparator();

        JMenuItem logoutMenuItem = new JMenuItem("Đăng xuất", createIcon("logout"));
        logoutMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl L"));
        logoutMenuItem.addActionListener(e -> logout());
        fileMenu.add(logoutMenuItem);

        JMenuItem exitMenuItem = new JMenuItem("Thoát", createIcon("exit"));
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt F4"));
        exitMenuItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        // Tools menu
        JMenu toolsMenu = new JMenu("Công cụ");
        toolsMenu.setMnemonic('C');

        JMenuItem registerMenuItem = new JMenuItem("Đăng ký tín chỉ", createIcon("add"));
        registerMenuItem.addActionListener(e -> showCourseRegistrationDialog());
        toolsMenu.add(registerMenuItem);

        toolsMenu.addSeparator();

        JMenuItem changePasswordMenuItem = new JMenuItem("Đổi mật khẩu", createIcon("password"));
        changePasswordMenuItem.addActionListener(e -> showChangePasswordDialog());
        toolsMenu.add(changePasswordMenuItem);

        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    private void setupEventListeners() {
        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        // Server connection handler
        serverConnection.setResponseHandler(new IServerConnection.ResponseHandler() {
            @Override
            public void onResponse(Message response) {
                // Handle server responses if needed
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    connectionStatusLabel.setText("Lỗi kết nối: " + error);
                    connectionStatusLabel.setForeground(Color.RED);
                });
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    connectionStatusLabel.setText("Trạng thái: Mất kết nối");
                    connectionStatusLabel.setForeground(Color.RED);

                    int result = JOptionPane.showConfirmDialog(
                            StudentMainFrame.this,
                            "Mất kết nối đến server. Bạn có muốn thử kết nối lại không?",
                            "Mất kết nối",
                            JOptionPane.YES_NO_OPTION);

                    if (result == JOptionPane.YES_OPTION) {
                        reconnectToServer();
                    } else {
                        returnToLogin();
                    }
                });
            }
        });
    }

    private void refreshAllPanels() {
        if (studentPanel != null) studentPanel.refreshData();
        if (coursePanel != null) coursePanel.refreshData();
        if (gradePanel != null) gradePanel.refreshData();
        if (registrationPanel != null) registrationPanel.refreshData();
        if (notificationPanel != null) notificationPanel.refreshData();
        updateConnectionStatus();
    }

    private void showCourseRegistrationDialog() {
        // Switch to the registration tab
        if (registrationPanel != null) {
            int tabIndex = tabbedPane.indexOfComponent(registrationPanel);
            if (tabIndex >= 0) {
                tabbedPane.setSelectedIndex(tabIndex);
            }
        }
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, serverConnection);
        dialog.setVisible(true);
    }


    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất không?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            serverConnection.logout();
            returnToLogin();
        }
    }

    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn thoát ứng dụng không?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            serverConnection.logout();
            serverConnection.disconnect();
            System.exit(0);
        }
    }

    private void returnToLogin() {
        setVisible(false);
        SwingUtilities.invokeLater(() -> {
            com.university.sms.client.gui.common.LoginFrame loginFrame = 
                new com.university.sms.client.gui.common.LoginFrame();
            loginFrame.setVisible(true);
            dispose();
        });
    }

    private void reconnectToServer() {
        connectionStatusLabel.setText("Đang kết nối lại...");
        connectionStatusLabel.setForeground(Color.BLUE);
        returnToLogin();
    }

    private void updateConnectionStatus() {
        if (serverConnection.isConnected()) {
            connectionStatusLabel.setText("Trạng thái: Đã kết nối");
            connectionStatusLabel.setForeground(new Color(0, 150, 0));
        } else {
            connectionStatusLabel.setText("Trạng thái: Mất kết nối");
            connectionStatusLabel.setForeground(Color.RED);
        }
    }

    private Icon createIcon(String iconName) {
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB));
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public IServerConnection getServerConnection() {
        return serverConnection;
    }
}

