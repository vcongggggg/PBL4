package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;
import com.university.sms.model.User;
import com.university.sms.model.Course;
import com.university.sms.model.Grade;
import com.university.sms.model.Enrollment;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel quản lý điểm số
 * - Admin/Teacher: Có thể nhập và sửa điểm
 * - Student: Chỉ xem điểm của mình
 */
public class GradePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable gradeTable;
    private DefaultTableModel tableModel;
    private JButton addGradeButton;
    private JButton editGradeButton;
    private JButton deleteGradeButton;
    private JButton refreshButton;
    private JTextField searchField;
    private JComboBox<String> courseFilterCombo;

    private List<Course> courses;

    public GradePanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        loadInitialData();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        String[] columnNames;
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            columnNames = new String[]{"Mã môn học", "Tên môn học", "Tín chỉ", "Điểm GK", "Điểm CK", "Điểm TK", "Xếp loại"};
        } else {
            columnNames = new String[]{"MSSV", "Tên SV", "Mã môn", "Tên môn", "Điểm GK", "Điểm CK", "Điểm TK", "Xếp loại"};
        }

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        gradeTable = new JTable(tableModel);
        gradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gradeTable.setRowHeight(25);
        gradeTable.getTableHeader().setReorderingAllowed(false);

        // Double click to edit (if not read-only)
        if (!isReadOnly) {
            gradeTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        editGrade();
                    }
                }
            });
        }

        // Buttons
        addGradeButton = new JButton("Nhập điểm");
        editGradeButton = new JButton("Sửa điểm");
        deleteGradeButton = new JButton("Xóa");
        refreshButton = new JButton("Làm mới");

        // Search
        searchField = new JTextField(20);
        searchField.setToolTipText("Tìm kiếm theo MSSV hoặc tên sinh viên");

        // Course filter
        courseFilterCombo = new JComboBox<>();
        courseFilterCombo.addItem("Tất cả môn học");

        // Disable add/edit/delete for read-only mode
        if (isReadOnly) {
            addGradeButton.setEnabled(false);
            editGradeButton.setEnabled(false);
            deleteGradeButton.setEnabled(false);
        }

        // Event listeners
        addGradeButton.addActionListener(e -> addGrade());
        editGradeButton.addActionListener(e -> editGrade());
        deleteGradeButton.addActionListener(e -> deleteGrade());
        refreshButton.addActionListener(e -> refreshData());
        searchField.addActionListener(e -> searchGrades());
        courseFilterCombo.addActionListener(e -> filterByCourse());
    }

    private void setupLayout() {
        // Top panel - Search and filter
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Tìm kiếm:"));
        topPanel.add(searchField);
        topPanel.add(new JLabel("Môn học:"));
        topPanel.add(courseFilterCombo);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        // Center - Table
        JScrollPane scrollPane = new JScrollPane(gradeTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom - Buttons
        if (!isReadOnly || currentUser.getRole() == User.UserRole.STUDENT) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            if (!isReadOnly) {
                buttonPanel.add(addGradeButton);
                buttonPanel.add(editGradeButton);
                buttonPanel.add(deleteGradeButton);
            }
            
            // Statistics for student
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                JButton statsButton = new JButton("Thống kê điểm");
                statsButton.addActionListener(e -> showGradeStatistics());
                buttonPanel.add(statsButton);
            }

            add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private void loadInitialData() {
        refreshData();
        loadCourses();
    }

    public void refreshData() {
        tableModel.setRowCount(0);

        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                List<Map<String, Object>> gradeList = new ArrayList<>();
                
                if (currentUser.getRole() == User.UserRole.STUDENT) {
                    // Student: Get own grades
                    Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_GRADES);
                    Message response = serverConnection.sendRequest(request);
                    
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Enrollment> enrollments = (List<Enrollment>) response.getData(Constants.KEY_GRADES);
                        
                        if (enrollments != null) {
                            // Get detailed grades for each enrollment
                            for (Enrollment enrollment : enrollments) {
                                Map<String, Object> gradeMap = new HashMap<>();
                                
                                // Get course info from enrollment
                                gradeMap.put("courseCode", enrollment.getCourseCode());
                                gradeMap.put("courseName", enrollment.getSubjectName());
                                gradeMap.put("credits", enrollment.getCredits());
                                
                                // Get grade details
                                Message gradeRequest = Message.createRequest(Constants.ACTION_GET_GRADES);
                                gradeRequest.addData(Constants.KEY_ENROLLMENT, enrollment.getEnrollmentId());
                                Message gradeResponse = serverConnection.sendRequest(gradeRequest);
                                
                                BigDecimal midtermGrade = null;
                                BigDecimal finalGrade = null;
                                BigDecimal totalGrade = enrollment.getFinalGrade();
                                
                                if (gradeResponse != null && gradeResponse.isSuccess()) {
                                    @SuppressWarnings("unchecked")
                                    List<Grade> grades = (List<Grade>) gradeResponse.getData(Constants.KEY_GRADES);
                                    
                                    if (grades != null) {
                                        for (Grade grade : grades) {
                                            if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                                                midtermGrade = grade.getScore();
                                            } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                                                finalGrade = grade.getScore();
                                            }
                                        }
                                    }
                                }
                                
                                gradeMap.put("midtermGrade", midtermGrade);
                                gradeMap.put("finalGrade", finalGrade);
                                gradeMap.put("totalGrade", totalGrade);
                                gradeMap.put("classification", enrollment.getLetterGrade() != null ? 
                                    enrollment.getLetterGrade() : calculateLetterGrade(totalGrade));
                                
                                gradeList.add(gradeMap);
                            }
                        }
                    }
                } else {
                    // Admin/Teacher: Get grades by course filter
                    int selectedIndex = courseFilterCombo.getSelectedIndex();
                    if (selectedIndex > 0 && courses != null && selectedIndex <= courses.size()) {
                        Course selectedCourse = courses.get(selectedIndex - 1);
                        
                        Message request = Message.createRequest(Constants.ACTION_GET_GRADES);
                        request.addData(Constants.KEY_COURSE_ID, selectedCourse.getCourseId());
                        Message response = serverConnection.sendRequest(request);
                        
                        if (response != null && response.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            List<Grade> grades = (List<Grade>) response.getData(Constants.KEY_GRADES);
                            
                            if (grades != null) {
                                // Group by enrollment/student
                                Map<Integer, List<Grade>> gradesByEnrollment = grades.stream()
                                    .collect(Collectors.groupingBy(Grade::getEnrollmentId));
                                
                                for (Map.Entry<Integer, List<Grade>> entry : gradesByEnrollment.entrySet()) {
                                    List<Grade> studentGrades = entry.getValue();
                                    if (!studentGrades.isEmpty()) {
                                        Grade firstGrade = studentGrades.get(0);
                                        
                                        Map<String, Object> gradeMap = new HashMap<>();
                                        gradeMap.put("studentId", firstGrade.getStudentCode());
                                        gradeMap.put("studentName", firstGrade.getStudentName());
                                        gradeMap.put("courseCode", firstGrade.getCourseCode());
                                        gradeMap.put("courseName", firstGrade.getSubjectName());
                                        
                                        BigDecimal midtermGrade = null;
                                        BigDecimal finalGrade = null;
                                        
                                        for (Grade grade : studentGrades) {
                                            if (grade.getGradeType() == Grade.GradeType.MIDTERM) {
                                                midtermGrade = grade.getScore();
                                            } else if (grade.getGradeType() == Grade.GradeType.FINAL) {
                                                finalGrade = grade.getScore();
                                            }
                                        }
                                        
                                        gradeMap.put("midtermGrade", midtermGrade);
                                        gradeMap.put("finalGrade", finalGrade);
                                        gradeMap.put("totalGrade", finalGrade != null ? finalGrade : midtermGrade);
                                        gradeMap.put("classification", calculateLetterGrade(
                                            finalGrade != null ? finalGrade : midtermGrade));
                                        
                                        gradeList.add(gradeMap);
                                    }
                                }
                            }
                        }
                    }
                }
                
                return gradeList;
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> grades = get();
                    updateTable(grades);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(GradePanel.this,
                            "Lỗi khi tải dữ liệu điểm: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
    
    private String calculateLetterGrade(BigDecimal grade) {
        if (grade == null) return "N/A";
        
        double score = grade.doubleValue();
        if (score >= 9.0) return "A+";
        if (score >= 8.5) return "A";
        if (score >= 8.0) return "B+";
        if (score >= 7.0) return "B";
        if (score >= 6.5) return "C+";
        if (score >= 6.0) return "C";
        if (score >= 5.5) return "D+";
        if (score >= 5.0) return "D";
        if (score >= 4.0) return "F+";
        return "F";
    }

    private void loadCourses() {
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                Message response = serverConnection.getAllCourses();
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Course> courseList = (List<Course>) response.getData(Constants.KEY_COURSES);
                    return courseList;
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    courses = get();
                    courseFilterCombo.removeAllItems();
                    courseFilterCombo.addItem("Tất cả môn học");
                    for (Course course : courses) {
                        courseFilterCombo.addItem(course.getCourseCode() + " - " + course.getSubjectName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void updateTable(List<Map<String, Object>> grades) {
        tableModel.setRowCount(0);

        for (Map<String, Object> grade : grades) {
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                tableModel.addRow(new Object[]{
                        grade.get("courseCode"),
                        grade.get("courseName"),
                        grade.get("credits"),
                        grade.get("midtermGrade"),
                        grade.get("finalGrade"),
                        grade.get("totalGrade"),
                        grade.get("classification")
                });
            } else {
                tableModel.addRow(new Object[]{
                        grade.get("studentId"),
                        grade.get("studentName"),
                        grade.get("courseCode"),
                        grade.get("courseName"),
                        grade.get("midtermGrade"),
                        grade.get("finalGrade"),
                        grade.get("totalGrade"),
                        grade.get("classification")
                });
            }
        }
    }

    private void addGrade() {
        GradeInputDialog dialog = new GradeInputDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                serverConnection,
                null // New grade
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            refreshData();
        }
    }

    private void editGrade() {
        int selectedRow = gradeTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một điểm để sửa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // TODO: Get grade data from selected row and show edit dialog
        JOptionPane.showMessageDialog(this,
                "Chức năng sửa điểm đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteGrade() {
        int selectedRow = gradeTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một điểm để xóa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa điểm này?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // TODO: Implement delete functionality
            JOptionPane.showMessageDialog(this,
                    "Chức năng xóa điểm đang được phát triển",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void searchGrades() {
        String keyword = searchField.getText().trim();
        // TODO: Implement search functionality
        JOptionPane.showMessageDialog(this,
                "Tìm kiếm: " + keyword + "\nChức năng đang được phát triển",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void filterByCourse() {
        int selectedIndex = courseFilterCombo.getSelectedIndex();
        if (selectedIndex <= 0) {
            refreshData(); // Show all
        } else {
            // TODO: Filter by selected course
            refreshData();
        }
    }

    private void showGradeStatistics() {
        // TODO: Show statistics dialog
        JOptionPane.showMessageDialog(this,
                "Thống kê điểm:\n" +
                "- Điểm TB tích lũy: Đang tính...\n" +
                "- Số tín chỉ đã đạt: Đang tính...\n" +
                "- Xếp loại: Đang tính...\n\n" +
                "Chức năng đang được phát triển",
                "Thống kê điểm",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Inner class: Dialog nhập điểm
     */
    private static class GradeInputDialog extends JDialog {
        private boolean confirmed = false;

        public GradeInputDialog(Frame parent, IServerConnection serverConnection, Map<String, Object> gradeData) {
            super(parent, gradeData == null ? "Nhập điểm mới" : "Sửa điểm", true);
            
            setLayout(new BorderLayout(10, 10));
            setSize(400, 300);
            setLocationRelativeTo(parent);

            JLabel label = new JLabel("Chức năng nhập điểm đang được phát triển", JLabel.CENTER);
            add(label, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Hủy");

            okButton.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}
