package com.university.sms.client.gui;

import com.university.sms.client.ServerConnection;
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
    private ServerConnection serverConnection;
    private boolean isReadOnly;
    
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    
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

    public StudentPanel(User currentUser, ServerConnection serverConnection, boolean isReadOnly) {
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
        String[] columnNames = {"Mã SV", "Họ tên", "Email", "Khoa", "Lớp", "GPA", "Tín chỉ", "Trạng thái"};
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
        gbc.gridx = 0; gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(studentCodeField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Họ tên:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(fullNameField, gbc);
        
        row++;
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(emailField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Số điện thoại:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(phoneField, gbc);
        
        row++;
        
        // Row 3
        gbc.gridx = 0; gbc.gridy = row;
        studentInfoPanel.add(new JLabel("Khoa:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(departmentField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Lớp:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(classField, gbc);
        
        row++;
        
        // Row 4
        gbc.gridx = 0; gbc.gridy = row;
        studentInfoPanel.add(new JLabel("GPA:"), gbc);
        gbc.gridx = 1;
        studentInfoPanel.add(gpaField, gbc);
        gbc.gridx = 2;
        studentInfoPanel.add(new JLabel("Tổng tín chỉ:"), gbc);
        gbc.gridx = 3;
        studentInfoPanel.add(creditsField, gbc);
        
        row++;
        
        // Row 5
        gbc.gridx = 0; gbc.gridy = row;
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
        JOptionPane.showMessageDialog(this, "Chức năng thêm sinh viên sẽ được phát triển trong phiên bản sau.", 
                                     "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void editSelectedStudent() {
        JOptionPane.showMessageDialog(this, "Chức năng sửa thông tin sinh viên sẽ được phát triển trong phiên bản sau.", 
                                     "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Chức năng xóa sinh viên sẽ được phát triển trong phiên bản sau.", 
                                         "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
