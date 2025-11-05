package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Notification;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

    public NotificationPanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        loadNotifications();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        String[] columnNames = {"", "Tiêu đề", "Người gửi", "Ngày gửi", "Trạng thái"};
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
        notificationTable.getColumnModel().getColumn(0).setPreferredWidth(30);  // Icon
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
        
        // Notification dropdown button with badge
        notificationButton = new JButton("🔔 Thông báo");
        notificationButton.setFont(new Font("Arial", Font.PLAIN, 13));
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

        // Setup listeners
        sendButton.addActionListener(e -> showSendNotificationDialog());
        markAllReadButton.addActionListener(e -> markAllAsRead());
        refreshButton.addActionListener(e -> refreshData());
    }

    public void refreshData() {
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
                        List<Notification> notificationList = (List<Notification>) response.getData(Constants.KEY_NOTIFICATIONS, List.class);
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
            
            tableModel.addRow(new Object[]{icon, title, sender, date, status});
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
                "Chi tiết thông báo", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel titleLabel = new JLabel(notif.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        infoPanel.add(new JLabel("Người gửi: " + 
                (notif.getSenderName() != null ? notif.getSenderName() : "System")));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        if (notif.getCreatedAt() != null) {
            infoPanel.add(new JLabel("Ngày gửi: " + sdf.format(notif.getCreatedAt())));
        }
        
        infoPanel.add(new JLabel("Mức độ: " + notif.getPriorityIcon() + " " + 
                notif.getPriority().getDisplayName()));
        
        contentPanel.add(infoPanel, BorderLayout.CENTER);

        // Content
        JTextArea contentArea = new JTextArea(notif.getContent());
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Nội dung:"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        contentPanel.add(scrollPane, BorderLayout.SOUTH);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Close button
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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

