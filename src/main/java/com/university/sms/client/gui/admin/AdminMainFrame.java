package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
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
import java.net.InetAddress;

public class AdminMainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private User currentUser;
    private IServerConnection serverConnection;
    private ModernDashboard modernDashboard;
    private JLabel userInfoLabel;
    private JLabel connectionStatusLabel;
    private JLabel serverStatsLabel;
    private JLabel serverVersionLabel;
    private JLabel clientDbVersionLabel;
    private JLabel syncStatusLabel;
    private StudentPanel studentPanel;
    private CoursePanel coursePanel;
    private AdminPanel adminPanel;
    private ReportPanel reportPanel;
    private TeacherPanel teacherPanel;
    private SubjectPanel subjectPanel;
    private ClassPanel classPanel;
    private NotificationPanel notificationPanel;
    private javax.swing.Timer clientDbVersionTimer;
    private WatchService watchService;
    private Thread watchThread;
    private SyncStatusListener syncStatusListener;
    private volatile boolean autoSyncInProgress;

    public AdminMainFrame(User user, IServerConnection serverConnection) {
        this.currentUser = user;
        this.serverConnection = serverConnection;
        // Attach sync progress listener để cập nhật trạng thái khi đang upload/download
        attachSyncProgressListener();

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
        serverVersionLabel = new JLabel();
        clientDbVersionLabel = new JLabel();
        syncStatusLabel = new JLabel();

        createAdminPanels();

        // Hiển thị IP của client đang kết nối (thay cho thống kê Clients/DB version)
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            serverStatsLabel.setText("Client IP: " + ip);
        } catch (Exception e) {
            serverStatsLabel.setText("Client IP: unknown");
        }

        // Chỉ load sync status và start timer cho CSV/Postgres client
        if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection
                || serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
            loadClientDbVersion();
            loadSyncStatus();
            startVersionFileWatcher();
            startSyncStatusTimer();
        }
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

        // Quản lý lớp sinh hoạt
        classPanel = new ClassPanel(currentUser, serverConnection);
        modernDashboard.addNavItem("🏫", "Quản lý Lớp sinh hoạt", "class", classPanel);

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

        // Center: Client IP (always) + Sync status (only for CSV/Postgres client)
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        centerPanel.setOpaque(false);
        serverStatsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        serverStatsLabel.setForeground(new Color(100, 100, 100));
        centerPanel.add(serverStatsLabel);
        
        // Chỉ hiển thị sync status cho CSV/Postgres client
        if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection
                || serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
            serverVersionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            serverVersionLabel.setForeground(new Color(100, 100, 100));
            clientDbVersionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            clientDbVersionLabel.setForeground(new Color(100, 100, 100));
            syncStatusLabel.setFont(new Font("Arial", Font.BOLD, 11));
            centerPanel.add(serverVersionLabel);
            centerPanel.add(clientDbVersionLabel);
            centerPanel.add(syncStatusLabel);
        }
        
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
        if (classPanel != null)
            classPanel.refreshData();
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
            // Check if this is a CSV or PostgreSQL client and create appropriate LoginFrame
            com.university.sms.client.gui.common.LoginFrame.ConnectionFactory factory;
            if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
                factory = new com.university.sms.client.gui.common.LoginFrame.CsvConnectionFactory();
            } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
                factory = new com.university.sms.client.gui.common.LoginFrame.PostgresConnectionFactory();
            } else {
                factory = new com.university.sms.client.gui.common.LoginFrame.RegularConnectionFactory();
            }
            com.university.sms.client.gui.common.LoginFrame loginFrame = new com.university.sms.client.gui.common.LoginFrame(
                    factory);
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
     * Load client database version (if CSV or PostgreSQL client)
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
        } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
            try {
                com.university.sms.postgresclient.PostgresDataService postgresDataService = new com.university.sms.postgresclient.PostgresDataService();
                int version = postgresDataService.getVersion();
                // Format version tối đa 10 chữ số
                String versionStr = String.valueOf(version);
                if (versionStr.length() > 10) {
                    versionStr = versionStr.substring(0, 10);
                }
                clientDbVersionLabel.setText("PostgreSQL DB v" + versionStr);
            } catch (Exception e) {
                clientDbVersionLabel.setText("PostgreSQL DB: N/A");
            }
        } else {
            clientDbVersionLabel.setText("Client: Regular");
        }
    }

    /**
     * Load sync status (version comparison and action)
     * Tự động thực hiện upload/download tương ứng
     */
    private void loadSyncStatus() {
        // Chỉ xử lý cho CSV hoặc PostgreSQL client
        if (!(serverConnection instanceof com.university.sms.csvclient.CSVServerConnection)
                && !(serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection)) {
            syncStatusLabel.setText("");
            syncStatusLabel.setForeground(Color.BLACK);
            return;
        }

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                int clientVersion = 0;
                Message metadataResponse = null;

                // Get client version và server version via sync check
                if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
                    com.university.sms.csvclient.CSVDataService csvDataService = new com.university.sms.csvclient.CSVDataService();
                    clientVersion = csvDataService.getVersion();
                    com.university.sms.csvclient.CSVServerConnection csvConn = (com.university.sms.csvclient.CSVServerConnection) serverConnection;
                    metadataResponse = csvConn.sendMetadata();
                } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
                    com.university.sms.postgresclient.PostgresDataService postgresDataService = new com.university.sms.postgresclient.PostgresDataService();
                    clientVersion = postgresDataService.getVersion();
                    com.university.sms.postgresclient.PostgresServerConnection pgConn = (com.university.sms.postgresclient.PostgresServerConnection) serverConnection;
                    metadataResponse = pgConn.sendMetadata();
                }

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

                    // Store server version for UI update
                    metadataResponse.addData("serverVersionStr", serverVersionStr);
                    metadataResponse.addData("clientVersionStr", clientVersionStr);

                    // Create status message based on syncAction
                    String statusText;
                    Color statusColor;

                    if ("UPLOAD_TO_SERVER".equals(syncAction)) {
                        statusText = "↑ Cần upload";
                        statusColor = new Color(0, 150, 0); // Green
                    } else if ("DOWNLOAD_FROM_SERVER".equals(syncAction)) {
                        statusText = "↓ Cần download";
                        statusColor = new Color(0, 100, 200); // Blue
                    } else {
                        statusText = "✓ Đã đồng bộ";
                        statusColor = new Color(100, 100, 100); // Gray
                    }

                    // Store in response for UI update
                    metadataResponse.addData("statusText", statusText);
                    metadataResponse.addData("statusColor", statusColor);
                    metadataResponse.addData("syncAction", syncAction);
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
                        String serverVersionStr = (String) response.getData("serverVersionStr");
                        String clientVersionStr = (String) response.getData("clientVersionStr");
                        String syncAction = (String) response.getData("syncAction");

                        // Cập nhật server version label
                        if (serverVersionStr != null) {
                            String clientType = "";
                            if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
                                clientType = "CSV";
                            } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
                                clientType = "PostgreSQL";
                            }
                            if (!clientType.isEmpty()) {
                                serverVersionLabel.setText("Server " + clientType + " v" + serverVersionStr);
                            } else {
                                serverVersionLabel.setText("Server v" + serverVersionStr);
                            }
                        } else {
                            serverVersionLabel.setText("");
                        }

                        // Cập nhật client DB version nếu có thay đổi
                        if (clientVersionStr != null) {
                            SwingUtilities.invokeLater(() -> {
                                loadClientDbVersion();
                            });
                        }

                        // Cập nhật sync status
                        if (statusText != null) {
                            syncStatusLabel.setText(statusText);
                            if (statusColor != null) {
                                syncStatusLabel.setForeground(statusColor);
                            }
                        }

                        // Thực hiện auto sync nếu cần
                        if (syncAction != null
                                && ("UPLOAD_TO_SERVER".equals(syncAction)
                                        || "DOWNLOAD_FROM_SERVER".equals(syncAction))) {
                            runAutoSync(syncAction);
                        }
                    } else {
                        syncStatusLabel.setText("Sync: N/A");
                        syncStatusLabel.setForeground(Color.GRAY);
                        serverVersionLabel.setText("");
                    }
                } catch (Exception e) {
                    syncStatusLabel.setText("Sync: Error");
                    syncStatusLabel.setForeground(Color.RED);
                    serverVersionLabel.setText("");
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

    /**
     * Attach sync progress listener để cập nhật trạng thái khi đang upload/download
     * Sử dụng LoadingOverlay cho loading dialog và SyncStatusListener cho status
     * label
     */
    private void attachSyncProgressListener() {
        syncStatusListener = new SyncStatusListener();

        if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
            com.university.sms.csvclient.CSVServerConnection csvConn = (com.university.sms.csvclient.CSVServerConnection) serverConnection;
            csvConn.setSyncProgressListener(syncStatusListener);
        } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
            com.university.sms.postgresclient.PostgresServerConnection pgConn = (com.university.sms.postgresclient.PostgresServerConnection) serverConnection;
            pgConn.setSyncProgressListener(syncStatusListener);
        }
    }

    /**
     * Listener để cập nhật trạng thái sync trong status bar và loading overlay
     */
    private class SyncStatusListener implements com.university.sms.csvclient.CSVServerConnection.SyncProgressListener,
            com.university.sms.postgresclient.PostgresServerConnection.SyncProgressListener {
        private final com.university.sms.client.gui.common.LoadingOverlay overlay = com.university.sms.client.gui.common.LoadingOverlay
                .forWindow(AdminMainFrame.this);

        @Override
        public void onSyncStart(String action) {
            if (action == null || "NO_SYNC_NEEDED".equals(action)) {
                return;
            }

            // Hiển thị loading overlay
            String title = "Đang đồng bộ dữ liệu";
            if ("UPLOAD_TO_SERVER".equals(action)) {
                title = "Đang upload dữ liệu";
            } else if ("DOWNLOAD_FROM_SERVER".equals(action)) {
                title = "Đang download dữ liệu từ server";
            }
            overlay.show(title, "Đang chuẩn bị...");

            // Cập nhật status label
            SwingUtilities.invokeLater(() -> {
                if ("UPLOAD_TO_SERVER".equals(action)) {
                    syncStatusLabel.setText("↑ Đang upload...");
                    syncStatusLabel.setForeground(new Color(0, 150, 0)); // Green
                } else if ("DOWNLOAD_FROM_SERVER".equals(action)) {
                    syncStatusLabel.setText("↓ Đang download...");
                    syncStatusLabel.setForeground(new Color(0, 100, 200)); // Blue
                }
            });
        }

        @Override
        public void onSyncStep(String action, String message) {
            if (action == null || "NO_SYNC_NEEDED".equals(action)) {
                return;
            }
            overlay.updateMessage(message);
        }

        @Override
        public void onSyncCompleted(String action, com.university.sms.common.Message result) {
            if (action == null || "NO_SYNC_NEEDED".equals(action)) {
                overlay.hide();
                return;
            }

            // Cập nhật loading overlay
            boolean success = result != null && result.isSuccess();
            String message;
            if (result == null) {
                message = "Kết thúc đồng bộ";
            } else if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                message = result.getMessage();
            } else {
                message = success ? "Đồng bộ thành công" : "Đồng bộ thất bại";
            }
            overlay.complete(message, success);

            // Cập nhật status label
            SwingUtilities.invokeLater(() -> {
                if (result != null && result.isSuccess()) {
                    // Sau khi sync thành công, reload sync status để cập nhật version
                    loadSyncStatus();
                } else {
                    syncStatusLabel.setText("✗ Lỗi: " + (result != null ? result.getMessage() : "Unknown error"));
                    syncStatusLabel.setForeground(Color.RED);
                }
            });
        }
    }

    /**
     * Tự động thực hiện upload hoặc download khi cần
     */
    private void runAutoSync(String syncAction) {
        if (syncAction == null || "NO_SYNC_NEEDED".equals(syncAction) || autoSyncInProgress) {
            return;
        }

        autoSyncInProgress = true;

        SwingWorker<Message, Void> syncWorker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                if (serverConnection instanceof com.university.sms.csvclient.CSVServerConnection) {
                    com.university.sms.csvclient.CSVServerConnection csvConn = (com.university.sms.csvclient.CSVServerConnection) serverConnection;
                    return csvConn.syncData(syncAction);
                } else if (serverConnection instanceof com.university.sms.postgresclient.PostgresServerConnection) {
                    com.university.sms.postgresclient.PostgresServerConnection pgConn = (com.university.sms.postgresclient.PostgresServerConnection) serverConnection;
                    return pgConn.syncData(syncAction);
                }
                return Message.createErrorResponse(Constants.ACTION_SYNC_DATA, "Unsupported client for auto sync");
            }

            @Override
            protected void done() {
                autoSyncInProgress = false;
                boolean reloadNeeded = true;
                try {
                    Message result = get();
                    if (result == null || !result.isSuccess()) {
                        syncStatusLabel.setText("✗ Sync lỗi: "
                                + (result != null && result.getMessage() != null ? result.getMessage()
                                        : "Unknown error"));
                        syncStatusLabel.setForeground(Color.RED);
                        reloadNeeded = true;
                    }
                } catch (Exception e) {
                    syncStatusLabel.setText("✗ Sync lỗi: " + e.getMessage());
                    syncStatusLabel.setForeground(Color.RED);
                    reloadNeeded = true;
                } finally {
                    if (reloadNeeded) {
                        loadSyncStatus();
                    }
                }
            }
        };

        syncWorker.execute();
    }
}
