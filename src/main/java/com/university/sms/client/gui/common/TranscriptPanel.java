package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Transcript;
import com.university.sms.model.Transcript.SemesterRecord;
import com.university.sms.model.Transcript.CourseRecord;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel hiển thị học bạ/bảng điểm tổng hợp
 */
public class TranscriptPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TranscriptPanel.class.getName());

    private IServerConnection serverConnection;
    private User currentUser;
    private Transcript currentTranscript;

    private JLabel titleLabel;
    private JPanel summaryPanel;
    private JTabbedPane semesterTabs;
    private JButton refreshButton;
    private JButton exportButton;

    // Summary components
    private JLabel gpaLabel;
    private JLabel rankLabel;
    private JLabel creditsLabel;
    private JLabel completedLabel;
    private JProgressBar gpaProgressBar;

    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    public TranscriptPanel(IServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
    }

    private void setupEventListeners() {
        refreshButton.addActionListener(e -> refreshData());
        exportButton.addActionListener(e -> exportToPDF());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (isInitialized && !isRefreshing && currentUser != null) {
                    refreshData();
                }
            }
        });
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleLabel = new JLabel("Học Bạ & Kết Quả Học Tập", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Làm mới");
        exportButton = new JButton("Xuất PDF");

        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(buttonPanel, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        summaryPanel = createSummaryPanel();
        add(summaryPanel, BorderLayout.WEST);

        semesterTabs = new JTabbedPane(JTabbedPane.TOP);
        semesterTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(semesterTabs, BorderLayout.CENTER);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Tổng Quan"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.setPreferredSize(new Dimension(280, 0));

        JPanel gpaCard = createInfoCard("GPA Tích Lũy", "0.00", Color.decode("#3498db"));
        gpaLabel = (JLabel) ((JPanel) gpaCard.getComponent(2)).getComponent(0);

        gpaProgressBar = new JProgressBar(0, 400);
        gpaProgressBar.setStringPainted(true);
        gpaProgressBar.setForeground(Color.decode("#2ecc71"));
        gpaCard.add(gpaProgressBar);

        JPanel rankCard = createInfoCard("Xếp Loại", "Chưa có", Color.decode("#e74c3c"));
        rankLabel = (JLabel) ((JPanel) rankCard.getComponent(2)).getComponent(0);

        JPanel creditsCard = createInfoCard("Tín Chỉ Tích Lũy", "0", Color.decode("#f39c12"));
        creditsLabel = (JLabel) ((JPanel) creditsCard.getComponent(2)).getComponent(0);

        JPanel completedCard = createInfoCard("Môn Hoàn Thành", "0", Color.decode("#9b59b6"));
        completedLabel = (JLabel) ((JPanel) completedCard.getComponent(2)).getComponent(0);

        panel.add(gpaCard);
        panel.add(Box.createVerticalStrut(10));
        panel.add(rankCard);
        panel.add(Box.createVerticalStrut(10));
        panel.add(creditsCard);
        panel.add(Box.createVerticalStrut(10));
        panel.add(completedCard);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createInfoCard(String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(Color.DARK_GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        valuePanel.setOpaque(false);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);
        valuePanel.add(valueLabel);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(valuePanel);

        return card;
    }

    private void setupLayout() {
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void refreshData() {
        if (currentUser == null)
            return;

        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;

        SwingWorker<Transcript, Void> worker = new SwingWorker<>() {
            @Override
            protected Transcript doInBackground() {
                try {
                    Message request = new Message();
                    request.setAction(Constants.ACTION_GET_TRANSCRIPT);

                    Message response = serverConnection.sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        return response.getData(Constants.KEY_TRANSCRIPT, Transcript.class);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải học bạ", e);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Transcript transcript = get();
                    if (transcript != null) {
                        displayTranscript(transcript);
                    } else {
                        JOptionPane.showMessageDialog(TranscriptPanel.this,
                                "Không thể tải học bạ",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TranscriptPanel.this,
                            "Không thể tải học bạ: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    private void displayTranscript(Transcript transcript) {
        this.currentTranscript = transcript;

        BigDecimal gpa = transcript.getCumulativeGPA();
        if (gpa == null || gpa.compareTo(BigDecimal.ZERO) == 0) {
            gpaLabel.setText("Chưa có");
            gpaProgressBar.setValue(0);
            gpaProgressBar.setString("-- / 4.00");
            gpaProgressBar.setForeground(Color.decode("#95a5a6"));
        } else {
            gpaLabel.setText(String.format("%.2f", gpa.doubleValue()));
            gpaProgressBar.setValue((int) (gpa.doubleValue() * 100));
            gpaProgressBar.setString(String.format("%.2f / 4.00", gpa.doubleValue()));

            if (gpa.doubleValue() >= 3.6) {
                gpaProgressBar.setForeground(Color.decode("#27ae60"));
            } else if (gpa.doubleValue() >= 3.2) {
                gpaProgressBar.setForeground(Color.decode("#3498db"));
            } else if (gpa.doubleValue() >= 2.5) {
                gpaProgressBar.setForeground(Color.decode("#f39c12"));
            } else {
                gpaProgressBar.setForeground(Color.decode("#e74c3c"));
            }
        }

        String rank = transcript.getAcademicRank();
        if (rank == null || rank.trim().isEmpty() || "Chưa có dữ liệu".equalsIgnoreCase(rank)) {
            rankLabel.setText("Chưa có");
        } else {
            rankLabel.setText(rank);
        }

        creditsLabel.setText(String.valueOf(transcript.getTotalCreditsEarned()));
        completedLabel.setText(String.valueOf(transcript.getTotalCoursesCompleted()));

        semesterTabs.removeAll();

        JPanel allCoursesPanel = createAllCoursesPanel(transcript);
        semesterTabs.addTab("Tất Cả", allCoursesPanel);

        List<SemesterRecord> semesters = transcript.getSemesterRecords();
        for (SemesterRecord semester : semesters) {
            JPanel semesterPanel = createSemesterPanel(semester);
            String tabTitle = String.format("HK%d - %s", semester.getSemester(), semester.getAcademicYear());
            semesterTabs.addTab(tabTitle, semesterPanel);
        }

        revalidate();
        repaint();
    }

    private JPanel createAllCoursesPanel(Transcript transcript) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = { "STT", "Học kỳ", "Mã môn", "Tên môn", "Tín chỉ", "Điểm số", "Điểm chữ", "Điểm 4" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int stt = 1;
        for (SemesterRecord semester : transcript.getSemesterRecords()) {
            for (CourseRecord course : semester.getCourses()) {
                boolean hasGrade = hasCourseGrade(course);
                Object[] row = {
                        stt++,
                        String.format("HK%d-%s", semester.getSemester(), semester.getAcademicYear()),
                        course.getCourseCode(),
                        course.getSubjectName(),
                        course.getCredits(),
                        formatCourseGrade(course.getFinalGrade(), hasGrade),
                        formatCourseLetter(course.getLetterGrade(), hasGrade),
                        formatCourseGrade(course.getGradePoints(), hasGrade)
                };
                model.addRow(row);
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);

                if (value != null && !isSelected) {
                    String grade = value.toString();
                    if (grade.startsWith("A")) {
                        c.setForeground(Color.decode("#27ae60"));
                    } else if (grade.startsWith("B")) {
                        c.setForeground(Color.decode("#3498db"));
                    } else if (grade.startsWith("C")) {
                        c.setForeground(Color.decode("#f39c12"));
                    } else if (grade.startsWith("D")) {
                        c.setForeground(Color.decode("#e67e22"));
                    } else if (grade.equals("F")) {
                        c.setForeground(Color.decode("#e74c3c"));
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSemesterPanel(SemesterRecord semester) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel semesterGPALabel = new JLabel(String.format("GPA Học Kỳ: %.2f",
                semester.getSemesterGPA() != null ? semester.getSemesterGPA().doubleValue() : 0.0));
        semesterGPALabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel creditsLabel = new JLabel(String.format("Tín Chỉ Đạt: %d", semester.getCreditsEarned()));
        creditsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel coursesLabel = new JLabel(String.format("Số Môn: %d", semester.getCourses().size()));
        coursesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        summaryPanel.add(semesterGPALabel);
        summaryPanel.add(creditsLabel);
        summaryPanel.add(coursesLabel);

        panel.add(summaryPanel, BorderLayout.NORTH);

        String[] columnNames = { "STT", "Mã môn", "Tên môn", "Tín chỉ", "Điểm số", "Điểm chữ", "Điểm 4", "Trạng thái" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int stt = 1;
        for (CourseRecord course : semester.getCourses()) {
            boolean hasGrade = hasCourseGrade(course);
            Object[] row = {
                    stt++,
                    course.getCourseCode(),
                    course.getSubjectName(),
                    course.getCredits(),
                    formatCourseGrade(course.getFinalGrade(), hasGrade),
                    formatCourseLetter(course.getLetterGrade(), hasGrade),
                    formatCourseGrade(course.getGradePoints(), hasGrade),
                    getStatusText(course.getStatus())
            };
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 8; i++) {
            if (i != 2) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String getStatusText(String status) {
        if (status == null)
            return "N/A";
        switch (status.toLowerCase()) {
            case "completed":
                return "Hoàn thành";
            case "failed":
                return "Không đạt";
            case "enrolled":
                return "Đang học";
            case "dropped":
                return "Bỏ học";
            default:
                return status;
        }
    }

    private void exportToPDF() {
        if (currentTranscript == null) {
            JOptionPane.showMessageDialog(this,
                    "Chưa có dữ liệu để xuất",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Tính năng xuất PDF đang được phát triển...\n\n" +
                        "Thông tin sẽ xuất:\n" +
                        "- Thông tin sinh viên\n" +
                        "- Điểm chi tiết từng học kỳ\n" +
                        "- GPA tích lũy: " + String.format("%.2f", currentTranscript.getCumulativeGPA()) + "\n" +
                        "- Xếp loại: " + currentTranscript.getAcademicRank(),
                "Xuất Học Bạ",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean hasCourseGrade(CourseRecord course) {
        if (course == null) {
            return false;
        }

        if (course.getLetterGrade() != null && !course.getLetterGrade().trim().isEmpty()
                && !"N/A".equalsIgnoreCase(course.getLetterGrade().trim())) {
            return true;
        }

        if (course.getFinalGrade() != null && course.getFinalGrade().compareTo(BigDecimal.ZERO) != 0) {
            return true;
        }

        if (course.getGradePoints() != null && course.getGradePoints().compareTo(BigDecimal.ZERO) != 0) {
            return true;
        }

        return false;
    }

    private String formatCourseGrade(BigDecimal value, boolean hasGrade) {
        if (!hasGrade || value == null) {
            return "";
        }
        return String.format("%.2f", value);
    }

    private String formatCourseLetter(String letter, boolean hasGrade) {
        if (!hasGrade || letter == null) {
            return "";
        }
        return letter;
    }
}
