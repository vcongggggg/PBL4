package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Panel báo cáo và thống kê
 * - Admin: Báo cáo tổng hợp toàn hệ thống
 * - Teacher: Báo cáo lớp học của mình
 * - Student: Không có access
 */
public class ReportPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    private JTabbedPane tabbedPane;
    private AnalyticsDashboard analyticsDashboard;
    private JComboBox<String> reportTypeCombo;
    private JComboBox<String> semesterCombo;
    private JButton generateButton;
    private JButton exportButton;
    private JTextArea reportTextArea;

    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    public ReportPanel(User currentUser, IServerConnection serverConnection) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
        // loadInitialData(); // Bỏ - để ComponentListener handle auto-refresh
    }

    private void setupEventListeners() {
        // Add component listener to refresh when panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (isInitialized && !isRefreshing) {
                    refreshData();
                }
            }
        });
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();

        // Tab 1: Advanced Analytics Dashboard (cho Admin/Teacher)
        if (currentUser.getRole() != User.UserRole.STUDENT) {
            analyticsDashboard = new AnalyticsDashboard(serverConnection, currentUser, true, true);
            tabbedPane.addTab("Thống kê", analyticsDashboard);
        } else {
            // Student chỉ thấy statistics đơn giản
            JPanel statisticsPanel = createStatisticsPanel();
            tabbedPane.addTab("Thống kê", statisticsPanel);
        }

        // Tab 2: Detailed Reports
        JPanel detailReportPanel = createDetailReportPanel();
        tabbedPane.addTab("Báo cáo Chi tiết", detailReportPanel);
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Statistics cards - will be updated with real data
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel statCard1 = createStatCard("Tổng số Sinh viên", "0", Color.BLUE);
        JPanel statCard2 = createStatCard("Tổng số Giảng viên", "0", Color.GREEN);
        JPanel statCard3 = createStatCard("Tổng số Môn học", "0", Color.ORANGE);
        JPanel statCard4 = createStatCard("Tổng số Lớp học", "0", Color.RED);

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            cardsPanel.add(statCard1);
            cardsPanel.add(statCard2);
            cardsPanel.add(statCard3);
            cardsPanel.add(statCard4);
        } else if (currentUser.getRole() == User.UserRole.TEACHER) {
            statCard1 = createStatCard("Số lớp đang dạy", "0", Color.BLUE);
            statCard2 = createStatCard("Tổng số Sinh viên", "0", Color.GREEN);
            statCard3 = createStatCard("Điểm TB lớp", "0.0", Color.ORANGE);
            statCard4 = createStatCard("Tỷ lệ đậu", "0%", Color.RED);
            cardsPanel.add(statCard1);
            cardsPanel.add(statCard2);
            cardsPanel.add(statCard3);
            cardsPanel.add(statCard4);
        }

        // Quick stats table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Thống kê nhanh"));

        DefaultTableModel tableModel = new DefaultTableModel();
        JTable table = new JTable(tableModel);
        table.setEnabled(false);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(cardsPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);

        // Load real data
        loadStatisticsData(statCard1, statCard2, statCard3, statCard4, tableModel);

        return panel;
    }

    private void loadStatisticsData(JPanel card1, JPanel card2, JPanel card3, JPanel card4, DefaultTableModel tableModel) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (currentUser.getRole() == User.UserRole.ADMIN) {
                        Message studentsRequest = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
                        Message studentsResponse = serverConnection.sendRequest(studentsRequest);
                        
                        Message teachersRequest = Message.createRequest(Constants.ACTION_GET_ALL_TEACHERS);
                        Message teachersResponse = serverConnection.sendRequest(teachersRequest);
                        
                        Message subjectsRequest = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
                        Message subjectsResponse = serverConnection.sendRequest(subjectsRequest);
                        
                        Message classesRequest = Message.createRequest(Constants.ACTION_GET_CLASSES);
                        Message classesResponse = serverConnection.sendRequest(classesRequest);
                        
                        Message facultiesRequest = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
                        Message facultiesResponse = serverConnection.sendRequest(facultiesRequest);

                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Student> students = 
                            studentsResponse != null && studentsResponse.isSuccess() ? 
                            (List<com.university.sms.model.Student>) studentsResponse.getData("students") : null;
                        
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.User> teachers = 
                            teachersResponse != null && teachersResponse.isSuccess() ? 
                            (List<com.university.sms.model.User>) teachersResponse.getData("teachers") : null;
                        
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Subject> subjects = 
                            subjectsResponse != null && subjectsResponse.isSuccess() ? 
                            (List<com.university.sms.model.Subject>) subjectsResponse.getData(Constants.KEY_SUBJECTS) : null;
                        
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Class> classes = 
                            classesResponse != null && classesResponse.isSuccess() ? 
                            (List<com.university.sms.model.Class>) classesResponse.getData("classes") : null;
                        
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Faculty> faculties = 
                            facultiesResponse != null && facultiesResponse.isSuccess() ? 
                            (List<com.university.sms.model.Faculty>) facultiesResponse.getData("faculties") : null;

                        SwingUtilities.invokeLater(() -> {
                            updateStatCard(card1, String.valueOf(students != null ? students.size() : 0));
                            updateStatCard(card2, String.valueOf(teachers != null ? teachers.size() : 0));
                            updateStatCard(card3, String.valueOf(subjects != null ? subjects.size() : 0));
                            updateStatCard(card4, String.valueOf(classes != null ? classes.size() : 0));
                            
                            // Update table
                            if (faculties != null) {
                                tableModel.setColumnIdentifiers(new Object[] { "Khoa", "Số SV", "Số GV", "GPA TB" });
                                for (com.university.sms.model.Faculty fac : faculties) {
                                    int studentCount = 0;
                                    if (students != null) {
                                        for (com.university.sms.model.Student s : students) {
                                            if (fac.getFacultyCode().equals(s.getFacultyCode())) {
                                                studentCount++;
                                            }
                                        }
                                    }
                                    tableModel.addRow(new Object[] { 
                                        fac.getFacultyName(), 
                                        studentCount, 
                                        "N/A", 
                                        "N/A" 
                                    });
                                }
                            }
                        });
                    } else if (currentUser.getRole() == User.UserRole.TEACHER) {
                        Message coursesRequest = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
                        Message coursesResponse = serverConnection.sendRequest(coursesRequest);
                        
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Course> courses = 
                            coursesResponse != null && coursesResponse.isSuccess() ? 
                            (List<com.university.sms.model.Course>) coursesResponse.getData("courses") : null;

                        int totalStudents = 0;
                        double totalGrade = 0;
                        int totalGrades = 0;
                        int passedCount = 0;
                        
                        if (courses != null) {
                            for (com.university.sms.model.Course course : courses) {
                                Message enrollRequest = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                                enrollRequest.addData(Constants.KEY_COURSE_ID, course.getCourseId());
                                Message enrollResponse = serverConnection.sendRequest(enrollRequest);
                                
                                if (enrollResponse != null && enrollResponse.isSuccess()) {
                                    @SuppressWarnings("unchecked")
                                    List<com.university.sms.model.Enrollment> enrollments = 
                                        (List<com.university.sms.model.Enrollment>) enrollResponse.getData("enrollments");
                                    if (enrollments != null) {
                                        totalStudents += enrollments.size();
                                        for (com.university.sms.model.Enrollment e : enrollments) {
                                            if (e.getFinalGrade() != null) {
                                                totalGrade += e.getFinalGrade().doubleValue();
                                                totalGrades++;
                                                if (e.getFinalGrade().doubleValue() >= 5.0) {
                                                    passedCount++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        final int finalCourses = courses != null ? courses.size() : 0;
                        final int finalStudents = totalStudents;
                        final double avgGrade = totalGrades > 0 ? totalGrade / totalGrades : 0;
                        final double passRate = totalStudents > 0 ? (passedCount * 100.0 / totalStudents) : 0;
                        
                        SwingUtilities.invokeLater(() -> {
                            updateStatCard(card1, String.valueOf(finalCourses));
                            updateStatCard(card2, String.valueOf(finalStudents));
                            updateStatCard(card3, String.format("%.1f", avgGrade));
                            updateStatCard(card4, String.format("%.1f%%", passRate));
                            
                            // Update table
                            if (courses != null) {
                                tableModel.setColumnIdentifiers(new Object[] { "Môn học", "Số SV", "Điểm TB", "Tỷ lệ đậu" });
                                for (com.university.sms.model.Course course : courses) {
                                    tableModel.addRow(new Object[] { 
                                        course.getSubjectName() != null ? course.getSubjectName() : course.getSubjectCode(),
                                        "N/A", 
                                        "N/A", 
                                        "N/A" 
                                    });
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void updateStatCard(JPanel card, String value) {
        if (card != null && card.getComponentCount() > 0) {
            Component[] components = card.getComponents();
            for (Component comp : components) {
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    if (label.getFont() != null && label.getFont().getSize() > 20) {
                        label.setText(value);
                        break;
                    }
                }
            }
        }
    }

    private JPanel createDetailReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controlPanel.add(new JLabel("Loại báo cáo:"));
        reportTypeCombo = new JComboBox<>();

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            reportTypeCombo.addItem("Báo cáo tổng hợp");
            reportTypeCombo.addItem("Báo cáo sinh viên");
            reportTypeCombo.addItem("Báo cáo giảng viên");
            reportTypeCombo.addItem("Báo cáo học vụ");
            reportTypeCombo.addItem("Báo cáo tài chính");
        } else {
            reportTypeCombo.addItem("Báo cáo lớp học");
            reportTypeCombo.addItem("Báo cáo điểm danh");
            reportTypeCombo.addItem("Báo cáo kết quả học tập");
        }

        controlPanel.add(reportTypeCombo);

        controlPanel.add(new JLabel("Học kỳ:"));
        semesterCombo = new JComboBox<>(new String[] {
                "HK1 2024-2025",
                "HK2 2023-2024",
                "HK1 2023-2024",
                "HK2 2022-2023"
        });
        controlPanel.add(semesterCombo);

        generateButton = new JButton("Tạo báo cáo");
        generateButton.addActionListener(e -> generateReport());
        controlPanel.add(generateButton);

        exportButton = new JButton("Xuất Excel");
        exportButton.addActionListener(e -> exportReport());
        controlPanel.add(exportButton);

        // Report display area
        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportTextArea.setText("Chọn loại báo cáo và nhấn 'Tạo báo cáo' để xem kết quả");

        JScrollPane scrollPane = new JScrollPane(reportTextArea);

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void setupLayout() {
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadInitialData() {
        // Load initial statistics
    }

    public void refreshData() {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;

        SwingUtilities.invokeLater(() -> {
            try {
                loadInitialData();
                if (analyticsDashboard != null) {
                    analyticsDashboard.refreshData();
                }
            } finally {
                isRefreshing = false;
            }
        });
    }

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        String semester = (String) semesterCombo.getSelectedItem();

        reportTextArea.setText("Đang tạo báo cáo...");
        reportTextArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        generateButton.setEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    StringBuilder report = new StringBuilder();
                    report.append("===================================================\n");
                    report.append("         ").append(reportType.toUpperCase()).append("\n");
                    report.append("===================================================\n\n");
                    report.append("Học kỳ: ").append(semester).append("\n");
                    report.append("Ngày tạo: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
                    report.append("Người tạo: ").append(currentUser.getFullName()).append("\n\n");
                    report.append("---------------------------------------------------\n\n");

                    if (currentUser.getRole() == User.UserRole.ADMIN) {
                        report.append(generateAdminReport(reportType, semester));
                    } else if (currentUser.getRole() == User.UserRole.TEACHER) {
                        report.append(generateTeacherReport(reportType, semester));
                    }

                    report.append("---------------------------------------------------\n");
                    report.append("\n===================================================\n");
                    return report.toString();
                } catch (Exception e) {
                    return "Lỗi khi tạo báo cáo: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String report = get();
                    reportTextArea.setText(report);
                } catch (Exception e) {
                    reportTextArea.setText("Lỗi khi tạo báo cáo: " + e.getMessage());
                } finally {
                    reportTextArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    generateButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private String generateAdminReport(String reportType, String semester) {
        StringBuilder report = new StringBuilder();

        try {
            // Get all data
            Message studentsRequest = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
            Message studentsResponse = serverConnection.sendRequest(studentsRequest);
            
            Message teachersRequest = Message.createRequest(Constants.ACTION_GET_ALL_TEACHERS);
            Message teachersResponse = serverConnection.sendRequest(teachersRequest);
            
            Message subjectsRequest = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
            Message subjectsResponse = serverConnection.sendRequest(subjectsRequest);
            
            Message classesRequest = Message.createRequest(Constants.ACTION_GET_CLASSES);
            Message classesResponse = serverConnection.sendRequest(classesRequest);
            
            Message facultiesRequest = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
            Message facultiesResponse = serverConnection.sendRequest(facultiesRequest);

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Student> students = 
                studentsResponse != null && studentsResponse.isSuccess() ? 
                (List<com.university.sms.model.Student>) studentsResponse.getData("students") : null;
            
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.User> teachers = 
                teachersResponse != null && teachersResponse.isSuccess() ? 
                (List<com.university.sms.model.User>) teachersResponse.getData("teachers") : null;
            
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Subject> subjects = 
                subjectsResponse != null && subjectsResponse.isSuccess() ? 
                (List<com.university.sms.model.Subject>) subjectsResponse.getData(Constants.KEY_SUBJECTS) : null;
            
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Class> classes = 
                classesResponse != null && classesResponse.isSuccess() ? 
                (List<com.university.sms.model.Class>) classesResponse.getData("classes") : null;
            
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Faculty> faculties = 
                facultiesResponse != null && facultiesResponse.isSuccess() ? 
                (List<com.university.sms.model.Faculty>) facultiesResponse.getData("faculties") : null;

            if (reportType.contains("Tổng hợp") || reportType.contains("tổng hợp")) {
                report.append("I. TỔNG QUAN\n\n");
                report.append("   1. Tổng số sinh viên: ").append(students != null ? students.size() : 0).append("\n");
                report.append("   2. Tổng số giảng viên: ").append(teachers != null ? teachers.size() : 0).append("\n");
                report.append("   3. Tổng số môn học: ").append(subjects != null ? subjects.size() : 0).append("\n");
                report.append("   4. Tổng số lớp học: ").append(classes != null ? classes.size() : 0).append("\n\n");

                if (faculties != null) {
                    report.append("II. THỐNG KÊ THEO KHOA\n\n");
                    report.append(String.format("   %-30s %10s %10s %10s\n", "Khoa", "Sinh viên", "Giảng viên", "GPA TB"));
                    report.append("   " + "-".repeat(70) + "\n");
                    
                    for (com.university.sms.model.Faculty fac : faculties) {
                        int studentCount = 0;
                        int teacherCount = 0;
                        
                        if (students != null) {
                            for (com.university.sms.model.Student s : students) {
                                if (fac.getFacultyCode().equals(s.getFacultyCode())) {
                                    studentCount++;
                                }
                            }
                        }
                        
                        if (teachers != null) {
                            // Count teachers by checking their subjects
                            for (com.university.sms.model.User t : teachers) {
                                if (t.getRole() == User.UserRole.TEACHER) {
                                    // Check if teacher teaches subjects in this faculty
                                    if (subjects != null) {
                                        for (com.university.sms.model.Subject sub : subjects) {
                                            if (fac.getFacultyCode().equals(sub.getFacultyCode())) {
                                                teacherCount++;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Get faculty statistics for GPA
                        Message statRequest = new Message();
                        statRequest.setAction(Constants.ACTION_GET_FACULTY_STATISTICS);
                        java.util.Map<String, Object> reqData = new java.util.HashMap<>();
                        reqData.put("facultyCode", fac.getFacultyCode());
                        statRequest.setData(reqData);
                        Message statResponse = serverConnection.sendRequest(statRequest);
                        
                        String gpaStr = "N/A";
                        if (statResponse != null && statResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> stats = 
                                (java.util.Map<String, Object>) statResponse.getData(Constants.KEY_STATISTICS);
                            if (stats != null && stats.get("averageGPA") != null) {
                                double gpa = ((Number) stats.get("averageGPA")).doubleValue();
                                gpaStr = String.format("%.2f", gpa);
                            }
                        }
                        
                        String facName = fac.getFacultyName();
                        if (facName.length() > 30) {
                            facName = facName.substring(0, 27) + "...";
                        }
                        report.append(String.format("   %-30s %10d %10d %10s\n", facName, studentCount, teacherCount, gpaStr));
                    }
                    report.append("\n");
                }
            } else if (reportType.contains("Sinh viên") || reportType.contains("sinh viên")) {
                report.append("I. DANH SÁCH SINH VIÊN\n\n");
                if (students != null && !students.isEmpty()) {
                    report.append(String.format("   %-15s %-30s %-20s %-15s\n", "Mã SV", "Họ tên", "Khoa", "Lớp"));
                    report.append("   " + "-".repeat(80) + "\n");
                    for (com.university.sms.model.Student s : students) {
                        report.append(String.format("   %-15s %-30s %-20s %-15s\n", 
                            s.getStudentCode(), 
                            s.getFullName() != null && s.getFullName().length() > 30 ? s.getFullName().substring(0, 27) + "..." : s.getFullName(),
                            s.getFacultyName() != null && s.getFacultyName().length() > 20 ? s.getFacultyName().substring(0, 17) + "..." : s.getFacultyName(),
                            s.getClassName() != null ? s.getClassName() : ""));
                    }
                    report.append("\n");
                }
            } else if (reportType.contains("Giảng viên") || reportType.contains("giảng viên")) {
                report.append("I. DANH SÁCH GIẢNG VIÊN\n\n");
                if (teachers != null && !teachers.isEmpty()) {
                    report.append(String.format("   %-20s %-30s %-30s\n", "Username", "Họ tên", "Email"));
                    report.append("   " + "-".repeat(80) + "\n");
                    for (com.university.sms.model.User t : teachers) {
                        if (t.getRole() == User.UserRole.TEACHER) {
                            report.append(String.format("   %-20s %-30s %-30s\n", 
                                t.getUsername(),
                                t.getFullName() != null && t.getFullName().length() > 30 ? t.getFullName().substring(0, 27) + "..." : t.getFullName(),
                                t.getEmail() != null ? t.getEmail() : ""));
                        }
                    }
                    report.append("\n");
                }
            }
        } catch (Exception e) {
            report.append("Lỗi khi lấy dữ liệu: ").append(e.getMessage()).append("\n");
        }

        return report.toString();
    }

    private String generateTeacherReport(String reportType, String semester) {
        StringBuilder report = new StringBuilder();

        try {
            report.append("I. THÔNG TIN GIẢNG VIÊN\n\n");
            report.append("   Họ tên: ").append(currentUser.getFullName()).append("\n");
            report.append("   Username: ").append(currentUser.getUsername()).append("\n\n");

            // Get teacher's courses
            Message coursesRequest = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
            Message coursesResponse = serverConnection.sendRequest(coursesRequest);
            
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Course> courses = 
                coursesResponse != null && coursesResponse.isSuccess() ? 
                (List<com.university.sms.model.Course>) coursesResponse.getData("courses") : null;

            if (courses != null) {
                report.append("   Số lớp đang dạy: ").append(courses.size()).append("\n\n");
                
                int totalStudents = 0;
                for (com.university.sms.model.Course course : courses) {
                    Message enrollRequest = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                    enrollRequest.addData(Constants.KEY_COURSE_ID, course.getCourseId());
                    Message enrollResponse = serverConnection.sendRequest(enrollRequest);
                    
                    if (enrollResponse != null && enrollResponse.isSuccess()) {
                        List<?> enrollments = (List<?>) enrollResponse.getData("enrollments");
                        if (enrollments != null) {
                            totalStudents += enrollments.size();
                        }
                    }
                }
                report.append("   Tổng số sinh viên: ").append(totalStudents).append("\n\n");

                if (reportType.contains("Kết quả") || reportType.contains("kết quả")) {
                    report.append("II. KẾT QUẢ DẠY HỌC\n\n");
                    report.append(String.format("   %-25s %10s %10s %12s\n", "Môn học", "Số SV", "Điểm TB", "Tỷ lệ đậu"));
                    report.append("   " + "-".repeat(60) + "\n");
                    
                    for (com.university.sms.model.Course course : courses) {
                        Message enrollRequest = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                        enrollRequest.addData(Constants.KEY_COURSE_ID, course.getCourseId());
                        Message enrollResponse = serverConnection.sendRequest(enrollRequest);
                        
                        int studentCount = 0;
                        double totalGrade = 0;
                        int passedCount = 0;
                        
                        if (enrollResponse != null && enrollResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<com.university.sms.model.Enrollment> enrollments = 
                                (List<com.university.sms.model.Enrollment>) enrollResponse.getData("enrollments");
                            if (enrollments != null) {
                                studentCount = enrollments.size();
                                for (com.university.sms.model.Enrollment e : enrollments) {
                                    if (e.getFinalGrade() != null) {
                                        totalGrade += e.getFinalGrade().doubleValue();
                                        if (e.getFinalGrade().doubleValue() >= 5.0) {
                                            passedCount++;
                                        }
                                    }
                                }
                            }
                        }
                        
                        double avgGrade = studentCount > 0 ? totalGrade / studentCount : 0;
                        double passRate = studentCount > 0 ? (passedCount * 100.0 / studentCount) : 0;
                        
                        String subjectName = course.getSubjectName();
                        if (subjectName != null && subjectName.length() > 25) {
                            subjectName = subjectName.substring(0, 22) + "...";
                        }
                        
                        report.append(String.format("   %-25s %10d %10.2f %11.1f%%\n", 
                            subjectName != null ? subjectName : course.getSubjectCode(),
                            studentCount, avgGrade, passRate));
                    }
                    report.append("\n");
                }
            }
        } catch (Exception e) {
            report.append("Lỗi khi lấy dữ liệu: ").append(e.getMessage()).append("\n");
        }

        return report.toString();
    }

    private void exportReport() {
        if (reportTextArea.getText().isEmpty() ||
                reportTextArea.getText().contains("Chọn loại báo cáo")) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng tạo báo cáo trước khi xuất",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất báo cáo");
        fileChooser.setSelectedFile(new java.io.File("Bao_cao_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Files.writeString(file.toPath(), reportTextArea.getText());

                JOptionPane.showMessageDialog(this,
                        "Báo cáo đã được xuất thành công!\nVị trí: " + file.getAbsolutePath(),
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi xuất báo cáo: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
