package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.CourseRegistration.RegistrationStatus;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for students to view and manage their course registrations
 */
public class MyRegistrationsPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MyRegistrationsPanel.class.getName());
    private IServerConnection serverConnection;
    private User currentUser;
    private int studentId;

    private JTable registrationTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton browseCoursesBtn;
    private JButton cancelRegistrationBtn;
    private JButton refreshBtn;

    private JComboBox<String> statusFilter;
    private JLabel statsLabel;
    private JLabel creditsLabel;

    public MyRegistrationsPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title and stats
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Đăng Ký Tín Chỉ Của Tôi");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        statsLabel = new JLabel("");
        creditsLabel = new JLabel("");
        statsPanel.add(statsLabel);
        statsPanel.add(creditsLabel);
        topPanel.add(statsPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Trạng thái:"));

        String[] statuses = { "Tất cả", "Chờ duyệt", "Đã duyệt", "Đã hủy" };
        statusFilter = new JComboBox<>(statuses);
        statusFilter.addActionListener(e -> applyFilter());
        filterPanel.add(statusFilter);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        browseCoursesBtn = new JButton("Đăng Ký Tín Chỉ");
        browseCoursesBtn.setToolTipText("Xem và đăng ký các môn học có sẵn");
        browseCoursesBtn.addActionListener(e -> onBrowseCourses());
        buttonPanel.add(browseCoursesBtn);

        cancelRegistrationBtn = new JButton("Hủy Đăng ký");
        cancelRegistrationBtn.setEnabled(false);
        cancelRegistrationBtn.addActionListener(e -> onCancelRegistration());
        buttonPanel.add(cancelRegistrationBtn);

        refreshBtn = new JButton("Làm Mới");
        refreshBtn.addActionListener(e -> refreshData());
        buttonPanel.add(refreshBtn);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(filterPanel, BorderLayout.WEST);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        // Table
        String[] columns = { "ID", "Môn học", "Mã môn", "TC", "Giảng viên",
                "Lịch học", "Phòng", "Ngày ĐK", "Trạng thái", "Ghi chú" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        registrationTable = new JTable(tableModel);
        registrationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        registrationTable.setAutoCreateRowSorter(true);
        registrationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Column widths
        registrationTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        registrationTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Môn học
        registrationTable.getColumnModel().getColumn(2).setPreferredWidth(80); // Mã môn
        registrationTable.getColumnModel().getColumn(3).setPreferredWidth(40); // TC
        registrationTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Giảng viên
        registrationTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Lịch học
        registrationTable.getColumnModel().getColumn(6).setPreferredWidth(70); // Phòng
        registrationTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Ngày ĐK
        registrationTable.getColumnModel().getColumn(8).setPreferredWidth(100); // Trạng thái
        registrationTable.getColumnModel().getColumn(9).setPreferredWidth(200); // Ghi chú

        sorter = new TableRowSorter<>(tableModel);
        registrationTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(registrationTable);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(controlPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void setServerConnection(IServerConnection connection) {
        this.serverConnection = connection;
    }

    public void setCurrentUser(User user, int studentId) {
        this.currentUser = user;
        this.studentId = studentId;
        if (serverConnection != null) {
            refreshData();
        }
    }

    private void refreshData() {
        if (serverConnection == null || currentUser == null) {
            return;
        }

        try {
            // Get student's registrations
            Message request = Message.createRequest(Constants.ACTION_GET_MY_REGISTRATIONS);
            request.addData(Constants.KEY_STUDENT_ID, studentId);

            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<CourseRegistration> registrations = (List<CourseRegistration>) response
                        .getData(Constants.KEY_REGISTRATIONS);

                updateTable(registrations);
                updateStats(registrations);
                updateCredits();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi từ server";
                JOptionPane.showMessageDialog(this,
                        "Lỗi tải dữ liệu: " + errorMsg,
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách đăng ký", e);
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable(List<CourseRegistration> registrations) {
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (CourseRegistration reg : registrations) {
            Object[] row = {
                    reg.getRegistrationId(),
                    reg.getSubjectName(),
                    reg.getCourseCode(),
                    reg.getCredits(),
                    reg.getTeacherName(),
                    reg.getScheduleDay() + " - " + reg.getScheduleTime(),
                    reg.getRoom(),
                    reg.getRegistrationDate() != null ? sdf.format(reg.getRegistrationDate()) : "",
                    getStatusText(reg.getRegistrationStatus()),
                    reg.getNotes() != null ? reg.getNotes() : ""
            };
            tableModel.addRow(row);
        }

        applyFilter();
    }

    private void updateStats(List<CourseRegistration> registrations) {
        int pending = 0, approved = 0, cancelled = 0;

        for (CourseRegistration reg : registrations) {
            switch (reg.getRegistrationStatus()) {
                case PENDING:
                    pending++;
                    break;
                case APPROVED:
                    approved++;
                    break;
                case CANCELLED:
                    cancelled++;
                    break;
            }
        }

        statsLabel.setText(String.format(
                "Tổng: %d | Chờ: %d | Duyệt: %d | Hủy: %d",
                registrations.size(), pending, approved, cancelled));
    }

    private void updateCredits() {
        // Get current semester from the first registration or use default
        String academicYear = "2024-2025"; // Default
        int semester = 1; // Default

        // Try to get from first approved registration
        if (tableModel.getRowCount() > 0) {
            // In real app, should get from course data
        }

        try {
            Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_CREDITS);
            request.addData(Constants.KEY_STUDENT_ID, studentId);
            request.addData(Constants.KEY_ACADEMIC_YEAR, academicYear);
            request.addData(Constants.KEY_SEMESTER, semester);

            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                Integer credits = (Integer) response.getData("credits");
                if (credits != null) {
                    creditsLabel.setText("Số tín chỉ đã đăng ký: " + credits + "/24");
                    if (credits >= 24) {
                        creditsLabel.setForeground(Color.RED);
                    } else if (credits >= 20) {
                        creditsLabel.setForeground(Color.ORANGE);
                    } else {
                        creditsLabel.setForeground(Color.BLACK);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi khi tính số tín chỉ đã đăng ký", e);
        }
    }

    private void applyFilter() {
        String selectedStatus = (String) statusFilter.getSelectedItem();

        if ("Tất cả".equals(selectedStatus)) {
            sorter.setRowFilter(null);
        } else {
            String filterStatus = selectedStatus;
            sorter.setRowFilter(RowFilter.regexFilter(filterStatus, 8)); // Column 8 is status
        }
    }

    private void onBrowseCourses() {
        try {
            // Get available courses
            Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);

                // Show course selection dialog
                showCourseSelectionDialog(courses);
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                JOptionPane.showMessageDialog(this,
                        "Lỗi tải danh sách khóa học: " + errorMsg,
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách khóa học", e);
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCourseSelectionDialog(List<Course> courses) {
        // Simple list dialog for course selection
        String[] courseNames = courses.stream()
                .map(c -> c.getCourseCode() + " - " + c.getSubjectName() +
                        " (" + c.getTeacherName() + ", " + c.getScheduleDay() + ")")
                .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Chọn môn học bạn muốn đăng ký:",
                "Đăng Ký Tín Chỉ",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courseNames,
                courseNames.length > 0 ? courseNames[0] : null);

        if (selected != null) {
            // Find the selected course
            int index = java.util.Arrays.asList(courseNames).indexOf(selected);
            if (index >= 0) {
                Course course = courses.get(index);

                // Show registration dialog
                CourseRegistrationDialog dialog = new CourseRegistrationDialog(
                        (Frame) SwingUtilities.getWindowAncestor(this),
                        serverConnection,
                        studentId,
                        course);
                dialog.setVisible(true);

                if (dialog.isRegistered()) {
                    refreshData();
                }
            }
        }
    }

    private void onCancelRegistration() {
        int selectedRow = registrationTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        int modelRow = registrationTable.convertRowIndexToModel(selectedRow);
        int registrationId = (Integer) tableModel.getValueAt(modelRow, 0);
        String status = (String) tableModel.getValueAt(modelRow, 8);

        if ("Đã hủy".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Đăng ký này đã bị hủy trước đó!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn hủy đăng ký này?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Message msg = Message.createRequest(Constants.ACTION_CANCEL_REGISTRATION);
                msg.addData(Constants.KEY_REGISTRATION_ID, registrationId);
                msg.addData(Constants.KEY_STUDENT_ID, studentId);

                Message response = serverConnection.sendRequest(msg);

                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Hủy đăng ký thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi hủy đăng ký môn học", e);
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateButtonStates() {
        int selectedRow = registrationTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        if (hasSelection) {
            int modelRow = registrationTable.convertRowIndexToModel(selectedRow);
            String status = (String) tableModel.getValueAt(modelRow, 8);
            boolean canCancel = !"Đã hủy".equals(status);

            cancelRegistrationBtn.setEnabled(canCancel);
        } else {
            cancelRegistrationBtn.setEnabled(false);
        }
    }

    private String getStatusText(RegistrationStatus status) {
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case APPROVED:
                return "Đã duyệt";
            case CANCELLED:
                return "Đã hủy";
            default:
                return status.toString();
        }
    }
}
