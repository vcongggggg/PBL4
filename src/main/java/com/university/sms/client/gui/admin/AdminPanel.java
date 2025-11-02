package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.CourseRegistration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel quản trị hệ thống (chỉ dành cho Admin)
 * - Quản lý người dùng (CRUD)
 * - Duyệt yêu cầu mở lớp
 * - Cấu hình hệ thống
 */
public class AdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;

    private JTabbedPane tabbedPane;

    // User Management Tab
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JButton addUserButton;
    private JButton editUserButton;
    private JButton deleteUserButton;
    private JButton refreshUserButton;
    private JTextField userSearchField;

    // Class Request Tab
    private JTable requestTable;
    private DefaultTableModel requestTableModel;
    private JButton approveButton;
    private JButton rejectButton;
    private JButton refreshRequestButton;

    public AdminPanel(User currentUser, IServerConnection serverConnection) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;

        initializeComponents();
        setupLayout();
        loadInitialData();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabbedPane = new JTabbedPane();

        // Tab 1: User Management
        JPanel userManagementPanel = createUserManagementPanel();
        tabbedPane.addTab("Quản lý Người dùng", userManagementPanel);

        // Tab 2: Class Request Approval
        JPanel classRequestPanel = createClassRequestPanel();
        tabbedPane.addTab("Duyệt yêu cầu Mở lớp", classRequestPanel);

        // Tab 3: System Configuration
        JPanel systemConfigPanel = createSystemConfigPanel();
        tabbedPane.addTab("Cấu hình Hệ thống", systemConfigPanel);
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Table
        String[] columnNames = {"ID", "Tên đăng nhập", "Họ tên", "Email", "Vai trò", "Trạng thái"};
        userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(25);

        // Double click to edit
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editUser();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(userTable);

        // Top panel - Search
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Tìm kiếm:"));
        userSearchField = new JTextField(20);
        topPanel.add(userSearchField);
        refreshUserButton = new JButton("Làm mới");
        refreshUserButton.addActionListener(e -> refreshUsers());
        topPanel.add(refreshUserButton);

        // Bottom panel - Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addUserButton = new JButton("Thêm người dùng");
        editUserButton = new JButton("Sửa");
        deleteUserButton = new JButton("Xóa");

        addUserButton.addActionListener(e -> addUser());
        editUserButton.addActionListener(e -> editUser());
        deleteUserButton.addActionListener(e -> deleteUser());
        userSearchField.addActionListener(e -> searchUsers());

        buttonPanel.add(addUserButton);
        buttonPanel.add(editUserButton);
        buttonPanel.add(deleteUserButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createClassRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Table with more details
        String[] columnNames = {"ID", "Giảng viên", "Môn học", "Năm học", "HK", 
                               "Thứ", "Giờ", "Phòng", "SL", "Lý do", "Ngày yêu cầu", "Trạng thái"};
        requestTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestTable = new JTable(requestTableModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setRowHeight(25);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Set column widths
        requestTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Giảng viên
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Môn học
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Năm học
        requestTable.getColumnModel().getColumn(4).setPreferredWidth(40);   // HK
        requestTable.getColumnModel().getColumn(5).setPreferredWidth(60);   // Thứ
        requestTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Giờ
        requestTable.getColumnModel().getColumn(7).setPreferredWidth(70);   // Phòng
        requestTable.getColumnModel().getColumn(8).setPreferredWidth(40);   // SL
        requestTable.getColumnModel().getColumn(9).setPreferredWidth(200);  // Lý do
        requestTable.getColumnModel().getColumn(10).setPreferredWidth(100); // Ngày
        requestTable.getColumnModel().getColumn(11).setPreferredWidth(100); // Trạng thái

        JScrollPane scrollPane = new JScrollPane(requestTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        approveButton = new JButton("Duyệt");
        rejectButton = new JButton("Từ chối");
        refreshRequestButton = new JButton("Làm mới");
        
        JButton viewDetailsBtn = new JButton("Chi tiết");
        viewDetailsBtn.addActionListener(e -> viewRequestDetails());

        approveButton.addActionListener(e -> approveRequest());
        rejectButton.addActionListener(e -> rejectRequest());
        refreshRequestButton.addActionListener(e -> refreshRequests());

        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(refreshRequestButton);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("Danh sách yêu cầu mở lớp từ giảng viên");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoLabel, BorderLayout.NORTH);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSystemConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Config options
        configPanel.add(createConfigSection("Cấu hình Database", 
            new String[]{"Host: localhost", "Port: 3306", "Database: student_management"}));
        
        configPanel.add(Box.createVerticalStrut(20));
        
        configPanel.add(createConfigSection("Cấu hình Server",
            new String[]{"Port: 8888", "Max Clients: 100", "Timeout: 30s"}));
        
        configPanel.add(Box.createVerticalStrut(20));
        
        configPanel.add(createConfigSection("Cấu hình Hệ thống",
            new String[]{"Học kỳ hiện tại: HK1 2024-2025", "Năm học: 2024-2025"}));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editConfigButton = new JButton("Chỉnh sửa");
        JButton backupButton = new JButton("Backup Database");
        JButton restoreButton = new JButton("Restore Database");
        JButton viewLogsButton = new JButton("Xem Logs");

        editConfigButton.addActionListener(e -> editSystemConfig());
        backupButton.addActionListener(e -> backupDatabase());
        restoreButton.addActionListener(e -> restoreDatabase());
        viewLogsButton.addActionListener(e -> viewSystemLogs());

        buttonPanel.add(editConfigButton);
        buttonPanel.add(backupButton);
        buttonPanel.add(restoreButton);
        buttonPanel.add(viewLogsButton);

        panel.add(new JScrollPane(configPanel), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createConfigSection(String title, String[] configs) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (String config : configs) {
            JLabel label = new JLabel(config);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            section.add(label);
        }

        return section;
    }

    private void setupLayout() {
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadInitialData() {
        refreshUsers();
        refreshRequests();
    }

    public void refreshData() {
        refreshUsers();
        refreshRequests();
    }

    private void refreshUsers() {
        userTableModel.setRowCount(0);

        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                // TODO: Implement GET_ALL_USERS API
                // For now, return empty list
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    for (User user : users) {
                        userTableModel.addRow(new Object[]{
                                user.getUserId(),
                                user.getUsername(),
                                user.getFullName(),
                                user.getEmail(),
                                user.getRole(),
                                "Active" // TODO: Add status field
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AdminPanel.this,
                            "Lỗi khi tải danh sách người dùng: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    // Old refreshRequests() removed - using the new implementation below (line ~512)

    private void addUser() {
        UserEditDialog dialog = new UserEditDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                serverConnection,
                null // New user
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            refreshUsers();
        }
    }

    private void editUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một người dùng để sửa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // TODO: Get user data and show edit dialog
        JOptionPane.showMessageDialog(this,
                "Chức năng sửa người dùng đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một người dùng để xóa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa người dùng này?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // TODO: Implement delete
            JOptionPane.showMessageDialog(this,
                    "Chức năng xóa người dùng đang được phát triển",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void searchUsers() {
        String keyword = userSearchField.getText().trim();
        // TODO: Implement search
        JOptionPane.showMessageDialog(this,
                "Tìm kiếm: " + keyword + "\nChức năng đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void approveRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một yêu cầu để duyệt",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int requestId = (Integer) requestTableModel.getValueAt(selectedRow, 0);
        String status = (String) requestTableModel.getValueAt(selectedRow, 11);
        
        if (!"Chờ duyệt".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ có thể duyệt yêu cầu đang chờ duyệt!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String note = JOptionPane.showInputDialog(this,
                "Nhập ghi chú (tùy chọn):",
                "Duyệt yêu cầu",
                JOptionPane.QUESTION_MESSAGE);

        if (note == null) return; // User cancelled

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn duyệt yêu cầu này?\n" +
                "Hệ thống sẽ tự động tạo khóa học mới.",
                "Xác nhận duyệt",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Message msg = Message.createRequest(Constants.ACTION_APPROVE_CLASS_REQUEST);
                msg.addData(Constants.KEY_REQUEST_ID, requestId);
                msg.addData(Constants.KEY_ADMIN_ID, currentUser.getUserId());
                msg.addData(Constants.KEY_NOTE, note != null ? note : "");
                
                Message response = serverConnection.sendRequest(msg);
                
                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Yêu cầu đã được duyệt thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshRequests();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void rejectRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một yêu cầu để từ chối",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int requestId = (Integer) requestTableModel.getValueAt(selectedRow, 0);
        String status = (String) requestTableModel.getValueAt(selectedRow, 11);
        
        if (!"Chờ duyệt".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ có thể từ chối yêu cầu đang chờ duyệt!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this,
                "Nhập lý do từ chối:",
                "Từ chối yêu cầu",
                JOptionPane.QUESTION_MESSAGE);

        if (reason != null && !reason.trim().isEmpty()) {
            try {
                Message msg = Message.createRequest(Constants.ACTION_REJECT_CLASS_REQUEST);
                msg.addData(Constants.KEY_REQUEST_ID, requestId);
                msg.addData(Constants.KEY_ADMIN_ID, currentUser.getUserId());
                msg.addData(Constants.KEY_REASON, reason);
                
                Message response = serverConnection.sendRequest(msg);
                
                if (response != null && response.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "Yêu cầu đã bị từ chối!",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshRequests();
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + errorMsg,
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void refreshRequests() {
        try {
            Message msg = Message.createRequest(Constants.ACTION_GET_PENDING_CLASS_REQUESTS);
            Message response = serverConnection.sendRequest(msg);
            
            if (response != null && response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<ClassOpeningRequest> requests = (List<ClassOpeningRequest>) 
                    response.getData(Constants.KEY_CLASS_REQUESTS);
                
                updateRequestTable(requests);
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                JOptionPane.showMessageDialog(this,
                        "Lỗi tải dữ liệu: " + errorMsg,
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRequestTable(List<ClassOpeningRequest> requests) {
        requestTableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        for (ClassOpeningRequest req : requests) {
            Object[] row = {
                req.getRequestId(),
                req.getTeacherName(),
                req.getSubjectCode() + " - " + req.getSubjectName(),
                req.getAcademicYear(),
                req.getSemester(),
                req.getScheduleDay(),
                req.getScheduleTime(),
                req.getRoom(),
                req.getMaxStudents(),
                req.getReason(),
                req.getRequestDate() != null ? sdf.format(req.getRequestDate()) : "",
                getStatusText(req.getRequestStatus())
            };
            requestTableModel.addRow(row);
        }
    }
    
    private void viewRequestDetails() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một yêu cầu để xem chi tiết",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int requestId = (Integer) requestTableModel.getValueAt(selectedRow, 0);
        
        try {
            Message msg = Message.createRequest(Constants.ACTION_GET_CLASS_REQUEST_BY_ID);
            msg.addData(Constants.KEY_REQUEST_ID, requestId);
            
            Message response = serverConnection.sendRequest(msg);
            
            if (response != null && response.isSuccess()) {
                ClassOpeningRequest req = (ClassOpeningRequest) response.getData(Constants.KEY_CLASS_REQUEST);
                showRequestDetails(req);
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + errorMsg,
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showRequestDetails(ClassOpeningRequest req) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        StringBuilder details = new StringBuilder();
        details.append("<html><body style='width: 400px; font-family: Arial;'>");
        details.append("<h3>Chi tiết Yêu cầu Mở lớp</h3>");
        details.append("<table>");
        details.append("<tr><td><b>Mã yêu cầu:</b></td><td>").append(req.getRequestId()).append("</td></tr>");
        details.append("<tr><td><b>Giảng viên:</b></td><td>").append(req.getTeacherName()).append("</td></tr>");
        details.append("<tr><td><b>Môn học:</b></td><td>").append(req.getSubjectName()).append("</td></tr>");
        details.append("<tr><td><b>Mã môn:</b></td><td>").append(req.getSubjectCode()).append("</td></tr>");
        details.append("<tr><td><b>Số tín chỉ:</b></td><td>").append(req.getCredits()).append("</td></tr>");
        details.append("<tr><td><b>Năm học:</b></td><td>").append(req.getAcademicYear()).append("</td></tr>");
        details.append("<tr><td><b>Học kỳ:</b></td><td>").append(req.getSemester()).append("</td></tr>");
        details.append("<tr><td><b>Lịch học:</b></td><td>").append(req.getScheduleDay()).append(" - ").append(req.getScheduleTime()).append("</td></tr>");
        details.append("<tr><td><b>Phòng học:</b></td><td>").append(req.getRoom()).append("</td></tr>");
        details.append("<tr><td><b>Sĩ số:</b></td><td>").append(req.getMaxStudents()).append("</td></tr>");
        details.append("<tr><td><b>Trạng thái:</b></td><td>").append(getStatusText(req.getRequestStatus())).append("</td></tr>");
        details.append("<tr><td><b>Ngày gửi:</b></td><td>").append(req.getRequestDate() != null ? sdf.format(req.getRequestDate()) : "").append("</td></tr>");
        details.append("<tr><td colspan='2'><br/><b>Lý do:</b><br/>").append(req.getReason()).append("</td></tr>");
        
        if (req.getAdminNote() != null && !req.getAdminNote().isEmpty()) {
            details.append("<tr><td colspan='2'><br/><b>Ghi chú Admin:</b><br/>").append(req.getAdminNote()).append("</td></tr>");
        }
        
        if (req.getApproverName() != null) {
            details.append("<tr><td><b>Người duyệt:</b></td><td>").append(req.getApproverName()).append("</td></tr>");
        }
        
        if (req.getDecisionDate() != null) {
            details.append("<tr><td><b>Ngày quyết định:</b></td><td>").append(sdf.format(req.getDecisionDate())).append("</td></tr>");
        }
        
        details.append("</table></body></html>");
        
        JOptionPane.showMessageDialog(this,
                details.toString(),
                "Chi tiết Yêu cầu",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String getStatusText(ClassOpeningRequest.RequestStatus status) {
        switch (status) {
            case PENDING: return "Chờ duyệt";
            case APPROVED: return "Đã duyệt";
            case REJECTED: return "Từ chối";
            default: return status.toString();
        }
    }

    private void editSystemConfig() {
        JOptionPane.showMessageDialog(this,
                "Chức năng chỉnh sửa cấu hình đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void backupDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn backup database?",
                "Xác nhận backup",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // TODO: Implement backup
            JOptionPane.showMessageDialog(this,
                    "Database đã được backup thành công!\nVị trí: backup_" + 
                    System.currentTimeMillis() + ".sql",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void restoreDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file backup");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            // TODO: Implement restore
            JOptionPane.showMessageDialog(this,
                    "Chức năng restore đang được phát triển",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void viewSystemLogs() {
        JOptionPane.showMessageDialog(this,
                "Chức năng xem logs đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Inner class: Dialog thêm/sửa người dùng
     */
    private static class UserEditDialog extends JDialog {
        private boolean confirmed = false;

        public UserEditDialog(Frame parent, IServerConnection serverConnection, User user) {
            super(parent, user == null ? "Thêm người dùng mới" : "Sửa thông tin người dùng", true);
            
            setLayout(new BorderLayout(10, 10));
            setSize(450, 400);
            setLocationRelativeTo(parent);

            JLabel label = new JLabel("Chức năng thêm/sửa người dùng đang được phát triển", JLabel.CENTER);
            add(label, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton okButton = new JButton("Lưu");
            JButton cancelButton = new JButton("Hủy");

            okButton.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}
