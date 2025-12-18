package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Notification;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel quản lý thông báo
 * - Admin/Teacher: Có thể gửi thông báo
 * - Student: Chỉ xem thông báo nhận được
 */
public class NotificationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable notificationTable;
    private DefaultTableModel tableModel;
    private JButton sendButton;
    private JButton markAllReadButton;
    private JButton refreshButton;
    private JLabel unreadCountLabel;
    private JButton notificationButton; // Button with badge for dropdown
    private NotificationDropdown notificationDropdown;

    private List<Notification> notifications;

    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    public NotificationPanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
        // loadNotifications(); // Bỏ - để ComponentListener handle auto-refresh
    }

    private void setupEventListeners() {
        // Button listeners
        sendButton.addActionListener(e -> showSendNotificationDialog());
        markAllReadButton.addActionListener(e -> markAllAsRead());
        refreshButton.addActionListener(e -> refreshData());

        // Add component listener to refresh when panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (isInitialized && !isRefreshing) {
                    refreshData();
                }
            }
        });
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        String[] columnNames = { "", "Tiêu đề", "Người gửi", "Ngày gửi", "Trạng thái" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return String.class; // Icon column
                }
                return super.getColumnClass(columnIndex);
            }
        };

        notificationTable = new JTable(tableModel);
        notificationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notificationTable.setRowHeight(30);
        notificationTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        notificationTable.getColumnModel().getColumn(0).setPreferredWidth(30); // Icon
        notificationTable.getColumnModel().getColumn(0).setMaxWidth(30);
        notificationTable.getColumnModel().getColumn(1).setPreferredWidth(300); // Title
        notificationTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Sender
        notificationTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Date
        notificationTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Status

        // Custom renderer for styling
        notificationTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Get notification for this row
                if (row < notifications.size()) {
                    Notification notif = notifications.get(row);

                    // Unread notifications - bold font
                    if (!notif.isRead()) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        if (!isSelected) {
                            c.setBackground(new Color(255, 255, 200)); // Light yellow
                        }
                    } else {
                        setFont(getFont().deriveFont(Font.PLAIN));
                        if (!isSelected) {
                            c.setBackground(Color.WHITE);
                        }
                    }

                    // Urgent notifications - red text
                    if (notif.getPriority() == Notification.Priority.URGENT) {
                        setForeground(Color.RED);
                    } else if (!isSelected) {
                        setForeground(Color.BLACK);
                    }
                }

                return c;
            }
        });

        // Double click to view details and mark as read
        notificationTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewNotificationDetails();
                }
            }
        });

        // Buttons
        sendButton = new JButton("Gửi thông báo");
        sendButton.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
        sendButton.setVisible(!isReadOnly); // Only visible for Admin/Teacher

        markAllReadButton = new JButton("Đánh dấu tất cả đã đọc");
        markAllReadButton.setIcon(UIManager.getIcon("Tree.leafIcon"));

        refreshButton = new JButton("Làm mới");
        refreshButton.setIcon(UIManager.getIcon("FileView.fileIcon"));

        // Unread count label
        unreadCountLabel = new JLabel("Chưa đọc: 0");
        unreadCountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        unreadCountLabel.setForeground(Color.RED);

        // Notification dropdown button với icon + text, dùng Segoe UI để không vỡ tiếng Việt
        notificationButton = new JButton("🔔 Thông báo");
        notificationButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notificationButton.addActionListener(e -> showNotificationDropdown());

        // Create dropdown
        notificationDropdown = new NotificationDropdown();
        notificationDropdown.setClickListener(new NotificationDropdown.NotificationClickListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                viewNotificationDetails(notification);
            }

            @Override
            public void onMarkAllRead() {
                markAllAsRead();
            }

            @Override
            public void onViewAll() {
                // Switch to notification tab if in MainFrame
                // This will be handled by MainFrame if needed
            }
        });
    }

    private void setupLayout() {
        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        if (!isReadOnly) {
            toolBar.add(sendButton);
            toolBar.addSeparator();
        }

        toolBar.add(markAllReadButton);
        toolBar.addSeparator();
        toolBar.add(refreshButton);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(notificationButton);
        toolBar.addSeparator();
        toolBar.add(unreadCountLabel);

        add(toolBar, BorderLayout.NORTH);

        // Table in scroll pane
        JScrollPane scrollPane = new JScrollPane(notificationTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;
        loadNotifications();
    }

    private void loadNotifications() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Message request = Message.createRequest(Constants.ACTION_GET_NOTIFICATIONS);
                    request.addData(Constants.KEY_USER_ID, currentUser.getUserId());

                    Message response = serverConnection.sendRequest(request);

                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Notification> notificationList = (List<Notification>) response
                                .getData(Constants.KEY_NOTIFICATIONS, List.class);
                        notifications = notificationList != null ? notificationList : new java.util.ArrayList<>();
                        Integer unreadCount = response.getData(Constants.KEY_UNREAD_COUNT, Integer.class);

                        SwingUtilities.invokeLater(() -> {
                            updateTable();
                            if (unreadCount != null) {
                                unreadCountLabel.setText("Chưa đọc: " + unreadCount);
                                // Update dropdown
                                if (notificationDropdown != null) {
                                    notificationDropdown.updateNotifications(notifications);
                                }
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            ToastNotification.showError(NotificationPanel.this,
                                    "Lỗi khi tải thông báo: " + response.getMessage());
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        ToastNotification.showError(NotificationPanel.this,
                                "Lỗi kết nối: " + e.getMessage());
                    });
                } finally {
                    isRefreshing = false;
                }
                return null;
            }
        };
        worker.execute();
    }

    private void updateTable() {
        tableModel.setRowCount(0);

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Notification notif : notifications) {
            String icon = notif.getPriorityIcon();
            String title = notif.getTitle();
            String sender = notif.getSenderName() != null ? notif.getSenderName() : "System";
            String date = notif.getCreatedAt() != null ? sdf.format(notif.getCreatedAt()) : "";
            String status = notif.isRead() ? "Đã đọc" : "Chưa đọc";

            tableModel.addRow(new Object[] { icon, title, sender, date, status });
        }
    }

    private void showNotificationDropdown() {
        if (notificationDropdown != null && notifications != null) {
            notificationDropdown.updateNotifications(notifications);
            notificationDropdown.show(notificationButton, 0, notificationButton.getHeight());
        }
    }

    private void viewNotificationDetails() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= notifications.size()) {
            return;
        }

        Notification notif = notifications.get(selectedRow);
        viewNotificationDetails(notif);
    }

    private void viewNotificationDetails(Notification notif) {

        // Mark as read if unread
        if (!notif.isRead()) {
            markNotificationAsRead(notif.getNotificationId());
        }

        // Show details dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "", true);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        
        Color primaryColor = new Color(44, 62, 80); // Match sidebar color
        Color backgroundColor = new Color(245, 247, 250);
        Color cardColor = Color.WHITE;
        Color borderColor = new Color(220, 224, 230);

        // Custom header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(primaryColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setPreferredSize(new Dimension(0, 60));

        JLabel headerTitleLabel = new JLabel("Chi tiết thông báo");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerTitleLabel.setForeground(Color.WHITE);
        headerPanel.add(headerTitleLabel, BorderLayout.WEST);

        // Close button in header
        JButton headerCloseButton = new JButton("X");
        headerCloseButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerCloseButton.setForeground(Color.WHITE);
        headerCloseButton.setBackground(primaryColor);
        headerCloseButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        headerCloseButton.setFocusPainted(false);
        headerCloseButton.setContentAreaFilled(false);
        headerCloseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerCloseButton.addActionListener(e -> dialog.dispose());
        headerCloseButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                headerCloseButton.setForeground(new Color(255, 200, 200));
                headerCloseButton.setBackground(new Color(220, 53, 69));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                headerCloseButton.setForeground(Color.WHITE);
                headerCloseButton.setBackground(primaryColor);
            }
        });
        headerPanel.add(headerCloseButton, BorderLayout.EAST);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Form card
        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBackground(cardColor);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        // Title
        JLabel titleLabel = new JLabel(notif.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(52, 73, 94));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(titleLabel);
        formCard.add(Box.createVerticalStrut(20));

        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        JLabel senderLabel = createInfoLabel("Người gửi", 
            notif.getSenderName() != null ? notif.getSenderName() : "System");
        infoPanel.add(senderLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        if (notif.getCreatedAt() != null) {
            JLabel dateLabel = createInfoLabel("Ngày gửi", sdf.format(notif.getCreatedAt()));
            infoPanel.add(dateLabel);
            infoPanel.add(Box.createVerticalStrut(10));
        }

        JLabel priorityLabel = createInfoLabel("Mức độ", 
            notif.getPriorityIcon() + " " + notif.getPriority().getDisplayName());
        infoPanel.add(priorityLabel);

        formCard.add(infoPanel);
        formCard.add(Box.createVerticalStrut(20));

        // Content
        JTextArea contentArea = new JTextArea(notif.getContent());
        contentArea.setEditable(false);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        contentArea.setBackground(new Color(248, 249, 250));

        JLabel contentLabel = new JLabel("Nội dung:");
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentLabel.setForeground(new Color(73, 80, 87));
        contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(contentLabel);
        formCard.add(Box.createVerticalStrut(6));

        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(contentArea.getBorder());
        contentScroll.setPreferredSize(new Dimension(0, 150));
        contentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        contentArea.setBorder(null);
        contentScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(contentScroll);

        mainPanel.add(formCard);
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JButton closeButton = createStyledButton("Đóng", new Color(108, 117, 125), false);
        closeButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Viền ngoài cùng cho dialog chi tiết thông báo
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));

        // Close on ESC key
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setSize(600, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel createInfoLabel(String label, String value) {
        JLabel infoLabel = new JLabel("<html><b>" + label + ":</b> " + value + "</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(new Color(73, 80, 87));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return infoLabel;
    }

    private JButton createStyledButton(String text, Color bgColor, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(isPrimary ? 150 : 100, 42));
        
        Color hoverColor = new Color(
            Math.max(0, bgColor.getRed() - 15),
            Math.max(0, bgColor.getGreen() - 15),
            Math.max(0, bgColor.getBlue() - 15)
        );
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void markNotificationAsRead(int notificationId) {
        try {
            Message request = Message.createRequest(Constants.ACTION_MARK_NOTIFICATION_READ);
            request.addData(Constants.KEY_NOTIFICATION_ID, notificationId);

            serverConnection.sendRequest(request);

            // Refresh table
            refreshData();

        } catch (Exception e) {
            // Silent fail
        }
    }

    private void markAllAsRead() {
        int result = JOptionPane.showConfirmDialog(this,
                "Đánh dấu tất cả thông báo là đã đọc?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            try {
                Message request = Message.createRequest(Constants.ACTION_MARK_NOTIFICATION_READ);
                // Don't add notification_id - server will mark all for current user

                Message response = serverConnection.sendRequest(request);

                if (response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Đã đánh dấu tất cả thông báo là đã đọc",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + response.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSendNotificationDialog() {
        NotificationDialog dialog = new NotificationDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                serverConnection,
                currentUser);
        dialog.setVisible(true);

        if (dialog.isSuccess()) {
            refreshData();
        }
    }
}
