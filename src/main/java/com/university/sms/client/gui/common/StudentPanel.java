package com.university.sms.client.gui.common;

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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
    private JCheckBox showInactiveCheckbox;
    private AdvancedSearchPanel advancedSearchPanel; // Optional advanced search
    private JButton addButton;
    private JButton deleteButton;
    private JButton activateButton;

    private java.util.List<Student> currentStudents = new java.util.ArrayList<>();

    // Flag to prevent multiple simultaneous refresh
    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    // Log area components
    private JTextArea logArea;
    private JScrollPane logScrollPane;

    public StudentPanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true; // Mark as initialized after setup
        // loadInitialData(); // Bỏ - để ComponentListener handle auto-refresh
    }

    private void initializeComponents() {
        // Create table with Edit button column
        String[] columnNames = { "Mã SV", "Họ tên", "Email", "Khoa", "Lớp", "GPA", "Tín chỉ", "Trạng thái",
                "Thao tác" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only the "Thao tác" column is editable
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 8 ? JButton.class : Object.class;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(30);

        // Add button renderer and editor for Edit column
        studentTable.getColumn("Thao tác").setCellRenderer(new ButtonRenderer());
        studentTable.getColumn("Thao tác").setCellEditor(new ButtonEditor(new JCheckBox()));

        // Create search components
        searchField = new JTextField(20);
        searchButton = new JButton("Tìm kiếm");
        refreshButton = new JButton("Làm mới");
        showInactiveCheckbox = new JCheckBox("Hiển thị tài khoản đã vô hiệu hóa");

        // Create action buttons
        addButton = new JButton("Thêm");
        deleteButton = new JButton("Xóa");
        activateButton = new JButton("Kích hoạt lại");
        activateButton.setEnabled(false);

        // Create log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log hoạt động"));

        // Set button states based on user role and read-only mode
        setupButtonStates();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel with search and buttons
        JPanel topPanel = new JPanel(new BorderLayout());

        // Search panel - có thể dùng AdvancedSearchPanel hoặc search đơn giản
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        if (!isReadOnly) {
            searchPanel.add(showInactiveCheckbox);
        }

        // Nút để toggle advanced search (tùy chọn)
        JButton advancedSearchButton = new JButton("🔍 Nâng cao");
        advancedSearchButton.addActionListener(e -> toggleAdvancedSearch());
        searchPanel.add(advancedSearchButton);

        topPanel.add(searchPanel, BorderLayout.WEST);

        // Advanced search panel (ẩn mặc định)
        advancedSearchPanel = new AdvancedSearchPanel();
        advancedSearchPanel.setVisible(false);
        advancedSearchPanel.setSearchListener((searchText, filters) -> {
            performAdvancedSearch(searchText, filters);
        });

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // Chỉ hiển thị các nút khi không phải Teacher (isReadOnly = false)
        if (!isReadOnly) {
            buttonPanel.add(addButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(activateButton);
        }
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // Container panel cho top panel và advanced search
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(topPanel, BorderLayout.NORTH);
        topContainer.add(advancedSearchPanel, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // Center panel with table and log area
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // Table panel
        JScrollPane tableScrollPane = new JScrollPane(studentTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 400));
        splitPane.setTopComponent(tableScrollPane);

        // Log panel
        splitPane.setBottomComponent(logScrollPane);

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

        // Show inactive checkbox
        showInactiveCheckbox.addActionListener(new ActionListener() {
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

        // Delete button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedStudent();
            }
        });

        // Activate button
        activateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateSelectedStudent();
            }
        });

        // Search field enter key
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        // Table selection listener
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Auto-refresh khi panel được hiển thị (chỉ sau khi đã khởi tạo xong)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                // Chỉ refresh nếu panel đã được khởi tạo hoàn toàn
                if (isInitialized && !isRefreshing) {
                    refreshData();
                }
            }
        });
    }

    private void setupButtonStates() {
        if (isReadOnly) {
            addButton.setEnabled(false);
            deleteButton.setEnabled(false);
            activateButton.setEnabled(false);
        } else {
            // Enable buttons based on user role
            boolean canModify = currentUser.getRole() == User.UserRole.ADMIN;
            addButton.setEnabled(canModify);
            deleteButton.setEnabled(false); // Will be enabled based on selection
            activateButton.setEnabled(false); // Will be enabled based on selection
        }
    }

    private void updateButtonStates() {
        int selectedRow = studentTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        if (!isReadOnly && currentUser.getRole() == User.UserRole.ADMIN && hasSelection &&
                currentStudents != null && selectedRow < currentStudents.size()) {

            Student selectedStudent = currentStudents.get(selectedRow);
            boolean isAccountActive = selectedStudent.isActive();
            boolean isStatusActive = selectedStudent.getStudentStatus() == Student.StudentStatus.ACTIVE;

            deleteButton.setEnabled(isAccountActive && isStatusActive);
            activateButton.setEnabled(!isAccountActive);
        } else {
            deleteButton.setEnabled(false);
            activateButton.setEnabled(false);
        }
    }

    private void addLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @SuppressWarnings("unused")
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
                student.getFacultyName() != null ? student.getFacultyName() : "N/A",
                student.getClassName() != null ? student.getClassName() : "N/A",
                student.getGpa(),
                student.getTotalCredits(),
                student.getStudentStatus(),
                "Xem/Sửa" // Button text
        };
        tableModel.addRow(rowData);
        addLog("Hiển thị thông tin sinh viên: " + student.getStudentCode());
    }

    public void refreshData() {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        if (currentUser.getRole() == User.UserRole.STUDENT) {
            loadStudentOwnInfo();
            return;
        }

        isRefreshing = true;
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                // Get all students (with or without inactive)
                if (showInactiveCheckbox.isSelected()) {
                    Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS_INCLUDE_INACTIVE);
                    return serverConnection.sendRequest(request);
                } else {
                    return serverConnection.getAllStudents();
                }
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
                } finally {
                    isRefreshing = false;
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

        addLog("Đang tìm kiếm: " + keyword);
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

        boolean includeInactive = showInactiveCheckbox.isSelected();

        for (Student student : students) {
            // Nếu không bao gồm inactive: chỉ hiển thị sinh viên có is_active = true VÀ student_status = ACTIVE
            if (!includeInactive) {
                // Kiểm tra cả is_active và student_status
                if (!student.isActive() || student.getStudentStatus() != Student.StudentStatus.ACTIVE) {
                    continue;
                }
            }
            // Nếu includeInactive = true: hiển thị tất cả (không lọc)
            
            Object[] rowData = {
                    student.getStudentCode(),
                    student.getFullName(),
                    student.getEmail(),
                    student.getFacultyName() != null ? student.getFacultyName() : "N/A",
                    student.getClassName() != null ? student.getClassName() : "N/A",
                    student.getGpa(),
                    student.getTotalCredits(),
                    student.getStudentStatus(),
                    "Xem/Sửa" // Button text
            };
            tableModel.addRow(rowData);
        }
        addLog("Đã tải " + tableModel.getRowCount() + " sinh viên"
                + (includeInactive ? " (bao gồm tài khoản và trạng thái không hoạt động)" : " (chỉ hiển thị tài khoản và trạng thái đang hoạt động)"));
    }

    private void showAddStudentDialog() {
        // Create a custom dialog with dropdowns
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm sinh viên", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField code = new JTextField(20);
        JTextField username = new JTextField(20);
        JTextField name = new JTextField(20);
        JTextField email = new JTextField(20);
        JTextField phone = new JTextField(20);
        JComboBox<FacultyItem> facultyCombo = new JComboBox<>();
        JComboBox<ClassItem> classCombo = new JComboBox<>();
        classCombo.addItem(new ClassItem(null, "-- Không có --"));

        // Thêm item mặc định cho faculty combo
        facultyCombo.addItem(new FacultyItem(null, "-- Chọn khoa --"));

        // Load faculties
        SwingWorker<List<com.university.sms.model.Faculty>, Void> facultyWorker = new SwingWorker<List<com.university.sms.model.Faculty>, Void>() {
            @Override
            protected List<com.university.sms.model.Faculty> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<com.university.sms.model.Faculty> faculties = (List<com.university.sms.model.Faculty>) response
                            .getData("faculties");
                    return faculties;
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi từ server";
                    throw new Exception(errorMsg);
                }
            }

            @Override
            protected void done() {
                try {
                    List<com.university.sms.model.Faculty> faculties = get();
                    if (faculties != null && !faculties.isEmpty()) {
                        // Xóa item mặc định và thêm danh sách khoa
                        facultyCombo.removeAllItems();
                        facultyCombo.addItem(new FacultyItem(null, "-- Chọn khoa --"));
                        for (com.university.sms.model.Faculty f : faculties) {
                            facultyCombo.addItem(new FacultyItem(f.getFacultyCode(), f.getFacultyName()));
                        }
                        addLog("Đã tải " + faculties.size() + " khoa");
                    } else {
                        addLog("Không có khoa nào trong hệ thống");
                        JOptionPane.showMessageDialog(dialog,
                                "Không có khoa nào trong hệ thống. Vui lòng thêm khoa trước khi thêm sinh viên.",
                                "Cảnh báo",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    String errorMsg = "Lỗi khi tải danh sách khoa: " + e.getMessage();
                    addLog(errorMsg);
                    JOptionPane.showMessageDialog(dialog,
                            errorMsg,
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        facultyWorker.execute();

        // Load classes
        SwingWorker<List<com.university.sms.model.Class>, Void> classWorker = new SwingWorker<List<com.university.sms.model.Class>, Void>() {
            @Override
            protected List<com.university.sms.model.Class> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ALL_CLASSES);
                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<com.university.sms.model.Class> classes = (List<com.university.sms.model.Class>) response
                            .getData(Constants.KEY_CLASSES);
                    return classes;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    List<com.university.sms.model.Class> classes = get();
                    if (classes != null) {
                        for (com.university.sms.model.Class c : classes) {
                            classCombo.addItem(
                                    new ClassItem(c.getClassCode(), c.getClassCode() + " - " + c.getClassName()));
                        }
                    }
                } catch (Exception e) {
                    addLog("Lỗi khi tải danh sách lớp: " + e.getMessage());
                }
            }
        };
        classWorker.execute();

        // Add listener to filter classes by selected faculty
        facultyCombo.addActionListener(e -> {
            FacultyItem selected = (FacultyItem) facultyCombo.getSelectedItem();
            if (selected != null && selected.code != null) {
                classCombo.removeAllItems();
                classCombo.addItem(new ClassItem(null, "-- Không có --"));
                // Reload classes filtered by faculty
                SwingWorker<List<com.university.sms.model.Class>, Void> filterWorker = new SwingWorker<List<com.university.sms.model.Class>, Void>() {
                    @Override
                    protected List<com.university.sms.model.Class> doInBackground() throws Exception {
                        Message request = Message.createRequest(Constants.ACTION_GET_ALL_CLASSES);
                        Message response = serverConnection.sendRequest(request);
                        if (response != null && response.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<com.university.sms.model.Class> classes = (List<com.university.sms.model.Class>) response
                                    .getData(Constants.KEY_CLASSES);
                            return classes;
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            List<com.university.sms.model.Class> classes = get();
                            if (classes != null) {
                                for (com.university.sms.model.Class c : classes) {
                                    if (selected.code.equals(c.getFacultyCode())) {
                                        classCombo.addItem(new ClassItem(c.getClassCode(),
                                                c.getClassCode() + " - " + c.getClassName()));
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            addLog("Lỗi khi lọc lớp: " + ex.getMessage());
                        }
                    }
                };
                filterWorker.execute();
            }
        });

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Mã SV:"), gbc);
        gbc.gridx = 1;
        panel.add(code, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(username, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Họ tên:"), gbc);
        gbc.gridx = 1;
        panel.add(name, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(email, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("SĐT:"), gbc);
        gbc.gridx = 1;
        panel.add(phone, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Khoa:"), gbc);
        gbc.gridx = 1;
        panel.add(facultyCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Lớp (có thể để trống):"), gbc);
        gbc.gridx = 1;
        panel.add(classCombo, gbc);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Hủy");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final boolean[] confirmed = { false };
        okButton.addActionListener(e -> {
            if (facultyCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn khoa", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            confirmed[0] = true;
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);

        if (!confirmed[0])
            return;

        com.university.sms.model.Student s = new com.university.sms.model.Student();
        s.setStudentCode(code.getText().trim());
        String usernameText = username.getText().trim();
        if (usernameText.isEmpty()) {
            usernameText = code.getText().trim(); // Use studentCode as username if not provided
        }
        s.setUsername(usernameText);
        s.setFullName(name.getText().trim());
        s.setEmail(email.getText().trim());
        s.setPhone(phone.getText().trim());
        FacultyItem selectedFaculty = (FacultyItem) facultyCombo.getSelectedItem();
        s.setFacultyCode(selectedFaculty != null ? selectedFaculty.code : null);
        ClassItem selectedClass = (ClassItem) classCombo.getSelectedItem();
        s.setClassCode(selectedClass != null && selectedClass.code != null ? selectedClass.code : null);
        s.setAdmissionYear(java.time.LocalDate.now().getYear());
        s.setStudentStatus(com.university.sms.model.Student.StudentStatus.ACTIVE);

        addLog("Đang thêm sinh viên: " + s.getStudentCode());
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
                        addLog("Đã thêm sinh viên thành công");
                        ToastNotification.showSuccess(StudentPanel.this, "Đã thêm sinh viên thành công!");
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
            addLog("Đang xóa sinh viên: " + studentCode);
            deleteButton.setEnabled(false);
            final String code = studentCode;
            SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
                @Override
                protected Message doInBackground() throws Exception {
                    return serverConnection.deleteStudent(code);
                }

                @Override
                protected void done() {
                    deleteButton.setEnabled(true);
                    try {
                        Message resp = get();
                        if (resp.isSuccess()) {
                            refreshData();
                            addLog("Đã xóa sinh viên thành công");
                            ToastNotification.showSuccess(StudentPanel.this, "Đã xóa sinh viên thành công!");
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

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        addLog("LỖI: " + message);
    }

    private void toggleAdvancedSearch() {
        if (advancedSearchPanel != null) {
            boolean visible = !advancedSearchPanel.isVisible();
            advancedSearchPanel.setVisible(visible);
            revalidate();
            repaint();
        }
    }

    private void performAdvancedSearch(String searchText, java.util.Map<String, String> filters) {
        addLog("Đang tìm kiếm nâng cao: " + searchText);
        // Nếu có filters, có thể thêm logic filter phức tạp hơn
        // Hiện tại chỉ dùng search text như search đơn giản
        if (searchText != null && !searchText.trim().isEmpty()) {
            searchField.setText(searchText);
            performSearch();
        } else {
            refreshData();
        }
    }

    // Button Renderer for table
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Xem/Sửa" : value.toString());
            return this;
        }
    }

    // Button Editor for table
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int editingRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "Xem/Sửa" : value.toString();
            button.setText(label);
            isPushed = true;
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed && currentStudents != null && editingRow >= 0 && editingRow < currentStudents.size()) {
                Student student = currentStudents.get(editingRow);
                // Use SwingUtilities.invokeLater to avoid issues with cell editing
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showStudentDetailDialog(student);
                    }
                });
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    // Show Student Detail Dialog
    private void showStudentDetailDialog(Student student) {
        StudentDetailDialog dialog = new StudentDetailDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                student,
                serverConnection,
                currentUser,
                isReadOnly);
        dialog.setVisible(true);

        if (dialog.isDataChanged()) {
            refreshData();
            addLog("Đã cập nhật thông tin sinh viên: " + student.getStudentCode());
        }
    }

    private void activateSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow < 0 || currentStudents == null || selectedRow >= currentStudents.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sinh viên để kích hoạt lại.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Student student = currentStudents.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn kích hoạt lại sinh viên:\n" + student.getStudentCode() + " - "
                        + student.getFullName() + "?",
                "Xác nhận kích hoạt",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        addLog("Đang kích hoạt sinh viên: " + student.getStudentCode());
        activateButton.setEnabled(false);
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                // Get userId from username
                com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
                com.university.sms.model.User user = userDAO.findByUsername(student.getUsername());
                if (user == null) {
                    throw new Exception("Không tìm thấy user với username: " + student.getUsername());
                }

                Message request = Message.createRequest(Constants.ACTION_ACTIVATE_USER);
                request.addData("userId", user.getUserId());
                return serverConnection.sendRequest(request);
            }

            @Override
            protected void done() {
                activateButton.setEnabled(true);
                try {
                    Message response = get();
                    if (response != null && response.isSuccess()) {
                        JOptionPane.showMessageDialog(StudentPanel.this,
                                "Đã kích hoạt sinh viên thành công!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        addLog("Đã kích hoạt sinh viên: " + student.getStudentCode());
                        refreshData();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
                        showErrorMessage("Không thể kích hoạt sinh viên: " + errorMsg);
                        addLog("Lỗi: " + errorMsg);
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi kích hoạt sinh viên: " + e.getMessage());
                    addLog("Lỗi: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    // Helper classes for ComboBox items
    private static class FacultyItem {
        String code;
        String name;

        FacultyItem(String code, String name) {
            this.code = code;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class ClassItem {
        String code;
        String name;

        ClassItem(String code, String name) {
            this.code = code;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
