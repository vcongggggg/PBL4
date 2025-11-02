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
    private JButton deleteButton;

    private java.util.List<Student> currentStudents;

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
        loadInitialData();
    }

    private void initializeComponents() {
        // Create table with Edit button column
        String[] columnNames = { "Mã SV", "Họ tên", "Email", "Khoa", "Lớp", "GPA", "Tín chỉ", "Trạng thái", "Thao tác" };
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

        // Create action buttons
        addButton = new JButton("Thêm");
        deleteButton = new JButton("Xóa");

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
        buttonPanel.add(deleteButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

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
            deleteButton.setEnabled(false);
        } else {
            // Enable buttons based on user role
            boolean canModify = currentUser.getRole() == User.UserRole.ADMIN;
            addButton.setEnabled(canModify);
            deleteButton.setEnabled(canModify);
        }
    }
    
    private void addLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
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

        for (Student student : students) {
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
        addLog("Đã tải " + students.size() + " sinh viên");
    }


    private void showAddStudentDialog() {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JTextField facultyId = new JTextField();
        JTextField classId = new JTextField();
        Object[] fields = {
                "Mã SV:", code,
                "Họ tên:", name,
                "Email:", email,
                "SĐT:", phone,
                "Khoa (ID):", facultyId,
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
            s.setFacultyId(Integer.parseInt(facultyId.getText().trim()));
        } catch (Exception ignored) {
            s.setFacultyId(1);
        }
        try {
            s.setClassId(classId.getText().trim().isEmpty() ? null : Integer.parseInt(classId.getText().trim()));
        } catch (Exception ignored) {
            s.setClassId(null);
        }
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

            addLog("Đang xóa sinh viên: " + studentCode);
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
                            addLog("Đã xóa sinh viên thành công");
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

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        addLog("LỖI: " + message);
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
            if (isPushed && editingRow >= 0 && editingRow < currentStudents.size()) {
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
            isReadOnly
        );
        dialog.setVisible(true);
        
        if (dialog.isDataChanged()) {
            refreshData();
            addLog("Đã cập nhật thông tin sinh viên: " + student.getStudentCode());
        }
    }
}
