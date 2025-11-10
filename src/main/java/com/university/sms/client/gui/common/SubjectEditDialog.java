package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Subject;
import com.university.sms.model.Faculty;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog thêm/sửa môn học
 */
public class SubjectEditDialog extends JDialog {
  private static final long serialVersionUID = 1L;

  private IServerConnection serverConnection;
  private Subject subject; // null = add new, not null = edit
  private boolean isSaved = false;

  // Form fields
  private JTextField codeField;
  private JTextField nameField;
  private JSpinner creditsSpinner;
  private JComboBox<FacultyItem> facultyCombo;
  private JComboBox<SubjectItem> prerequisiteCombo;
  private JCheckBox requiredCheckbox;
  private JTextArea descriptionArea;

  private JButton saveButton;
  private JButton cancelButton;

  public SubjectEditDialog(Frame owner, IServerConnection serverConnection, Subject subject) {
    super(owner, subject == null ? "Thêm môn học" : "Sửa môn học", true);
    this.serverConnection = serverConnection;
    this.subject = subject;

    initializeComponents();
    setupLayout();
    loadFaculties();
    loadSubjects();

    if (subject != null) {
      populateFields();
    }

    pack();
    setLocationRelativeTo(owner);
  }

  private void initializeComponents() {
    codeField = new JTextField(20);
    nameField = new JTextField(30);

    creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));

    facultyCombo = new JComboBox<>();
    prerequisiteCombo = new JComboBox<>();
    prerequisiteCombo.addItem(new SubjectItem(null, "-- Không có --"));

    requiredCheckbox = new JCheckBox("Môn bắt buộc");
    requiredCheckbox.setSelected(true);

    descriptionArea = new JTextArea(5, 30);
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);

    saveButton = new JButton("Lưu");
    cancelButton = new JButton("Hủy");

    saveButton.addActionListener(e -> saveSubject());
    cancelButton.addActionListener(e -> dispose());

    // Disable code field when editing
    if (subject != null) {
      codeField.setEnabled(false);
    }
  }

  private void setupLayout() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Form panel
    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    // Row 0: Subject Code
    gbc.gridx = 0;
    gbc.gridy = 0;
    formPanel.add(new JLabel("Mã môn học:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    formPanel.add(codeField, gbc);

    // Row 1: Subject Name
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Tên môn học:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    formPanel.add(nameField, gbc);

    // Row 2: Credits
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Số tín chỉ:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    formPanel.add(creditsSpinner, gbc);

    // Row 3: Faculty
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Khoa:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    formPanel.add(facultyCombo, gbc);

    // Row 4: Prerequisite
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Môn tiên quyết:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    formPanel.add(prerequisiteCombo, gbc);

    // Row 5: Required checkbox
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    formPanel.add(requiredCheckbox, gbc);

    // Row 6: Description
    gbc.gridx = 0;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    formPanel.add(new JLabel("Mô tả:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    formPanel.add(new JScrollPane(descriptionArea), gbc);

    mainPanel.add(formPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(saveButton);
    buttonPanel.add(cancelButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);
  }

  private void loadFaculties() {
    SwingWorker<List<Faculty>, Void> worker = new SwingWorker<>() {
      @Override
      protected List<Faculty> doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
        Message response = serverConnection.sendRequest(request);
        if (response != null && response.isSuccess()) {
          @SuppressWarnings("unchecked")
          List<Faculty> faculties = (List<Faculty>) response.getData("faculties");
          return faculties;
        }
        return null;
      }

      @Override
      protected void done() {
        try {
          List<Faculty> faculties = get();
          if (faculties != null) {
            for (Faculty faculty : faculties) {
              facultyCombo.addItem(new FacultyItem(faculty.getFacultyCode(), faculty.getFacultyName()));
            }
            // Set selection if editing
            if (subject != null) {
              for (int i = 0; i < facultyCombo.getItemCount(); i++) {
                FacultyItem item = facultyCombo.getItemAt(i);
                if (item.code != null && item.code.equals(subject.getFacultyCode())) {
                  facultyCombo.setSelectedIndex(i);
                  break;
                }
              }
            }
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(SubjectEditDialog.this,
              "Lỗi khi tải danh sách khoa: " + e.getMessage(),
              "Lỗi",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    worker.execute();
  }

  private void loadSubjects() {
    SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
      @Override
      protected List<Subject> doInBackground() throws Exception {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
        Message response = serverConnection.sendRequest(request);
        if (response != null && response.isSuccess()) {
          @SuppressWarnings("unchecked")
          List<Subject> subjects = (List<Subject>) response.getData("subjects");
          return subjects;
        }
        return null;
      }

      @Override
      protected void done() {
        try {
          List<Subject> subjects = get();
          if (subjects != null) {
            for (Subject subj : subjects) {
              // Don't include current subject if editing
              if (subject == null
                  || (subj.getSubjectCode() != null && !subj.getSubjectCode().equals(subject.getSubjectCode()))) {
                prerequisiteCombo.addItem(
                    new SubjectItem(subj.getSubjectCode(), subj.getSubjectCode() + " - " + subj.getSubjectName()));
              }
            }
            // Set selection if editing
            if (subject != null && subject.getPrerequisiteSubjectCode() != null) {
              for (int i = 0; i < prerequisiteCombo.getItemCount(); i++) {
                SubjectItem item = prerequisiteCombo.getItemAt(i);
                if (item.code != null && item.code.equals(subject.getPrerequisiteSubjectCode())) {
                  prerequisiteCombo.setSelectedIndex(i);
                  break;
                }
              }
            }
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(SubjectEditDialog.this,
              "Lỗi khi tải danh sách môn học: " + e.getMessage(),
              "Lỗi",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    worker.execute();
  }

  private void populateFields() {
    codeField.setText(subject.getSubjectCode());
    nameField.setText(subject.getSubjectName());
    creditsSpinner.setValue(subject.getCredits());
    requiredCheckbox.setSelected(subject.isRequired());
    if (subject.getDescription() != null) {
      descriptionArea.setText(subject.getDescription());
    }
  }

  private void saveSubject() {
    // Validation
    String code = codeField.getText().trim();
    String name = nameField.getText().trim();

    if (code.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Vui lòng nhập mã môn học", "Lỗi", JOptionPane.ERROR_MESSAGE);
      codeField.requestFocus();
      return;
    }

    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Vui lòng nhập tên môn học", "Lỗi", JOptionPane.ERROR_MESSAGE);
      nameField.requestFocus();
      return;
    }

    if (facultyCombo.getSelectedItem() == null) {
      JOptionPane.showMessageDialog(this, "Vui lòng chọn khoa", "Lỗi", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Create or update subject
    Subject newSubject = subject == null ? new Subject() : subject;
    newSubject.setSubjectCode(code);
    newSubject.setSubjectName(name);
    newSubject.setCredits((Integer) creditsSpinner.getValue());
    newSubject.setFacultyCode(((FacultyItem) facultyCombo.getSelectedItem()).code);

    SubjectItem prereqItem = (SubjectItem) prerequisiteCombo.getSelectedItem();
    newSubject.setPrerequisiteSubjectCode(prereqItem != null && prereqItem.code != null ? prereqItem.code : null);

    newSubject.setRequired(requiredCheckbox.isSelected());
    newSubject.setDescription(descriptionArea.getText().trim());

    // Send to server
    SwingWorker<Message, Void> worker = new SwingWorker<>() {
      @Override
      protected Message doInBackground() throws Exception {
        String action = subject == null ? Constants.ACTION_ADD_SUBJECT : Constants.ACTION_UPDATE_SUBJECT;
        Message request = Message.createRequest(action);
        request.addData("subject", newSubject);
        return serverConnection.sendRequest(request);
      }

      @Override
      protected void done() {
        try {
          Message response = get();
          if (response != null && response.isSuccess()) {
            isSaved = true;
            JOptionPane.showMessageDialog(SubjectEditDialog.this,
                response.getMessage(),
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
          } else {
            String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
            JOptionPane.showMessageDialog(SubjectEditDialog.this,
                errorMsg,
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(SubjectEditDialog.this,
              "Lỗi: " + e.getMessage(),
              "Lỗi",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    worker.execute();
  }

  public boolean isSaved() {
    return isSaved;
  }

  // Helper classes for ComboBox items
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

  private static class SubjectItem {
    String code;
    String name;

    SubjectItem(String code, String name) {
      this.code = code;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
