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
        sidebar.setBackground(new Color(44, 62, 80)); // Dark blue
        sidebar.setPreferredSize(new Dimension(280, 0));
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
        panel.setMaximumSize(new Dimension(280, 80));

        // Logo với emoji font hỗ trợ tốt hơn
        JLabel logoLabel = new JLabel("🎓 SMS");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 28));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Student Management");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        panel.setMaximumSize(new Dimension(280, 80));

        // Divider
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(52, 73, 94));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));

        // User info panel với icon và text
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.X_AXIS));
        userPanel.setOpaque(false);
        userPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userIcon = new JLabel("👤");
        userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        userIcon.setForeground(Color.WHITE);

        JLabel userName = new JLabel(currentUser != null ? currentUser.getFullName() : "Guest");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userName.setForeground(Color.WHITE);

        userPanel.add(Box.createHorizontalGlue());
        userPanel.add(userIcon);
        userPanel.add(Box.createHorizontalStrut(8));
        userPanel.add(userName);
        userPanel.add(Box.createHorizontalGlue());

        String roleText = currentUser != null ? currentUser.getRole().toString() : "";
        // Convert role to Vietnamese
        if (roleText.equals("ADMIN"))
            roleText = "Quản trị viên";
        else if (roleText.equals("TEACHER"))
            roleText = "Giảng viên";
        else if (roleText.equals("STUDENT"))
            roleText = "Sinh viên";
        JLabel roleLabel = new JLabel(roleText);
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roleLabel.setForeground(new Color(149, 165, 166));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(userPanel);
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
        JPanel button = new JPanel(new BorderLayout(0, 0));
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(8, 20, 8, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Set size để đảm bảo fill full width
        button.setMinimumSize(new Dimension(280, 40));
        button.setPreferredSize(new Dimension(320, 40));
        button.setMaximumSize(new Dimension(320, 40));
        JPanel contentPanel = new JPanel();
        contentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
        contentPanel.setOpaque(false);

        // Icon với font hỗ trợ emoji và fixed width
        JLabel icon = new JLabel(navItem.getIcon(), JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        icon.setForeground(new Color(189, 195, 199));
        icon.setPreferredSize(new Dimension(28, 22)); // Fixed size cho tất cả icons
        icon.setMaximumSize(new Dimension(28, 22));

        // Label văn bản
        JLabel label = new JLabel(navItem.getLabel());
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(189, 195, 199));

        contentPanel.add(Box.createHorizontalStrut(5)); // Spacing
        contentPanel.add(icon);
        contentPanel.add(Box.createHorizontalStrut(8)); // Spacing
        contentPanel.add(label);
        contentPanel.add(Box.createHorizontalGlue());
        button.add(contentPanel, BorderLayout.CENTER); // CENTER để tràn full width

        // Lưu reference để update màu sau
        navItem.setIconLabel(icon); // Lưu icon label
        navItem.setTextLabel(label); // Lưu text label

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
        // Icon và label đã được set ở trên (dòng 213-214)

        return button;
    }

    private void selectNavItem(NavItem navItem) {
        // Deselect previous
        if (selectedNavItem != null) {
            JPanel prevButton = selectedNavItem.getButton();
            JLabel prevIcon = selectedNavItem.getIconLabel();
            JLabel prevText = selectedNavItem.getTextLabel();
            prevButton.setOpaque(false);
            prevButton.setBackground(null);
            if (prevIcon != null)
                prevIcon.setForeground(new Color(189, 195, 199));
            if (prevText != null)
                prevText.setForeground(new Color(189, 195, 199));
            prevButton.repaint();
        }

        // Select new
        selectedNavItem = navItem;
        JPanel button = navItem.getButton();
        JLabel iconLabel = navItem.getIconLabel();
        JLabel textLabel = navItem.getTextLabel();
        button.setOpaque(true);
        button.setBackground(new Color(41, 128, 185)); // Blue
        if (iconLabel != null)
            iconLabel.setForeground(Color.WHITE);
        if (textLabel != null)
            textLabel.setForeground(Color.WHITE);
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
        private JLabel iconLabel; // Label của icon emoji
        private JLabel textLabel; // Label của text
        private boolean hasBadge;
        private JLabel badgeLabel;

        public NavItem(String icon, String label, String panelName) {
            this.icon = icon;
            this.label = label;
            this.panelName = panelName;
            this.hasBadge = false;
        }

        // Getters and Setters
        public String getIcon() {
            return icon;
        }

        public String getLabel() {
            return label;
        }

        public String getPanelName() {
            return panelName;
        }

        public JPanel getButton() {
            return button;
        }

        public void setButton(JPanel button) {
            this.button = button;
        }

        public JLabel getIconLabel() {
            return iconLabel;
        }

        public void setIconLabel(JLabel iconLabel) {
            this.iconLabel = iconLabel;
        }

        public JLabel getTextLabel() {
            return textLabel;
        }

        public void setTextLabel(JLabel textLabel) {
            this.textLabel = textLabel;
        }

        public boolean hasBadge() {
            return hasBadge;
        }

        public void setHasBadge(boolean hasBadge) {
            this.hasBadge = hasBadge;
        }

        public JLabel getBadgeLabel() {
            return badgeLabel;
        }

        public void setBadgeLabel(JLabel badgeLabel) {
            this.badgeLabel = badgeLabel;
        }
    }
}
