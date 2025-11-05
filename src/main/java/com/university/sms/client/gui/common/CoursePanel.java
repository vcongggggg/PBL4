package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel quản lý lớp học phần (Courses/Class Sections)
 * - Admin: Xem tất cả lớp, có nút "Xem danh sách sinh viên"
 * - Teacher: Xem lớp của mình, có nút "Nhập điểm"
 */
public class CoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private User currentUser;
    private IServerConnection serverConnection;
    private boolean isReadOnly;

    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton viewStudentsButton; // For Admin
    private JButton deleteCourseButton; // For Admin - Hủy lớp
    private JButton gradeEntryButton; // For Teacher

    private List<Course> currentCourses;

    public CoursePanel(User currentUser, IServerConnection serverConnection, boolean isReadOnly) {
        this.currentUser = currentUser;
        this.serverConnection = serverConnection;
        this.isReadOnly = isReadOnly;

        initializeComponents();
        setupLayout();
        setupEventListeners();
        loadInitialData();
    }

    private void initializeComponents() {
        // Create table
        String[] columnNames = { "Mã lớp", "Môn học", "Giáo viên", "Năm học", "Học kỳ", "Phòng", "Lịch học",
                "SV hiện tại/Tối đa" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setRowHeight(25);

        refreshButton = new JButton("Làm mới");

        // Admin: "Xem danh sách sinh viên" và "Hủy lớp"
        if (currentUser.getRole() == User.UserRole.ADMIN) {
            viewStudentsButton = new JButton("Xem danh sách sinh viên");
            viewStudentsButton.setEnabled(false);

            deleteCourseButton = new JButton("Hủy lớp");
            deleteCourseButton.setEnabled(false);
            deleteCourseButton.setForeground(Color.RED);
        }

        // Teacher: "Nhập điểm" và "Xem danh sách sinh viên"
        if (currentUser.getRole() == User.UserRole.TEACHER) {
            viewStudentsButton = new JButton("Xem danh sách sinh viên");
            viewStudentsButton.setEnabled(false);
            
            gradeEntryButton = new JButton("Nhập điểm");
            gradeEntryButton.setEnabled(false);
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel with buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(refreshButton);

        if (currentUser.getRole() == User.UserRole.ADMIN) {
            if (viewStudentsButton != null) {
                topPanel.add(viewStudentsButton);
            }
            if (deleteCourseButton != null) {
                topPanel.add(deleteCourseButton);
            }
        }

        if (currentUser.getRole() == User.UserRole.TEACHER) {
            if (viewStudentsButton != null) {
                topPanel.add(viewStudentsButton);
            }
            if (gradeEntryButton != null) {
                topPanel.add(gradeEntryButton);
            }
        }

        add(topPanel, BorderLayout.NORTH);

        // Center with table
        JScrollPane scrollPane = new JScrollPane(courseTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });

        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        if (viewStudentsButton != null) {
            viewStudentsButton.addActionListener(e -> viewStudentsList());
        }

        if (deleteCourseButton != null) {
            deleteCourseButton.addActionListener(e -> deleteCourse());
        }

        if (gradeEntryButton != null) {
            gradeEntryButton.addActionListener(e -> openGradeEntryDialog());
        }
    }

    private void loadInitialData() {
        refreshData();
    }

    public void refreshData() {
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.getAllCourses();
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                        updateCourseTable(courses);
                    } else {
                        showErrorMessage("Không thể tải danh sách lớp học phần: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải dữ liệu: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void updateCourseTable(List<Course> courses) {
        this.currentCourses = courses;
        tableModel.setRowCount(0);

        if (courses != null) {
            for (Course course : courses) {
                Object[] rowData = {
                        course.getCourseCode(),
                        course.getSubjectName(),
                        course.getTeacherName(),
                        course.getAcademicYear(),
                        course.getSemester(),
                        course.getRoom(),
                        course.getScheduleDay() + " " + course.getScheduleTime(),
                        course.getCurrentStudents() + "/" + course.getMaxStudents()
                };
                tableModel.addRow(rowData);
            }
        }
    }

    private void updateButtonStates() {
        int selectedRow = courseTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        if (viewStudentsButton != null) {
            viewStudentsButton.setEnabled(hasSelection);
        }
        if (deleteCourseButton != null) {
            deleteCourseButton.setEnabled(hasSelection);
        }
        if (gradeEntryButton != null) {
            gradeEntryButton.setEnabled(hasSelection);
        }
    }

    private void viewStudentsList() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorMessage("Vui lòng chọn một lớp học phần để xem danh sách sinh viên");
            return;
        }

        Course selectedCourse = currentCourses.get(selectedRow);

        // Fetch enrollments for this course
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ENROLLMENTS_BY_COURSE);
                request.addData("courseId", selectedCourse.getCourseId());
                return serverConnection.sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<com.university.sms.model.Enrollment> enrollments = (List<com.university.sms.model.Enrollment>) response
                                .getData("enrollments");

                        // Show students in a dialog
                        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(CoursePanel.this),
                                "Danh sách sinh viên - " + selectedCourse.getSubjectName(), true);
                        dialog.setSize(800, 500);
                        dialog.setLocationRelativeTo(CoursePanel.this);

                        displayStudentsDialog(dialog, enrollments);
                        dialog.setVisible(true);
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Không nhận được phản hồi";
                        showErrorMessage("Không thể tải danh sách sinh viên: " + errorMsg);
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải danh sách sinh viên: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void displayStudentsDialog(JDialog dialog, List<com.university.sms.model.Enrollment> enrollments) {
        String[] columnNames = { "MSSV", "Họ tên", "Điểm cuối kỳ", "Xếp loại", "Điểm hệ 4", "Tình trạng" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (com.university.sms.model.Enrollment en : enrollments) {
            Object[] row = {
                    en.getStudentCode(),
                    en.getStudentName(),
                    en.getFinalGrade(),
                    en.getLetterGrade(),
                    en.getGradePoints(),
                    en.getEnrollmentStatus()
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
    }

    private void deleteCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorMessage("Vui lòng chọn một lớp học phần để hủy");
            return;
        }

        Course selectedCourse = currentCourses.get(selectedRow);

        // Xác nhận xóa (bao gồm cả thông tin về việc xóa đăng ký sinh viên)
        String warningMessage = "Bạn có chắc chắn muốn HỦY lớp học phần:\n" +
                "Mã lớp: " + selectedCourse.getCourseCode() + "\n" +
                "Môn học: " + selectedCourse.getSubjectName() + "\n" +
                "Giáo viên: " + selectedCourse.getTeacherName() + "\n\n";

        if (selectedCourse.getCurrentStudents() > 0) {
            warningMessage += "⚠️ LƯU Ý: Lớp này có " + selectedCourse.getCurrentStudents() + " sinh viên đã đăng ký.\n"
                    +
                    "TẤT CẢ ĐĂNG KÝ và ĐIỂM của sinh viên sẽ BỊ XÓA!\n\n";
        }

        warningMessage += "Hành động này KHÔNG THỂ HOÀN TÁC!";

        int confirm = JOptionPane.showConfirmDialog(this,
                warningMessage,
                "Xác nhận hủy lớp học phần",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Xóa lớp
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_DELETE_COURSE);
                request.addData(Constants.KEY_COURSE_ID, selectedCourse.getCourseId());
                return serverConnection.sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(CoursePanel.this,
                                "Đã hủy lớp học phần thành công",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    } else {
                        showErrorMessage("Không thể hủy lớp học phần: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi hủy lớp học phần: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openGradeEntryDialog() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorMessage("Vui lòng chọn một lớp để nhập điểm");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Chức năng nhập điểm đang được phát triển.\nVui lòng sử dụng tab 'Nhập Điểm' để nhập điểm cho sinh viên.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
