package com.university.sms.client.gui;

import com.university.sms.client.ServerConnection;
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
 * Panel quản lý khóa học
 */
public class CoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private User currentUser;
    private ServerConnection serverConnection;
    private boolean isReadOnly;
    
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;

    public CoursePanel(User currentUser, ServerConnection serverConnection, boolean isReadOnly) {
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
        String[] columnNames = {"Mã khóa học", "Tên môn học", "Giáo viên", "Năm học", "Học kỳ", "Phòng", "Lịch học", "SV hiện tại/Tối đa"};
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
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top panel with buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(refreshButton);
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
                        showErrorMessage("Không thể tải danh sách khóa học: " + response.getMessage());
                    }
                } catch (Exception e) {
                    showErrorMessage("Lỗi khi tải dữ liệu: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }

    private void updateCourseTable(List<Course> courses) {
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

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
