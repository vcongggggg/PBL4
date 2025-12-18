package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.Class;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Panel quản lý lớp sinh hoạt (dành cho Admin)
 */
public class ClassPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private User currentUser;
  private IServerConnection serverConnection;

  private JTable classTable;
  private DefaultTableModel tableModel;
  private JTextField searchField;
  private JButton searchButton;
  private JButton refreshButton;
  private JButton addButton;
  private JButton editButton;
  private JButton deleteButton;
  private JButton viewStudentsButton;

  private List<Class> currentClasses;

  // Flag to prevent multiple simultaneous refresh
  private boolean isRefreshing = false;
  private boolean isInitialized = false;

  // Log area components
  private JTextArea logArea;
  private JScrollPane logScrollPane;

  public ClassPanel(User currentUser, IServerConnection serverConnection) {
    this.currentUser = currentUser;
    this.serverConnection = serverConnection;

    initializeComponents();
    setupLayout();
    setupEventListeners();
    isInitialized = true;
  }

  private void initializeComponents() {
    // Create table
    String[] columnNames = { "Mã lớp", "Tên lớp", "Khoa", "Giáo viên", "Năm học", "Học kỳ", "Số SV tối đa" };
    tableModel = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    classTable = new JTable(tableModel);
    classTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    classTable.setRowHeight(30);

    // Create search components
    searchField = new JTextField(20);
    searchButton = new JButton("Tìm kiếm");
    refreshButton = new JButton("Làm mới");

    // Action buttons
    addButton = new JButton("Thêm lớp");
    editButton = new JButton("Sửa");
    deleteButton = new JButton("Xóa");
    viewStudentsButton = new JButton("Xem sinh viên");

    editButton.setEnabled(false);
    deleteButton.setEnabled(false);
    viewStudentsButton.setEnabled(false);

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
    actionPanel.add(viewStudentsButton);
    if (currentUser.getRole() == User.UserRole.ADMIN) {
      actionPanel.add(addButton);
      actionPanel.add(editButton);
      actionPanel.add(deleteButton);
    }
    topPanel.add(actionPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);

    // Center panel with table
    JScrollPane tableScrollPane = new JScrollPane(classTable);
    tableScrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách lớp sinh hoạt"));

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

    // Action buttons
    addButton.addActionListener(e -> addClass());
    editButton.addActionListener(e -> editClass());
    deleteButton.addActionListener(e -> deleteClass());
    viewStudentsButton.addActionListener(e -> viewClassStudents());

    // Table selection listener
    classTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        updateButtonStates();
      }
    });

    // Auto-refresh khi panel được hiển thị
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        if (isInitialized && !isRefreshing) {
          refreshData();
        }
      }
    });
  }

  public void refreshData() {
    if (isRefreshing) {
      return;
    }

    isRefreshing = true;
    addLog("Đang tải danh sách lớp...");
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_CLASSES);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Class> classes = (List<Class>) response.getData(Constants.KEY_CLASSES);
            if (classes != null) {
              updateClassTable(classes);
              addLog("Đã tải " + classes.size() + " lớp");
            } else {
              addLog("Không có dữ liệu lớp");
            }
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể tải danh sách lớp: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tải danh sách lớp: " + e.getMessage());
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
    // For now, just filter the current classes
    if (currentClasses != null) {
      List<Class> filtered = currentClasses.stream()
          .filter(c -> c.getClassCode().toLowerCase().contains(keyword.toLowerCase()) ||
              (c.getClassName() != null && c.getClassName().toLowerCase().contains(keyword.toLowerCase())) ||
              (c.getFacultyName() != null && c.getFacultyName().toLowerCase().contains(keyword.toLowerCase())))
          .collect(java.util.stream.Collectors.toList());
      updateClassTable(filtered);
      addLog("Tìm thấy " + filtered.size() + " lớp");
    }
  }

  private void updateClassTable(List<Class> classes) {
    this.currentClasses = classes;
    tableModel.setRowCount(0);

    for (Class clazz : classes) {
      Object[] row = {
          clazz.getClassCode(),
          clazz.getClassName(),
          clazz.getFacultyName() != null ? clazz.getFacultyName() : clazz.getFacultyCode(),
          clazz.getTeacherName() != null ? clazz.getTeacherName()
              : (clazz.getTeacherUsername() != null ? clazz.getTeacherUsername() : "N/A"),
          clazz.getAcademicYear(),
          clazz.getSemester(),
          clazz.getMaxStudents() != null ? clazz.getMaxStudents() : "N/A"
      };
      tableModel.addRow(row);
    }
  }

  private void updateButtonStates() {
    int selectedRow = classTable.getSelectedRow();
    boolean hasSelection = selectedRow >= 0;

    viewStudentsButton.setEnabled(hasSelection);
    if (hasSelection && currentUser.getRole() == User.UserRole.ADMIN) {
      editButton.setEnabled(true);
      deleteButton.setEnabled(true);
    } else {
      editButton.setEnabled(false);
      deleteButton.setEnabled(false);
    }
  }

  private void addClass() {
    ClassEditDialog dialog = new ClassEditDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        serverConnection,
        null // New class
    );
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã thêm lớp mới");
      refreshData();
    }
  }

  private void editClass() {
    int selectedRow = classTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một lớp để chỉnh sửa");
      return;
    }

    Class selectedClass = currentClasses.get(selectedRow);
    ClassEditDialog dialog = new ClassEditDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        serverConnection,
        selectedClass);
    dialog.setVisible(true);

    if (dialog.isSaved()) {
      addLog("Đã cập nhật thông tin lớp: " + selectedClass.getClassCode());
      refreshData();
    }
  }

  private void deleteClass() {
    int selectedRow = classTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một lớp để xóa");
      return;
    }

    Class selectedClass = currentClasses.get(selectedRow);
    int confirm = JOptionPane.showConfirmDialog(this,
        "Bạn có chắc chắn muốn xóa lớp:\n" + selectedClass.getClassCode() + " - " + selectedClass.getClassName() + "?",
        "Xác nhận xóa",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    addLog("Đang xóa lớp: " + selectedClass.getClassCode());
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_DELETE_CLASS);
        request.addData(Constants.KEY_CLASS_ID, selectedClass.getClassId());
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            JOptionPane.showMessageDialog(ClassPanel.this,
                "Đã xóa lớp thành công",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            addLog("Đã xóa lớp: " + selectedClass.getClassCode());
            refreshData();
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể xóa lớp: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi xóa lớp: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void viewClassStudents() {
    int selectedRow = classTable.getSelectedRow();
    if (selectedRow < 0) {
      showErrorMessage("Vui lòng chọn một lớp để xem sinh viên");
      return;
    }

    Class selectedClass = currentClasses.get(selectedRow);
    if (selectedClass.getClassCode() == null) {
      showErrorMessage("Lớp này chưa có mã lớp");
      return;
    }

    addLog("Đang tải danh sách sinh viên của lớp: " + selectedClass.getClassCode());
    SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
      @Override
      protected Message doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_STUDENTS_BY_CLASS);
        request.addData(Constants.KEY_CLASS_CODE, selectedClass.getClassCode());
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Student> students = (List<com.university.sms.model.Student>) response
                .getData(Constants.KEY_STUDENTS);
            showClassStudentsDialog(selectedClass, students);
            addLog("Đã tải " + (students != null ? students.size() : 0) + " sinh viên");
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            showErrorMessage("Không thể tải danh sách sinh viên: " + errorMsg);
            addLog("Lỗi: " + errorMsg);
          }
        } catch (Exception e) {
          showErrorMessage("Lỗi khi tải danh sách sinh viên: " + e.getMessage());
          addLog("Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  private void showClassStudentsDialog(Class clazz, List<com.university.sms.model.Student> students) {
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
        "Danh sách sinh viên - " + clazz.getClassCode() + " - " + clazz.getClassName(), true);
    dialog.setSize(900, 500);
    dialog.setLocationRelativeTo(this);

    String[] columnNames = { "Mã SV", "Họ tên", "Email", "SĐT", "Khoa", "GPA", "Tín chỉ", "Trạng thái" };
    DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    if (students != null) {
      for (com.university.sms.model.Student student : students) {
        Object[] row = {
            student.getStudentCode(),
            student.getFullName(),
            student.getEmail(),
            student.getPhone(),
            student.getFacultyName() != null ? student.getFacultyName() : student.getFacultyCode(),
            getGpaDisplay(student.getGpa()),
            student.getTotalCredits(),
            getStatusDisplay(student.getStudentStatus())
        };
        model.addRow(row);
      }
    }

    JTable table = new JTable(model);
    table.setRowHeight(25);
    JScrollPane scrollPane = new JScrollPane(table);

    JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("Tổng số sinh viên: " + (students != null ? students.size() : 0)));
    if (clazz.getMaxStudents() != null) {
      infoPanel.add(new JLabel(" | Số SV tối đa: " + clazz.getMaxStudents()));
    }

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton closeButton = new JButton("Đóng");
    closeButton.addActionListener(e -> dialog.dispose());
    buttonPanel.add(closeButton);

    dialog.setLayout(new BorderLayout());
    dialog.add(infoPanel, BorderLayout.NORTH);
    dialog.add(scrollPane, BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
  }

  private void addLog(String message) {
    logArea
        .append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] " + message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  private void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
  }

  // Helper methods for display formatting
  private String getStatusDisplay(com.university.sms.model.Student.StudentStatus status) {
    if (status == null) {
      return "N/A";
    }
    switch (status) {
      case ACTIVE:
        return "Đang học";
      case SUSPENDED:
        return "Tạm đình chỉ";
      case GRADUATED:
        return "Đã tốt nghiệp";
      case DROPPED:
        return "Thôi học";
      default:
        return status.toString();
    }
  }

  private Object getGpaDisplay(java.math.BigDecimal gpa) {
    if (gpa == null || gpa.compareTo(java.math.BigDecimal.ZERO) == 0) {
      return ""; // Để trống nếu không có GPA hoặc GPA = 0
    }
    return gpa;
  }

  // Inner class: Class Edit Dialog
  private static class ClassEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private Class classEntity;
    private boolean saved = false;

    private JTextField codeField;
    private JTextField nameField;
    private JComboBox<FacultyItem> facultyCombo;
    private JComboBox<TeacherItem> teacherCombo;
    private JTextField academicYearField;
    private JSpinner semesterSpinner;
    private JSpinner maxStudentsSpinner;

    public ClassEditDialog(Frame parent, IServerConnection serverConnection, Class classEntity) {
      super(parent, classEntity == null ? "Thêm lớp mới" : "Chỉnh sửa lớp", true);
      this.serverConnection = serverConnection;
      this.classEntity = classEntity;

      // Borderless + header tùy chỉnh để đồng bộ với các dialog khác
      setUndecorated(true);

      initComponents();
      pack();
      setLocationRelativeTo(parent);
    }

    private void initComponents() {
      setLayout(new BorderLayout());

      Color primaryColor = new Color(44, 62, 80); // match sidebar
      Color backgroundColor = new Color(245, 247, 250);
      Color cardBorderColor = new Color(220, 224, 230);

      // ========= Form controls =========
      JPanel formPanel = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.insets = new Insets(5, 5, 5, 5);

      int row = 0;

      // Class Code
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Mã lớp:"), gbc);
      gbc.gridx = 1;
      codeField = new JTextField(20);
      styleTextField(codeField);
      formPanel.add(codeField, gbc);

      // Class Name
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Tên lớp:"), gbc);
      gbc.gridx = 1;
      nameField = new JTextField(20);
      styleTextField(nameField);
      formPanel.add(nameField, gbc);

      // Faculty
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Khoa:"), gbc);
      gbc.gridx = 1;
      facultyCombo = new JComboBox<>();
      facultyCombo.addItem(new FacultyItem(null, "-- Chọn khoa --"));
      styleComboBox(facultyCombo);
      formPanel.add(facultyCombo, gbc);

      // Teacher
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Giáo viên:"), gbc);
      gbc.gridx = 1;
      teacherCombo = new JComboBox<>();
      teacherCombo.addItem(new TeacherItem(null, "-- Chọn giáo viên --"));
      teacherCombo.setEnabled(false);
      styleComboBox(teacherCombo);
      formPanel.add(teacherCombo, gbc);

      // Academic Year
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Năm học:"), gbc);
      gbc.gridx = 1;
      academicYearField = new JTextField(20);
      styleTextField(academicYearField);
      formPanel.add(academicYearField, gbc);

      // Semester
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Học kỳ:"), gbc);
      gbc.gridx = 1;
      semesterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
      styleSpinner(semesterSpinner);
      formPanel.add(semesterSpinner, gbc);

      // Max Students
      gbc.gridx = 0;
      gbc.gridy = row++;
      formPanel.add(new JLabel("Số SV tối đa:"), gbc);
      gbc.gridx = 1;
      maxStudentsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 200, 1));
      styleSpinner(maxStudentsSpinner);
      formPanel.add(maxStudentsSpinner, gbc);

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
              if (classEntity != null && classEntity.getFacultyCode() != null) {
                for (int i = 0; i < facultyCombo.getItemCount(); i++) {
                  FacultyItem item = facultyCombo.getItemAt(i);
                  if (item.code != null && item.code.equals(classEntity.getFacultyCode())) {
                    facultyCombo.setSelectedIndex(i);
                    loadTeachers(item.code);
                    break;
                  }
                }
              } else {
                facultyCombo.setSelectedIndex(0);
                loadTeachers(null);
              }
            }
          } catch (Exception e) {
            JOptionPane.showMessageDialog(ClassEditDialog.this,
                "Lỗi khi tải danh sách khoa: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      };
      facultyWorker.execute();

      facultyCombo.addActionListener(e -> {
        FacultyItem item = (FacultyItem) facultyCombo.getSelectedItem();
        loadTeachers(item != null ? item.code : null);
      });

      // Load data if editing
      if (classEntity != null) {
        codeField.setText(classEntity.getClassCode());
        codeField.setEnabled(false); // Cannot change code
        nameField.setText(classEntity.getClassName());
        academicYearField.setText(classEntity.getAcademicYear());
        semesterSpinner.setValue(classEntity.getSemester());
        if (classEntity.getMaxStudents() != null) {
          maxStudentsSpinner.setValue(classEntity.getMaxStudents());
        }
      } else {
        // Set default academic year
        academicYearField.setText(java.time.Year.now() + "-" + (java.time.Year.now().getValue() + 1));
      }

      // Header
      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setBackground(primaryColor);
      headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
      headerPanel.setPreferredSize(new Dimension(0, 60));

      JLabel titleLabel = new JLabel(classEntity == null ? "Thêm lớp mới" : "Chỉnh sửa lớp");
      titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel, BorderLayout.WEST);

      JButton closeButton = new JButton("X");
      closeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
      closeButton.setForeground(Color.WHITE);
      closeButton.setContentAreaFilled(false);
      closeButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
      closeButton.setFocusPainted(false);
      closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      closeButton.addActionListener(e -> dispose());
      closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
          closeButton.setForeground(new Color(255, 200, 200));
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
          closeButton.setForeground(Color.WHITE);
        }
      });
      headerPanel.add(closeButton, BorderLayout.EAST);

      add(headerPanel, BorderLayout.NORTH);

      // Root + card panel
      JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
      rootPanel.setBackground(backgroundColor);
      rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

      JPanel cardPanel = new JPanel(new BorderLayout(10, 10));
      cardPanel.setBackground(Color.WHITE);
      cardPanel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(cardBorderColor, 1),
          BorderFactory.createEmptyBorder(20, 20, 20, 20)));
      cardPanel.add(formPanel, BorderLayout.CENTER);

      // Button panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
      buttonPanel.setOpaque(false);
      JButton saveButton = new JButton("Lưu");
      JButton cancelButton = new JButton("Hủy");
      styleButton(saveButton, new Color(41, 128, 185));
      styleButton(cancelButton, new Color(108, 117, 125));

      saveButton.addActionListener(e -> save());
      cancelButton.addActionListener(e -> dispose());

      buttonPanel.add(cancelButton);
      buttonPanel.add(saveButton);
      cardPanel.add(buttonPanel, BorderLayout.SOUTH);

      rootPanel.add(cardPanel, BorderLayout.CENTER);
      add(rootPanel, BorderLayout.CENTER);

      // Viền ngoài cùng cho dialog thêm/sửa lớp
      if (getContentPane() instanceof JComponent) {
        ((JComponent) getContentPane())
            .setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));
      }
    }

    private void save() {
      // Validation
      String code = codeField.getText().trim();
      String name = nameField.getText().trim();

      if (code.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Vui lòng nhập mã lớp",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (name.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Vui lòng nhập tên lớp",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (facultyCombo.getSelectedItem() == null) {
        JOptionPane.showMessageDialog(this,
            "Vui lòng chọn khoa",
            "Lỗi",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Create or update class
      Class newClass = classEntity == null ? new Class() : classEntity;
      newClass.setClassCode(code);
      newClass.setClassName(name);
      FacultyItem selectedFaculty = (FacultyItem) facultyCombo.getSelectedItem();
      newClass.setFacultyCode(selectedFaculty != null ? selectedFaculty.code : null);
      TeacherItem selectedTeacher = (TeacherItem) teacherCombo.getSelectedItem();
      newClass.setTeacherUsername(
          selectedTeacher != null && selectedTeacher.username != null ? selectedTeacher.username : null);
      newClass.setAcademicYear(academicYearField.getText().trim());
      newClass.setSemester((Integer) semesterSpinner.getValue());
      newClass.setMaxStudents((Integer) maxStudentsSpinner.getValue());

      // Send to server
      SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
        @Override
        protected Message doInBackground() throws Exception {
          String action = classEntity == null ? Constants.ACTION_ADD_CLASS : Constants.ACTION_UPDATE_CLASS;
          Message request = Message.createRequest(action);
          request.addData(Constants.KEY_CLASS, newClass);
          return serverConnection.sendRequest(request);
        }

        @Override
        protected void done() {
          try {
            Message response = get();
            if (response != null && response.isSuccess()) {
              saved = true;
              JOptionPane.showMessageDialog(ClassEditDialog.this,
                  response.getMessage(),
                  "Thành công",
                  JOptionPane.INFORMATION_MESSAGE);
              dispose();
            } else {
              String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
              JOptionPane.showMessageDialog(ClassEditDialog.this,
                  errorMsg,
                  "Lỗi",
                  JOptionPane.ERROR_MESSAGE);
            }
          } catch (Exception e) {
            JOptionPane.showMessageDialog(ClassEditDialog.this,
                "Lỗi: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      };
      worker.execute();
    }

    // --- style helpers (chỉ thay đổi giao diện) ---
    private void styleTextField(JTextField field) {
      field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      field.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
          BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    private void styleComboBox(JComboBox<?> combo) {
      combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      combo.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
          BorderFactory.createEmptyBorder(8, 10, 8, 10)));
      combo.setBackground(Color.WHITE);
    }

    private void styleSpinner(JSpinner spinner) {
      spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        editor.getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 14));
      }
    }

    private void styleButton(JButton button, Color bgColor) {
      button.setFont(new Font("Segoe UI", Font.BOLD, 14));
      button.setForeground(Color.WHITE);
      button.setBackground(bgColor);
      button.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
      button.setFocusPainted(false);
      button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public boolean isSaved() {
      return saved;
    }

    // Helper classes for ComboBox items
    private void loadTeachers(String facultyCode) {
      teacherCombo.removeAllItems();
      if (facultyCode == null || facultyCode.trim().isEmpty()) {
        teacherCombo.addItem(new TeacherItem(null, "-- Chọn khoa trước --"));
        teacherCombo.setEnabled(false);
        return;
      }

      teacherCombo.addItem(new TeacherItem(null, "-- Không có --"));
      teacherCombo.setEnabled(true);

      SwingWorker<List<User>, Void> worker = new SwingWorker<List<User>, Void>() {
        @Override
        protected List<User> doInBackground() throws Exception {
          Message request = Message.createRequest(Constants.ACTION_GET_AVAILABLE_CLASS_TEACHERS);
          request.addData("facultyCode", facultyCode);
          if (classEntity != null && classEntity.getClassCode() != null) {
            request.addData("classCode", classEntity.getClassCode());
          }
          Message response = serverConnection.sendRequest(request);
          if (response != null && response.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<User> teachers = (List<User>) response.getData("teachers");
            return teachers;
          }
          return null;
        }

        @Override
        protected void done() {
          try {
            List<User> teachers = get();
            if (teachers != null) {
              for (User t : teachers) {
                teacherCombo.addItem(new TeacherItem(t.getUsername(), t.getFullName()));
              }
              if (classEntity != null && classEntity.getTeacherUsername() != null) {
                for (int i = 0; i < teacherCombo.getItemCount(); i++) {
                  TeacherItem item = teacherCombo.getItemAt(i);
                  if (item.username != null && item.username.equals(classEntity.getTeacherUsername())) {
                    teacherCombo.setSelectedIndex(i);
                    break;
                  }
                }
              } else {
                teacherCombo.setSelectedIndex(0);
              }
            }
          } catch (Exception e) {
            JOptionPane.showMessageDialog(ClassEditDialog.this,
                "Lỗi khi tải danh sách giáo viên: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      };
      worker.execute();
    }

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

    private static class TeacherItem {
      String username;
      String name;

      TeacherItem(String username, String name) {
        this.username = username;
        this.name = name;
      }

      @Override
      public String toString() {
        return name;
      }
    }
  }
}
