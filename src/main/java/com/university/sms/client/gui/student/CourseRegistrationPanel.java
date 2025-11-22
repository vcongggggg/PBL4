package com.university.sms.client.gui.student;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Course;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Subject;
import com.university.sms.model.Transcript;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Panel đăng ký tín chỉ cho sinh viên
 * Thiết kế split-panel: Top = Selected courses, Bottom = Available courses
 */
public class CourseRegistrationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CourseRegistrationPanel.class.getName());

    private IServerConnection serverConnection;
    private String studentCode;

    private JTable selectedTable;
    private DefaultTableModel selectedModel;
    private List<Course> selectedCourses = new ArrayList<>();
    private List<CourseRegistration> submittedRegistrations = new ArrayList<>();
    private List<SelectedRowEntry> selectedRowEntries = new ArrayList<>();
    private JButton removeSelectedButton;
    private JButton cancelRegistrationButton;

    private JTable availableTable;
    private DefaultTableModel availableModel;
    private List<Course> availableCourses = new ArrayList<>();

    private List<String> registeredCourseCodes = new ArrayList<>();
    private final Set<String> registeredSubjectCodes = new HashSet<>();

    private JLabel totalCreditsLabel;
    private JLabel conflictLabel;
    private JButton registerButton;
    private JTextField searchField;

    private final Map<String, String> prerequisiteBySubject = new HashMap<>();
    private final Map<String, String> subjectNameByCode = new HashMap<>();
    private final Set<String> completedSubjectCodes = new HashSet<>();
    private boolean prerequisiteDataLoaded;
    private boolean prerequisiteDataLoading;
    private boolean prerequisiteChecksAvailable;

    public CourseRegistrationPanel() {
        initComponents();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        JPanel topPanel = createSelectedCoursesPanel();
        splitPane.setTopComponent(topPanel);

        JPanel bottomPanel = createAvailableCoursesPanel();
        splitPane.setBottomComponent(bottomPanel);

        add(splitPane, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createSelectedCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Các môn đã chọn / đã đăng ký"));

        String[] columns = { "Mã MH", "Tên môn học", "TC", "Giảng viên", "Thứ", "Tiết", "Phòng", "Sĩ số",
                "Trạng thái" };
        selectedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        selectedTable = new JTable(selectedModel);
        selectedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedTable.setRowHeight(25);
        selectedTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedActionButtons();
            }
        });

        JScrollPane scrollPane = new JScrollPane(selectedTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeSelectedButton = new JButton("Xóa khỏi danh sách");
        removeSelectedButton.setEnabled(false);
        removeSelectedButton.addActionListener(e -> removeSelectedCourse());
        buttonPanel.add(removeSelectedButton);

        cancelRegistrationButton = new JButton("Hủy đăng ký");
        cancelRegistrationButton.setEnabled(false);
        cancelRegistrationButton.addActionListener(e -> cancelSubmittedRegistration());
        buttonPanel.add(cancelRegistrationButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAvailableCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Danh sách môn học có thể đăng ký"));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchField = new JTextField(30);
        searchField.addActionListener(e -> applySearchFilter());
        searchPanel.add(searchField);

        JButton searchBtn = new JButton("Tìm");
        searchBtn.addActionListener(e -> applySearchFilter());
        searchPanel.add(searchBtn);

        JButton refreshBtn = new JButton("Làm mới");
        refreshBtn.addActionListener(e -> {
            if (!prerequisiteDataLoaded) {
                startDataFlow();
            } else {
                loadAvailableCourses();
            }
        });
        searchPanel.add(refreshBtn);

        String[] columns = { "Mã MH", "Tên môn học", "TC", "Giảng viên", "Thứ", "Tiết", "Phòng", "Còn lại/Tối đa",
                "Trạng thái" };
        availableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        availableTable = new JTable(availableModel);
        availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableTable.setRowHeight(25);

        availableTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 8);
                    if ("Đã đầy".equals(status)) {
                        c.setBackground(new Color(255, 205, 210));
                    } else if ("Đã đăng ký".equals(status)) {
                        c.setBackground(new Color(200, 230, 201));
                    } else if ("Đã chọn".equals(status)) {
                        c.setBackground(new Color(255, 245, 157));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(availableTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Thêm vào danh sách");
        addBtn.addActionListener(e -> addSelectedCourse());
        buttonPanel.add(addBtn);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        totalCreditsLabel = new JLabel("Tổng tín chỉ đã chọn: 0");
        totalCreditsLabel.setFont(totalCreditsLabel.getFont().deriveFont(Font.BOLD, 14f));

        conflictLabel = new JLabel("");
        conflictLabel.setForeground(Color.RED);
        conflictLabel.setFont(conflictLabel.getFont().deriveFont(Font.BOLD, 12f));

        infoPanel.add(totalCreditsLabel);
        infoPanel.add(conflictLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        registerButton = new JButton("Đăng Ký Các Môn Đã Chọn");
        registerButton.setFont(registerButton.getFont().deriveFont(Font.BOLD, 14f));
        registerButton.setBackground(new Color(76, 175, 80));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.addActionListener(e -> registerSelectedCourses());
        buttonPanel.add(registerButton);

        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    public void setServerConnection(IServerConnection connection) {
        this.serverConnection = connection;
    }

    public void setCurrentUser(User user, int studentId) {
        com.university.sms.dao.StudentDAO studentDAO = new com.university.sms.dao.StudentDAO();
        com.university.sms.model.Student student = studentDAO.findByUsername(user.getUsername());
        this.studentCode = student != null ? student.getStudentCode() : null;
        if (serverConnection != null) {
            prerequisiteDataLoaded = false;
            prerequisiteDataLoading = false;
            prerequisiteChecksAvailable = false;
            prerequisiteBySubject.clear();
            subjectNameByCode.clear();
            completedSubjectCodes.clear();
            selectedCourses.clear();
            submittedRegistrations.clear();
            selectedRowEntries.clear();
            registeredCourseCodes.clear();
            registeredSubjectCodes.clear();
            refreshSelectedCoursesTable();
            startDataFlow(); // Load prerequisites first, then registrations/courses
        }
    }

    private void startDataFlow() {
        if (serverConnection == null || studentCode == null || studentCode.trim().isEmpty()) {
            return;
        }
        if (prerequisiteDataLoaded) {
            loadRegisteredCourses();
        } else {
            loadPrerequisiteData();
        }
    }

    private void loadPrerequisiteData() {
        if (prerequisiteDataLoading || serverConnection == null || studentCode == null) {
            return;
        }

        prerequisiteDataLoading = true;

        SwingWorker<PrerequisiteDataBundle, Void> worker = new SwingWorker<>() {
            @Override
            protected PrerequisiteDataBundle doInBackground() throws Exception {
                Map<String, String> prereqMap = new HashMap<>();
                Map<String, String> nameMap = new HashMap<>();
                boolean subjectsLoaded = true;

                try {
                    Message subjectRequest = Message.createRequest(Constants.ACTION_GET_ALL_SUBJECTS);
                    Message subjectResponse = serverConnection.sendRequest(subjectRequest);

                    if (subjectResponse != null && subjectResponse.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Subject> subjects = (List<Subject>) subjectResponse.getData(Constants.KEY_SUBJECTS);
                        if (subjects != null) {
                            for (Subject subject : subjects) {
                                String code = normalizeSubjectCode(subject.getSubjectCode());
                                if (code == null) {
                                    continue;
                                }
                                prereqMap.put(code, normalizeSubjectCode(subject.getPrerequisiteSubjectCode()));
                                nameMap.put(code, subject.getSubjectName());
                            }
                        }
                    } else {
                        String msg = subjectResponse != null ? subjectResponse.getMessage()
                                : "Không nhận được dữ liệu môn học";
                        throw new IllegalStateException("Không thể tải danh sách môn học: " + msg);
                    }
                } catch (Exception subjectError) {
                    subjectsLoaded = false;
                    LOGGER.log(Level.WARNING, "Không thể tải danh sách môn học, sẽ bỏ qua kiểm tra môn tiên quyết",
                            subjectError);
                    prereqMap.clear();
                    nameMap.clear();
                }

                Set<String> completedSubjects;
                boolean transcriptLoaded = true;
                try {
                    completedSubjects = fetchCompletedSubjectCodes();
                } catch (Exception transcriptError) {
                    LOGGER.log(Level.WARNING,
                            "Không thể tải danh sách môn đã hoàn thành, sẽ bỏ qua kiểm tra môn tiên quyết",
                            transcriptError);
                    completedSubjects = Collections.emptySet();
                    transcriptLoaded = false;
                }
                return new PrerequisiteDataBundle(prereqMap, nameMap, completedSubjects, transcriptLoaded,
                        subjectsLoaded);
            }

            @Override
            protected void done() {
                try {
                    PrerequisiteDataBundle data = get();
                    prerequisiteBySubject.clear();
                    prerequisiteBySubject.putAll(data.prerequisiteMap);
                    subjectNameByCode.clear();
                    subjectNameByCode.putAll(data.subjectNames);
                    completedSubjectCodes.clear();
                    completedSubjectCodes.addAll(data.completedSubjects);
                    prerequisiteChecksAvailable = data.transcriptLoaded && data.subjectsLoaded
                            && !prerequisiteBySubject.isEmpty();
                    if (!data.subjectsLoaded) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                CourseRegistrationPanel.this,
                                "Không thể tải danh sách môn học. Sẽ bỏ qua kiểm tra môn tiên quyết.",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE));
                    } else if (!data.transcriptLoaded) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                CourseRegistrationPanel.this,
                                "Không thể tải danh sách môn đã hoàn thành. Sẽ bỏ qua việc kiểm tra môn tiên quyết.",
                                "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE));
                    }
                    prerequisiteDataLoaded = true;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu môn tiên quyết", e);
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Không thể tải dữ liệu môn tiên quyết: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    prerequisiteDataLoading = false;
                    if (prerequisiteDataLoaded) {
                        loadRegisteredCourses();
                    }
                }
            }
        };
        worker.execute();
    }

    private Set<String> fetchCompletedSubjectCodes() throws Exception {
        Message transcriptRequest = Message.createRequest(Constants.ACTION_GET_TRANSCRIPT);
        transcriptRequest.addData("studentCode", studentCode);
        Message response = serverConnection.sendRequest(transcriptRequest);

        if (response == null || !response.isSuccess()) {
            String msg = response != null ? response.getMessage() : "Không nhận được dữ liệu bảng điểm";
            throw new IllegalStateException("Không thể tải bảng điểm sinh viên: " + msg);
        }

        Transcript transcript = (Transcript) response.getData(Constants.KEY_TRANSCRIPT);
        Set<String> completed = new HashSet<>();

        if (transcript != null && transcript.getSemesterRecords() != null) {
            for (Transcript.SemesterRecord semester : transcript.getSemesterRecords()) {
                if (semester == null || semester.getCourses() == null) {
                    continue;
                }
                for (Transcript.CourseRecord courseRecord : semester.getCourses()) {
                    if (courseRecord == null) {
                        continue;
                    }
                    if (isCourseRecordCompleted(courseRecord)) {
                        String code = normalizeSubjectCode(courseRecord.getSubjectCode());
                        if (code != null) {
                            completed.add(code);
                        }
                    }
                }
            }
        }

        return completed;
    }

    private boolean isCourseRecordCompleted(Transcript.CourseRecord courseRecord) {
        if (courseRecord.getSubjectCode() == null) {
            return false;
        }
        String status = courseRecord.getStatus();
        if (status == null) {
            return false;
        }
        if (!"completed".equalsIgnoreCase(status)) {
            return false;
        }
        return courseRecord.getGradePoints() != null
                && courseRecord.getGradePoints().compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    private void loadRegisteredCourses() {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return;
        }
        if (!prerequisiteDataLoaded) {
            if (!prerequisiteDataLoading) {
                loadPrerequisiteData();
            }
            return;
        }

        SwingWorker<List<CourseRegistration>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CourseRegistration> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_MY_REGISTRATIONS);
                request.addData("studentCode", studentCode);

                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<CourseRegistration> registrations = (List<CourseRegistration>) response
                            .getData(Constants.KEY_REGISTRATIONS);
                    return registrations != null ? registrations : new ArrayList<>();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    List<CourseRegistration> registrations = get();

                    registeredCourseCodes = registrations.stream()
                            .filter(reg -> reg
                                    .getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED)
                            .map(CourseRegistration::getCourseCode)
                            .collect(Collectors.toList());

                    registeredSubjectCodes.clear();
                    for (CourseRegistration reg : registrations) {
                        if (reg.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
                            continue;
                        }
                        String code = normalizeSubjectCode(reg.getSubjectCode());
                        if (code != null) {
                            registeredSubjectCodes.add(code);
                        }
                    }

                    updateSubmittedRegistrations(registrations);
                    loadAvailableCourses();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách môn đã đăng ký", e);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                                "Không thể tải danh sách môn đã đăng ký: " + e.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        worker.execute();
    }

    private void loadAvailableCourses() {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return;
        }
        if (!prerequisiteDataLoaded) {
            if (!prerequisiteDataLoading) {
                loadPrerequisiteData();
            }
            return;
        }

        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
                Message response = serverConnection.sendRequest(request);

                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Course> courses = (List<Course>) response.getData(Constants.KEY_COURSES);
                    return courses != null ? courses : new ArrayList<>();
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    availableCourses = get();
                    updateAvailableTable();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi tải danh sách môn học khả dụng", e);
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Lỗi khi tải danh sách môn học: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateAvailableTable() {
        availableModel.setRowCount(0);
        String searchText = searchField.getText().trim().toLowerCase();
        Set<String> lockedSubjects = getLockedSubjectCodes();

        for (Course course : availableCourses) {
            // Skip if already selected or registered
            if (isCourseSelected(course) || isCourseRegistered(course.getCourseCode())) {
                continue;
            }

            if (course.getCourseStatus() != Course.CourseStatus.PLANNING) {
                continue;
            }

            if (course.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
                continue;
            }

            String subjectCode = normalizeSubjectCode(course.getSubjectCode());
            if (subjectCode != null && lockedSubjects.contains(subjectCode)) {
                continue;
            }

            if (!isCourseEligibleForStudent(course)) {
                continue;
            }

            // Apply search filter
            if (!searchText.isEmpty()) {
                String searchableText = (course.getCourseCode() + " " +
                        course.getCourseName() + " " +
                        course.getTeacherName()).toLowerCase();
                if (!searchableText.contains(searchText)) {
                    continue;
                }
            }

            int remaining = course.getMaxStudents() - course.getCurrentEnrollment();
            String availabilityText = remaining + "/" + course.getMaxStudents();

            String status = remaining <= 0 ? "Đã đầy" : "Mở đăng ký";

            availableModel.addRow(new Object[] {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredits(),
                    course.getTeacherName(),
                    course.getScheduleDay(),
                    course.getScheduleTime(),
                    course.getRoom(),
                    availabilityText,
                    status
            });
        }
    }

    private void addSelectedCourse() {
        if (!prerequisiteDataLoaded) {
            JOptionPane.showMessageDialog(this,
                    "Dữ liệu môn tiên quyết đang được tải. Vui lòng thử lại sau.",
                    "Đang tải dữ liệu",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int selectedRow = availableTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một môn học để thêm",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String courseCode = (String) availableModel.getValueAt(selectedRow, 0);

        Course courseToAdd = null;
        for (Course c : availableCourses) {
            if (c.getCourseCode().equals(courseCode)) {
                courseToAdd = c;
                break;
            }
        }

        if (courseToAdd == null) {
            return;
        }

        if (courseToAdd.getRegistrationStatus() != Course.RegistrationStatus.OPEN) {
            JOptionPane.showMessageDialog(this,
                    "Lớp học này hiện không mở đăng ký.",
                    "Không thể thêm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (courseToAdd.getCurrentEnrollment() >= courseToAdd.getMaxStudents()) {
            JOptionPane.showMessageDialog(this,
                    "Lớp học này đã đầy!",
                    "Không thể thêm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Set<String> lockedSubjects = getLockedSubjectCodes();
        String subjectCode = normalizeSubjectCode(courseToAdd.getSubjectCode());
        if (subjectCode != null && lockedSubjects.contains(subjectCode)) {
            JOptionPane.showMessageDialog(this,
                    "Bạn đã có lớp khác của môn học này. Vui lòng hủy trước khi chọn lớp mới.",
                    "Không thể thêm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isCourseEligibleForStudent(courseToAdd)) {
            String requiredCode = prerequisiteBySubject
                    .getOrDefault(normalizeSubjectCode(courseToAdd.getSubjectCode()), "");
            JOptionPane.showMessageDialog(this,
                    "Bạn phải hoàn thành môn tiên quyết trước: "
                            + getPrerequisiteDisplayName(requiredCode),
                    "Không thể thêm",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check for schedule conflict
        if (hasScheduleConflict(courseToAdd)) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Môn học này có xung đột lịch học với môn đã chọn!\nBạn có chắc muốn thêm?",
                    "Cảnh báo xung đột",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Add to selected list
        selectedCourses.add(courseToAdd);

        refreshSelectedCoursesTable();
        updateAvailableTable();
        updateCreditsAndConflicts();
        updateSelectedActionButtons();
    }

    private void removeSelectedCourse() {
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một môn học để xóa",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedRow >= selectedRowEntries.size()) {
            return;
        }

        if (serverConnection == null) {
            JOptionPane.showMessageDialog(this,
                    "Chưa kết nối tới máy chủ. Vui lòng thử lại sau.",
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SelectedRowEntry entry = selectedRowEntries.get(selectedRow);
        if (entry.getType() == SelectedRowEntry.RowType.SUBMITTED) {
            JOptionPane.showMessageDialog(this,
                    "Môn học đã đăng ký không thể xóa tại đây.\nVui lòng chờ quản trị viên xử lý hoặc sử dụng mục 'Đăng ký của tôi' để hủy.",
                    "Không thể xóa",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        selectedCourses.remove(entry.getCourse());

        refreshSelectedCoursesTable();
        updateAvailableTable();
        updateCreditsAndConflicts();
    }

    private void cancelSubmittedRegistration() {
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= selectedRowEntries.size()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một đăng ký để hủy",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        SelectedRowEntry entry = selectedRowEntries.get(selectedRow);
        if (entry.getType() != SelectedRowEntry.RowType.SUBMITTED || entry.getRegistration() == null) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ có thể hủy những đăng ký đã gửi lên hệ thống.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        CourseRegistration registration = entry.getRegistration();
        if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
            JOptionPane.showMessageDialog(this,
                    "Đăng ký này đã được hủy trước đó.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (studentCode == null || studentCode.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy mã sinh viên để hủy đăng ký.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn hủy đăng ký môn " + registration.getSubjectName() + "?",
                "Xác nhận hủy đăng ký",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Message request = Message.createRequest(Constants.ACTION_CANCEL_REGISTRATION);
                request.addData(Constants.KEY_REGISTRATION_ID, registration.getRegistrationId());
                request.addData("studentCode", studentCode);

                Message response = serverConnection.sendRequest(request);
                if (response != null && response.isSuccess()) {
                    return true;
                }
                String error = response != null ? response.getMessage() : "Không nhận được phản hồi từ server";
                throw new IllegalStateException(error);
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Đã hủy đăng ký thành công.",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadRegisteredCourses();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi hủy đăng ký môn học", e);
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Không thể hủy đăng ký: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void registerSelectedCourses() {
        if (!prerequisiteDataLoaded) {
            JOptionPane.showMessageDialog(this,
                    "Dữ liệu môn tiên quyết đang được tải. Vui lòng thử lại sau.",
                    "Đang tải dữ liệu",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn ít nhất một môn học để đăng ký",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Course> invalidCourses = selectedCourses.stream()
                .filter(course -> !isCourseEligibleForStudent(course))
                .collect(Collectors.toList());
        if (!invalidCourses.isEmpty()) {
            Course invalid = invalidCourses.get(0);
            String requiredCode = prerequisiteBySubject
                    .getOrDefault(normalizeSubjectCode(invalid.getSubjectCode()), "");
            JOptionPane.showMessageDialog(this,
                    String.format("Không thể đăng ký %s vì chưa hoàn thành môn tiên quyết: %s",
                            invalid.getCourseName(), getPrerequisiteDisplayName(requiredCode)),
                    "Thiếu môn tiên quyết",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check conflicts
        if (hasAnyConflict()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Có xung đột lịch học trong các môn đã chọn!\nBạn có chắc muốn đăng ký?",
                    "Cảnh báo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Check if any course is full before submitting - refresh course list first
        Message refreshRequest = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
        Message refreshResponse = serverConnection.sendRequest(refreshRequest);
        Map<String, Course> latestCourses = new HashMap<>();
        if (refreshResponse != null && refreshResponse.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Course> allCourses = (List<Course>) refreshResponse.getData(Constants.KEY_COURSES);
            if (allCourses != null) {
                for (Course c : allCourses) {
                    latestCourses.put(c.getCourseCode(), c);
                }
            }
        }

        List<Course> fullCourses = new ArrayList<>();
        for (Course course : selectedCourses) {
            Course updatedCourse = latestCourses.get(course.getCourseCode());
            if (updatedCourse != null && updatedCourse.getCurrentStudents() >= updatedCourse.getMaxStudents()) {
                fullCourses.add(updatedCourse);
            }
        }

        if (!fullCourses.isEmpty()) {
            StringBuilder message = new StringBuilder("Các lớp học sau đã đầy và không thể đăng ký:\n\n");
            for (Course course : fullCourses) {
                message.append(String.format("- %s (%s): %d/%d sinh viên\n",
                        course.getCourseCode(), course.getCourseName(),
                        course.getCurrentStudents(), course.getMaxStudents()));
            }
            message.append("\nVui lòng xóa các lớp đã đầy khỏi danh sách trước khi đăng ký.");
            JOptionPane.showMessageDialog(this, message.toString(), "Lớp đã đầy", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có chắc muốn đăng ký %d môn học?\nTổng: %d tín chỉ",
                        selectedCourses.size(), getTotalCredits()),
                "Xác nhận đăng ký",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Map<Course, String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<Course, String> doInBackground() throws Exception {
                Map<Course, String> results = new HashMap<>();

                // Refresh course list to get latest currentStudents before submitting
                Message refreshRequest = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
                Message refreshResponse = serverConnection.sendRequest(refreshRequest);
                Map<String, Course> latestCoursesMap = new HashMap<>();
                if (refreshResponse != null && refreshResponse.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Course> allCourses = (List<Course>) refreshResponse.getData(Constants.KEY_COURSES);
                    if (allCourses != null) {
                        for (Course c : allCourses) {
                            latestCoursesMap.put(c.getCourseCode(), c);
                        }
                    }
                }

                for (Course course : selectedCourses) {
                    // Double check before sending request using latest course data
                    Course latestCourse = latestCoursesMap.get(course.getCourseCode());
                    if (latestCourse != null && latestCourse.getCurrentStudents() >= latestCourse.getMaxStudents()) {
                        results.put(course, "Lỗi: Lớp đã đầy");
                        continue;
                    }

                    Message request = Message.createRequest(Constants.ACTION_REGISTER_COURSE);
                    request.addData("studentCode", studentCode);
                    request.addData("courseCode", course.getCourseCode());

                    Message response = serverConnection.sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        results.put(course, "Thành công");
                    } else {
                        String error = response != null ? response.getMessage() : "Không có phản hồi";
                        results.put(course, "Lỗi: " + error);
                    }
                }

                return results;
            }

            @Override
            protected void done() {
                try {
                    Map<Course, String> results = get();

                    // Show results
                    StringBuilder message = new StringBuilder("Kết quả đăng ký:\n\n");
                    int successCount = 0;

                    for (Map.Entry<Course, String> entry : results.entrySet()) {
                        Course course = entry.getKey();
                        String result = entry.getValue();

                        message.append(course.getCourseCode())
                                .append(" - ")
                                .append(course.getCourseName())
                                .append(": ")
                                .append(result)
                                .append("\n");

                        if ("Thành công".equals(result)) {
                            successCount++;
                        }
                    }

                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            message.toString(),
                            "Kết quả đăng ký",
                            successCount == results.size() ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.WARNING_MESSAGE);

                    if (successCount == results.size() && successCount > 0) {
                        selectedCourses.clear();
                        refreshSelectedCoursesTable();
                        updateCreditsAndConflicts();
                    }
                    loadRegisteredCourses();

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi xử lý đăng ký môn học", e);
                    JOptionPane.showMessageDialog(CourseRegistrationPanel.this,
                            "Lỗi khi đăng ký: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private boolean hasScheduleConflict(Course newCourse) {
        for (Course existing : selectedCourses) {
            if (isTimeConflict(existing, newCourse)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyConflict() {
        for (int i = 0; i < selectedCourses.size(); i++) {
            for (int j = i + 1; j < selectedCourses.size(); j++) {
                if (isTimeConflict(selectedCourses.get(i), selectedCourses.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTimeConflict(Course c1, Course c2) {
        if (c1.getScheduleDay() == null || c2.getScheduleDay() == null) {
            return false;
        }

        if (!c1.getScheduleDay().equals(c2.getScheduleDay())) {
            return false;
        }

        if (c1.getScheduleTime() == null || c2.getScheduleTime() == null) {
            return false;
        }

        String scheduleTime1 = c1.getScheduleTime().trim();
        String scheduleTime2 = c2.getScheduleTime().trim();

        if (scheduleTime1.contains("(") && scheduleTime1.contains(")")) {
            int start = scheduleTime1.indexOf("(");
            int end = scheduleTime1.indexOf(")");
            scheduleTime1 = scheduleTime1.substring(start + 1, end).trim();
        }

        if (scheduleTime2.contains("(") && scheduleTime2.contains(")")) {
            int start = scheduleTime2.indexOf("(");
            int end = scheduleTime2.indexOf(")");
            scheduleTime2 = scheduleTime2.substring(start + 1, end).trim();
        }

        String[] time1 = scheduleTime1.split("-");
        String[] time2 = scheduleTime2.split("-");

        if (time1.length != 2 || time2.length != 2) {
            return false;
        }

        try {
            return !(time1[1].trim().compareTo(time2[0].trim()) <= 0 ||
                    time2[1].trim().compareTo(time1[0].trim()) <= 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateCreditsAndConflicts() {
        int totalCredits = getTotalCredits();
        totalCreditsLabel.setText("Tổng tín chỉ đã chọn: " + totalCredits);

        if (hasAnyConflict()) {
            conflictLabel.setText("⚠ CÓ XUNG ĐỘT LỊCH HỌC!");
        } else {
            conflictLabel.setText("");
        }

        registerButton.setEnabled(!selectedCourses.isEmpty());
    }

    private void updateSelectedActionButtons() {
        if (removeSelectedButton == null || cancelRegistrationButton == null || selectedTable == null) {
            return;
        }
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= selectedRowEntries.size()) {
            removeSelectedButton.setEnabled(false);
            cancelRegistrationButton.setEnabled(false);
            return;
        }
        SelectedRowEntry entry = selectedRowEntries.get(selectedRow);
        boolean isDraft = entry.getType() == SelectedRowEntry.RowType.DRAFT;
        removeSelectedButton.setEnabled(isDraft);

        if (entry.getType() == SelectedRowEntry.RowType.SUBMITTED && entry.getRegistration() != null) {
            boolean cancellable = entry.getRegistration()
                    .getRegistrationStatus() != CourseRegistration.RegistrationStatus.CANCELLED;
            cancelRegistrationButton.setEnabled(cancellable);
        } else {
            cancelRegistrationButton.setEnabled(false);
        }
    }

    private boolean isCourseEligibleForStudent(Course course) {
        if (!prerequisiteChecksAvailable) {
            return true;
        }
        String subjectCode = normalizeSubjectCode(course.getSubjectCode());
        if (subjectCode == null) {
            return true;
        }
        String prerequisiteCode = prerequisiteBySubject.get(subjectCode);
        if (prerequisiteCode == null || prerequisiteCode.isEmpty()) {
            return true;
        }
        return hasCompletedPrerequisite(prerequisiteCode);
    }

    private boolean hasCompletedPrerequisite(String prerequisiteCode) {
        String normalized = normalizeSubjectCode(prerequisiteCode);
        if (normalized == null) {
            return true;
        }
        return completedSubjectCodes.contains(normalized);
    }

    private String normalizeSubjectCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String getPrerequisiteDisplayName(String prerequisiteCode) {
        String normalized = normalizeSubjectCode(prerequisiteCode);
        if (normalized == null) {
            return "Môn tiên quyết yêu cầu";
        }
        String name = subjectNameByCode.get(normalized);
        if (name == null || name.isBlank()) {
            return normalized;
        }
        return normalized + " - " + name;
    }

    private int getTotalCredits() {
        int total = 0;
        for (Course course : selectedCourses) {
            total += course.getCredits();
        }
        return total;
    }

    private boolean isCourseSelected(Course course) {
        for (Course selected : selectedCourses) {
            if (selected.getCourseId() == course.getCourseId()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCourseRegistered(String courseCode) {
        return registeredCourseCodes.contains(courseCode);
    }

    private void refreshSelectedCoursesTable() {
        if (selectedModel == null) {
            return;
        }

        selectedModel.setRowCount(0);
        selectedRowEntries.clear();

        for (Course course : selectedCourses) {
            selectedModel.addRow(new Object[] {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredits(),
                    safeText(course.getTeacherName()),
                    safeText(course.getScheduleDay()),
                    safeText(course.getScheduleTime()),
                    safeText(course.getRoom()),
                    formatSeatInfo(course),
                    "Chưa đăng ký"
            });
            selectedRowEntries.add(SelectedRowEntry.forDraft(course));
        }

        for (CourseRegistration registration : submittedRegistrations) {
            selectedModel.addRow(new Object[] {
                    registration.getCourseCode(),
                    registration.getSubjectName(),
                    registration.getCredits(),
                    safeText(registration.getTeacherName()),
                    safeText(registration.getScheduleDay()),
                    safeText(registration.getScheduleTime()),
                    safeText(registration.getRoom()),
                    "-",
                    getRegistrationStatusText(registration.getRegistrationStatus())
            });
            selectedRowEntries.add(SelectedRowEntry.forSubmitted(registration));
        }

        if (selectedTable != null) {
            selectedTable.clearSelection();
        }
        updateSelectedActionButtons();
    }

    private String formatSeatInfo(Course course) {
        if (course == null) {
            return "";
        }
        if (course.getMaxStudents() <= 0) {
            return String.valueOf(course.getCurrentEnrollment());
        }
        return course.getCurrentEnrollment() + "/" + course.getMaxStudents();
    }

    private void updateSubmittedRegistrations(List<CourseRegistration> registrations) {
        submittedRegistrations.clear();
        if (registrations != null) {
            for (CourseRegistration registration : registrations) {
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.PENDING) {
                    submittedRegistrations.add(registration);
                }
            }
        }
        refreshSelectedCoursesTable();
        updateAvailableTable();
    }

    private Set<String> getLockedSubjectCodes() {
        Set<String> locked = new HashSet<>(registeredSubjectCodes);
        for (Course course : selectedCourses) {
            String code = normalizeSubjectCode(course.getSubjectCode());
            if (code != null) {
                locked.add(code);
            }
        }
        for (CourseRegistration reg : submittedRegistrations) {
            if (reg.getRegistrationStatus() == CourseRegistration.RegistrationStatus.CANCELLED) {
                continue;
            }
            String code = normalizeSubjectCode(reg.getSubjectCode());
            if (code != null) {
                locked.add(code);
            }
        }
        return locked;
    }

    private String getRegistrationStatusText(CourseRegistration.RegistrationStatus status) {
        if (status == null) {
            return "Đã đăng ký";
        }
        if (status == CourseRegistration.RegistrationStatus.PENDING) {
            return "Đã đăng ký (Chờ duyệt)";
        }
        if (status == CourseRegistration.RegistrationStatus.CANCELLED) {
            return "Đã hủy";
        }
        if (status == CourseRegistration.RegistrationStatus.APPROVED) {
            return "Đã duyệt";
        }
        return "Đã đăng ký";
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    private static class SelectedRowEntry {
        enum RowType {
            DRAFT, SUBMITTED
        }

        private final RowType type;
        private final Course course;
        private final CourseRegistration registration;

        private SelectedRowEntry(RowType type, Course course, CourseRegistration registration) {
            this.type = type;
            this.course = course;
            this.registration = registration;
        }

        static SelectedRowEntry forDraft(Course course) {
            return new SelectedRowEntry(RowType.DRAFT, course, null);
        }

        static SelectedRowEntry forSubmitted(CourseRegistration registration) {
            return new SelectedRowEntry(RowType.SUBMITTED, null, registration);
        }

        RowType getType() {
            return type;
        }

        Course getCourse() {
            return course;
        }

        CourseRegistration getRegistration() {
            return registration;
        }

    }

    private static class PrerequisiteDataBundle {
        private final Map<String, String> prerequisiteMap;
        private final Map<String, String> subjectNames;
        private final Set<String> completedSubjects;
        private final boolean transcriptLoaded;
        private final boolean subjectsLoaded;

        private PrerequisiteDataBundle(Map<String, String> prerequisiteMap,
                Map<String, String> subjectNames,
                Set<String> completedSubjects,
                boolean transcriptLoaded,
                boolean subjectsLoaded) {
            this.prerequisiteMap = prerequisiteMap;
            this.subjectNames = subjectNames;
            this.completedSubjects = completedSubjects;
            this.transcriptLoaded = transcriptLoaded;
            this.subjectsLoaded = subjectsLoaded;
        }
    }

    private void applySearchFilter() {
        updateAvailableTable();
    }

    public void refreshData() {
        startDataFlow();
    }
}
