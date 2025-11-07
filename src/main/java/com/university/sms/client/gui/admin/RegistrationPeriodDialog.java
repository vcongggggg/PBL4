package com.university.sms.client.gui.admin;

import com.university.sms.model.RegistrationPeriod;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Dialog for creating/editing registration period
 */
public class RegistrationPeriodDialog extends JDialog {
    private RegistrationPeriod period;
    private boolean confirmed = false;

    private JTextField txtAcademicYear;
    private JComboBox<String> cmbSemester;
    private JTextField txtStartDate;  // Format: dd/MM/yyyy
    private JTextField txtEndDate;    // Format: dd/MM/yyyy
    private JSpinner spinnerStartHour;
    private JSpinner spinnerStartMinute;
    private JSpinner spinnerEndHour;
    private JSpinner spinnerEndMinute;
    private JTextArea txtDescription;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private JButton btnSave;
    private JButton btnCancel;

    public RegistrationPeriodDialog(Frame parent, RegistrationPeriod period) {
        super(parent, period == null ? "Tạo kỳ đăng ký mới" : "Chỉnh sửa kỳ đăng ký", true);
        this.period = period;

        initializeComponents();
        setupLayout();
        setupEventListeners();

        if (period != null) {
            loadPeriodData();
        } else {
            setDefaultValues();
        }

        setSize(500, 500);
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        // Academic Year
        txtAcademicYear = new JTextField(20);

        // Semester
        String[] semesters = {"Học kỳ 1", "Học kỳ 2", "Học kỳ 3"};
        cmbSemester = new JComboBox<>(semesters);

        // Date text fields
        txtStartDate = new JTextField(15);
        txtStartDate.setToolTipText("Định dạng: dd/MM/yyyy (ví dụ: 01/08/2024)");
        txtEndDate = new JTextField(15);
        txtEndDate.setToolTipText("Định dạng: dd/MM/yyyy (ví dụ: 15/08/2024)");

        // Time spinners
        SpinnerNumberModel hourModel1 = new SpinnerNumberModel(0, 0, 23, 1);
        SpinnerNumberModel minuteModel1 = new SpinnerNumberModel(0, 0, 59, 1);
        SpinnerNumberModel hourModel2 = new SpinnerNumberModel(23, 0, 23, 1);
        SpinnerNumberModel minuteModel2 = new SpinnerNumberModel(59, 0, 59, 1);

        spinnerStartHour = new JSpinner(hourModel1);
        spinnerStartMinute = new JSpinner(minuteModel1);
        spinnerEndHour = new JSpinner(hourModel2);
        spinnerEndMinute = new JSpinner(minuteModel2);

        // Description
        txtDescription = new JTextArea(5, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);

        // Buttons
        btnSave = new JButton("💾 Lưu");
        btnCancel = new JButton("❌ Hủy");

        btnSave.setBackground(new Color(76, 175, 80));
        btnSave.setForeground(Color.WHITE);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Academic Year
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Năm học:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(txtAcademicYear, gbc);
        row++;

        // Semester
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Học kỳ:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(cmbSemester, gbc);
        row++;

        // Start Date
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Ngày bắt đầu (dd/MM/yyyy):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(txtStartDate, gbc);
        row++;

        // Start Time
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Giờ bắt đầu:"), gbc);

        gbc.gridx = 1;
        JPanel startTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startTimePanel.add(spinnerStartHour);
        startTimePanel.add(new JLabel(":"));
        startTimePanel.add(spinnerStartMinute);
        formPanel.add(startTimePanel, gbc);
        row++;

        // End Date
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Ngày kết thúc (dd/MM/yyyy):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(txtEndDate, gbc);
        row++;

        // End Time
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Giờ kết thúc:"), gbc);

        gbc.gridx = 1;
        JPanel endTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        endTimePanel.add(spinnerEndHour);
        endTimePanel.add(new JLabel(":"));
        endTimePanel.add(spinnerEndMinute);
        formPanel.add(endTimePanel, gbc);
        row++;

        // Description
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Mô tả:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane(txtDescription);
        formPanel.add(scrollPane, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        // Add to dialog
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        btnSave.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());
    }

    private void setDefaultValues() {
        // Set current academic year
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        String academicYear;
        if (month >= Calendar.AUGUST) {
            academicYear = year + "-" + (year + 1);
        } else {
            academicYear = (year - 1) + "-" + year;
        }

        txtAcademicYear.setText(academicYear);

        // Set default dates
        txtStartDate.setText(dateFormat.format(new Date()));
        cal.add(Calendar.DAY_OF_MONTH, 14);
        txtEndDate.setText(dateFormat.format(cal.getTime()));
    }

    private void loadPeriodData() {
        txtAcademicYear.setText(period.getAcademicYear());
        cmbSemester.setSelectedIndex(period.getSemester() - 1);

        if (period.getStartDate() != null) {
            txtStartDate.setText(dateFormat.format(new Date(period.getStartDate().getTime())));
            Calendar cal = Calendar.getInstance();
            cal.setTime(period.getStartDate());
            spinnerStartHour.setValue(cal.get(Calendar.HOUR_OF_DAY));
            spinnerStartMinute.setValue(cal.get(Calendar.MINUTE));
        }

        if (period.getEndDate() != null) {
            txtEndDate.setText(dateFormat.format(new Date(period.getEndDate().getTime())));
            Calendar cal = Calendar.getInstance();
            cal.setTime(period.getEndDate());
            spinnerEndHour.setValue(cal.get(Calendar.HOUR_OF_DAY));
            spinnerEndMinute.setValue(cal.get(Calendar.MINUTE));
        }

        if (period.getDescription() != null) {
            txtDescription.setText(period.getDescription());
        }
    }

    private void save() {
        // Validate
        if (txtAcademicYear.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập năm học!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (txtStartDate.getText().trim().isEmpty() || txtEndDate.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập ngày bắt đầu và kết thúc!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Parse dates
        Date startDate;
        Date endDate;
        try {
            startDate = dateFormat.parse(txtStartDate.getText().trim());
            endDate = dateFormat.parse(txtEndDate.getText().trim());
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this,
                "Định dạng ngày không hợp lệ! Vui lòng nhập theo định dạng dd/MM/yyyy",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Build timestamps
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(startDate);
        calStart.set(Calendar.HOUR_OF_DAY, (Integer) spinnerStartHour.getValue());
        calStart.set(Calendar.MINUTE, (Integer) spinnerStartMinute.getValue());
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(endDate);
        calEnd.set(Calendar.HOUR_OF_DAY, (Integer) spinnerEndHour.getValue());
        calEnd.set(Calendar.MINUTE, (Integer) spinnerEndMinute.getValue());
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);

        Timestamp startTimestamp = new Timestamp(calStart.getTimeInMillis());
        Timestamp endTimestamp = new Timestamp(calEnd.getTimeInMillis());

        // Validate dates
        if (endTimestamp.before(startTimestamp)) {
            JOptionPane.showMessageDialog(this,
                "Ngày kết thúc phải sau ngày bắt đầu!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create or update period
        if (period == null) {
            period = new RegistrationPeriod();
        }

        period.setAcademicYear(txtAcademicYear.getText().trim());
        period.setSemester(cmbSemester.getSelectedIndex() + 1);
        period.setStartDate(startTimestamp);
        period.setEndDate(endTimestamp);
        period.setDescription(txtDescription.getText().trim());

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public RegistrationPeriod getRegistrationPeriod() {
        return period;
    }
}

