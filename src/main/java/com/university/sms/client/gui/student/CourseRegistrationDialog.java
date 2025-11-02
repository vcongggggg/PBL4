package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for students to register for courses
 */
public class CourseRegistrationDialog extends JDialog {
    private IServerConnection serverConnection;
    private int studentId;
    private Course course;
    private boolean registered = false;
    
    private JTextArea notesArea;

    public CourseRegistrationDialog(Frame owner, IServerConnection connection, 
                                   int studentId, Course course) {
        super(owner, "Đăng Ký Tín Chỉ", true);
        this.serverConnection = connection;
        this.studentId = studentId;
        this.course = course;
        
        initComponents();
        validateRegistration();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(550, 400);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Course info panel
        JPanel infoPanel = createCourseInfoPanel();
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // Notes panel
        JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
        notesPanel.setBorder(BorderFactory.createTitledBorder("Ghi chú (tùy chọn)"));
        
        notesArea = new JTextArea(4, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        
        notesPanel.add(notesScroll, BorderLayout.CENTER);
        mainPanel.add(notesPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton registerBtn = new JButton("Đăng Ký");
        registerBtn.addActionListener(e -> onRegister());
        
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> onCancel());

        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createCourseInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin Khóa học"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Mã khóa học:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(new JLabel(course.getCourseCode()), gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Môn học:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(new JLabel(course.getSubjectName()), gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Mã môn:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(course.getSubjectCode()), gbc);

        // Row 3
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Số tín chỉ:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(String.valueOf(course.getCredits())), gbc);

        // Row 4
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Giảng viên:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(course.getTeacherName()), gbc);

        // Row 5
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Lịch học:"), gbc);
        gbc.gridx = 1;
        String schedule = course.getScheduleDay() + " - " + course.getScheduleTime();
        panel.add(new JLabel(schedule), gbc);

        // Row 6
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Phòng học:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(course.getRoom()), gbc);

        // Row 7
        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(new JLabel("Sĩ số:"), gbc);
        gbc.gridx = 1;
        String capacity = course.getCurrentStudents() + "/" + course.getMaxStudents();
        JLabel capacityLabel = new JLabel(capacity);
        if (course.getCurrentStudents() >= course.getMaxStudents()) {
            capacityLabel.setForeground(Color.RED);
            capacityLabel.setText(capacity + " (ĐẦY)");
        }
        panel.add(capacityLabel, gbc);

        // Row 8
        gbc.gridx = 0; gbc.gridy = 8;
        panel.add(new JLabel("Năm học - HK:"), gbc);
        gbc.gridx = 1;
        String term = course.getAcademicYear() + " - HK" + course.getSemester();
        panel.add(new JLabel(term), gbc);

        return panel;
    }

    private void validateRegistration() {
        // Validate if student can register
        try {
            Message msg = Message.createRequest(Constants.ACTION_VALIDATE_REGISTRATION);
            msg.addData(Constants.KEY_STUDENT_ID, studentId);
            msg.addData(Constants.KEY_COURSE_ID, course.getCourseId());
            
            Message response = serverConnection.sendRequest(msg);
            
            if (response != null && response.isSuccess()) {
                Boolean valid = (Boolean) response.getData("valid");
                String message = (String) response.getData("message");
                
                if (!valid) {
                    // Show warning but allow user to proceed
                    JOptionPane.showMessageDialog(this,
                        "Cảnh báo: " + message + "\n\nBạn vẫn có thể đăng ký nhưng có thể bị từ chối.",
                        "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onRegister() {
        // Confirm registration
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xác nhận đăng ký môn học:\n" + course.getSubjectName() + " (" + course.getCourseCode() + ")?",
            "Xác nhận",
            JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String notes = notesArea.getText().trim();
            
            Message msg = Message.createRequest(Constants.ACTION_REGISTER_COURSE);
            msg.addData(Constants.KEY_STUDENT_ID, studentId);
            msg.addData(Constants.KEY_COURSE_ID, course.getCourseId());
            msg.addData(Constants.KEY_NOTE, notes);
            
            Message response = serverConnection.sendRequest(msg);
            
            if (response != null && response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                    "Đăng ký thành công!\nYêu cầu của bạn đã được gửi và đang chờ duyệt.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                registered = true;
                dispose();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Không có phản hồi";
                JOptionPane.showMessageDialog(this,
                    "Đăng ký thất bại!\n" + errorMsg,
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

    private void onCancel() {
        registered = false;
        dispose();
    }

    public boolean isRegistered() {
        return registered;
    }
}



