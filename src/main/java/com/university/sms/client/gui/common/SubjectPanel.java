package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Subject;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel quản lý môn học (Khung chương trình đào tạo)
 */
public class SubjectPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private User currentUser;
  private IServerConnection serverConnection;

  private JTable subjectTable;
  private DefaultTableModel tableModel;
  private JTextField searchField;
  private JButton searchButton;
  private JButton refreshButton;
  private JButton addButton;
  private JButton editButton;
  private JButton deleteButton;

  private List<Subject> currentSubjects;

  // Log area components
  private JTextArea logArea;
  private JScrollPane logScrollPane;

  public SubjectPanel(User currentUser, IServerConnection serverConnection) {
    this.currentUser = currentUser;
    this.serverConnection = serverConnection;

    initializeComponents();
    setupLayout();
    setupEventListeners();
    loadInitialData();
  }

  private void initializeComponents() {
    // Create table
    String[] columnNames = { "Mã môn", "Tên môn học", "Số tín chỉ", "Khoa", "Bắt buộc", "Môn tiên quyết" };
    tableModel = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    subjectTable = new JTable(tableModel);
    subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    subjectTable.setRowHeight(30);

    // Create search components
    searchField = new JTextField(20);
    searchButton = new JButton("Tìm kiếm");
    refreshButton = new JButton("Làm mới");

    // Create action buttons
    addButton = new JButton("Thêm môn học");
    editButton = new JButton("Chỉnh sửa");
    deleteButton = new JButton("Xóa");
    editButton.setEnabled(false);
    deleteButton.setEnabled(false);

    // Create log area
    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    logScrollPane = new JScrollPane(logArea);
    logScrollPane.setBorder(BorderFactory.createTitledBorder("Log hoạt động"));
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    // Top panel with search and buttons
    JPanel topPanel = new JPanel(new BorderLayout());

    // Search panel
    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    searchPanel.add(new JLabel("Tìm kiếm:"));
    searchPanel.add(searchField);
    searchPanel.add(searchButton);
    searchPanel.add(refreshButton);
    topPanel.add(searchPanel, BorderLayout.WEST);

    // Action panel
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actionPanel.add(addButton);
    actionPanel.add(editButton);
    actionPanel.add(deleteButton);
    topPanel.add(actionPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);

    // Center panel with table
    JScrollPane tableScrollPane = new JScrollPane(subjectTable);
    tableScrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách môn học"));

    // Split pane for table and log
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, logScrollPane);
    splitPane.setDividerLocation(400);
    splitPane.setResizeWeight(0.7);

    add(splitPane, BorderLayout.CENTER);
  }

  private void setupEventListeners() {
    // Search button
    searchButton.addActionListener(e -> performSearch());

    // Search field enter key
    searchField.addActionListener(e -> performSearch());

    // Refresh button
    refreshButton.addActionListener(e -> refreshData());

    // Add button
    addButton.addActionListener(e -> showAddSubjectDialog());

    // Edit button
    editButton.addActionListener(e -> showEditSubjectDialog());

    // Delete button
    deleteButton.addActionListener(e -> deleteSubject());

    // Table selection listener
    subjectTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        updateButtonStates();
      }
    });
  }

  private void loadInitialData() {
    refreshData();
  }

  public void refreshData() {
    addLog("Đang tải danh sách môn học...");
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Subject> subjects = (List<Subject>) response.getData("subjects");
            if (subjects != null) {
              updateSubjectTable(subjects);
              addLog("Đã tải " + subjects.size() + " môn học");
            } else {
              addLog("Không có dữ liệu môn học");
            }
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể tải danh sách môn học: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tải danh sách môn học: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void performSearch() {
    String keyword = searchField.getText().trim();
    if (keyword.isEmpty()) {
      refreshData();
      return;
    }

    addLog("Đang tìm kiếm: " + keyword);
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_SEARCH_SUBJECTS);
        request.addData("keyword", keyword);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Subject> subjects = (List<Subject>) response.getData("subjects");
            updateSubjectTable(subjects);
            addLog("Tìm thấy " + subjects.size() + " môn học");
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Tìm kiếm thất bại: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tìm kiếm: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void showAddSubjectDialog() {
    Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
    SubjectEditDialog dialog = new SubjectEditDialog(parentFrame, serverConnection, null);
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã thêm môn học mới");
      refreshData();
    }
  }

  private void showEditSubjectDialog() {
    int selectedRow = subjectTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một môn học để chỉnh sửa");
      return;
    }

    Subject selectedSubject = currentSubjects.get(selectedRow);
    Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
    SubjectEditDialog dialog = new SubjectEditDialog(parentFrame, serverConnection, selectedSubject);
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã cập nhật môn học: " + selectedSubject.getSubjectCode());
      refreshData();
    }
  }

  private void deleteSubject() {
    int selectedRow = subjectTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một môn học để xóa");
      return;
    }

    Subject selectedSubject = currentSubjects.get(selectedRow);
    int confirm = JOptionPane.showConfirmDialog(this,
        "Bạn có chắc chắn muốn xóa môn học: " + selectedSubject.getSubjectName() + "?\n" +
            "Lưu ý: Không thể xóa nếu môn học đang được sử dụng trong chương trình đào tạo.",
        "Xác nhận xóa",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (confirm == JOptionPane.YES_OPTION) {
      addLog("Đang xóa môn học: " + selectedSubject.getSubjectCode());

      SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
        @Override
        protected Message doInBackground() throws Exception {
          Message request = Message.createRequest(Constants.ACTION_DELETE_SUBJECT);
          request.addData("subjectId", selectedSubject.getSubjectId());
          return serverConnection.sendRequest(request);
        }

        @Override
        protected void done() {
          try {
            Message response = get();
            if (response != null && response.isSuccess()) {
              JOptionPane.showMessageDialog(SubjectPanel.this,
                  response.getMessage(),
                  "Thành công",
                  JOptionPane.INFORMATION_MESSAGE);
              addLog("Đã xóa môn học: " + selectedSubject.getSubjectCode());
              refreshData();
            } else {
              String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
              showErrorMessage(errorMsg);
              addLog("Lỗi xóa: " + errorMsg);
            }
          } catch (Exception e) {
            showErrorMessage("Lỗi khi xóa môn học: " + e.getMessage());
            addLog("Lỗi: " + e.getMessage());
          }
        }
      };

      worker.execute();
    }
  }

  private void updateSubjectTable(List<Subject> subjects) {
    this.currentSubjects = subjects;
    tableModel.setRowCount(0);

    for (Subject subject : subjects) {
      Object[] row = {
          subject.getSubjectCode(),
          subject.getSubjectName(),
          subject.getCredits(),
          subject.getFacultyName(),
          subject.isRequired() ? "Bắt buộc" : "Tự chọn",
          subject.getPrerequisiteSubjectName() != null ? subject.getPrerequisiteSubjectName() : ""
      };
      tableModel.addRow(row);
    }
  }

  private void updateButtonStates() {
    int selectedRow = subjectTable.getSelectedRow();
    editButton.setEnabled(selectedRow >= 0);
    deleteButton.setEnabled(selectedRow >= 0);
  }

  private void addLog(String message) {
    logArea
        .append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] " + message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  private void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
  }
}
