package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Notification;
import com.university.sms.model.Notification.TargetType;
import com.university.sms.model.Notification.Priority;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Dialog gửi thông báo
 * Dùng cho Admin/Teacher để gửi thông báo đến sinh viên
 */
public class NotificationDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private User currentUser;
    private boolean success = false;

    // Components
    private JTextField titleField;
    private JTextArea contentArea;
    private JComboBox<TargetType> targetTypeCombo;
    private JComboBox<Object> targetIdCombo;
    private JComboBox<Priority> priorityCombo;
    private JTextField expiresAtField;
    private JButton sendButton;
    private JButton cancelButton;

    public NotificationDialog(Frame parent, IServerConnection serverConnection, User currentUser) {
        super(parent, "", true);
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;

        setUndecorated(true);
        initComponents();
        setupLayout();
        setupListeners();

        setSize(650, 600);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        // Title
        titleField = createStyledTextField(true);
        titleField.setToolTipText("Tiêu đề thông báo (tối đa 200 ký tự)");

        // Content
        contentArea = new JTextArea(10, 30);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setToolTipText("Nội dung thông báo");
        contentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // Target Type
        targetTypeCombo = createStyledComboBox(TargetType.values());
        targetTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TargetType) {
                    setText(((TargetType) value).getDisplayName());
                }
                return this;
            }
        });

        // Target ID
        targetIdCombo = new JComboBox<>();
        targetIdCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        targetIdCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        targetIdCombo.setPreferredSize(new Dimension(0, 40));
        targetIdCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        targetIdCombo.setBackground(Color.WHITE);
        targetIdCombo.setEnabled(false);

        // Priority
        priorityCombo = createStyledComboBox(Priority.values());
        priorityCombo.setSelectedItem(Priority.MEDIUM);
        priorityCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Priority) {
                    Priority p = (Priority) value;
                    String icon = "";
                    switch (p) {
                        case URGENT:
                            icon = "⚠️ ";
                            break;
                        case HIGH:
                            icon = "🔴 ";
                            break;
                        case MEDIUM:
                            icon = "🟡 ";
                            break;
                        case LOW:
                            icon = "🔵 ";
                            break;
                    }
                    setText(icon + p.getDisplayName());
                }
                return this;
            }
        });

        // Expires At
        expiresAtField = createStyledTextField(false);
        expiresAtField.setToolTipText("Ngày hết hạn (YYYY-MM-DD HH:MM, để trống = không hết hạn)");

        // Buttons
        sendButton = createStyledButton("Gửi thông báo", new Color(41, 128, 185), true);
        cancelButton = createStyledButton("Hủy", new Color(108, 117, 125), false);
    }

    private JTextField createStyledTextField(boolean editable) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        field.setPreferredSize(new Dimension(0, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setEditable(editable);
        if (!editable) {
            field.setBackground(new Color(248, 249, 250));
        }
        return field;
    }

    private <T> JComboBox<T> createStyledComboBox(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        // Dùng Segoe UI cho text (emoji sẽ fallback, tránh vỡ tiếng Việt)
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        combo.setPreferredSize(new Dimension(0, 40));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        combo.setBackground(Color.WHITE);
        return combo;
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

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        Color primaryColor = new Color(44, 62, 80); // Match sidebar color
        Color backgroundColor = new Color(245, 247, 250);
        Color cardColor = Color.WHITE;
        Color borderColor = new Color(220, 224, 230);

        // Custom header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(primaryColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setPreferredSize(new Dimension(0, 60));

        JLabel titleLabel = new JLabel("Gửi thông báo");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Close button in header
        JButton headerCloseButton = new JButton("X");
        headerCloseButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerCloseButton.setForeground(Color.WHITE);
        headerCloseButton.setBackground(primaryColor);
        headerCloseButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        headerCloseButton.setFocusPainted(false);
        headerCloseButton.setContentAreaFilled(false);
        headerCloseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerCloseButton.addActionListener(e -> dispose());
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
        add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info label
        JLabel infoLabel = new JLabel("* = Bắt buộc | Thông báo sẽ được gửi đến tất cả đối tượng được chọn");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(108, 117, 125));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(infoLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Form card
        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBackground(cardColor);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        // Add fields to form
        formCard.add(createFieldPanel("Tiêu đề", titleField, true));
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(createFieldPanel("Gửi đến", targetTypeCombo, true));
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(createFieldPanel("Đối tượng", targetIdCombo, false));
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(createFieldPanel("Mức độ", priorityCombo, true));
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(createFieldPanel("Hết hạn", expiresAtField, false));
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(createFieldPanel("Nội dung", contentArea, true));

        mainPanel.add(formCard);
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        buttonPanel.add(cancelButton);
        buttonPanel.add(sendButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Viền ngoài cùng cho dialog gửi thông báo
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));
        }

        // Close on ESC key
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private JPanel createFieldPanel(String label, JComponent component, boolean required) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComponent = new JLabel(label + (required ? " *" : ""));
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComponent.setForeground(new Color(73, 80, 87));
        labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelComponent);
        panel.add(Box.createVerticalStrut(6));

        if (component instanceof JTextArea) {
            JScrollPane scrollPane = new JScrollPane(component);
            scrollPane.setBorder(component.getBorder());
            scrollPane.setPreferredSize(new Dimension(0, 120));
            scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            component.setBorder(null);
            scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(scrollPane);
        } else {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
            panel.add(component);
        }

        return panel;
    }

    private void setupListeners() {
        // Target Type change listener
        targetTypeCombo.addActionListener(e -> {
            TargetType selectedType = (TargetType) targetTypeCombo.getSelectedItem();
            updateTargetIdCombo(selectedType);
        });

        sendButton.addActionListener(e -> sendNotification());
        cancelButton.addActionListener(e -> dispose());

        // Enter key to send
        getRootPane().setDefaultButton(sendButton);
    }

    private void updateTargetIdCombo(TargetType targetType) {
        targetIdCombo.removeAllItems();

        if (targetType == TargetType.ALL) {
            targetIdCombo.setEnabled(false);
            return;
        }

        targetIdCombo.setEnabled(true);

        try {
            switch (targetType) {
                case FACULTY:
                    loadFaculties();
                    break;
                case CLASS:
                    loadClasses();
                    break;
                case STUDENT:
                    loadStudents();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải danh sách: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFaculties() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
            Message response = serverConnection.sendRequest(request);

            if (response.isSuccess()) {
                List<?> faculties = response.getData(Constants.KEY_FACULTIES, List.class);
                if (faculties != null) {
                    for (Object obj : faculties) {
                        targetIdCombo.addItem(obj);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void loadClasses() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_ALL_CLASSES);
            Message response = serverConnection.sendRequest(request);

            if (response.isSuccess()) {
                List<?> classes = response.getData(Constants.KEY_CLASSES, List.class);
                if (classes != null) {
                    for (Object obj : classes) {
                        targetIdCombo.addItem(obj);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void loadStudents() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
            Message response = serverConnection.sendRequest(request);

            if (response.isSuccess()) {
                List<?> students = response.getData(Constants.KEY_STUDENTS, List.class);
                if (students != null) {
                    for (Object obj : students) {
                        targetIdCombo.addItem(obj);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void sendNotification() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Build notification object
            Notification notification = new Notification();
            notification.setTitle(titleField.getText().trim());
            notification.setContent(contentArea.getText().trim());
            notification.setSenderUsername(currentUser.getUsername());
            notification.setTargetType((TargetType) targetTypeCombo.getSelectedItem());
            notification.setPriority((Priority) priorityCombo.getSelectedItem());

            // Set target code if applicable
            if (notification.getTargetType() != TargetType.ALL) {
                Object selected = targetIdCombo.getSelectedItem();
                if (selected != null) {
                    // Extract code from the selected object
                    String targetCode = null;
                    if (selected instanceof com.university.sms.model.Faculty) {
                        targetCode = ((com.university.sms.model.Faculty) selected).getFacultyCode();
                    } else if (selected instanceof com.university.sms.model.Class) {
                        targetCode = ((com.university.sms.model.Class) selected).getClassCode();
                    } else if (selected instanceof com.university.sms.model.Student) {
                        targetCode = ((com.university.sms.model.Student) selected).getStudentCode();
                    }
                    notification.setTargetCode(targetCode);
                }
            }

            // Parse expires at if provided
            String expiresStr = expiresAtField.getText().trim();
            if (!expiresStr.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                java.util.Date utilDate = sdf.parse(expiresStr);
                notification.setExpiresAt(new Timestamp(utilDate.getTime()));
            }

            // Send to server
            Message request = Message.createRequest(Constants.ACTION_SEND_NOTIFICATION);
            request.addData(Constants.KEY_NOTIFICATION, notification);

            Message response = serverConnection.sendRequest(request);

            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        "Gửi thông báo thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                success = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + response.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi gửi thông báo: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInput() {
        // Title
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tiêu đề không được để trống!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return false;
        }
        if (title.length() > 200) {
            JOptionPane.showMessageDialog(this,
                    "Tiêu đề không được vượt quá 200 ký tự!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return false;
        }

        // Content
        if (contentArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nội dung không được để trống!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            contentArea.requestFocus();
            return false;
        }

        // Target Code (if not ALL)
        TargetType targetType = (TargetType) targetTypeCombo.getSelectedItem();
        if (targetType != TargetType.ALL && targetIdCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn đối tượng nhận thông báo!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            targetIdCombo.requestFocus();
            return false;
        }

        // Expires At (if provided)
        String expiresStr = expiresAtField.getText().trim();
        if (!expiresStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                sdf.setLenient(false);
                sdf.parse(expiresStr);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Ngày hết hạn không hợp lệ! Vui lòng nhập theo định dạng YYYY-MM-DD HH:MM",
                        "Lỗi nhập liệu",
                        JOptionPane.WARNING_MESSAGE);
                expiresAtField.requestFocus();
                return false;
            }
        }

        return true;
    }

    public boolean isSuccess() {
        return success;
    }
}
