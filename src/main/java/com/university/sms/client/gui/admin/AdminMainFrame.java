package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.client.gui.common.*;
import javax.swing.*;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.io.IOException;

public class AdminMainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private User currentUser;
    private IServerConnection serverConnection;
    private ModernDashboard modernDashboard;
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;
    private JLabel serverStatsLabel;
    private JLabel clientDbVersionLabel;
    private JLabel syncStatusLabel;
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private AdminPanel adminPanel;
    private ReportPanel reportPanel;
    private TeacherPanel teacherPanel;
    private SubjectPanel subjectPanel;
    private NotificationPanel notificationPanel;
    private javax.swing.Timer clientDbVersionTimer;
    private WatchService watchService;
    private Thread watchThread;

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
        serverStatsLabel = new JLabel();
        clientDbVersionLabel = new JLabel();
        syncStatusLabel = new JLabel();

        createAdminPanels();

        // Load server statistics and client DB version
        loadServerStatistics();
        loadClientDbVersion();
        loadSyncStatus();

        // Start file watcher to auto-update client DB version when .version file
        // changes
        startVersionFileWatcher();

        // Start timer to auto-update sync status
        startSyncStatusTimer();
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

        // Left side: User info
        userInfoLabel.setText("Người dùng: " + currentUser.getFullName() + " (Admin)");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        statusPanel.add(userInfoLabel, BorderLayout.WEST);

        // Center: Server stats, client DB version, and sync status
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        centerPanel.setOpaque(false);
        serverStatsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        serverStatsLabel.setForeground(new Color(100, 100, 100));
        clientDbVersionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        clientDbVersionLabel.setForeground(new Color(100, 100, 100));
        syncStatusLabel.setFont(new Font("Arial", Font.BOLD, 11));
        centerPanel.add(serverStatsLabel);
        centerPanel.add(clientDbVersionLabel);
        centerPanel.add(syncStatusLabel);
        statusPanel.add(centerPanel, BorderLayout.CENTER);

        // Right side: Connection status
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

    /**
     * Load server statistics (admin only)
     */
    private void loadServerStatistics() {
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.getServerStatistics();
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        Integer totalClients = (Integer) response.getData("totalClients");
                        Integer adminClients = (Integer) response.getData("adminClients");
                        Integer teacherClients = (Integer) response.getData("teacherClients");
                        Integer studentClients = (Integer) response.getData("studentClients");

                        if (totalClients != null) {
                            String statsText = String.format(
                                    "Clients: Tổng %d (Admin: %d, Teacher: %d, Student: %d)",
                                    totalClients, adminClients != null ? adminClients : 0,
                                    teacherClients != null ? teacherClients : 0,
                                    studentClients != null ? studentClients : 0);
                            serverStatsLabel.setText(statsText);
                        }
                    } else {
                        serverStatsLabel.setText("Không thể tải thống kê server");
                    }
                } catch (Exception e) {
                    serverStatsLabel.setText("Lỗi tải thống kê server");
                }
            }
        };
        worker.execute();
    }

    /**
     * Load client database version (if CSV client)
     */
    private void loadClientDbVersion() {
        if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
            try {
                com.university.sms.csvclient.CSVDataService csvDataService = new com.university.sms.csvclient.CSVDataService();
                int version = csvDataService.getVersion();
                // Format version tối đa 10 chữ số
                String versionStr = String.valueOf(version);
                if (versionStr.length() > 10) {
                    versionStr = versionStr.substring(0, 10);
                }
                clientDbVersionLabel.setText("Client DB v" + versionStr);
            } catch (Exception e) {
                clientDbVersionLabel.setText("Client DB: N/A");
            }
        } else {
            clientDbVersionLabel.setText("Client: Regular");
        }
    }

    /**
     * Load sync status (version comparison and action)
     * Tự động download khi phát hiện server version mới hơn
     */
    private void loadSyncStatus() {
        if (!(serverConnection instanceof com.university.sms.csvclient.CSVServerConnection)) {
            syncStatusLabel.setText("");
            syncStatusLabel.setForeground(Color.BLACK);
            return; // Only show for CSV clients
        }

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                // Get client version
                com.university.sms.csvclient.CSVDataService csvDataService = new com.university.sms.csvclient.CSVDataService();
                int clientVersion = csvDataService.getVersion();

                // Get server version via sync check
                com.university.sms.csvclient.CSVServerConnection csvConn = (com.university.sms.csvclient.CSVServerConnection) serverConnection;
                Message metadataResponse = csvConn.sendMetadata();

                if (metadataResponse != null && metadataResponse.isSuccess()) {
                    Integer serverVersion = (Integer) metadataResponse.getData("client_source_version");
                    String syncAction = (String) metadataResponse.getData("sync_action");

                    // Format versions (max 10 digits)
                    String clientVersionStr = String.valueOf(clientVersion);
                    if (clientVersionStr.length() > 10) {
                        clientVersionStr = clientVersionStr.substring(0, 10);
                    }
                    String serverVersionStr = serverVersion != null ? String.valueOf(serverVersion) : "0";
                    if (serverVersionStr.length() > 10) {
                        serverVersionStr = serverVersionStr.substring(0, 10);
                    }

                    // Create status message
                    String statusText;
                    Color statusColor;

                    if ("UPLOAD_TO_SERVER".equals(syncAction)) {
                        statusText = String.format("↑ Upload: Client v%s > Server v%s", clientVersionStr,
                                serverVersionStr);
                        statusColor = new Color(0, 150, 0); // Green
                    } else if ("DOWNLOAD_FROM_SERVER".equals(syncAction)) {
                        // TỰ ĐỘNG DOWNLOAD khi phát hiện server version mới hơn
                        statusText = String.format("↓ Download: Server v%s > Client v%s", serverVersionStr,
                                clientVersionStr);
                        statusColor = new Color(0, 100, 200); // Blue

                        // Tự động download
                        Message downloadResponse = csvConn.syncData("DOWNLOAD_FROM_SERVER");
                        if (downloadResponse.isSuccess()) {
                            // Cập nhật lại client version sau khi download
                            try {
                                csvDataService = new com.university.sms.csvclient.CSVDataService();
                                int newClientVersion = csvDataService.getVersion();
                                String newClientVersionStr = String.valueOf(newClientVersion);
                                if (newClientVersionStr.length() > 10) {
                                    newClientVersionStr = newClientVersionStr.substring(0, 10);
                                }
                                statusText = String.format("✓ Đã đồng bộ: Client v%s = Server v%s", newClientVersionStr,
                                        serverVersionStr);
                                statusColor = new Color(100, 100, 100); // Gray - đã sync
                            } catch (Exception e) {
                                statusText = statusText + " (Đã tải về)";
                                // Ignore nếu không cập nhật được version
                            }
                        } else {
                            statusText = statusText + " (Lỗi: " + downloadResponse.getMessage() + ")";
                            statusColor = Color.RED;
                        }
                    } else {
                        statusText = String.format("✓ Synced: Client v%s = Server v%s", clientVersionStr,
                                serverVersionStr);
                        statusColor = new Color(100, 100, 100); // Gray
                    }

                    // Store in response for UI update
                    metadataResponse.addData("statusText", statusText);
                    metadataResponse.addData("statusColor", statusColor);
                }

                return metadataResponse;
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response != null && response.isSuccess()) {
                        String statusText = (String) response.getData("statusText");
                        Color statusColor = (Color) response.getData("statusColor");

                        if (statusText != null) {
                            syncStatusLabel.setText(statusText);
                            if (statusColor != null) {
                                syncStatusLabel.setForeground(statusColor);
                            }
                        }

                        // Cập nhật lại client DB version sau khi download (nếu có thay đổi)
                        if (statusText != null && statusText.contains("Đã đồng bộ")) {
                            loadClientDbVersion();
                        }
                    } else {
                        syncStatusLabel.setText("Sync: N/A");
                        syncStatusLabel.setForeground(Color.GRAY);
                    }
                } catch (Exception e) {
                    syncStatusLabel.setText("Sync: Error");
                    syncStatusLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    /**
     * Start timer to auto-update sync status every 10 seconds
     */
    private void startSyncStatusTimer() {
        javax.swing.Timer syncTimer = new javax.swing.Timer(10000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSyncStatus();
            }
        });
        syncTimer.start();
    }

    /**
     * Start file watcher to auto-update client DB version when .version file
     * changes
     */
    private void startVersionFileWatcher() {
        if (!(serverConnection instanceof com.university.sms.csvclient.CSVServerConnection)) {
            return; // Only watch for CSV clients
        }

        try {
            // Get the CSV data directory
            java.nio.file.Path dataDir = java.nio.file.Paths.get("data", "csv");
            java.nio.file.Path versionFile = dataDir.resolve(".version");

            if (!Files.exists(versionFile)) {
                return; // File doesn't exist yet
            }

            // Create watch service
            watchService = FileSystems.getDefault().newWatchService();
            dataDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // Start watching thread
            watchThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        WatchKey key = watchService.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                                Path filename = ev.context();

                                // Check if .version file was modified
                                if (filename.toString().equals(".version")) {
                                    // Update UI on EDT
                                    SwingUtilities.invokeLater(() -> {
                                        loadClientDbVersion();
                                    });
                                }
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // Log error but don't crash
                    System.err.println("Error watching version file: " + e.getMessage());
                }
            });

            watchThread.setDaemon(true);
            watchThread.start();

        } catch (IOException e) {
            // If file watching fails, fall back to timer
            System.err.println("Failed to start file watcher, using timer instead: " + e.getMessage());
            startClientDbVersionTimer();
        }
    }

    /**
     * Fallback: Start timer to auto-update client DB version every 5 seconds
     */
    private void startClientDbVersionTimer() {
        clientDbVersionTimer = new javax.swing.Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadClientDbVersion();
            }
        });
        clientDbVersionTimer.start();
    }

    /**
     * Clean up resources when window closes
     */
    @Override
    public void dispose() {
        // Stop file watcher
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        // Stop watch thread
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }

        // Stop timer
        if (clientDbVersionTimer != null) {
            clientDbVersionTimer.stop();
        }

        super.dispose();
    }
}
