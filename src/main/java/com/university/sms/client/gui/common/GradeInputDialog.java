package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Grade;
import com.university.sms.model.Grade.GradeType;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Dialog nhập/chỉnh sửa điểm
 * Dùng cho Teacher để nhập điểm chi tiết
 */
public class GradeInputDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private IServerConnection serverConnection;
    private Grade grade;
    private boolean isEditMode;
    private boolean success = false;

    // Components
    private JComboBox<GradeType> gradeTypeCombo;
    private JTextField gradeNameField;
    private JTextField scoreField;
    private JTextField maxScoreField;
    private JTextField weightField;
    private JTextField gradeDateField;
    private JTextArea notesArea;
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * Constructor for adding new grade
     */
    public GradeInputDialog(Frame parent, IServerConnection serverConnection, int enrollmentId) {
        super(parent, "Nhập điểm mới", true);
        this.serverConnection = serverConnection;
        this.grade = new Grade();
        this.grade.setEnrollmentId(enrollmentId);
        this.isEditMode = false;
        
        initComponents();
        setupLayout();
        setupListeners();
        
        setSize(500, 450);
        setLocationRelativeTo(parent);
    }

    /**
     * Constructor for editing existing grade
     */
    public GradeInputDialog(Frame parent, IServerConnection serverConnection, Grade grade) {
        super(parent, "Chỉnh sửa điểm", true);
        this.serverConnection = serverConnection;
        this.grade = grade;
        this.isEditMode = true;
        
        initComponents();
        setupLayout();
        setupListeners();
        populateFields();
        
        setSize(500, 450);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        // Grade Type
        gradeTypeCombo = new JComboBox<>(GradeType.values());
        gradeTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GradeType) {
                    setText(((GradeType) value).getDisplayName());
                }
                return this;
            }
        });

        // Grade Name
        gradeNameField = new JTextField(20);
        gradeNameField.setToolTipText("Ví dụ: Bài tập tuần 1, Kiểm tra giữa kỳ, Thi cuối kỳ...");

        // Score
        scoreField = new JTextField(10);
        scoreField.setToolTipText("Điểm đạt được (ví dụ: 8.5)");

        // Max Score
        maxScoreField = new JTextField(10);
        maxScoreField.setText("10.0");
        maxScoreField.setToolTipText("Điểm tối đa (mặc định: 10.0)");

        // Weight
        weightField = new JTextField(10);
        weightField.setText("1.0");
        weightField.setToolTipText("Trọng số (0.0 - 1.0, ví dụ: 0.1 = 10%, 0.3 = 30%)");

        // Grade Date
        gradeDateField = new JTextField(10);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        gradeDateField.setText(sdf.format(new java.util.Date()));
        gradeDateField.setToolTipText("Ngày nhập điểm (YYYY-MM-DD)");

        // Notes
        notesArea = new JTextArea(5, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setToolTipText("Ghi chú (tùy chọn)");

        // Buttons
        saveButton = new JButton(isEditMode ? "Cập nhật" : "Lưu");
        saveButton.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        
        cancelButton = new JButton("Hủy");
        cancelButton.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Main panel with form
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Grade Type
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Loại điểm: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(gradeTypeCombo, gbc);
        row++;

        // Grade Name
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Tên bài: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(gradeNameField, gbc);
        row++;

        // Score
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Điểm đạt: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(scoreField, gbc);
        row++;

        // Max Score
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Điểm tối đa: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(maxScoreField, gbc);
        row++;

        // Weight
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Trọng số: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(weightField, gbc);
        row++;

        // Grade Date
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Ngày: *"), gbc);
        gbc.gridx = 1;
        mainPanel.add(gradeDateField, gbc);
        row++;

        // Notes
        gbc.gridx = 0; gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(new JLabel("Ghi chú:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JScrollPane notesScroll = new JScrollPane(notesArea);
        mainPanel.add(notesScroll, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Info label
        JLabel infoLabel = new JLabel("* = Bắt buộc");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        add(infoLabel, BorderLayout.NORTH);
    }

    private void setupListeners() {
        saveButton.addActionListener(e -> saveGrade());
        cancelButton.addActionListener(e -> dispose());

        // Enter key to save
        getRootPane().setDefaultButton(saveButton);
    }

    private void populateFields() {
        if (grade != null) {
            gradeTypeCombo.setSelectedItem(grade.getGradeType());
            gradeNameField.setText(grade.getGradeName());
            
            if (grade.getScore() != null) {
                scoreField.setText(grade.getScore().toString());
            }
            
            if (grade.getMaxScore() != null) {
                maxScoreField.setText(grade.getMaxScore().toString());
            }
            
            if (grade.getWeight() != null) {
                weightField.setText(grade.getWeight().toString());
            }
            
            if (grade.getGradeDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                gradeDateField.setText(sdf.format(grade.getGradeDate()));
            }
            
            if (grade.getNotes() != null) {
                notesArea.setText(grade.getNotes());
            }
        }
    }

    private void saveGrade() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Build grade object
            grade.setGradeType((GradeType) gradeTypeCombo.getSelectedItem());
            grade.setGradeName(gradeNameField.getText().trim());
            grade.setScore(new BigDecimal(scoreField.getText().trim()));
            grade.setMaxScore(new BigDecimal(maxScoreField.getText().trim()));
            grade.setWeight(new BigDecimal(weightField.getText().trim()));
            
            // Parse date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date utilDate = sdf.parse(gradeDateField.getText().trim());
            grade.setGradeDate(new Date(utilDate.getTime()));
            
            grade.setNotes(notesArea.getText().trim());

            // Send to server
            String action = isEditMode ? Constants.ACTION_UPDATE_GRADE : Constants.ACTION_ADD_GRADE;
            Message request = Message.createRequest(action);
            request.addData(Constants.KEY_GRADE, grade);

            Message response = serverConnection.sendRequest(request);

            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        isEditMode ? "Cập nhật điểm thành công!" : "Thêm điểm thành công!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                success = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Lỗi: " + response.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi lưu điểm: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInput() {
        // Grade Name
        if (gradeNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tên bài không được để trống!",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            gradeNameField.requestFocus();
            return false;
        }

        // Score
        try {
            BigDecimal score = new BigDecimal(scoreField.getText().trim());
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Điểm không được âm");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Điểm đạt không hợp lệ! Vui lòng nhập số (ví dụ: 8.5)",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            scoreField.requestFocus();
            return false;
        }

        // Max Score
        try {
            BigDecimal maxScore = new BigDecimal(maxScoreField.getText().trim());
            if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Điểm tối đa phải > 0");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Điểm tối đa không hợp lệ! Vui lòng nhập số > 0",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            maxScoreField.requestFocus();
            return false;
        }

        // Score <= Max Score
        try {
            BigDecimal score = new BigDecimal(scoreField.getText().trim());
            BigDecimal maxScore = new BigDecimal(maxScoreField.getText().trim());
            if (score.compareTo(maxScore) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Điểm đạt không được vượt quá điểm tối đa!",
                        "Lỗi nhập liệu",
                        JOptionPane.WARNING_MESSAGE);
                scoreField.requestFocus();
                return false;
            }
        } catch (Exception e) {
            // Already handled above
        }

        // Weight
        try {
            BigDecimal weight = new BigDecimal(weightField.getText().trim());
            if (weight.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Trọng số phải trong khoảng (0.0, 1.0]");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Trọng số không hợp lệ! Vui lòng nhập số từ 0.0 đến 1.0 (ví dụ: 0.3 = 30%)",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            weightField.requestFocus();
            return false;
        }

        // Date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(gradeDateField.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ngày không hợp lệ! Vui lòng nhập theo định dạng YYYY-MM-DD (ví dụ: 2024-11-04)",
                    "Lỗi nhập liệu",
                    JOptionPane.WARNING_MESSAGE);
            gradeDateField.requestFocus();
            return false;
        }

        return true;
    }

    public boolean isSuccess() {
        return success;
    }
}

