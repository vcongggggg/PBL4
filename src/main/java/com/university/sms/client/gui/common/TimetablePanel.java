package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.TimetableEntry;
import com.university.sms.model.TimetableEntry.DayOfWeek;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel hiển thị thời khóa biểu dạng lịch tuần
 */
public class TimetablePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private User currentUser;

    private JPanel calendarPanel;
    private JLabel titleLabel;
    private JButton refreshButton;
    private JButton exportButton;

    // Calendar grid: [period][day]
    private JPanel[][] cellPanels;
    private static final int MAX_PERIODS = 12; // Tiết 1-12

    private boolean isRefreshing = false;
    private boolean isInitialized = false;
    private static final String[] TIME_RANGES = {
            "07:00-07:50", "08:00-08:50", "09:00-09:50", "10:00-10:50",
            "11:00-11:50", "13:00-13:50", "14:00-14:50", "15:00-15:50",
            "16:00-16:50", "17:00-17:50", "18:00-18:50", "19:00-19:50"
    };

    public TimetablePanel(IServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        initializeComponents();
        setupLayout();
        setupEventListeners();
        isInitialized = true;
    }

    private void setupEventListeners() {
        refreshButton.addActionListener(e -> refreshData());
        exportButton.addActionListener(e -> exportToPDF());

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
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleLabel = new JLabel("📅 Thời Khóa Biểu", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("🔄 Làm mới");
        exportButton = new JButton("📥 Xuất PDF");

        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(buttonPanel, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // Calendar panel
        calendarPanel = new JPanel();
        calendarPanel.setLayout(new GridBagLayout());
        calendarPanel.setBackground(Color.WHITE);

        createCalendarGrid();

        JScrollPane scrollPane = new JScrollPane(calendarPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Event listeners
        refreshButton.addActionListener(e -> refreshData());
        exportButton.addActionListener(e -> exportToPDF());
    }

    private void createCalendarGrid() {
        calendarPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0;

        // Header row - Days of week
        String[] days = { "Tiết/Thời gian", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN" };

        for (int col = 0; col < days.length; col++) {
            JPanel headerCell = createHeaderCell(days[col]);
            gbc.gridx = col;
            gbc.gridy = 0;
            gbc.weightx = (col == 0) ? 0.2 : 1.0;
            gbc.weighty = 0.05;
            calendarPanel.add(headerCell, gbc);
        }

        // Initialize cell panels array
        cellPanels = new JPanel[MAX_PERIODS][7]; // 12 periods x 7 days

        // Create grid cells
        gbc.weighty = 1.0;
        for (int period = 0; period < MAX_PERIODS; period++) {
            // Time column
            JPanel timeCell = createTimeCell(period + 1, TIME_RANGES[period]);
            gbc.gridx = 0;
            gbc.gridy = period + 1;
            gbc.weightx = 0.2;
            calendarPanel.add(timeCell, gbc);

            // Day columns
            for (int day = 0; day < 7; day++) {
                JPanel emptyCell = createEmptyCell();
                cellPanels[period][day] = emptyCell;
                gbc.gridx = day + 1;
                gbc.weightx = 1.0;
                calendarPanel.add(emptyCell, gbc);
            }
        }
    }

    private JPanel createHeaderCell(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(41, 128, 185)); // Blue
        panel.setBorder(new LineBorder(Color.WHITE, 2));

        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTimeCell(int period, String timeRange) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(236, 240, 241)); // Light gray
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));

        JLabel periodLabel = new JLabel("Tiết " + period, JLabel.CENTER);
        periodLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel timeLabel = new JLabel(timeRange, JLabel.CENTER);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);

        JPanel content = new JPanel(new GridLayout(2, 1));
        content.setBackground(panel.getBackground());
        content.add(periodLabel);
        content.add(timeLabel);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEmptyCell() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        panel.setPreferredSize(new Dimension(150, 80));
        return panel;
    }

    private JPanel createCourseCell(TimetableEntry entry) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Parse color
        Color bgColor = Color.decode(entry.getColor() != null ? entry.getColor() : "#FFB3BA");
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Subject name
        JLabel subjectLabel = new JLabel(entry.getSubjectName());
        subjectLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subjectLabel.setForeground(Color.BLACK);

        // Room and teacher
        String details = String.format("📍 %s\n👨‍🏫 %s",
                entry.getRoom() != null ? entry.getRoom() : "N/A",
                entry.getTeacherName() != null ? entry.getTeacherName() : "N/A");

        JTextArea detailsArea = new JTextArea(details);
        detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        detailsArea.setForeground(Color.DARK_GRAY);
        detailsArea.setOpaque(false);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        panel.add(subjectLabel, BorderLayout.NORTH);
        panel.add(detailsArea, BorderLayout.CENTER);

        // Tooltip
        panel.setToolTipText(String.format(
                "<html><b>%s</b><br>Mã: %s<br>Phòng: %s<br>GV: %s<br>Tiết: %d-%d</html>",
                entry.getSubjectName(),
                entry.getCourseCode(),
                entry.getRoom(),
                entry.getTeacherName(),
                entry.getStartPeriod(),
                entry.getEndPeriod()));

        // Click to view details
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showCourseDetails(entry);
            }
        });

        return panel;
    }

    private void setupLayout() {
        // Layout is already set up in initializeComponents
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Don't auto-refresh here, let ComponentListener handle it
        // refreshData();
    }

    public void refreshData() {
        if (currentUser == null)
            return;

        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;

        SwingWorker<List<TimetableEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TimetableEntry> doInBackground() {
                try {
                    Message request = new Message();
                    request.setAction(Constants.ACTION_GET_TIMETABLE);
                    Map<String, Object> data = new HashMap<>();
                    data.put(Constants.KEY_USER_ID, currentUser.getUserId());
                    data.put(Constants.KEY_USER_ROLE, currentUser.getRole().toString());
                    request.setData(data);

                    Message response = serverConnection.sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<TimetableEntry> entries = (List<TimetableEntry>) response.getData(Constants.KEY_TIMETABLE);
                        return entries;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    List<TimetableEntry> entries = get();
                    if (entries == null) {
                        System.out.println("TimetablePanel: Received null entries from server");
                        entries = new ArrayList<>();
                    } else {
                        System.out.println("TimetablePanel: Received " + entries.size() + " entries from server");
                    }
                    displayTimetable(entries);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(TimetablePanel.this,
                            "Không thể tải thời khóa biểu: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    private void displayTimetable(List<TimetableEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            System.out.println("TimetablePanel: No entries to display");
            return;
        }

        System.out.println("TimetablePanel: Displaying " + entries.size() + " entries");

        // Clear all cells first
        for (int period = 0; period < MAX_PERIODS; period++) {
            for (int day = 0; day < 7; day++) {
                JPanel cell = cellPanels[period][day];
                if (cell != null) {
                    cell.removeAll();
                    cell.setBackground(Color.WHITE);
                    cell.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
                }
            }
        }

        // Place entries in grid
        int placedCount = 0;
        for (TimetableEntry entry : entries) {
            if (entry == null) {
                System.out.println("TimetablePanel: Entry is null, skipping");
                continue;
            }

            if (entry.getDayOfWeek() == null) {
                System.out.println("TimetablePanel: Entry has null dayOfWeek: " + entry.getSubjectName());
                continue;
            }

            int dayIndex = getDayIndex(entry.getDayOfWeek());
            if (dayIndex < 0 || dayIndex >= 7) {
                System.out.println("TimetablePanel: Invalid dayIndex: " + dayIndex + " for " + entry.getSubjectName());
                continue;
            }

            int startPeriod = entry.getStartPeriod();
            int endPeriod = entry.getEndPeriod();

            System.out.println("TimetablePanel: Processing entry: " + entry.getSubjectName() +
                    ", Day: " + entry.getDayOfWeek() + " (index: " + dayIndex + ")" +
                    ", Period: " + startPeriod + "-" + endPeriod);

            if (startPeriod < 1 || startPeriod > MAX_PERIODS) {
                System.out.println("TimetablePanel: Invalid startPeriod: " + startPeriod);
                continue;
            }

            // Place in first period
            int periodIndex = startPeriod - 1;
            if (periodIndex < MAX_PERIODS && cellPanels[periodIndex][dayIndex] != null) {
                JPanel coursePanel = createCourseCell(entry);

                // Replace cell in grid
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = dayIndex + 1;
                gbc.gridy = periodIndex + 1;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1.0;
                gbc.weighty = 1.0;

                // If course spans multiple periods, increase height
                if (endPeriod > startPeriod && endPeriod <= MAX_PERIODS) {
                    gbc.gridheight = endPeriod - startPeriod + 1;
                } else {
                    gbc.gridheight = 1;
                }

                calendarPanel.remove(cellPanels[periodIndex][dayIndex]);
                calendarPanel.add(coursePanel, gbc);
                cellPanels[periodIndex][dayIndex] = coursePanel;
                placedCount++;
            }
        }

        System.out.println("TimetablePanel: Placed " + placedCount + " entries in grid");

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private int getDayIndex(DayOfWeek day) {
        switch (day) {
            case MONDAY:
                return 0;
            case TUESDAY:
                return 1;
            case WEDNESDAY:
                return 2;
            case THURSDAY:
                return 3;
            case FRIDAY:
                return 4;
            case SATURDAY:
                return 5;
            case SUNDAY:
                return 6;
            default:
                return -1;
        }
    }

    private void showCourseDetails(TimetableEntry entry) {
        String message = String.format(
                "Môn học: %s\n" +
                        "Mã lớp: %s\n" +
                        "Phòng: %s\n" +
                        "Giảng viên: %s\n" +
                        "Thời gian: %s - Tiết %d-%d\n" +
                        "Số tín chỉ: %d",
                entry.getSubjectName(),
                entry.getCourseCode(),
                entry.getRoom(),
                entry.getTeacherName(),
                entry.getDayOfWeek().getDisplayName(),
                entry.getStartPeriod(),
                entry.getEndPeriod(),
                entry.getCredits());

        JOptionPane.showMessageDialog(this,
                message,
                "Chi tiết lớp học",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToPDF() {
        JOptionPane.showMessageDialog(this,
                "Tính năng xuất PDF đang được phát triển...",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
