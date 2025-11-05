package com.university.sms.client.gui.common;

import com.university.sms.model.Notification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dropdown menu hiển thị notifications
 * Modern UI thay thế tab-based notification panel
 */
public class NotificationDropdown extends JPopupMenu {
    private static final long serialVersionUID = 1L;
    
    private JPanel contentPanel;
    private JLabel headerLabel;
    private List<Notification> notifications;
    private NotificationClickListener clickListener;
    
    public interface NotificationClickListener {
        void onNotificationClick(Notification notification);
        void onMarkAllRead();
        void onViewAll();
    }
    
    public NotificationDropdown() {
        this.notifications = new ArrayList<>();
        initializeComponents();
    }
    
    private void initializeComponents() {
        setPreferredSize(new Dimension(380, 450));
        setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        
        // Header
        JPanel header = createHeader();
        contentPanel.add(header);
        
        // Scrollable notification list
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(380, 400));
        
        add(scrollPane);
        
        // Footer
        JPanel footer = createFooter();
        add(footer);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(41, 128, 185));
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        headerLabel = new JLabel("🔔 Thông Báo (0)");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerLabel.setForeground(Color.WHITE);
        header.add(headerLabel, BorderLayout.WEST);
        
        JButton markAllButton = new JButton("✓ Đọc hết");
        markAllButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        markAllButton.setForeground(Color.WHITE);
        markAllButton.setContentAreaFilled(false);
        markAllButton.setBorderPainted(false);
        markAllButton.setFocusPainted(false);
        markAllButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markAllButton.addActionListener(e -> {
            if (clickListener != null) {
                clickListener.onMarkAllRead();
            }
        });
        header.add(markAllButton, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(236, 240, 241));
        footer.setPreferredSize(new Dimension(380, 45));
        
        JButton viewAllButton = new JButton("Xem tất cả →");
        viewAllButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        viewAllButton.setForeground(new Color(41, 128, 185));
        viewAllButton.setContentAreaFilled(false);
        viewAllButton.setBorderPainted(false);
        viewAllButton.setFocusPainted(false);
        viewAllButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAllButton.addActionListener(e -> {
            if (clickListener != null) {
                clickListener.onViewAll();
            }
            setVisible(false);
        });
        footer.add(viewAllButton);
        
        return footer;
    }
    
    /**
     * Cập nhật danh sách thông báo
     */
    public void updateNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        
        // Update header
        int unreadCount = (int) this.notifications.stream().filter(n -> !n.isRead()).count();
        headerLabel.setText(String.format("🔔 Thông Báo (%d chưa đọc)", unreadCount));
        
        // Clear old items (keep header)
        Component header = contentPanel.getComponent(0);
        contentPanel.removeAll();
        contentPanel.add(header);
        
        // Add notifications
        if (this.notifications.isEmpty()) {
            contentPanel.add(createEmptyState());
        } else {
            // Show max 5 recent notifications
            int count = Math.min(5, this.notifications.size());
            for (int i = 0; i < count; i++) {
                Notification notification = this.notifications.get(i);
                JPanel notifItem = createNotificationItem(notification);
                contentPanel.add(notifItem);
            }
        }
        
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    private JPanel createEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(50, 20, 50, 20));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        JLabel iconLabel = new JLabel("📭");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel textLabel = new JLabel("Không có thông báo mới");
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textLabel.setForeground(Color.GRAY);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(textLabel);
        
        return panel;
    }
    
    private JPanel createNotificationItem(Notification notification) {
        JPanel item = new JPanel(new BorderLayout(10, 5));
        item.setBackground(notification.isRead() ? Color.WHITE : new Color(255, 251, 230));
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
            new EmptyBorder(12, 15, 12, 15)
        ));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Priority icon
        String priorityIcon = getPriorityIcon(notification.getPriority());
        JLabel iconLabel = new JLabel(priorityIcon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        item.add(iconLabel, BorderLayout.WEST);
        
        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Title
        JLabel titleLabel = new JLabel(notification.getTitle());
        titleLabel.setFont(new Font("Segoe UI", notification.isRead() ? Font.PLAIN : Font.BOLD, 13));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Content preview
        String contentPreview = notification.getContent();
        if (contentPreview.length() > 80) {
            contentPreview = contentPreview.substring(0, 80) + "...";
        }
        JLabel contentLabel = new JLabel(contentPreview);
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        contentLabel.setForeground(Color.GRAY);
        contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Time
        JLabel timeLabel = new JLabel(getTimeAgo(notification.getCreatedAt()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(Color.LIGHT_GRAY);
        timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(contentLabel);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(timeLabel);
        
        item.add(contentPanel, BorderLayout.CENTER);
        
        // Unread indicator
        if (!notification.isRead()) {
            JLabel unreadDot = new JLabel("●");
            unreadDot.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            unreadDot.setForeground(Color.decode("#e74c3c"));
            item.add(unreadDot, BorderLayout.EAST);
        }
        
        // Click listener
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickListener != null) {
                    clickListener.onNotificationClick(notification);
                }
                setVisible(false);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(245, 245, 245));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(notification.isRead() ? Color.WHITE : new Color(255, 251, 230));
            }
        });
        
        return item;
    }
    
    private String getPriorityIcon(Notification.Priority priority) {
        if (priority == null) return "📢";
        switch (priority) {
            case URGENT: return "⚠️";
            case HIGH: return "🔴";
            case MEDIUM: return "🟡";
            case LOW: return "🔵";
            default: return "📢";
        }
    }
    
    private String getTimeAgo(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        
        long diff = System.currentTimeMillis() - timestamp.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " ngày trước";
        if (hours > 0) return hours + " giờ trước";
        if (minutes > 0) return minutes + " phút trước";
        return "Vừa xong";
    }
    
    public void setClickListener(NotificationClickListener listener) {
        this.clickListener = listener;
    }
}

