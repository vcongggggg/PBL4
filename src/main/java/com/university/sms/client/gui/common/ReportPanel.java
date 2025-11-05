package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            AnalyticsDashboard analyticsDashboard = new AnalyticsDashboard(serverConnection);
            analyticsDashboard.setCurrentUser(currentUser);
            tabbedPane.addTab("Thống kê", analyticsDashboard);
        } else {
            // Student chỉ thấy statistics đơn giản
            JPanel statisticsPanel = createStatisticsPanel();
            tabbedPane.addTab("Thống kê", statisticsPanel);
        }

        // Tab 2: Detailed Reports
        JPanel detailReportPanel = createDetailReportPanel();
        tabbedPane.addTab("Báo cáo Chi tiết", detailReportPanel);

        // Tab 3: Charts (for Admin/Teacher)
        if (currentUser.getRole() != User.UserRole.STUDENT) {
            JPanel chartsPanel = createChartsPanel();
            tabbedPane.addTab("Biểu đồ", chartsPanel);
        }
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Statistics cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            cardsPanel.add(createStatCard("Tổng số Sinh viên", "1,234", Color.BLUE));
            cardsPanel.add(createStatCard("Tổng số Giảng viên", "156", Color.GREEN));
            cardsPanel.add(createStatCard("Tổng số Môn học", "89", Color.ORANGE));
            cardsPanel.add(createStatCard("Tổng số Lớp học", "245", Color.RED));
        } else if (currentUser.getRole() == User.UserRole.TEACHER) {
            cardsPanel.add(createStatCard("Số lớp đang dạy", "5", Color.BLUE));
            cardsPanel.add(createStatCard("Tổng số Sinh viên", "187", Color.GREEN));
            cardsPanel.add(createStatCard("Điểm TB lớp", "7.8", Color.ORANGE));
            cardsPanel.add(createStatCard("Tỷ lệ đậu", "92%", Color.RED));
        }

        // Quick stats table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Thống kê nhanh"));

        String[] columnNames;
        Object[][] data;

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            columnNames = new String[] { "Khoa", "Số SV", "Số GV", "Tỷ lệ đậu" };
            data = new Object[][] {
                    { "Công nghệ Thông tin", 450, 45, "90%" },
                    { "Kinh tế", 380, 38, "88%" },
                    { "Ngoại ngữ", 320, 32, "92%" },
                    { "Khoa học Tự nhiên", 84, 41, "85%" }
            };
        } else {
            columnNames = new String[] { "Môn học", "Số SV", "Điểm TB", "Tỷ lệ đậu" };
            data = new Object[][] {
                    { "Lập trình Java", 45, "7.5", "88%" },
                    { "Cơ sở dữ liệu", 38, "8.2", "95%" },
                    { "Mạng máy tính", 42, "7.8", "90%" }
            };
        }

        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(cardsPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
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

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel chartLabel = new JLabel("Biểu đồ thống kê", JLabel.CENTER);
        chartLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Sample chart placeholder
        JPanel chartArea = new JPanel();
        chartArea.setLayout(new BoxLayout(chartArea, BoxLayout.Y_AXIS));
        chartArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Chart 1
        JPanel chart1 = createChartPlaceholder("Biểu đồ phân bố điểm", 200);
        chartArea.add(chart1);
        chartArea.add(Box.createVerticalStrut(20));

        // Chart 2
        JPanel chart2 = createChartPlaceholder("Biểu đồ tỷ lệ đậu/rớt theo khoa", 200);
        chartArea.add(chart2);

        JButton refreshChartsButton = new JButton("Làm mới biểu đồ");
        refreshChartsButton.addActionListener(e -> refreshCharts());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshChartsButton);

        panel.add(chartLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(chartArea), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

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

    private JPanel createChartPlaceholder(String title, int height) {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBorder(BorderFactory.createTitledBorder(title));
        placeholder.setPreferredSize(new Dimension(0, height));
        placeholder.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        JLabel label = new JLabel("Biểu đồ sẽ được hiển thị ở đây", JLabel.CENTER);
        label.setForeground(Color.GRAY);
        placeholder.add(label, BorderLayout.CENTER);

        return placeholder;
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
            } finally {
                isRefreshing = false;
            }
        });
    }

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        String semester = (String) semesterCombo.getSelectedItem();

        StringBuilder report = new StringBuilder();
        report.append("===================================================\n");
        report.append("         ").append(reportType.toUpperCase()).append("\n");
        report.append("===================================================\n\n");
        report.append("Học kỳ: ").append(semester).append("\n");
        report.append("Ngày tạo: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        report.append("Người tạo: ").append(currentUser.getFullName()).append("\n\n");
        report.append("---------------------------------------------------\n\n");

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            report.append("I. TỔNG QUAN\n\n");
            report.append("   1. Tổng số sinh viên: 1,234\n");
            report.append("   2. Tổng số giảng viên: 156\n");
            report.append("   3. Tổng số môn học: 89\n");
            report.append("   4. Tổng số lớp học: 245\n\n");

            report.append("II. THỐNG KÊ THEO KHOA\n\n");
            report.append(String.format("   %-30s %10s %10s %10s\n", "Khoa", "Sinh viên", "Giảng viên", "Tỷ lệ đậu"));
            report.append("   " + "-".repeat(70) + "\n");
            report.append(String.format("   %-30s %10s %10s %10s\n", "Công nghệ Thông tin", "450", "45", "90%"));
            report.append(String.format("   %-30s %10s %10s %10s\n", "Kinh tế", "380", "38", "88%"));
            report.append(String.format("   %-30s %10s %10s %10s\n", "Ngoại ngữ", "320", "32", "92%"));
            report.append(String.format("   %-30s %10s %10s %10s\n\n", "Khoa học Tự nhiên", "84", "41", "85%"));

        } else if (currentUser.getRole() == User.UserRole.TEACHER) {
            report.append("I. THÔNG TIN GIẢNG VIÊN\n\n");
            report.append("   Họ tên: ").append(currentUser.getFullName()).append("\n");
            report.append("   Số lớp đang dạy: 5\n");
            report.append("   Tổng số sinh viên: 187\n\n");

            report.append("II. KẾT QUẢ DẠY HỌC\n\n");
            report.append(String.format("   %-25s %10s %10s %12s\n", "Môn học", "Số SV", "Điểm TB", "Tỷ lệ đậu"));
            report.append("   " + "-".repeat(60) + "\n");
            report.append(String.format("   %-25s %10s %10s %12s\n", "Lập trình Java", "45", "7.5", "88%"));
            report.append(String.format("   %-25s %10s %10s %12s\n", "Cơ sở dữ liệu", "38", "8.2", "95%"));
            report.append(String.format("   %-25s %10s %10s %12s\n\n", "Mạng máy tính", "42", "7.8", "90%"));
        }

        report.append("---------------------------------------------------\n");
        report.append("\nGhi chú: Đây là báo cáo mẫu. Dữ liệu thực tế sẽ được\n");
        report.append("         lấy từ hệ thống trong phiên bản tiếp theo.\n");
        report.append("\n===================================================\n");

        reportTextArea.setText(report.toString());
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

    private void refreshCharts() {
        JOptionPane.showMessageDialog(this,
                "Biểu đồ đã được làm mới!",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
