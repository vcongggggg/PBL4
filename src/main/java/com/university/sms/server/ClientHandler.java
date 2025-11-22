package com.university.sms.server;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.service.AuthenticationService;
import com.university.sms.service.AuthenticationService.AuthenticationResult;
import com.university.sms.service.StudentService;
import com.university.sms.service.CourseService;
import com.university.sms.service.SubjectService;
import com.university.sms.service.ClassOpeningRequestService;
import com.university.sms.service.CourseRegistrationService;
import com.university.sms.service.GradeService;
import com.university.sms.service.NotificationService;
import com.university.sms.service.TimetableService;
import com.university.sms.service.TranscriptService;
import com.university.sms.dao.CourseRegistrationDAO;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.university.sms.util.DatabaseConnection;
import com.university.sms.server.handler.StudentHandler;
import com.university.sms.server.handler.TeacherHandler;
import com.university.sms.server.handler.AdminHandler;
import com.university.sms.server.handler.CourseHandler;
import com.university.sms.server.handler.SubjectHandler;
import com.university.sms.server.handler.EnrollmentHandler;
import com.university.sms.server.handler.NotificationHandler;
import com.university.sms.server.handler.TimetableHandler;
import com.university.sms.server.handler.StatisticsHandler;
import com.university.sms.server.handler.DataOriginHelper;
import com.university.sms.server.handler.SyncHandler;

/**
 * Xử lý kết nối từ mỗi client
 */
public class ClientHandler implements Runnable, DataOriginHelper {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private User currentUser;
    private boolean isConnected;

    // Các service
    private AuthenticationService authService;
    private StudentService studentService;
    private CourseService courseService;
    private SubjectService subjectService;
    private ClassOpeningRequestService classRequestService;
    private CourseRegistrationService registrationService;
    private GradeService gradeService;
    private NotificationService notificationService;
    private TimetableService timetableService;
    private TranscriptService transcriptService;

    // Nguồn dữ liệu client (ví dụ: CSV, POSTGRES, v.v.) được lưu khi đồng bộ
    private String clientSource = "UNKNOWN";

    // Handlers
    private StudentHandler studentHandler;
    private TeacherHandler teacherHandler;
    private AdminHandler adminHandler;
    private CourseHandler courseHandler;
    private SubjectHandler subjectHandler;
    private EnrollmentHandler enrollmentHandler;
    private NotificationHandler notificationHandler;
    private TimetableHandler timetableHandler;
    private StatisticsHandler statisticsHandler;
    private SyncHandler syncHandler;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;

        // Khởi tạo các service
        this.authService = new AuthenticationService();
        this.studentService = new StudentService();
        this.courseService = new CourseService();
        this.subjectService = new SubjectService();
        this.classRequestService = new ClassOpeningRequestService();
        this.registrationService = new CourseRegistrationService();
        this.gradeService = new GradeService();
        this.notificationService = new NotificationService();
        this.timetableService = new TimetableService();
        this.transcriptService = new TranscriptService();

        // Khởi tạo handlers
        this.studentHandler = new StudentHandler(
                studentService,
                registrationService,
                transcriptService,
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this // DataOriginHelper
        );
        this.teacherHandler = new TeacherHandler(
                classRequestService,
                registrationService,
                gradeService,
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this // DataOriginHelper
        );
        this.adminHandler = new AdminHandler(
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this, // DataOriginHelper
                studentService,
                classRequestService,
                registrationService);
        this.courseHandler = new CourseHandler(
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this, // DataOriginHelper
                courseService);
        this.subjectHandler = new SubjectHandler(
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this, // DataOriginHelper
                subjectService);
        this.enrollmentHandler = new EnrollmentHandler(
                null, // currentUser sẽ được set sau khi login
                clientSource,
                this, // DataOriginHelper
                studentService,
                courseService);
        this.notificationHandler = new NotificationHandler(
                null, // currentUser sẽ được set sau khi login
                notificationService);
        this.timetableHandler = new TimetableHandler(
                null, // currentUser sẽ được set sau khi login
                timetableService);
        this.statisticsHandler = new StatisticsHandler(
                null, // currentUser sẽ được set sau khi login
                transcriptService,
                this::getServerVersion);
        this.syncHandler = new SyncHandler(
                studentService,
                courseService,
                classRequestService,
                notificationService,
                this,
                () -> this.clientSource,
                source -> this.clientSource = source);
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            LOGGER.info("Client đã kết nối: " + clientSocket.getRemoteSocketAddress());

            while (isConnected && !clientSocket.isClosed()) {
                try {
                    Message request = (Message) inputStream.readObject();
                    LOGGER.info("Nhận yêu cầu: " + request.getAction() + " từ " +
                            (currentUser != null ? currentUser.getUsername() : "anonymous"));

                    Message response = processRequest(request);
                    sendResponse(response);

                } catch (SocketException e) {
                    LOGGER.info("Client đã ngắt kết nối: " + clientSocket.getRemoteSocketAddress());
                    break;
                } catch (EOFException e) {
                    LOGGER.info("Kết nối client đã kết thúc: " + clientSocket.getRemoteSocketAddress());
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi khi xử lý yêu cầu từ client", e);

                    Message errorResponse = Message.createErrorResponse("ERROR", "Server error occurred");
                    sendResponse(errorResponse);
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi khởi tạo client handler", e);
        } finally {
            disconnect();
        }
    }

    /**
     * Xử lý yêu cầu từ client
     */
    private Message processRequest(Message request) {
        String action = request.getAction();

        try {
            // Các hành động yêu cầu xác thực
            if (!isAuthenticated() && !isPublicAction(action)) {
                return Message.createErrorResponse(action, Constants.MSG_UNAUTHORIZED);
            }

            switch (action) {
                // Hành động xác thực
                case Constants.ACTION_LOGIN:
                    return handleLogin(request);
                case Constants.ACTION_LOGOUT:
                    return handleLogout(request);
                case Constants.ACTION_CHANGE_PASSWORD:
                    return handleChangePassword(request);

                // Hành động quản lý giáo viên
                case Constants.ACTION_ADD_TEACHER:
                    return adminHandler.handleAddTeacher(request);
                case Constants.ACTION_UPDATE_TEACHER:
                    return adminHandler.handleUpdateTeacher(request);
                case Constants.ACTION_DELETE_TEACHER:
                    return adminHandler.handleDeleteTeacher(request);
                case Constants.ACTION_ACTIVATE_USER:
                    return adminHandler.handleActivateUser(request);
                case Constants.ACTION_GET_ALL_TEACHERS_INCLUDE_INACTIVE:
                    return adminHandler.handleGetAllTeachersIncludeInactive(request);
                case Constants.ACTION_GET_ALL_STUDENTS_INCLUDE_INACTIVE:
                    return adminHandler.handleGetAllStudentsIncludeInactive(request);
                case Constants.ACTION_GET_STUDENTS_PAGED:
                    return adminHandler.handleGetStudentsPaged(request);

                // Hành động sinh viên
                case Constants.ACTION_GET_STUDENT_INFO:
                    return studentHandler.handleGetStudentInfo(request);
                case Constants.ACTION_GET_ALL_STUDENTS:
                    return studentHandler.handleGetAllStudents(request);
                case Constants.ACTION_GET_STUDENTS_BY_CLASS:
                    return studentHandler.handleGetStudentsByClass(request);
                case Constants.ACTION_SEARCH_STUDENTS:
                    return studentHandler.handleSearchStudents(request);
                case Constants.ACTION_ADD_STUDENT:
                    return studentHandler.handleAddStudent(request);
                case Constants.ACTION_UPDATE_STUDENT:
                    return studentHandler.handleUpdateStudent(request);
                case Constants.ACTION_DELETE_STUDENT:
                    return studentHandler.handleDeleteStudent(request);

                // Hành động khóa học (quản lý lớp học phần: mở/đóng, CRUD course)
                case Constants.ACTION_GET_ALL_COURSES:
                    return courseHandler.handleGetAllCourses(request);
                case Constants.ACTION_GET_COURSE_INFO:
                    return courseHandler.handleGetCourseInfo(request);
                case Constants.ACTION_ADD_COURSE:
                    return courseHandler.handleAddCourse(request);
                case Constants.ACTION_UPDATE_COURSE:
                    return courseHandler.handleUpdateCourse(request);
                case Constants.ACTION_DELETE_COURSE:
                    return courseHandler.handleDeleteCourse(request);
                case Constants.ACTION_OPEN_COURSE_REGISTRATION:
                    return courseHandler.handleOpenCourseRegistration(request);
                case Constants.ACTION_CLOSE_COURSE_REGISTRATION:
                    return courseHandler.handleCloseCourseRegistration(request);

                // Hành động giáo viên
                case Constants.ACTION_GET_ALL_TEACHERS:
                    return adminHandler.handleGetAllTeachers(request);
                case Constants.ACTION_SEARCH_TEACHERS:
                    return adminHandler.handleSearchTeachers(request);
                case Constants.ACTION_GET_COURSES_BY_TEACHER:
                    return teacherHandler.handleGetCoursesByTeacher(request);

                // Hành động khoa
                case Constants.ACTION_GET_ALL_FACULTIES:
                    return handleGetAllFaculties(request);

                // Hành động lớp
                case Constants.ACTION_GET_ALL_CLASSES:
                    return handleGetAllClasses(request);

                // Hành động môn học
                case Constants.ACTION_GET_ALL_SUBJECTS:
                    return subjectHandler.handleGetAllSubjects(request);
                case Constants.ACTION_SEARCH_SUBJECTS:
                    return subjectHandler.handleSearchSubjects(request);
                case Constants.ACTION_ADD_SUBJECT:
                    return subjectHandler.handleAddSubject(request);
                case Constants.ACTION_UPDATE_SUBJECT:
                    return subjectHandler.handleUpdateSubject(request);
                case Constants.ACTION_DELETE_SUBJECT:
                    return subjectHandler.handleDeleteSubject(request);

                // Hành động đăng ký học phần (bản ghi enrollment chính thức sau khi đăng ký
                // khóa học được duyệt)
                case Constants.ACTION_GET_ENROLLMENTS_BY_COURSE:
                    return teacherHandler.handleGetEnrollmentsByCourse(request);
                case Constants.ACTION_GET_ENROLLMENTS:
                    return enrollmentHandler.handleGetEnrollments(request);
                case Constants.ACTION_GET_STUDENT_GRADES:
                    return teacherHandler.handleGetStudentGrades(request);
                case Constants.ACTION_ENROLL_COURSE:
                    return enrollmentHandler.handleEnrollCourse(request);
                case Constants.ACTION_DROP_COURSE:
                    return enrollmentHandler.handleDropCourse(request);

                // Hành động điểm
                case Constants.ACTION_ADD_GRADE:
                    return teacherHandler.handleAddGrade(request);
                case Constants.ACTION_UPDATE_GRADE:
                    return teacherHandler.handleUpdateGrade(request);
                case Constants.ACTION_DELETE_GRADE:
                    return teacherHandler.handleDeleteGrade(request);
                case Constants.ACTION_GET_GRADES:
                    return teacherHandler.handleGetGrades(request);
                case Constants.ACTION_CALCULATE_FINAL_GRADE:
                    return teacherHandler.handleCalculateFinalGrade(request);

                // Hành động đồng bộ
                case Constants.ACTION_SYNC_CHECK:
                    return syncHandler.handleSyncCheck(request);
                case Constants.ACTION_DOWNLOAD_DATA:
                    return syncHandler.handleDownloadData(request);
                case Constants.ACTION_UPLOAD_USERS:
                    return syncHandler.handleUploadUsers(request);
                case Constants.ACTION_UPLOAD_FACULTIES:
                    return syncHandler.handleUploadFaculties(request);
                case Constants.ACTION_UPLOAD_CLASSES:
                    return syncHandler.handleUploadClasses(request);
                case Constants.ACTION_UPLOAD_STUDENTS:
                    return syncHandler.handleUploadStudents(request);
                case Constants.ACTION_UPLOAD_SUBJECTS:
                    return syncHandler.handleUploadSubjects(request);
                case Constants.ACTION_UPLOAD_COURSES:
                    return syncHandler.handleUploadCourses(request);
                case Constants.ACTION_UPLOAD_ENROLLMENTS:
                    return syncHandler.handleUploadEnrollments(request);
                case Constants.ACTION_UPLOAD_GRADES:
                    return syncHandler.handleUploadGrades(request);
                case Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS:
                    return syncHandler.handleUploadClassOpeningRequests(request);
                case Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS:
                    return syncHandler.handleUploadCourseRegistrations(request);
                case Constants.ACTION_UPLOAD_NOTIFICATIONS:
                    return syncHandler.handleUploadNotifications(request);

                // Hành động yêu cầu mở lớp
                case Constants.ACTION_GET_ALL_CLASS_REQUESTS:
                    return adminHandler.handleGetAllClassRequests(request);
                case Constants.ACTION_GET_CLASS_REQUEST_BY_ID:
                    return adminHandler.handleGetClassRequestById(request);
                case Constants.ACTION_GET_MY_CLASS_REQUESTS:
                    return teacherHandler.handleGetMyClassRequests(request);
                case Constants.ACTION_GET_PENDING_CLASS_REQUESTS:
                    return teacherHandler.handleGetPendingClassRequests(request);
                case Constants.ACTION_SUBMIT_CLASS_REQUEST:
                    return teacherHandler.handleSubmitClassRequest(request);
                case Constants.ACTION_UPDATE_CLASS_REQUEST:
                    return teacherHandler.handleUpdateClassRequest(request);
                case Constants.ACTION_CANCEL_CLASS_REQUEST:
                    return teacherHandler.handleCancelClassRequest(request);
                case Constants.ACTION_APPROVE_CLASS_REQUEST:
                    return adminHandler.handleApproveClassRequest(request);
                case Constants.ACTION_REJECT_CLASS_REQUEST:
                    return adminHandler.handleRejectClassRequest(request);
                case Constants.ACTION_GET_CLASS_REQUEST_STATS:
                    return adminHandler.handleGetClassRequestStats(request);

                // Hành động đăng ký khóa học
                case Constants.ACTION_GET_ALL_REGISTRATIONS:
                    return adminHandler.handleGetAllRegistrations(request);
                case Constants.ACTION_GET_REGISTRATION_BY_ID:
                    return adminHandler.handleGetRegistrationById(request);
                case Constants.ACTION_GET_MY_REGISTRATIONS:
                    return studentHandler.handleGetMyRegistrations(request);
                case Constants.ACTION_GET_COURSE_REGISTRATIONS:
                    return teacherHandler.handleGetCourseRegistrations(request);
                case Constants.ACTION_GET_PENDING_REGISTRATIONS:
                    return teacherHandler.handleGetPendingRegistrations(request);
                case Constants.ACTION_REGISTER_COURSE:
                    return studentHandler.handleRegisterCourse(request);
                case Constants.ACTION_CANCEL_REGISTRATION:
                    return studentHandler.handleCancelRegistration(request);
                case Constants.ACTION_APPROVE_REGISTRATION:
                    return teacherHandler.handleApproveRegistration(request);
                case Constants.ACTION_REJECT_REGISTRATION:
                    return teacherHandler.handleRejectRegistration(request);
                case Constants.ACTION_VALIDATE_REGISTRATION:
                    return adminHandler.handleValidateRegistration(request);
                case Constants.ACTION_GET_STUDENT_CREDITS:
                    return adminHandler.handleGetStudentCredits(request);
                case Constants.ACTION_GET_REGISTRATION_STATS:
                    return adminHandler.handleGetRegistrationStats(request);
                case Constants.ACTION_GET_COMPLETED_SUBJECT_CODES:
                    return studentHandler.handleGetCompletedSubjectCodes(request);

                // Hành động thông báo
                case Constants.ACTION_GET_NOTIFICATIONS:
                    return notificationHandler.handleGetNotifications(request);
                case Constants.ACTION_SEND_NOTIFICATION:
                    return notificationHandler.handleSendNotification(request);
                case Constants.ACTION_MARK_NOTIFICATION_READ:
                    return notificationHandler.handleMarkNotificationRead(request);

                // Hành động thời khóa biểu và bảng điểm
                case Constants.ACTION_GET_TIMETABLE:
                    return timetableHandler.handleGetTimetable(request);

                case Constants.ACTION_GET_TRANSCRIPT:
                    return studentHandler.handleGetTranscript(request);

                case Constants.ACTION_GET_SEMESTER_TRANSCRIPT:
                    return studentHandler.handleGetSemesterTranscript(request);

                case Constants.ACTION_GET_HONOR_STUDENTS:
                    return statisticsHandler.handleGetHonorStudents(request);

                case Constants.ACTION_GET_FACULTY_STATISTICS:
                    return statisticsHandler.handleGetFacultyStatistics(request);

                case Constants.ACTION_GET_GPA_TREND:
                    return statisticsHandler.handleGetGpaTrend(request);

                case Constants.ACTION_VALIDATE_SCHEDULE:
                    return timetableHandler.handleValidateSchedule(request);

                case Constants.ACTION_GET_SERVER_STATISTICS:
                    return statisticsHandler.handleGetServerStatistics(request);

                default:
                    return Message.createErrorResponse(action, "Không tìm thấy hành động: " + action);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xử lý hành động: " + action, e);
            return Message.createErrorResponse(action, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Xử lý đăng nhập
     */
    private Message handleLogin(Message request) {
        String username = request.getData(Constants.KEY_USERNAME, String.class);
        String password = request.getData(Constants.KEY_PASSWORD, String.class);

        if (username == null || password == null) {
            return Message.createErrorResponse(Constants.ACTION_LOGIN, Constants.MSG_INVALID_DATA);
        }

        // Sử dụng authenticateDetailed để có thông tin chi tiết lỗi
        AuthenticationResult result = authService.authenticateDetailed(username, password);

        if (result.isSuccess()) {
            User user = result.getUser();
            this.currentUser = user;

            // Cập nhật currentUser cho tất cả handlers
            updateHandlersCurrentUser(user);

            String clientIP = clientSocket.getRemoteSocketAddress().toString();
            authService.logLogin(user.getUsername(), clientIP, "Java Client", "success");

            Message response = Message.createSuccessResponse(Constants.ACTION_LOGIN, Constants.MSG_LOGIN_SUCCESS);
            response.addData(Constants.KEY_USER, user);

            LOGGER.info("Người dùng đăng nhập thành công: " + username);
            return response;
        } else {
            // Ghi log lần đăng nhập thất bại
            authService.logFailedLogin(username, clientSocket.getRemoteSocketAddress().toString());

            // Trả về message chi tiết dựa trên errorCode
            String errorMessage = result.getMessage();
            if (result.getErrorCode() != null) {
                switch (result.getErrorCode()) {
                    case AuthenticationResult.ERROR_ACCOUNT_DISABLED:
                        errorMessage = Constants.MSG_ACCOUNT_DISABLED;
                        break;
                    case AuthenticationResult.ERROR_USER_NOT_FOUND:
                    case AuthenticationResult.ERROR_INVALID_CREDENTIALS:
                        errorMessage = Constants.MSG_INVALID_CREDENTIALS;
                        break;
                    default:
                        errorMessage = result.getMessage();
                }
            }

            LOGGER.warning("Đăng nhập thất bại cho người dùng " + username + ": " + errorMessage);
            return Message.createErrorResponse(Constants.ACTION_LOGIN, errorMessage);
        }
    }

    /**
     * Xử lý đăng xuất
     */
    private Message handleLogout(Message request) {
        if (currentUser != null) {
            LOGGER.info("Người dùng đã đăng xuất: " + currentUser.getUsername());
            currentUser = null;
            updateHandlersCurrentUser(null);
        }
        return Message.createSuccessResponse(Constants.ACTION_LOGOUT, Constants.MSG_LOGOUT_SUCCESS);
    }

    /**
     * Cập nhật currentUser cho tất cả handlers
     */
    private void updateHandlersCurrentUser(User user) {
        if (studentHandler != null) {
            studentHandler.updateCurrentUser(user);
        }
        if (teacherHandler != null) {
            teacherHandler.updateCurrentUser(user);
        }
        if (adminHandler != null) {
            adminHandler.updateCurrentUser(user);
        }
        if (courseHandler != null) {
            courseHandler.updateCurrentUser(user);
        }
        if (subjectHandler != null) {
            subjectHandler.updateCurrentUser(user);
        }
        if (enrollmentHandler != null) {
            enrollmentHandler.updateCurrentUser(user);
        }
        if (notificationHandler != null) {
            notificationHandler.updateCurrentUser(user);
        }
        if (timetableHandler != null) {
            timetableHandler.updateCurrentUser(user);
        }
        if (statisticsHandler != null) {
            statisticsHandler.updateCurrentUser(user);
        }
        if (syncHandler != null) {
            syncHandler.updateCurrentUser(user);
        }
    }

    /**
     * Xử lý đổi mật khẩu
     */
    private Message handleChangePassword(Message request) {
        String newPassword = request.getData(Constants.KEY_PASSWORD, String.class);

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_CHANGE_PASSWORD, Constants.MSG_INVALID_DATA);
        }

        boolean success = authService.changePassword(currentUser.getUsername(), newPassword);
        if (success) {
            return Message.createSuccessResponse(Constants.ACTION_CHANGE_PASSWORD, "Đổi mật khẩu thành công");
        } else {
            return Message.createErrorResponse(Constants.ACTION_CHANGE_PASSWORD, "Đổi mật khẩu thất bại");
        }
    }

    /**
     * Gửi phản hồi cho client
     */
    private void sendResponse(Message response) {
        try {
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi phản hồi cho client", e);
        }
    }

    /**
     * Kiểm tra xem action có cần xác thực không
     */
    private boolean isPublicAction(String action) {
        return Constants.ACTION_LOGIN.equals(action) ||
                Constants.ACTION_SYNC_CHECK.equals(action);
    }

    /**
     * Lấy server version từ database
     */
    private int getServerVersion() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT CAST(config_value AS UNSIGNED) as version " +
                    "FROM system_config WHERE config_key = 'db_version'";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                int version = rs.getInt("version");
                return version;
            }

            // Nếu chưa có, tạo version ban đầu
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO system_config (config_key, config_value, description) " +
                            "VALUES ('db_version', '1', 'Database version')");
            stmt.executeUpdate();
            return 1;

        } catch (Exception e) {
            LOGGER.warning("Error getting server version: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Helper method để tạo message thống nhất cho upload results
     */
    private String createUploadMessage(String entityName, int successCount, int failCount) {
        if (failCount == 0) {
            return String.format("Uploaded %d %s successfully", successCount, entityName);
        } else if (successCount == 0) {
            return String.format("Uploaded 0 %s successfully, %d failed", entityName, failCount);
        } else {
            return String.format("Uploaded %d %s successfully, %d failed", successCount, entityName, failCount);
        }
    }

    /**
     * Xử lý upload students từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadStudents(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Student> students = (List<com.university.sms.model.Student>) request
                    .getData("students");

            if (students == null || students.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS, "No students to upload");
            }

            LOGGER.info("Uploading " + students.size() + " students from client");

            // Lưu từng student vào database
            int successCount = 0;
            int failCount = 0;

            com.university.sms.dao.StudentDAO studentDAO = new com.university.sms.dao.StudentDAO();
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
            for (com.university.sms.model.Student student : students) {
                try {
                    // Đảm bảo user tồn tại dựa trên username
                    String username = student.getUsername();
                    boolean userOk = true;
                    if (username == null || username.isEmpty()) {
                        // Dự phòng: sử dụng studentCode làm username nếu chưa được set
                        username = student.getStudentCode();
                        student.setUsername(username);
                    }

                    // Kiểm tra user có tồn tại theo username
                    com.university.sms.model.User existingUser = userDAO.findByUsername(username);
                    if (existingUser == null) {
                        // Tạo user tối thiểu từ thông tin sinh viên
                        com.university.sms.model.User u = new com.university.sms.model.User();
                        u.setUsername(username);
                        u.setPassword("password");
                        u.setFullName(student.getFullName());
                        u.setEmail(student.getEmail());
                        u.setPhone(student.getPhone());
                        u.setAddress(student.getAddress());
                        u.setRole(com.university.sms.model.User.UserRole.STUDENT);
                        userOk = userDAO.addUser(u);
                        if (userOk) {
                            saveDataOrigin("user", u.getUserId(), clientSource);
                        }
                    }

                    // Đảm bảo lớp tồn tại (nếu có)
                    String classCode = student.getClassCode();
                    boolean classOk = true;
                    if (classCode != null && !classCode.isEmpty()) {
                        com.university.sms.model.Class existingClass = classDAO.findByCode(classCode);
                        if (existingClass == null) {
                            classOk = false;
                            LOGGER.warning(
                                    "Class code not found: " + classCode + " for student " + student.getStudentCode());
                        }
                    }

                    // Đảm bảo khoa tồn tại
                    String facultyCode = student.getFacultyCode();
                    boolean facultyOk = true;
                    if (facultyCode != null && !facultyCode.isEmpty()) {
                        com.university.sms.model.Faculty existingFaculty = facultyDAO.findByCode(facultyCode);
                        if (existingFaculty == null) {
                            facultyOk = false;
                            LOGGER.warning("Faculty code not found: " + facultyCode + " for student "
                                    + student.getStudentCode());
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.Student existing = studentDAO.findByStudentCode(student.getStudentCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        if (userOk && classOk && facultyOk && studentDAO.addStudent(student)) {
                            saveDataOrigin("student", student.getStudentId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning("Failed to save student: " + student.getStudentCode());
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("Student already exists, skipping: " + student.getStudentCode() + " (source: "
                                + clientSource + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên sinh viên: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("students", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_STUDENTS, message);

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên sinh viên: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload courses từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadCourses(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Course> courses = (List<com.university.sms.model.Course>) request
                    .getData("courses");

            if (courses == null || courses.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES, "No courses to upload");
            }

            LOGGER.info("Uploading " + courses.size() + " courses from client");

            // Lưu từng course vào database
            int successCount = 0;
            int failCount = 0;

            com.university.sms.dao.CourseDAO courseDAO = new com.university.sms.dao.CourseDAO();
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.dao.SubjectDAO subjectDAO = new com.university.sms.dao.SubjectDAO();
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            for (com.university.sms.model.Course course : courses) {
                try {
                    // Đảm bảo môn học tồn tại
                    String subjectCode = course.getSubjectCode();
                    boolean subjectOk = true;
                    if (subjectCode != null && !subjectCode.isEmpty()) {
                        com.university.sms.model.Subject existingSubject = subjectDAO.findByCode(subjectCode);
                        if (existingSubject == null) {
                            subjectOk = false;
                            LOGGER.warning(
                                    "Subject code not found: " + subjectCode + " for course " + course.getCourseCode());
                        }
                    }

                    // Đảm bảo lớp tồn tại (nếu có)
                    String classCode = course.getClassCode();
                    boolean classOk = true;
                    if (classCode != null && !classCode.isEmpty()) {
                        com.university.sms.model.Class existingClass = classDAO.findByCode(classCode);
                        if (existingClass == null) {
                            classOk = false;
                            LOGGER.warning(
                                    "Class code not found: " + classCode + " for course " + course.getCourseCode());
                        }
                    }

                    // Đảm bảo giáo viên tồn tại dựa trên username
                    String teacherUsername = course.getTeacherUsername();
                    boolean userOk = true;
                    if (teacherUsername != null && !teacherUsername.isEmpty()) {
                        // Kiểm tra giáo viên có tồn tại theo username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(teacherUsername);
                        if (existingUser == null) {
                            // Tạo user giáo viên tối thiểu từ thông tin khóa học
                            // Thử lấy faculty_code từ môn học nếu có
                            String facultyCode = null;
                            if (subjectCode != null && !subjectCode.isEmpty()) {
                                com.university.sms.model.Subject subject = subjectDAO.findByCode(subjectCode);
                                if (subject != null) {
                                    facultyCode = subject.getFacultyCode();
                                }
                            }

                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(teacherUsername);
                            u.setPassword("password");
                            u.setFullName(course.getTeacherName() != null ? course.getTeacherName() : teacherUsername);
                            u.setEmail(teacherUsername + "@csv-teacher.edu.vn"); // Tạo email từ username
                            u.setRole(com.university.sms.model.User.UserRole.TEACHER);
                            u.setFacultyCode(facultyCode); // Đặt faculty_code nếu có
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            } else {
                                LOGGER.warning("Không thể tạo user giáo viên: " + teacherUsername +
                                        " (có thể trùng email/phone)");
                            }
                        } else {
                            // User đã tồn tại, không sao
                            LOGGER.info("User giáo viên đã tồn tại: " + teacherUsername);
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.Course existing = courseDAO.findByCourseCode(course.getCourseCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        if (subjectOk && classOk && userOk && courseDAO.addCourse(course)) {
                            if (course.getCourseId() > 0) {
                                saveDataOrigin("course", course.getCourseId(), clientSource);
                                successCount++;
                            } else {
                                failCount++;
                                LOGGER.warning("Failed to save course: " + course.getCourseCode() + " - ID not set");
                            }
                        } else {
                            failCount++;
                            LOGGER.warning("Failed to save course: " + course.getCourseCode() + " - "
                                    + (course.getCourseName() != null ? course.getCourseName() : "") +
                                    (subjectOk ? "" : " (subject not found)") +
                                    (classOk ? "" : " (class not found)") +
                                    (userOk ? "" : " (teacher user not found/created)"));
                        }
                    } else {
                        // Đã tồn tại → Không đếm vào successCount hoặc failCount, chỉ log
                        LOGGER.info("Course already exists, skipping: " + course.getCourseCode() + " (source: "
                                + clientSource + ")");
                        // Không đếm vào successCount hoặc failCount
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên khóa học " + course.getCourseCode() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("courses", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_COURSES, message);

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên khóa học: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload enrollments từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadEnrollments(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Enrollment> enrollments = (List<com.university.sms.model.Enrollment>) request
                    .getData("enrollments");

            if (enrollments == null || enrollments.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
                        "No enrollments to upload");
            }

            LOGGER.info("Uploading " + enrollments.size() + " enrollments from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            com.university.sms.dao.StudentDAO studentDAO = new com.university.sms.dao.StudentDAO();
            com.university.sms.dao.CourseDAO courseDAO = new com.university.sms.dao.CourseDAO();

            for (com.university.sms.model.Enrollment e : enrollments) {
                try {
                    // Validate FK student_code
                    boolean studentOk = true;
                    if (e.getStudentCode() != null && !e.getStudentCode().trim().isEmpty()) {
                        com.university.sms.model.Student existingStudent = studentDAO
                                .findByStudentCode(e.getStudentCode().trim());
                        if (existingStudent == null) {
                            studentOk = false;
                            LOGGER.warning("Student code not found: " + e.getStudentCode() + " for enrollment");
                        }
                    }

                    // Validate FK course_code
                    boolean courseOk = true;
                    if (e.getCourseCode() != null && !e.getCourseCode().trim().isEmpty()) {
                        com.university.sms.model.Course existingCourse = courseDAO
                                .findByCourseCode(e.getCourseCode().trim());
                        if (existingCourse == null) {
                            courseOk = false;
                            LOGGER.warning("Course code not found: " + e.getCourseCode() + " for enrollment");
                        }
                    }

                    if (!studentOk || !courseOk) {
                        failCount++;
                        LOGGER.warning("Failed to save enrollment: studentCode=" + e.getStudentCode() +
                                ", courseCode=" + e.getCourseCode() + " (FK validation failed)");
                        continue;
                    }

                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh trùng lặp)
                    // Check duplicate dựa trên student_code, course_code (có UNIQUE constraint)
                    boolean exists = false;
                    try {
                        String checkSql = "SELECT COUNT(*) FROM enrollments WHERE " +
                                "student_code = ? AND course_code = ?";
                        try (java.sql.Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                                java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                            checkStmt.setString(1, e.getStudentCode());
                            checkStmt.setString(2, e.getCourseCode());

                            try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    exists = true;
                                    LOGGER.info("Enrollment already exists, using existing: student="
                                            + e.getStudentCode() +
                                            ", course=" + e.getCourseCode() + " (source: " + clientSource + ")");
                                }
                            }
                        }
                    } catch (Exception checkEx) {
                        LOGGER.warning("Error checking enrollment duplicate: " + checkEx.getMessage());
                        // Nếu kiểm tra lỗi, vẫn tiếp tục insert (không chặn)
                    }

                    if (!exists) {
                        // Đặt lại ID để database tự tăng, không giữ ID từ CSV
                        e.setEnrollmentId(0);
                        boolean ok = enrollmentDAO.save(e);

                        if (ok && e.getEnrollmentId() > 0) {
                            saveDataOrigin("enrollment", e.getEnrollmentId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning("Failed to save enrollment: studentCode=" + e.getStudentCode() +
                                    ", courseCode=" + e.getCourseCode());
                        }
                    } else {
                        // Đã tồn tại → Không đếm vào successCount hoặc failCount, chỉ log
                        // Không đếm vào successCount hoặc failCount
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi lưu đăng ký học phần: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("enrollments", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_ENROLLMENTS, message);

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên đăng ký học phần: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_ENROLLMENTS,
                    "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload users từ client (phục vụ CSV -> tạo trước user để students có FK
     * hợp lệ)
     */
    @SuppressWarnings("unused")
    private Message handleUploadUsers(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.User> users = (List<com.university.sms.model.User>) request
                    .getData("users");

            if (users == null || users.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS, "No users to upload");
            }

            LOGGER.info("Uploading " + users.size() + " users from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();

            for (com.university.sms.model.User u : users) {
                try {
                    // Validate FK faculty_code nếu có
                    boolean facultyOk = true;
                    if (u.getFacultyCode() != null && !u.getFacultyCode().trim().isEmpty()) {
                        com.university.sms.model.Faculty existingFaculty = facultyDAO
                                .findByCode(u.getFacultyCode().trim());
                        if (existingFaculty == null) {
                            facultyOk = false;
                            LOGGER.warning(
                                    "Faculty code not found: " + u.getFacultyCode() + " for user " + u.getUsername());
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.User existing = userDAO.findByUsername(u.getUsername());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        u.setUserId(0); // Đặt lại ID để database tự tăng
                        if (facultyOk && userDAO.addUser(u)) {
                            saveDataOrigin("user", u.getUserId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning("Failed to save user: " + u.getUsername() +
                                    (facultyOk ? " (check email/phone uniqueness)" : " (faculty_code not found)"));
                        }
                    } else {
                        // Đã tồn tại → Không đếm vào successCount hoặc failCount, chỉ log
                        LOGGER.info(
                                "User already exists, skipping: " + u.getUsername() + " (source: " + clientSource
                                        + ")");
                        // Không đếm vào successCount hoặc failCount
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên người dùng " + u.getUsername() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("users", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_USERS, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên người dùng: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload faculties từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadFaculties(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Faculty> faculties = (List<com.university.sms.model.Faculty>) request
                    .getData("faculties");

            if (faculties == null || faculties.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES, "No faculties to upload");
            }

            LOGGER.info("Uploading " + faculties.size() + " faculties from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();

            for (com.university.sms.model.Faculty f : faculties) {
                try {
                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.Faculty existing = facultyDAO.findByCode(f.getFacultyCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        f.setFacultyId(0); // Đặt lại ID để database tự tăng
                        if (facultyDAO.addFaculty(f)) {
                            saveDataOrigin("faculty", f.getFacultyId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning(
                                    "Failed to save faculty: " + f.getFacultyCode() + " - " + f.getFacultyName());
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("Faculty already exists, skipping: " + f.getFacultyCode() + " (source: "
                                + clientSource + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên khoa " + f.getFacultyCode() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("faculties", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_FACULTIES, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên khoa: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_FACULTIES, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload classes từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadClasses(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Class> classes = (List<com.university.sms.model.Class>) request
                    .getData("classes");

            if (classes == null || classes.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES, "No classes to upload");
            }

            LOGGER.info("Uploading " + classes.size() + " classes from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();

            for (com.university.sms.model.Class c : classes) {
                try {
                    // Đảm bảo khoa tồn tại
                    String facultyCode = c.getFacultyCode();
                    boolean facultyOk = true;
                    if (facultyCode != null && !facultyCode.isEmpty()) {
                        com.university.sms.model.Faculty existingFaculty = facultyDAO.findByCode(facultyCode);
                        if (existingFaculty == null) {
                            facultyOk = false;
                            LOGGER.warning("Faculty code not found: " + facultyCode + " for class " + c.getClassCode());
                        }
                    }

                    // Đảm bảo giáo viên tồn tại dựa trên username
                    String teacherUsername = c.getTeacherUsername();
                    boolean userOk = true;
                    if (teacherUsername != null && !teacherUsername.isEmpty()) {
                        // Kiểm tra giáo viên có tồn tại theo username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(teacherUsername);
                        if (existingUser == null) {
                            // Tạo user giáo viên tối thiểu từ thông tin lớp
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(teacherUsername);
                            u.setPassword("password");
                            u.setFullName(teacherUsername); // Model Class không có tên giáo viên, sử dụng username
                            u.setEmail(teacherUsername + "@csv-teacher.edu.vn"); // Tạo email từ username
                            u.setRole(com.university.sms.model.User.UserRole.TEACHER);
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.Class existing = classDAO.findByCode(c.getClassCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        c.setClassId(0); // Đặt lại ID để database tự tăng
                        if (facultyOk && userOk && classDAO.save(c)) {
                            saveDataOrigin("class", c.getClassId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning("Failed to save class: " + c.getClassCode() + " - " + c.getClassName());
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("Class already exists, skipping: " + c.getClassCode() + " (source: " + clientSource
                                + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên lớp " + c.getClassCode() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("classes", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_CLASSES, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên lớp: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASSES, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload subjects từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadSubjects(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Subject> subjects = (List<com.university.sms.model.Subject>) request
                    .getData("subjects");

            if (subjects == null || subjects.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS, "No subjects to upload");
            }

            LOGGER.info("Uploading " + subjects.size() + " subjects from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.SubjectDAO subjectDAO = new com.university.sms.dao.SubjectDAO();
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();

            for (com.university.sms.model.Subject s : subjects) {
                try {
                    // Đảm bảo khoa tồn tại
                    String facultyCode = s.getFacultyCode();
                    boolean facultyOk = true;
                    if (facultyCode != null && !facultyCode.isEmpty()) {
                        com.university.sms.model.Faculty existingFaculty = facultyDAO.findByCode(facultyCode);
                        if (existingFaculty == null) {
                            facultyOk = false;
                            LOGGER.warning(
                                    "Faculty code not found: " + facultyCode + " for subject " + s.getSubjectCode());
                        }
                    }

                    // Đảm bảo môn học tiên quyết tồn tại (nếu có)
                    String prerequisiteCode = s.getPrerequisiteSubjectCode();
                    boolean prerequisiteOk = true;
                    if (prerequisiteCode != null && !prerequisiteCode.isEmpty()) {
                        com.university.sms.model.Subject existingPrereq = subjectDAO.findByCode(prerequisiteCode);
                        if (existingPrereq == null) {
                            prerequisiteOk = false;
                            LOGGER.warning("Prerequisite subject code not found: " + prerequisiteCode + " for subject "
                                    + s.getSubjectCode());
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè dữ liệu từ nguồn
                    // khác)
                    com.university.sms.model.Subject existing = subjectDAO.findByCode(s.getSubjectCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        s.setSubjectId(0); // Đặt lại ID để database tự tăng
                        if (facultyOk && prerequisiteOk && subjectDAO.save(s)) {
                            saveDataOrigin("subject", s.getSubjectId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning(
                                    "Failed to save subject: " + s.getSubjectCode() + " - " + s.getSubjectName());
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("Subject already exists, skipping: " + s.getSubjectCode() + " (source: "
                                + clientSource + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên môn học " + s.getSubjectCode() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("subjects", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_SUBJECTS, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên môn học: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_SUBJECTS, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload grades từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadGrades(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Grade> grades = (List<com.university.sms.model.Grade>) request
                    .getData("grades");

            if (grades == null || grades.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES, "No grades to upload");
            }

            LOGGER.info("Uploading " + grades.size() + " grades from client");

            int successCount = 0;
            int failCount = 0;
            com.university.sms.dao.GradeDAO gradeDAO = new com.university.sms.dao.GradeDAO();

            for (com.university.sms.model.Grade g : grades) {
                try {
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh trùng lặp)
                    // Kiểm tra trùng lặp dựa trên student_code, course_code, grade_type, grade_name
                    // (vì grades không có UNIQUE constraint)
                    boolean exists = false;
                    try {
                        String checkSql = "SELECT COUNT(*) FROM grades WHERE " +
                                "student_code = ? AND course_code = ? AND grade_type = ? " +
                                "AND (grade_name = ? OR (grade_name IS NULL AND ? IS NULL))";
                        try (java.sql.Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                                java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                            checkStmt.setString(1, g.getStudentCode());
                            checkStmt.setString(2, g.getCourseCode());
                            checkStmt.setString(3, g.getGradeType().name().toLowerCase());
                            if (g.getGradeName() != null && !g.getGradeName().isEmpty()) {
                                checkStmt.setString(4, g.getGradeName());
                                checkStmt.setString(5, g.getGradeName());
                            } else {
                                checkStmt.setNull(4, java.sql.Types.VARCHAR);
                                checkStmt.setNull(5, java.sql.Types.VARCHAR);
                            }

                            try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    exists = true;
                                    LOGGER.info("Grade already exists, skipping: student=" + g.getStudentCode() +
                                            ", course=" + g.getCourseCode() + ", type=" + g.getGradeType() +
                                            ", name=" + g.getGradeName() + " (source: " + clientSource + ")");
                                }
                            }
                        }
                    } catch (Exception checkEx) {
                        LOGGER.warning("Error checking grade duplicate: " + checkEx.getMessage());
                        // Nếu kiểm tra lỗi, vẫn tiếp tục insert (không chặn)
                    }

                    if (!exists) {
                        // Đặt lại ID để database tự tăng, không giữ ID từ CSV
                        g.setGradeId(0);
                        if (gradeDAO.save(g)) {
                            saveDataOrigin("grade", g.getGradeId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                    // Nếu đã tồn tại, không đếm vào successCount hoặc failCount, chỉ log
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên điểm: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("grades", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_GRADES, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên grades: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES, "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload class opening requests từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadClassOpeningRequests(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.ClassOpeningRequest> requests = (List<com.university.sms.model.ClassOpeningRequest>) request
                    .getData("requests");

            if (requests == null || requests.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
                        "No class opening requests to upload");
            }

            LOGGER.info("Uploading " + requests.size() + " class opening requests from client");

            int successCount = 0;
            int failCount = 0;
            ClassOpeningRequestService service = new ClassOpeningRequestService();

            com.university.sms.dao.ClassOpeningRequestDAO requestDAO = new com.university.sms.dao.ClassOpeningRequestDAO();

            for (com.university.sms.model.ClassOpeningRequest r : requests) {
                try {
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh trùng lặp)
                    // Kiểm tra trùng lặp dựa trên teacher, subject, academic_year, semester,
                    // schedule_day, schedule_time
                    List<com.university.sms.model.ClassOpeningRequest> existingRequests = requestDAO
                            .findByTeacher(r.getTeacherUsername());
                    boolean exists = false;
                    for (com.university.sms.model.ClassOpeningRequest existing : existingRequests) {
                        if (r.getSubjectCode() != null && r.getSubjectCode().equals(existing.getSubjectCode()) &&
                                r.getAcademicYear() != null && r.getAcademicYear().equals(existing.getAcademicYear()) &&
                                r.getSemester() == existing.getSemester() &&
                                ((r.getScheduleDay() == null && existing.getScheduleDay() == null) ||
                                        (r.getScheduleDay() != null
                                                && r.getScheduleDay().equals(existing.getScheduleDay())))
                                &&
                                ((r.getScheduleTime() == null && existing.getScheduleTime() == null) ||
                                        (r.getScheduleTime() != null
                                                && r.getScheduleTime().equals(existing.getScheduleTime())))) {
                            exists = true;
                            LOGGER.info(
                                    "ClassOpeningRequest already exists, skipping: teacher=" + r.getTeacherUsername() +
                                            ", subject=" + r.getSubjectCode() + ", year=" + r.getAcademicYear() +
                                            ", semester=" + r.getSemester() + " (source: " + clientSource + ")");
                            break;
                        }
                    }

                    if (!exists) {
                        // Đặt lại ID để database tự tăng, không giữ ID từ CSV
                        r.setRequestId(0);
                        if (service.submitRequest(r)) {
                            saveDataOrigin("class_opening_request", r.getRequestId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                    // Nếu đã tồn tại, không đếm vào successCount hoặc failCount, chỉ log
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên class opening request: " + ex.getMessage());
                }
            }

            String message = createUploadMessage("class opening requests", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên class opening requests: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS,
                    "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload course registrations từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadCourseRegistrations(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.CourseRegistration> registrations = (List<com.university.sms.model.CourseRegistration>) request
                    .getData("registrations");

            if (registrations == null || registrations.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
                        "No course registrations to upload");
            }

            LOGGER.info("Uploading " + registrations.size() + " course registrations from client");

            int successCount = 0;
            int failCount = 0;
            CourseRegistrationDAO dao = new CourseRegistrationDAO();

            for (com.university.sms.model.CourseRegistration r : registrations) {
                try {
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh trùng lặp)
                    // Check duplicate dựa trên student_code, course_code (có UNIQUE constraint)
                    boolean exists = false;
                    try {
                        String checkSql = "SELECT COUNT(*) FROM course_registrations WHERE " +
                                "student_code = ? AND course_code = ?";
                        try (java.sql.Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                                java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                            checkStmt.setString(1, r.getStudentCode());
                            checkStmt.setString(2, r.getCourseCode());

                            try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    exists = true;
                                    LOGGER.info("CourseRegistration already exists, skipping: student="
                                            + r.getStudentCode() +
                                            ", course=" + r.getCourseCode() + " (source: " + clientSource + ")");
                                }
                            }
                        }
                    } catch (Exception checkEx) {
                        LOGGER.warning("Error checking course registration duplicate: " + checkEx.getMessage());
                        // Nếu kiểm tra lỗi, vẫn tiếp tục insert (không chặn)
                    }

                    if (!exists) {
                        // Đặt lại ID để database tự tăng, không giữ ID từ CSV
                        r.setRegistrationId(0);
                        if (dao.save(r)) {
                            saveDataOrigin("course_registration", r.getRegistrationId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                    // Nếu đã tồn tại, không đếm vào successCount hoặc failCount, chỉ log
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên course registration: " + ex.getMessage());
                }
            }

            String message = createUploadMessage("course registrations", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên course registrations: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS,
                    "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload notifications từ client
     */
    @SuppressWarnings("unused")
    private Message handleUploadNotifications(Message request) {
        try {
            // Chỉ admin mới được upload
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
                        "Không có quyền truy cập");
            }

            @SuppressWarnings("unchecked")
            List<com.university.sms.model.Notification> notifications = (List<com.university.sms.model.Notification>) request
                    .getData("notifications");

            if (notifications == null || notifications.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
                        "No notifications to upload");
            }

            LOGGER.info("Uploading " + notifications.size() + " notifications from client");

            int successCount = 0;
            int failCount = 0;
            NotificationService service = new NotificationService();
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();

            for (com.university.sms.model.Notification n : notifications) {
                try {
                    // Đảm bảo người gửi tồn tại dựa trên username
                    String senderUsername = n.getSenderUsername();
                    boolean userOk = true;
                    if (senderUsername != null && !senderUsername.isEmpty()) {
                        // Kiểm tra người gửi có tồn tại theo username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(senderUsername);
                        if (existingUser == null) {
                            // Tạo user tối thiểu từ thông tin thông báo
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(senderUsername);
                            u.setPassword("password");
                            u.setFullName(n.getSenderName() != null ? n.getSenderName() : senderUsername);
                            u.setEmail(senderUsername + "@csv-admin.edu.vn"); // Tạo email từ username
                            u.setRole(com.university.sms.model.User.UserRole.ADMIN); // Mặc định là ADMIN cho thông báo
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh trùng lặp)
                    // Kiểm tra trùng lặp dựa trên title, content, sender_username, target_type,
                    // target_code
                    boolean exists = false;
                    try {
                        // Truy vấn để kiểm tra trùng lặp
                        String checkSql = "SELECT COUNT(*) FROM notifications WHERE " +
                                "title = ? AND content = ? AND sender_username = ? AND target_type = ? " +
                                "AND (target_code = ? OR (target_code IS NULL AND ? IS NULL))";
                        try (java.sql.Connection conn = com.university.sms.util.DatabaseConnection.getConnection();
                                java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                            checkStmt.setString(1, n.getTitle());
                            checkStmt.setString(2, n.getContent());
                            checkStmt.setString(3, n.getSenderUsername());
                            checkStmt.setString(4, n.getTargetType().name().toLowerCase());
                            if (n.getTargetCode() != null && !n.getTargetCode().isEmpty()) {
                                checkStmt.setString(5, n.getTargetCode());
                                checkStmt.setString(6, n.getTargetCode());
                            } else {
                                checkStmt.setNull(5, java.sql.Types.VARCHAR);
                                checkStmt.setNull(6, java.sql.Types.VARCHAR);
                            }

                            try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    exists = true;
                                    LOGGER.info("Notification already exists, skipping: title=" + n.getTitle() +
                                            ", sender=" + n.getSenderUsername() + " (source: " + clientSource + ")");
                                }
                            }
                        }
                    } catch (Exception checkEx) {
                        LOGGER.warning("Error checking notification duplicate: " + checkEx.getMessage());
                        // Nếu kiểm tra lỗi, vẫn tiếp tục insert (không chặn)
                    }

                    if (!exists && userOk) {
                        // Đặt lại ID để database tự tăng, không giữ ID từ CSV
                        n.setNotificationId(0);
                        if (service.createNotification(n)) {
                            saveDataOrigin("notification", n.getNotificationId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                            LOGGER.warning(
                                    "Failed to save notification: " + n.getNotificationId() + " - " + n.getTitle());
                        }
                    }
                    // Nếu đã tồn tại, không đếm vào successCount hoặc failCount, chỉ log
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên thông báo: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = createUploadMessage("notifications", successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên notifications: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_NOTIFICATIONS,
                    "Error: " + e.getMessage());
        }
    }

    /**
     * Lưu thông tin nguồn dữ liệu vào bảng data_origin
     * Chỉ insert nếu chưa có, không ghi đè source đã tồn tại (giữ source đầu tiên)
     * Nếu source đã tồn tại và trùng với source hiện tại, cập nhật timestamp để
     * version tăng
     */
    @Override
    public void saveDataOrigin(String entityType, int entityId, String source) {
        if (entityId <= 0)
            return;

        // Kiểm tra xem đã có source chưa
        String existingSource = getDataOrigin(entityType, entityId);
        if (existingSource != null) {
            // Đã có source
            if (existingSource.equals(source)) {
                // Nếu source trùng, cập nhật timestamp để version tăng
                updateDataOriginTimestamp(entityType, entityId);
            } else {
                // Source khác: cập nhật timestamp của existingSource để version của source đó
                // tăng
                // Ví dụ: CSV client sửa data có source POSTGRES → cập nhật timestamp POSTGRES
                // Không thay đổi source, chỉ cập nhật timestamp
                updateDataOriginTimestamp(entityType, entityId);
                LOGGER.fine("Data origin already exists for " + entityType + "#" + entityId +
                        " with source: " + existingSource + ", updating timestamp but keeping original source");
            }
            return;
        }

        // Chưa có source, insert mới (updated_at sẽ tự động = NOW() do DEFAULT
        // CURRENT_TIMESTAMP)
        String sql = "INSERT INTO data_origin (entity_type, entity_id, source, updated_at) VALUES (?, ?, ?, NOW())";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setInt(2, entityId);
            stmt.setString(3, source != null ? source : "UNKNOWN");
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving data origin: " + entityType + "#" + entityId + " -> " + source, e);
        }
    }

    /**
     * Cập nhật timestamp của data_origin để version tăng khi có thay đổi từ regular
     * client
     */
    @Override
    public void updateDataOriginTimestamp(String entityType, int entityId) {
        updateDataOriginTimestampWithResult(entityType, entityId);
    }

    /**
     * Cập nhật timestamp của data_origin và trả về số dòng được cập nhật
     */
    private int updateDataOriginTimestampWithResult(String entityType, int entityId) {
        if (entityId <= 0)
            return 0;
        String sql = "UPDATE data_origin SET updated_at = NOW() WHERE entity_type = ? AND entity_id = ?";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setInt(2, entityId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                LOGGER.warning(
                        "Không tìm thấy data_origin record để cập nhật timestamp cho " + entityType + "#" + entityId);
            } else {
                LOGGER.fine("Đã cập nhật timestamp cho " + entityType + "#" + entityId);
            }
            return updated;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi khi cập nhật data origin timestamp: " + entityType + "#" + entityId, e);
            return 0;
        }
    }

    /**
     * Helper method: Lấy source hiện tại của entity
     */
    @Override
    public String getDataOrigin(String entityType, int entityId) {
        String sql = "SELECT source FROM data_origin WHERE entity_type = ? AND entity_id = ?";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setInt(2, entityId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("source");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting data origin: " + entityType + "#" + entityId, e);
        }
        return null;
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa
     */
    private boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Ngắt kết nối client
     */
    public void disconnect() {
        isConnected = false;

        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing client connection", e);
        }

        if (currentUser != null) {
            LOGGER.info("Client disconnected: " + currentUser.getUsername());
        }
    }

    // ========== CLASS OPENING REQUEST HANDLERS ==========

    // ========== COURSE REGISTRATION HANDLERS ==========

    // ==================== Teacher Management Handlers ====================

    // ==================== User Activation Handler ====================

    // ==================== Faculty Handlers ====================

    private Message handleGetAllFaculties(Message request) {
        try {
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
            List<com.university.sms.model.Faculty> faculties = facultyDAO.findAll();

            Message response = Message.createSuccessResponse(request.getAction(), "Lấy danh sách khoa thành công");
            response.addData("faculties", faculties);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting faculties", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // ==================== Class Handlers ====================

    private Message handleGetAllClasses(Message request) {
        try {
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            List<com.university.sms.model.Class> classes = classDAO.findAll();

            Message response = Message.createSuccessResponse(request.getAction(), "Lấy danh sách lớp thành công");
            response.addData(Constants.KEY_CLASSES, classes);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting classes", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isConnected && !clientSocket.isClosed();
    }
}
