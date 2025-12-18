package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog để xem và chỉnh sửa thông tin sinh viên
 */
public class StudentDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private Student student;
    private IServerConnection serverConnection;
    private User currentUser;
    private boolean isReadOnly;
    private boolean dataChanged = false;

    // Form fields
    private JTextField studentCodeField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField facultyField;
    private JTextField classField;
    private JTextField gpaField;
    private JTextField creditsField;
    private JTextField admissionYearField;
    private JTextField citizenIdField;
    private JTextField emergencyContactField;
    private JTextField emergencyPhoneField;
    private JComboBox<String> statusComboBox;
    private JComboBox<String> genderComboBox;

    private JButton saveButton;
    private JButton closeButton;

    public StudentDetailDialog(Frame parent, Student student, IServerConnection serverConnection,
            User currentUser, boolean isReadOnly) {
        super(parent, "", true);
        this.student = student;
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.isReadOnly = isReadOnly;

        setUndecorated(true);
        initializeComponents();
        setupLayout();
        loadStudentData();
        setupEventListeners();

        setSize(750, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        // Create styled form fields
        studentCodeField = createStyledTextField(false);
        fullNameField = createStyledTextField(true);
        emailField = createStyledTextField(true);
        phoneField = createStyledTextField(true);
        facultyField = createStyledTextField(false);
        classField = createStyledTextField(false);
        gpaField = createStyledTextField(false);
        creditsField = createStyledTextField(false);
        admissionYearField = createStyledTextField(true);
        citizenIdField = createStyledTextField(true);
        emergencyContactField = createStyledTextField(true);
        emergencyPhoneField = createStyledTextField(true);

        // Status combo box - Vietnamese labels
        statusComboBox = createStyledComboBox(new String[] {
                "Đang học", "Tạm đình chỉ", "Đã tốt nghiệp", "Thôi học"
        });

        // Gender combo box
        genderComboBox = createStyledComboBox(new String[] {
                "MALE", "FEMALE", "OTHER"
        });

        // Buttons
        saveButton = createStyledButton("Lưu thay đổi", new Color(41, 128, 185), true);
        closeButton = createStyledButton("Đóng", new Color(108, 117, 125), false);

        // Set read-only mode based on user role
        if (isReadOnly) {
            setFieldsEditable(false);
            saveButton.setEnabled(false);
        } else if (currentUser.getRole() == User.UserRole.STUDENT) {
            // Students can only edit: email, phone, emergency contacts
            studentCodeField.setEditable(false);
            fullNameField.setEditable(false);
            facultyField.setEditable(false);
            classField.setEditable(false);
            gpaField.setEditable(false);
            creditsField.setEditable(false);
            statusComboBox.setEnabled(false);
            admissionYearField.setEditable(false);
            citizenIdField.setEditable(false);
            genderComboBox.setEnabled(false);
            // Only allow editing: email, phone, emergencyContact, emergencyPhone
        } else if (currentUser.getRole() == User.UserRole.TEACHER) {
            // Teachers can only edit some fields
            studentCodeField.setEditable(false);
            facultyField.setEditable(false);
            classField.setEditable(false);
            admissionYearField.setEditable(false);
        }
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

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
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

        JLabel titleLabel = new JLabel("Thông tin Sinh viên - " + student.getStudentCode());
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

        // Section 1: Thông tin cơ bản
        JPanel basicInfoCard = createSectionCard("Thông tin Cơ bản", cardColor, borderColor);
        basicInfoCard.add(createFieldPanel("Mã sinh viên", studentCodeField, false));
        basicInfoCard.add(Box.createVerticalStrut(15));
        basicInfoCard.add(createFieldPanel("Họ và tên", fullNameField, true));
        basicInfoCard.add(Box.createVerticalStrut(15));
        basicInfoCard.add(createFieldPanel("Email", emailField, true));
        basicInfoCard.add(Box.createVerticalStrut(15));
        basicInfoCard.add(createFieldPanel("Số điện thoại", phoneField, false));
        basicInfoCard.add(Box.createVerticalStrut(15));
        basicInfoCard.add(createFieldPanel("Giới tính", genderComboBox, false));
        basicInfoCard.add(Box.createVerticalStrut(15));
        basicInfoCard.add(createFieldPanel("CCCD/CMND", citizenIdField, false));
        mainPanel.add(basicInfoCard);
        mainPanel.add(Box.createVerticalStrut(15));

        // Section 2: Thông tin Học tập
        JPanel academicInfoCard = createSectionCard("Thông tin Học tập", cardColor, borderColor);
        academicInfoCard.add(createFieldPanel("Khoa", facultyField, false));
        academicInfoCard.add(Box.createVerticalStrut(15));
        academicInfoCard.add(createFieldPanel("Lớp", classField, false));
        academicInfoCard.add(Box.createVerticalStrut(15));
        academicInfoCard.add(createFieldPanel("Năm nhập học", admissionYearField, false));
        academicInfoCard.add(Box.createVerticalStrut(15));
        academicInfoCard.add(createFieldPanel("GPA", gpaField, false));
        academicInfoCard.add(Box.createVerticalStrut(15));
        academicInfoCard.add(createFieldPanel("Tổng tín chỉ", creditsField, false));
        academicInfoCard.add(Box.createVerticalStrut(15));
        academicInfoCard.add(createFieldPanel("Trạng thái", statusComboBox, false));
        mainPanel.add(academicInfoCard);
        mainPanel.add(Box.createVerticalStrut(15));

        // Section 3: Thông tin Liên hệ Khẩn cấp
        JPanel emergencyInfoCard = createSectionCard("Thông tin Liên hệ Khẩn cấp", cardColor, borderColor);
        emergencyInfoCard.add(createFieldPanel("Người liên hệ khẩn cấp", emergencyContactField, false));
        emergencyInfoCard.add(Box.createVerticalStrut(15));
        emergencyInfoCard.add(createFieldPanel("SĐT khẩn cấp", emergencyPhoneField, false));
        mainPanel.add(emergencyInfoCard);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        buttonPanel.add(this.closeButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Viền ngoài cùng cho dialog chi tiết sinh viên
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));
        }
    }

    private JPanel createSectionCard(String title, Color bgColor, Color borderColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JLabel sectionTitle = new JLabel(title);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sectionTitle.setForeground(new Color(52, 73, 94));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sectionTitle);
        card.add(Box.createVerticalStrut(15));

        return card;
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

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        panel.add(component);

        return panel;
    }


    private void loadStudentData() {
        studentCodeField.setText(student.getStudentCode());
        fullNameField.setText(student.getFullName());
        emailField.setText(student.getEmail());
        phoneField.setText(student.getPhone() != null ? student.getPhone() : "");
        facultyField.setText(student.getFacultyName() != null ? student.getFacultyName() : "N/A");
        classField.setText(student.getClassName() != null ? student.getClassName() : "N/A");
        // Display GPA - để trống nếu không có hoặc GPA = 0
        if (student.getGpa() != null && student.getGpa().compareTo(java.math.BigDecimal.ZERO) != 0) {
            gpaField.setText(student.getGpa().toString());
        } else {
            gpaField.setText(""); // Để trống nếu không có GPA hoặc GPA = 0
        }
        creditsField.setText(String.valueOf(student.getTotalCredits()));
        admissionYearField.setText(String.valueOf(student.getAdmissionYear()));
        citizenIdField.setText(student.getCitizenId() != null ? student.getCitizenId() : "");
        emergencyContactField.setText(student.getEmergencyContact() != null ? student.getEmergencyContact() : "");
        emergencyPhoneField.setText(student.getEmergencyPhone() != null ? student.getEmergencyPhone() : "");

        if (student.getStudentStatus() != null) {
            statusComboBox.setSelectedItem(getStatusDisplay(student.getStudentStatus()));
        }

        if (student.getGender() != null) {
            genderComboBox.setSelectedItem(student.getGender().toString());
        }
    }

    private void setupEventListeners() {
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveStudentData();
            }
        });

        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

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

    private void saveStudentData() {
        // Validate required fields
        if (fullNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Họ và tên không được để trống!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Email không được để trống!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tạo snapshot để tránh làm bẩn dữ liệu gốc khi lưu thất bại
        Student updatedStudent = createStudentSnapshot();

        updatedStudent.setFullName(fullNameField.getText().trim());
        updatedStudent.setEmail(emailField.getText().trim());
        updatedStudent.setPhone(phoneField.getText().trim());
        updatedStudent.setCitizenId(citizenIdField.getText().trim());
        updatedStudent.setEmergencyContact(emergencyContactField.getText().trim());
        updatedStudent.setEmergencyPhone(emergencyPhoneField.getText().trim());

        String statusStr = (String) statusComboBox.getSelectedItem();
        if (statusStr != null) {
            updatedStudent.setStudentStatus(getStatusFromDisplay(statusStr));
        }

        String genderStr = (String) genderComboBox.getSelectedItem();
        if (genderStr != null) {
            updatedStudent.setGender(Student.Gender.valueOf(genderStr));
        }

        try {
            updatedStudent.setAdmissionYear(Integer.parseInt(admissionYearField.getText().trim()));
        } catch (NumberFormatException ex) {
            // Giữ giá trị cũ trong snapshot
        }

        // Send update to server
        saveButton.setEnabled(false);
        final Student studentForUpdate = updatedStudent;

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.updateStudent(studentForUpdate);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        applyUpdatedStudentData(studentForUpdate);
                        dataChanged = true;
                        JOptionPane.showMessageDialog(StudentDetailDialog.this,
                                "Cập nhật thông tin sinh viên thành công!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(StudentDetailDialog.this,
                                "Cập nhật thất bại: " + response.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentDetailDialog.this,
                            "Lỗi khi cập nhật: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setFieldsEditable(boolean editable) {
        fullNameField.setEditable(editable);
        emailField.setEditable(editable);
        phoneField.setEditable(editable);
        admissionYearField.setEditable(editable);
        citizenIdField.setEditable(editable);
        emergencyContactField.setEditable(editable);
        emergencyPhoneField.setEditable(editable);
        statusComboBox.setEnabled(editable);
        genderComboBox.setEnabled(editable);
    }

    public boolean isDataChanged() {
        return dataChanged;
    }

    private Student createStudentSnapshot() {
        Student snapshot = new Student();
        snapshot.setStudentId(student.getStudentId());
        snapshot.setStudentCode(student.getStudentCode());
        snapshot.setUsername(student.getUsername());
        snapshot.setClassCode(student.getClassCode());
        snapshot.setFacultyCode(student.getFacultyCode());
        snapshot.setAdmissionYear(student.getAdmissionYear());
        snapshot.setStudentStatus(student.getStudentStatus());
        snapshot.setGpa(student.getGpa());
        snapshot.setTotalCredits(student.getTotalCredits());
        snapshot.setBirthDate(student.getBirthDate());
        snapshot.setGender(student.getGender());
        snapshot.setCitizenId(student.getCitizenId());
        snapshot.setEmergencyContact(student.getEmergencyContact());
        snapshot.setEmergencyPhone(student.getEmergencyPhone());
        snapshot.setFullName(student.getFullName());
        snapshot.setEmail(student.getEmail());
        snapshot.setPhone(student.getPhone());
        snapshot.setAddress(student.getAddress());
        snapshot.setFacultyName(student.getFacultyName());
        snapshot.setClassName(student.getClassName());
        snapshot.setActive(student.isActive());
        snapshot.setCreatedAt(student.getCreatedAt());
        snapshot.setUpdatedAt(student.getUpdatedAt());
        return snapshot;
    }

    private void applyUpdatedStudentData(Student updatedStudent) {
        student.setFullName(updatedStudent.getFullName());
        student.setEmail(updatedStudent.getEmail());
        student.setPhone(updatedStudent.getPhone());
        student.setCitizenId(updatedStudent.getCitizenId());
        student.setEmergencyContact(updatedStudent.getEmergencyContact());
        student.setEmergencyPhone(updatedStudent.getEmergencyPhone());
        student.setAdmissionYear(updatedStudent.getAdmissionYear());
        student.setStudentStatus(updatedStudent.getStudentStatus());
        student.setGender(updatedStudent.getGender());
    }

    // Helper methods for status display conversion
    private String getStatusDisplay(Student.StudentStatus status) {
        if (status == null) {
            return "Đang học";
        }
        switch (status) {
            case ACTIVE:
                return "Đang học";
            case SUSPENDED:
                return "Tạm đình chỉ";
            case GRADUATED:
                return "Đã tốt nghiệp";
            case DROPPED:
                return "Thôi học";
            default:
                return "Đang học";
        }
    }

    private Student.StudentStatus getStatusFromDisplay(String displayText) {
        if (displayText == null) {
            return Student.StudentStatus.ACTIVE;
        }
        switch (displayText) {
            case "Đang học":
                return Student.StudentStatus.ACTIVE;
            case "Tạm đình chỉ":
                return Student.StudentStatus.SUSPENDED;
            case "Đã tốt nghiệp":
                return Student.StudentStatus.GRADUATED;
            case "Thôi học":
                return Student.StudentStatus.DROPPED;
            default:
                return Student.StudentStatus.ACTIVE;
        }
    }
}
