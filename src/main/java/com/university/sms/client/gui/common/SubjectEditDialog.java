package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Subject;
import com.university.sms.model.Faculty;

import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collections;
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
  private JButton prerequisiteListButton;
  private JCheckBox requiredCheckbox;
  private JTextArea descriptionArea;
  private List<Subject> allSubjects = Collections.emptyList();
  private volatile boolean subjectsLoaded = false;
  private volatile boolean facultiesLoaded = false;

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
    prerequisiteListButton = new JButton("Danh sách...");
    prerequisiteListButton.addActionListener(e -> showPrerequisiteSelectionDialog());

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
    GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.insets = new Insets(5, 5, 5, 5);
    labelConstraints.anchor = GridBagConstraints.WEST;
    labelConstraints.gridx = 0;

    GridBagConstraints fieldConstraints = new GridBagConstraints();
    fieldConstraints.insets = new Insets(5, 5, 5, 5);
    fieldConstraints.gridx = 1;
    fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
    fieldConstraints.weightx = 1.0;

    addFormRow(formPanel, 0, "Mã môn học:", codeField, labelConstraints, fieldConstraints);
    addFormRow(formPanel, 1, "Tên môn học:", nameField, labelConstraints, fieldConstraints);
    addFormRow(formPanel, 2, "Số tín chỉ:", creditsSpinner, labelConstraints, fieldConstraints);
    addFormRow(formPanel, 3, "Khoa:", facultyCombo, labelConstraints, fieldConstraints);

    JPanel prerequisiteFieldPanel = new JPanel(new BorderLayout(5, 0));
    prerequisiteFieldPanel.add(prerequisiteCombo, BorderLayout.CENTER);
    prerequisiteFieldPanel.add(prerequisiteListButton, BorderLayout.EAST);
    addFormRow(formPanel, 4, "Môn tiên quyết:", prerequisiteFieldPanel, labelConstraints, fieldConstraints);

    GridBagConstraints checkboxConstraints = (GridBagConstraints) fieldConstraints.clone();
    checkboxConstraints.gridy = 5;
    checkboxConstraints.gridx = 1;
    formPanel.add(requiredCheckbox, checkboxConstraints);

    GridBagConstraints descriptionLabelConstraints = (GridBagConstraints) labelConstraints.clone();
    descriptionLabelConstraints.gridy = 6;
    descriptionLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
    formPanel.add(new JLabel("Mô tả:"), descriptionLabelConstraints);

    GridBagConstraints descriptionFieldConstraints = (GridBagConstraints) fieldConstraints.clone();
    descriptionFieldConstraints.gridy = 6;
    descriptionFieldConstraints.fill = GridBagConstraints.BOTH;
    descriptionFieldConstraints.weighty = 1.0;
    formPanel.add(new JScrollPane(descriptionArea), descriptionFieldConstraints);

    mainPanel.add(formPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(saveButton);
    buttonPanel.add(cancelButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);

    addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentShown(java.awt.event.ComponentEvent e) {
        if (!facultiesLoaded) {
          loadFaculties();
        }
        if (!subjectsLoaded) {
          loadSubjects();
        }
      }
    });
  }

  private void addFormRow(JPanel panel, int row, String labelText, JComponent field,
      GridBagConstraints labelTemplate, GridBagConstraints fieldTemplate) {
    GridBagConstraints labelConstraints = (GridBagConstraints) labelTemplate.clone();
    labelConstraints.gridy = row;
    panel.add(new JLabel(labelText), labelConstraints);

    GridBagConstraints fieldConstraints = (GridBagConstraints) fieldTemplate.clone();
    fieldConstraints.gridy = row;
    panel.add(field, fieldConstraints);
  }

  private synchronized void loadFaculties() {
    if (facultiesLoaded) {
      return;
    }

    try {
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_FACULTIES);
      Message response = serverConnection.sendRequest(request);

      if (response != null && response.isSuccess()) {
        @SuppressWarnings("unchecked")
        List<Faculty> faculties = (List<Faculty>) response.getData("faculties");

        if (faculties != null) {
          for (Faculty faculty : faculties) {
            facultyCombo.addItem(new FacultyItem(faculty.getFacultyCode(), faculty.getFacultyName()));
          }
          facultiesLoaded = true;

          if (subject != null) {
            for (int i = 0; i < facultyCombo.getItemCount(); i++) {
              FacultyItem item = facultyCombo.getItemAt(i);
              if (item.code != null && item.code.equals(subject.getFacultyCode())) {
                facultyCombo.setSelectedIndex(i);
                break;
              }
            }
          } else if (facultyCombo.getItemCount() > 0) {
            facultyCombo.setSelectedIndex(0);
          }
        }
      } else {
        showServerError("khoa", response);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Lỗi khi tải danh sách khoa: " + e.getMessage(),
          "Lỗi",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private synchronized void loadSubjects() {
    if (subjectsLoaded) {
      return;
    }

    try {
      Message request = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
      Message response = serverConnection.sendRequest(request);

      if (response != null && response.isSuccess()) {
        @SuppressWarnings("unchecked")
        List<Subject> subjects = (List<Subject>) response.getData(Constants.KEY_SUBJECTS);

        if (subjects != null && !subjects.isEmpty()) {
          allSubjects = subjects;
          subjects.sort((s1, s2) -> {
            String code1 = s1.getSubjectCode() != null ? s1.getSubjectCode() : "";
            String code2 = s2.getSubjectCode() != null ? s2.getSubjectCode() : "";
            return code1.compareToIgnoreCase(code2);
          });

          for (Subject subj : subjects) {
            if (subject == null
                || (subj.getSubjectCode() != null && !subj.getSubjectCode().equals(subject.getSubjectCode()))) {
              String displayText = subj.getSubjectCode() + " - " + subj.getSubjectName();
              prerequisiteCombo.addItem(new SubjectItem(subj.getSubjectCode(), displayText));
            }
          }
        } else {
          allSubjects = Collections.emptyList();
        }

        updatePrerequisiteSelection();
        subjectsLoaded = true;
      } else {
        showServerError("môn học", response);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Lỗi khi tải danh sách môn học: " + e.getMessage(),
          "Lỗi",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void populateFields() {
    codeField.setText(subject.getSubjectCode());
    nameField.setText(subject.getSubjectName());
    creditsSpinner.setValue(subject.getCredits());
    requiredCheckbox.setSelected(subject.isRequired());
    if (subject.getDescription() != null) {
      descriptionArea.setText(subject.getDescription());
    }

    updatePrerequisiteSelection();
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

  private void showPrerequisiteSelectionDialog() {
    if (!subjectsLoaded) {
      loadSubjects();
    }

    if (allSubjects == null || allSubjects.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "Không tìm thấy môn học nào khác để chọn làm môn tiên quyết.",
          "Thông báo",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    JDialog dialog = new JDialog(this, "Chọn môn tiên quyết", true);
    dialog.setSize(700, 450);
    dialog.setLocationRelativeTo(this);

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JTextField searchField = new JTextField();
    panel.add(searchField, BorderLayout.NORTH);

    String[] columns = { "Mã môn", "Tên môn học", "Tín chỉ", "Khoa" };
    DefaultTableModel model = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    for (Subject subj : allSubjects) {
      if (subj == null || subj.getSubjectCode() == null) {
        continue;
      }
      if (subject != null && subj.getSubjectCode().equals(subject.getSubjectCode())) {
        continue;
      }
      model.addRow(new Object[] {
          subj.getSubjectCode(),
          subj.getSubjectName(),
          subj.getCredits(),
          subj.getFacultyName() != null ? subj.getFacultyName() : subj.getFacultyCode()
      });
    }

    JTable table = new JTable(model);
    table.setRowHeight(24);
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    table.setRowSorter(sorter);

    JScrollPane scrollPane = new JScrollPane(table);
    panel.add(scrollPane, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton selectButton = new JButton("Chọn");
    JButton clearButton = new JButton("Không có");
    JButton cancelButton = new JButton("Hủy");

    buttonPanel.add(clearButton);
    buttonPanel.add(selectButton);
    buttonPanel.add(cancelButton);

    panel.add(buttonPanel, BorderLayout.SOUTH);

    searchField.getDocument().addDocumentListener(new DocumentListener() {
      private void filter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
          sorter.setRowFilter(null);
        } else {
          sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text.trim())));
        }
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        filter();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        filter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        filter();
      }
    });

    Runnable selectCurrentRow = () -> {
      int viewRow = table.getSelectedRow();
      if (viewRow < 0) {
        JOptionPane.showMessageDialog(dialog,
            "Vui lòng chọn một môn học.",
            "Thông báo",
            JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      int modelRow = table.convertRowIndexToModel(viewRow);
      String code = (String) model.getValueAt(modelRow, 0);
      String name = (String) model.getValueAt(modelRow, 1);
      setPrerequisiteSelection(code, code + " - " + name);
      dialog.dispose();
    };

    selectButton.addActionListener(e -> selectCurrentRow.run());
    clearButton.addActionListener(e -> {
      prerequisiteCombo.setSelectedIndex(0);
      dialog.dispose();
    });
    cancelButton.addActionListener(e -> dialog.dispose());

    table.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
          selectCurrentRow.run();
        }
      }
    });

    dialog.setContentPane(panel);
    dialog.setVisible(true);
  }

  private void setPrerequisiteSelection(String code, String displayText) {
    if (code == null) {
      prerequisiteCombo.setSelectedIndex(0);
      return;
    }

    for (int i = 0; i < prerequisiteCombo.getItemCount(); i++) {
      SubjectItem item = prerequisiteCombo.getItemAt(i);
      if (item.code != null && item.code.equals(code)) {
        prerequisiteCombo.setSelectedIndex(i);
        return;
      }
    }

    SubjectItem newItem = new SubjectItem(code, displayText);
    prerequisiteCombo.addItem(newItem);
    prerequisiteCombo.setSelectedItem(newItem);
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

  private void updatePrerequisiteSelection() {
    if (subject != null && subject.getPrerequisiteSubjectCode() != null) {
      boolean found = false;
      for (int i = 0; i < prerequisiteCombo.getItemCount(); i++) {
        SubjectItem item = prerequisiteCombo.getItemAt(i);
        if (item.code != null && item.code.equals(subject.getPrerequisiteSubjectCode())) {
          prerequisiteCombo.setSelectedIndex(i);
          found = true;
          break;
        }
      }
      if (!found) {
        prerequisiteCombo.addItem(new SubjectItem(subject.getPrerequisiteSubjectCode(),
            subject.getPrerequisiteSubjectCode() + " - (Đã bị xóa hoặc không tồn tại)"));
        prerequisiteCombo.setSelectedIndex(prerequisiteCombo.getItemCount() - 1);
      }
    } else if (subject == null && prerequisiteCombo.getItemCount() > 0) {
      prerequisiteCombo.setSelectedIndex(0);
    }
  }

  private void showServerError(String entityName, Message response) {
    String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi từ server";
    JOptionPane.showMessageDialog(this,
        "Không thể tải danh sách " + entityName + ": " + errorMsg,
        "Lỗi",
        JOptionPane.ERROR_MESSAGE);
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
