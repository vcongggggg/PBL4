package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;
import com.university.sms.model.Course;
import com.university.sms.model.Grade;
import com.university.sms.model.Enrollment;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel quản lý điểm số
 * - Admin/Teacher: Có thể nhập và sửa điểm
 * - Student: Chỉ xem điểm của mình
 */
public class GradePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable gradeTable;
    private DefaultTableModel tableModel;
    private JButton addGradeButton;
    private JButton editGradeButton;
    private JButton deleteGradeButton;
    private JButton refreshButton;
    private JTextField searchField;
    private JComboBox<String> courseFilterCombo;

    private List<Course> courses;

    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    public GradePanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
        // loadInitialData(); // Bỏ - để ComponentListener handle auto-refresh
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        String[] columnNames;
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            columnNames = new String[] { "Mã môn học", "Tên môn học", "Tín chỉ", "Điểm BT", "Điểm GK", "Điểm CK",
                    "Điểm TK", "Xếp loại" };
        } else {
            // Teacher/Admin: Show list of courses
            columnNames = new String[] { "Mã lớp", "Môn học", "Giáo viên", "Năm học", "Học kỳ", "Số sinh viên" };
        }

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        gradeTable = new JTable(tableModel);
        gradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gradeTable.setRowHeight(30);
        gradeTable.getTableHeader().setReorderingAllowed(false);
        gradeTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gradeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Double click to open grade input dialog for Teacher
        if (!isReadOnly && currentUser.getRole() == User.UserRole.TEACHER) {
            gradeTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        addGrade(); // Open grade input dialog
                    }
                }
            });
        }

        // Buttons
        addGradeButton = new JButton("Nhập điểm");
        refreshButton = new JButton("Làm mới");

        // Search
        searchField = new JTextField(20);
        searchField.setToolTipText("Tìm kiếm theo mã lớp hoặc tên môn");

        // Disable add for read-only mode
        if (isReadOnly) {
            addGradeButton.setEnabled(false);
        }

        // Event listeners
        addGradeButton.addActionListener(e -> addGrade());
        refreshButton.addActionListener(e -> refreshData());
        searchField.addActionListener(e -> searchCourses());

        // Update button state when selection changes
        gradeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }

    private void updateButtonStates() {
        int selectedRow = gradeTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;
        addGradeButton.setEnabled(hasSelection && !isReadOnly);
    }

    private void setupLayout() {
        // Top panel - Search and refresh
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        if (currentUser.getRole() == User.UserRole.STUDENT) {
            topPanel.add(new JLabel("Tìm kiếm:"));
            topPanel.add(searchField);
        } else {
            // Teacher/Admin: Search courses
            topPanel.add(new JLabel("Tìm kiếm lớp:"));
            topPanel.add(searchField);
        }
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        // Center - Table
        JScrollPane scrollPane = new JScrollPane(gradeTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom - Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        if (currentUser.getRole() == User.UserRole.STUDENT) {
            // Statistics for student
            JButton statsButton = new JButton("Thống kê điểm");
            statsButton.addActionListener(e -> showGradeStatistics());
            buttonPanel.add(statsButton);
        } else if (!isReadOnly) {
            // Teacher: Grade input button
            buttonPanel.add(addGradeButton);
            addGradeButton.setEnabled(false); // Disabled until a course is selected
        }

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        // Add component listener to refresh when panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (isInitialized && !isRefreshing) {
                    // Load courses for Teacher/Admin first
                    if (currentUser.getRole() != User.UserRole.STUDENT) {
                        loadCourses();
                    }
                    refreshData();
                }
            }
        });
    }

    private void loadInitialData() {
        refreshData();
        loadCourses();
    }

    public void refreshData() {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;
        tableModel.setRowCount(0);

        // For Teacher/Admin: Load courses instead of grades
        if (currentUser.getRole() != User.UserRole.STUDENT) {
            loadCourses();
            isRefreshing = false;
            return;
        }

        // For Student: Load grades
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                List<Map<String, Object>> gradeList = new ArrayList<>();

                if (currentUser.getRole() == User.UserRole.STUDENT) {
                    // Student: Get own grades
                    Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_GRADES);
                    Message response = serverConnection.sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Enrollment> enrollments = (List<Enrollment>) response.getData(Constants.KEY_GRADES);

                        if (enrollments != null) {
                            // Get detailed grades for each enrollment
                            for (Enrollment enrollment : enrollments) {
                                Map<String, Object> gradeMap = new HashMap<>();

                                // Get course info from enrollment
                                gradeMap.put("courseCode", enrollment.getCourseCode());
                                gradeMap.put("courseName", enrollment.getSubjectName());
                                gradeMap.put("credits", enrollment.getCredits());

                                // Get grade details from grades table
                                Message gradeRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
                                gradeRequest.addData(Constants.KEY_ENROLLMENT, enrollment.getEnrollmentId());
                                Message gradeResponse = serverConnection.sendRequest(gradeRequest);

                                BigDecimal assignmentGrade = null;
                                BigDecimal midtermGrade = null;
                                BigDecimal finalGrade = null;
                                BigDecimal totalGrade = enrollment.getFinalGrade();

                                if (gradeResponse != null && gradeResponse.isSuccess()) {
                                    @SuppressWarnings("unchecked")
                                    List<Grade> grades = (List<Grade>) gradeResponse.getData(Constants.KEY_GRADES);

                                    if (grades != null) {
                                        for (Grade grade : grades) {
                                            if (grade.getGradeType() == Grade.GradeType.ASSIGNMENT) {
                                                assignmentGrade = grade.getScore();
                                            } else if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                                                midtermGrade = grade.getScore();
                                            } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                                                finalGrade = grade.getScore();
                                            }
                                        }
                                    }
                                }

                                // Tính điểm tổng kết và xếp loại chỉ khi có đủ 3 điểm
                                if (assignmentGrade != null && midtermGrade != null && finalGrade != null) {
                                    // Tính theo tỷ lệ: BT 20%, GK 30%, CK 50%
                                    totalGrade = assignmentGrade.multiply(new BigDecimal("0.2"))
                                            .add(midtermGrade.multiply(new BigDecimal("0.3")))
                                            .add(finalGrade.multiply(new BigDecimal("0.5")))
                                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                                } else {
                                    totalGrade = null;
                                }

                                String letterGrade = "";
                                if (totalGrade != null) {
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

                                gradeMap.put("assignmentGrade", assignmentGrade);
                                gradeMap.put("midtermGrade", midtermGrade);
                                gradeMap.put("finalGrade", finalGrade);
                                gradeMap.put("totalGrade", totalGrade);
                                gradeMap.put("classification", letterGrade);

                                gradeList.add(gradeMap);
                            }
                        }
                    }
                } else {
                    // Admin/Teacher: Get enrollments by course filter
                    int selectedIndex = courseFilterCombo.getSelectedIndex();
                    if (selectedIndex > 0 && courses != null && selectedIndex <= courses.size()) {
                        Course selectedCourse = courses.get(selectedIndex - 1);

                        Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                        request.addData("courseId", selectedCourse.getCourseId());
                        Message response = serverConnection.sendRequest(request);

                        if (response != null && response.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<Enrollment> enrollments = (List<Enrollment>) response.getData("enrollments");

                            if (enrollments != null) {
                                for (Enrollment enrollment : enrollments) {
                                    Map<String, Object> gradeMap = new HashMap<>();
                                    gradeMap.put("studentId", enrollment.getStudentCode());
                                    gradeMap.put("studentName", enrollment.getStudentName());
                                    gradeMap.put("courseCode", enrollment.getCourseCode());
                                    gradeMap.put("courseName", enrollment.getSubjectName());

                                    // Get grade details from grades table
                                    Message gradeRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
                                    gradeRequest.addData(Constants.KEY_ENROLLMENT, enrollment.getEnrollmentId());
                                    Message gradeResponse = serverConnection.sendRequest(gradeRequest);

                                    BigDecimal assignmentGrade = null;
                                    BigDecimal midtermGrade = null;
                                    BigDecimal finalGrade = null;

                                    if (gradeResponse != null && gradeResponse.isSuccess()) {
                                        @SuppressWarnings("unchecked")
                                        List<Grade> grades = (List<Grade>) gradeResponse.getData(Constants.KEY_GRADES);

                                        if (grades != null) {
                                            for (Grade grade : grades) {
                                                if (grade.getGradeType() == Grade.GradeType.ASSIGNMENT) {
                                                    assignmentGrade = grade.getScore();
                                                } else if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                                                    midtermGrade = grade.getScore();
                                                } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                                                    finalGrade = grade.getScore();
                                                }
                                            }
                                        }
                                    }

                                    // Tính điểm tổng kết và xếp loại chỉ khi có đủ 3 điểm
                                    BigDecimal totalGrade = null;
                                    if (assignmentGrade != null && midtermGrade != null && finalGrade != null) {
                                        // Tính theo tỷ lệ: BT 20%, GK 30%, CK 50%
                                        totalGrade = assignmentGrade.multiply(new BigDecimal("0.2"))
                                                .add(midtermGrade.multiply(new BigDecimal("0.3")))
                                                .add(finalGrade.multiply(new BigDecimal("0.5")))
                                                .setScale(2, BigDecimal.ROUND_HALF_UP);
                                    }

                                    String letterGrade = "";
                                    if (totalGrade != null) {
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

                                    gradeMap.put("assignmentGrade", assignmentGrade);
                                    gradeMap.put("midtermGrade", midtermGrade);
                                    gradeMap.put("finalGrade", finalGrade);
                                    gradeMap.put("totalGrade", totalGrade);
                                    gradeMap.put("classification", letterGrade);

                                    gradeList.add(gradeMap);
                                }
                            }
                        }
                    }
                }

                return gradeList;
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> grades = get();
                    updateTable(grades);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(GradePanel.this,
                            "Lỗi khi tải dữ liệu điểm: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    private String calculateLetterGrade(BigDecimal grade) {
        if (grade == null)
            return "N/A";

        double score = grade.doubleValue();
        if (score >= 9.0)
            return "A+";
        if (score >= 8.5)
            return "A";
        if (score >= 8.0)
            return "B+";
        if (score >= 7.0)
            return "B";
        if (score >= 6.5)
            return "C+";
        if (score >= 6.0)
            return "C";
        if (score >= 5.5)
            return "D+";
        if (score >= 5.0)
            return "D";
        if (score >= 4.0)
            return "F+";
        return "F";
    }

    private void loadCourses() {
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                // Teacher: chỉ load các khóa học mà giáo viên dạy
                if (currentUser.getRole() == User.UserRole.TEACHER) {
                    Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
                    request.addData("teacherId", currentUser.getUserId());
                    Message response = serverConnection.sendRequest(request);
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Course> courseList = (List<Course>) response.getData(Constants.KEY_COURSES);
                        return courseList != null ? courseList : List.of();
                    }
                    return List.of();
                } else {
                    // Admin: load tất cả khóa học
                    Message response = serverConnection.getAllCourses();
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Course> courseList = (List<Course>) response.getData(Constants.KEY_COURSES);
                        return courseList != null ? courseList : List.of();
                    }
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    courses = get();
                    tableModel.setRowCount(0);

                    if (courses != null && !courses.isEmpty()) {
                        for (Course course : courses) {
                            Object[] row = {
                                    course.getCourseCode(),
                                    course.getSubjectName(),
                                    course.getTeacherName() != null ? course.getTeacherName() : "N/A",
                                    course.getAcademicYear(),
                                    course.getSemester(),
                                    course.getCurrentStudents() + "/" + course.getMaxStudents()
                            };
                            tableModel.addRow(row);
                        }
                    } else {
                        JOptionPane.showMessageDialog(GradePanel.this,
                                "Không có lớp học phần nào",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(GradePanel.this,
                            "Lỗi khi tải danh sách khóa học: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void updateTable(List<Map<String, Object>> grades) {
        tableModel.setRowCount(0);

        for (Map<String, Object> grade : grades) {
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                tableModel.addRow(new Object[] {
                        grade.get("courseCode"),
                        grade.get("courseName"),
                        grade.get("credits"),
                        formatGrade(grade.get("assignmentGrade")),
                        formatGrade(grade.get("midtermGrade")),
                        formatGrade(grade.get("finalGrade")),
                        formatGrade(grade.get("totalGrade")),
                        grade.get("classification") != null ? grade.get("classification") : ""
                });
            } else {
                tableModel.addRow(new Object[] {
                        grade.get("studentId"),
                        grade.get("studentName"),
                        grade.get("courseCode"),
                        grade.get("courseName"),
                        formatGrade(grade.get("assignmentGrade")),
                        formatGrade(grade.get("midtermGrade")),
                        formatGrade(grade.get("finalGrade")),
                        formatGrade(grade.get("totalGrade")),
                        grade.get("classification") != null ? grade.get("classification") : ""
                });
            }
        }
    }

    private String formatGrade(Object grade) {
        if (grade == null) {
            return "";
        }
        if (grade instanceof BigDecimal) {
            return String.format("%.2f", ((BigDecimal) grade).doubleValue());
        }
        return grade.toString();
    }

    private void addGrade() {
        // Kiểm tra xem đã chọn lớp chưa
        int selectedRow = gradeTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn lớp học phần để nhập điểm",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (courses == null || selectedRow >= courses.size()) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy thông tin lớp học",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Course selectedCourse = courses.get(selectedRow);

        // Hiển thị dialog nhập điểm cho sinh viên
        showStudentSelectionDialog(selectedCourse);
    }

    private void editGrade() {
        int selectedRow = gradeTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một điểm để sửa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // TODO: Get grade data from selected row and show edit dialog
        JOptionPane.showMessageDialog(this,
                "Chức năng sửa điểm đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteGrade() {
        int selectedRow = gradeTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một điểm để xóa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa điểm này?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // TODO: Implement delete functionality
            JOptionPane.showMessageDialog(this,
                    "Chức năng xóa điểm đang được phát triển",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void searchCourses() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            loadCourses();
            return;
        }

        tableModel.setRowCount(0);
        if (courses != null) {
            for (Course course : courses) {
                if (course.getCourseCode().toLowerCase().contains(keyword) ||
                        course.getSubjectName().toLowerCase().contains(keyword)) {
                    Object[] row = {
                            course.getCourseCode(),
                            course.getSubjectName(),
                            course.getTeacherName() != null ? course.getTeacherName() : "N/A",
                            course.getAcademicYear(),
                            course.getSemester(),
                            course.getCurrentStudents() + "/" + course.getMaxStudents()
                    };
                    tableModel.addRow(row);
                }
            }
        }
    }

    private void showGradeStatistics() {
        // TODO: Show statistics dialog
        JOptionPane.showMessageDialog(this,
                "Thống kê điểm:\n" +
                        "- Điểm TB tích lũy: Đang tính...\n" +
                        "- Số tín chỉ đã đạt: Đang tính...\n" +
                        "- Xếp loại: Đang tính...\n\n" +
                        "Chức năng đang được phát triển",
                "Thống kê điểm",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Hiển thị dialog chọn sinh viên và nhập điểm
     */
    private void showStudentSelectionDialog(Course course) {
        SwingWorker<List<Enrollment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Enrollment> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                request.addData("courseId", course.getCourseId());
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Enrollment> enrollments = (List<Enrollment>) response.getData("enrollments");
                    return enrollments != null ? enrollments : new ArrayList<>();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    List<Enrollment> enrollments = get();
                    if (enrollments.isEmpty()) {
                        JOptionPane.showMessageDialog(GradePanel.this,
                                "Không có sinh viên nào trong lớp này",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    // Show dialog to select student and input grades
                    showGradeInputDialog(course, enrollments);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(GradePanel.this,
                            "Lỗi khi tải danh sách sinh viên: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Hiển thị dialog nhập điểm cho sinh viên
     */
    private void showGradeInputDialog(Course course, List<Enrollment> enrollments) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Nhập điểm - " + course.getSubjectName(), true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        // Create table for grade input
        String[] columnNames = { "MSSV", "Họ tên", "Điểm BT", "Điểm GK", "Điểm CK" };
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                // Only grade columns are editable
                return column >= 2 && column <= 4;
            }
        };

        // Load existing grades and populate table
        for (Enrollment enrollment : enrollments) {
            // Get existing grades
            BigDecimal assignmentGrade = null;
            BigDecimal midtermGrade = null;
            BigDecimal finalGrade = null;

            try {
                Message gradeRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
                gradeRequest.addData(Constants.KEY_ENROLLMENT, enrollment.getEnrollmentId());
                Message gradeResponse = serverConnection.sendRequest(gradeRequest);

                if (gradeResponse != null && gradeResponse.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Grade> grades = (List<Grade>) gradeResponse.getData(Constants.KEY_GRADES);

                    if (grades != null) {
                        for (Grade grade : grades) {
                            if (grade.getGradeType() == Grade.GradeType.ASSIGNMENT) {
                                assignmentGrade = grade.getScore();
                            } else if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                                midtermGrade = grade.getScore();
                            } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                                finalGrade = grade.getScore();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Object[] row = {
                    enrollment.getStudentCode(),
                    enrollment.getStudentName(),
                    assignmentGrade != null ? String.format("%.2f", assignmentGrade) : "",
                    midtermGrade != null ? String.format("%.2f", midtermGrade) : "",
                    finalGrade != null ? String.format("%.2f", finalGrade) : ""
            };
            tableModel.addRow(row);
        }

        JTable gradeInputTable = new JTable(tableModel);
        gradeInputTable.setRowHeight(30);
        gradeInputTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gradeInputTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Set column widths
        gradeInputTable.getColumnModel().getColumn(0).setPreferredWidth(80); // MSSV
        gradeInputTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Name
        gradeInputTable.getColumnModel().getColumn(2).setPreferredWidth(80); // BT
        gradeInputTable.getColumnModel().getColumn(3).setPreferredWidth(80); // GK
        gradeInputTable.getColumnModel().getColumn(4).setPreferredWidth(80); // CK

        JScrollPane scrollPane = new JScrollPane(gradeInputTable);

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin lớp học"));
        infoPanel.add(new JLabel("Mã lớp:"));
        infoPanel.add(new JLabel(course.getCourseCode()));
        infoPanel.add(new JLabel("Môn học:"));
        infoPanel.add(new JLabel(course.getSubjectName()));
        infoPanel.add(new JLabel("Số sinh viên:"));
        infoPanel.add(new JLabel(String.valueOf(enrollments.size())));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Lưu tất cả");
        JButton cancelButton = new JButton("Hủy");

        saveButton.addActionListener(e -> {
            // QUAN TRỌNG: Dừng editing để commit giá trị vào model
            if (gradeInputTable.isEditing()) {
                TableCellEditor editor = gradeInputTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
            }
            saveAllGrades(dialog, enrollments, gradeInputTable, tableModel);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Lưu tất cả điểm vào database
     */
    private void saveAllGrades(JDialog dialog, List<Enrollment> enrollments, JTable gradeInputTable,
            DefaultTableModel tableModel) {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                Enrollment enrollment = enrollments.get(i);

                // Lấy giá trị từ table model (đã được commit sau stopCellEditing)
                Object assignmentObj = tableModel.getValueAt(i, 2);
                Object midtermObj = tableModel.getValueAt(i, 3);
                Object finalObj = tableModel.getValueAt(i, 4);

                String assignmentStr = assignmentObj != null ? assignmentObj.toString().trim() : "";
                String midtermStr = midtermObj != null ? midtermObj.toString().trim() : "";
                String finalStr = finalObj != null ? finalObj.toString().trim() : "";

                System.out.println("DEBUG: Row " + i + " - MSSV: " + enrollment.getStudentCode());
                System.out.println("  Assignment: '" + assignmentStr + "'");
                System.out.println("  Midterm: '" + midtermStr + "'");
                System.out.println("  Final: '" + finalStr + "'");

                // Save each grade type separately (or delete if empty)
                int changesCount = 0;

                // Assignment grade - save or delete
                boolean result1 = saveOrDeleteGrade(enrollment.getEnrollmentId(),
                        Grade.GradeType.ASSIGNMENT, assignmentStr);
                System.out.println("  Result Assignment: " + result1);
                if (result1)
                    changesCount++;

                // Midterm grade - save or delete
                boolean result2 = saveOrDeleteGrade(enrollment.getEnrollmentId(),
                        Grade.GradeType.MIDTERM, midtermStr);
                System.out.println("  Result Midterm: " + result2);
                if (result2)
                    changesCount++;

                // Final grade - save or delete
                boolean result3 = saveOrDeleteGrade(enrollment.getEnrollmentId(),
                        Grade.GradeType.FINAL, finalStr);
                System.out.println("  Result Final: " + result3);
                if (result3)
                    changesCount++;

                if (changesCount > 0) {
                    successCount++;
                }

            } catch (NumberFormatException e) {
                errorCount++;
                String errorMsg = "Điểm không hợp lệ: " + e.getMessage();
                errors.append("Dòng ").append(i + 1).append(": ").append(errorMsg).append("\n");
                System.err.println("ERROR Row " + i + ": " + errorMsg);
                e.printStackTrace();
            } catch (Exception e) {
                errorCount++;
                errors.append("Dòng ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
                System.err.println("ERROR Row " + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Show result
        String message;
        int messageType;

        if (errorCount == 0) {
            message = "Đã lưu điểm thành công cho " + successCount + " sinh viên";
            messageType = JOptionPane.INFORMATION_MESSAGE;
        } else {
            message = "Lưu thành công: " + successCount + " sinh viên\n" +
                    "Lỗi: " + errorCount + " sinh viên\n\n" +
                    "Chi tiết lỗi:\n" + errors.toString();
            messageType = JOptionPane.WARNING_MESSAGE;
        }

        // Hiển thị thông báo kết quả
        JOptionPane.showMessageDialog(dialog, message, "Kết quả", messageType);

        // Refresh dữ liệu nhưng giữ dialog mở để có thể tiếp tục chỉnh sửa
        refreshData();
    }

    /**
     * Lưu hoặc xóa điểm
     */
    private boolean saveOrDeleteGrade(int enrollmentId, Grade.GradeType gradeType, String scoreStr) {
        // If empty, send null score to delete the grade
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return deleteGradeBySettingNull(enrollmentId, gradeType);
        }
        // Otherwise, save/update the grade
        return saveGradeByType(enrollmentId, gradeType, scoreStr.trim());
    }

    /**
     * Xóa điểm bằng cách gọi DELETE_GRADE
     */
    private boolean deleteGradeBySettingNull(int enrollmentId, Grade.GradeType gradeType) {
        try {
            // Find existing grade
            Message getRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
            getRequest.addData(Constants.KEY_ENROLLMENT, enrollmentId);
            Message getResponse = serverConnection.sendRequest(getRequest);

            if (getResponse != null && getResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Grade> grades = (List<Grade>) getResponse.getData(Constants.KEY_GRADES);

                if (grades != null) {
                    for (Grade grade : grades) {
                        if (grade.getGradeType() == gradeType && grade.getGradeId() > 0) {
                            // Delete this grade
                            Message deleteRequest = Message.createRequest(Constants.ACTION_DELETE_GRADE);
                            deleteRequest.addData(Constants.KEY_GRADE_ID, grade.getGradeId());
                            Message deleteResponse = serverConnection.sendRequest(deleteRequest);
                            return deleteResponse != null && deleteResponse.isSuccess();
                        }
                    }
                }
            }
            return true; // No grade to delete is also success
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu điểm theo loại - kiểm tra xem đã có chưa để ADD hoặc UPDATE
     */
    private boolean saveGradeByType(int enrollmentId, Grade.GradeType gradeType, String scoreStr) {
        try {
            BigDecimal score = new BigDecimal(scoreStr);
            System.out.println("DEBUG saveGradeByType: score parsed = " + score);

            // Kiểm tra xem điểm đã tồn tại chưa
            Message getRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
            getRequest.addData(Constants.KEY_ENROLLMENT, enrollmentId);
            Message getResponse = serverConnection.sendRequest(getRequest);

            Grade existingGrade = null;
            if (getResponse != null && getResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Grade> grades = (List<Grade>) getResponse.getData(Constants.KEY_GRADES);

                System.out.println("DEBUG: Found " + (grades != null ? grades.size() : 0) + " existing grades");

                if (grades != null) {
                    for (Grade g : grades) {
                        if (g.getGradeType() == gradeType) {
                            existingGrade = g;
                            System.out.println("DEBUG: Found existing grade ID: " + g.getGradeId());
                            break;
                        }
                    }
                }
            }

            // Create/Update grade object
            Grade grade = existingGrade != null ? existingGrade : new Grade();
            grade.setEnrollmentId(enrollmentId);
            grade.setGradeType(gradeType);
            grade.setScore(score);
            grade.setMaxScore(new BigDecimal("10.0"));

            // Set weight and name based on type
            switch (gradeType) {
                case ASSIGNMENT:
                    grade.setWeight(new BigDecimal("0.2")); // 20%
                    grade.setGradeName("Điểm bài tập");
                    break;
                case MIDTERM:
                    grade.setWeight(new BigDecimal("0.3")); // 30%
                    grade.setGradeName("Điểm giữa kỳ");
                    break;
                case FINAL:
                    grade.setWeight(new BigDecimal("0.5")); // 50%
                    grade.setGradeName("Điểm cuối kỳ");
                    break;
                default:
                    System.err.println("ERROR: Unknown grade type: " + gradeType);
                    return false;
            }

            grade.setGradeDate(new java.sql.Date(System.currentTimeMillis()));

            // Send to server - ADD or UPDATE
            String action = existingGrade != null ? Constants.ACTION_UPDATE_GRADE : Constants.ACTION_ADD_GRADE;
            System.out.println("DEBUG: Sending " + action + " request for grade type " + gradeType);

            Message request = Message.createRequest(action);
            request.addData(Constants.KEY_GRADE, grade);
            Message response = serverConnection.sendRequest(request);

            boolean success = response != null && response.isSuccess();
            if (!success && response != null) {
                System.err.println("ERROR: Server response failed: " + response.getMessage());
            }

            return success;

        } catch (Exception e) {
            System.err.println("ERROR in saveGradeByType: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
