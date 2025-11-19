package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.ClassOpeningRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.List;

public class AdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;

    private JTable requestTable;
    private DefaultTableModel requestTableModel;
    private JButton approveButton;
    private JButton rejectButton;
    private JButton refreshRequestButton;
    private boolean isInitialized = false;
    private boolean isRefreshing = false;

    public AdminPanel(User currentUser, IServerConnection serverConnection) {
        // currentUser không được sử dụng trong panel này, nhưng giữ lại để nhất quán
        // với các panel khác
        this.serverConnection = serverConnection;

        initializeComponents();
        setupEventListeners();
        isInitialized = true;
        // ComponentListener sẽ tự động gọi refreshData() khi panel được hiển thị
    }

    private void setupEventListeners() {
        // Auto-refresh khi panel được hiển thị (chỉ sau khi đã khởi tạo xong)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                // Chỉ refresh nếu panel đã được khởi tạo hoàn toàn
                if (isInitialized && !isRefreshing) {
                    refreshData();
                }
            }
        });
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = { "ID", "Giảng viên", "Môn học", "Năm học", "HK",
                "Thứ", "Giờ", "Phòng", "SL", "Lý do", "Ngày yêu cầu", "Trạng thái" };
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

        requestTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Giảng viên
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Môn học
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Năm học
        requestTable.getColumnModel().getColumn(4).setPreferredWidth(40); // HK
        requestTable.getColumnModel().getColumn(5).setPreferredWidth(60); // Thứ
        requestTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Giờ
        requestTable.getColumnModel().getColumn(7).setPreferredWidth(70); // Phòng
        requestTable.getColumnModel().getColumn(8).setPreferredWidth(40); // SL
        requestTable.getColumnModel().getColumn(9).setPreferredWidth(200); // Lý do
        requestTable.getColumnModel().getColumn(10).setPreferredWidth(100); // Ngày
        requestTable.getColumnModel().getColumn(11).setPreferredWidth(100); // Trạng thái

        JScrollPane scrollPane = new JScrollPane(requestTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        approveButton = new JButton("Duyệt");
        rejectButton = new JButton("Từ chối");
        refreshRequestButton = new JButton("Làm mới");

        JButton viewDetailsBtn = new JButton("Chi tiết");
        viewDetailsBtn.addActionListener(e -> viewRequestDetails());

        approveButton.addActionListener(e -> approveRequest());
        rejectButton.addActionListener(e -> rejectRequest());
        refreshRequestButton.addActionListener(e -> refreshData());

        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(refreshRequestButton);

        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("Danh sách yêu cầu mở lớp từ giảng viên");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoLabel, BorderLayout.NORTH);

        add(infoPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void refreshData() {
        refreshRequests();
    }

    private void refreshRequests() {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;
        requestTableModel.setRowCount(0);

        SwingWorker<List<ClassOpeningRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ClassOpeningRequest> doInBackground() throws Exception {
                Message msg = Message.createRequest(Constants.ACTION_GET_PENDING_CLASS_REQUESTS);
                Message response = serverConnection.sendRequest(msg);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<ClassOpeningRequest> requests = (List<ClassOpeningRequest>) response
                            .getData(Constants.KEY_CLASS_REQUESTS);
                    return requests != null ? requests : List.of();
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<ClassOpeningRequest> requests = get();
                    updateRequestTable(requests);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminPanel.this,
                            "Lỗi: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
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
        details.append("<tr><td><b>Lịch học:</b></td><td>").append(req.getScheduleDay()).append(" - ")
                .append(req.getScheduleTime()).append("</td></tr>");
        details.append("<tr><td><b>Phòng học:</b></td><td>").append(req.getRoom()).append("</td></tr>");
        details.append("<tr><td><b>Sĩ số:</b></td><td>").append(req.getMaxStudents()).append("</td></tr>");
        details.append("<tr><td><b>Trạng thái:</b></td><td>").append(getStatusText(req.getRequestStatus()))
                .append("</td></tr>");
        details.append("<tr><td><b>Ngày gửi:</b></td><td>")
                .append(req.getRequestDate() != null ? sdf.format(req.getRequestDate()) : "").append("</td></tr>");
        details.append("<tr><td colspan='2'><br/><b>Lý do:</b><br/>").append(req.getReason()).append("</td></tr>");

        if (req.getAdminNote() != null && !req.getAdminNote().isEmpty()) {
            details.append("<tr><td colspan='2'><br/><b>Ghi chú Admin:</b><br/>").append(req.getAdminNote())
                    .append("</td></tr>");
        }

        if (req.getApproverName() != null) {
            details.append("<tr><td><b>Người duyệt:</b></td><td>").append(req.getApproverName()).append("</td></tr>");
        }

        if (req.getDecisionDate() != null) {
            details.append("<tr><td><b>Ngày duyệt:</b></td><td>").append(sdf.format(req.getDecisionDate()))
                    .append("</td></tr>");
        }

        details.append("</table></body></html>");

        JOptionPane.showMessageDialog(this,
                details.toString(),
                "Chi tiết yêu cầu",
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
        String teacherName = (String) requestTableModel.getValueAt(selectedRow, 1);
        String subject = (String) requestTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn duyệt yêu cầu này?\n" +
                        "Giảng viên: " + teacherName + "\n" +
                        "Môn học: " + subject,
                "Xác nhận duyệt",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String note = JOptionPane.showInputDialog(this,
                "Ghi chú (tùy chọn):",
                "Ghi chú duyệt",
                JOptionPane.PLAIN_MESSAGE);

        try {
            Message msg = Message.createRequest(Constants.ACTION_APPROVE_CLASS_REQUEST);
            msg.addData(Constants.KEY_REQUEST_ID, requestId);
            if (note != null && !note.trim().isEmpty()) {
                msg.addData(Constants.KEY_NOTE, note);
            }

            Message response = serverConnection.sendRequest(msg);

            if (response != null && response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        "Đã duyệt yêu cầu thành công",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshData();
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
        String teacherName = (String) requestTableModel.getValueAt(selectedRow, 1);
        String subject = (String) requestTableModel.getValueAt(selectedRow, 2);

        String reason = JOptionPane.showInputDialog(this,
                "Lý do từ chối (bắt buộc):\n" +
                        "Giảng viên: " + teacherName + "\n" +
                        "Môn học: " + subject,
                "Từ chối yêu cầu",
                JOptionPane.PLAIN_MESSAGE);

        if (reason == null || reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập lý do từ chối",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Message msg = Message.createRequest(Constants.ACTION_REJECT_CLASS_REQUEST);
            msg.addData(Constants.KEY_REQUEST_ID, requestId);
            msg.addData(Constants.KEY_REASON, reason);

            Message response = serverConnection.sendRequest(msg);

            if (response != null && response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        "Đã từ chối yêu cầu",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshData();
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

    private String getStatusText(ClassOpeningRequest.RequestStatus status) {
        if (status == null)
            return "N/A";
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case APPROVED:
                return "Đã duyệt";
            case REJECTED:
                return "Từ chối";
            default:
                return status.name();
        }
    }
}
