package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Faculty;
import com.university.sms.model.Transcript;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Function;
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
    private JButton loadGpaTrendButton;

    private final boolean showStatsCards;
    private final boolean showCharts;

    // Stat cards
    private StatCard totalStudentsCard;
    private StatCard averageGPACard;
    private StatCard excellentStudentsCard;
    private StatCard failingStudentsCard;

    // Chart panels
    private ChartPanel gradeDistChart;
    private LineChartPanel gpaTrendChart;
    private ChartPanel facultyChart;
    private TopStudentsPanel topPerformersPanel;

    public AnalyticsDashboard(IServerConnection serverConnection, User currentUser) {
        this(serverConnection, currentUser, true, true);
    }

    private boolean isRefreshing = false;
    private boolean pendingRefresh = false;
    private boolean gpaTrendLoaded = false;
    private boolean gpaTrendLoading = false;
    private String lastSelectedFacultyCode = null;
    private String cachedUserFacultyCode = null;
    private boolean userFacultyCodeResolved = false;

    public AnalyticsDashboard(IServerConnection serverConnection, User currentUser,
            boolean showStatsCards, boolean showCharts) {
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.showStatsCards = showStatsCards;
        this.showCharts = showCharts;
        initializeComponents();
        setupLayout();
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
        JPanel gpaContainer = new JPanel(new BorderLayout(5, 5));
        gpaContainer.setOpaque(false);
        gpaContainer.add(gpaTrendChart, BorderLayout.CENTER);
        loadGpaTrendButton = new JButton("Làm mới xu hướng GPA");
        loadGpaTrendButton.addActionListener(e -> {
            gpaTrendLoaded = false;
            loadGpaTrendData();
        });
        gpaContainer.add(loadGpaTrendButton, BorderLayout.SOUTH);
        panel.add(gpaContainer);

        // Faculty Comparison Chart
        facultyChart = createFacultyComparisonChart();
        panel.add(facultyChart);

        // Top Performers Chart
        topPerformersPanel = createTopPerformersPanel();
        panel.add(topPerformersPanel);

        return panel;
    }

    private ChartPanel createGradeDistributionChart() {
        ChartPanel chart = new ChartPanel("📊 Phân Bố Điểm");
        chart.setValueFormatter(value -> String.format("%.0f SV", value));
        return chart;
    }

    private LineChartPanel createGPATrendChart() {
        LineChartPanel chart = new LineChartPanel("📈 Xu Hướng GPA Theo Học Kỳ");
        return chart;
    }

    private ChartPanel createFacultyComparisonChart() {
        ChartPanel chart = new ChartPanel("🏛️ So Sánh GPA Các Khoa");
        chart.setValueFormatter(value -> String.format("%.2f GPA", value));
        return chart;
    }

    private TopStudentsPanel createTopPerformersPanel() {
        return new TopStudentsPanel("🏆 Top 5 Sinh Viên Xuất Sắc");
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
        pendingRefresh = false;
        isRefreshing = false;
        gpaTrendLoaded = false;
        gpaTrendLoading = false;
        lastSelectedFacultyCode = null;
        cachedUserFacultyCode = null;
        userFacultyCodeResolved = false;
    }

    public void refreshData() {
        if (currentUser == null)
            return;

        if (isRefreshing) {
            pendingRefresh = true;
            return;
        }
        isRefreshing = true;
        gpaTrendLoaded = false;
        gpaTrendLoading = false;
        if (loadGpaTrendButton != null) {
            loadGpaTrendButton.setEnabled(true);
            loadGpaTrendButton.setText("Làm mới xu hướng GPA");
        }
        if (gpaTrendChart != null) {
            gpaTrendChart.clearPoints();
        }

        // Only admin or teacher can view analytics
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole().toString())
                && !"TEACHER".equalsIgnoreCase(currentUser.getRole().toString())) {
            isRefreshing = false;
            return;
        }

        lastSelectedFacultyCode = getSelectedFacultyFilter();

        SwingWorker<StatisticsData, Void> worker = new SwingWorker<>() {
            @Override
            protected StatisticsData doInBackground() {
                try {
                    String facultyCode = lastSelectedFacultyCode;
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
                } finally {
                    isRefreshing = false;
                    if (pendingRefresh) {
                        pendingRefresh = false;
                        refreshData();
                    }
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
    }

    /**
     * Lấy dữ liệu GPA trend từ server (đã tối ưu)
     */
    private Map<String, Double> getGPATrendBySemester(String facultyCode) {
        Map<String, Double> trendData = new HashMap<>();
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_GPA_TREND);
            if (facultyCode != null && !facultyCode.isEmpty()) {
                request.addData(Constants.KEY_FACULTY_CODE, facultyCode);
            }

            Message response = serverConnection.sendRequest(request);
            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Double> serverTrend = (Map<String, Double>) response.getData(Constants.KEY_GPA_TREND);
                if (serverTrend != null) {
                    trendData.putAll(serverTrend);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy xu hướng GPA từ server", e);
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

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            if (facultyCombo != null && facultyCombo.getSelectedItem() != null) {
                FacultyItem item = (FacultyItem) facultyCombo.getSelectedItem();
                return item.code;
            }
            return null;
        }

        if (userFacultyCodeResolved) {
            return cachedUserFacultyCode;
        }

        try {
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    com.university.sms.model.Student student = response.getData(Constants.KEY_STUDENT,
                            com.university.sms.model.Student.class);
                    if (student != null) {
                        cachedUserFacultyCode = student.getFacultyCode();
                        userFacultyCodeResolved = true;
                        return cachedUserFacultyCode;
                    }
                }
            } else if (currentUser.getRole() == User.UserRole.TEACHER) {
                Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<com.university.sms.model.Course> courses = (List<com.university.sms.model.Course>) response
                            .getData("courses");
                    if (courses != null && !courses.isEmpty()) {
                        String subjectCode = courses.get(0).getSubjectCode();
                        Message subRequest = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
                        Message subResponse = serverConnection.sendRequest(subRequest);
                        if (subResponse != null && subResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<com.university.sms.model.Subject> subjects = (List<com.university.sms.model.Subject>) subResponse
                                    .getData(Constants.KEY_SUBJECTS);
                            if (subjects != null) {
                                for (com.university.sms.model.Subject subject : subjects) {
                                    if (subjectCode.equals(subject.getSubjectCode())) {
                                        cachedUserFacultyCode = subject.getFacultyCode();
                                        userFacultyCodeResolved = true;
                                        return cachedUserFacultyCode;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xác định khoa của người dùng hiện tại", e);
        }

        return null; // Default fallback
    }

    private String getSelectedFacultyFilter() {
        if (currentUser == null) {
            return null;
        }

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            if (facultyCombo != null && facultyCombo.getSelectedItem() != null) {
                FacultyItem item = (FacultyItem) facultyCombo.getSelectedItem();
                return item.code;
            }
            return null;
        }

        return getCurrentUserFacultyCode();
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
            updateTopPerformersPanel(data.honorStudents);
        } else if (topPerformersPanel != null) {
            topPerformersPanel.clearStudents();
        }

        // Tự động tải xu hướng GPA sau khi có số liệu tổng quát
        if (gpaTrendChart != null) {
            loadGpaTrendData();
        }
    }

    private void updateGPATrendChart(Map<String, Double> trendData) {
        if (gpaTrendChart == null || trendData == null || trendData.isEmpty())
            return;

        gpaTrendChart.clearPoints();

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

        for (String semesterKey : sortedSemesters) {
            Double gpa = trendData.get(semesterKey);
            if (gpa != null) {
                String[] parts = semesterKey.split("-");
                String label = "HK" + parts[1] + " " + parts[0];
                gpaTrendChart.addPoint(label, gpa);
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

        int excellentCount = ((Number) stats.getOrDefault("excellentCount", 0)).intValue();
        int goodCount = ((Number) stats.getOrDefault("goodCount", 0)).intValue();
        int fairCount = ((Number) stats.getOrDefault("fairCount", 0)).intValue();
        int averageCount = ((Number) stats.getOrDefault("averageCount", 0)).intValue();
        int poorCount = ((Number) stats.getOrDefault("poorCount", 0)).intValue();

        gradeDistChart.addBar("A (Xuất sắc)", excellentCount, Color.decode("#27ae60"));
        gradeDistChart.addBar("B (Giỏi)", goodCount, Color.decode("#3498db"));
        gradeDistChart.addBar("C (Khá)", fairCount, Color.decode("#f39c12"));
        gradeDistChart.addBar("D (Trung bình)", averageCount, Color.decode("#e67e22"));
        gradeDistChart.addBar("F (Yếu)", poorCount, Color.decode("#e74c3c"));
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
                    facultyChart.addBar(label, gpa, colors[colorIndex % colors.length]);
                    colorIndex++;
                }
            }
        }
    }

    private void updateTopPerformersPanel(List<?> honorStudents) {
        if (topPerformersPanel == null || honorStudents == null) {
            return;
        }

        java.util.List<TopStudentsPanel.StudentInfo> studentInfos = new java.util.ArrayList<>();
        int count = Math.min(5, honorStudents.size());
        for (int i = 0; i < count; i++) {
            Object studentObj = honorStudents.get(i);
            String name = null;
            String code = null;
            Double gpaValue = null;

            if (studentObj instanceof Map<?, ?>) {
                Map<?, ?> studentMap = (Map<?, ?>) studentObj;
                Object nameObj = studentMap.get("studentName");
                name = nameObj instanceof String ? (String) nameObj : "N/A";
                Object codeObj = studentMap.get("studentCode");
                code = codeObj instanceof String ? (String) codeObj : null;
                Object gpaObj = studentMap.get("gpa");
                if (gpaObj instanceof Number) {
                    gpaValue = ((Number) gpaObj).doubleValue();
                }
            } else if (studentObj instanceof Transcript) {
                Transcript transcript = (Transcript) studentObj;
                name = transcript.getStudentName();
                code = transcript.getStudentCode();
                if (transcript.getCumulativeGPA() != null) {
                    gpaValue = transcript.getCumulativeGPA().doubleValue();
                }
            }

            if (name != null && gpaValue != null) {
                studentInfos.add(new TopStudentsPanel.StudentInfo(
                        code != null ? code : "—",
                        name,
                        gpaValue));
            }
        }

        topPerformersPanel.setStudents(studentInfos);
    }

    private void loadGpaTrendData() {
        if (gpaTrendLoaded || gpaTrendLoading) {
            return;
        }

        gpaTrendLoading = true;
        if (loadGpaTrendButton != null) {
            loadGpaTrendButton.setEnabled(false);
            loadGpaTrendButton.setText("Đang tải...");
        }

        final String facultyCode = lastSelectedFacultyCode != null ? lastSelectedFacultyCode
                : getSelectedFacultyFilter();

        SwingWorker<Map<String, Double>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Double> doInBackground() {
                return getGPATrendBySemester(facultyCode);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Double> trendData = get();
                    if (trendData != null && !trendData.isEmpty()) {
                        updateGPATrendChart(trendData);
                        gpaTrendLoaded = true;
                    } else if (gpaTrendChart != null) {
                        gpaTrendChart.clearPoints();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải xu hướng GPA", e);
                    JOptionPane.showMessageDialog(AnalyticsDashboard.this,
                            "Không thể tải xu hướng GPA: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    gpaTrendLoading = false;
                    if (loadGpaTrendButton != null) {
                        if (gpaTrendLoaded) {
                            loadGpaTrendButton.setText("Làm mới xu hướng GPA");
                        } else {
                            loadGpaTrendButton.setText("Làm mới xu hướng GPA");
                        }
                        loadGpaTrendButton.setEnabled(true);
                    }
                }
            }
        };

        worker.execute();
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
        private Function<Double, String> valueFormatter;

        public ChartPanel(String title) {
            this.title = title;
            this.bars = new java.util.ArrayList<>();
            this.valueFormatter = value -> String.format("%.0f%%", value);

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

        public void setValueFormatter(Function<Double, String> formatter) {
            if (formatter != null) {
                this.valueFormatter = formatter;
            }
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
                String valueText = valueFormatter != null ? valueFormatter.apply(bar.value)
                        : String.format("%.0f%%", bar.value);
                int textWidth = g2d.getFontMetrics().stringWidth(valueText);
                g2d.drawString(valueText, x + (barWidth - textWidth) / 2, y - 5);

                // Draw label
                Font labelFont = new Font("Segoe UI", Font.PLAIN, 10);
                g2d.setFont(labelFont);
                g2d.setColor(Color.GRAY);
                FontMetrics labelMetrics = g2d.getFontMetrics(labelFont);
                java.util.List<String> lines = wrapLabel(bar.label, labelMetrics, barWidth);
                int labelY = chartTop + chartHeight + 15;
                for (String line : lines) {
                    textWidth = labelMetrics.stringWidth(line);
                    g2d.drawString(line, x + (barWidth - textWidth) / 2, labelY);
                    labelY += labelMetrics.getHeight();
                }

                x += barWidth + 10;
            }
        }

        private java.util.List<String> wrapLabel(String label, FontMetrics metrics, int maxWidth) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (label == null || label.isEmpty()) {
                lines.add("");
                return lines;
            }

            String[] words = label.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth || currentLine.length() == 0) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            if (lines.size() > 2) {
                StringBuilder merged = new StringBuilder(lines.get(1));
                for (int i = 2; i < lines.size(); i++) {
                    merged.append(" ").append(lines.get(i));
                }
                String secondLine = merged.toString();
                while (metrics.stringWidth(secondLine + "...") > maxWidth && secondLine.length() > 1) {
                    secondLine = secondLine.substring(0, secondLine.length() - 1);
                }
                if (!secondLine.endsWith("...")) {
                    secondLine = secondLine.trim() + "...";
                }
                lines = Arrays.asList(lines.get(0), secondLine);
            }

            return lines;
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
     * Line Chart Panel Component for GPA Trend
     */
    private static class LineChartPanel extends JPanel {
        private String title;
        private java.util.List<PointData> points;
        private Color lineColor = Color.decode("#3498db");
        private Color pointColor = Color.decode("#2980b9");
        private Color fillColor = new Color(52, 152, 219, 50); // Semi-transparent blue

        public LineChartPanel(String title) {
            this.title = title;
            this.points = new java.util.ArrayList<>();

            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            setPreferredSize(new Dimension(400, 300));
        }

        public void addPoint(String label, double value) {
            points.add(new PointData(label, value));
            repaint();
        }

        public void clearPoints() {
            points.clear();
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

            if (points.isEmpty())
                return;

            // Calculate dimensions
            int chartTop = 50;
            int chartBottom = getHeight() - 60;
            int chartHeight = chartBottom - chartTop;
            int chartLeft = 50;
            int chartRight = getWidth() - 30;
            int chartWidth = chartRight - chartLeft;

            // Find min and max values (GPA range: 0-4)
            double minValue = 0.0;
            double maxValue = 4.0;

            // Draw Y-axis grid lines and labels
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2d.setColor(Color.GRAY);
            double yStep = 1.0; // GPA step
            for (double val = minValue; val <= maxValue; val += yStep) {
                int y = chartBottom - (int) ((val - minValue) / (maxValue - minValue) * chartHeight);
                // Grid line
                g2d.setColor(new Color(230, 230, 230));
                g2d.drawLine(chartLeft, y, chartRight, y);
                // Label
                g2d.setColor(Color.GRAY);
                String label = String.format("%.1f", val);
                g2d.drawString(label, chartLeft - 35, y + 4);
            }

            // Draw X-axis
            g2d.setColor(Color.GRAY);
            g2d.drawLine(chartLeft, chartBottom, chartRight, chartBottom);

            // Calculate point positions
            int pointCount = points.size();
            int[] xPositions = new int[pointCount];
            int[] yPositions = new int[pointCount];
            int spacing = pointCount > 1 ? chartWidth / (pointCount - 1) : chartWidth / 2;

            for (int i = 0; i < pointCount; i++) {
                PointData point = points.get(i);
                xPositions[i] = pointCount > 1 ? chartLeft + i * spacing : chartLeft + chartWidth / 2;
                yPositions[i] = chartBottom - (int) ((point.value - minValue) / (maxValue - minValue) * chartHeight);
            }

            // Draw filled area under the line
            if (pointCount > 1) {
                int[] xFill = new int[pointCount + 2];
                int[] yFill = new int[pointCount + 2];
                System.arraycopy(xPositions, 0, xFill, 0, pointCount);
                System.arraycopy(yPositions, 0, yFill, 0, pointCount);
                xFill[pointCount] = xPositions[pointCount - 1];
                yFill[pointCount] = chartBottom;
                xFill[pointCount + 1] = xPositions[0];
                yFill[pointCount + 1] = chartBottom;
                g2d.setColor(fillColor);
                g2d.fillPolygon(xFill, yFill, pointCount + 2);
            }

            // Draw lines connecting points
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pointCount - 1; i++) {
                g2d.drawLine(xPositions[i], yPositions[i], xPositions[i + 1], yPositions[i + 1]);
            }

            // Draw points and labels
            for (int i = 0; i < pointCount; i++) {
                PointData point = points.get(i);
                int x = xPositions[i];
                int y = yPositions[i];

                // Draw point (filled circle with border)
                g2d.setColor(Color.WHITE);
                g2d.fillOval(x - 6, y - 6, 12, 12);
                g2d.setColor(pointColor);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawOval(x - 6, y - 6, 12, 12);
                g2d.fillOval(x - 4, y - 4, 8, 8);

                // Draw value above point
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valueText = String.format("%.2f", point.value);
                int textWidth = g2d.getFontMetrics().stringWidth(valueText);
                g2d.drawString(valueText, x - textWidth / 2, y - 12);

                // Draw X-axis label
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2d.setColor(Color.GRAY);
                textWidth = g2d.getFontMetrics().stringWidth(point.label);
                // Rotate label if too many points
                if (pointCount > 4) {
                    Graphics2D g2dRotated = (Graphics2D) g2d.create();
                    g2dRotated.rotate(-Math.PI / 4, x, chartBottom + 15);
                    g2dRotated.drawString(point.label, x - textWidth / 2, chartBottom + 15);
                    g2dRotated.dispose();
                } else {
                    g2d.drawString(point.label, x - textWidth / 2, chartBottom + 20);
                }
            }
        }

        private static class PointData {
            String label;
            double value;

            PointData(String label, double value) {
                this.label = label;
                this.value = value;
            }
        }
    }

    /**
     * Bảng hiển thị top sinh viên
     */
    private static class TopStudentsPanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable table;

        TopStudentsPanel(String title) {
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            titleLabel.setForeground(Color.DARK_GRAY);
            add(titleLabel, BorderLayout.NORTH);

            tableModel = new DefaultTableModel(new Object[] { "Hạng", "Mã SV", "Họ và tên", "GPA" }, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = new JTable(tableModel);
            table.setRowHeight(28);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.setFillsViewportHeight(true);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);
        }

        void setStudents(java.util.List<StudentInfo> students) {
            clearStudents();
            int rank = 1;
            for (StudentInfo info : students) {
                tableModel.addRow(new Object[] {
                        rank++,
                        info.code,
                        info.name,
                        String.format("%.2f", info.gpa)
                });
            }
        }

        void clearStudents() {
            tableModel.setRowCount(0);
        }

        static class StudentInfo {
            final String code;
            final String name;
            final double gpa;

            StudentInfo(String code, String name, double gpa) {
                this.code = code;
                this.name = name;
                this.gpa = gpa;
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
