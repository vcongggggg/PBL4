package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel quản lý lớp học phần (Courses/Class Sections)
 * - Student: Chỉ hiển thị các khóa học đã đăng ký, có filter theo học kỳ
 * - Admin: Xem tất cả lớp, có nút "Xem danh sách sinh viên"
 * - Teacher: Xem lớp của mình, có nút "Nhập điểm"
 */
public class CoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton viewStudentsButton; // For Admin/Teacher (top button)
    private JButton deleteCourseButton; // For Admin - Hủy lớp
    private JButton gradeEntryButton; // For Teacher

    private List<Course> currentCourses;
    private List<Course> allCourses; // All courses for filtering

    // Semester filter
    private JComboBox<String> semesterFilter;
    private JLabel semesterLabel;
    private Map<String, Course> courseMap; // Map courseId to Course for quick lookup

    // Flag to prevent multiple simultaneous refresh
    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    // Column index for "SV hiện tại/Tối đa"
    private static final int STUDENT_COUNT_COLUMN = 7;

    public CoursePanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
    }

    private void initializeComponents() {
        // Create table
        String[] columnNames = { "Mã lớp", "Môn học", "Giáo viên", "Năm học", "Học kỳ", "Phòng", "Lịch học",
                "SV hiện tại/Tối đa" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing only for the button column
                return column == STUDENT_COUNT_COLUMN;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == STUDENT_COUNT_COLUMN) {
                    return String.class;
                }
                return Object.class;
            }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setRowHeight(35); // Increased height for button

        // Set custom renderer and editor for student count column
        courseTable.getColumnModel().getColumn(STUDENT_COUNT_COLUMN)
                .setCellRenderer(new ButtonCellRenderer());
        courseTable.getColumnModel().getColumn(STUDENT_COUNT_COLUMN)
                .setCellEditor(new ButtonCellEditor(new JCheckBox()));

        refreshButton = new JButton("Làm mới");

        // Semester filter (for all roles, but especially useful for students)
        semesterLabel = new JLabel("Học kỳ:");
        semesterFilter = new JComboBox<>();
        semesterFilter.addItem("Tất cả");
        semesterFilter.addActionListener(e -> filterBySemester());

        // Admin: "Xem danh sách sinh viên" và "Hủy lớp"
        if (currentUser.getRole() == User.UserRole.ADMIN) {
            viewStudentsButton = new JButton("Xem danh sách sinh viên");
            viewStudentsButton.setEnabled(false);

            deleteCourseButton = new JButton("Hủy lớp");
            deleteCourseButton.setEnabled(false);
            deleteCourseButton.setForeground(Color.RED);
        }

        // Teacher: "Nhập điểm" và "Xem danh sách sinh viên"
        if (currentUser.getRole() == User.UserRole.TEACHER) {
            viewStudentsButton = new JButton("Xem danh sách sinh viên");
            viewStudentsButton.setEnabled(false);

            gradeEntryButton = new JButton("Nhập điểm");
            gradeEntryButton.setEnabled(false);
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel with buttons and filter
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(refreshButton);

        // Add semester filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterPanel.add(semesterLabel);
        filterPanel.add(semesterFilter);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(buttonPanel);
        leftPanel.add(new JSeparator(SwingConstants.VERTICAL));
        leftPanel.add(filterPanel);

        topPanel.add(leftPanel, BorderLayout.WEST);

        // Right side buttons for Admin only (Teacher không có nút phía trên)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (currentUser.getRole() == User.UserRole.ADMIN) {
            if (viewStudentsButton != null) {
                rightPanel.add(viewStudentsButton);
            }
            if (deleteCourseButton != null) {
                rightPanel.add(deleteCourseButton);
            }
        }

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center with table
        JScrollPane scrollPane = new JScrollPane(courseTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        refreshButton.addActionListener(e -> refreshData());

        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        if (viewStudentsButton != null) {
            viewStudentsButton.addActionListener(e -> viewStudentsList());
        }

        if (deleteCourseButton != null) {
            deleteCourseButton.addActionListener(e -> deleteCourse());
        }

        if (gradeEntryButton != null) {
            gradeEntryButton.addActionListener(e -> openGradeEntryDialog());
        }

        // Auto-refresh khi panel được hiển thị
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (isInitialized && !isRefreshing) {
                    refreshData();
                }
            }
        });
    }

    public void refreshData() {
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;
        SwingWorker<List<Course>, Void> worker = new SwingWorker<List<Course>, Void>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                if (currentUser.getRole() == User.UserRole.STUDENT) {
                    // For students: get enrolled courses only
                    return getEnrolledCourses();
                } else if (currentUser.getRole() == User.UserRole.TEACHER) {
                    // For teachers: get only courses taught by this teacher
                    Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
                    request.addData("teacherId", currentUser.getUserId());
                    Message response = serverConnection.sendRequest(request);
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                        return courses != null ? courses : new ArrayList<>();
                    }
                    return new ArrayList<>();
                } else {
                    // For admin: get all courses
                    Message response = serverConnection.getAllCourses();
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                        return courses != null ? courses : new ArrayList<>();
                    }
                    return new ArrayList<>();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Course> courses = get();
                    allCourses = courses;
                    updateSemesterFilter(courses);
                    filterBySemester();
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải dữ liệu: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    /**
     * Get enrolled courses for student
     */
    private List<Course> getEnrolledCourses() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS);
            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Enrollment> enrollments = (List<Enrollment>) response.getData(Constants.KEY_ENROLLMENTS);

                if (enrollments == null || enrollments.isEmpty()) {
                    return new ArrayList<>();
                }

                // Get course IDs from enrollments
                Set<Integer> courseIds = enrollments.stream()
                        .map(Enrollment::getCourseId)
                        .collect(Collectors.toSet());

                // Get all courses and filter by enrolled course IDs
                Message allCoursesResponse = serverConnection.getAllCourses();
                if (allCoursesResponse.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Course> allCourses = (List<Course>) allCoursesResponse.getData(Constants.KEY_COURSES);
                    if (allCourses != null) {
                        return allCourses.stream()
                                .filter(c -> courseIds.contains(c.getCourseId()))
                                .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Update semester filter dropdown with available semesters
     */
    private void updateSemesterFilter(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            semesterFilter.removeAllItems();
            semesterFilter.addItem("Tất cả");
            return;
        }

        // Get unique semester combinations
        Set<String> semesterSet = new TreeSet<>((s1, s2) -> {
            // Sort by academic year first, then by semester
            String[] parts1 = s1.split(" - HK");
            String[] parts2 = s2.split(" - HK");
            int yearCompare = parts1[0].compareTo(parts2[0]);
            if (yearCompare != 0)
                return yearCompare;
            if (parts1.length > 1 && parts2.length > 1) {
                return Integer.compare(Integer.parseInt(parts1[1]), Integer.parseInt(parts2[1]));
            }
            return s1.compareTo(s2);
        });

        for (Course course : courses) {
            String semesterKey = course.getAcademicYear() + " - HK" + course.getSemester();
            semesterSet.add(semesterKey);
        }

        semesterFilter.removeAllItems();
        semesterFilter.addItem("Tất cả");

        // Find current semester (default selection)
        String currentSemester = getCurrentSemester(courses);
        boolean currentSemesterFound = false;

        for (String semester : semesterSet) {
            semesterFilter.addItem(semester);
            if (semester.equals(currentSemester)) {
                currentSemesterFound = true;
            }
        }

        // Set default to current semester if found, otherwise "Tất cả"
        if (currentSemesterFound) {
            semesterFilter.setSelectedItem(currentSemester);
        } else {
            semesterFilter.setSelectedItem("Tất cả");
        }
    }

    /**
     * Get current semester based on current date
     */
    private String getCurrentSemester(List<Course> courses) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Assume: Semester 1: Sep-Dec (9-12), Semester 2: Jan-May (1-5)
        int currentSemester;
        String academicYear;

        if (currentMonth >= 9 || currentMonth <= 1) {
            // Semester 1 or early semester 2
            if (currentMonth >= 9) {
                currentSemester = 1;
                academicYear = (currentYear) + "-" + (currentYear + 1);
            } else {
                currentSemester = 2;
                academicYear = (currentYear - 1) + "-" + currentYear;
            }
        } else {
            // Semester 2
            currentSemester = 2;
            academicYear = (currentYear - 1) + "-" + currentYear;
        }

        String semesterKey = academicYear + " - HK" + currentSemester;

        // Check if this semester exists in courses
        for (Course course : courses) {
            String courseKey = course.getAcademicYear() + " - HK" + course.getSemester();
            if (courseKey.equals(semesterKey)) {
                return semesterKey;
            }
        }

        // If not found, return the most recent semester
        if (!courses.isEmpty()) {
            Course latestCourse = courses.stream()
                    .max(Comparator.comparing(Course::getAcademicYear)
                            .thenComparing(Course::getSemester))
                    .orElse(courses.get(0));
            return latestCourse.getAcademicYear() + " - HK" + latestCourse.getSemester();
        }

        return "Tất cả";
    }

    /**
     * Filter courses by selected semester
     */
    private void filterBySemester() {
        if (allCourses == null) {
            return;
        }

        String selected = (String) semesterFilter.getSelectedItem();
        if (selected == null || "Tất cả".equals(selected)) {
            updateCourseTable(allCourses);
            return;
        }

        // Parse selected semester (format: "2024-2025 - HK1")
        String[] parts = selected.split(" - HK");
        if (parts.length != 2) {
            updateCourseTable(allCourses);
            return;
        }

        String academicYear = parts[0].trim();
        int semester = Integer.parseInt(parts[1].trim());

        List<Course> filtered = allCourses.stream()
                .filter(c -> academicYear.equals(c.getAcademicYear()) && semester == c.getSemester())
                .collect(Collectors.toList());

        updateCourseTable(filtered);
    }

    private void updateCourseTable(List<Course> courses) {
        this.currentCourses = courses;
        tableModel.setRowCount(0);

        // Create course map for quick lookup
        courseMap = new HashMap<>();
        if (courses != null) {
            for (Course course : courses) {
                courseMap.put(course.getCourseId() + "", course);
                Object[] rowData = {
                        course.getCourseCode(),
                        course.getSubjectName(),
                        course.getTeacherName(),
                        course.getAcademicYear(),
                        course.getSemester(),
                        course.getRoom(),
                        (course.getScheduleDay() != null ? course.getScheduleDay() : "") + " " +
                                (course.getScheduleTime() != null ? course.getScheduleTime() : ""),
                        course.getCurrentStudents() + "/" + course.getMaxStudents() // This will be rendered as button
                };
                tableModel.addRow(rowData);
            }
        }
    }

    private void updateButtonStates() {
        int selectedRow = courseTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        if (viewStudentsButton != null) {
            viewStudentsButton.setEnabled(hasSelection);
        }
        if (deleteCourseButton != null) {
            deleteCourseButton.setEnabled(hasSelection);
        }
        if (gradeEntryButton != null) {
            gradeEntryButton.setEnabled(hasSelection);
        }
    }

    /**
     * View students list - called from button in table cell or top button
     */
    private void viewStudentsList(int rowIndex) {
        if (rowIndex < 0 || currentCourses == null || rowIndex >= currentCourses.size()) {
            showErrorMessage("Vui lòng chọn một lớp học phần để xem danh sách sinh viên");
            return;
        }

        Course selectedCourse = currentCourses.get(rowIndex);
        viewStudentsListForCourse(selectedCourse);
    }

    private void viewStudentsList() {
        int selectedRow = courseTable.getSelectedRow();
        viewStudentsList(selectedRow);
    }

    private void viewStudentsListForCourse(Course course) {
        // Fetch enrollments for this course
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                request.addData("courseId", course.getCourseId());
                return serverConnection.sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Enrollment> enrollments = (List<Enrollment>) response.getData("enrollments");

                        // Show students in a dialog
                        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(CoursePanel.this),
                                "Danh sách sinh viên - " + course.getSubjectName(), true);
                        dialog.setSize(1000, 500);
                        dialog.setLocationRelativeTo(CoursePanel.this);

                        displayStudentsDialog(dialog, enrollments, course);
                        dialog.setVisible(true);
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
                        showErrorMessage("Không thể tải danh sách sinh viên: " + errorMsg);
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải danh sách sinh viên: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void displayStudentsDialog(JDialog dialog, List<Enrollment> enrollments, Course course) {
        String[] columnNames = { "MSSV", "Họ tên", "Điểm BT", "Điểm GK", "Điểm CK", "Điểm TK", "Xếp loại",
                "Tình trạng" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if (enrollments != null) {
            for (Enrollment en : enrollments) {
                // Get grades from grades table
                BigDecimal assignmentGrade = null;
                BigDecimal midtermGrade = null;
                BigDecimal finalExamGrade = null;

                try {
                    Message gradeRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
                    gradeRequest.addData(Constants.KEY_ENROLLMENT, en.getEnrollmentId());
                    Message gradeResponse = serverConnection.sendRequest(gradeRequest);

                    if (gradeResponse != null && gradeResponse.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Grade> grades = (List<com.university.sms.model.Grade>) gradeResponse
                                .getData(Constants.KEY_GRADES);

                        if (grades != null) {
                            for (com.university.sms.model.Grade grade : grades) {
                                if (grade.getGradeType() == com.university.sms.model.Grade.GradeType.ASSIGNMENT) {
                                    assignmentGrade = grade.getScore();
                                } else if (grade.getGradeType() == com.university.sms.model.Grade.GradeType.MIDTERM) {
                                    midtermGrade = grade.getScore();
                                } else if (grade.getGradeType() == com.university.sms.model.Grade.GradeType.FINAL) {
                                    finalExamGrade = grade.getScore();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Tính điểm tổng kết và xếp loại chỉ khi có đủ 3 điểm
                String finalGradeStr = "";
                String letterGrade = "";

                if (assignmentGrade != null && midtermGrade != null && finalExamGrade != null) {
                    // Tính theo tỷ lệ: BT 20%, GK 30%, CK 50%
                    BigDecimal totalGrade = assignmentGrade.multiply(new BigDecimal("0.2"))
                            .add(midtermGrade.multiply(new BigDecimal("0.3")))
                            .add(finalExamGrade.multiply(new BigDecimal("0.5")))
                            .setScale(2, BigDecimal.ROUND_HALF_UP);

                    finalGradeStr = String.format("%.2f", totalGrade);

                    // Xếp loại
                    double grade = totalGrade.doubleValue();
                    if (grade >= 9.0)
                        letterGrade = "A+";
                    else if (grade >= 8.5)
                        letterGrade = "A";
                    else if (grade >= 8.0)
                        letterGrade = "B+";
                    else if (grade >= 7.0)
                        letterGrade = "B";
                    else if (grade >= 6.5)
                        letterGrade = "C+";
                    else if (grade >= 5.5)
                        letterGrade = "C";
                    else if (grade >= 5.0)
                        letterGrade = "D+";
                    else if (grade >= 4.0)
                        letterGrade = "D";
                    else
                        letterGrade = "F";
                }

                Object[] row = {
                        en.getStudentCode() != null ? en.getStudentCode() : "N/A",
                        en.getStudentName() != null ? en.getStudentName() : "N/A",
                        assignmentGrade != null ? String.format("%.2f", assignmentGrade) : "",
                        midtermGrade != null ? String.format("%.2f", midtermGrade) : "",
                        finalExamGrade != null ? String.format("%.2f", finalExamGrade) : "",
                        finalGradeStr,
                        letterGrade,
                        en.getEnrollmentStatus() != null ? getStatusText(en.getEnrollmentStatus().toString()) : "N/A"
                };
                model.addRow(row);
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Center align columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columnNames.length; i++) {
            if (i != 1) { // Don't center student name
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);

        // Course info panel
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin lớp học"));
        infoPanel.add(new JLabel("Mã lớp:"));
        infoPanel.add(new JLabel(course.getCourseCode()));
        infoPanel.add(new JLabel("Môn học:"));
        infoPanel.add(new JLabel(course.getSubjectName()));
        infoPanel.add(new JLabel("Giáo viên:"));
        infoPanel.add(new JLabel(course.getTeacherName() != null ? course.getTeacherName() : "N/A"));
        infoPanel.add(new JLabel("Số lượng sinh viên:"));
        infoPanel.add(new JLabel(course.getCurrentStudents() + "/" + course.getMaxStudents()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    private String getStatusText(String status) {
        if (status == null)
            return "N/A";
        switch (status.toUpperCase()) {
            case "COMPLETED":
                return "Hoàn thành";
            case "FAILED":
                return "Không đạt";
            case "ENROLLED":
                return "Đang học";
            case "DROPPED":
                return "Bỏ học";
            default:
                return status;
        }
    }

    private void deleteCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorMessage("Vui lòng chọn một lớp học phần để hủy");
            return;
        }

        Course selectedCourse = currentCourses.get(selectedRow);

        String warningMessage = "Bạn có chắc chắn muốn HỦY lớp học phần:\n" +
                "Mã lớp: " + selectedCourse.getCourseCode() + "\n" +
                "Môn học: " + selectedCourse.getSubjectName() + "\n" +
                "Giáo viên: " + selectedCourse.getTeacherName() + "\n\n";

        if (selectedCourse.getCurrentStudents() > 0) {
            warningMessage += "⚠️ LƯU Ý: Lớp này có " + selectedCourse.getCurrentStudents() + " sinh viên đã đăng ký.\n"
                    +
                    "TẤT CẢ ĐĂNG KÝ và ĐIỂM của sinh viên sẽ BỊ XÓA!\n\n";
        }

        warningMessage += "Hành động này KHÔNG THỂ HOÀN TÁC!";

        int confirm = JOptionPane.showConfirmDialog(this,
                warningMessage,
                "Xác nhận hủy lớp học phần",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_DELETE_COURSE);
                request.addData(Constants.KEY_COURSE_ID, selectedCourse.getCourseId());
                return serverConnection.sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(CoursePanel.this,
                                "Đã hủy lớp học phần thành công",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    } else {
                        showErrorMessage("Không thể hủy lớp học phần: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi hủy lớp học phần: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openGradeEntryDialog() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorMessage("Vui lòng chọn một lớp để nhập điểm");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Chức năng nhập điểm đang được phát triển.\nVui lòng sử dụng tab 'Nhập Điểm' để nhập điểm cho sinh viên.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Custom cell renderer for button in table
     */
    private class ButtonCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private JButton button;

        public ButtonCellRenderer() {
            button = new JButton("👥 Xem danh sách");
            button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            button.setFocusPainted(false);
            button.setMargin(new Insets(2, 5, 2, 5));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (value == null) {
                return new JLabel("");
            }

            // Create a panel with student count and button
            JPanel panel = new JPanel(new BorderLayout(5, 0));
            panel.setOpaque(true);

            // Student count label
            String countText = value.toString();
            JLabel countLabel = new JLabel(countText);
            countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            countLabel.setHorizontalAlignment(JLabel.CENTER);

            // Button - bỏ icon, chỉ giữ text
            JButton renderButton = new JButton("Xem");
            renderButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            renderButton.setFocusPainted(false);
            renderButton.setMargin(new Insets(2, 5, 2, 5));
            renderButton.setPreferredSize(new Dimension(60, 25));

            panel.add(countLabel, BorderLayout.CENTER);
            panel.add(renderButton, BorderLayout.EAST);

            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
                countLabel.setForeground(table.getSelectionForeground());
            } else {
                panel.setBackground(table.getBackground());
                countLabel.setForeground(table.getForeground());
            }

            return panel;
        }
    }

    /**
     * Custom cell editor for button in table
     */
    private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private JPanel panel;
        private JLabel countLabel;
        private JButton button;
        private int currentRow;

        public ButtonCellEditor(JCheckBox checkBox) {
            panel = new JPanel(new BorderLayout(5, 0));
            panel.setOpaque(true);

            countLabel = new JLabel();
            countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            countLabel.setHorizontalAlignment(JLabel.CENTER);

            button = new JButton("Xem");
            button.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            button.setFocusPainted(false);
            button.setMargin(new Insets(2, 5, 2, 5));
            button.setPreferredSize(new Dimension(60, 25));

            button.addActionListener(e -> {
                fireEditingStopped();
                viewStudentsList(currentRow);
            });

            panel.add(countLabel, BorderLayout.CENTER);
            panel.add(button, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            currentRow = row;
            if (value != null) {
                countLabel.setText(value.toString());
            }
            panel.setBackground(table.getSelectionBackground());
            countLabel.setForeground(table.getSelectionForeground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return countLabel.getText();
        }
    }
}
