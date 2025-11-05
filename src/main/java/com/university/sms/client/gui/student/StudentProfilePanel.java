package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.client.gui.common.ToastNotification;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel hiển thị thông tin cá nhân của sinh viên đang đăng nhập
 */
public class StudentProfilePanel extends JPanel {
  private static final long serialVersionUID = 1L;

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
    setBackground(new Color(236, 240, 241));

    // Avatar
    avatarLabel = new JLabel("👤", JLabel.CENTER);
    avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
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
    statusLabel = new JLabel("✅ Đang học");
    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

    // Buttons
    editButton = new JButton("✏️ Chỉnh sửa Thông tin");
    editButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
    editButton.setBackground(new Color(52, 152, 219));
    editButton.setForeground(Color.WHITE);
    editButton.setFocusPainted(false);
    editButton.setBorderPainted(false);
    editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    editButton.addActionListener(e -> editProfile());

    changePasswordButton = new JButton("🔒 Đổi Mật khẩu");
    changePasswordButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
    changePasswordButton.setBackground(new Color(46, 204, 113));
    changePasswordButton.setForeground(Color.WHITE);
    changePasswordButton.setFocusPainted(false);
    changePasswordButton.setBorderPainted(false);
    changePasswordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    changePasswordButton.addActionListener(e -> changePassword());
  }

  private void setupLayout() {
    // Left panel - Avatar & Basic Info
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBackground(Color.WHITE);
    leftPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(52, 152, 219), 3),
        new EmptyBorder(30, 25, 30, 25)));
    leftPanel.setPreferredSize(new Dimension(380, 0));

    // Avatar container with background
    JPanel avatarContainer = new JPanel();
    avatarContainer.setLayout(new BoxLayout(avatarContainer, BoxLayout.Y_AXIS));
    avatarContainer.setOpaque(false);
    avatarContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

    JPanel avatarPanel = new JPanel(new BorderLayout());
    avatarPanel.setOpaque(false);
    avatarPanel.add(avatarLabel, BorderLayout.CENTER);
    avatarPanel.setMaximumSize(new Dimension(350, 100));

    avatarContainer.add(avatarPanel);
    avatarContainer.add(Box.createVerticalStrut(15));

    leftPanel.add(avatarContainer);

    // Name
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(nameLabel);
    leftPanel.add(Box.createVerticalStrut(8));

    // Student ID
    studentIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(studentIdLabel);
    leftPanel.add(Box.createVerticalStrut(15));

    // Separator
    JSeparator separator1 = new JSeparator();
    separator1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    separator1.setForeground(new Color(52, 152, 219));
    leftPanel.add(separator1);
    leftPanel.add(Box.createVerticalStrut(15));

    // Status
    JPanel statusPanel = createInfoRow("Trạng thái:", statusLabel);
    leftPanel.add(statusPanel);
    leftPanel.add(Box.createVerticalStrut(30));

    // Separator before buttons
    JSeparator separator2 = new JSeparator();
    separator2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    separator2.setForeground(new Color(52, 152, 219));
    leftPanel.add(separator2);
    leftPanel.add(Box.createVerticalStrut(20));

    // Buttons
    JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 12, 12));
    buttonPanel.setOpaque(false);
    buttonPanel.setMaximumSize(new Dimension(280, 90));
    buttonPanel.add(editButton);
    buttonPanel.add(changePasswordButton);
    buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    leftPanel.add(buttonPanel);
    leftPanel.add(Box.createVerticalStrut(10));

    leftPanel.add(Box.createVerticalGlue());

    // Right panel - Detailed Info
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    rightPanel.setBackground(Color.WHITE);
    rightPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

    // Contact Information Card
    JPanel contactCard = createCardWithIcon("📞", "Thông tin Liên hệ", new Color(52, 152, 219));
    contactCard.add(createDetailRowWithIcon("📧", "Email", emailLabel));
    contactCard.add(Box.createVerticalStrut(10));
    contactCard.add(createDetailRowWithIcon("📱", "Điện thoại", phoneLabel));
    rightPanel.add(contactCard);
    rightPanel.add(Box.createVerticalStrut(20));

    // Academic Information Card
    JPanel academicCard = createCardWithIcon("🎓", "Thông tin Học tập", new Color(155, 89, 182));
    academicCard.add(createDetailRowWithIcon("🏛️", "Khoa", facultyLabel));
    academicCard.add(Box.createVerticalStrut(10));
    academicCard.add(createDetailRowWithIcon("👥", "Lớp", classLabel));
    rightPanel.add(academicCard);
    rightPanel.add(Box.createVerticalStrut(20));

    // Statistics Card
    JPanel statsCard = createCardWithIcon("📊", "Thống kê Học tập", new Color(230, 126, 34));

    JPanel gpaPanel = new JPanel();
    gpaPanel.setLayout(new BoxLayout(gpaPanel, BoxLayout.Y_AXIS));
    gpaPanel.setBackground(new Color(46, 204, 113, 20));
    gpaPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
        new EmptyBorder(20, 15, 20, 15)));

    JLabel gpaTitle = new JLabel("📈 GPA Tích lũy");
    gpaTitle.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
    gpaTitle.setForeground(Color.DARK_GRAY);
    gpaTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

    gpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
    gpaLabel.setForeground(new Color(46, 204, 113));
    gpaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    gpaPanel.add(gpaTitle);
    gpaPanel.add(Box.createVerticalStrut(15));
    gpaPanel.add(gpaLabel);

    JPanel creditsPanel = new JPanel();
    creditsPanel.setLayout(new BoxLayout(creditsPanel, BoxLayout.Y_AXIS));
    creditsPanel.setBackground(new Color(52, 152, 219, 20));
    creditsPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
        new EmptyBorder(20, 15, 20, 15)));

    JLabel creditsTitle = new JLabel("📚 Tín chỉ Tích lũy");
    creditsTitle.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
    creditsTitle.setForeground(Color.DARK_GRAY);
    creditsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

    creditsLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
    creditsLabel.setForeground(new Color(52, 152, 219));
    creditsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    creditsPanel.add(creditsTitle);
    creditsPanel.add(Box.createVerticalStrut(15));
    creditsPanel.add(creditsLabel);

    JPanel statsGrid = new JPanel(new GridLayout(1, 2, 25, 0));
    statsGrid.setOpaque(false);
    statsGrid.add(gpaPanel);
    statsGrid.add(creditsPanel);
    statsCard.add(statsGrid);

    rightPanel.add(statsCard);
    rightPanel.add(Box.createVerticalGlue());

    // Add to main layout
    add(leftPanel, BorderLayout.WEST);
    add(rightPanel, BorderLayout.CENTER);
  }

  private JPanel createCardWithIcon(String icon, String title, Color accentColor) {
    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
        new EmptyBorder(0, 0, 0, 0),
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            new EmptyBorder(15, 15, 15, 15))));

    // Title row with icon
    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
    titlePanel.setOpaque(false);
    titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel iconLabel = new JLabel(icon);
    iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
    iconLabel.setForeground(accentColor);

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    titleLabel.setForeground(accentColor);

    titlePanel.add(iconLabel);
    titlePanel.add(Box.createHorizontalStrut(8));
    titlePanel.add(titleLabel);

    card.add(titlePanel);
    card.add(Box.createVerticalStrut(15));

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
    iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
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
          request.addData(Constants.KEY_STUDENT_ID, currentUser.getUserId());

          Message response = serverConnection.sendRequest(request);

          if (response != null && response.isSuccess()) {
            return response.getData(Constants.KEY_STUDENT, Student.class);
          }
        } catch (Exception e) {
          e.printStackTrace();
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
          e.printStackTrace();
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

    statusLabel.setText(studentInfo.isActive() ? "✅ Đang học" : "⛔ Đã nghỉ học");
    statusLabel.setForeground(studentInfo.isActive() ? new Color(46, 204, 113) : Color.RED);
  }

  private void editProfile() {
    ToastNotification.showInfo(this, "Chức năng chỉnh sửa thông tin đang được phát triển");
  }

  private void changePassword() {
    ToastNotification.showInfo(this, "Chức năng đổi mật khẩu đang được phát triển");
  }

  public void refreshData() {
    loadStudentData();
  }
}
