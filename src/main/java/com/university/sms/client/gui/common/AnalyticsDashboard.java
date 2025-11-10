package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard hiển thị thống kê và phân tích nâng cao
 */
public class AnalyticsDashboard extends JPanel {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private User currentUser;

    private JLabel titleLabel;
    private JPanel statsCardsPanel;
    private JPanel chartsPanel;
    private JButton refreshButton;

    // Stat cards
    private StatCard totalStudentsCard;
    private StatCard averageGPACard;
    private StatCard excellentStudentsCard;
    private StatCard failingStudentsCard;

    public AnalyticsDashboard(IServerConnection serverConnection) {
        this.serverConnection = serverConnection;
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

        refreshButton = new JButton("🔄 Làm mới");
        refreshButton.addActionListener(e -> refreshData());

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(refreshButton, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // Main content with scroll
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // Stats cards panel
        statsCardsPanel = createStatsCardsPanel();
        mainContent.add(statsCardsPanel);
        mainContent.add(Box.createVerticalStrut(20));

        // Charts panel
        chartsPanel = createChartsPanel();
        mainContent.add(chartsPanel);

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
        JPanel gradeDistChart = createGradeDistributionChart();
        panel.add(gradeDistChart);

        // GPA Trend Chart
        JPanel gpaTrendChart = createGPATrendChart();
        panel.add(gpaTrendChart);

        // Faculty Comparison Chart
        JPanel facultyChart = createFacultyComparisonChart();
        panel.add(facultyChart);

        // Top Performers Chart
        JPanel topPerformersChart = createTopPerformersChart();
        panel.add(topPerformersChart);

        return panel;
    }

    private JPanel createGradeDistributionChart() {
        ChartPanel chart = new ChartPanel("📊 Phân Bố Điểm");

        // Sample data - will be replaced with real data
        chart.addBar("A (Xuất sắc)", 20, Color.decode("#27ae60"));
        chart.addBar("B (Giỏi)", 35, Color.decode("#3498db"));
        chart.addBar("C (Khá)", 25, Color.decode("#f39c12"));
        chart.addBar("D (Trung bình)", 15, Color.decode("#e67e22"));
        chart.addBar("F (Yếu)", 5, Color.decode("#e74c3c"));

        return chart;
    }

    private JPanel createGPATrendChart() {
        ChartPanel chart = new ChartPanel("📈 Xu Hướng GPA Theo Học Kỳ");

        // Sample data
        chart.addBar("HK1-2023", 65, Color.decode("#3498db"));
        chart.addBar("HK2-2023", 70, Color.decode("#3498db"));
        chart.addBar("HK1-2024", 72, Color.decode("#3498db"));
        chart.addBar("HK2-2024", 75, Color.decode("#2ecc71"));

        return chart;
    }

    private JPanel createFacultyComparisonChart() {
        ChartPanel chart = new ChartPanel("🏛️ So Sánh GPA Các Khoa");

        // Sample data
        chart.addBar("Công nghệ TT", 80, Color.decode("#9b59b6"));
        chart.addBar("Kinh tế", 75, Color.decode("#e74c3c"));
        chart.addBar("Ngoại ngữ", 78, Color.decode("#f39c12"));
        chart.addBar("Y học", 82, Color.decode("#1abc9c"));

        return chart;
    }

    private JPanel createTopPerformersChart() {
        ChartPanel chart = new ChartPanel("🏆 Top 5 Sinh Viên Xuất Sắc");

        // Sample data
        chart.addBar("Nguyễn Văn A", 95, Color.decode("#f1c40f"));
        chart.addBar("Trần Thị B", 93, Color.decode("#95a5a6"));
        chart.addBar("Lê Văn C", 91, Color.decode("#cd7f32"));
        chart.addBar("Phạm Thị D", 89, Color.decode("#3498db"));
        chart.addBar("Hoàng Văn E", 87, Color.decode("#9b59b6"));

        return chart;
    }

    private void setupLayout() {
        // Layout is already set up
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
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

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    String facultyCode = getCurrentUserFacultyCode();
                    if (facultyCode == null || facultyCode.isEmpty()) {
                        // If can't determine faculty, return null (skip)
                        return null;
                    }

                    Message request = new Message();
                    request.setAction(Constants.ACTION_GET_FACULTY_STATISTICS);
                    Map<String, Object> data = new HashMap<>();
                    data.put("facultyCode", facultyCode);
                    request.setData(data);

                    Message response = serverConnection.sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stats = (Map<String, Object>) response.getData(Constants.KEY_STATISTICS);
                        return stats;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> stats = get();
                    if (stats != null) {
                        updateStatistics(stats);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    /**
     * Lấy facultyCode từ currentUser
     * - STUDENT: Lấy từ student.facultyCode
     * - TEACHER: Lấy từ courses/subjects (TODO: implement)
     * - ADMIN: Return null (có thể cải thiện để cho phép chọn)
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
                // TODO: Get facultyCode from teacher's courses/subjects
                // For now, return null
                return null;
            } else if (currentUser.getRole() == User.UserRole.ADMIN) {
                // Admin can view all, but need to select faculty
                // TODO: Could add UI to select faculty
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Default fallback
    }

    private void updateStatistics(Map<String, Object> stats) {
        // Update stat cards
        totalStudentsCard.setValue(stats.get("totalStudents").toString());

        Object avgGPA = stats.get("averageGPA");
        if (avgGPA != null) {
            averageGPACard.setValue(String.format("%.2f", ((Number) avgGPA).doubleValue()));
        }

        excellentStudentsCard.setValue(stats.get("excellentCount").toString());
        failingStudentsCard.setValue(stats.get("poorCount").toString());

        // Update charts with real data
        updateGradeDistributionChart(stats);
    }

    private void updateGradeDistributionChart(Map<String, Object> stats) {
        // TODO: Update chart with real data from stats
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
}
