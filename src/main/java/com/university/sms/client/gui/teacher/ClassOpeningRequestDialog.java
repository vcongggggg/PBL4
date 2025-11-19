package com.university.sms.client.gui.teacher;

import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.ClassOpeningRequest.RequestStatus;
import com.university.sms.model.Subject;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog for teacher to submit/edit class opening requests
 */
public class ClassOpeningRequestDialog extends JDialog {
    private ClassOpeningRequest request;
    private boolean confirmed = false;

    private JComboBox<Subject> subjectCombo;
    private JTextField academicYearField;
    private JComboBox<Integer> semesterCombo;
    private JComboBox<String> dayCombo;
    private JComboBox<Integer> startPeriodCombo;
    private JComboBox<Integer> endPeriodCombo;
    private JTextField roomField;
    private JSpinner maxStudentsSpinner;
    private JTextArea reasonArea;

    private List<Subject> subjects;
    private String teacherUsername;

    /**
     * Constructor for new request
     */
    public ClassOpeningRequestDialog(Frame owner, String teacherUsername, List<Subject> subjects) {
        super(owner, "Gửi Yêu Cầu Mở Lớp", true);
        this.teacherUsername = teacherUsername;
        this.subjects = subjects;
        this.request = null;

        initComponents();
        setLocationRelativeTo(owner);
    }

    /**
     * Constructor for editing existing request
     */
    public ClassOpeningRequestDialog(Frame owner, ClassOpeningRequest request, List<Subject> subjects) {
        super(owner, "Chỉnh Sửa Yêu Cầu Mở Lớp", true);
        this.teacherUsername = request.getTeacherUsername();
        this.subjects = subjects;
        this.request = request;

        initComponents();
        loadRequestData();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 550);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Row 0: Subject
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Môn học: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        subjectCombo = new JComboBox<>();
        for (Subject subject : subjects) {
            subjectCombo.addItem(subject);
        }
        subjectCombo.setRenderer(new SubjectComboRenderer());
        formPanel.add(subjectCombo, gbc);

        // Row 1: Academic Year
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Năm học: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        academicYearField = new JTextField();
        academicYearField.setToolTipText("Ví dụ: 2024-2025");
        // Đặt năm học mặc định là 2024-2025
        if (request == null) {
            academicYearField.setText("2024-2025");
        }
        formPanel.add(academicYearField, gbc);

        // Row 2: Semester
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Học kỳ: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        // Chỉ có 2 học kỳ: 1 và 2
        semesterCombo = new JComboBox<>(new Integer[] { 1, 2 });
        formPanel.add(semesterCombo, gbc);

        // Row 3: Schedule Day
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Thứ: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        String[] days = { "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật" };
        dayCombo = new JComboBox<>(days);
        formPanel.add(dayCombo, gbc);

        // Row 4: Start Period
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Tiết bắt đầu: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        Integer[] periods = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        startPeriodCombo = new JComboBox<>(periods);
        startPeriodCombo.setRenderer(new PeriodComboRenderer());
        formPanel.add(startPeriodCombo, gbc);

        // Row 5: End Period
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Tiết kết thúc: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        endPeriodCombo = new JComboBox<>(periods);
        endPeriodCombo.setRenderer(new PeriodComboRenderer());
        endPeriodCombo.setSelectedIndex(1); // Default to period 2
        formPanel.add(endPeriodCombo, gbc);

        // Row 6: Room
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Phòng học: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        roomField = new JTextField();
        roomField.setToolTipText("Ví dụ: A101, B202");
        formPanel.add(roomField, gbc);

        // Row 7: Max Students
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Sĩ số tối đa: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(30, 1, 100, 1);
        maxStudentsSpinner = new JSpinner(spinnerModel);
        formPanel.add(maxStudentsSpinner, gbc);

        // Row 8: Reason (multi-line)
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Lý do: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        reasonArea = new JTextArea(4, 20);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setToolTipText("Nêu rõ lý do cần mở lớp này");
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        formPanel.add(reasonScroll, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Info label
        JLabel infoLabel = new JLabel("<html><i>* Các trường bắt buộc</i></html>");
        infoLabel.setForeground(Color.GRAY);
        mainPanel.add(infoLabel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton submitBtn = new JButton(request == null ? "Gửi Yêu Cầu" : "Cập Nhật");
        submitBtn.addActionListener(e -> onSubmit());

        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> onCancel());

        buttonPanel.add(submitBtn);
        buttonPanel.add(cancelBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void loadRequestData() {
        if (request == null)
            return;

        // Find and select subject
        for (int i = 0; i < subjectCombo.getItemCount(); i++) {
            Subject subject = subjectCombo.getItemAt(i);
            if (subject.getSubjectCode() != null && subject.getSubjectCode().equals(request.getSubjectCode())) {
                subjectCombo.setSelectedIndex(i);
                break;
            }
        }

        academicYearField.setText(request.getAcademicYear());
        semesterCombo.setSelectedItem(request.getSemester());
        dayCombo.setSelectedItem(request.getScheduleDay());

        // Parse schedule time to get start and end periods
        String scheduleTime = request.getScheduleTime();
        if (scheduleTime != null && !scheduleTime.isEmpty()) {
            parsePeriods(scheduleTime);
        }

        roomField.setText(request.getRoom());
        maxStudentsSpinner.setValue(request.getMaxStudents());
        reasonArea.setText(request.getReason());
    }

    /**
     * Parse schedule time string to extract start and end periods
     */
    private void parsePeriods(String scheduleTime) {
        try {
            // Try format: "Tiết 1-3" or "1-3"
            String periodStr = scheduleTime;
            if (scheduleTime.toLowerCase().contains("tiết")) {
                periodStr = scheduleTime.split("(?i)tiết")[1].trim().split("\\(")[0].trim();
            }

            String[] parts = periodStr.split("-");
            if (parts.length == 2) {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());

                if (start >= 1 && start <= 12) {
                    startPeriodCombo.setSelectedItem(start);
                }
                if (end >= 1 && end <= 12) {
                    endPeriodCombo.setSelectedItem(end);
                }
            }
        } catch (Exception e) {
            // Use defaults if parsing fails
        }
    }

    private void onSubmit() {
        // Validate inputs
        if (subjectCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn môn học!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String academicYear = academicYearField.getText().trim();
        if (academicYear.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập năm học!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            academicYearField.requestFocus();
            return;
        }

        // Validate periods
        int startPeriod = (Integer) startPeriodCombo.getSelectedItem();
        int endPeriod = (Integer) endPeriodCombo.getSelectedItem();

        if (startPeriod > endPeriod) {
            JOptionPane.showMessageDialog(this, "Tiết bắt đầu phải nhỏ hơn hoặc bằng tiết kết thúc!",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            startPeriodCombo.requestFocus();
            return;
        }

        String room = roomField.getText().trim();
        if (room.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập phòng học!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            roomField.requestFocus();
            return;
        }

        String reason = reasonArea.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            reasonArea.requestFocus();
            return;
        }

        if (reason.length() > 500) {
            JOptionPane.showMessageDialog(this, "Lý do quá dài! Tối đa 500 ký tự.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            reasonArea.requestFocus();
            return;
        }

        // Create or update request object
        if (request == null) {
            request = new ClassOpeningRequest();
            request.setTeacherUsername(teacherUsername);
            request.setRequestStatus(RequestStatus.PENDING);
        }

        Subject selectedSubject = (Subject) subjectCombo.getSelectedItem();
        request.setSubjectCode(selectedSubject.getSubjectCode());
        request.setAcademicYear(academicYear);
        request.setSemester((Integer) semesterCombo.getSelectedItem());
        request.setScheduleDay((String) dayCombo.getSelectedItem());

        // Build schedule time string from periods (already declared above)
        String scheduleTime = buildScheduleTime(startPeriod, endPeriod);
        request.setScheduleTime(scheduleTime);

        request.setRoom(room);
        request.setMaxStudents((Integer) maxStudentsSpinner.getValue());
        request.setReason(reason);

        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ClassOpeningRequest getRequest() {
        return request;
    }

    /**
     * Build schedule time string from start and end periods
     */
    private String buildScheduleTime(int startPeriod, int endPeriod) {
        // Map of period to time
        String[] timeRanges = {
                "07:00-07:50", "08:00-08:50", "09:00-09:50", "10:00-10:50",
                "11:00-11:50", "13:00-13:50", "14:00-14:50", "15:00-15:50",
                "16:00-16:50", "17:00-17:50", "18:00-18:50", "19:00-19:50"
        };

        if (startPeriod < 1 || startPeriod > 12 || endPeriod < 1 || endPeriod > 12) {
            return startPeriod + "-" + endPeriod;
        }

        String startTime = timeRanges[startPeriod - 1].split("-")[0];
        String endTime = timeRanges[endPeriod - 1].split("-")[1];

        return "Tiết " + startPeriod + "-" + endPeriod + " (" + startTime + "-" + endTime + ")";
    }

    /**
     * Custom renderer for Subject combo box
     */
    private static class SubjectComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Subject) {
                Subject subject = (Subject) value;
                setText(subject.getSubjectCode() + " - " + subject.getSubjectName() +
                        " (" + subject.getCredits() + " TC)");
            }

            return this;
        }
    }

    /**
     * Custom renderer for Period combo box
     */
    private static class PeriodComboRenderer extends DefaultListCellRenderer {
        private static final String[] TIME_RANGES = {
                "07:00-07:50", "08:00-08:50", "09:00-09:50", "10:00-10:50",
                "11:00-11:50", "13:00-13:50", "14:00-14:50", "15:00-15:50",
                "16:00-16:50", "17:00-17:50", "18:00-18:50", "19:00-19:50"
        };

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Integer) {
                int period = (Integer) value;
                if (period >= 1 && period <= 12) {
                    setText("Tiết " + period + " (" + TIME_RANGES[period - 1] + ")");
                }
            }

            return this;
        }
    }
}
