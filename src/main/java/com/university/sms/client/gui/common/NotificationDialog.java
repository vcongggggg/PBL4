package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Notification;
import com.university.sms.model.Notification.TargetType;
import com.university.sms.model.Notification.Priority;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;
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
        super(parent, "Gửi thông báo", true);
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;

        initComponents();
        setupLayout();
        setupListeners();

        setSize(600, 500);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        // Title
        titleField = new JTextField(30);
        titleField.setToolTipText("Tiêu đề thông báo (tối đa 200 ký tự)");

        // Content
        contentArea = new JTextArea(10, 30);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setToolTipText("Nội dung thông báo");

        // Target Type
        targetTypeCombo = new JComboBox<>(TargetType.values());
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
        targetIdCombo.setEnabled(false);

        // Priority
        priorityCombo = new JComboBox<>(Priority.values());
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
        expiresAtField = new JTextField(15);
        expiresAtField.setToolTipText("Ngày hết hạn (YYYY-MM-DD HH:MM, để trống = không hết hạn)");

        // Buttons
        sendButton = new JButton("Gửi thông báo");
        sendButton.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));

        cancelButton = new JButton("Hủy");
        cancelButton.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Main panel with form
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Title
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Tiêu đề: *"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(titleField, gbc);
        row++;

        // Target Type
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Gửi đến: *"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(targetTypeCombo, gbc);
        row++;

        // Target ID
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Đối tượng:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(targetIdCombo, gbc);
        row++;

        // Priority
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Mức độ: *"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(priorityCombo, gbc);
        row++;

        // Expires At
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Hết hạn:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(expiresAtField, gbc);
        row++;

        // Content
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(new JLabel("Nội dung: *"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane contentScroll = new JScrollPane(contentArea);
        mainPanel.add(contentScroll, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Info label
        JLabel infoLabel = new JLabel("* = Bắt buộc | Thông báo sẽ được gửi đến tất cả đối tượng được chọn");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        add(infoLabel, BorderLayout.NORTH);
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
