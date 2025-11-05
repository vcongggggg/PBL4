package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Dashboard với card-based navigation
 * Thay thế JTabbedPane truyền thống
 */
public class ModernDashboard extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private IServerConnection serverConnection;
    private User currentUser;
    
    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    
    private List<NavItem> navItems;
    private NavItem selectedNavItem;
    
    // Notification badge
    private JLabel notificationBadge;
    private int unreadCount = 0;
    
    public ModernDashboard(IServerConnection serverConnection, User currentUser) {
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.navItems = new ArrayList<>();
        
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));
        
        // Sidebar
        sidebarPanel = createSidebar();
        add(sidebarPanel, BorderLayout.WEST);
        
        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));  // Dark blue
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Logo/Header
        JPanel headerPanel = createHeaderPanel();
        sidebar.add(headerPanel);
        sidebar.add(Box.createVerticalStrut(30));
        
        // Navigation will be added via addNavItem()
        
        sidebar.add(Box.createVerticalGlue());
        
        // Footer with user info
        JPanel footerPanel = createFooterPanel();
        sidebar.add(footerPanel);
        
        return sidebar;
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 20, 0, 20));
        panel.setMaximumSize(new Dimension(250, 80));
        
        JLabel logoLabel = new JLabel("🎓 SMS");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Student Management");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(logoLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitleLabel);
        
        return panel;
    }
    
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 20, 10, 20));
        panel.setMaximumSize(new Dimension(250, 80));
        
        // Divider
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(52, 73, 94));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));
        
        JLabel userLabel = new JLabel("👤 " + (currentUser != null ? currentUser.getFullName() : "Guest"));
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLabel.setForeground(Color.WHITE);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String roleText = currentUser != null ? currentUser.getRole().toString() : "";
        // Convert role to Vietnamese
        if (roleText.equals("ADMIN")) roleText = "Quản trị viên";
        else if (roleText.equals("TEACHER")) roleText = "Giảng viên";
        else if (roleText.equals("STUDENT")) roleText = "Sinh viên";
        JLabel roleLabel = new JLabel(roleText);
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roleLabel.setForeground(new Color(149, 165, 166));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(roleLabel);
        
        return panel;
    }
    
    /**
     * Thêm navigation item
     */
    public void addNavItem(String icon, String label, String panelName, JPanel panel) {
        NavItem navItem = new NavItem(icon, label, panelName);
        navItems.add(navItem);
        
        // Add panel to CardLayout
        contentPanel.add(panel, panelName);
        
        // Add nav button to sidebar
        JPanel navButton = createNavButton(navItem);
        sidebarPanel.add(navButton, sidebarPanel.getComponentCount() - 2); // Before footer
        
        // Select first item by default
        if (navItems.size() == 1) {
            selectNavItem(navItem);
        }
    }
    
    /**
     * Thêm navigation item với badge (cho notifications)
     */
    public void addNavItemWithBadge(String icon, String label, String panelName, JPanel panel) {
        NavItem navItem = new NavItem(icon, label, panelName);
        navItem.setHasBadge(true);
        navItems.add(navItem);
        
        // Add panel to CardLayout
        contentPanel.add(panel, panelName);
        
        // Add nav button to sidebar
        JPanel navButton = createNavButton(navItem);
        sidebarPanel.add(navButton, sidebarPanel.getComponentCount() - 2);
        
        // Store badge reference
        if (notificationBadge == null) {
            notificationBadge = navItem.getBadgeLabel();
        }
    }
    
    private JPanel createNavButton(NavItem navItem) {
        JPanel button = new JPanel(new BorderLayout(10, 0));
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setMaximumSize(new Dimension(250, 50));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Icon + Label
        JLabel iconLabel = new JLabel(navItem.getIcon() + "  " + navItem.getLabel());
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        iconLabel.setForeground(new Color(189, 195, 199));
        button.add(iconLabel, BorderLayout.WEST);
        
        // Badge (if applicable)
        if (navItem.hasBadge()) {
            JLabel badge = new JLabel("0");
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(true);
            badge.setBackground(Color.decode("#e74c3c"));
            badge.setBorder(new EmptyBorder(2, 6, 2, 6));
            badge.setVisible(false); // Hidden initially
            button.add(badge, BorderLayout.EAST);
            navItem.setBadgeLabel(badge);
        }
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (selectedNavItem != navItem) {
                    button.setOpaque(true);
                    button.setBackground(new Color(52, 73, 94));
                    button.repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedNavItem != navItem) {
                    button.setOpaque(false);
                    button.repaint();
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                selectNavItem(navItem);
            }
        });
        
        navItem.setButton(button);
        navItem.setIconLabel(iconLabel);
        
        return button;
    }
    
    private void selectNavItem(NavItem navItem) {
        // Deselect previous
        if (selectedNavItem != null) {
            JPanel prevButton = selectedNavItem.getButton();
            JLabel prevIcon = selectedNavItem.getIconLabel();
            prevButton.setOpaque(false);
            prevButton.setBackground(null);
            prevIcon.setForeground(new Color(189, 195, 199));
            prevButton.repaint();
        }
        
        // Select new
        selectedNavItem = navItem;
        JPanel button = navItem.getButton();
        JLabel iconLabel = navItem.getIconLabel();
        button.setOpaque(true);
        button.setBackground(new Color(41, 128, 185)); // Blue
        iconLabel.setForeground(Color.WHITE);
        button.repaint();
        
        // Show panel
        cardLayout.show(contentPanel, navItem.getPanelName());
    }
    
    /**
     * Cập nhật badge cho notifications
     */
    public void updateNotificationBadge(int count) {
        this.unreadCount = count;
        if (notificationBadge != null) {
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(count > 0);
        }
    }
    
    /**
     * Show specific panel
     */
    public void showPanel(String panelName) {
        for (NavItem item : navItems) {
            if (item.getPanelName().equals(panelName)) {
                selectNavItem(item);
                break;
            }
        }
    }
    
    private void setupLayout() {
        // Layout is already set up
    }
    
    /**
     * Navigation Item Model
     */
    private static class NavItem {
        private String icon;
        private String label;
        private String panelName;
        private JPanel button;
        private JLabel iconLabel;
        private boolean hasBadge;
        private JLabel badgeLabel;
        
        public NavItem(String icon, String label, String panelName) {
            this.icon = icon;
            this.label = label;
            this.panelName = panelName;
            this.hasBadge = false;
        }
        
        // Getters and Setters
        public String getIcon() { return icon; }
        public String getLabel() { return label; }
        public String getPanelName() { return panelName; }
        public JPanel getButton() { return button; }
        public void setButton(JPanel button) { this.button = button; }
        public JLabel getIconLabel() { return iconLabel; }
        public void setIconLabel(JLabel iconLabel) { this.iconLabel = iconLabel; }
        public boolean hasBadge() { return hasBadge; }
        public void setHasBadge(boolean hasBadge) { this.hasBadge = hasBadge; }
        public JLabel getBadgeLabel() { return badgeLabel; }
        public void setBadgeLabel(JLabel badgeLabel) { this.badgeLabel = badgeLabel; }
    }
}

