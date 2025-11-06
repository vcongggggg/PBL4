package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.client.gui.common.*;
import javax.swing.*;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AdminMainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private User currentUser;
    private IServerConnection serverConnection;
    private ModernDashboard modernDashboard; // Thay thế JTabbedPane
    private JTabbedPane tabbedPane; // Giữ lại để dễ rollback nếu cần
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private AdminPanel adminPanel;
    private ReportPanel reportPanel;
    private TeacherPanel teacherPanel;
    private SubjectPanel subjectPanel;
    private NotificationPanel notificationPanel;

    public AdminMainFrame(User user, IServerConnection serverConnection) {
        this.currentUser = user;
        this.serverConnection = serverConnection;

        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupEventListeners();

        // Refresh panel đầu tiên sau khi window được show
        addWindowListener(new WindowAdapter() {
            private boolean firstTime = true;

            @Override
            public void windowOpened(WindowEvent e) {
                if (firstTime) {
                    firstTime = false;
                    SwingUtilities.invokeLater(() -> {
                        if (studentPanel != null)
                            studentPanel.refreshData();
                    });
                }
            }
        });
    }

    private void initializeComponents() {
        setTitle("Hệ thống Quản lý Sinh viên - Admin - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create Modern Dashboard (thay thế JTabbedPane)
        modernDashboard = new ModernDashboard(serverConnection, currentUser);

        userInfoLabel = new JLabel();
        connectionStatusLabel = new JLabel();

        createAdminPanels();
    }

    private void createAdminPanels() {
        // Quản lý sinh viên
        studentPanel = new StudentPanel(currentUser, serverConnection, false);
        modernDashboard.addNavItem("👥", "Quản lý Sinh viên", "student", studentPanel);

        // Quản lý giảng viên
        teacherPanel = new TeacherPanel(currentUser, serverConnection);
        modernDashboard.addNavItem("👨‍🏫", "Quản lý Giảng viên", "teacher", teacherPanel);

        // Quản lý lớp học phần
        coursePanel = new CoursePanel(currentUser, serverConnection, false);
        modernDashboard.addNavItem("📚", "Quản lý Lớp học phần", "course", coursePanel);

        // Khung chương trình đào tạo
        subjectPanel = new SubjectPanel(currentUser, serverConnection);
        modernDashboard.addNavItem("📖", "Khung chương trình", "subject", subjectPanel);

        // Quản trị Hệ thống
        adminPanel = new AdminPanel(currentUser, serverConnection);
        modernDashboard.addNavItem("🔧", "Quản trị Hệ thống", "admin", adminPanel);

        // Báo cáo & Thống kê
        reportPanel = new ReportPanel(currentUser, serverConnection);
        modernDashboard.addNavItem("📈", "Báo cáo & Thống kê", "report", reportPanel);

        // Thông báo (với badge)
        notificationPanel = new NotificationPanel(currentUser, serverConnection, false);
        modernDashboard.addNavItemWithBadge("🔔", "Thông báo", "notification", notificationPanel);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Main content - Sử dụng ModernDashboard
        add(modernDashboard, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.setPreferredSize(new Dimension(0, 25));

        userInfoLabel.setText("Người dùng: " + currentUser.getFullName() + " (Admin)");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusPanel.add(userInfoLabel, BorderLayout.WEST);

        connectionStatusLabel.setText("Trạng thái: Đã kết nối");
        connectionStatusLabel.setForeground(new Color(0, 150, 0));
        connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusPanel.add(connectionStatusLabel, BorderLayout.EAST);

        return statusPanel;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

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

        JMenu toolsMenu = new JMenu("Công cụ");
        toolsMenu.setMnemonic('C');

        JMenuItem changePasswordMenuItem = new JMenuItem("Đổi mật khẩu", createIcon("password"));
        changePasswordMenuItem.addActionListener(e -> showChangePasswordDialog());
        toolsMenu.add(changePasswordMenuItem);

        toolsMenu.addSeparator();

        // Dark Mode Toggle in menu
        // JPanel darkModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // darkModePanel.setOpaque(false);
        // darkModePanel.add(new JLabel("Dark Mode:"));
        // DarkModeToggle darkModeToggle = new DarkModeToggle();
        // darkModePanel.add(darkModeToggle);
        // JMenuItem darkModeMenuItem = new JMenuItem();
        // darkModeMenuItem.setLayout(new BorderLayout());
        // darkModeMenuItem.add(darkModePanel, BorderLayout.CENTER);
        // toolsMenu.add(darkModeMenuItem);

        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    private void setupEventListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        serverConnection.setResponseHandler(new IServerConnection.ResponseHandler() {
            @Override
            public void onResponse(Message response) {
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
        if (studentPanel != null)
            studentPanel.refreshData();
        if (coursePanel != null)
            coursePanel.refreshData();
        if (adminPanel != null)
            adminPanel.refreshData();
        if (reportPanel != null)
            reportPanel.refreshData();
        if (teacherPanel != null)
            teacherPanel.refreshData();
        if (subjectPanel != null)
            subjectPanel.refreshData();
        if (notificationPanel != null)
            notificationPanel.refreshData();
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
            com.university.sms.client.gui.common.LoginFrame loginFrame = new com.university.sms.client.gui.common.LoginFrame();
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
