package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.Course;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Panel quản lý giảng viên (dành cho Admin)
 */
public class TeacherPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private User currentUser;
  private IServerConnection serverConnection;

  private JTable teacherTable;
  private DefaultTableModel tableModel;
  private JTextField searchField;
  private JButton searchButton;
  private JButton refreshButton;
  private JCheckBox showInactiveCheckbox;
  private JButton viewCoursesButton;
  private JButton addButton;
  private JButton editButton;
  private JButton deleteButton;
  private JButton activateButton;

  private List<User> currentTeachers;

  // Flag to prevent multiple simultaneous refresh
  private boolean isRefreshing = false;
  private boolean isInitialized = false;

  // Log area components
  private JTextArea logArea;
  private JScrollPane logScrollPane;

  public TeacherPanel(User currentUser, IServerConnection serverConnection) {
    this.currentUser = currentUser;
    this.serverConnection = serverConnection;

    initializeComponents();
    setupLayout();
    setupEventListeners();
    isInitialized = true; // Mark as initialized after setup
    // loadInitialData(); // Bỏ - để ComponentListener handle auto-refresh
  }

  private void initializeComponents() {
    // Create table
    String[] columnNames = { "Mã GV", "Họ tên", "Email", "Số điện thoại", "Địa chỉ", "Khoa", "Trạng thái" };
    tableModel = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    teacherTable = new JTable(tableModel);
    teacherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    teacherTable.setRowHeight(30);

    // Create search components
    searchField = new JTextField(20);
    searchButton = new JButton("Tìm kiếm");
    refreshButton = new JButton("Làm mới");
    showInactiveCheckbox = new JCheckBox("Hiển thị tài khoản đã vô hiệu hóa");

    // Action buttons
    addButton = new JButton("Thêm giảng viên");
    editButton = new JButton("Sửa");
    deleteButton = new JButton("Xóa");
    viewCoursesButton = new JButton("Xem lớp đang dạy");
    activateButton = new JButton("Kích hoạt lại");

    editButton.setEnabled(false);
    deleteButton.setEnabled(false);
    viewCoursesButton.setEnabled(false);
    activateButton.setEnabled(false);

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
    searchPanel.add(showInactiveCheckbox);
    topPanel.add(searchPanel, BorderLayout.WEST);

    // Action panel
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actionPanel.add(addButton);
    actionPanel.add(editButton);
    actionPanel.add(deleteButton);
    actionPanel.add(activateButton);
    actionPanel.add(viewCoursesButton);
    topPanel.add(actionPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);

    // Center panel with table
    JScrollPane tableScrollPane = new JScrollPane(teacherTable);
    tableScrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách giảng viên"));

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

    // Show inactive checkbox
    showInactiveCheckbox.addActionListener(e -> refreshData());

    // Action buttons
    addButton.addActionListener(e -> addTeacher());
    editButton.addActionListener(e -> editTeacher());
    deleteButton.addActionListener(e -> deleteTeacher());
    activateButton.addActionListener(e -> activateTeacher());
    viewCoursesButton.addActionListener(e -> viewTeacherCourses());

    // Table selection listener
    teacherTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        updateButtonStates();
      }
    });

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

  private void loadInitialData() {
    refreshData();
  }

  public void refreshData() {
    // Prevent multiple simultaneous refreshes
    if (isRefreshing) {
      return;
    }

    isRefreshing = true;
    addLog("Đang tải danh sách giảng viên...");
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        String action = showInactiveCheckbox.isSelected()
            ? Constants.ACTION_GET_ALL_TEACHERS_INCLUDE_INACTIVE
            : Constants.ACTION_GET_ALL_TEACHERS;
        Message request = Message.createRequest(action);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<User> teachers = (List<User>) response.getData("teachers");
            if (teachers != null) {
              updateTeacherTable(teachers);
              addLog("Đã tải " + teachers.size() + " giảng viên");
            } else {
              addLog("Không có dữ liệu giảng viên");
            }
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể tải danh sách giảng viên: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tải danh sách giảng viên: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        } finally {
          isRefreshing = false;
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
        Message request = Message.createRequest(Constants.ACTION_SEARCH_TEACHERS);
        request.addData("keyword", keyword);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<User> teachers = (List<User>) response.getData("teachers");
            updateTeacherTable(teachers);
            addLog("Tìm thấy " + teachers.size() + " giảng viên");
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

  private void viewTeacherCourses() {
    int selectedRow = teacherTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một giảng viên để xem lớp đang dạy");
      return;
    }

    User selectedTeacher = currentTeachers.get(selectedRow);
    addLog("Đang tải lớp của giảng viên: " + selectedTeacher.getFullName());

    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSES_BY_TEACHER);
        request.addData("teacherUsername", selectedTeacher.getUsername());
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Course> courses = (List<Course>) response.getData("courses");
            showTeacherCoursesDialog(selectedTeacher, courses);
            addLog("Đã tải " + courses.size() + " lớp học phần");
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể tải danh sách lớp: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tải danh sách lớp: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void showTeacherCoursesDialog(User teacher, List<Course> courses) {
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
        "Lớp học phần - " + teacher.getFullName(), true);
    dialog.setSize(800, 500);
    dialog.setLocationRelativeTo(this);

    String[] columnNames = { "Mã lớp", "Môn học", "Năm học", "Học kỳ", "Lịch học", "Phòng", "SV hiện tại/Tối đa" };
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    for (Course course : courses) {
      Object[] row = {
          course.getCourseCode(),
          course.getSubjectName(),
          course.getAcademicYear(),
          course.getSemester(),
          course.getScheduleDay() + " " + course.getScheduleTime(),
          course.getRoom(),
          course.getCurrentStudents() + "/" + course.getMaxStudents()
      };
      model.addRow(row);
    }

    JTable table = new JTable(model);
    table.setRowHeight(25);
    JScrollPane scrollPane = new JScrollPane(table);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton closeButton = new JButton("Đóng");
    closeButton.addActionListener(e -> dialog.dispose());
    buttonPanel.add(closeButton);

    dialog.setLayout(new BorderLayout());
    dialog.add(scrollPane, BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
  }

  private void updateTeacherTable(List<User> teachers) {
    this.currentTeachers = teachers;
    tableModel.setRowCount(0);

    // Load faculty names for display
    java.util.Map<String, String> facultyMap = new java.util.HashMap<>();
    SwingWorker<java.util.Map<String, String>, Void> facultyWorker = new SwingWorker<java.util.Map<String, String>, Void>() {
      @Override
      protected java.util.Map<String, String> doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
        Message response = serverConnection.sendRequest(request);
        if (response != null && response.isSuccess()) {
          @SuppressWarnings("unchecked")
          List<com.university.sms.model.Faculty> faculties = (List<com.university.sms.model.Faculty>) response
              .getData("faculties");
          java.util.Map<String, String> map = new java.util.HashMap<>();
          if (faculties != null) {
            for (com.university.sms.model.Faculty f : faculties) {
              map.put(f.getFacultyCode(), f.getFacultyName());
            }
          }
          return map;
        }
        return new java.util.HashMap<>();
      }

      @Override
      protected void done() {
        try {
          facultyMap.putAll(get());
          // Update table with faculty names
          tableModel.setRowCount(0);
          for (User teacher : currentTeachers) {
            String facultyName = teacher.getFacultyCode() != null
                ? facultyMap.getOrDefault(teacher.getFacultyCode(), teacher.getFacultyCode())
                : "N/A";
            Object[] row = {
                teacher.getUsername(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.getAddress(),
                facultyName,
                teacher.isActive() ? "Hoạt động" : "Khóa"
            };
            tableModel.addRow(row);
          }
        } catch (Exception e) {
          // If loading fails, show without faculty
          for (User teacher : currentTeachers) {
            String facultyName = teacher.getFacultyCode() != null ? teacher.getFacultyCode() : "N/A";
            Object[] row = {
                teacher.getUsername(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.getAddress(),
                facultyName,
                teacher.isActive() ? "Hoạt động" : "Khóa"
            };
            tableModel.addRow(row);
          }
        }
      }
    };
    facultyWorker.execute();
  }

  private void updateButtonStates() {
    int selectedRow = teacherTable.getSelectedRow();
    boolean hasSelection = selectedRow >= 0;

    if (hasSelection && currentTeachers != null && selectedRow < currentTeachers.size()) {
      User selectedTeacher = currentTeachers.get(selectedRow);
      boolean isActive = selectedTeacher.isActive();

      editButton.setEnabled(true);
      deleteButton.setEnabled(isActive); // Chỉ cho xóa (vô hiệu hóa) nếu đang active
      activateButton.setEnabled(!isActive); // Chỉ cho kích hoạt lại nếu đang inactive
      viewCoursesButton.setEnabled(true);
    } else {
      editButton.setEnabled(false);
      deleteButton.setEnabled(false);
      activateButton.setEnabled(false);
      viewCoursesButton.setEnabled(false);
    }
  }

  private void activateTeacher() {
    int selectedRow = teacherTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một giảng viên để kích hoạt lại");
      return;
    }

    User selectedTeacher = currentTeachers.get(selectedRow);
    if (selectedTeacher.isActive()) {
      showErrorMessage("Giảng viên này đang hoạt động");
      return;
    }

    int confirm = JOptionPane.showConfirmDialog(this,
        "Bạn có chắc chắn muốn kích hoạt lại giảng viên:\n" + selectedTeacher.getFullName() + "?",
        "Xác nhận kích hoạt",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    addLog("Đang kích hoạt giảng viên: " + selectedTeacher.getFullName());
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_ACTIVATE_USER);
        request.addData("userId", selectedTeacher.getUserId());
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            JOptionPane.showMessageDialog(TeacherPanel.this,
                "Đã kích hoạt giảng viên thành công",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            addLog("Đã kích hoạt giảng viên: " + selectedTeacher.getFullName());
            refreshData();
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể kích hoạt giảng viên: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi kích hoạt giảng viên: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void addTeacher() {
    TeacherEditDialog dialog = new TeacherEditDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        serverConnection,
        null // New teacher
    );
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã thêm giảng viên mới");
      refreshData();
    }
  }

  private void editTeacher() {
    int selectedRow = teacherTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một giảng viên để chỉnh sửa");
      return;
    }

    User selectedTeacher = currentTeachers.get(selectedRow);
    TeacherEditDialog dialog = new TeacherEditDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        serverConnection,
        selectedTeacher);
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã cập nhật thông tin giảng viên: " + selectedTeacher.getFullName());
      refreshData();
    }
  }

  private void deleteTeacher() {
    int selectedRow = teacherTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một giảng viên để xóa");
      return;
    }

    User selectedTeacher = currentTeachers.get(selectedRow);
    int confirm = JOptionPane.showConfirmDialog(this,
        "Bạn có chắc chắn muốn xóa giảng viên:\n" + selectedTeacher.getFullName() + "?",
        "Xác nhận xóa",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    addLog("Đang xóa giảng viên: " + selectedTeacher.getFullName());
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_DELETE_TEACHER);
        request.addData("userId", selectedTeacher.getUserId());
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            JOptionPane.showMessageDialog(TeacherPanel.this,
                "Đã xóa giảng viên thành công",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            addLog("Đã xóa giảng viên: " + selectedTeacher.getFullName());
            refreshData();
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể xóa giảng viên: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi xóa giảng viên: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  // Inner class: Teacher Edit Dialog
  private static class TeacherEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private User teacher;
    private boolean saved = false;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextArea addressArea;
    private JComboBox<FacultyItem> facultyCombo;

    public TeacherEditDialog(Frame parent, IServerConnection serverConnection, User teacher) {
      super(parent, teacher == null ? "Thêm giảng viên mới" : "Chỉnh sửa giảng viên", true);
      this.serverConnection = serverConnection;
      this.teacher = teacher;

      initComponents();
      setSize(500, 500);
      setLocationRelativeTo(parent);
    }

    private void initComponents() {
      setLayout(new BorderLayout(10, 10));

      // Form panel
      JPanel formPanel = new JPanel(new GridBagLayout());
      formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.insets = new Insets(5, 5, 5, 5);

      // Username
      gbc.gridx = 0;
      gbc.gridy = 0;
      formPanel.add(new JLabel("Tên đăng nhập:"), gbc);
      gbc.gridx = 1;
      usernameField = new JTextField(20);
      formPanel.add(usernameField, gbc);

      // Password
      gbc.gridx = 0;
      gbc.gridy = 1;
      formPanel.add(new JLabel(teacher == null ? "Mật khẩu:" : "Mật khẩu mới (để trống nếu không đổi):"), gbc);
      gbc.gridx = 1;
      passwordField = new JPasswordField(20);
      formPanel.add(passwordField, gbc);

      // Full name
      gbc.gridx = 0;
      gbc.gridy = 2;
      formPanel.add(new JLabel("Họ tên:"), gbc);
      gbc.gridx = 1;
      fullNameField = new JTextField(20);
      formPanel.add(fullNameField, gbc);

      // Email
      gbc.gridx = 0;
      gbc.gridy = 3;
      formPanel.add(new JLabel("Email:"), gbc);
      gbc.gridx = 1;
      emailField = new JTextField(20);
      formPanel.add(emailField, gbc);

      // Phone
      gbc.gridx = 0;
      gbc.gridy = 4;
      formPanel.add(new JLabel("Số điện thoại:"), gbc);
      gbc.gridx = 1;
      phoneField = new JTextField(20);
      formPanel.add(phoneField, gbc);

      // Address
      gbc.gridx = 0;
      gbc.gridy = 5;
      formPanel.add(new JLabel("Địa chỉ:"), gbc);
      gbc.gridx = 1;
      addressArea = new JTextArea(3, 20);
      addressArea.setLineWrap(true);
      JScrollPane addressScroll = new JScrollPane(addressArea);
      formPanel.add(addressScroll, gbc);

      // Faculty
      gbc.gridx = 0;
      gbc.gridy = 6;
      formPanel.add(new JLabel("Khoa:"), gbc);
      gbc.gridx = 1;
      facultyCombo = new JComboBox<>();
      formPanel.add(facultyCombo, gbc);

      // Load faculties
      SwingWorker<List<com.university.sms.model.Faculty>, Void> facultyWorker = new SwingWorker<List<com.university.sms.model.Faculty>, Void>() {
        @Override
        protected List<com.university.sms.model.Faculty> doInBackground() throws Exception {
          Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
          Message response = serverConnection.sendRequest(request);
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Faculty> faculties = (List<com.university.sms.model.Faculty>) response
                .getData("faculties");
            return faculties;
          }
          return null;
        }

        @Override
        protected void done() {
          try {
            List<com.university.sms.model.Faculty> faculties = get();
            if (faculties != null) {
              for (com.university.sms.model.Faculty f : faculties) {
                facultyCombo.addItem(new FacultyItem(f.getFacultyCode(), f.getFacultyName()));
              }
              // Set selection if editing
              if (teacher != null && teacher.getFacultyCode() != null) {
                for (int i = 0; i < facultyCombo.getItemCount(); i++) {
                  FacultyItem item = facultyCombo.getItemAt(i);
                  if (item.code != null && item.code.equals(teacher.getFacultyCode())) {
                    facultyCombo.setSelectedIndex(i);
                    break;
                  }
                }
              }
            }
          } catch (Exception e) {
            JOptionPane.showMessageDialog(TeacherEditDialog.this,
                "Lỗi khi tải danh sách khoa: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      };
      facultyWorker.execute();

      // Load data if editing
      if (teacher != null) {
        usernameField.setText(teacher.getUsername());
        usernameField.setEnabled(false); // Cannot change username
        fullNameField.setText(teacher.getFullName());
        emailField.setText(teacher.getEmail());
        phoneField.setText(teacher.getPhone());
        addressArea.setText(teacher.getAddress());
      }

      add(formPanel, BorderLayout.CENTER);

      // Button panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      JButton saveButton = new JButton("Lưu");
      JButton cancelButton = new JButton("Hủy");

      saveButton.addActionListener(e -> save());
      cancelButton.addActionListener(e -> dispose());

      buttonPanel.add(saveButton);
      buttonPanel.add(cancelButton);
      add(buttonPanel, BorderLayout.SOUTH);
    }

    private void save() {
      // Validate
      String username = usernameField.getText().trim();
      String password = new String(passwordField.getPassword());
      String fullName = fullNameField.getText().trim();
      String email = emailField.getText().trim();

      if (username.isEmpty() || fullName.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Vui lòng nhập tên đăng nhập và họ tên",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (teacher == null && password.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Vui lòng nhập mật khẩu cho giảng viên mới",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Get selected faculty
      FacultyItem selectedFaculty = (FacultyItem) facultyCombo.getSelectedItem();
      String facultyCode = selectedFaculty != null ? selectedFaculty.code : null;

      // Create/update teacher
      Message request;
      if (teacher == null) {
        // Add new teacher
        request = Message.createRequest(Constants.ACTION_ADD_TEACHER);
        request.addData("username", username);
        request.addData("password", password);
        request.addData("fullName", fullName);
        request.addData("email", email);
        request.addData("phone", phoneField.getText().trim());
        request.addData("address", addressArea.getText().trim());
        if (facultyCode != null) {
          request.addData("facultyCode", facultyCode);
        }
      } else {
        // Update existing teacher
        request = Message.createRequest(Constants.ACTION_UPDATE_TEACHER);
        request.addData("userId", teacher.getUserId());
        request.addData("fullName", fullName);
        request.addData("email", email);
        request.addData("phone", phoneField.getText().trim());
        request.addData("address", addressArea.getText().trim());
        if (facultyCode != null) {
          request.addData("facultyCode", facultyCode);
        }

        // Only update password if provided
        if (!password.isEmpty()) {
          request.addData("password", password);
        }
      }

      Message response = serverConnection.sendRequest(request);
      if (response != null && response.isSuccess()) {
        saved = true;
        dispose();
      } else {
        String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
        JOptionPane.showMessageDialog(this,
            "Lỗi: " + errorMsg,
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
      }
    }

    public boolean isSaved() {
      return saved;
    }
  }

  private void addLog(String message) {
    logArea
        .append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] " + message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  private void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
  }

  // Helper class for ComboBox items
  private static class FacultyItem {
    String code;
    String name;

    FacultyItem(String code, String name) {
      this.code = code;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
