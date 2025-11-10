package com.university.sms.client.gui.teacher;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.ClassOpeningRequest.RequestStatus;
import com.university.sms.model.Subject;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

/**
 * Panel for teachers to manage their class opening requests
 */
public class MyClassRequestsPanel extends JPanel {
    private IServerConnection serverConnection;
    private User currentUser;

    private JTable requestTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton newRequestBtn;
    private JButton editRequestBtn;
    private JButton cancelRequestBtn;
    private JButton refreshBtn;

    private JComboBox<String> statusFilter;
    private JLabel statsLabel;

    private boolean isRefreshing = false;
    private boolean isInitialized = false;

    public MyClassRequestsPanel() {
        initComponents();
        setupEventListeners();
        isInitialized = true;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title and stats
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Yêu Cầu Mở Lớp Của Tôi");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.WEST);

        statsLabel = new JLabel("");
        statsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(statsLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Trạng thái:"));

        String[] statuses = { "Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối" };
        statusFilter = new JComboBox<>(statuses);
        statusFilter.addActionListener(e -> applyFilter());
        filterPanel.add(statusFilter);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        newRequestBtn = new JButton("Gửi Yêu Cầu Mới");
        newRequestBtn.addActionListener(e -> onNewRequest());
        buttonPanel.add(newRequestBtn);

        editRequestBtn = new JButton("Chỉnh Sửa");
        editRequestBtn.setEnabled(false);
        editRequestBtn.addActionListener(e -> onEditRequest());
        buttonPanel.add(editRequestBtn);

        cancelRequestBtn = new JButton("Hủy Yêu Cầu");
        cancelRequestBtn.setEnabled(false);
        cancelRequestBtn.addActionListener(e -> onCancelRequest());
        buttonPanel.add(cancelRequestBtn);

        refreshBtn = new JButton("Làm Mới");
        refreshBtn.addActionListener(e -> refreshData());
        buttonPanel.add(refreshBtn);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(filterPanel, BorderLayout.WEST);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        // Table
        String[] columns = { "ID", "Môn học", "Năm học", "HK", "Thứ", "Giờ",
                "Phòng", "SL", "Trạng thái", "Ngày gửi", "Ghi chú Admin" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestTable = new JTable(tableModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoCreateRowSorter(true);
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Column widths
        requestTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Môn học
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Năm học
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(40); // HK
        requestTable.getColumnModel().getColumn(4).setPreferredWidth(60); // Thứ
        requestTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Giờ
        requestTable.getColumnModel().getColumn(6).setPreferredWidth(70); // Phòng
        requestTable.getColumnModel().getColumn(7).setPreferredWidth(40); // SL
        requestTable.getColumnModel().getColumn(8).setPreferredWidth(100); // Trạng thái
        requestTable.getColumnModel().getColumn(9).setPreferredWidth(100); // Ngày gửi
        requestTable.getColumnModel().getColumn(10).setPreferredWidth(200); // Ghi chú

        sorter = new TableRowSorter<>(tableModel);
        requestTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(requestTable);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(controlPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        // Auto-refresh when panel is shown
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (isInitialized && !isRefreshing && serverConnection != null && currentUser != null) {
                    refreshData();
                }
            }
        });
    }

    public void setServerConnection(IServerConnection connection) {
        this.serverConnection = connection;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Don't call refreshData() here to avoid blocking UI thread
        // ComponentListener will handle auto-refresh when panel is shown
    }

    public void refreshData() {
        if (serverConnection == null || currentUser == null) {
            return;
        }

        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;

        // Use SwingWorker to avoid blocking UI thread
        SwingWorker<List<ClassOpeningRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ClassOpeningRequest> doInBackground() throws Exception {
                // Get teacher's requests in background thread
                Message request = Message.createRequest(Constants.ACTION_GET_MY_CLASS_REQUESTS);
                request.addData("teacherUsername", currentUser.getUsername());

                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<ClassOpeningRequest> requests = (List<ClassOpeningRequest>) response
                            .getData(Constants.KEY_CLASS_REQUESTS);
                    return requests;
                } else {
                    throw new Exception(response != null ? response.getMessage() : "Không có phản hồi từ server");
                }
            }

            @Override
            protected void done() {
                try {
                    List<ClassOpeningRequest> requests = get();
                    updateTable(requests);
                    updateStats(requests);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Don't show error dialog during initial load
                    // Just log the error
                    System.err.println("Error loading class requests: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    private void updateTable(List<ClassOpeningRequest> requests) {
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (ClassOpeningRequest req : requests) {
            Object[] row = {
                    req.getRequestId(),
                    req.getSubjectCode() + " - " + req.getSubjectName(),
                    req.getAcademicYear(),
                    req.getSemester(),
                    req.getScheduleDay(),
                    req.getScheduleTime(),
                    req.getRoom(),
                    req.getMaxStudents(),
                    getStatusText(req.getRequestStatus()),
                    req.getRequestDate() != null ? sdf.format(req.getRequestDate()) : "",
                    req.getAdminNote() != null ? req.getAdminNote() : ""
            };
            tableModel.addRow(row);
        }

        applyFilter();
    }

    private void updateStats(List<ClassOpeningRequest> requests) {
        int pending = 0, approved = 0, rejected = 0;

        for (ClassOpeningRequest req : requests) {
            switch (req.getRequestStatus()) {
                case PENDING:
                    pending++;
                    break;
                case APPROVED:
                    approved++;
                    break;
                case REJECTED:
                    rejected++;
                    break;
            }
        }

        statsLabel.setText(String.format(
                "Tổng: %d | Chờ duyệt: %d | Đã duyệt: %d | Từ chối: %d",
                requests.size(), pending, approved, rejected));
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

    private void onNewRequest() {
        try {
            // Get subjects list
            List<Subject> subjects = getSubjectsList();
            if (subjects.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không có môn học nào trong hệ thống!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ClassOpeningRequestDialog dialog = new ClassOpeningRequestDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    currentUser.getUsername(),
                    subjects);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                ClassOpeningRequest request = dialog.getRequest();

                // Send to server
                Message msg = Message.createRequest(Constants.ACTION_SUBMIT_CLASS_REQUEST);
                msg.addData(Constants.KEY_CLASS_REQUEST, request);

                Message response = serverConnection.sendRequest(msg);

                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Gửi yêu cầu thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEditRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        int modelRow = requestTable.convertRowIndexToModel(selectedRow);
        int requestId = (Integer) tableModel.getValueAt(modelRow, 0);
        String status = (String) tableModel.getValueAt(modelRow, 8);

        if (!"Chờ duyệt".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ có thể chỉnh sửa yêu cầu đang chờ duyệt!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Get full request details
            ClassOpeningRequest request = getRequestById(requestId);
            if (request == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy yêu cầu!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Subject> subjects = getSubjectsList();
            ClassOpeningRequestDialog dialog = new ClassOpeningRequestDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    request,
                    subjects);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                ClassOpeningRequest updatedRequest = dialog.getRequest();

                // Send update to server
                Message msg = Message.createRequest(Constants.ACTION_UPDATE_CLASS_REQUEST);
                msg.addData(Constants.KEY_CLASS_REQUEST, updatedRequest);

                Message response = serverConnection.sendRequest(msg);

                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Cập nhật yêu cầu thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        int modelRow = requestTable.convertRowIndexToModel(selectedRow);
        int requestId = (Integer) tableModel.getValueAt(modelRow, 0);
        String status = (String) tableModel.getValueAt(modelRow, 8);

        if (!"Chờ duyệt".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ có thể hủy yêu cầu đang chờ duyệt!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn hủy yêu cầu này?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Message msg = Message.createRequest(Constants.ACTION_CANCEL_CLASS_REQUEST);
                msg.addData(Constants.KEY_REQUEST_ID, requestId);
                msg.addData("teacherUsername", currentUser.getUsername());

                Message response = serverConnection.sendRequest(msg);

                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Hủy yêu cầu thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateButtonStates() {
        int selectedRow = requestTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        if (hasSelection) {
            int modelRow = requestTable.convertRowIndexToModel(selectedRow);
            String status = (String) tableModel.getValueAt(modelRow, 8);
            boolean isPending = "Chờ duyệt".equals(status);

            editRequestBtn.setEnabled(isPending);
            cancelRequestBtn.setEnabled(isPending);
        } else {
            editRequestBtn.setEnabled(false);
            cancelRequestBtn.setEnabled(false);
        }
    }

    private String getStatusText(RequestStatus status) {
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case APPROVED:
                return "Đã duyệt";
            case REJECTED:
                return "Từ chối";
            default:
                return status.toString();
        }
    }

    private List<Subject> getSubjectsList() {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_SUBJECTS);
            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Subject> subjects = (List<Subject>) response.getData(Constants.KEY_SUBJECTS);
                return subjects != null ? subjects : new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private ClassOpeningRequest getRequestById(int requestId) {
        try {
            Message request = Message.createRequest(Constants.ACTION_GET_CLASS_REQUEST_BY_ID);
            request.addData(Constants.KEY_REQUEST_ID, requestId);

            Message response = serverConnection.sendRequest(request);

            if (response != null && response.isSuccess()) {
                return (ClassOpeningRequest) response.getData(Constants.KEY_CLASS_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Public method to show submit dialog (called from TeacherMainFrame toolbar)
     */
    public void showSubmitDialog() {
        onNewRequest();
    }
}
