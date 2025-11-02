package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for course registration similar to university's system
 * Top: Selected courses (registration cart)
 * Bottom: Available courses to browse
 */
public class CourseRegistrationPanel extends JPanel {
    private IServerConnection serverConnection;
    private User currentUser;
    private int studentId;
    
    // Top table - Selected courses for registration
    private JTable selectedTable;
    private DefaultTableModel selectedModel;
    private List<Course> selectedCourses = new ArrayList<>();
    
    // Bottom table - Available courses
    private JTable availableTable;
    private DefaultTableModel availableModel;
    private List<Course> availableCourses = new ArrayList<>();
    
    // Registered courses (already in database)
    private List<Integer> registeredCourseIds = new ArrayList<>();
    
    // UI Components
    private JLabel totalCreditsLabel;
    private JLabel conflictLabel;
    private JButton registerButton;
    private JButton removeButton;
    private JButton refreshButton;
    private JComboBox<String> semesterFilter;
    private JTextField searchField;
    
    // Status tracking
    private Map<String, List<String>> scheduleMap = new HashMap<>(); // scheduleKey -> course names
    private boolean hasConflict = false;

    public CourseRegistrationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.4);

        // Top panel - Selected courses
        JPanel topPanel = createSelectedCoursesPanel();
        splitPane.setTopComponent(topPanel);

        // Bottom panel - Available courses
        JPanel bottomPanel = createAvailableCoursesPanel();
        splitPane.setBottomComponent(bottomPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSelectedCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Lớp đã chọn, đăng ký:"));

        // Header with info and actions
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        
        // Left side - Info labels
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        totalCreditsLabel = new JLabel("Tổng số tín chỉ: 0");
        totalCreditsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        conflictLabel = new JLabel("");
        conflictLabel.setForeground(Color.RED);
        conflictLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(totalCreditsLabel);
        infoPanel.add(conflictLabel);
        headerPanel.add(infoPanel, BorderLayout.WEST);
        
        // Right side - Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        removeButton = new JButton("Xóa");
        removeButton.setToolTipText("Xóa lớp đã chọn");
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> removeSelectedCourse());
        actionPanel.add(removeButton);
        
        registerButton = new JButton("Đăng ký");
        registerButton.setFont(new Font("Arial", Font.BOLD, 12));
        registerButton.setBackground(new Color(0, 123, 255));
        registerButton.setForeground(Color.WHITE);
        registerButton.setEnabled(false);
        registerButton.addActionListener(e -> confirmRegistration());
        actionPanel.add(registerButton);
        
        headerPanel.add(actionPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table for selected courses
        String[] columns = {"TT", "Mã lớp học phần", "Tên lớp học phần", "T.chỉ", "Giảng viên", 
                           "Thời khóa biểu", "Tuần học", "Đ.ký lúc", "Đ"};
        selectedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 3) return Integer.class;
                return String.class;
            }
        };
        
        selectedTable = new JTable(selectedModel);
        selectedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeButton.setEnabled(selectedTable.getSelectedRow() >= 0);
            }
        });
        
        // Set column widths
        selectedTable.getColumnModel().getColumn(0).setPreferredWidth(30);   // TT
        selectedTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Mã lớp
        selectedTable.getColumnModel().getColumn(2).setPreferredWidth(250);  // Tên lớp
        selectedTable.getColumnModel().getColumn(3).setPreferredWidth(40);   // TC
        selectedTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Giảng viên
        selectedTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Thời khóa biểu
        selectedTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Tuần học
        selectedTable.getColumnModel().getColumn(7).setPreferredWidth(120);  // Đ.ký lúc
        selectedTable.getColumnModel().getColumn(8).setPreferredWidth(30);   // Đ

        JScrollPane scrollPane = new JScrollPane(selectedTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAvailableCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Lớp chọn riêng:"));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        filterPanel.add(new JLabel("Năm học - Học kỳ:"));
        String[] semesters = {"2024-2025 - HK1", "2024-2025 - HK2", "2023-2024 - HK2"};
        semesterFilter = new JComboBox<>(semesters);
        semesterFilter.addActionListener(e -> loadAvailableCourses());
        filterPanel.add(semesterFilter);
        
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(new JLabel("Tìm kiếm:"));
        searchField = new JTextField(20);
        searchField.addActionListener(e -> applySearchFilter());
        filterPanel.add(searchField);
        
        JButton searchButton = new JButton("Tìm");
        searchButton.addActionListener(e -> applySearchFilter());
        filterPanel.add(searchButton);
        
        refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(e -> loadAvailableCourses());
        filterPanel.add(refreshButton);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Table for available courses
        String[] columns = {"TT", "Mã lớp học phần", "Tên lớp học phần", "T.chỉ", "Giảng viên", 
                           "Thời khóa biểu", "Tuần học", "Đ.ký lúc", "K", "T", "G", "CLC"};
        availableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 3) return Integer.class;
                return String.class;
            }
        };
        
        availableTable = new JTable(availableModel);
        availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Double-click or Enter to add course
        availableTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelectedCourse();
                }
            }
        });
        
        // Set column widths
        availableTable.getColumnModel().getColumn(0).setPreferredWidth(30);   // TT
        availableTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Mã lớp
        availableTable.getColumnModel().getColumn(2).setPreferredWidth(250);  // Tên lớp
        availableTable.getColumnModel().getColumn(3).setPreferredWidth(40);   // TC
        availableTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Giảng viên
        availableTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Thời khóa biểu
        availableTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Tuần học
        availableTable.getColumnModel().getColumn(7).setPreferredWidth(120);  // Đ.ký lúc
        availableTable.getColumnModel().getColumn(8).setPreferredWidth(30);   // K
        availableTable.getColumnModel().getColumn(9).setPreferredWidth(30);   // T
        availableTable.getColumnModel().getColumn(10).setPreferredWidth(30);  // G
        availableTable.getColumnModel().getColumn(11).setPreferredWidth(40);  // CLC

        // Color coding for rows based on capacity
        availableTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    // Check if course is full (you would need to add this logic based on your data)
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(availableTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public void setServerConnection(IServerConnection connection) {
        this.serverConnection = connection;
    }

    public void setCurrentUser(User user, int studentId) {
        this.currentUser = user;
        this.studentId = studentId;
        if (serverConnection != null) {
            loadRegisteredCourses();
            loadAvailableCourses();
        }
    }
    
    /**
     * Load danh sách các lớp mà sinh viên đã đăng ký (trong database)
     */
    private void loadRegisteredCourses() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_MY_REGISTRATIONS);
            request.addData(Constants.KEY_STUDENT_ID, studentId);
            
            Message response = serverConnection.sendRequest(request);
            
            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<CourseRegistration> registrations = (List<CourseRegistration>) 
                    response.getData(Constants.KEY_REGISTRATIONS);
                
                registeredCourseIds.clear();
                if (registrations != null) {
                    for (CourseRegistration reg : registrations) {
                        // Chỉ thêm các lớp đang pending hoặc approved, không thêm cancelled
                        if (reg.getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED) {
                            registeredCourseIds.add(reg.getCourseId());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Không hiển thị error dialog ở đây, chỉ log
        }
    }

    private void loadAvailableCourses() {
        if (serverConnection == null) {
            return;
        }

        try {
            // Get available courses from server
            Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
            Message response = serverConnection.sendRequest(request);
            
            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                
                availableCourses = courses != null ? courses : new ArrayList<>();
                updateAvailableTable();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi từ server";
                JOptionPane.showMessageDialog(this,
                    "Lỗi tải danh sách lớp học: " + errorMsg,
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Lỗi kết nối: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateAvailableTable() {
        availableModel.setRowCount(0);
        
        int rowNum = 1;
        for (Course course : availableCourses) {
            // Skip courses that are already selected in cart
            if (isCourseSelected(course)) {
                continue;
            }
            
            // Skip courses that are already registered in database
            if (isCourseRegistered(course.getCourseId())) {
                continue;
            }
            
            String schedule = course.getScheduleDay() + ": " + course.getScheduleTime() + "," + course.getRoom();
            String enrollmentInfo = course.getCurrentStudents() + "/" + course.getMaxStudents();
            
            Object[] row = {
                rowNum++,
                course.getCourseCode(),
                course.getSubjectName(),
                course.getCredits(),
                course.getTeacherName(),
                schedule,
                course.getWeeks() != null ? course.getWeeks() : "1-16",
                enrollmentInfo,
                "", // K - special flag
                "", // T - special flag
                "", // G - special flag  
                ""  // CLC - special flag
            };
            
            availableModel.addRow(row);
        }
    }

    private void addSelectedCourse() {
        int selectedRow = availableTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        // Get the course code from the selected row
        String courseCode = (String) availableModel.getValueAt(selectedRow, 1);
        
        // Find the course in availableCourses list
        Course courseToAdd = null;
        for (Course c : availableCourses) {
            if (c.getCourseCode().equals(courseCode)) {
                courseToAdd = c;
                break;
            }
        }
        
        if (courseToAdd == null) {
            return;
        }
        
        // Check if already selected
        if (isCourseSelected(courseToAdd)) {
            JOptionPane.showMessageDialog(this,
                "Lớp học này đã được chọn!",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if course is full
        if (courseToAdd.getCurrentStudents() >= courseToAdd.getMaxStudents()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Lớp học này đã đầy. Bạn có muốn thêm vào danh sách chờ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        // Add to selected courses
        selectedCourses.add(courseToAdd);
        updateSelectedTable();
        updateAvailableTable();
        checkScheduleConflicts();
        updateTotalCredits();
    }

    private void removeSelectedCourse() {
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        selectedCourses.remove(selectedRow);
        updateSelectedTable();
        updateAvailableTable();
        checkScheduleConflicts();
        updateTotalCredits();
    }

    private void updateSelectedTable() {
        selectedModel.setRowCount(0);
        
        int rowNum = 1;
        for (Course course : selectedCourses) {
            String schedule = course.getScheduleDay() + ": " + course.getScheduleTime() + "," + course.getRoom();
            String enrollmentInfo = course.getCurrentStudents() + "/" + course.getMaxStudents();
            
            Object[] row = {
                rowNum++,
                course.getCourseCode(),
                course.getSubjectName(),
                course.getCredits(),
                course.getTeacherName(),
                schedule,
                course.getWeeks() != null ? course.getWeeks() : "1-16",
                enrollmentInfo,
                "" // Status flag
            };
            
            selectedModel.addRow(row);
        }
        
        registerButton.setEnabled(selectedCourses.size() > 0 && !hasConflict);
    }

    private void updateTotalCredits() {
        int totalCredits = 0;
        for (Course course : selectedCourses) {
            totalCredits += course.getCredits();
        }
        
        totalCreditsLabel.setText("Tổng số tín chỉ: " + totalCredits);
        
        // Warning if too many credits
        if (totalCredits > 24) {
            totalCreditsLabel.setForeground(Color.RED);
        } else if (totalCredits > 20) {
            totalCreditsLabel.setForeground(Color.ORANGE);
        } else {
            totalCreditsLabel.setForeground(Color.BLACK);
        }
    }

    private void checkScheduleConflicts() {
        scheduleMap.clear();
        hasConflict = false;
        
        for (Course course : selectedCourses) {
            String scheduleKey = course.getScheduleDay() + "-" + course.getScheduleTime();
            
            if (!scheduleMap.containsKey(scheduleKey)) {
                scheduleMap.put(scheduleKey, new ArrayList<>());
            }
            
            scheduleMap.get(scheduleKey).add(course.getSubjectName());
            
            if (scheduleMap.get(scheduleKey).size() > 1) {
                hasConflict = true;
            }
        }
        
        if (hasConflict) {
            conflictLabel.setText("⚠ Có xung đột thời gian!");
            registerButton.setEnabled(false);
            
            // Show conflict details
            StringBuilder conflicts = new StringBuilder("Các lớp bị xung đột:\n\n");
            for (Map.Entry<String, List<String>> entry : scheduleMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    conflicts.append("- Thời gian: ").append(entry.getKey()).append("\n");
                    for (String courseName : entry.getValue()) {
                        conflicts.append("  + ").append(courseName).append("\n");
                    }
                    conflicts.append("\n");
                }
            }
            
            JOptionPane.showMessageDialog(this,
                conflicts.toString(),
                "Xung đột thời gian", JOptionPane.WARNING_MESSAGE);
        } else {
            conflictLabel.setText("");
            registerButton.setEnabled(selectedCourses.size() > 0);
        }
    }

    private void confirmRegistration() {
        if (selectedCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn ít nhất một lớp học!",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (hasConflict) {
            JOptionPane.showMessageDialog(this,
                "Không thể đăng ký vì có xung đột thời gian!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Show confirmation dialog
        int totalCredits = selectedCourses.stream().mapToInt(Course::getCredits).sum();
        String message = String.format(
            "Xác nhận đăng ký %d lớp học (Tổng: %d tín chỉ)?\n\nDanh sách:\n%s",
            selectedCourses.size(),
            totalCredits,
            selectedCourses.stream()
                .map(c -> "- " + c.getCourseCode() + ": " + c.getSubjectName())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
        );
        
        int confirm = JOptionPane.showConfirmDialog(this,
            message,
            "Xác nhận đăng ký",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Register all selected courses
        registerAllCourses();
    }

    private void registerAllCourses() {
        int successCount = 0;
        int failCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (Course course : selectedCourses) {
            try {
                Message msg = Message.createRequest(Constants.ACTION_REGISTER_COURSE);
                msg.addData(Constants.KEY_STUDENT_ID, studentId);
                msg.addData(Constants.KEY_COURSE_ID, course.getCourseId());
                msg.addData(Constants.KEY_NOTE, "Đăng ký qua hệ thống");
                
                Message response = serverConnection.sendRequest(msg);
                
                if (response != null && response.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    errors.append("- ").append(course.getCourseCode()).append(": ").append(errorMsg).append("\n");
                }
                
            } catch (Exception e) {
                failCount++;
                errors.append("- ").append(course.getCourseCode()).append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        // Show result
        if (failCount == 0) {
            JOptionPane.showMessageDialog(this,
                String.format("Đăng ký thành công %d lớp học!\n\nYêu cầu của bạn đang chờ duyệt.", successCount),
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Clear selected courses
            selectedCourses.clear();
            updateSelectedTable();
            updateAvailableTable();
            updateTotalCredits();
        } else {
            JOptionPane.showMessageDialog(this,
                String.format("Đăng ký hoàn tất:\n- Thành công: %d\n- Thất bại: %d\n\nLỗi:\n%s",
                    successCount, failCount, errors.toString()),
                "Kết quả đăng ký",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean isCourseSelected(Course course) {
        for (Course selected : selectedCourses) {
            if (selected.getCourseId() == course.getCourseId()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra xem lớp đã được đăng ký trong database chưa
     */
    private boolean isCourseRegistered(int courseId) {
        return registeredCourseIds.contains(courseId);
    }

    private void applySearchFilter() {
        String searchText = searchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            updateAvailableTable();
            return;
        }
        
        availableModel.setRowCount(0);
        
        int rowNum = 1;
        for (Course course : availableCourses) {
            // Skip courses already in cart
            if (isCourseSelected(course)) {
                continue;
            }
            
            // Skip courses already registered in database
            if (isCourseRegistered(course.getCourseId())) {
                continue;
            }
            
            // Search in course code, subject name, or teacher name
            if (course.getCourseCode().toLowerCase().contains(searchText) ||
                course.getSubjectName().toLowerCase().contains(searchText) ||
                course.getTeacherName().toLowerCase().contains(searchText)) {
                
                String schedule = course.getScheduleDay() + ": " + course.getScheduleTime() + "," + course.getRoom();
                String enrollmentInfo = course.getCurrentStudents() + "/" + course.getMaxStudents();
                
                Object[] row = {
                    rowNum++,
                    course.getCourseCode(),
                    course.getSubjectName(),
                    course.getCredits(),
                    course.getTeacherName(),
                    schedule,
                    course.getWeeks() != null ? course.getWeeks() : "1-16",
                    enrollmentInfo,
                    "", "", "", ""
                };
                
                availableModel.addRow(row);
            }
        }
    }
}

