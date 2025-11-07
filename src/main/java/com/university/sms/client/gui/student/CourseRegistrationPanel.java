package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Student;
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
 * Panel đăng ký tín chỉ cho sinh viên
 * Thiết kế split-panel: Top = Selected courses, Bottom = Available courses
 */
public class CourseRegistrationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private IServerConnection serverConnection;
    private User currentUser;
    private int studentId;
    private int studentFacultyId; // Faculty ID of the student (for filtering courses)

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
    private JTextField searchField;

    public CourseRegistrationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Split pane for top (selected) and bottom (available) tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4); // 40% top, 60% bottom

        // Top Panel: Selected Courses
        JPanel topPanel = createSelectedCoursesPanel();
        splitPane.setTopComponent(topPanel);

        // Bottom Panel: Available Courses
        JPanel bottomPanel = createAvailableCoursesPanel();
        splitPane.setBottomComponent(bottomPanel);

        add(splitPane, BorderLayout.CENTER);

        // Control buttons at the bottom
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createSelectedCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Các môn đã chọn (Chưa đăng ký)"));

        // Table columns
        String[] columns = {"Mã MH", "Tên môn học", "TC", "Giảng viên", "Thứ", "Tiết", "Phòng", "Sĩ số"};
        selectedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        selectedTable = new JTable(selectedModel);
        selectedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(selectedTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeBtn = new JButton("Xóa khỏi danh sách");
        removeBtn.addActionListener(e -> removeSelectedCourse());
        buttonPanel.add(removeBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAvailableCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Danh sách môn học có thể đăng ký"));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchField = new JTextField(30);
        searchField.addActionListener(e -> applySearchFilter());
        searchPanel.add(searchField);
        
        JButton searchBtn = new JButton("Tìm");
        searchBtn.addActionListener(e -> applySearchFilter());
        searchPanel.add(searchBtn);
        
        JButton refreshBtn = new JButton("Làm mới");
        refreshBtn.addActionListener(e -> loadAvailableCourses());
        searchPanel.add(refreshBtn);

        // Table columns
        String[] columns = {"Mã MH", "Tên môn học", "TC", "Giảng viên", "Thứ", "Tiết", "Phòng", "Còn lại/Tối đa", "Trạng thái"};
        availableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        availableTable = new JTable(availableModel);
        availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableTable.setRowHeight(25);
        
        // Color coding for status
        availableTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 8);
                    if ("Đã đăng ký".equals(status)) {
                        c.setBackground(new Color(200, 230, 201)); // Light green
                    } else if ("Đã chọn".equals(status)) {
                        c.setBackground(new Color(255, 245, 157)); // Light yellow
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(availableTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Thêm vào danh sách");
        addBtn.addActionListener(e -> addSelectedCourse());
        buttonPanel.add(addBtn);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        totalCreditsLabel = new JLabel("Tổng tín chỉ đã chọn: 0");
        totalCreditsLabel.setFont(totalCreditsLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        conflictLabel = new JLabel("");
        conflictLabel.setForeground(Color.RED);
        conflictLabel.setFont(conflictLabel.getFont().deriveFont(Font.BOLD, 12f));
        
        infoPanel.add(totalCreditsLabel);
        infoPanel.add(conflictLabel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        registerButton = new JButton("Đăng Ký Các Môn Đã Chọn");
        registerButton.setFont(registerButton.getFont().deriveFont(Font.BOLD, 14f));
        registerButton.setBackground(new Color(76, 175, 80));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.addActionListener(e -> registerSelectedCourses());
        buttonPanel.add(registerButton);

        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    public void setServerConnection(IServerConnection connection) {
        this.serverConnection = connection;
    }

    public void setCurrentUser(User user, int studentId) {
        this.currentUser = user;
        this.studentId = studentId;
        if (serverConnection != null) {
            loadStudentInfo(); // Load student info first to get facultyId
        }
    }

    private void loadStudentInfo() {
        SwingWorker<Student, Void> worker = new SwingWorker<>() {
            @Override
            protected Student doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
                request.addData(Constants.KEY_STUDENT_ID, studentId);
                
                Message response = serverConnection.sendRequest(request);
                
                if (response != null && response.isSuccess()) {
                    return response.getData(Constants.KEY_STUDENT, Student.class);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Student student = get();
                    if (student != null) {
                        studentFacultyId = student.getFacultyId();
                        loadRegisteredCourses(); // Load courses already registered by student first
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                                "Không thể tải thông tin sinh viên",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Lỗi khi tải thông tin sinh viên: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        worker.execute();
    }

    private void loadRegisteredCourses() {
        SwingWorker<List<Integer>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Integer> doInBackground() throws Exception {
                List<Integer> ids = new ArrayList<>();
                Message request = Message.createRequest(Constants.ACTION_GET_MY_REGISTRATIONS);
                request.addData(Constants.KEY_STUDENT_ID, studentId);
                
                Message response = serverConnection.sendRequest(request);
                
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<CourseRegistration> registrations = (List<CourseRegistration>) 
                        response.getData(Constants.KEY_REGISTRATIONS);
                    
                    if (registrations != null) {
                        for (CourseRegistration reg : registrations) {
                            // Chỉ thêm những đăng ký đã được duyệt hoặc đang chờ duyệt
                            if (reg.getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED) {
                                ids.add(reg.getCourseId());
                            }
                        }
                    }
                }
                return ids;
            }

            @Override
            protected void done() {
                try {
                    registeredCourseIds = get();
                    loadAvailableCourses(); // Load available courses AFTER registered courses are loaded
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Không thể tải danh sách môn đã đăng ký: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        worker.execute();
    }

    private void loadAvailableCourses() {
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                // Load courses filtered by student's faculty
                // This will include courses from the student's faculty and general courses (faculty_id = 0)
                Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_FACULTY);
                request.addData(Constants.KEY_FACULTY_ID, studentFacultyId);
                Message response = serverConnection.sendRequest(request);
                
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                    return courses != null ? courses : new ArrayList<>();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    availableCourses = get();
                    updateAvailableTable();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Lỗi khi tải danh sách môn học: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateAvailableTable() {
        availableModel.setRowCount(0);
        String searchText = searchField.getText().trim().toLowerCase();
        
        for (Course course : availableCourses) {
            // Skip if already selected or registered
            if (isCourseSelected(course) || isCourseRegistered(course.getCourseId())) {
                continue;
            }
            
            // Apply search filter
            if (!searchText.isEmpty()) {
                String searchableText = (course.getCourseCode() + " " + 
                                        course.getCourseName() + " " + 
                                        course.getTeacherName()).toLowerCase();
                if (!searchableText.contains(searchText)) {
                    continue;
                }
            }
            
            int remaining = course.getMaxStudents() - course.getCurrentEnrollment();
            String availabilityText = remaining + "/" + course.getMaxStudents();
            
            String status = "Có thể đăng ký";
            if (remaining <= 0) {
                status = "Đã đầy";
            }
            
            availableModel.addRow(new Object[]{
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredits(),
                    course.getTeacherName(),
                    course.getScheduleDay(),
                    course.getScheduleTime(),
                    course.getRoom(),
                    availabilityText,
                    status
            });
        }
    }

    private void addSelectedCourse() {
        int selectedRow = availableTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một môn học để thêm",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String courseCode = (String) availableModel.getValueAt(selectedRow, 0);
        
        // Find course in availableCourses
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
        
        // Check if already full
        if (courseToAdd.getCurrentEnrollment() >= courseToAdd.getMaxStudents()) {
            JOptionPane.showMessageDialog(this,
                    "Lớp học này đã đầy!",
                    "Không thể thêm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check for schedule conflict
        if (hasScheduleConflict(courseToAdd)) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Môn học này có xung đột lịch học với môn đã chọn!\nBạn có chắc muốn thêm?",
                    "Cảnh báo xung đột",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        // Add to selected list
        selectedCourses.add(courseToAdd);
        
        // Update tables
        selectedModel.addRow(new Object[]{
                courseToAdd.getCourseCode(),
                courseToAdd.getCourseName(),
                courseToAdd.getCredits(),
                courseToAdd.getTeacherName(),
                courseToAdd.getScheduleDay(),
                courseToAdd.getScheduleTime(),
                courseToAdd.getRoom(),
                courseToAdd.getCurrentEnrollment() + "/" + courseToAdd.getMaxStudents()
        });
        
        updateAvailableTable();
        updateCreditsAndConflicts();
    }

    private void removeSelectedCourse() {
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một môn học để xóa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        selectedCourses.remove(selectedRow);
        selectedModel.removeRow(selectedRow);
        
        updateAvailableTable();
        updateCreditsAndConflicts();
    }

    private void registerSelectedCourses() {
        if (selectedCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn ít nhất một môn học để đăng ký",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Check conflicts
        if (hasAnyConflict()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Có xung đột lịch học trong các môn đã chọn!\nBạn có chắc muốn đăng ký?",
                    "Cảnh báo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có chắc muốn đăng ký %d môn học?\nTổng: %d tín chỉ",
                        selectedCourses.size(), getTotalCredits()),
                "Xác nhận đăng ký",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Register courses
        SwingWorker<Map<Course, String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<Course, String> doInBackground() throws Exception {
                Map<Course, String> results = new HashMap<>();
                
                for (Course course : selectedCourses) {
                    Message request = Message.createRequest(Constants.ACTION_REGISTER_COURSE);
                    request.addData(Constants.KEY_STUDENT_ID, studentId);
                    request.addData(Constants.KEY_COURSE_ID, course.getCourseId());
                    
                    Message response = serverConnection.sendRequest(request);
                    
                    if (response != null && response.isSuccess()) {
                        results.put(course, "Thành công");
                    } else {
                        String error = response != null ? response.getMessage() : "Không có phản hồi";
                        results.put(course, "Lỗi: " + error);
                    }
                }
                
                return results;
            }

            @Override
            protected void done() {
                try {
                    Map<Course, String> results = get();
                    
                    // Show results
                    StringBuilder message = new StringBuilder("Kết quả đăng ký:\n\n");
                    int successCount = 0;
                    
                    for (Map.Entry<Course, String> entry : results.entrySet()) {
                        Course course = entry.getKey();
                        String result = entry.getValue();
                        
                        message.append(course.getCourseCode())
                                .append(" - ")
                                .append(course.getCourseName())
                                .append(": ")
                                .append(result)
                                .append("\n");
                        
                        if ("Thành công".equals(result)) {
                            successCount++;
                        }
                    }
                    
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            message.toString(),
                            "Kết quả đăng ký",
                            successCount == results.size() ? 
                                    JOptionPane.INFORMATION_MESSAGE : 
                                    JOptionPane.WARNING_MESSAGE);
                    
                    // Clear selected courses and reload only if all succeeded
                    if (successCount == results.size() && successCount > 0) {
                        selectedCourses.clear();
                        selectedModel.setRowCount(0);
                        updateCreditsAndConflicts();
                    }
                    // Reload registered courses to update the filter
                    loadRegisteredCourses();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Lỗi khi đăng ký: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private boolean hasScheduleConflict(Course newCourse) {
        for (Course existing : selectedCourses) {
            if (isTimeConflict(existing, newCourse)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyConflict() {
        for (int i = 0; i < selectedCourses.size(); i++) {
            for (int j = i + 1; j < selectedCourses.size(); j++) {
                if (isTimeConflict(selectedCourses.get(i), selectedCourses.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTimeConflict(Course c1, Course c2) {
        // Check NULL for scheduleDay
        if (c1.getScheduleDay() == null || c2.getScheduleDay() == null) {
            return false;
        }
        
        // Different days = no conflict
        if (!c1.getScheduleDay().equals(c2.getScheduleDay())) {
            return false;
        }
        
        // Check NULL for scheduleTime
        if (c1.getScheduleTime() == null || c2.getScheduleTime() == null) {
            return false;
        }
        
        // Parse time slots (e.g., "7:00-9:00" or "Tiết 1-3 (07:00-09:30)")
        String scheduleTime1 = c1.getScheduleTime().trim();
        String scheduleTime2 = c2.getScheduleTime().trim();
        
        // Extract time range if format is "Tiết X-Y (HH:MM-HH:MM)"
        if (scheduleTime1.contains("(") && scheduleTime1.contains(")")) {
            int start = scheduleTime1.indexOf("(");
            int end = scheduleTime1.indexOf(")");
            scheduleTime1 = scheduleTime1.substring(start + 1, end).trim();
        }
        
        if (scheduleTime2.contains("(") && scheduleTime2.contains(")")) {
            int start = scheduleTime2.indexOf("(");
            int end = scheduleTime2.indexOf(")");
            scheduleTime2 = scheduleTime2.substring(start + 1, end).trim();
        }
        
        // Split by "-" to get start and end time
        String[] time1 = scheduleTime1.split("-");
        String[] time2 = scheduleTime2.split("-");
        
        if (time1.length != 2 || time2.length != 2) {
            return false;
        }
        
        try {
            // Simple overlap check: if end1 <= start2 OR end2 <= start1, no conflict
            return !(time1[1].trim().compareTo(time2[0].trim()) <= 0 || 
                     time2[1].trim().compareTo(time1[0].trim()) <= 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateCreditsAndConflicts() {
        int totalCredits = getTotalCredits();
        totalCreditsLabel.setText("Tổng tín chỉ đã chọn: " + totalCredits);
        
        if (hasAnyConflict()) {
            conflictLabel.setText("⚠ CÓ XUNG ĐỘT LỊCH HỌC!");
        } else {
            conflictLabel.setText("");
        }
        
        registerButton.setEnabled(!selectedCourses.isEmpty());
    }

    private int getTotalCredits() {
        int total = 0;
        for (Course course : selectedCourses) {
            total += course.getCredits();
        }
        return total;
    }

    private boolean isCourseSelected(Course course) {
        for (Course selected : selectedCourses) {
            if (selected.getCourseId() == course.getCourseId()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCourseRegistered(int courseId) {
        return registeredCourseIds.contains(courseId);
    }

    private void applySearchFilter() {
        updateAvailableTable();
    }

    public void refreshData() {
        loadRegisteredCourses();
        loadAvailableCourses();
    }
}

