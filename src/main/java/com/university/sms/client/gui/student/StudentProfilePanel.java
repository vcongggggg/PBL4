package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.client.gui.common.ChangePasswordDialog;
import com.university.sms.client.gui.common.StudentDetailDialog;
import com.university.sms.client.gui.common.ToastNotification;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel hiển thị thông tin cá nhân của sinh viên đang đăng nhập
 */
public class StudentProfilePanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(StudentProfilePanel.class.getName());

  private User currentUser;
  private IServerConnection serverConnection;
  private Student studentInfo;

  // UI Components
  private JLabel avatarLabel;
  private JLabel nameLabel;
  private JLabel studentIdLabel;
  private JLabel emailLabel;
  private JLabel phoneLabel;
  private JLabel facultyLabel;
  private JLabel classLabel;
  private JLabel gpaLabel;
  private JLabel creditsLabel;
  private JLabel statusLabel;
  private JButton editButton;
  private JButton changePasswordButton;

  public StudentProfilePanel(User currentUser, IServerConnection serverConnection) {
    this.currentUser = currentUser;
    this.serverConnection = serverConnection;

    initializeComponents();
    setupLayout();
    loadStudentData();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout(20, 20));
    setBorder(new EmptyBorder(20, 20, 20, 20));
    setBackground(new Color(240, 242, 245));

    // Avatar
    avatarLabel = new JLabel("👤", JLabel.CENTER);
    avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
    avatarLabel.setForeground(new Color(52, 152, 219));

    // Labels
    nameLabel = new JLabel("---");
    nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

    studentIdLabel = new JLabel("Mã SV: ---");
    studentIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    studentIdLabel.setForeground(Color.GRAY);

    emailLabel = new JLabel("---");
    phoneLabel = new JLabel("---");
    facultyLabel = new JLabel("---");
    classLabel = new JLabel("---");
    gpaLabel = new JLabel("0.00");
    creditsLabel = new JLabel("0");
    statusLabel = new JLabel("Đang học");
    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

    // Buttons with improved styling - same size
    editButton = new JButton("Chỉnh sửa Thông tin");
    editButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
    editButton.setBackground(new Color(52, 152, 219));
    editButton.setForeground(Color.WHITE);
    editButton.setFocusPainted(false);
    editButton.setBorderPainted(false);
    editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    editButton.setPreferredSize(new Dimension(280, 40));
    editButton.setMinimumSize(new Dimension(280, 40));
    editButton.setMaximumSize(new Dimension(280, 40));
    editButton.addActionListener(e -> editProfile());

    changePasswordButton = new JButton("Đổi Mật khẩu");
    changePasswordButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
    changePasswordButton.setBackground(new Color(46, 204, 113));
    changePasswordButton.setForeground(Color.WHITE);
    changePasswordButton.setFocusPainted(false);
    changePasswordButton.setBorderPainted(false);
    changePasswordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    changePasswordButton.setPreferredSize(new Dimension(280, 40));
    changePasswordButton.setMinimumSize(new Dimension(280, 40));
    changePasswordButton.setMaximumSize(new Dimension(280, 40));
    changePasswordButton.addActionListener(e -> changePassword());
  }

  private void setupLayout() {
    // Main wrapper panel with better spacing
    JPanel mainWrapper = new JPanel(new BorderLayout(25, 0));
    mainWrapper.setOpaque(false);

    // Left panel - Avatar & Basic Info with rounded border and shadow effect
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBackground(Color.WHITE);
    leftPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
        new EmptyBorder(30, 30, 30, 30)));
    leftPanel.setPreferredSize(new Dimension(380, 0));

    // Avatar centered
    avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(avatarLabel);
    leftPanel.add(Box.createVerticalStrut(15));

    // Name with better styling
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(nameLabel);
    leftPanel.add(Box.createVerticalStrut(5));

    // Student ID
    studentIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(studentIdLabel);
    leftPanel.add(Box.createVerticalStrut(18));

    // Separator
    JSeparator separator1 = new JSeparator();
    separator1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    separator1.setForeground(new Color(230, 230, 230));
    leftPanel.add(separator1);
    leftPanel.add(Box.createVerticalStrut(12));

    // Status with better presentation
    JPanel statusPanel = createInfoRow("Trạng thái:", statusLabel);
    leftPanel.add(statusPanel);
    leftPanel.add(Box.createVerticalStrut(18));

    // Separator before buttons
    JSeparator separator2 = new JSeparator();
    separator2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    separator2.setForeground(new Color(230, 230, 230));
    leftPanel.add(separator2);
    leftPanel.add(Box.createVerticalStrut(18));

    // Buttons without icons - improved layout
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    buttonPanel.setOpaque(false);
    buttonPanel.setMaximumSize(new Dimension(300, 100));

    // Edit button
    editButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    buttonPanel.add(editButton);
    buttonPanel.add(Box.createVerticalStrut(10));

    // Change password button
    changePasswordButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    buttonPanel.add(changePasswordButton);

    buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(buttonPanel);

    leftPanel.add(Box.createVerticalGlue());

    // Right panel - Detailed Info with better layout
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    rightPanel.setOpaque(false);
    rightPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

    // Top row: Contact and Academic side by side
    JPanel topCardsRow = new JPanel(new GridLayout(1, 2, 18, 0));
    topCardsRow.setOpaque(false);
    topCardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
    topCardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Contact Information Card
    JPanel contactCard = createCardWithIcon("📞", "Thông tin Liên hệ", new Color(52, 152, 219));
    contactCard.add(createDetailRowWithIcon("📧", "Email", emailLabel));
    contactCard.add(Box.createVerticalStrut(10));
    contactCard.add(createDetailRowWithIcon("📱", "Điện thoại", phoneLabel));
    topCardsRow.add(contactCard);

    // Academic Information Card
    JPanel academicCard = createCardWithIcon("🎓", "Thông tin Học tập", new Color(155, 89, 182));
    academicCard.add(createDetailRowWithIcon("🏛️", "Khoa", facultyLabel));
    academicCard.add(Box.createVerticalStrut(10));
    academicCard.add(createDetailRowWithIcon("👥", "Lớp", classLabel));
    topCardsRow.add(academicCard);

    rightPanel.add(topCardsRow);
    rightPanel.add(Box.createVerticalStrut(18));

    // Statistics Card - aligned left like other cards
    JPanel statsCard = createCardWithIcon("📊", "Thống kê Học tập", new Color(230, 126, 34));
    statsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
    statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel gpaPanel = new JPanel();
    gpaPanel.setLayout(new BoxLayout(gpaPanel, BoxLayout.Y_AXIS));
    gpaPanel.setBackground(new Color(46, 204, 113, 18));
    gpaPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
        new EmptyBorder(10, 15, 10, 15)));

    // GPA Title with icon
    JPanel gpaTitlePanel = new JPanel();
    gpaTitlePanel.setLayout(new BoxLayout(gpaTitlePanel, BoxLayout.X_AXIS));
    gpaTitlePanel.setOpaque(false);
    gpaTitlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel gpaIcon = new JLabel("📈 ");
    gpaIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

    JLabel gpaTitle = new JLabel("GPA Tích lũy");
    gpaTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
    gpaTitle.setForeground(Color.DARK_GRAY);

    gpaTitlePanel.add(Box.createHorizontalGlue());
    gpaTitlePanel.add(gpaIcon);
    gpaTitlePanel.add(gpaTitle);
    gpaTitlePanel.add(Box.createHorizontalGlue());

    gpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
    gpaLabel.setForeground(new Color(46, 204, 113));
    gpaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    gpaPanel.add(gpaTitlePanel);
    gpaPanel.add(Box.createVerticalStrut(8));
    gpaPanel.add(gpaLabel);

    JPanel creditsPanel = new JPanel();
    creditsPanel.setLayout(new BoxLayout(creditsPanel, BoxLayout.Y_AXIS));
    creditsPanel.setBackground(new Color(52, 152, 219, 18));
    creditsPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
        new EmptyBorder(10, 15, 10, 15)));

    // Credits Title with icon
    JPanel creditsTitlePanel = new JPanel();
    creditsTitlePanel.setLayout(new BoxLayout(creditsTitlePanel, BoxLayout.X_AXIS));
    creditsTitlePanel.setOpaque(false);
    creditsTitlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel creditsIcon = new JLabel("📚 ");
    creditsIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

    JLabel creditsTitle = new JLabel("Tín chỉ Tích lũy");
    creditsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
    creditsTitle.setForeground(Color.DARK_GRAY);

    creditsTitlePanel.add(Box.createHorizontalGlue());
    creditsTitlePanel.add(creditsIcon);
    creditsTitlePanel.add(creditsTitle);
    creditsTitlePanel.add(Box.createHorizontalGlue());

    creditsLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
    creditsLabel.setForeground(new Color(52, 152, 219));
    creditsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    creditsPanel.add(creditsTitlePanel);
    creditsPanel.add(Box.createVerticalStrut(8));
    creditsPanel.add(creditsLabel);

    JPanel statsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
    statsGrid.setOpaque(false);
    statsGrid.add(gpaPanel);
    statsGrid.add(creditsPanel);

    statsCard.add(statsGrid);

    rightPanel.add(statsCard);

    // Add panels to wrapper
    mainWrapper.add(leftPanel, BorderLayout.WEST);
    mainWrapper.add(rightPanel, BorderLayout.CENTER);

    // Add wrapper to main layout
    add(mainWrapper, BorderLayout.CENTER);
  }

  private JPanel createCardWithIcon(String icon, String title, Color accentColor) {
    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
        new EmptyBorder(0, 0, 0, 0),
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(16, 18, 16, 18))));

    // Title row with icon
    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
    titlePanel.setOpaque(false);
    titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel iconLabel = new JLabel(icon);
    iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
    iconLabel.setForeground(accentColor);

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    titleLabel.setForeground(accentColor);

    titlePanel.add(iconLabel);
    titlePanel.add(Box.createHorizontalStrut(8));
    titlePanel.add(titleLabel);

    card.add(titlePanel);
    card.add(Box.createVerticalStrut(12));

    return card;
  }

  private JPanel createInfoRow(String label, JLabel valueLabel) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
    panel.setOpaque(false);
    panel.setMaximumSize(new Dimension(300, 30));

    JLabel labelComp = new JLabel(label);
    labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    labelComp.setForeground(Color.GRAY);

    valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

    panel.add(labelComp);
    panel.add(valueLabel);

    return panel;
  }

  private JPanel createDetailRowWithIcon(String icon, String label, JLabel valueLabel) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setOpaque(false);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Icon
    JLabel iconLabel = new JLabel(icon);
    iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
    iconLabel.setPreferredSize(new Dimension(25, 25));

    // Label
    JLabel labelComp = new JLabel(label);
    labelComp.setFont(new Font("Segoe UI", Font.BOLD, 13));
    labelComp.setForeground(Color.DARK_GRAY);
    labelComp.setPreferredSize(new Dimension(100, 25));

    // Value
    valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    panel.add(iconLabel);
    panel.add(Box.createHorizontalStrut(8));
    panel.add(labelComp);
    panel.add(Box.createHorizontalStrut(10));
    panel.add(valueLabel);
    panel.add(Box.createHorizontalGlue());

    return panel;
  }

  private void loadStudentData() {
    SwingWorker<Student, Void> worker = new SwingWorker<>() {
      @Override
      protected Student doInBackground() throws Exception {
        try {
          Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
          // Student tự lấy thông tin của mình, không cần gửi studentId

          Message response = serverConnection.sendRequest(request);

          if (response != null && response.isSuccess()) {
            return response.getData(Constants.KEY_STUDENT, Student.class);
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Lỗi khi tải thông tin sinh viên", e);
        }
        return null;
      }

      @Override
      protected void done() {
        try {
          studentInfo = get();
          if (studentInfo != null) {
            updateUIData();
          } else {
            ToastNotification.showError(StudentProfilePanel.this, "Không thể tải thông tin sinh viên");
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Lỗi khi xử lý dữ liệu sinh viên", e);
          ToastNotification.showError(StudentProfilePanel.this, "Lỗi: " + e.getMessage());
        }
      }
    };

    worker.execute();
  }

  public void updateUIData() {
    if (studentInfo == null)
      return;

    nameLabel.setText(studentInfo.getFullName());
    studentIdLabel.setText("Mã SV: " + studentInfo.getStudentCode());
    emailLabel.setText(studentInfo.getEmail() != null ? studentInfo.getEmail() : "Chưa cập nhật");
    phoneLabel.setText(studentInfo.getPhone() != null ? studentInfo.getPhone() : "Chưa cập nhật");
    facultyLabel.setText(studentInfo.getFacultyName() != null ? studentInfo.getFacultyName() : "Chưa cập nhật");
    classLabel.setText(studentInfo.getClassName() != null ? studentInfo.getClassName() : "Chưa cập nhật");

    if (studentInfo.getGpa() != null) {
      gpaLabel.setText(String.format("%.2f", studentInfo.getGpa()));
    }

    creditsLabel.setText(String.valueOf(studentInfo.getTotalCredits()));

    // Update status
    if (studentInfo.isActive()) {
      statusLabel.setText("Đang học");
      statusLabel.setForeground(new Color(46, 204, 113));
    } else {
      statusLabel.setText("Đã nghỉ học");
      statusLabel.setForeground(Color.RED);
    }
  }

  private void editProfile() {
    if (studentInfo == null) {
      ToastNotification.showError(this, "Không có thông tin sinh viên để chỉnh sửa");
      return;
    }

    // Get parent frame
    Window window = SwingUtilities.getWindowAncestor(this);
    Frame parentFrame = (window instanceof Frame) ? (Frame) window : null;

    // Open edit dialog - student can edit their own info
    StudentDetailDialog dialog = new StudentDetailDialog(
        parentFrame,
        studentInfo,
        serverConnection,
        currentUser,
        false // Allow editing for student's own profile
    );
    dialog.setVisible(true);

    // Refresh data if changed
    if (dialog.isDataChanged()) {
      refreshData();
      ToastNotification.showSuccess(this, "Cập nhật thông tin thành công!");
    }
  }

  private void changePassword() {
    // Get parent frame
    Window window = SwingUtilities.getWindowAncestor(this);
    Frame parentFrame = (window instanceof Frame) ? (Frame) window : null;

    // Open change password dialog
    ChangePasswordDialog dialog = new ChangePasswordDialog(parentFrame, serverConnection);
    dialog.setVisible(true);
  }

  public void refreshData() {
    loadStudentData();
  }
}
