package com.university.sms.client.gui.admin;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.RegistrationPeriod;
import com.university.sms.model.RegistrationPeriod.PeriodStatus;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin panel for managing course registration periods
 */
public class RegistrationPeriodPanel extends JPanel {
    private IServerConnection serverConnection;
    private User currentUser;
    
    private JTable periodTable;
    private DefaultTableModel tableModel;
    
    private JButton btnCreate;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnOpenPeriod;
    private JButton btnClosePeriod;
    private JButton btnViewLog;
    private JButton btnRefresh;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public RegistrationPeriodPanel(IServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshData();
    }

    private void initializeComponents() {
        // Table
        String[] columnNames = {
            "ID", "Năm học", "Học kỳ", "Bắt đầu", "Kết thúc", "Trạng thái", "Mô tả"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        periodTable = new JTable(tableModel);
        periodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        periodTable.setRowHeight(30);
        periodTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        periodTable.getColumnModel().getColumn(6).setPreferredWidth(200);

        // Buttons
        btnCreate = new JButton("➕ Tạo mới");
        btnEdit = new JButton("✏️ Chỉnh sửa");
        btnDelete = new JButton("🗑️ Xóa");
        btnOpenPeriod = new JButton("▶️ Mở đăng ký");
        btnClosePeriod = new JButton("⏹️ Đóng đăng ký");
        btnViewLog = new JButton("📋 Xem log");
        btnRefresh = new JButton("🔄 Làm mới");

        // Style buttons
        btnCreate.setBackground(new Color(76, 175, 80));
        btnCreate.setForeground(Color.WHITE);
        btnOpenPeriod.setBackground(new Color(33, 150, 243));
        btnOpenPeriod.setForeground(Color.WHITE);
        btnClosePeriod.setBackground(new Color(255, 152, 0));
        btnClosePeriod.setForeground(Color.WHITE);
        btnDelete.setBackground(new Color(244, 67, 54));
        btnDelete.setForeground(Color.WHITE);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("🗓️ Quản lý thời gian đăng ký tín chỉ");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.add(btnCreate);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(btnOpenPeriod);
        buttonPanel.add(btnClosePeriod);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(btnViewLog);
        buttonPanel.add(btnRefresh);

        // Table panel
        JScrollPane scrollPane = new JScrollPane(periodTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách kỳ đăng ký"));

        // Add to main panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titlePanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        btnCreate.addActionListener(e -> createPeriod());
        btnEdit.addActionListener(e -> editPeriod());
        btnDelete.addActionListener(e -> deletePeriod());
        btnOpenPeriod.addActionListener(e -> openPeriod());
        btnClosePeriod.addActionListener(e -> closePeriod());
        btnViewLog.addActionListener(e -> viewLog());
        btnRefresh.addActionListener(e -> refreshData());

        periodTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }

    private void updateButtonStates() {
        int selectedRow = periodTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        btnEdit.setEnabled(hasSelection);
        btnDelete.setEnabled(hasSelection);

        if (hasSelection) {
            String statusStr = (String) tableModel.getValueAt(selectedRow, 5);
            PeriodStatus status = PeriodStatus.valueOf(statusStr);

            btnOpenPeriod.setEnabled(status == PeriodStatus.DRAFT);
            btnClosePeriod.setEnabled(status == PeriodStatus.OPEN);
            btnViewLog.setEnabled(status == PeriodStatus.COMPLETED);
        } else {
            btnOpenPeriod.setEnabled(false);
            btnClosePeriod.setEnabled(false);
            btnViewLog.setEnabled(false);
        }
    }

    private void createPeriod() {
        RegistrationPeriodDialog dialog = new RegistrationPeriodDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            RegistrationPeriod period = dialog.getRegistrationPeriod();
            period.setCreatedBy(currentUser.getUserId());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Message request = Message.createRequest(Constants.ACTION_CREATE_REGISTRATION_PERIOD);
                    request.addData(Constants.KEY_PERIOD, period);
                    Message response = serverConnection.sendRequest(request);
                    return response != null && response.isSuccess();
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Tạo kỳ đăng ký thành công!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        } else {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Không thể tạo kỳ đăng ký!",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                            "Lỗi: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void editPeriod() {
        int selectedRow = periodTable.getSelectedRow();
        if (selectedRow < 0) return;

        int periodId = (Integer) tableModel.getValueAt(selectedRow, 0);

        // Fetch full period data
        new SwingWorker<RegistrationPeriod, Void>() {
            @Override
            protected RegistrationPeriod doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_REGISTRATION_PERIOD);
                request.addData(Constants.KEY_PERIOD_ID, periodId);
                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    return (RegistrationPeriod) response.getData(Constants.KEY_PERIOD);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    RegistrationPeriod period = get();
                    if (period != null) {
                        RegistrationPeriodDialog dialog = new RegistrationPeriodDialog(
                            (Frame) SwingUtilities.getWindowAncestor(RegistrationPeriodPanel.this), 
                            period);
                        dialog.setVisible(true);

                        if (dialog.isConfirmed()) {
                            RegistrationPeriod updatedPeriod = dialog.getRegistrationPeriod();
                            updatePeriod(updatedPeriod);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void updatePeriod(RegistrationPeriod period) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_UPDATE_REGISTRATION_PERIOD);
                request.addData(Constants.KEY_PERIOD, period);
                Message response = serverConnection.sendRequest(request);
                return response != null && response.isSuccess();
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                            "Cập nhật kỳ đăng ký thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                            "Không thể cập nhật kỳ đăng ký!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void deletePeriod() {
        int selectedRow = periodTable.getSelectedRow();
        if (selectedRow < 0) return;

        int periodId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String statusStr = (String) tableModel.getValueAt(selectedRow, 5);

        if (!PeriodStatus.DRAFT.name().equals(statusStr)) {
            JOptionPane.showMessageDialog(this,
                "Chỉ có thể xóa kỳ đăng ký ở trạng thái DRAFT!",
                "Cảnh báo",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa kỳ đăng ký này?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Message request = Message.createRequest(Constants.ACTION_DELETE_REGISTRATION_PERIOD);
                    request.addData(Constants.KEY_PERIOD_ID, periodId);
                    Message response = serverConnection.sendRequest(request);
                    return response != null && response.isSuccess();
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Xóa kỳ đăng ký thành công!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        } else {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Không thể xóa kỳ đăng ký!",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    private void openPeriod() {
        int selectedRow = periodTable.getSelectedRow();
        if (selectedRow < 0) return;

        int periodId = (Integer) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn mở kỳ đăng ký này?\n" +
            "Sinh viên sẽ có thể bắt đầu đăng ký môn học.",
            "Xác nhận mở đăng ký",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Message request = Message.createRequest(Constants.ACTION_OPEN_REGISTRATION_PERIOD);
                    request.addData(Constants.KEY_PERIOD_ID, periodId);
                    Message response = serverConnection.sendRequest(request);
                    return response != null && response.isSuccess();
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Mở kỳ đăng ký thành công!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        } else {
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Không thể mở kỳ đăng ký!",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    private void closePeriod() {
        int selectedRow = periodTable.getSelectedRow();
        if (selectedRow < 0) return;

        int periodId = (Integer) tableModel.getValueAt(selectedRow, 0);

        // Show confirmation with warning
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("<html><b>⚠️ LƯU Ý QUAN TRỌNG:</b></html>"));
        panel.add(new JLabel("Khi đóng kỳ đăng ký, hệ thống sẽ:"));
        panel.add(new JLabel("1. Tự động duyệt TẤT CẢ đơn đăng ký PENDING"));
        panel.add(new JLabel("2. Hủy các lớp có < 50% sinh viên đăng ký"));
        panel.add(new JLabel(" "));
        panel.add(new JLabel("<html><b>Bạn có chắc chắn muốn đóng kỳ đăng ký?</b></html>"));

        int confirm = JOptionPane.showConfirmDialog(this,
            panel,
            "Xác nhận đóng đăng ký",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Show progress dialog
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Đang xử lý...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
            progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            progressPanel.add(new JLabel("Đang đóng kỳ đăng ký và xử lý tự động..."), 
                BorderLayout.NORTH);
            progressPanel.add(progressBar, BorderLayout.CENTER);
            progressDialog.add(progressPanel);
            progressDialog.setSize(400, 120);
            progressDialog.setLocationRelativeTo(this);

            new SwingWorker<Message, Void>() {
                @Override
                protected Message doInBackground() throws Exception {
                    Message request = Message.createRequest(Constants.ACTION_CLOSE_REGISTRATION_PERIOD);
                    request.addData(Constants.KEY_PERIOD_ID, periodId);
                    request.addData(Constants.KEY_CLOSED_BY, currentUser.getUserId());
                    return serverConnection.sendRequest(request);
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        Message response = get();
                        if (response != null && response.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = (Map<String, Object>) response.getData(Constants.KEY_RESULT);

                            String message = String.format(
                                "Đóng kỳ đăng ký thành công!\n\n" +
                                "Kết quả xử lý:\n" +
                                "✅ Đăng ký được duyệt: %d\n" +
                                "❌ Đăng ký bị từ chối: %d\n" +
                                "⚠️ Lỗi xử lý: %d\n" +
                                "🗑️ Lớp bị hủy: %d",
                                result.get("approvedCount"),
                                result.get("rejectedCount"),
                                result.get("errorCount"),
                                result.get("cancelledCoursesCount")
                            );

                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                message,
                                "Hoàn tất",
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        } else {
                            String errorMsg = response != null ? response.getMessage() : "Không có phản hồi từ server";
                            JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                                "Lỗi: " + errorMsg,
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                            "Lỗi: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();

            progressDialog.setVisible(true);
        }
    }

    private void viewLog() {
        int selectedRow = periodTable.getSelectedRow();
        if (selectedRow < 0) return;

        int periodId = (Integer) tableModel.getValueAt(selectedRow, 0);

        // TODO: Implement log viewer dialog
        JOptionPane.showMessageDialog(this,
            "Chức năng xem log đang được phát triển...",
            "Thông báo",
            JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshData() {
        new SwingWorker<List<RegistrationPeriod>, Void>() {
            @Override
            protected List<RegistrationPeriod> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ALL_REGISTRATION_PERIODS);
                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<RegistrationPeriod> periods = (List<RegistrationPeriod>) response.getData(Constants.KEY_PERIODS);
                    return periods != null ? periods : new ArrayList<>();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    List<RegistrationPeriod> periods = get();
                    tableModel.setRowCount(0);

                    for (RegistrationPeriod period : periods) {
                        tableModel.addRow(new Object[]{
                            period.getPeriodId(),
                            period.getAcademicYear(),
                            "Học kỳ " + period.getSemester(),
                            dateFormat.format(period.getStartDate()),
                            dateFormat.format(period.getEndDate()),
                            period.getStatus().name(),
                            period.getDescription()
                        });
                    }

                    updateButtonStates();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(RegistrationPeriodPanel.this,
                        "Lỗi tải dữ liệu: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}

