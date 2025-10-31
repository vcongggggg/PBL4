package com.university.sms.client.gui;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel quản lý sinh viên
 */
public class StudentPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    private java.util.List<Student> currentStudents;

    private JPanel studentInfoPanel;
    private JTextField studentCodeField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField departmentField;
    private JTextField classField;
    private JTextField gpaField;
    private JTextField creditsField;
    private JTextField statusField;

    public StudentPanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        loadInitialData();
    }

    private void initializeComponents() {
        // Create table
        String[] columnNames = { "Mã SV", "Họ tên", "Email", "Khoa", "Lớp", "GPA", "Tín chỉ", "Trạng thái" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(25);

        // Create search components
        searchField = new JTextField(20);
        searchButton = new JButton("Tìm kiếm");
        refreshButton = new JButton("Làm mới");

        // Create action buttons
        addButton = new JButton("Thêm");
        editButton = new JButton("Sửa");
        deleteButton = new JButton("Xóa");

        // Create student info panel
        createStudentInfoPanel();

        // Set button states based on user role and read-only mode
        setupButtonStates();
    }

    private void createStudentInfoPanel() {
        studentInfoPanel = new JPanel(new GridBagLayout());
        studentInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin sinh viên"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Create text fields
        studentCodeField = new JTextField(15);
        fullNameField = new JTextField(15);
        emailField = new JTextField(15);
        phoneField = new JTextField(15);
        departmentField = new JTextField(15);
        classField = new JTextField(15);
        gpaField = new JTextField(15);
        creditsField = new JTextField(15);
        statusField = new JTextField(15);

        // Make fields read-only if necessary
        if (isReadOnly) {
            setFieldsReadOnly(true);
        }

        // Layout components
        int row = 0;

        // Row 1
        gbc.gridx = 0;
        gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(studentCodeField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Họ tên:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(fullNameField, gbc);

        row++;

        // Row 2
        gbc.gridx = 0;
        gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(emailField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Số điện thoại:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(phoneField, gbc);

        row++;

        // Row 3
        gbc.gridx = 0;
        gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Khoa:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(departmentField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Lớp:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(classField, gbc);

        row++;

        // Row 4
        gbc.gridx = 0;
        gbc.gridy = row;
        studentInfoPanel.add(new JLabel("GPA:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(gpaField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Tổng tín chỉ:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(creditsField, gbc);

        row++;

        // Row 5
        gbc.gridx = 0;
        gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Trạng thái:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(statusField, gbc);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel with search and buttons
        JPanel topPanel = new JPanel(new BorderLayout());

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        topPanel.add(searchPanel, BorderLayout.WEST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with table and info
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Table panel
        JScrollPane tableScrollPane = new JScrollPane(studentTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 300));
        splitPane.setTopComponent(tableScrollPane);

        // Info panel
        splitPane.setBottomComponent(studentInfoPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        // Search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        // Refresh button
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });

        // Add button
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddStudentDialog();
            }
        });

        // Edit button
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedStudent();
            }
        });

        // Delete button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedStudent();
            }
        });

        // Table selection listener
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedStudentInfo();
            }
        });

        // Search field enter key
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });
    }

    private void setupButtonStates() {
        if (isReadOnly) {
            addButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            // Enable buttons based on user role
            boolean canModify = currentUser.getRole() == User.UserRole.ADMIN;
            addButton.setEnabled(canModify);
            deleteButton.setEnabled(canModify);
            editButton.setEnabled(canModify || currentUser.getRole() == User.UserRole.TEACHER);
        }
    }

    private void loadInitialData() {
        // Don't load data here - it will be called by setServerConnection()
        // This prevents double loading
    }

    private void loadStudentOwnInfo() {
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.getStudentInfo(null); // null means get own info
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        Student student = (Student) response.getData(Constants.KEY_STUDENT);
                        if (student != null) {
                            displaySingleStudent(student);
                        }
                    } else {
                        showErrorMessage("Không thể tải thông tin sinh viên: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải thông tin: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void displaySingleStudent(Student student) {
        // Clear table and add single student
        tableModel.setRowCount(0);
        Object[] rowData = {
                student.getStudentCode(),
                student.getFullName(),
                student.getEmail(),
                "N/A", // Department name would need to be fetched
                "N/A", // Class name would need to be fetched
                student.getGpa(),
                student.getTotalCredits(),
                student.getStudentStatus()
        };
        tableModel.addRow(rowData);

        // Select the row and display info
        studentTable.setRowSelectionInterval(0, 0);
        displayStudentInfo(student);
    }

    public void refreshData() {
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            loadStudentOwnInfo();
            return;
        }

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                // Get all students
                return serverConnection.getAllStudents();
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Student> students = (List<Student>) response.getData(Constants.KEY_STUDENTS);
                        updateStudentTable(students);
                    } else {
                        showErrorMessage("Không thể tải danh sách sinh viên: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải dữ liệu: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshData();
            return;
        }

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.searchStudents(keyword);
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Student> students = (List<Student>) response.getData(Constants.KEY_STUDENTS);
                        updateStudentTable(students);
                    } else {
                        showErrorMessage("Tìm kiếm thất bại: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tìm kiếm: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void updateStudentTable(List<Student> students) {
        currentStudents = students;
        tableModel.setRowCount(0);

        for (Student student : students) {
            Object[] rowData = {
                    student.getStudentCode(),
                    student.getFullName(),
                    student.getEmail(),
                    "N/A", // Department name
                    "N/A", // Class name
                    student.getGpa(),
                    student.getTotalCredits(),
                    student.getStudentStatus()
            };
            tableModel.addRow(rowData);
        }
    }

    private void displaySelectedStudentInfo() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Get student code from table
            String studentCode = (String) tableModel.getValueAt(selectedRow, 0);

            // For now, display basic info from table
            // In a full implementation, you would fetch complete student info
            studentCodeField.setText((String) tableModel.getValueAt(selectedRow, 0));
            fullNameField.setText((String) tableModel.getValueAt(selectedRow, 1));
            emailField.setText((String) tableModel.getValueAt(selectedRow, 2));
            departmentField.setText((String) tableModel.getValueAt(selectedRow, 3));
            classField.setText((String) tableModel.getValueAt(selectedRow, 4));
            gpaField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 5)));
            creditsField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 6)));
            statusField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 7)));
        }
    }

    private void displayStudentInfo(Student student) {
        studentCodeField.setText(student.getStudentCode());
        fullNameField.setText(student.getFullName());
        emailField.setText(student.getEmail());
        phoneField.setText(student.getPhone());
        departmentField.setText("N/A"); // Would need department name
        classField.setText("N/A"); // Would need class name
        gpaField.setText(student.getGpa().toString());
        creditsField.setText(String.valueOf(student.getTotalCredits()));
        statusField.setText(student.getStudentStatus().toString());
    }

    private void showAddStudentDialog() {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JTextField deptId = new JTextField();
        JTextField classId = new JTextField();
        Object[] fields = {
                "Mã SV:", code,
                "Họ tên:", name,
                "Email:", email,
                "SĐT:", phone,
                "Khoa (ID):", deptId,
                "Lớp (ID, có thể để trống):", classId
        };
        int res = JOptionPane.showConfirmDialog(this, fields, "Thêm sinh viên", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION)
            return;

        com.university.sms.model.Student s = new com.university.sms.model.Student();
        s.setStudentCode(code.getText().trim());
        s.setFullName(name.getText().trim());
        s.setEmail(email.getText().trim());
        s.setPhone(phone.getText().trim());
        try {
            s.setDepartmentId(Integer.parseInt(deptId.getText().trim()));
        } catch (Exception ignored) {
            s.setDepartmentId(1);
        }
        try {
            s.setClassId(classId.getText().trim().isEmpty() ? null : Integer.parseInt(classId.getText().trim()));
        } catch (Exception ignored) {
            s.setClassId(null);
        }
        s.setAdmissionYear(java.time.LocalDate.now().getYear());
        s.setStudentStatus(com.university.sms.model.Student.StudentStatus.ACTIVE);

        addButton.setEnabled(false);
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.addStudent(s);
            }

            @Override
            protected void done() {
                addButton.setEnabled(true);
                try {
                    Message resp = get();
                    if (resp.isSuccess()) {
                        refreshData();
                        JOptionPane.showMessageDialog(StudentPanel.this, "Đã thêm sinh viên.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showErrorMessage("Thêm thất bại: " + resp.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi thêm: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void editSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sinh viên cần sửa.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String studentCode = (String) tableModel.getValueAt(selectedRow, 0);
        com.university.sms.model.Student existing = null;
        if (currentStudents != null) {
            for (Student s : currentStudents) {
                if (studentCode.equals(s.getStudentCode())) {
                    existing = s;
                    break;
                }
            }
        }
        if (existing == null) {
            showErrorMessage("Không tìm thấy dữ liệu chi tiết sinh viên.");
            return;
        }

        JTextField name = new JTextField(existing.getFullName());
        JTextField email = new JTextField(existing.getEmail());
        JTextField phone = new JTextField(existing.getPhone());
        Object[] fields = {
                "Họ tên:", name,
                "Email:", email,
                "SĐT:", phone
        };
        int res = JOptionPane.showConfirmDialog(this, fields, "Sửa sinh viên", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION)
            return;

        com.university.sms.model.Student toUpdate = new com.university.sms.model.Student();
        toUpdate.setStudentId(existing.getStudentId());
        toUpdate.setStudentCode(existing.getStudentCode());
        toUpdate.setFullName(name.getText().trim());
        toUpdate.setEmail(email.getText().trim());
        toUpdate.setPhone(phone.getText().trim());
        toUpdate.setClassId(existing.getClassId());
        toUpdate.setBirthDate(existing.getBirthDate());
        toUpdate.setGender(existing.getGender());
        toUpdate.setCitizenId(existing.getCitizenId());
        toUpdate.setEmergencyContact(existing.getEmergencyContact());
        toUpdate.setEmergencyPhone(existing.getEmergencyPhone());

        editButton.setEnabled(false);
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.updateStudent(toUpdate);
            }

            @Override
            protected void done() {
                editButton.setEnabled(true);
                try {
                    Message resp = get();
                    if (resp.isSuccess()) {
                        refreshData();
                        JOptionPane.showMessageDialog(StudentPanel.this, "Đã cập nhật sinh viên.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showErrorMessage("Cập nhật thất bại: " + resp.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi cập nhật: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sinh viên cần xóa.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String studentCode = (String) tableModel.getValueAt(selectedRow, 0);
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa sinh viên " + studentCode + " không?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // Resolve studentId from currentStudents list by code
            Integer studentId = null;
            if (currentStudents != null) {
                for (Student s : currentStudents) {
                    if (studentCode.equals(s.getStudentCode())) {
                        studentId = s.getStudentId();
                        break;
                    }
                }
            }
            if (studentId == null || studentId <= 0) {
                showErrorMessage("Không xác định được ID sinh viên để xóa.");
                return;
            }

            deleteButton.setEnabled(false);
            final int sid = studentId;
            SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
                @Override
                protected Message doInBackground() throws Exception {
                    return serverConnection.deleteStudent(sid);
                }

                @Override
                protected void done() {
                    deleteButton.setEnabled(true);
                    try {
                        Message resp = get();
                        if (resp.isSuccess()) {
                            refreshData();
                            JOptionPane.showMessageDialog(StudentPanel.this, "Đã xóa sinh viên.",
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showErrorMessage("Xóa thất bại: " + resp.getMessage());
                        }
                    } catch (Exception ex) {
                        showErrorMessage("Lỗi khi xóa: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void setFieldsReadOnly(boolean readOnly) {
        studentCodeField.setEditable(!readOnly);
        fullNameField.setEditable(!readOnly);
        emailField.setEditable(!readOnly);
        phoneField.setEditable(!readOnly);
        departmentField.setEditable(!readOnly);
        classField.setEditable(!readOnly);
        gpaField.setEditable(!readOnly);
        creditsField.setEditable(!readOnly);
        statusField.setEditable(!readOnly);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
