package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;

import javax.swing.*;
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
        super(parent, "Thông tin sinh viên - " + student.getStudentCode(), true);
        this.student = student;
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        loadStudentData();
        setupEventListeners();

        setSize(700, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        // Create form fields
        studentCodeField = new JTextField(20);
        studentCodeField.setEditable(false); // Student code should not be editable

        fullNameField = new JTextField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);
        facultyField = new JTextField(20);
        facultyField.setEditable(false); // Read-only display
        classField = new JTextField(20);
        classField.setEditable(false); // Read-only display
        gpaField = new JTextField(20);
        gpaField.setEditable(false); // GPA is calculated
        creditsField = new JTextField(20);
        creditsField.setEditable(false); // Credits are calculated
        admissionYearField = new JTextField(20);
        citizenIdField = new JTextField(20);
        emergencyContactField = new JTextField(20);
        emergencyPhoneField = new JTextField(20);

        // Status combo box
        statusComboBox = new JComboBox<>(new String[] {
                "ACTIVE", "SUSPENDED", "GRADUATED", "DROPPED"
        });

        // Gender combo box
        genderComboBox = new JComboBox<>(new String[] {
                "MALE", "FEMALE", "OTHER"
        });

        // Buttons
        saveButton = new JButton("Lưu thay đổi");
        closeButton = new JButton("Đóng");

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

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Main panel with form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Student Code
        addFormField(formPanel, gbc, row++, "Mã sinh viên:", studentCodeField);

        // Full Name
        addFormField(formPanel, gbc, row++, "Họ và tên:", fullNameField);

        // Email
        addFormField(formPanel, gbc, row++, "Email:", emailField);

        // Phone
        addFormField(formPanel, gbc, row++, "Số điện thoại:", phoneField);

        // Faculty
        addFormField(formPanel, gbc, row++, "Khoa:", facultyField);

        // Class
        addFormField(formPanel, gbc, row++, "Lớp:", classField);

        // Admission Year
        addFormField(formPanel, gbc, row++, "Năm nhập học:", admissionYearField);

        // Gender
        addFormField(formPanel, gbc, row++, "Giới tính:", genderComboBox);

        // Citizen ID
        addFormField(formPanel, gbc, row++, "CCCD/CMND:", citizenIdField);

        // GPA
        addFormField(formPanel, gbc, row++, "GPA:", gpaField);

        // Credits
        addFormField(formPanel, gbc, row++, "Tổng tín chỉ:", creditsField);

        // Status
        addFormField(formPanel, gbc, row++, "Trạng thái:", statusComboBox);

        // Emergency Contact
        addFormField(formPanel, gbc, row++, "Người liên hệ khẩn cấp:", emergencyContactField);

        // Emergency Phone
        addFormField(formPanel, gbc, row++, "SĐT khẩn cấp:", emergencyPhoneField);

        // Scroll pane for form
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private void loadStudentData() {
        studentCodeField.setText(student.getStudentCode());
        fullNameField.setText(student.getFullName());
        emailField.setText(student.getEmail());
        phoneField.setText(student.getPhone() != null ? student.getPhone() : "");
        facultyField.setText(student.getFacultyName() != null ? student.getFacultyName() : "N/A");
        classField.setText(student.getClassName() != null ? student.getClassName() : "N/A");
        gpaField.setText(student.getGpa().toString());
        creditsField.setText(String.valueOf(student.getTotalCredits()));
        admissionYearField.setText(String.valueOf(student.getAdmissionYear()));
        citizenIdField.setText(student.getCitizenId() != null ? student.getCitizenId() : "");
        emergencyContactField.setText(student.getEmergencyContact() != null ? student.getEmergencyContact() : "");
        emergencyPhoneField.setText(student.getEmergencyPhone() != null ? student.getEmergencyPhone() : "");

        if (student.getStudentStatus() != null) {
            statusComboBox.setSelectedItem(student.getStudentStatus().toString());
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

        closeButton.addActionListener(new ActionListener() {
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
            updatedStudent.setStudentStatus(Student.StudentStatus.valueOf(statusStr));
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
}
