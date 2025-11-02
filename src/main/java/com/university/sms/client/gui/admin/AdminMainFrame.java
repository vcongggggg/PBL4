package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.client.gui.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Giao diện chính cho Admin
 * Quản lý: Người dùng, Môn học, Duyệt yêu cầu mở lớp
 */
public class AdminMainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    private JTabbedPane tabbedPane;
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;

    // Admin-specific panels
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private AdminPanel adminPanel;
    private ReportPanel reportPanel;

    public AdminMainFrame(User user, IServerConnection serverConnection) {
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
        setTitle("Hệ thống Quản lý Sinh viên - Admin - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create status labels
        userInfoLabel = new JLabel();
        connectionStatusLabel = new JLabel();

        // Create admin-specific panels
        createAdminPanels();
    }

    private void createAdminPanels() {
        // Admin quản lý sinh viên
        studentPanel = new StudentPanel(currentUser, serverConnection, false);
        tabbedPane.addTab("Quản lý Sinh viên", createIcon("student"), studentPanel, 
                         "Quản lý toàn bộ thông tin sinh viên");

        // Admin quản lý môn học
        coursePanel = new CoursePanel(currentUser, serverConnection, false);
        tabbedPane.addTab("Quản lý Môn học", createIcon("course"), coursePanel, 
                         "Quản lý tất cả các môn học");

        // Admin panel cho quản trị hệ thống
        adminPanel = new AdminPanel(currentUser, serverConnection);
        tabbedPane.addTab("Quản trị Hệ thống", createIcon("admin"), adminPanel, 
                         "Quản trị hệ thống");

        // Admin xem báo cáo
        reportPanel = new ReportPanel(currentUser, serverConnection);
        tabbedPane.addTab("Báo cáo & Thống kê", createIcon("report"), reportPanel, 
                         "Xem báo cáo tổng hợp");
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
        userInfoLabel.setText("Người dùng: " + currentUser.getFullName() + " (Admin)");
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
                            AdminMainFrame.this,
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
        if (adminPanel != null) adminPanel.refreshData();
        if (reportPanel != null) reportPanel.refreshData();
        updateConnectionStatus();
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

