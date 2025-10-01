package com.university.sms.client.gui;

import com.university.sms.client.ServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Giao diện chính của ứng dụng
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private User currentUser;
    private ServerConnection serverConnection;
    
    private JTabbedPane tabbedPane;
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;
    
    // Panels for different functionalities
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private GradePanel gradePanel;
    private ReportPanel reportPanel;
    private AdminPanel adminPanel;

    public MainFrame(User user, ServerConnection serverConnection) {
        this.currentUser = user;
        this.serverConnection = serverConnection;
        
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupEventListeners();
        setupUserInterface();
    }

    private void initializeComponents() {
        setTitle("Hệ thống Quản lý Sinh viên - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create status labels
        userInfoLabel = new JLabel();
        connectionStatusLabel = new JLabel();
        
        // Create panels based on user role
        createPanelsBasedOnRole();
    }

    private void createPanelsBasedOnRole() {
        switch (currentUser.getRole()) {
            case STUDENT:
                createStudentPanels();
                break;
            case TEACHER:
                createTeacherPanels();
                break;
            case ADMIN:
                createAdminPanels();
                break;
        }
    }

    private void createStudentPanels() {
        // Student can view their own information, courses, and grades
        studentPanel = new StudentPanel(currentUser, serverConnection, true); // read-only for student
        coursePanel = new CoursePanel(currentUser, serverConnection, true);
        gradePanel = new GradePanel(currentUser, serverConnection, true);
        
        tabbedPane.addTab("Thông tin cá nhân", createIcon("student"), studentPanel, "Xem thông tin cá nhân");
        tabbedPane.addTab("Khóa học", createIcon("course"), coursePanel, "Xem khóa học đã đăng ký");
        tabbedPane.addTab("Điểm số", createIcon("grade"), gradePanel, "Xem điểm số và kết quả học tập");
    }

    private void createTeacherPanels() {
        // Teacher can manage students, courses, and grades
        studentPanel = new StudentPanel(currentUser, serverConnection, false);
        coursePanel = new CoursePanel(currentUser, serverConnection, false);
        gradePanel = new GradePanel(currentUser, serverConnection, false);
        reportPanel = new ReportPanel(currentUser, serverConnection);
        
        tabbedPane.addTab("Quản lý Sinh viên", createIcon("student"), studentPanel, "Quản lý thông tin sinh viên");
        tabbedPane.addTab("Quản lý Khóa học", createIcon("course"), coursePanel, "Quản lý khóa học giảng dạy");
        tabbedPane.addTab("Quản lý Điểm", createIcon("grade"), gradePanel, "Nhập và quản lý điểm số");
        tabbedPane.addTab("Báo cáo", createIcon("report"), reportPanel, "Xem báo cáo và thống kê");
    }

    private void createAdminPanels() {
        // Admin has access to all functionalities
        studentPanel = new StudentPanel(currentUser, serverConnection, false);
        coursePanel = new CoursePanel(currentUser, serverConnection, false);
        gradePanel = new GradePanel(currentUser, serverConnection, false);
        reportPanel = new ReportPanel(currentUser, serverConnection);
        adminPanel = new AdminPanel(currentUser, serverConnection);
        
        tabbedPane.addTab("Quản lý Sinh viên", createIcon("student"), studentPanel, "Quản lý toàn bộ sinh viên");
        tabbedPane.addTab("Quản lý Khóa học", createIcon("course"), coursePanel, "Quản lý tất cả khóa học");
        tabbedPane.addTab("Quản lý Điểm", createIcon("grade"), gradePanel, "Quản lý điểm số hệ thống");
        tabbedPane.addTab("Báo cáo", createIcon("report"), reportPanel, "Báo cáo tổng hợp");
        tabbedPane.addTab("Quản trị", createIcon("admin"), adminPanel, "Quản trị hệ thống");
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
        userInfoLabel.setText("Người dùng: " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
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
        
        // Change password button
        JButton changePasswordButton = new JButton("Đổi mật khẩu", createIcon("password"));
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
        toolBar.add(changePasswordButton);
        
        toolBar.addSeparator();
        
        // Logout button
        JButton logoutButton = new JButton("Đăng xuất", createIcon("logout"));
        logoutButton.addActionListener(e -> logout());
        toolBar.add(logoutButton);
        
        // Add glue to push items to the left
        toolBar.add(Box.createHorizontalGlue());
        
        // About button
        JButton aboutButton = new JButton("Về chương trình", createIcon("info"));
        aboutButton.addActionListener(e -> showAboutDialog());
        toolBar.add(aboutButton);
        
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
        
        JMenuItem changePasswordMenuItem = new JMenuItem("Đổi mật khẩu", createIcon("password"));
        changePasswordMenuItem.addActionListener(e -> showChangePasswordDialog());
        toolsMenu.add(changePasswordMenuItem);
        
        menuBar.add(toolsMenu);
        
        // Help menu
        JMenu helpMenu = new JMenu("Trợ giúp");
        helpMenu.setMnemonic('H');
        
        JMenuItem aboutMenuItem = new JMenuItem("Về chương trình", createIcon("info"));
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutMenuItem);
        
        menuBar.add(helpMenu);
        
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
        serverConnection.setResponseHandler(new ServerConnection.ResponseHandler() {
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
                        MainFrame.this,
                        "Mất kết nối đến server. Bạn có muốn thử kết nối lại không?",
                        "Mất kết nối",
                        JOptionPane.YES_NO_OPTION
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        // Try to reconnect
                        reconnectToServer();
                    } else {
                        // Return to login screen
                        returnToLogin();
                    }
                });
            }
        });
    }

    private void setupUserInterface() {
        // Set initial focus
        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
        
        // Update connection status
        updateConnectionStatus();
    }

    private void refreshAllPanels() {
        // Refresh all panels
        if (studentPanel != null) {
            studentPanel.refreshData();
        }
        if (coursePanel != null) {
            coursePanel.refreshData();
        }
        if (gradePanel != null) {
            gradePanel.refreshData();
        }
        if (reportPanel != null) {
            reportPanel.refreshData();
        }
        if (adminPanel != null) {
            adminPanel.refreshData();
        }
        
        updateConnectionStatus();
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, serverConnection);
        dialog.setVisible(true);
    }

    private void showAboutDialog() {
        String message = "Hệ thống Quản lý Sinh viên\n\n" +
                        "Phiên bản: 1.0\n" +
                        "Phát triển bởi: Nhóm PBL4\n" +
                        "Năm: 2024\n\n" +
                        "Hệ thống quản lý thông tin sinh viên, khóa học,\n" +
                        "điểm số và các hoạt động học tập khác.";
        
        JOptionPane.showMessageDialog(this, message, "Về chương trình", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc chắn muốn đăng xuất không?",
            "Xác nhận đăng xuất",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Send logout request to server
            serverConnection.logout();
            
            // Return to login screen
            returnToLogin();
        }
    }

    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc chắn muốn thoát ứng dụng không?",
            "Xác nhận thoát",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Logout and disconnect
            serverConnection.logout();
            serverConnection.disconnect();
            
            // Exit application
            System.exit(0);
        }
    }

    private void returnToLogin() {
        // Hide main window
        setVisible(false);
        
        // Show login window
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            
            // Dispose main window
            dispose();
        });
    }

    private void reconnectToServer() {
        // Implementation for reconnection
        connectionStatusLabel.setText("Đang kết nối lại...");
        connectionStatusLabel.setForeground(Color.BLUE);
        
        // This would need to be implemented based on ServerConnection capabilities
        // For now, just return to login
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
        // Simple icon creation - in a real application, you would load actual icons
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB));
    }

    // Getter methods for panels (for testing or external access)
    public User getCurrentUser() {
        return currentUser;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
}
