package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Faculty;
import com.university.sms.model.Transcript;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dashboard hiển thị thống kê và phân tích nâng cao
 */
public class AnalyticsDashboard extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AnalyticsDashboard.class.getName());

    private IServerConnection serverConnection;
    private User currentUser;

    private JLabel titleLabel;
    private JPanel statsCardsPanel;
    private JPanel chartsPanel;
    private JButton refreshButton;
    private JComboBox<FacultyItem> facultyCombo; // For admin to select faculty

    private final boolean showStatsCards;
    private final boolean showCharts;

    // Stat cards
    private StatCard totalStudentsCard;
    private StatCard averageGPACard;
    private StatCard excellentStudentsCard;
    private StatCard failingStudentsCard;

    // Chart panels
    private ChartPanel gradeDistChart;
    private ChartPanel gpaTrendChart;
    private ChartPanel facultyChart;
    private ChartPanel topPerformersChart;

    public AnalyticsDashboard(IServerConnection serverConnection, User currentUser) {
        this(serverConnection, currentUser, true, true);
    }

    public AnalyticsDashboard(IServerConnection serverConnection, User currentUser,
            boolean showStatsCards, boolean showCharts) {
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.showStatsCards = showStatsCards;
        this.showCharts = showCharts;
        initializeComponents();
        setupLayout();
        if (currentUser != null) {
            refreshData();
        }
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(236, 240, 241));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleLabel = new JLabel("📊 Thống Kê & Phân Tích", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setOpaque(false);

        // Faculty selector for admin
        if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
            controlPanel.add(new JLabel("Khoa:"));
            facultyCombo = new JComboBox<>();
            facultyCombo.addItem(new FacultyItem(null, "Tất cả các khoa"));
            facultyCombo.addActionListener(e -> refreshData());
            loadFaculties();
            controlPanel.add(facultyCombo);
        }

        refreshButton = new JButton("🔄 Làm mới");
        refreshButton.addActionListener(e -> refreshData());
        controlPanel.add(refreshButton);

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(controlPanel, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // Main content with scroll
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // Stats cards panel
        if (showStatsCards) {
            statsCardsPanel = createStatsCardsPanel();
            mainContent.add(statsCardsPanel);
            mainContent.add(Box.createVerticalStrut(20));
        }

        // Charts panel
        if (showCharts) {
            chartsPanel = createChartsPanel();
            mainContent.add(chartsPanel);
        }

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatsCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        totalStudentsCard = new StatCard("👥 Tổng Sinh Viên", "0", Color.decode("#3498db"), "↑ +0");
        averageGPACard = new StatCard("📈 GPA Trung Bình", "0.00", Color.decode("#2ecc71"), "");
        excellentStudentsCard = new StatCard("🏆 Sinh Viên Xuất Sắc", "0", Color.decode("#f39c12"), "≥ 3.6 GPA");
        failingStudentsCard = new StatCard("⚠️ Sinh Viên Yếu", "0", Color.decode("#e74c3c"), "< 2.0 GPA");

        panel.add(totalStudentsCard);
        panel.add(averageGPACard);
        panel.add(excellentStudentsCard);
        panel.add(failingStudentsCard);

        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 15));
        panel.setOpaque(false);

        // Grade Distribution Chart
        gradeDistChart = createGradeDistributionChart();
        panel.add(gradeDistChart);

        // GPA Trend Chart
        gpaTrendChart = createGPATrendChart();
        panel.add(gpaTrendChart);

        // Faculty Comparison Chart
        facultyChart = createFacultyComparisonChart();
        panel.add(facultyChart);

        // Top Performers Chart
        topPerformersChart = createTopPerformersChart();
        panel.add(topPerformersChart);

        return panel;
    }

    private ChartPanel createGradeDistributionChart() {
        ChartPanel chart = new ChartPanel("📊 Phân Bố Điểm");
        // Will be populated with real data
        return chart;
    }

    private ChartPanel createGPATrendChart() {
        ChartPanel chart = new ChartPanel("📈 Xu Hướng GPA Theo Học Kỳ");
        // Will be populated with real data
        return chart;
    }

    private ChartPanel createFacultyComparisonChart() {
        ChartPanel chart = new ChartPanel("🏛️ So Sánh GPA Các Khoa");
        // Will be populated with real data
        return chart;
    }

    private ChartPanel createTopPerformersChart() {
        ChartPanel chart = new ChartPanel("🏆 Top 5 Sinh Viên Xuất Sắc");
        // Will be populated with real data
        return chart;
    }

    private void setupLayout() {
        // Layout is already set up
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        removeAll();
        initializeComponents();
        setupLayout();
        revalidate();
        repaint();
        refreshData();
    }

    public void refreshData() {
        if (currentUser == null)
            return;

        // Only admin or teacher can view analytics
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole().toString())
                && !"TEACHER".equalsIgnoreCase(currentUser.getRole().toString())) {
            return;
        }

        SwingWorker<StatisticsData, Void> worker = new SwingWorker<>() {
            @Override
            protected StatisticsData doInBackground() {
                try {
                    String facultyCode = getCurrentUserFacultyCode();
                    StatisticsData data = new StatisticsData();

                    // Get faculty statistics if faculty is selected
                    if (facultyCode != null && !facultyCode.isEmpty()) {
                        Message request = new Message();
                        request.setAction(Constants.ACTION_GET_FACULTY_STATISTICS);
                        Map<String, Object> reqData = new HashMap<>();
                        reqData.put("facultyCode", facultyCode);
                        request.setData(reqData);

                        Message response = serverConnection.sendRequest(request);
                        if (response != null && response.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> stats = (Map<String, Object>) response
                                    .getData(Constants.KEY_STATISTICS);
                            data.facultyStats = stats;
                        }
                    }

                    // For admin: get all faculties statistics for comparison
                    if (currentUser.getRole() == User.UserRole.ADMIN) {
                        Message facRequest = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
                        Message facResponse = serverConnection.sendRequest(facRequest);
                        if (facResponse != null && facResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<Faculty> faculties = (List<Faculty>) facResponse.getData("faculties");
                            data.allFaculties = faculties;

                            // Get statistics for each faculty
                            if (faculties != null) {
                                data.facultyStatsMap = new HashMap<>();
                                for (Faculty fac : faculties) {
                                    Message statRequest = new Message();
                                    statRequest.setAction(Constants.ACTION_GET_FACULTY_STATISTICS);
                                    Map<String, Object> reqData = new HashMap<>();
                                    reqData.put("facultyCode", fac.getFacultyCode());
                                    statRequest.setData(reqData);

                                    Message statResponse = serverConnection.sendRequest(statRequest);
                                    if (statResponse != null && statResponse.isSuccess()) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> stats = (Map<String, Object>) statResponse
                                                .getData(Constants.KEY_STATISTICS);
                                        data.facultyStatsMap.put(fac.getFacultyCode(), stats);
                                    }
                                }
                            }
                        }
                    }

                    // Get top honor students (for all faculties if admin, or specific faculty)
                    Message honorRequest = new Message();
                    honorRequest.setAction(Constants.ACTION_GET_HONOR_STUDENTS);
                    Map<String, Object> reqData = new HashMap<>();
                    // If admin viewing all, pass null to get top students from all faculties
                    reqData.put(Constants.KEY_FACULTY_CODE, facultyCode);
                    honorRequest.setData(reqData);

                    Message honorResponse = serverConnection.sendRequest(honorRequest);
                    if (honorResponse != null && honorResponse.isSuccess()) {
                        List<?> honorStudents = (List<?>) honorResponse.getData(Constants.KEY_HONOR_STUDENTS);
                        data.honorStudents = honorStudents;
                    }

                    // Get GPA trend by semester (for all students or specific faculty)
                    data.gpaTrendData = getGPATrendBySemester(facultyCode);

                    return data;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu phân tích", e);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    StatisticsData data = get();
                    if (data != null) {
                        updateStatistics(data);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật thống kê phân tích", e);
                }
            }
        };

        worker.execute();
    }

    private static class StatisticsData {
        Map<String, Object> facultyStats;
        List<Faculty> allFaculties;
        Map<String, Map<String, Object>> facultyStatsMap;
        List<?> honorStudents;
        Map<String, Double> gpaTrendData; // Key: "academicYear-semester", Value: average GPA
    }

    /**
     * Lấy dữ liệu GPA trend theo học kỳ
     */
    private Map<String, Double> getGPATrendBySemester(String facultyCode) {
        Map<String, Double> trendData = new HashMap<>();
        try {
            // Get all students first to filter by faculty if needed
            Message studentsRequest = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
            Message studentsResponse = serverConnection.sendRequest(studentsRequest);

            java.util.Set<String> studentCodes = new java.util.HashSet<>();
            if (studentsResponse != null && studentsResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<com.university.sms.model.Student> students = (List<com.university.sms.model.Student>) studentsResponse
                        .getData("students");
                if (students != null) {
                    for (com.university.sms.model.Student student : students) {
                        if (facultyCode == null || facultyCode.isEmpty() ||
                                facultyCode.equals(student.getFacultyCode())) {
                            studentCodes.add(student.getStudentCode());
                        }
                    }
                }
            }

            if (studentCodes.isEmpty()) {
                return trendData;
            }

            // Get all courses to map course_code to academic_year and semester
            Message coursesRequest = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
            Message coursesResponse = serverConnection.sendRequest(coursesRequest);

            Map<String, com.university.sms.model.Course> courseMap = new HashMap<>();
            if (coursesResponse != null && coursesResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<com.university.sms.model.Course> courses = (List<com.university.sms.model.Course>) coursesResponse
                        .getData("courses");
                if (courses != null) {
                    for (com.university.sms.model.Course course : courses) {
                        courseMap.put(course.getCourseCode(), course);
                    }
                }
            }

            // Get enrollments for each student
            Map<String, List<Double>> semesterGPAs = new HashMap<>();

            for (String studentCode : studentCodes) {
                Message enrollRequest = Message.createRequest(Constants.ACTION_GET_STUDENT_GRADES);
                enrollRequest.addData("studentCode", studentCode);
                Message enrollResponse = serverConnection.sendRequest(enrollRequest);

                if (enrollResponse != null && enrollResponse.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<com.university.sms.model.Enrollment> enrollments = (List<com.university.sms.model.Enrollment>) enrollResponse
                            .getData("enrollments");

                    if (enrollments != null) {
                        for (com.university.sms.model.Enrollment enrollment : enrollments) {
                            if (enrollment
                                    .getEnrollmentStatus() == com.university.sms.model.Enrollment.EnrollmentStatus.COMPLETED
                                    && enrollment.getGradePoints() != null) {

                                com.university.sms.model.Course course = courseMap.get(enrollment.getCourseCode());
                                if (course != null) {
                                    String semesterKey = course.getAcademicYear() + "-" + course.getSemester();
                                    semesterGPAs.computeIfAbsent(semesterKey, k -> new java.util.ArrayList<>())
                                            .add(enrollment.getGradePoints().doubleValue());
                                }
                            }
                        }
                    }
                }
            }

            // Calculate average GPA for each semester
            for (Map.Entry<String, List<Double>> entry : semesterGPAs.entrySet()) {
                List<Double> gpas = entry.getValue();
                double avgGPA = gpas.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                trendData.put(entry.getKey(), avgGPA);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính toán xu hướng GPA theo học kỳ", e);
        }
        return trendData;
    }

    /**
     * Lấy facultyCode từ currentUser
     * - STUDENT: Lấy từ student.facultyCode
     * - TEACHER: Lấy từ courses/subjects
     * - ADMIN: Lấy từ faculty selector hoặc null (tất cả)
     */
    private String getCurrentUserFacultyCode() {
        if (currentUser == null)
            return null;

        try {
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                // Get student info to get facultyCode
                Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    com.university.sms.model.Student student = response.getData(Constants.KEY_STUDENT,
                            com.university.sms.model.Student.class);
                    if (student != null) {
                        return student.getFacultyCode();
                    }
                }
            } else if (currentUser.getRole() == User.UserRole.TEACHER) {
                // Get teacher's courses to determine faculty
                Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<com.university.sms.model.Course> courses = (List<com.university.sms.model.Course>) response
                            .getData("courses");
                    if (courses != null && !courses.isEmpty()) {
                        // Get faculty from first course's subject
                        String subjectCode = courses.get(0).getSubjectCode();
                        // Get all subjects and find the one with matching code
                        Message subRequest = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
                        Message subResponse = serverConnection.sendRequest(subRequest);
                        if (subResponse != null && subResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<com.university.sms.model.Subject> subjects = (List<com.university.sms.model.Subject>) subResponse
                                    .getData(Constants.KEY_SUBJECTS);
                            if (subjects != null) {
                                for (com.university.sms.model.Subject subject : subjects) {
                                    if (subjectCode.equals(subject.getSubjectCode())) {
                                        return subject.getFacultyCode();
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            } else if (currentUser.getRole() == User.UserRole.ADMIN) {
                // Admin: get from faculty selector
                if (facultyCombo != null && facultyCombo.getSelectedItem() != null) {
                    FacultyItem item = (FacultyItem) facultyCombo.getSelectedItem();
                    return item.code; // null means "all faculties"
                }
                return null; // All faculties
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xác định khoa của người dùng hiện tại", e);
        }

        return null; // Default fallback
    }

    private void loadFaculties() {
        SwingWorker<List<Faculty>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Faculty> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Faculty> faculties = (List<Faculty>) response.getData("faculties");
                    return faculties;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    List<Faculty> faculties = get();
                    if (faculties != null && facultyCombo != null) {
                        for (Faculty faculty : faculties) {
                            facultyCombo.addItem(new FacultyItem(faculty.getFacultyCode(), faculty.getFacultyName()));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách khoa", e);
                }
            }
        };
        worker.execute();
    }

    private void updateStatistics(StatisticsData data) {
        // Update stat cards from faculty stats
        if (data.facultyStats != null) {
            Map<String, Object> stats = data.facultyStats;
            if (totalStudentsCard != null) {
                totalStudentsCard.setValue(stats.get("totalStudents").toString());
            }

            Object avgGPA = stats.get("averageGPA");
            if (avgGPA != null) {
                if (averageGPACard != null) {
                    averageGPACard.setValue(String.format("%.2f", ((Number) avgGPA).doubleValue()));
                }
            }

            if (excellentStudentsCard != null) {
                excellentStudentsCard.setValue(stats.get("excellentCount").toString());
            }
            if (failingStudentsCard != null) {
                failingStudentsCard.setValue(stats.get("poorCount").toString());
            }

            // Update grade distribution chart
            updateGradeDistributionChart(stats);
        } else {
            // For admin viewing all, aggregate data
            if (data.facultyStatsMap != null && !data.facultyStatsMap.isEmpty()) {
                Map<String, Object> aggregatedStats = aggregateStatistics(data.facultyStatsMap);
                int totalStudents = ((Number) aggregatedStats.getOrDefault("totalStudents", 0)).intValue();
                int excellentCount = ((Number) aggregatedStats.getOrDefault("excellentCount", 0)).intValue();
                int poorCount = ((Number) aggregatedStats.getOrDefault("poorCount", 0)).intValue();
                double avgGpa = ((Number) aggregatedStats.getOrDefault("averageGPA", 0d)).doubleValue();

                if (totalStudentsCard != null) {
                    totalStudentsCard.setValue(String.valueOf(totalStudents));
                }
                if (averageGPACard != null && totalStudents > 0) {
                    averageGPACard.setValue(String.format("%.2f", avgGpa));
                }
                if (excellentStudentsCard != null) {
                    excellentStudentsCard.setValue(String.valueOf(excellentCount));
                }
                if (failingStudentsCard != null) {
                    failingStudentsCard.setValue(String.valueOf(poorCount));
                }

                updateGradeDistributionChart(aggregatedStats);
            }
        }

        // Update faculty comparison chart (for admin)
        if (currentUser.getRole() == User.UserRole.ADMIN && data.facultyStatsMap != null) {
            updateFacultyComparisonChart(data.facultyStatsMap, data.allFaculties);
        }

        // Update top performers chart
        if (data.honorStudents != null && !data.honorStudents.isEmpty()) {
            updateTopPerformersChart(data.honorStudents);
        }

        // Update GPA trend chart
        if (data.gpaTrendData != null && !data.gpaTrendData.isEmpty()) {
            updateGPATrendChart(data.gpaTrendData);
        }
    }

    private void updateGPATrendChart(Map<String, Double> trendData) {
        if (gpaTrendChart == null || trendData == null || trendData.isEmpty())
            return;

        gpaTrendChart.clearBars();

        // Sort semesters chronologically
        List<String> sortedSemesters = new java.util.ArrayList<>(trendData.keySet());
        sortedSemesters.sort((s1, s2) -> {
            String[] parts1 = s1.split("-");
            String[] parts2 = s2.split("-");
            int yearCompare = parts1[0].compareTo(parts2[0]);
            if (yearCompare != 0)
                return yearCompare;
            return Integer.compare(Integer.parseInt(parts1[1]), Integer.parseInt(parts2[1]));
        });

        Color[] colors = {
                Color.decode("#3498db"), Color.decode("#2ecc71"),
                Color.decode("#f39c12"), Color.decode("#e74c3c"),
                Color.decode("#9b59b6"), Color.decode("#1abc9c")
        };

        int colorIndex = 0;
        for (String semesterKey : sortedSemesters) {
            Double gpa = trendData.get(semesterKey);
            if (gpa != null) {
                String[] parts = semesterKey.split("-");
                String label = "HK" + parts[1] + " " + parts[0];
                // Convert GPA (0-4 scale) to percentage (0-100) for display
                gpaTrendChart.addBar(label, gpa * 25, colors[colorIndex % colors.length]);
                colorIndex++;
            }
        }
    }

    private void updateGradeDistributionChart(Map<String, Object> stats) {
        if (gradeDistChart == null || stats == null)
            return;

        gradeDistChart.clearBars();

        // Get percentages from stats
        int excellentPercent = ((Number) stats.getOrDefault("excellentPercent", 0)).intValue();
        int goodPercent = ((Number) stats.getOrDefault("goodPercent", 0)).intValue();
        int fairPercent = ((Number) stats.getOrDefault("fairPercent", 0)).intValue();
        int averagePercent = ((Number) stats.getOrDefault("averagePercent", 0)).intValue();
        int poorPercent = ((Number) stats.getOrDefault("poorPercent", 0)).intValue();

        gradeDistChart.addBar("A (Xuất sắc)", excellentPercent, Color.decode("#27ae60"));
        gradeDistChart.addBar("B (Giỏi)", goodPercent, Color.decode("#3498db"));
        gradeDistChart.addBar("C (Khá)", fairPercent, Color.decode("#f39c12"));
        gradeDistChart.addBar("D (Trung bình)", averagePercent, Color.decode("#e67e22"));
        gradeDistChart.addBar("F (Yếu)", poorPercent, Color.decode("#e74c3c"));
    }

    private void updateFacultyComparisonChart(Map<String, Map<String, Object>> facultyStatsMap,
            List<Faculty> faculties) {
        if (facultyChart == null || facultyStatsMap == null || faculties == null)
            return;

        facultyChart.clearBars();

        Color[] colors = {
                Color.decode("#9b59b6"), Color.decode("#e74c3c"),
                Color.decode("#f39c12"), Color.decode("#1abc9c"),
                Color.decode("#3498db"), Color.decode("#e67e22")
        };

        int colorIndex = 0;
        for (Faculty fac : faculties) {
            Map<String, Object> stats = facultyStatsMap.get(fac.getFacultyCode());
            if (stats != null) {
                Object avgGPA = stats.get("averageGPA");
                if (avgGPA != null) {
                    double gpa = ((Number) avgGPA).doubleValue();
                    String label = fac.getFacultyName();
                    if (label.length() > 15) {
                        label = label.substring(0, 12) + "...";
                    }
                    facultyChart.addBar(label, gpa * 20, colors[colorIndex % colors.length]);
                    colorIndex++;
                }
            }
        }
    }

    private void updateTopPerformersChart(List<?> honorStudents) {
        if (topPerformersChart == null || honorStudents == null)
            return;

        topPerformersChart.clearBars();

        Color[] colors = {
                Color.decode("#f1c40f"), Color.decode("#95a5a6"),
                Color.decode("#cd7f32"), Color.decode("#3498db"),
                Color.decode("#9b59b6")
        };

        int count = Math.min(5, honorStudents.size());
        for (int i = 0; i < count; i++) {
            Object studentObj = honorStudents.get(i);
            String name = null;
            Double gpaValue = null;

            if (studentObj instanceof Map<?, ?>) {
                Map<?, ?> studentMap = (Map<?, ?>) studentObj;
                Object nameObj = studentMap.get("studentName");
                name = nameObj instanceof String ? (String) nameObj : "N/A";
                Object gpaObj = studentMap.get("gpa");
                if (gpaObj instanceof Number) {
                    gpaValue = ((Number) gpaObj).doubleValue();
                }
            } else if (studentObj instanceof Transcript) {
                Transcript transcript = (Transcript) studentObj;
                name = transcript.getStudentName();
                if (transcript.getCumulativeGPA() != null) {
                    gpaValue = transcript.getCumulativeGPA().doubleValue();
                }
            }

            if (name != null && gpaValue != null) {
                String label = name.length() > 15 ? name.substring(0, 12) + "..." : name;
                topPerformersChart.addBar(label, gpaValue * 20, colors[i % colors.length]);
            }
        }
    }

    private Map<String, Object> aggregateStatistics(Map<String, Map<String, Object>> facultyStatsMap) {
        Map<String, Object> aggregated = new HashMap<>();
        int totalStudents = 0;
        double weightedGPA = 0;
        int excellent = 0;
        int good = 0;
        int fair = 0;
        int average = 0;
        int poor = 0;

        for (Map<String, Object> stats : facultyStatsMap.values()) {
            int students = ((Number) stats.getOrDefault("totalStudents", 0)).intValue();
            totalStudents += students;
            weightedGPA += ((Number) stats.getOrDefault("averageGPA", 0d)).doubleValue() * students;
            excellent += ((Number) stats.getOrDefault("excellentCount", 0)).intValue();
            good += ((Number) stats.getOrDefault("goodCount", 0)).intValue();
            fair += ((Number) stats.getOrDefault("fairCount", 0)).intValue();
            average += ((Number) stats.getOrDefault("averageCount", 0)).intValue();
            poor += ((Number) stats.getOrDefault("poorCount", 0)).intValue();
        }

        aggregated.put("totalStudents", totalStudents);
        aggregated.put("averageGPA", totalStudents > 0 ? weightedGPA / totalStudents : 0);
        aggregated.put("excellentCount", excellent);
        aggregated.put("goodCount", good);
        aggregated.put("fairCount", fair);
        aggregated.put("averageCount", average);
        aggregated.put("poorCount", poor);

        if (totalStudents > 0) {
            aggregated.put("excellentPercent", Math.round(excellent * 100.0 / totalStudents));
            aggregated.put("goodPercent", Math.round(good * 100.0 / totalStudents));
            aggregated.put("fairPercent", Math.round(fair * 100.0 / totalStudents));
            aggregated.put("averagePercent", Math.round(average * 100.0 / totalStudents));
            aggregated.put("poorPercent", Math.round(poor * 100.0 / totalStudents));
        }

        return aggregated;
    }

    /**
     * Stat Card Component
     */
    private static class StatCard extends JPanel {
        private JLabel valueLabel;
        private JLabel subTextLabel;

        public StatCard(String title, String value, Color color, String subText) {
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));

            // Title
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            titleLabel.setForeground(Color.GRAY);
            add(titleLabel, BorderLayout.NORTH);

            // Value
            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
            valueLabel.setForeground(color);
            add(valueLabel, BorderLayout.CENTER);

            // Sub text
            subTextLabel = new JLabel(subText);
            subTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subTextLabel.setForeground(Color.LIGHT_GRAY);
            add(subTextLabel, BorderLayout.SOUTH);
        }

        public void setValue(String value) {
            valueLabel.setText(value);
        }

        @SuppressWarnings("unused")
        public void setSubText(String text) {
            subTextLabel.setText(text);
        }
    }

    /**
     * Simple Chart Panel Component
     */
    private static class ChartPanel extends JPanel {
        private String title;
        private java.util.List<BarData> bars;

        public ChartPanel(String title) {
            this.title = title;
            this.bars = new java.util.ArrayList<>();

            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            setPreferredSize(new Dimension(400, 300));
        }

        public void addBar(String label, double value, Color color) {
            bars.add(new BarData(label, value, color));
            repaint();
        }

        public void clearBars() {
            bars.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw title
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawString(title, 10, 20);

            if (bars.isEmpty())
                return;

            // Calculate dimensions
            int chartTop = 50;
            int chartHeight = getHeight() - chartTop - 80;
            int chartLeft = 20;
            int chartWidth = getWidth() - chartLeft - 20;

            // Find max value
            double maxValue = bars.stream().mapToDouble(b -> b.value).max().orElse(100);

            // Draw bars
            int barWidth = chartWidth / bars.size() - 10;
            int x = chartLeft;

            for (BarData bar : bars) {
                int barHeight = (int) ((bar.value / maxValue) * chartHeight);
                int y = chartTop + chartHeight - barHeight;

                // Draw bar
                g2d.setColor(bar.color);
                g2d.fillRoundRect(x, y, barWidth, barHeight, 5, 5);

                // Draw value on top
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valueText = String.format("%.0f%%", bar.value);
                int textWidth = g2d.getFontMetrics().stringWidth(valueText);
                g2d.drawString(valueText, x + (barWidth - textWidth) / 2, y - 5);

                // Draw label
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2d.setColor(Color.GRAY);

                // Wrap label if too long
                String label = bar.label;
                if (label.length() > 10) {
                    label = label.substring(0, 10) + "...";
                }

                // Rotate label if needed
                textWidth = g2d.getFontMetrics().stringWidth(label);
                if (textWidth > barWidth) {
                    // Draw rotated
                    g2d.rotate(-Math.PI / 4, x + barWidth / 2, chartTop + chartHeight + 20);
                    g2d.drawString(label, x + barWidth / 2, chartTop + chartHeight + 20);
                    g2d.rotate(Math.PI / 4, x + barWidth / 2, chartTop + chartHeight + 20);
                } else {
                    g2d.drawString(label, x + (barWidth - textWidth) / 2, chartTop + chartHeight + 15);
                }

                x += barWidth + 10;
            }
        }

        private static class BarData {
            String label;
            double value;
            Color color;

            BarData(String label, double value, Color color) {
                this.label = label;
                this.value = value;
                this.color = color;
            }
        }
    }

    /**
     * Helper class for faculty combo box items
     */
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
}
