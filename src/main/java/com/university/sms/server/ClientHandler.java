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

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
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
import com.university.sms.server.handler.PostgresHandler;
import com.university.sms.server.handler.CsvHandler;

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
    private CsvHandler csvHandler;
    private PostgresHandler postgresHandler;

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
                () -> this.clientSource,
                this // DataOriginHelper
        );
        this.teacherHandler = new TeacherHandler(
                classRequestService,
                registrationService,
                gradeService,
                null, // currentUser sẽ được set sau khi login
                () -> this.clientSource,
                this // DataOriginHelper
        );
        this.adminHandler = new AdminHandler(
                null, // currentUser sẽ được set sau khi login
                () -> this.clientSource,
                this, // DataOriginHelper
                studentService,
                classRequestService,
                registrationService);
        this.courseHandler = new CourseHandler(
                null, // currentUser sẽ được set sau khi login
                () -> this.clientSource,
                this, // DataOriginHelper
                courseService);
        this.subjectHandler = new SubjectHandler(
                null, // currentUser sẽ được set sau khi login
                () -> this.clientSource,
                this, // DataOriginHelper
                subjectService);
        this.enrollmentHandler = new EnrollmentHandler(
                null, // currentUser sẽ được set sau khi login
                () -> this.clientSource,
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
        this.csvHandler = new CsvHandler(
                studentService,
                courseService,
                classRequestService,
                notificationService,
                this,
                source -> this.clientSource = source);
        this.postgresHandler = new PostgresHandler(
                studentService,
                courseService,
                classRequestService,
                notificationService,
                this,
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
                case Constants.ACTION_GET_AVAILABLE_CLASS_TEACHERS:
                    return adminHandler.handleGetAvailableClassTeachers(request);
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
                case Constants.ACTION_GET_AVAILABLE_CLASSES:
                    return handleGetAvailableClasses(request);
                case Constants.ACTION_ADD_CLASS:
                    return handleAddClass(request);
                case Constants.ACTION_UPDATE_CLASS:
                    return handleUpdateClass(request);

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
                    return routeSyncCheck(request);
                case Constants.ACTION_DOWNLOAD_DATA:
                    if (isPostgresClient()) {
                        return postgresHandler.handleDownloadData(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleDownloadData(request);
                    }
                    return csvHandler.handleDownloadData(request);
                case Constants.ACTION_UPLOAD_USERS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadUsers(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadUsers(request);
                    }
                    return csvHandler.handleUploadUsers(request);
                case Constants.ACTION_UPLOAD_FACULTIES:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadFaculties(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadFaculties(request);
                    }
                    return csvHandler.handleUploadFaculties(request);
                case Constants.ACTION_UPLOAD_CLASSES:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadClasses(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadClasses(request);
                    }
                    return csvHandler.handleUploadClasses(request);
                case Constants.ACTION_UPLOAD_STUDENTS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadStudents(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadStudents(request);
                    }
                    return csvHandler.handleUploadStudents(request);
                case Constants.ACTION_UPLOAD_SUBJECTS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadSubjects(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadSubjects(request);
                    }
                    return csvHandler.handleUploadSubjects(request);
                case Constants.ACTION_UPLOAD_COURSES:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadCourses(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadCourses(request);
                    }
                    return csvHandler.handleUploadCourses(request);
                case Constants.ACTION_UPLOAD_ENROLLMENTS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadEnrollments(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadEnrollments(request);
                    }
                    return csvHandler.handleUploadEnrollments(request);
                case Constants.ACTION_UPLOAD_GRADES:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadGrades(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadGrades(request);
                    }
                    return csvHandler.handleUploadGrades(request);
                case Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadClassOpeningRequests(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadClassOpeningRequests(request);
                    }
                    return csvHandler.handleUploadClassOpeningRequests(request);
                case Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadCourseRegistrations(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadCourseRegistrations(request);
                    }
                    return csvHandler.handleUploadCourseRegistrations(request);
                case Constants.ACTION_UPLOAD_NOTIFICATIONS:
                    if (isPostgresClient()) {
                        return postgresHandler.handleUploadNotifications(request);
                    } else if (isCsvClient()) {
                        return csvHandler.handleUploadNotifications(request);
                    }
                    return csvHandler.handleUploadNotifications(request);

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
        if (csvHandler != null) {
            csvHandler.updateCurrentUser(user);
        }
        if (postgresHandler != null) {
            postgresHandler.updateCurrentUser(user);
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

    private boolean isPostgresClient() {
        return "POSTGRES".equalsIgnoreCase(clientSource);
    }

    private boolean isCsvClient() {
        return "CSV".equalsIgnoreCase(clientSource);
    }

    private Message routeSyncCheck(Message request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) request.getData("metadata", Map.class);
        String requestedSource = null;
        if (metadata != null && metadata.get("database_type") != null) {
            requestedSource = metadata.get("database_type").toString();
        }
        if ("POSTGRES".equalsIgnoreCase(requestedSource) || isPostgresClient()) {
            return postgresHandler.handleSyncCheck(request);
        }
        return csvHandler.handleSyncCheck(request);
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

    private Message handleGetAvailableClasses(Message request) {
        try {
            String facultyCode = request.getData("facultyCode", String.class);
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            List<com.university.sms.model.Class> classes = classDAO.findAvailableClasses(facultyCode);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Lấy danh sách lớp còn trống thành công");
            response.addData(Constants.KEY_CLASSES, classes);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting available classes", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleAddClass(Message request) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_ADD_CLASS, Constants.MSG_UNAUTHORIZED);
        }

        try {
            com.university.sms.model.Class classEntity = request.getData(Constants.KEY_CLASS,
                    com.university.sms.model.Class.class);
            if (classEntity == null) {
                return Message.createErrorResponse(Constants.ACTION_ADD_CLASS, Constants.MSG_INVALID_DATA);
            }

            if (classEntity.getClassCode() == null || classEntity.getClassCode().trim().isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_ADD_CLASS, "Thiếu mã lớp");
            }

            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            com.university.sms.model.Class existing = classDAO.findByCode(classEntity.getClassCode());
            if (existing != null) {
                return Message.createErrorResponse(Constants.ACTION_ADD_CLASS,
                        "Mã lớp đã tồn tại: " + classEntity.getClassCode());
            }

            boolean ok = classDAO.insert(classEntity);
            if (ok) {
                // Lưu source khi admin thêm mới
                if (classEntity.getClassId() > 0) {
                    this.saveDataOrigin("class", classEntity.getClassId(), clientSource);
                }
                LOGGER.info("Class added: " + classEntity.getClassCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(Constants.ACTION_ADD_CLASS, "Thêm lớp thành công");
            }
            return Message.createErrorResponse(Constants.ACTION_ADD_CLASS, "Không thể thêm lớp");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding class", e);
            return Message.createErrorResponse(Constants.ACTION_ADD_CLASS, "Lỗi: " + e.getMessage());
        }
    }

    private Message handleUpdateClass(Message request) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_CLASS, Constants.MSG_UNAUTHORIZED);
        }

        try {
            com.university.sms.model.Class classEntity = request.getData(Constants.KEY_CLASS,
                    com.university.sms.model.Class.class);
            if (classEntity == null || classEntity.getClassId() <= 0) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_CLASS, Constants.MSG_INVALID_DATA);
            }

            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            boolean ok = classDAO.update(classEntity);
            if (ok) {
                // Khi sửa: chỉ update timestamp nếu đã có source, không tạo mới source
                String existingSource = this.getDataOrigin("class", classEntity.getClassId());
                if (existingSource != null) {
                    this.updateDataOriginTimestamp("class", classEntity.getClassId());
                }
                LOGGER.info("Class updated: " + classEntity.getClassCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(Constants.ACTION_UPDATE_CLASS, "Cập nhật lớp thành công");
            }
            return Message.createErrorResponse(Constants.ACTION_UPDATE_CLASS, "Không thể cập nhật lớp");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating class", e);
            return Message.createErrorResponse(Constants.ACTION_UPDATE_CLASS, "Lỗi: " + e.getMessage());
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
