package com.university.sms.server;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
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
import com.university.sms.model.ClassOpeningRequest;
import com.university.sms.model.CourseRegistration;
import com.university.sms.model.Grade;
import com.university.sms.model.Notification;
import com.university.sms.dao.UserDAO;
import com.university.sms.dao.CourseDAO;
import com.university.sms.dao.EnrollmentDAO;
import com.university.sms.dao.StudentDAO;
import com.university.sms.dao.CourseRegistrationDAO;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.university.sms.util.DatabaseConnection;

/**
 * Xử lý kết nối từ mỗi client
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private User currentUser;
    private boolean isConnected;

    // Services
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

    // Source/client provenance (e.g., CSV, POSTGRES, etc.) captured during sync
    private String clientSource = "UNKNOWN";

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;

        // Initialize services
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
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            LOGGER.info("Client connected: " + clientSocket.getRemoteSocketAddress());

            // Listen for messages from client
            while (isConnected && !clientSocket.isClosed()) {
                try {
                    Message request = (Message) inputStream.readObject();
                    LOGGER.info("Received request: " + request.getAction() + " from " +
                            (currentUser != null ? currentUser.getUsername() : "anonymous"));

                    Message response = processRequest(request);
                    sendResponse(response);

                } catch (SocketException e) {
                    LOGGER.info("Client disconnected: " + clientSocket.getRemoteSocketAddress());
                    break;
                } catch (EOFException e) {
                    LOGGER.info("Client connection ended: " + clientSocket.getRemoteSocketAddress());
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing client request", e);

                    Message errorResponse = Message.createErrorResponse("ERROR", "Server error occurred");
                    sendResponse(errorResponse);
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error initializing client handler", e);
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
            // Authentication required actions
            if (!isAuthenticated() && !isPublicAction(action)) {
                return Message.createErrorResponse(action, Constants.MSG_UNAUTHORIZED);
            }

            switch (action) {
                // Authentication actions
                case Constants.ACTION_LOGIN:
                    return handleLogin(request);
                case Constants.ACTION_LOGOUT:
                    return handleLogout(request);
                case Constants.ACTION_CHANGE_PASSWORD:
                    return handleChangePassword(request);

                // Teacher Management actions
                case Constants.ACTION_ADD_TEACHER:
                    return handleAddTeacher(request);
                case Constants.ACTION_UPDATE_TEACHER:
                    return handleUpdateTeacher(request);
                case Constants.ACTION_DELETE_TEACHER:
                    return handleDeleteTeacher(request);
                case Constants.ACTION_ACTIVATE_USER:
                    return handleActivateUser(request);
                case Constants.ACTION_GET_ALL_TEACHERS_INCLUDE_INACTIVE:
                    return handleGetAllTeachersIncludeInactive(request);
                case Constants.ACTION_GET_ALL_STUDENTS_INCLUDE_INACTIVE:
                    return handleGetAllStudentsIncludeInactive(request);

                // Student actions
                case Constants.ACTION_GET_STUDENT_INFO:
                    return handleGetStudentInfo(request);
                case Constants.ACTION_GET_ALL_STUDENTS:
                    return handleGetAllStudents(request);
                case Constants.ACTION_GET_STUDENTS_BY_CLASS:
                    return handleGetStudentsByClass(request);
                case Constants.ACTION_SEARCH_STUDENTS:
                    return handleSearchStudents(request);
                case Constants.ACTION_ADD_STUDENT:
                    return handleAddStudent(request);
                case Constants.ACTION_UPDATE_STUDENT:
                    return handleUpdateStudent(request);
                case Constants.ACTION_DELETE_STUDENT:
                    return handleDeleteStudent(request);

                // Course actions
                case Constants.ACTION_GET_ALL_COURSES:
                    return handleGetAllCourses(request);
                case Constants.ACTION_GET_COURSES:
                    return handleGetAllCourses(request); // Use same handler
                case Constants.ACTION_GET_COURSE_INFO:
                    return handleGetCourseInfo(request);
                case Constants.ACTION_ADD_COURSE:
                    return handleAddCourse(request);
                case Constants.ACTION_UPDATE_COURSE:
                    return handleUpdateCourse(request);
                case Constants.ACTION_DELETE_COURSE:
                    return handleDeleteCourse(request);
                case Constants.ACTION_OPEN_COURSE_REGISTRATION:
                    return handleOpenCourseRegistration(request);
                case Constants.ACTION_CLOSE_COURSE_REGISTRATION:
                    return handleCloseCourseRegistration(request);

                // Teacher actions
                case Constants.ACTION_GET_ALL_TEACHERS:
                    return handleGetAllTeachers(request);
                case Constants.ACTION_SEARCH_TEACHERS:
                    return handleSearchTeachers(request);
                case Constants.ACTION_GET_COURSES_BY_TEACHER:
                    return handleGetCoursesByTeacher(request);

                // Faculty actions
                case Constants.ACTION_GET_FACULTIES:
                case Constants.ACTION_GET_ALL_FACULTIES:
                    return handleGetAllFaculties(request);

                // Class actions
                case Constants.ACTION_GET_CLASSES:
                case Constants.ACTION_GET_ALL_CLASSES:
                    return handleGetAllClasses(request);

                // Subject actions
                case Constants.ACTION_GET_SUBJECTS:
                    return handleGetSubjects(request);
                case Constants.ACTION_GET_ALL_SUBJECTS:
                    return handleGetAllSubjects(request);
                case Constants.ACTION_SEARCH_SUBJECTS:
                    return handleSearchSubjects(request);
                case Constants.ACTION_ADD_SUBJECT:
                    return handleAddSubject(request);
                case Constants.ACTION_UPDATE_SUBJECT:
                    return handleUpdateSubject(request);
                case Constants.ACTION_DELETE_SUBJECT:
                    return handleDeleteSubject(request);

                // Enrollment actions
                case Constants.ACTION_GET_ENROLLMENTS_BY_COURSE:
                    return handleGetEnrollmentsByCourse(request);
                case Constants.ACTION_GET_ENROLLMENTS:
                    return handleGetEnrollments(request);
                case Constants.ACTION_GET_STUDENT_GRADES:
                    return handleGetStudentGrades(request);
                case Constants.ACTION_ENROLL_COURSE:
                    return handleEnrollCourse(request);
                case Constants.ACTION_DROP_COURSE:
                    return handleDropCourse(request);

                // Grade actions
                case Constants.ACTION_ADD_GRADE:
                    return handleAddGrade(request);
                case Constants.ACTION_UPDATE_GRADE:
                    return handleUpdateGrade(request);
                case Constants.ACTION_DELETE_GRADE:
                    return handleDeleteGrade(request);
                case Constants.ACTION_GET_GRADES:
                    return handleGetGrades(request);
                case Constants.ACTION_CALCULATE_FINAL_GRADE:
                    return handleCalculateFinalGrade(request);

                // Sync actions
                case Constants.ACTION_SYNC_CHECK:
                    return handleSyncCheck(request);
                case Constants.ACTION_DOWNLOAD_DATA:
                    return handleDownloadData(request);
                case Constants.ACTION_UPLOAD_USERS:
                    return handleUploadUsers(request);
                case Constants.ACTION_UPLOAD_FACULTIES:
                    return handleUploadFaculties(request);
                case Constants.ACTION_UPLOAD_CLASSES:
                    return handleUploadClasses(request);
                case Constants.ACTION_UPLOAD_STUDENTS:
                    return handleUploadStudents(request);
                case Constants.ACTION_UPLOAD_SUBJECTS:
                    return handleUploadSubjects(request);
                case Constants.ACTION_UPLOAD_COURSES:
                    return handleUploadCourses(request);
                case Constants.ACTION_UPLOAD_ENROLLMENTS:
                    return handleUploadEnrollments(request);
                case Constants.ACTION_UPLOAD_GRADES:
                    return handleUploadGrades(request);
                case Constants.ACTION_UPLOAD_CLASS_OPENING_REQUESTS:
                    return handleUploadClassOpeningRequests(request);
                case Constants.ACTION_UPLOAD_COURSE_REGISTRATIONS:
                    return handleUploadCourseRegistrations(request);
                case Constants.ACTION_UPLOAD_NOTIFICATIONS:
                    return handleUploadNotifications(request);

                // Class Opening Request actions
                case Constants.ACTION_GET_ALL_CLASS_REQUESTS:
                    return handleGetAllClassRequests(request);
                case Constants.ACTION_GET_CLASS_REQUEST_BY_ID:
                    return handleGetClassRequestById(request);
                case Constants.ACTION_GET_MY_CLASS_REQUESTS:
                    return handleGetMyClassRequests(request);
                case Constants.ACTION_GET_PENDING_CLASS_REQUESTS:
                    return handleGetPendingClassRequests(request);
                case Constants.ACTION_SUBMIT_CLASS_REQUEST:
                    return handleSubmitClassRequest(request);
                case Constants.ACTION_UPDATE_CLASS_REQUEST:
                    return handleUpdateClassRequest(request);
                case Constants.ACTION_CANCEL_CLASS_REQUEST:
                    return handleCancelClassRequest(request);
                case Constants.ACTION_APPROVE_CLASS_REQUEST:
                    return handleApproveClassRequest(request);
                case Constants.ACTION_REJECT_CLASS_REQUEST:
                    return handleRejectClassRequest(request);
                case Constants.ACTION_GET_CLASS_REQUEST_STATS:
                    return handleGetClassRequestStats(request);

                // Course Registration actions
                case Constants.ACTION_GET_ALL_REGISTRATIONS:
                    return handleGetAllRegistrations(request);
                case Constants.ACTION_GET_REGISTRATION_BY_ID:
                    return handleGetRegistrationById(request);
                case Constants.ACTION_GET_MY_REGISTRATIONS:
                    return handleGetMyRegistrations(request);
                case Constants.ACTION_GET_COURSE_REGISTRATIONS:
                    return handleGetCourseRegistrations(request);
                case Constants.ACTION_GET_PENDING_REGISTRATIONS:
                    return handleGetPendingRegistrations(request);
                case Constants.ACTION_REGISTER_COURSE:
                    return handleRegisterCourse(request);
                case Constants.ACTION_CANCEL_REGISTRATION:
                    return handleCancelRegistration(request);
                case Constants.ACTION_APPROVE_REGISTRATION:
                    return handleApproveRegistration(request);
                case Constants.ACTION_REJECT_REGISTRATION:
                    return handleRejectRegistration(request);
                case Constants.ACTION_VALIDATE_REGISTRATION:
                    return handleValidateRegistration(request);
                case Constants.ACTION_GET_STUDENT_CREDITS:
                    return handleGetStudentCredits(request);
                case Constants.ACTION_GET_REGISTRATION_STATS:
                    return handleGetRegistrationStats(request);

                // Notification actions
                case Constants.ACTION_GET_NOTIFICATIONS:
                    return handleGetNotifications(request);
                case Constants.ACTION_SEND_NOTIFICATION:
                    return handleSendNotification(request);
                case Constants.ACTION_MARK_NOTIFICATION_READ:
                    return handleMarkNotificationRead(request);

                // Timetable & Transcript actions
                case Constants.ACTION_GET_TIMETABLE:
                    return handleGetTimetable(request);

                case Constants.ACTION_GET_TRANSCRIPT:
                    return handleGetTranscript(request);

                case Constants.ACTION_GET_SEMESTER_TRANSCRIPT:
                    return handleGetSemesterTranscript(request);

                case Constants.ACTION_GET_HONOR_STUDENTS:
                    return handleGetHonorStudents(request);

                case Constants.ACTION_GET_FACULTY_STATISTICS:
                    return handleGetFacultyStatistics(request);

                case Constants.ACTION_VALIDATE_SCHEDULE:
                    return handleValidateSchedule(request);

                case Constants.ACTION_GET_SERVER_STATISTICS:
                    return handleGetServerStatistics(request);

                default:
                    return Message.createErrorResponse(action, "Unknown action: " + action);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing action: " + action, e);
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

            String clientIP = clientSocket.getRemoteSocketAddress().toString();
            authService.logLogin(user.getUsername(), clientIP, "Java Client", "success");

            Message response = Message.createSuccessResponse(Constants.ACTION_LOGIN, Constants.MSG_LOGIN_SUCCESS);
            response.addData(Constants.KEY_USER, user);

            LOGGER.info("User logged in successfully: " + username);
            return response;
        } else {
            // Log failed login attempt
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

            LOGGER.warning("Login failed for user " + username + ": " + errorMessage);
            return Message.createErrorResponse(Constants.ACTION_LOGIN, errorMessage);
        }
    }

    /**
     * Xử lý đăng xuất
     */
    private Message handleLogout(Message request) {
        if (currentUser != null) {
            LOGGER.info("User logged out: " + currentUser.getUsername());
            currentUser = null;
        }
        return Message.createSuccessResponse(Constants.ACTION_LOGOUT, Constants.MSG_LOGOUT_SUCCESS);
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

    private Message handleGetStudentInfo(Message request) {
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            var student = studentService.findByUsername(currentUser.getUsername());
            if (student != null) {
                Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
                        "Lấy thông tin thành công");
                response.addData(Constants.KEY_STUDENT, student);
                return response;
            }
        } else if (currentUser.getRole() == User.UserRole.ADMIN ||
                currentUser.getRole() == User.UserRole.TEACHER) {
            String studentCode = request.getData("studentCode", String.class);
            if (studentCode != null && !studentCode.isEmpty()) {
                StudentDAO studentDAO = new StudentDAO();
                Student student = studentDAO.findByStudentCode(studentCode);
                if (student != null) {
                    Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
                            "Lấy thông tin thành công");
                    response.addData(Constants.KEY_STUDENT, student);
                    return response;
                }
            }
        }

        return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_INFO, Constants.MSG_STUDENT_NOT_FOUND);
    }

    /**
     * Lấy tất cả sinh viên
     */
    private Message handleGetAllStudents(Message request) {
        // Chỉ admin và giáo viên mới có quyền xem danh sách sinh viên
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
            return Message.createErrorResponse(Constants.ACTION_GET_ALL_STUDENTS, Constants.MSG_UNAUTHORIZED);
        }

        try {
            var students = studentService.getAllStudents();
            String responseAction = request.getAction();
            Message response = Message.createSuccessResponse(responseAction, "Lấy danh sách thành công");
            response.addData(Constants.KEY_STUDENTS, students);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách tất cả sinh viên: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả sinh viên (bao gồm cả đã vô hiệu hóa)
     */
    private Message handleGetAllStudentsIncludeInactive(Message request) {
        // Chỉ admin mới có quyền xem cả sinh viên đã vô hiệu hóa
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
        }

        try {
            StudentDAO studentDAO = new StudentDAO();
            List<Student> students = studentDAO.findAllIncludeInactive();
            Message response = Message.createSuccessResponse(request.getAction(), "Lấy danh sách thành công");
            response.addData(Constants.KEY_STUDENTS, students);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách tất cả sinh viên (bao gồm không hoạt động): " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách sinh viên theo lớp
     */
    private Message handleGetStudentsByClass(Message request) {
        // Chỉ admin và giáo viên mới có quyền xem danh sách sinh viên theo lớp
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
            return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, Constants.MSG_UNAUTHORIZED);
        }

        try {
            String classCode = request.getData(Constants.KEY_CLASS_CODE, String.class);
            if (classCode == null || classCode.trim().isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, "Thiếu mã lớp");
            }

            List<Student> students = studentService.getStudentsByClass(classCode.trim());
            Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS,
                    "Lấy danh sách sinh viên thành công");
            response.addData(Constants.KEY_STUDENTS, students);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách sinh viên theo lớp: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_GET_STUDENTS_BY_CLASS, "Lỗi server: " + e.getMessage());
        }
    }

    /**
     * Xử lý tìm kiếm sinh viên
     */
    private Message handleSearchStudents(Message request) {
        // Chỉ admin và giáo viên mới có quyền tìm kiếm sinh viên
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
            return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, Constants.MSG_UNAUTHORIZED);
        }

        String keyword = request.getData(Constants.KEY_SEARCH_KEYWORD, String.class);
        if (keyword == null || keyword.trim().isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_SEARCH_STUDENTS, Constants.MSG_INVALID_DATA);
        }

        var students = studentService.searchStudents(keyword);
        Message response = Message.createSuccessResponse(Constants.ACTION_SEARCH_STUDENTS, "Tìm kiếm thành công");
        response.addData(Constants.KEY_STUDENTS, students);
        return response;
    }

    private Message handleAddStudent(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_UNAUTHORIZED);
        }

        com.university.sms.model.Student student = request.getData(Constants.KEY_STUDENT,
                com.university.sms.model.Student.class);
        if (student == null) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_INVALID_DATA);
        }

        // Validate required fields
        if (student.getStudentCode() == null || student.getStudentCode().trim().isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Thiếu mã sinh viên");
        }
        if (student.getFacultyCode() == null || student.getFacultyCode().trim().isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Thiếu mã khoa");
        }

        // Validate facultyCode exists
        com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
        com.university.sms.model.Faculty faculty = facultyDAO.findByCode(student.getFacultyCode());
        if (faculty == null) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                    "Mã khoa không tồn tại: " + student.getFacultyCode());
        }

        // Validate classCode exists (if provided)
        if (student.getClassCode() != null && !student.getClassCode().trim().isEmpty()) {
            com.university.sms.dao.ClassDAO classDAO = new com.university.sms.dao.ClassDAO();
            com.university.sms.model.Class classObj = classDAO.findByCode(student.getClassCode());
            if (classObj == null) {
                return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                        "Mã lớp không tồn tại: " + student.getClassCode());
            }
        }

        // Ensure related user exists (create if missing)
        try {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            boolean userOk = true;
            String username = student.getUsername();
            if (username == null || username.isEmpty()) {
                username = student.getStudentCode();
                student.setUsername(username);
            }

            // Validate email format (if provided) - ALWAYS check, regardless of username
            // existence
            if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
                if (!isValidEmailFormat(student.getEmail().trim())) {
                    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                            "Email không hợp lệ. Email phải có định dạng: example@domain.com");
                }
            }

            // Normalize and validate phone format (if provided) - ALWAYS check, regardless
            // of username existence
            String normalizedStudentPhone = null;
            if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
                normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
                if (!isValidPhoneFormat(normalizedStudentPhone)) {
                    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                            "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
                }
            }

            // Check if email already exists (if provided) - ALWAYS check, regardless of
            // username existence
            if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
                com.university.sms.model.User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
                if (existingUserByEmail != null) {
                    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                            "Email đã được sử dụng bởi user khác: " + student.getEmail());
                }
            }

            // Check if phone already exists (if provided) - ALWAYS check, regardless of
            // username existence
            if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
                com.university.sms.model.User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
                if (existingUserByPhone != null) {
                    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                            "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
                }
            }

            // Check if username already exists
            com.university.sms.model.User byUsername = userDAO.findByUsername(username);
            if (byUsername != null) {
                // Username exists - check if it's a student
                if (byUsername.getRole() != com.university.sms.model.User.UserRole.STUDENT) {
                    return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                            "Username đã tồn tại với vai trò khác: " + username);
                }
                // Username exists and is a student - OK, reuse it
                // Note: Phone and email validation already done above, so they won't conflict
            } else {
                // Create new user
                com.university.sms.model.User u = new com.university.sms.model.User();
                u.setUsername(username);
                u.setPassword("password"); // Default password - should be changed on first login
                u.setFullName(student.getFullName());
                u.setEmail(student.getEmail());
                u.setPhone(normalizedStudentPhone); // Use normalized phone
                u.setAddress(student.getAddress());
                u.setRole(com.university.sms.model.User.UserRole.STUDENT);
                userOk = userDAO.addUser(u);
                if (userOk) {
                    saveDataOrigin("user", u.getUserId(), clientSource);
                }
            }
            if (!userOk) {
                return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                        "Không thể tạo tài khoản người dùng. Username có thể đã tồn tại.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing user for student: " + student.getStudentCode(), e);
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                    "Lỗi khi tạo tài khoản: " + e.getMessage());
        }

        // Check if student code already exists
        com.university.sms.dao.StudentDAO studentDAO = new com.university.sms.dao.StudentDAO();
        com.university.sms.model.Student existingStudent = studentDAO.findByStudentCode(student.getStudentCode());
        if (existingStudent != null) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT,
                    "Mã sinh viên đã tồn tại: " + student.getStudentCode());
        }

        boolean ok = studentService.addStudent(student);
        if (ok) {
            saveDataOrigin("student", student.getStudentId(), clientSource);
            LOGGER.info("Student added successfully: " + student.getStudentCode() + " by " + currentUser.getUsername());
            return Message.createSuccessResponse(Constants.ACTION_ADD_STUDENT, "Thêm sinh viên thành công");
        }
        return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Không thể thêm sinh viên. Vui lòng thử lại.");
    }

    /**
     * Xử lý cập nhật thông tin sinh viên
     */
    private Message handleUpdateStudent(Message request) {
        String subAction = request.getData("action", String.class);
        if ("delete".equalsIgnoreCase(subAction)) {
            return handleDeleteStudent(request);
        }

        com.university.sms.model.Student student = request.getData(Constants.KEY_STUDENT,
                com.university.sms.model.Student.class);
        if (student == null || student.getStudentId() <= 0) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_INVALID_DATA);
        }

        // Validate email format (if provided)
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            if (!isValidEmailFormat(student.getEmail().trim())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Email không hợp lệ. Email phải có định dạng: example@domain.com");
            }
        }

        // Normalize and validate phone format (if provided)
        String normalizedStudentPhone = null;
        if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
            normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
            if (!isValidPhoneFormat(normalizedStudentPhone)) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
            }
        }

        // Check if email already exists (if provided and different from current)
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
            if (existingUserByEmail != null && student.getUsername() != null
                    && !existingUserByEmail.getUsername().equals(student.getUsername())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Email đã được sử dụng bởi user khác: " + student.getEmail());
            }
        }

        // Get current student to compare phone
        com.university.sms.dao.StudentDAO studentDAO = new com.university.sms.dao.StudentDAO();
        com.university.sms.model.Student currentStudent = studentDAO.findById(student.getStudentId());
        if (currentStudent == null) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Không tìm thấy sinh viên");
        }

        // Check if phone has changed (compare normalized versions)
        String currentPhone = currentStudent.getPhone();
        String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
        boolean phoneChanged = normalizedStudentPhone != null
                && !normalizedStudentPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

        // Check if phone already exists (if provided and different from current)
        if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty() && phoneChanged) {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
            if (existingUserByPhone != null && student.getUsername() != null
                    && !existingUserByPhone.getUsername().equals(student.getUsername())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
            }
        }

        // Update student with normalized phone (if changed) or keep current
        if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty()) {
            student.setPhone(phoneChanged ? normalizedStudentPhone : currentPhone);
        }

        // Kiểm tra quyền và xử lý tương ứng
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            // Sinh viên chỉ được cập nhật thông tin của chính mình
            // Kiểm tra trong handleStudentSelfUpdate bằng cách lấy student từ DB
            return handleStudentSelfUpdate(student);
        } else if (currentUser.getRole() == User.UserRole.ADMIN || currentUser.getRole() == User.UserRole.TEACHER) {
            // Admin và Teacher có thể cập nhật đầy đủ
            boolean ok = studentService.updateStudent(student);
            if (ok) {
                saveDataOrigin("student", student.getStudentId(), clientSource);
                return Message.createSuccessResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_SUCCESS);
            }
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_DATABASE_ERROR);
        } else {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_UNAUTHORIZED);
        }
    }

    /**
     * Xử lý sinh viên tự cập nhật thông tin cá nhân (giới hạn một số field)
     */
    private Message handleStudentSelfUpdate(com.university.sms.model.Student student) {
        // ✅ REFACTORED: Lấy thông tin sinh viên hiện tại từ database bằng studentCode
        StudentDAO studentDAO = new StudentDAO();
        if (student.getStudentCode() == null || student.getStudentCode().isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Student code is required");
        }

        com.university.sms.model.Student currentStudent = studentDAO.findByStudentCode(student.getStudentCode());
        if (currentStudent == null) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Không tìm thấy thông tin sinh viên");
        }

        // ✅ Kiểm tra quyền: Sinh viên chỉ được cập nhật thông tin của chính mình
        if (currentStudent.getUsername() == null || !currentStudent.getUsername().equals(currentUser.getUsername())) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                    "Bạn chỉ có thể cập nhật thông tin của chính mình");
        }

        // Validate email format (if provided)
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            if (!isValidEmailFormat(student.getEmail().trim())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Email không hợp lệ. Email phải có định dạng: example@domain.com");
            }
        }

        // Normalize and validate phone format (if provided)
        String normalizedStudentPhone = null;
        if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
            normalizedStudentPhone = normalizePhoneNumber(student.getPhone().trim());
            if (!isValidPhoneFormat(normalizedStudentPhone)) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
            }
        }

        // Check if email already exists (if provided and different from current)
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User existingUserByEmail = userDAO.findByEmail(student.getEmail().trim());
            if (existingUserByEmail != null && currentStudent.getUsername() != null
                    && !existingUserByEmail.getUsername().equals(currentStudent.getUsername())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Email đã được sử dụng bởi user khác: " + student.getEmail());
            }
        }

        // Check if phone has changed (compare normalized versions)
        String currentPhone = currentStudent.getPhone();
        String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
        boolean phoneChanged = normalizedStudentPhone != null
                && !normalizedStudentPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

        // Check if phone already exists (if provided and different from current)
        if (normalizedStudentPhone != null && !normalizedStudentPhone.isEmpty() && phoneChanged) {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            com.university.sms.model.User existingUserByPhone = userDAO.findByPhone(normalizedStudentPhone);
            if (existingUserByPhone != null && currentStudent.getUsername() != null
                    && !existingUserByPhone.getUsername().equals(currentStudent.getUsername())) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT,
                        "Số điện thoại đã được sử dụng bởi user khác: " + normalizedStudentPhone);
            }
        }

        // Sinh viên chỉ được phép cập nhật các field sau:
        // - Thông tin liên hệ: email, phone, emergency_contact, emergency_phone
        currentStudent.setEmail(student.getEmail());
        currentStudent.setPhone(phoneChanged ? normalizedStudentPhone : currentPhone);
        currentStudent.setEmergencyContact(student.getEmergencyContact());
        currentStudent.setEmergencyPhone(student.getEmergencyPhone());

        // Các field sau KHÔNG được phép thay đổi bởi sinh viên:
        // - full_name, citizen_id, gender, birth_date
        // - class_id, admission_year, student_status
        // - GPA và credits (được tính tự động)

        boolean ok = studentService.updateStudent(currentStudent);
        if (ok) {
            saveDataOrigin("student", currentStudent.getStudentId(), clientSource);
            return Message.createSuccessResponse(Constants.ACTION_UPDATE_STUDENT, "Cập nhật thông tin thành công");
        }
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Lỗi khi cập nhật thông tin");
    }

    private Message handleDeleteStudent(Message request) {
        try {
            // Only admin can delete students
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa sinh viên");
            }

            String studentCode = request.getData("studentCode", String.class);
            if (studentCode == null || studentCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Student code is required");
            }

            StudentDAO studentDAO = new StudentDAO();
            Student student = studentDAO.findByStudentCode(studentCode);
            if (student == null) {
                return Message.createErrorResponse(request.getAction(), "Không tìm thấy sinh viên");
            }

            // Lưu source gốc để dùng sau
            String existingSource = getDataOrigin("student", student.getStudentId());

            // Cập nhật trạng thái tất cả enrollments của student
            // Giữ nguyên grades và course_registrations, chỉ cập nhật enrollment status
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            List<Enrollment> enrollments = enrollmentDAO.findByStudentCode(studentCode);
            int enrollmentsUpdated = 0;
            for (Enrollment enrollment : enrollments) {
                try {
                    // Đảm bảo enrollment có source trong data_origin để có thể sync
                    // Nếu chưa có, tạo mới với source = CSV (mặc định cho enrollments)
                    String enrollmentSource = getDataOrigin("enrollment", enrollment.getEnrollmentId());
                    if (enrollmentSource == null) {
                        // Enrollment chưa có source, tạo mới với source = CSV để có thể sync
                        saveDataOrigin("enrollment", enrollment.getEnrollmentId(), "CSV");
                        enrollmentSource = "CSV";
                        LOGGER.info("Đã tạo data_origin cho enrollment ID: " + enrollment.getEnrollmentId()
                                + " với source = CSV");
                    }

                    // Kiểm tra trạng thái hiện tại của enrollment
                    // Nếu status khác COMPLETED (Kết thúc học phần) → chuyển về DROPPED (Thôi học)
                    if (enrollment.getEnrollmentStatus() != Enrollment.EnrollmentStatus.COMPLETED) {
                        if (enrollmentDAO.updateEnrollmentStatus(enrollment.getEnrollmentId(),
                                Enrollment.EnrollmentStatus.DROPPED)) {
                            enrollmentsUpdated++;

                            // Cập nhật timestamp SAU KHI update enrollment status thành công
                            // Đảm bảo version tăng để CSV client có thể download
                            if (enrollmentSource != null) {
                                int updated = updateDataOriginTimestampWithResult("enrollment",
                                        enrollment.getEnrollmentId());
                                if (updated > 0) {
                                    LOGGER.info(
                                            "Đã cập nhật timestamp cho enrollment ID: " + enrollment.getEnrollmentId()
                                                    + " (source: " + enrollmentSource + ")");
                                } else {
                                    LOGGER.warning("Không thể cập nhật timestamp cho enrollment ID: "
                                            + enrollment.getEnrollmentId() + " - có thể chưa có trong data_origin");
                                }
                            }

                            LOGGER.info("Đã cập nhật enrollment ID: " + enrollment.getEnrollmentId()
                                    + " của sinh viên " + studentCode
                                    + " từ status: " + enrollment.getEnrollmentStatus().name()
                                    + " → DROPPED (Thôi học), source: " + enrollmentSource);
                        } else {
                            LOGGER.warning("Không thể cập nhật enrollment ID: " + enrollment.getEnrollmentId()
                                    + " của sinh viên " + studentCode);
                        }
                    } else {
                        LOGGER.info("Enrollment ID: " + enrollment.getEnrollmentId()
                                + " của sinh viên " + studentCode
                                + " đã có status COMPLETED (Kết thúc học phần), giữ nguyên");
                    }
                } catch (Exception e) {
                    LOGGER.warning(
                            "Lỗi khi cập nhật enrollment ID: " + enrollment.getEnrollmentId() + ": " + e.getMessage());
                    LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
                }
            }
            if (enrollmentsUpdated > 0) {
                LOGGER.info("Đã cập nhật " + enrollmentsUpdated + " enrollments của sinh viên " + studentCode);
            }

            // Chuyển tất cả course_registrations đang PENDING thành CANCELLED khi student
            // bị SUSPENDED
            CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
            List<CourseRegistration> registrations = registrationDAO.findByStudent(studentCode);
            int registrationsCancelled = 0;
            for (CourseRegistration registration : registrations) {
                try {
                    // Chỉ chuyển những registration đang PENDING
                    if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.PENDING) {
                        // Cập nhật timestamp data_origin nếu registration có source CSV
                        String registrationSource = getDataOrigin("course_registration",
                                registration.getRegistrationId());
                        if (registrationSource != null) {
                            updateDataOriginTimestamp("course_registration", registration.getRegistrationId());
                        }

                        // Chuyển từ PENDING sang CANCELLED (reject)
                        if (registrationDAO.cancel(registration.getRegistrationId())) {
                            registrationsCancelled++;
                            LOGGER.info("Đã hủy (reject) course registration ID: " + registration.getRegistrationId()
                                    + " của sinh viên " + studentCode
                                    + " (từ PENDING → CANCELLED do student bị SUSPENDED)");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Lỗi khi hủy course registration ID: " + registration.getRegistrationId() + ": "
                            + e.getMessage());
                    LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
                }
            }
            if (registrationsCancelled > 0) {
                LOGGER.info("Đã hủy (reject) " + registrationsCancelled + " course registrations PENDING của sinh viên "
                        + studentCode);
            }

            // Chỉ vô hiệu hóa user/student thay vì xóa dữ liệu
            UserDAO userDAO = new UserDAO();
            boolean userDeactivated = userDAO.deactivateUser(student.getUsername());

            // Cập nhật trạng thái student = SUSPENDED
            boolean statusUpdated = studentDAO.updateStudentStatus(student.getStudentId(),
                    Student.StudentStatus.SUSPENDED);

            if (userDeactivated && statusUpdated) {
                if (existingSource != null) {
                    updateDataOriginTimestamp("student", student.getStudentId());
                    LOGGER.info("Đã cập nhật timestamp cho student ID: " + student.getStudentId()
                            + " (source: " + existingSource + ")");
                } else {
                    saveDataOrigin("student", student.getStudentId(), "CSV");
                    LOGGER.warning("Student ID: " + student.getStudentId()
                            + " chưa có trong data_origin, đã tạo mới với source = CSV");
                }

                User user = userDAO.findByUsername(student.getUsername());
                if (user != null) {
                    String userSource = getDataOrigin("user", user.getUserId());
                    if (userSource != null) {
                        updateDataOriginTimestamp("user", user.getUserId());
                        LOGGER.info("Đã cập nhật timestamp cho user ID: " + user.getUserId()
                                + " (source: " + userSource + ")");
                    } else {
                        saveDataOrigin("user", user.getUserId(), "CSV");
                        LOGGER.warning("User ID: " + user.getUserId()
                                + " chưa có trong data_origin, đã tạo mới với source = CSV");
                    }
                }

                LOGGER.info("Student deactivated: " + student.getStudentCode() + " by " + currentUser.getUsername());

                Student updatedStudent = studentDAO.findByStudentCode(studentCode);

                Message response = Message.createSuccessResponse(request.getAction(),
                        "Đã vô hiệu hóa sinh viên thành công");
                if (updatedStudent != null) {
                    response.addData(Constants.KEY_STUDENT, updatedStudent);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể vô hiệu hóa sinh viên");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating student", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả khóa học
     */
    private Message handleGetAllCourses(Message request) {
        try {
            LOGGER.info("Getting all courses...");
            var courses = courseService.getAllCourses();
            LOGGER.info("Found " + courses.size() + " courses");
            // Use the same action as request for proper response matching
            String responseAction = request.getAction();
            Message response = Message.createSuccessResponse(responseAction, "Lấy danh sách khóa học thành công");
            response.addData(Constants.KEY_COURSES, courses);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách tất cả khóa học: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
        }
    }

    // Removed handleGetCourses - using handleGetAllCourses for both actions

    /**
     * Xử lý lấy thông tin khóa học
     */
    private Message handleGetCourseInfo(Message request) {
        String courseCode = request.getData("courseCode", String.class);
        if (courseCode == null || courseCode.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_INVALID_DATA);
        }

        var course = courseService.getCourseByCode(courseCode);
        if (course != null) {
            Message response = Message.createSuccessResponse(Constants.ACTION_GET_COURSE_INFO,
                    "Lấy thông tin khóa học thành công");
            response.addData(Constants.KEY_COURSE, course);
            return response;
        }

        return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_COURSE_NOT_FOUND);
    }

    /**
     * Thêm khóa học mới
     */
    private Message handleAddCourse(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_UNAUTHORIZED);
        }

        com.university.sms.model.Course course = request.getData(Constants.KEY_COURSE,
                com.university.sms.model.Course.class);
        if (course == null) {
            return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_INVALID_DATA);
        }

        boolean ok = courseService.addCourse(course);
        if (ok) {
            saveDataOrigin("course", course.getCourseId(), clientSource);
            return Message.createSuccessResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_ADD_COURSE, Constants.MSG_DATABASE_ERROR);
    }

    /**
     * Cập nhật khóa học
     */
    private Message handleUpdateCourse(Message request) {
        // Cho phép ADMIN và TEACHER cập nhật thông tin cơ bản
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_UNAUTHORIZED);
        }

        com.university.sms.model.Course course = request.getData(Constants.KEY_COURSE,
                com.university.sms.model.Course.class);
        if (course == null || course.getCourseId() <= 0) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_INVALID_DATA);
        }

        boolean ok = courseService.updateCourse(course);
        if (ok) {
            saveDataOrigin("course", course.getCourseId(), clientSource);
            return Message.createSuccessResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_UPDATE_COURSE, Constants.MSG_DATABASE_ERROR);
    }

    /**
     * Hủy/Xóa lớp học phần với logic hybrid:
     * - Xóa hoàn toàn nếu: PLANNING + chưa có dữ liệu liên quan
     * - Hủy và giữ lại nếu: đã có dữ liệu liên quan hoặc ONGOING
     */
    private Message handleDeleteCourse(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_UNAUTHORIZED);
        }
        // ✅ REFACTORED: Use courseCode
        String courseCode = request.getData("courseCode", String.class);
        if (courseCode == null || courseCode.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_INVALID_DATA);
        }

        // Lấy course để kiểm tra business rules trước
        com.university.sms.model.Course course = courseService.getCourseByCode(courseCode);
        if (course == null) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, "Không tìm thấy lớp học phần");
        }

        // Kiểm tra business rules và trả về thông báo lỗi cụ thể
        // Business Rule: Không cho hủy lớp đã hoàn thành (completed) - cần lưu lịch sử
        if (course.getCourseStatus() == com.university.sms.model.Course.CourseStatus.COMPLETED) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
                    "Không thể hủy lớp học phần đã hoàn thành. Lớp đã kết thúc và cần lưu lịch sử.");
        }

        // Business Rule: Không cho hủy lớp đã bị hủy (cancelled)
        if (course.getCourseStatus() == com.university.sms.model.Course.CourseStatus.CANCELLED) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
                    "Lớp học phần đã bị hủy trước đó.");
        }

        // Cập nhật timestamp nếu course có source CSV (trước khi xóa)
        if (course.getCourseId() > 0) {
            String existingSource = getDataOrigin("course", course.getCourseId());
            if ("CSV".equals(existingSource)) {
                updateDataOriginTimestamp("course", course.getCourseId());
            }
        }

        // Thực hiện hủy/xóa lớp (logic hybrid tự động quyết định)
        boolean ok = courseService.deleteCourse(courseCode);
        if (ok) {
            LOGGER.info("Course deleted/cancelled successfully: " + courseCode + " by " + currentUser.getUsername());
            return Message.createSuccessResponse(Constants.ACTION_DELETE_COURSE, "Hủy lớp học phần thành công");
        }
        return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE,
                "Không thể hủy/xóa lớp học phần. Vui lòng thử lại.");
    }

    private Message handleOpenCourseRegistration(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
        }

        Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
        if (courseId == null) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_INVALID_DATA);
        }

        try {
            boolean opened = courseService.openRegistration(courseId);
            if (opened) {
                return Message.createSuccessResponse(request.getAction(), "Đã mở đăng ký cho lớp học phần.");
            }
            return Message.createErrorResponse(request.getAction(),
                    "Lớp học phần đang trong trạng thái không thể mở đăng ký.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening course registration", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleCloseCourseRegistration(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
        }

        Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
        if (courseId == null) {
            return Message.createErrorResponse(request.getAction(), Constants.MSG_INVALID_DATA);
        }

        try {
            CourseService.RegistrationClosureResult result = courseService.closeRegistration(courseId);
            Message response = Message.createSuccessResponse(request.getAction(),
                    result.getMessage() != null ? result.getMessage() : Constants.MSG_SUCCESS);
            response.addData("registrations", result.getRegistrations());
            response.addData("enrollments", result.getEnrollments());
            response.addData(Constants.KEY_STATUS, result.getFinalStatus());
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing course registration", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy danh sách đăng ký
     */
    private Message handleGetEnrollments(Message request) {
        try {
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();

            // Student: chỉ xem của chính mình
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                var me = studentService.findByUsername(currentUser.getUsername());
                if (me == null) {
                    return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS,
                            Constants.MSG_STUDENT_NOT_FOUND);
                }
                var list = enrollmentDAO.findByStudentCode(me.getStudentCode());
                Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
                resp.addData(Constants.KEY_ENROLLMENTS, list);
                return resp;
            }

            String studentCode = request.getData("studentCode", String.class);
            if (studentCode == null || studentCode.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_INVALID_DATA);
            }
            var list = enrollmentDAO.findByStudentCode(studentCode);
            Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
            resp.addData(Constants.KEY_ENROLLMENTS, list);
            return resp;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách đăng ký học phần: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Xử lý lấy điểm sinh viên
     */
    private Message handleGetStudentGrades(Message request) {
        try {
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();

            // ✅ REFACTORED: Use studentCode instead of studentId
            String targetStudentCode;
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                var me = studentService.findByUsername(currentUser.getUsername());
                if (me == null) {
                    return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES,
                            Constants.MSG_STUDENT_NOT_FOUND);
                }
                targetStudentCode = me.getStudentCode();
            } else {

                String studentCode = request.getData("studentCode", String.class);
                if (studentCode == null || studentCode.isEmpty()) {
                    return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES, Constants.MSG_INVALID_DATA);
                }
                targetStudentCode = studentCode;
            }

            var all = enrollmentDAO.findByStudentCode(targetStudentCode);
            // Lọc những dòng có điểm cuối kỳ (finalGrade hoặc letterGrade)
            java.util.List<com.university.sms.model.Enrollment> grades = new java.util.ArrayList<>();
            for (var e : all) {
                if (e.getFinalGrade() != null || e.getLetterGrade() != null) {
                    grades.add(e);
                }
            }

            Message resp = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_GRADES, Constants.MSG_SUCCESS);
            resp.addData(Constants.KEY_GRADES, grades);
            return resp;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy điểm sinh viên: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Đăng ký khóa học
     */
    private Message handleEnrollCourse(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String courseCode = request.getData("courseCode", String.class);

            if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_INVALID_DATA);
            }

            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(
                    studentCode, courseCode);
            boolean ok;
            com.university.sms.model.Enrollment enrollment = null;
            if (existing == null) {
                com.university.sms.model.Enrollment e = new com.university.sms.model.Enrollment();
                e.setStudentCode(studentCode);
                e.setCourseCode(courseCode);
                ok = enrollmentDAO.save(e);
                if (ok) {
                    // ✅ REFACTORED: Update current_students using courseCode
                    courseService.incrementCurrentStudents(courseCode);
                    saveDataOrigin("enrollment", e.getEnrollmentId(), clientSource);
                    // Lấy enrollment đã được lưu để trả về cho client
                    enrollment = enrollmentDAO.findByStudentAndCourse(studentCode, courseCode);
                }
            } else {
                ok = true; // already enrolled
                enrollment = existing;
            }
            if (ok) {
                Message response = Message.createSuccessResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SUCCESS);
                if (enrollment != null) {
                    response.addData(Constants.KEY_ENROLLMENT, enrollment);
                }
                return response;
            }
            return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi đăng ký khóa học: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Hủy đăng ký khóa học
     */
    private Message handleDropCourse(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String courseCode = request.getData("courseCode", String.class);

            if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
            }

            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(
                    studentCode, courseCode);
            if (existing == null) {
                return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
            }

            // Lưu enrollment trước khi xóa để trả về cho client
            com.university.sms.model.Enrollment deletedEnrollment = existing;
            // Cập nhật version CSV nếu enrollment có source CSV (trước khi xóa)
            String existingSource = getDataOrigin("enrollment", existing.getEnrollmentId());
            if ("CSV".equals(existingSource)) {
                updateDataOriginTimestamp("enrollment", existing.getEnrollmentId());
            }
            boolean ok = enrollmentDAO.deleteEnrollment(existing.getEnrollmentId());
            if (ok) {
                // ✅ REFACTORED: Update current_students using courseCode
                courseService.decrementCurrentStudents(courseCode);
                Message response = Message.createSuccessResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_SUCCESS);
                response.addData(Constants.KEY_ENROLLMENT, deletedEnrollment);
                return response;
            }
            return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi hủy đăng ký khóa học: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Cập nhật điểm cuối kỳ (final grade)
     */
    private Message handleUpdateFinalGrade(Message request) {
        try {
            Integer enrollmentId = request.getData("enrollmentId", Integer.class);
            java.math.BigDecimal finalGrade = request.getData("finalGrade", java.math.BigDecimal.class);
            String letter = request.getData("letterGrade", String.class);
            java.math.BigDecimal points = request.getData("gradePoints", java.math.BigDecimal.class);
            if (enrollmentId == null) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_GRADE, Constants.MSG_INVALID_DATA);
            }
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            boolean ok = enrollmentDAO.updateFinalGrade(enrollmentId, finalGrade, letter, points);
            if (ok) {
                return Message.createSuccessResponse(Constants.ACTION_UPDATE_GRADE, Constants.MSG_SUCCESS);
            }
            return Message.createErrorResponse(Constants.ACTION_UPDATE_GRADE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi cập nhật điểm cuối kỳ: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_UPDATE_GRADE, Constants.MSG_SERVER_ERROR);
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
            LOGGER.log(Level.SEVERE, "Error sending response to client", e);
        }
    }

    /**
     * Kiểm tra xem action có cần xác thực không
     */
    private boolean isPublicAction(String action) {
        return Constants.ACTION_LOGIN.equals(action) ||
                Constants.ACTION_SYNC_CHECK.equals(action) ||
                Constants.ACTION_DOWNLOAD_DATA.equals(action);
    }

    /**
     * Xử lý sync check request từ client
     */
    private Message handleSyncCheck(Message request) {
        try {
            // Lấy metadata từ client
            @SuppressWarnings("unchecked")
            Map<String, Object> clientMetadata = (Map<String, Object>) request.getData("metadata", Map.class);

            if (clientMetadata == null) {
                return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Invalid metadata");
            }

            // Lấy thông tin client
            String clientDbType = (String) clientMetadata.get("database_type");
            if (clientDbType != null && !clientDbType.trim().isEmpty()) {
                this.clientSource = clientDbType.trim().toUpperCase();
            }

            // Lấy version client (có thể là 0 nếu chưa sync lần nào)
            Object clientVersionObj = clientMetadata.get("db_version");
            int clientVersion = 0;
            if (clientVersionObj != null) {
                if (clientVersionObj instanceof Number) {
                    clientVersion = ((Number) clientVersionObj).intValue();
                } else if (clientVersionObj instanceof String) {
                    try {
                        clientVersion = Integer.parseInt((String) clientVersionObj);
                    } catch (NumberFormatException e) {
                        clientVersion = 0;
                    }
                }
            }

            int clientTotalRecords = 0;
            Object clientTotalRecordsObj = clientMetadata.get("total_records");
            if (clientTotalRecordsObj instanceof Number) {
                clientTotalRecords = ((Number) clientTotalRecordsObj).intValue();
            }

            // Lấy metadata server
            Map<String, Object> serverMetadata = getServerMetadata();
            int serverVersion = ((Number) serverMetadata.get("db_version")).intValue();

            // Lấy version cho source của client (CSV, POSTGRES, etc.)
            String clientSourceKey = this.clientSource.toLowerCase() + "_version";
            int clientSourceVersion = 0;
            if (serverMetadata.containsKey(clientSourceKey)) {
                Object versionObj = serverMetadata.get(clientSourceKey);
                if (versionObj instanceof Number) {
                    clientSourceVersion = ((Number) versionObj).intValue();
                }
            }

            String clientSourceTotalKey = this.clientSource.toLowerCase() + "_total_records";
            int clientSourceTotalRecords = 0;
            if (serverMetadata.containsKey(clientSourceTotalKey)) {
                Object totalObj = serverMetadata.get(clientSourceTotalKey);
                if (totalObj instanceof Number) {
                    clientSourceTotalRecords = ((Number) totalObj).intValue();
                }
            }

            LOGGER.info("Sync check - Client: " + (clientDbType != null ? clientDbType : "UNKNOWN") +
                    " [" + this.clientSource + "] v" + clientVersion + " (" + clientTotalRecords +
                    "), Server [" + this.clientSource + "]: v" + clientSourceVersion + " (" + clientSourceTotalRecords
                    + ")");

            // Quyết định sync action
            String syncAction;
            // Chỉ áp dụng two-way sync cho các external client (CSV, POSTGRES, etc.), không
            // phải REGULAR
            if (this.clientSource != null && !"REGULAR".equals(this.clientSource)
                    && !"UNKNOWN".equals(this.clientSource)) {
                // External client (CSV, POSTGRES, etc.): so sánh với version của source đó trên
                // server
                // Version = 0 hoặc rỗng -> mặc định upload (lần đầu kết nối)
                if (clientVersion == 0) {
                    // Version rỗng -> mặc định upload
                    syncAction = "UPLOAD_TO_SERVER";
                } else if (clientSourceVersion > clientVersion) {
                    // Server có version mới hơn (timestamp lớn hơn) → Download
                    syncAction = "DOWNLOAD_FROM_SERVER";
                } else if (clientVersion > clientSourceVersion) {
                    // Client có version mới hơn (timestamp lớn hơn) → Upload
                    syncAction = "UPLOAD_TO_SERVER";
                } else {
                    // Bằng nhau → Không cần sync
                    syncAction = "NO_SYNC_NEEDED";
                }
            } else {
                // Regular client hoặc client khác: luôn upload
                syncAction = "UPLOAD_TO_SERVER";
            }

            Message response = Message.createSuccessResponse(Constants.ACTION_SYNC_CHECK,
                    "Sync check completed");
            response.addData("sync_action", syncAction);
            response.addData("server_version", serverVersion);
            response.addData("client_source_version", clientSourceVersion);
            response.addData("server_metadata", serverMetadata);

            return response;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý kiểm tra đồng bộ: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy metadata của server
     * Tính version riêng cho từng source (CSV, POSTGRES, etc.)
     */
    private Map<String, Object> getServerMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Đếm records tổng
            int studentCount = studentService.getTotalCount();
            int courseCount = courseService.getTotalCount();

            // Lấy version tổng từ database
            int dbVersion = getServerVersion();

            // Tính version riêng cho từng source (CSV, POSTGRES, etc.)
            // Lấy danh sách các sources có trong database
            List<String> sources = getAvailableSources();

            // Tính version và count cho từng source
            for (String source : sources) {
                int sourceVersion = getDataVersionBySource(source);
                int sourceStudentCount = getDataCountBySource("student", source);
                int sourceCourseCount = getDataCountBySource("course", source);
                int sourceEnrollmentCount = getDataCountBySource("enrollment", source);
                int sourceFacultyCount = getDataCountBySource("faculty", source);
                int sourceClassCount = getDataCountBySource("class", source);
                int sourceSubjectCount = getDataCountBySource("subject", source);
                int sourceTotalRecords = sourceStudentCount + sourceCourseCount + sourceEnrollmentCount +
                        sourceFacultyCount + sourceClassCount + sourceSubjectCount;

                String sourceKey = source.toLowerCase();
                metadata.put(sourceKey + "_version", sourceVersion);
                metadata.put(sourceKey + "_student_count", sourceStudentCount);
                metadata.put(sourceKey + "_course_count", sourceCourseCount);
                metadata.put(sourceKey + "_enrollment_count", sourceEnrollmentCount);
                metadata.put(sourceKey + "_faculty_count", sourceFacultyCount);
                metadata.put(sourceKey + "_class_count", sourceClassCount);
                metadata.put(sourceKey + "_subject_count", sourceSubjectCount);
                metadata.put(sourceKey + "_total_records", sourceTotalRecords);
            }

            metadata.put("db_version", dbVersion);
            metadata.put("student_count", studentCount);
            metadata.put("course_count", courseCount);
            metadata.put("total_records", studentCount + courseCount);

        } catch (Exception e) {
            LOGGER.warning("Lỗi khi lấy metadata server: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
            metadata.put("db_version", 1);
            metadata.put("student_count", 0);
            metadata.put("course_count", 0);
            metadata.put("total_records", 0);
        }

        return metadata;
    }

    /**
     * Lấy danh sách các sources có trong database
     */
    private List<String> getAvailableSources() {
        List<String> sources = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT source FROM data_origin WHERE source IS NOT NULL ORDER BY source";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sources.add(rs.getString("source"));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi lấy danh sách nguồn dữ liệu: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
        }
        return sources;
    }

    /**
     * Tính số lượng records có source cụ thể cho entity type
     * Đếm tất cả records, không phân biệt active/inactive
     */
    private int getDataCountBySource(String entityType, String source) {
        String tableName = getTableName(entityType);
        String entityIdColumn = getEntityIdColumn(entityType);

        // Đếm tất cả records, không phân biệt active/inactive
        String sql = "SELECT COUNT(*) as count FROM " + tableName + " e " +
                "JOIN data_origin dor ON dor.entity_type = ? AND dor.entity_id = e." + entityIdColumn + " " +
                "WHERE dor.source = ?";

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setString(2, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi đếm số lượng " + entityType + " với nguồn " + source + ": " + e.getMessage());
            LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
        }
        return 0;
    }

    /**
     * Tính version cho data của một source cụ thể
     * Version = timestamp của lần thay đổi cuối cùng (seconds since epoch)
     * UNIX_TIMESTAMP() trả về số giây, không cần chia cho 1000
     * Hoặc nếu không có, dùng tổng số records làm fallback
     */
    private int getDataVersionBySource(String source) {
        // Lấy timestamp của lần thay đổi cuối cùng từ data_origin
        // UNIX_TIMESTAMP() trả về số giây (seconds since epoch), không cần chia cho
        // 1000
        String sql = "SELECT MAX(UNIX_TIMESTAMP(updated_at)) as last_update FROM data_origin WHERE source = ?";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long timestamp = rs.getLong("last_update");
                    if (!rs.wasNull() && timestamp > 0) {
                        // UNIX_TIMESTAMP() đã trả về giây, không cần chia cho 1000
                        // Chỉ cast về int (có thể mất precision nếu > Integer.MAX_VALUE)
                        return (int) timestamp;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi lấy timestamp cập nhật cuối cho nguồn " + source + ": " + e.getMessage());
            LOGGER.log(Level.WARNING, "Chi tiết lỗi", e);
        }

        // Fallback: dùng tổng số records nếu không có timestamp
        int total = getDataCountBySource("student", source) + getDataCountBySource("course", source) +
                getDataCountBySource("enrollment", source) + getDataCountBySource("faculty", source) +
                getDataCountBySource("class", source) + getDataCountBySource("subject", source);
        return total;
    }

    /**
     * Helper: Lấy tên bảng từ entity type
     */
    private String getTableName(String entityType) {
        switch (entityType) {
            case "student":
                return "students";
            case "course":
                return "courses";
            case "enrollment":
                return "enrollments";
            case "faculty":
                return "faculties";
            case "class":
                return "classes";
            case "subject":
                return "subjects";
            default:
                return entityType + "s";
        }
    }

    /**
     * Helper: Lấy tên cột ID từ entity type
     */
    private String getEntityIdColumn(String entityType) {
        switch (entityType) {
            case "student":
                return "student_id";
            case "course":
                return "course_id";
            case "enrollment":
                return "enrollment_id";
            case "faculty":
                return "faculty_id";
            case "class":
                return "class_id";
            case "subject":
                return "subject_id";
            default:
                return entityType + "_id";
        }
    }

    /**
     * Xử lý download data từ server về client
     * Chỉ trả về data có source = clientSource để tránh conflict
     */
    private Message handleDownloadData(Message request) {
        try {
            // Lấy source của client (CSV, POSTGRES, etc.)
            String source = this.clientSource;
            if (source == null || "UNKNOWN".equals(source) || "REGULAR".equals(source)) {
                // Nếu không có source hoặc là REGULAR, mặc định là CSV để backward compatible
                source = "CSV";
            }

            LOGGER.info("Downloading " + source + " data to client");

            // Lấy data có source = clientSource
            List<com.university.sms.model.Student> students = getStudentsBySource(source);
            List<com.university.sms.model.Course> courses = getCoursesBySource(source);
            List<com.university.sms.model.Enrollment> enrollments = getEnrollmentsBySource(source);
            List<com.university.sms.model.Faculty> faculties = getFacultiesBySource(source);
            List<com.university.sms.model.Class> classes = getClassesBySource(source);
            List<com.university.sms.model.Subject> subjects = getSubjectsBySource(source);
            List<com.university.sms.model.User> users = getUsersBySource(source);
            List<com.university.sms.model.Grade> grades = getGradesBySource(source);
            List<com.university.sms.model.Notification> notifications = getNotificationsBySource(source);
            List<com.university.sms.model.ClassOpeningRequest> classOpeningRequests = getClassOpeningRequestsBySource(
                    source);
            List<com.university.sms.model.CourseRegistration> courseRegistrations = getCourseRegistrationsBySource(
                    source);

            Message response = Message.createSuccessResponse(Constants.ACTION_DOWNLOAD_DATA,
                    "Downloaded " + students.size() + " students, " + courses.size() + " courses, " +
                            users.size() + " users, " + grades.size() + " grades, " + notifications.size()
                            + " notifications, " +
                            classOpeningRequests.size() + " requests, " + courseRegistrations.size()
                            + " registrations");
            response.addData("students", students);
            response.addData("courses", courses);
            response.addData("enrollments", enrollments);
            response.addData("faculties", faculties);
            response.addData("classes", classes);
            response.addData("subjects", subjects);
            response.addData("users", users);
            response.addData("grades", grades);
            response.addData("notifications", notifications);
            response.addData("classOpeningRequests", classOpeningRequests);
            response.addData("courseRegistrations", courseRegistrations);

            // Gửi kèm version để client update
            Map<String, Object> serverMetadata = getServerMetadata();
            String versionKey = source.toLowerCase() + "_version";
            response.addData("client_source_version", serverMetadata.get(versionKey));
            response.addData("client_source", source);

            return response;

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải dữ liệu: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(Constants.ACTION_DOWNLOAD_DATA, "Lỗi: " + e.getMessage());
        }
    }

    private List<com.university.sms.model.Student> getStudentsBySource(String source) {
        // Query lấy TẤT CẢ student có source = CSV, không phân biệt active/inactive
        String sql = "SELECT s.*, u.full_name, u.email, u.phone, u.address, u.is_active, " +
                "f.faculty_name, c.class_name " +
                "FROM students s " +
                "LEFT JOIN users u ON s.username = u.username " +
                "LEFT JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "LEFT JOIN classes c ON s.class_code = c.class_code " +
                "JOIN data_origin dor ON dor.entity_type = 'student' AND dor.entity_id = s.student_id " +
                "WHERE dor.source = ? " +
                "ORDER BY s.student_id";

        List<com.university.sms.model.Student> students = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Student student = new com.university.sms.model.Student();
                    int studentId = rs.getInt("student_id");
                    student.setStudentId(studentId);
                    student.setUsername(rs.getString("username"));
                    student.setStudentCode(rs.getString("student_code"));
                    String classCode = rs.getString("class_code");
                    if (!rs.wasNull()) {
                        student.setClassCode(classCode);
                    }
                    student.setFacultyCode(rs.getString("faculty_code"));
                    student.setAdmissionYear(rs.getInt("admission_year"));
                    String status = rs.getString("student_status");
                    if (status != null) {
                        student.setStudentStatus(
                                com.university.sms.model.Student.StudentStatus.valueOf(status.toUpperCase()));
                    }
                    student.setGpa(rs.getBigDecimal("gpa"));
                    student.setTotalCredits(rs.getInt("total_credits"));
                    student.setBirthDate(rs.getDate("birth_date"));
                    String gender = rs.getString("gender");
                    if (gender != null) {
                        student.setGender(com.university.sms.model.Student.Gender.valueOf(gender.toUpperCase()));
                    }
                    student.setCitizenId(rs.getString("citizen_id"));
                    student.setEmergencyContact(rs.getString("emergency_contact"));
                    student.setEmergencyPhone(rs.getString("emergency_phone"));
                    // Handle NULL values from LEFT JOIN
                    student.setFullName(rs.getString("full_name"));
                    student.setEmail(rs.getString("email"));
                    student.setPhone(rs.getString("phone"));
                    student.setAddress(rs.getString("address"));
                    try {
                        if (rs.getObject("is_active") != null) {
                            student.setActive(rs.getBoolean("is_active"));
                        } else {
                            student.setActive(true);
                        }
                    } catch (java.sql.SQLException e) {
                        student.setActive(true);
                    }
                    student.setFacultyName(rs.getString("faculty_name"));
                    student.setClassName(rs.getString("class_name"));
                    students.add(student);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách sinh viên theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }
        return students;
    }

    /**
     * Helper: Lấy courses có source = 'CSV'
     */
    private List<com.university.sms.model.Course> getCoursesBySource(String source) {
        String sql = "SELECT c.*, sub.subject_name, sub.subject_code, sub.credits, " +
                "u.full_name AS teacher_name, cl.class_name " +
                "FROM courses c " +
                "JOIN subjects sub ON c.subject_code = sub.subject_code " +
                "JOIN users u ON c.teacher_username = u.username " +
                "LEFT JOIN classes cl ON c.class_code = cl.class_code " +
                "JOIN data_origin dor ON dor.entity_type = 'course' AND dor.entity_id = c.course_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Course> courses = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Course course = new com.university.sms.model.Course();
                    course.setCourseId(rs.getInt("course_id"));
                    course.setCourseCode(rs.getString("course_code"));
                    course.setSubjectCode(rs.getString("subject_code"));
                    course.setTeacherUsername(rs.getString("teacher_username"));
                    String classCode = rs.getString("class_code");
                    if (!rs.wasNull()) {
                        course.setClassCode(classCode);
                    }
                    course.setAcademicYear(rs.getString("academic_year"));
                    course.setSemester(rs.getInt("semester"));
                    course.setScheduleDay(rs.getString("schedule_day"));
                    course.setScheduleTime(rs.getString("schedule_time"));
                    course.setRoom(rs.getString("room"));
                    course.setMaxStudents(rs.getInt("max_students"));
                    course.setCurrentStudents(rs.getInt("current_students"));
                    String status = rs.getString("course_status");
                    if (status != null) {
                        course.setCourseStatus(
                                com.university.sms.model.Course.CourseStatus.valueOf(status.toUpperCase()));
                    }
                    course.setStartDate(rs.getDate("start_date"));
                    course.setEndDate(rs.getDate("end_date"));
                    course.setSubjectName(rs.getString("subject_name"));
                    course.setCredits(rs.getInt("credits"));
                    course.setTeacherName(rs.getString("teacher_name"));
                    course.setClassName(rs.getString("class_name"));
                    courses.add(course);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách khóa học theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return courses;
    }

    /**
     * Helper: Lấy enrollments có source = 'CSV'
     */
    private List<com.university.sms.model.Enrollment> getEnrollmentsBySource(String source) {
        String sql = "SELECT e.* FROM enrollments e " +
                "JOIN data_origin dor ON dor.entity_type = 'enrollment' AND dor.entity_id = e.enrollment_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Enrollment> enrollments = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Enrollment enrollment = new com.university.sms.model.Enrollment();
                    enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
                    enrollment.setStudentCode(rs.getString("student_code"));
                    enrollment.setCourseCode(rs.getString("course_code"));
                    enrollment.setEnrollmentDate(rs.getTimestamp("enrollment_date"));

                    // Xử lý enrollment_status - đảm bảo luôn có giá trị
                    String status = rs.getString("enrollment_status");
                    if (status != null && !status.trim().isEmpty()) {
                        try {
                            enrollment.setEnrollmentStatus(
                                    com.university.sms.model.Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            // Nếu status không hợp lệ, dùng ENROLLED làm mặc định
                            LOGGER.warning("Enrollment status không hợp lệ: " + status + " cho enrollment ID: "
                                    + enrollment.getEnrollmentId() + ", dùng ENROLLED làm mặc định");
                            enrollment
                                    .setEnrollmentStatus(com.university.sms.model.Enrollment.EnrollmentStatus.ENROLLED);
                        }
                    } else {
                        // Nếu status là null hoặc rỗng, dùng ENROLLED làm mặc định
                        enrollment.setEnrollmentStatus(com.university.sms.model.Enrollment.EnrollmentStatus.ENROLLED);
                    }

                    enrollment.setFinalGrade(rs.getBigDecimal("final_grade"));
                    enrollment.setLetterGrade(rs.getString("letter_grade"));
                    enrollment.setGradePoints(rs.getBigDecimal("grade_points"));
                    enrollments.add(enrollment);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách đăng ký học phần theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return enrollments;
    }

    /**
     * Helper: Lấy faculties có source = 'CSV'
     */
    private List<com.university.sms.model.Faculty> getFacultiesBySource(String source) {
        String sql = "SELECT f.* FROM faculties f " +
                "JOIN data_origin dor ON dor.entity_type = 'faculty' AND dor.entity_id = f.faculty_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Faculty> faculties = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Faculty faculty = new com.university.sms.model.Faculty();
                    faculty.setFacultyId(rs.getInt("faculty_id"));
                    faculty.setFacultyCode(rs.getString("faculty_code"));
                    faculty.setFacultyName(rs.getString("faculty_name"));
                    faculty.setDescription(rs.getString("description"));
                    faculty.setHeadTeacherUsername(rs.getString("head_teacher_username"));
                    faculties.add(faculty);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách khoa theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return faculties;
    }

    /**
     * Helper: Lấy classes có source = 'CSV'
     */
    private List<com.university.sms.model.Class> getClassesBySource(String source) {
        String sql = "SELECT c.* FROM classes c " +
                "JOIN data_origin dor ON dor.entity_type = 'class' AND dor.entity_id = c.class_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Class> classes = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Class classEntity = new com.university.sms.model.Class();
                    classEntity.setClassId(rs.getInt("class_id"));
                    classEntity.setClassCode(rs.getString("class_code"));
                    classEntity.setClassName(rs.getString("class_name"));
                    classEntity.setFacultyCode(rs.getString("faculty_code"));
                    classEntity.setTeacherUsername(rs.getString("teacher_username"));
                    classEntity.setAcademicYear(rs.getString("academic_year"));
                    classEntity.setSemester(rs.getInt("semester"));
                    classEntity.setMaxStudents(rs.getInt("max_students"));
                    classes.add(classEntity);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách lớp theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return classes;
    }

    /**
     * Helper: Lấy subjects có source = 'CSV'
     */
    private List<com.university.sms.model.Subject> getSubjectsBySource(String source) {
        String sql = "SELECT s.*, f.faculty_name FROM subjects s " +
                "LEFT JOIN faculties f ON s.faculty_code = f.faculty_code " +
                "JOIN data_origin dor ON dor.entity_type = 'subject' AND dor.entity_id = s.subject_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Subject> subjects = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Subject subject = new com.university.sms.model.Subject();
                    subject.setSubjectId(rs.getInt("subject_id"));
                    subject.setSubjectCode(rs.getString("subject_code"));
                    subject.setSubjectName(rs.getString("subject_name"));
                    subject.setCredits(rs.getInt("credits"));
                    subject.setFacultyCode(rs.getString("faculty_code"));
                    subject.setPrerequisiteSubjectCode(rs.getString("prerequisite_subject_code"));
                    subject.setDescription(rs.getString("description"));
                    subject.setRequired(rs.getBoolean("is_required"));
                    subject.setFacultyName(rs.getString("faculty_name"));
                    subjects.add(subject);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách môn học theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return subjects;
    }

    /**
     * Helper: Lấy users có source = 'CSV'
     */
    private List<com.university.sms.model.User> getUsersBySource(String source) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN data_origin dor ON dor.entity_type = 'user' AND dor.entity_id = u.user_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.User> users = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.User user = new com.university.sms.model.User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setAddress(rs.getString("address"));
                    String role = rs.getString("role");
                    if (role != null) {
                        user.setRole(com.university.sms.model.User.UserRole.valueOf(role.toUpperCase()));
                    }
                    user.setActive(rs.getBoolean("is_active"));
                    users.add(user);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách người dùng theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return users;
    }

    /**
     * Helper: Lấy grades có source = 'CSV'
     */
    private List<com.university.sms.model.Grade> getGradesBySource(String source) {
        String sql = "SELECT g.* FROM grades g " +
                "JOIN data_origin dor ON dor.entity_type = 'grade' AND dor.entity_id = g.grade_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Grade> grades = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Grade grade = new com.university.sms.model.Grade();
                    grade.setGradeId(rs.getInt("grade_id"));
                    grade.setStudentCode(rs.getString("student_code"));
                    grade.setCourseCode(rs.getString("course_code"));
                    String gradeType = rs.getString("grade_type");
                    if (gradeType != null) {
                        grade.setGradeType(com.university.sms.model.Grade.GradeType.valueOf(gradeType.toUpperCase()));
                    }
                    grade.setGradeName(rs.getString("grade_name"));
                    grade.setScore(rs.getBigDecimal("score"));
                    grade.setMaxScore(rs.getBigDecimal("max_score"));
                    grade.setWeight(rs.getBigDecimal("weight"));
                    grade.setNotes(rs.getString("notes"));
                    grade.setCreatedAt(rs.getTimestamp("created_at"));
                    grades.add(grade);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách điểm theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return grades;
    }

    /**
     * Helper: Lấy notifications có source = 'CSV'
     */
    private List<com.university.sms.model.Notification> getNotificationsBySource(String source) {
        String sql = "SELECT n.* FROM notifications n " +
                "JOIN data_origin dor ON dor.entity_type = 'notification' AND dor.entity_id = n.notification_id " +
                "WHERE dor.source = ?";

        List<com.university.sms.model.Notification> notifications = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.Notification notification = new com.university.sms.model.Notification();
                    notification.setNotificationId(rs.getInt("notification_id"));
                    notification.setTitle(rs.getString("title"));
                    notification.setContent(rs.getString("content"));
                    notification.setSenderUsername(rs.getString("sender_username"));
                    String targetType = rs.getString("target_type");
                    if (targetType != null) {
                        notification.setTargetType(
                                com.university.sms.model.Notification.TargetType.valueOf(targetType.toUpperCase()));
                    }
                    notification.setTargetCode(rs.getString("target_code"));
                    String priority = rs.getString("priority");
                    if (priority != null) {
                        notification.setPriority(
                                com.university.sms.model.Notification.Priority.valueOf(priority.toUpperCase()));
                    }
                    notification.setRead(rs.getBoolean("is_read"));
                    notification.setCreatedAt(rs.getTimestamp("created_at"));
                    java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (!rs.wasNull()) {
                        notification.setExpiresAt(expiresAt);
                    }
                    notifications.add(notification);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách thông báo theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return notifications;
    }

    /**
     * Helper: Lấy class opening requests có source = 'CSV'
     */
    private List<com.university.sms.model.ClassOpeningRequest> getClassOpeningRequestsBySource(String source) {
        String sql = "SELECT cor.* FROM class_opening_requests cor " +
                "JOIN data_origin dor ON dor.entity_type = 'class_opening_request' AND dor.entity_id = cor.request_id "
                +
                "WHERE dor.source = ?";

        List<com.university.sms.model.ClassOpeningRequest> requests = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.ClassOpeningRequest request = new com.university.sms.model.ClassOpeningRequest();
                    request.setRequestId(rs.getInt("request_id"));
                    request.setTeacherUsername(rs.getString("teacher_username"));
                    request.setSubjectCode(rs.getString("subject_code"));
                    request.setAcademicYear(rs.getString("academic_year"));
                    request.setSemester(rs.getInt("semester"));
                    request.setScheduleDay(rs.getString("schedule_day"));
                    request.setScheduleTime(rs.getString("schedule_time"));
                    request.setRoom(rs.getString("room"));
                    int maxStudents = rs.getInt("max_students");
                    if (!rs.wasNull()) {
                        request.setMaxStudents(maxStudents);
                    }
                    request.setReason(rs.getString("reason"));
                    String status = rs.getString("request_status");
                    if (status != null) {
                        request.setRequestStatus(com.university.sms.model.ClassOpeningRequest.RequestStatus
                                .valueOf(status.toUpperCase()));
                    }
                    request.setAdminNote(rs.getString("admin_note"));
                    request.setRequestDate(rs.getTimestamp("request_date"));
                    java.sql.Timestamp decisionDate = rs.getTimestamp("decision_date");
                    if (!rs.wasNull()) {
                        request.setDecisionDate(decisionDate);
                    }
                    request.setCreatedAt(rs.getTimestamp("created_at"));
                    java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (!rs.wasNull()) {
                        request.setUpdatedAt(updatedAt);
                    }
                    requests.add(request);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách yêu cầu mở lớp theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return requests;
    }

    /**
     * Helper: Lấy course registrations có source = 'CSV'
     */
    private List<com.university.sms.model.CourseRegistration> getCourseRegistrationsBySource(String source) {
        String sql = "SELECT cr.* FROM course_registrations cr " +
                "JOIN data_origin dor ON dor.entity_type = 'course_registration' AND dor.entity_id = cr.registration_id "
                +
                "WHERE dor.source = ?";

        List<com.university.sms.model.CourseRegistration> registrations = new java.util.ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.university.sms.model.CourseRegistration registration = new com.university.sms.model.CourseRegistration();
                    registration.setRegistrationId(rs.getInt("registration_id"));
                    registration.setStudentCode(rs.getString("student_code"));
                    registration.setCourseCode(rs.getString("course_code"));
                    registration.setRegistrationDate(rs.getTimestamp("registration_date"));
                    String status = rs.getString("registration_status");
                    if (status != null) {
                        registration
                                .setRegistrationStatus(com.university.sms.model.CourseRegistration.RegistrationStatus
                                        .valueOf(status.toUpperCase()));
                    }
                    java.sql.Timestamp cancelDate = rs.getTimestamp("cancel_date");
                    if (!rs.wasNull()) {
                        registration.setCancelDate(cancelDate);
                    }
                    registration.setNotes(rs.getString("notes"));
                    registration.setCreatedAt(rs.getTimestamp("created_at"));
                    registrations.add(registration);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy danh sách đăng ký khóa học theo nguồn '" + source + "': " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
        }

        return registrations;
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
     * Xử lý upload students từ client
     */
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
                    // ✅ REFACTORED: Ensure user exists based on username (code-based approach)
                    String username = student.getUsername();
                    boolean userOk = true;
                    if (username == null || username.isEmpty()) {
                        // Fallback: use studentCode as username if not set
                        username = student.getStudentCode();
                        student.setUsername(username);
                    }

                    // Check if user exists by username
                    com.university.sms.model.User existingUser = userDAO.findByUsername(username);
                    if (existingUser == null) {
                        // Create a minimal user from student info
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

                    // ✅ REFACTORED: Ensure class exists (if specified)
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

                    // ✅ REFACTORED: Ensure faculty exists
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

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
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

            String message = String.format("Uploaded %d students successfully, %d failed",
                    successCount, failCount);

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
                    // ✅ REFACTORED: Ensure subject exists
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

                    // ✅ REFACTORED: Ensure class exists (if specified)
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

                    // ✅ REFACTORED: Ensure teacher user exists based on username (code-based
                    // approach)
                    String teacherUsername = course.getTeacherUsername();
                    boolean userOk = true;
                    if (teacherUsername != null && !teacherUsername.isEmpty()) {
                        // Check if teacher user exists by username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(teacherUsername);
                        if (existingUser == null) {
                            // Create a minimal teacher user from course info
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(teacherUsername);
                            u.setPassword("password");
                            u.setFullName(course.getTeacherName() != null ? course.getTeacherName() : teacherUsername);
                            u.setEmail(teacherUsername + "@csv-teacher.edu.vn"); // Generate email from username
                            u.setRole(com.university.sms.model.User.UserRole.TEACHER);
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
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
                                    + course.getCourseName());
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("Course already exists, skipping: " + course.getCourseCode() + " (source: "
                                + clientSource + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi tải lên khóa học " + course.getCourseCode() + ": " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = String.format("Uploaded %d courses successfully, %d failed",
                    successCount, failCount);

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

            for (com.university.sms.model.Enrollment e : enrollments) {
                try {
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh duplicate)
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
                                    LOGGER.info("Enrollment already exists, skipping: student=" + e.getStudentCode() +
                                            ", course=" + e.getCourseCode() + " (source: " + clientSource + ")");
                                }
                            }
                        }
                    } catch (Exception checkEx) {
                        LOGGER.warning("Error checking enrollment duplicate: " + checkEx.getMessage());
                        // Nếu check lỗi, vẫn tiếp tục insert (không block)
                    }

                    if (!exists) {
                        // Reset ID để database tự tăng, không giữ ID từ CSV
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
                    }
                    // Nếu đã tồn tại, không đếm vào successCount hoặc failCount, chỉ log
                } catch (Exception ex) {
                    failCount++;
                    LOGGER.severe("Lỗi khi lưu đăng ký học phần: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "Chi tiết lỗi", ex);
                }
            }

            String message = String.format("Uploaded %d enrollments successfully, %d failed",
                    successCount, failCount);
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

            for (com.university.sms.model.User u : users) {
                try {
                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
                    // khác)
                    com.university.sms.model.User existing = userDAO.findByUsername(u.getUsername());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        u.setUserId(0); // Reset ID để database tự tăng
                        if (userDAO.addUser(u)) {
                            saveDataOrigin("user", u.getUserId(), clientSource);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    } else {
                        // Đã tồn tại → SKIP (không update, không insert)
                        LOGGER.info("User already exists, skipping: " + u.getUsername() + " (source: " + clientSource
                                + ")");
                        // Không đếm vào successCount hoặc failCount, chỉ log
                    }
                } catch (Exception ex) {
                    failCount++;
                }
            }

            String message = String.format("Uploaded %d users successfully, %d failed",
                    successCount, failCount);
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
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();

            for (com.university.sms.model.Faculty f : faculties) {
                try {
                    // ✅ REFACTORED: Ensure head teacher user exists based on username (code-based
                    // approach)
                    String headTeacherUsername = f.getHeadTeacherUsername();
                    boolean userOk = true;
                    if (headTeacherUsername != null && !headTeacherUsername.isEmpty()) {
                        // Check if head teacher user exists by username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(headTeacherUsername);
                        if (existingUser == null) {
                            // Create a minimal teacher user from faculty info
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(headTeacherUsername);
                            u.setPassword("password");
                            u.setFullName(
                                    f.getHeadTeacherName() != null ? f.getHeadTeacherName() : headTeacherUsername);
                            u.setEmail(headTeacherUsername + "@csv-teacher.edu.vn"); // Generate email from username
                            u.setRole(com.university.sms.model.User.UserRole.TEACHER);
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
                    // khác)
                    com.university.sms.model.Faculty existing = facultyDAO.findByCode(f.getFacultyCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        f.setFacultyId(0); // Reset ID để database tự tăng
                        if (userOk && facultyDAO.addFaculty(f)) {
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

            String message = String.format("Uploaded %d faculties successfully, %d failed",
                    successCount, failCount);
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
                    // ✅ REFACTORED: Ensure faculty exists
                    String facultyCode = c.getFacultyCode();
                    boolean facultyOk = true;
                    if (facultyCode != null && !facultyCode.isEmpty()) {
                        com.university.sms.model.Faculty existingFaculty = facultyDAO.findByCode(facultyCode);
                        if (existingFaculty == null) {
                            facultyOk = false;
                            LOGGER.warning("Faculty code not found: " + facultyCode + " for class " + c.getClassCode());
                        }
                    }

                    // ✅ REFACTORED: Ensure teacher user exists based on username (code-based
                    // approach)
                    String teacherUsername = c.getTeacherUsername();
                    boolean userOk = true;
                    if (teacherUsername != null && !teacherUsername.isEmpty()) {
                        // Check if teacher user exists by username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(teacherUsername);
                        if (existingUser == null) {
                            // Create a minimal teacher user from class info
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(teacherUsername);
                            u.setPassword("password");
                            u.setFullName(teacherUsername); // Class model doesn't have teacher name, use username
                            u.setEmail(teacherUsername + "@csv-teacher.edu.vn"); // Generate email from username
                            u.setRole(com.university.sms.model.User.UserRole.TEACHER);
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
                    // khác)
                    com.university.sms.model.Class existing = classDAO.findByCode(c.getClassCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        c.setClassId(0); // Reset ID để database tự tăng
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

            String message = String.format("Uploaded %d classes successfully, %d failed",
                    successCount, failCount);
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
                    // ✅ REFACTORED: Ensure faculty exists
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

                    // ✅ REFACTORED: Ensure prerequisite subject exists (if specified)
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

                    // Chỉ INSERT mới, không UPDATE nếu đã tồn tại (tránh ghi đè data từ source
                    // khác)
                    com.university.sms.model.Subject existing = subjectDAO.findByCode(s.getSubjectCode());
                    if (existing == null) {
                        // Chưa tồn tại → INSERT mới
                        s.setSubjectId(0); // Reset ID để database tự tăng
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

            String message = String.format("Uploaded %d subjects successfully, %d failed",
                    successCount, failCount);
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
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh duplicate)
                    // Check duplicate dựa trên student_code, course_code, grade_type, grade_name
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
                        // Nếu check lỗi, vẫn tiếp tục insert (không block)
                    }

                    if (!exists) {
                        // Reset ID để database tự tăng, không giữ ID từ CSV
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

            String message = String.format("Uploaded %d grades successfully, %d failed",
                    successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_GRADES, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi xử lý tải lên grades: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_GRADES, "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload class opening requests từ client
     */
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
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh duplicate)
                    // Check duplicate dựa trên teacher, subject, academic_year, semester,
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
                        // Reset ID để database tự tăng, không giữ ID từ CSV
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

            String message = String.format("Uploaded %d class opening requests successfully, %d failed",
                    successCount, failCount);
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
                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh duplicate)
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
                        // Nếu check lỗi, vẫn tiếp tục insert (không block)
                    }

                    if (!exists) {
                        // Reset ID để database tự tăng, không giữ ID từ CSV
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

            String message = String.format("Uploaded %d course registrations successfully, %d failed",
                    successCount, failCount);
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
                    // ✅ REFACTORED: Ensure sender user exists based on username (code-based
                    // approach)
                    String senderUsername = n.getSenderUsername();
                    boolean userOk = true;
                    if (senderUsername != null && !senderUsername.isEmpty()) {
                        // Check if sender user exists by username
                        com.university.sms.model.User existingUser = userDAO.findByUsername(senderUsername);
                        if (existingUser == null) {
                            // Create a minimal user from notification info
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(senderUsername);
                            u.setPassword("password");
                            u.setFullName(n.getSenderName() != null ? n.getSenderName() : senderUsername);
                            u.setEmail(senderUsername + "@csv-admin.edu.vn"); // Generate email from username
                            u.setRole(com.university.sms.model.User.UserRole.ADMIN); // Default to ADMIN for
                                                                                     // notifications
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    // Chỉ INSERT mới, không INSERT nếu đã tồn tại (tránh duplicate)
                    // Check duplicate dựa trên title, content, sender_username, target_type,
                    // target_code
                    boolean exists = false;
                    try {
                        // Query để check duplicate
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
                        // Nếu check lỗi, vẫn tiếp tục insert (không block)
                    }

                    if (!exists && userOk) {
                        // Reset ID để database tự tăng, không giữ ID từ CSV
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

            String message = String.format("Uploaded %d notifications successfully, %d failed",
                    successCount, failCount);
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
    private void saveDataOrigin(String entityType, int entityId, String source) {
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
    private void updateDataOriginTimestamp(String entityType, int entityId) {
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
    private String getDataOrigin(String entityType, int entityId) {
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

    private Message handleGetAllClassRequests(Message request) {
        try {
            List<ClassOpeningRequest> requests = classRequestService.getAllRequests();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_CLASS_REQUESTS, requests);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy all class requests: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetClassRequestById(Message request) {
        try {
            int requestId = (Integer) request.getData(Constants.KEY_REQUEST_ID);
            ClassOpeningRequest classRequest = classRequestService.getRequestById(requestId);

            if (classRequest == null) {
                return Message.createErrorResponse(request.getAction(), "Request not found");
            }

            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_CLASS_REQUEST, classRequest);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy class request by ID: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetMyClassRequests(Message request) {
        try {
            String teacherUsername = request.getData("teacherUsername", String.class);
            if (teacherUsername == null || teacherUsername.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Teacher username is required");
            }
            List<ClassOpeningRequest> requests = classRequestService.getRequestsByTeacher(teacherUsername);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_CLASS_REQUESTS, requests);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy teacher's class requests: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetPendingClassRequests(Message request) {
        try {
            List<ClassOpeningRequest> requests = classRequestService.getPendingRequests();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_CLASS_REQUESTS, requests);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy pending class requests: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleSubmitClassRequest(Message request) {
        try {
            ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
            boolean success = classRequestService.submitRequest(classRequest);

            if (success) {
                // Lưu data origin sau khi submit thành công
                if (classRequest != null && classRequest.getRequestId() > 0) {
                    saveDataOrigin("class_opening_request", classRequest.getRequestId(), clientSource);
                }
                return Message.createSuccessResponse(request.getAction(), "Request submitted successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to submit request");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi gửi class request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleUpdateClassRequest(Message request) {
        try {
            ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
            boolean success = classRequestService.updateRequest(classRequest);

            if (success) {
                // Cập nhật data origin sau khi update thành công
                if (classRequest != null && classRequest.getRequestId() > 0) {
                    saveDataOrigin("class_opening_request", classRequest.getRequestId(), clientSource);
                }
                return Message.createSuccessResponse(request.getAction(), "Request updated successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to update request");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi cập nhật class request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleCancelClassRequest(Message request) {
        try {
            int requestId = (Integer) request.getData(Constants.KEY_REQUEST_ID);
            String teacherUsername = request.getData("teacherUsername", String.class);
            if (teacherUsername == null || teacherUsername.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Teacher username is required");
            }
            // Cập nhật timestamp của source gốc trước khi cancel
            ClassOpeningRequest classRequest = classRequestService.getRequestById(requestId);
            if (classRequest != null && classRequest.getRequestId() > 0) {
                String existingSource = getDataOrigin("class_opening_request", classRequest.getRequestId());
                if (existingSource != null) {
                    updateDataOriginTimestamp("class_opening_request", classRequest.getRequestId());
                }
            }

            boolean success = classRequestService.cancelRequest(requestId, teacherUsername);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Request cancelled successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to cancel request");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi hủy class request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleApproveClassRequest(Message request) {
        try {
            // Kiểm tra quyền Admin
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ Admin mới có quyền duyệt yêu cầu");
            }

            Integer requestId = request.getData(Constants.KEY_REQUEST_ID, Integer.class);
            if (requestId == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu thông tin request ID");
            }

            String adminUsername = currentUser.getUsername();
            String note = request.getData(Constants.KEY_NOTE, String.class);

            boolean success = classRequestService.approveRequest(requestId, adminUsername, note);

            if (success) {
                LOGGER.info("Admin " + currentUser.getUsername() + " approved request " + requestId);
                // Lấy request đã được cập nhật để trả về cho client
                ClassOpeningRequest updatedRequest = classRequestService.getRequestById(requestId);
                // saveDataOrigin sẽ tự động cập nhật timestamp nếu request có source CSV
                if (updatedRequest != null && updatedRequest.getRequestId() > 0) {
                    saveDataOrigin("class_opening_request", updatedRequest.getRequestId(), clientSource);
                }
                Message response = Message.createSuccessResponse(request.getAction(), "Đã duyệt yêu cầu thành công");
                if (updatedRequest != null) {
                    response.addData("request", updatedRequest);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể duyệt yêu cầu");
            }
        } catch (IllegalStateException e) {
            LOGGER.warning("Cannot approve request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi duyệt yêu cầu mở lớp: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleRejectClassRequest(Message request) {
        try {
            // Kiểm tra quyền Admin
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ Admin mới có quyền từ chối yêu cầu");
            }

            Integer requestId = request.getData(Constants.KEY_REQUEST_ID, Integer.class);
            if (requestId == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu thông tin request ID");
            }

            String adminUsername = currentUser.getUsername();
            String reason = request.getData(Constants.KEY_REASON, String.class);

            if (reason == null || reason.trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Vui lòng nhập lý do từ chối");
            }

            boolean success = classRequestService.rejectRequest(requestId, adminUsername, reason);

            if (success) {
                LOGGER.info("Admin " + currentUser.getUsername() + " rejected request " + requestId);
                // Lấy request đã được cập nhật để trả về cho client
                ClassOpeningRequest updatedRequest = classRequestService.getRequestById(requestId);
                // saveDataOrigin sẽ tự động cập nhật timestamp nếu request có source CSV
                if (updatedRequest != null && updatedRequest.getRequestId() > 0) {
                    saveDataOrigin("class_opening_request", updatedRequest.getRequestId(), clientSource);
                }
                Message response = Message.createSuccessResponse(request.getAction(), "Đã từ chối yêu cầu");
                if (updatedRequest != null) {
                    response.addData("request", updatedRequest);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể từ chối yêu cầu");
            }
        } catch (IllegalStateException e) {
            LOGGER.warning("Cannot reject request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi từ chối yêu cầu mở lớp: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Chi tiết lỗi", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetClassRequestStats(Message request) {
        try {
            ClassOpeningRequestService.RequestStatistics stats = classRequestService.getStatistics();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_STATISTICS, stats);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy class request stats: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    // ========== COURSE REGISTRATION HANDLERS ==========

    private Message handleGetAllRegistrations(Message request) {
        try {
            List<CourseRegistration> registrations = registrationService.getAllRegistrations();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy all registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetRegistrationById(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            CourseRegistration registration = registrationService.getRegistrationById(registrationId);

            if (registration == null) {
                return Message.createErrorResponse(request.getAction(), "Registration not found");
            }

            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATION, registration);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy registration by ID: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetMyRegistrations(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            if (studentCode == null || studentCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Student code is required");
            }

            List<CourseRegistration> registrations = registrationService.getRegistrationsByStudent(studentCode);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy student's registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetCourseRegistrations(Message request) {
        try {
            String courseCode = request.getData("courseCode", String.class);
            if (courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Course code is required");
            }
            List<CourseRegistration> registrations = registrationService.getRegistrationsByCourse(courseCode);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy course registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetPendingRegistrations(Message request) {
        try {
            List<CourseRegistration> registrations = registrationService.getPendingRegistrations();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy pending registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleRegisterCourse(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String courseCode = request.getData("courseCode", String.class);
            String notes = request.getData(Constants.KEY_NOTE, String.class);

            if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(),
                        "Student code and course code are required");
            }

            boolean success = registrationService.registerCourse(studentCode, courseCode, notes);

            if (success) {
                // Lấy registration đã được tạo để lưu data origin
                CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
                // Tìm registration mới nhất của student và course này
                List<CourseRegistration> registrations = registrationDAO.findByStudent(studentCode);
                CourseRegistration registration = registrations.stream()
                        .filter(r -> courseCode.equals(r.getCourseCode()))
                        .findFirst()
                        .orElse(null);
                if (registration != null && registration.getRegistrationId() > 0) {
                    saveDataOrigin("course_registration", registration.getRegistrationId(), clientSource);
                }
                return Message.createSuccessResponse(request.getAction(), "Registration submitted successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to submit registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi đăng ký course: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleCancelRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            String studentCode = request.getData("studentCode", String.class);

            if (studentCode == null || studentCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Student code is required");
            }

            // Cập nhật timestamp của source gốc trước khi cancel
            CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
            CourseRegistration registrationBeforeCancel = registrationDAO.findById(registrationId);
            if (registrationBeforeCancel != null && registrationBeforeCancel.getRegistrationId() > 0) {
                String existingSource = getDataOrigin("course_registration",
                        registrationBeforeCancel.getRegistrationId());
                if (existingSource != null) {
                    updateDataOriginTimestamp("course_registration", registrationBeforeCancel.getRegistrationId());
                }
            }

            boolean success = registrationService.cancelRegistration(registrationId, studentCode);

            if (success) {
                // Lấy lại registration data sau khi cancel để gửi về client
                CourseRegistration registration = registrationDAO.findById(registrationId);

                Message response = Message.createSuccessResponse(request.getAction(),
                        "Registration cancelled successfully");
                if (registration != null) {
                    response.addData(Constants.KEY_REGISTRATION, registration);
                    LOGGER.info("Returning course registration data to client: " + registrationId +
                            " (status: " + registration.getRegistrationStatus() + ")");
                } else {
                    LOGGER.warning("Course registration not found after cancel: " + registrationId);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to cancel registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi hủy registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleApproveRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            boolean success = registrationService.approveRegistration(registrationId);

            if (success) {
                // Lấy lại registration data sau khi approve để gửi về client
                CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
                CourseRegistration registration = registrationDAO.findById(registrationId);

                // Cập nhật data_origin để tăng version cho nguồn tương ứng (CSV nếu bản ghi gốc
                // từ CSV)
                if (registration != null) {
                    saveDataOrigin("course_registration", registration.getRegistrationId(), clientSource);
                }

                Message response = Message.createSuccessResponse(request.getAction(),
                        "Registration approved successfully");
                if (registration != null) {
                    response.addData(Constants.KEY_REGISTRATION, registration);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to approve registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi duyệt registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleRejectRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            String reason = (String) request.getData(Constants.KEY_REASON);

            boolean success = registrationService.rejectRegistration(registrationId, reason);

            if (success) {
                // Cập nhật data_origin để tăng version cho nguồn tương ứng (CSV nếu bản ghi gốc
                // từ CSV)
                CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
                CourseRegistration registration = registrationDAO.findById(registrationId);
                if (registration != null) {
                    saveDataOrigin("course_registration", registration.getRegistrationId(), clientSource);
                }

                return Message.createSuccessResponse(request.getAction(), "Registration rejected successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to reject registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Error rejecting registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleValidateRegistration(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String courseCode = request.getData("courseCode", String.class);

            if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(),
                        "Student code and course code are required");
            }

            CourseRegistrationService.RegistrationValidation validation = registrationService
                    .validateRegistration(studentCode, courseCode);

            Message response = Message.createSuccessResponse(request.getAction(), validation.getMessage());
            response.addData("valid", validation.isValid());
            response.addData("message", validation.getMessage());
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error validating registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetStudentCredits(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String academicYear = request.getData(Constants.KEY_ACADEMIC_YEAR, String.class);
            Integer semester = request.getData(Constants.KEY_SEMESTER, Integer.class);

            if (studentCode == null || studentCode.isEmpty() || academicYear == null || semester == null) {
                return Message.createErrorResponse(request.getAction(),
                        "Student code, academic year, and semester are required");
            }

            int credits = registrationService.getStudentCredits(studentCode, academicYear, semester);

            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData("credits", credits);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy student credits: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetRegistrationStats(Message request) {
        try {
            CourseRegistrationService.RegistrationStatistics stats = registrationService.getStatistics();
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_STATISTICS, stats);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi lấy registration stats: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    // ==================== Teacher Management Handlers ====================

    private Message handleAddTeacher(Message request) {
        try {
            // Only admin can add teachers
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền thêm giảng viên");
            }

            String username = request.getData("username", String.class);
            String password = request.getData("password", String.class);
            String fullName = request.getData("fullName", String.class);
            String email = request.getData("email", String.class);
            String phone = request.getData("phone", String.class);
            String address = request.getData("address", String.class);
            String facultyCode = request.getData("facultyCode", String.class);

            // Validate required fields
            if (username == null || username.trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu tên đăng nhập");
            }
            if (password == null || password.trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu mật khẩu");
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu họ tên");
            }

            // Validate password strength (minimum 6 characters)
            if (password.length() < 6) {
                return Message.createErrorResponse(request.getAction(),
                        "Mật khẩu phải có ít nhất 6 ký tự");
            }

            // Validate email format (if provided)
            if (email != null && !email.trim().isEmpty()) {
                if (!isValidEmailFormat(email.trim())) {
                    return Message.createErrorResponse(request.getAction(),
                            "Email không hợp lệ. Email phải có định dạng: example@domain.com");
                }
            }

            // Check if username already exists
            UserDAO userDAO = new UserDAO();
            User existingUser = userDAO.findByUsername(username);
            if (existingUser != null) {
                return Message.createErrorResponse(request.getAction(),
                        "Tên đăng nhập đã tồn tại: " + username);
            }

            // Check if email already exists (if provided)
            if (email != null && !email.trim().isEmpty()) {
                User existingUserByEmail = userDAO.findByEmail(email.trim());
                if (existingUserByEmail != null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Email đã được sử dụng bởi user khác: " + email);
                }
            }

            // Normalize and validate phone format (if provided)
            String normalizedPhone = null;
            if (phone != null && !phone.trim().isEmpty()) {
                normalizedPhone = normalizePhoneNumber(phone.trim());
                if (!isValidPhoneFormat(normalizedPhone)) {
                    return Message.createErrorResponse(request.getAction(),
                            "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
                }
            }

            // Check if phone already exists (if provided)
            if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
                User existingUserByPhone = userDAO.findByPhone(normalizedPhone);
                if (existingUserByPhone != null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Số điện thoại đã được sử dụng bởi user khác: " + normalizedPhone);
                }
            }

            // Validate facultyCode if provided
            if (facultyCode != null && !facultyCode.trim().isEmpty()) {
                com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
                com.university.sms.model.Faculty faculty = facultyDAO.findByCode(facultyCode.trim());
                if (faculty == null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Mã khoa không tồn tại: " + facultyCode);
                }
            }

            // Create new teacher
            User newTeacher = new User();
            newTeacher.setUsername(username.trim());
            newTeacher.setPassword(password);
            newTeacher.setFullName(fullName.trim());
            newTeacher.setEmail(email != null ? email.trim() : null);
            newTeacher.setPhone(normalizedPhone); // Use normalized phone
            newTeacher.setAddress(address != null ? address.trim() : null);
            newTeacher.setFacultyCode(facultyCode != null && !facultyCode.trim().isEmpty() ? facultyCode.trim() : null);
            newTeacher.setRole(User.UserRole.TEACHER);
            newTeacher.setActive(true);

            boolean success = userDAO.addUser(newTeacher);

            if (success) {
                // Lưu data origin sau khi thêm thành công
                if (newTeacher.getUserId() > 0) {
                    saveDataOrigin("user", newTeacher.getUserId(), clientSource);
                }
                LOGGER.info("Teacher added: " + username + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Thêm giảng viên thành công");
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "Không thể thêm giảng viên. Tên đăng nhập có thể đã tồn tại.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding teacher", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleUpdateTeacher(Message request) {
        try {
            // Only admin can update teachers
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền cập nhật giảng viên");
            }

            Integer userId = request.getData("userId", Integer.class);
            if (userId == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu ID giảng viên");
            }

            UserDAO userDAO = new UserDAO();
            User teacher = userDAO.findById(userId);
            if (teacher == null || teacher.getRole() != User.UserRole.TEACHER) {
                return Message.createErrorResponse(request.getAction(), "Không tìm thấy giảng viên");
            }

            // Update fields
            String fullName = request.getData("fullName", String.class);
            String email = request.getData("email", String.class);
            String phone = request.getData("phone", String.class);
            String address = request.getData("address", String.class);
            String password = request.getData("password", String.class);
            String facultyCode = request.getData("facultyCode", String.class);

            // Validate facultyCode if provided
            if (facultyCode != null && !facultyCode.trim().isEmpty()) {
                com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
                com.university.sms.model.Faculty faculty = facultyDAO.findByCode(facultyCode.trim());
                if (faculty == null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Mã khoa không tồn tại: " + facultyCode);
                }
            }

            // Validate email format (if provided)
            if (email != null && !email.trim().isEmpty()) {
                if (!isValidEmailFormat(email.trim())) {
                    return Message.createErrorResponse(request.getAction(),
                            "Email không hợp lệ. Email phải có định dạng: example@domain.com");
                }
            }

            // Check if email already exists (if provided and different from current)
            if (email != null && !email.trim().isEmpty()) {
                User existingUserByEmail = userDAO.findByEmail(email.trim());
                if (existingUserByEmail != null && existingUserByEmail.getUserId() != teacher.getUserId()) {
                    return Message.createErrorResponse(request.getAction(),
                            "Email đã được sử dụng bởi user khác: " + email);
                }
            }
            // Normalize phone for comparison and validation
            String normalizedPhone = null;
            if (phone != null && !phone.trim().isEmpty()) {
                normalizedPhone = normalizePhoneNumber(phone.trim());
            }

            // Check if phone has changed (compare normalized versions)
            String currentPhone = teacher.getPhone();
            String normalizedCurrentPhone = currentPhone != null ? normalizePhoneNumber(currentPhone) : null;
            boolean phoneChanged = normalizedPhone != null
                    && !normalizedPhone.equals(normalizedCurrentPhone != null ? normalizedCurrentPhone : "");

            // Validate phone format (if provided and different from current)
            if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
                // Only validate format if phone has changed
                if (phoneChanged && !isValidPhoneFormat(normalizedPhone)) {
                    return Message.createErrorResponse(request.getAction(),
                            "Số điện thoại không hợp lệ. Số điện thoại phải có 10 số (bắt đầu bằng 0) hoặc 11 số (bắt đầu bằng +84). Ví dụ: 0912345678 hoặc +84912345678");
                }
            }

            // Check if phone already exists (if provided and different from current)
            if (normalizedPhone != null && !normalizedPhone.isEmpty() && phoneChanged) {
                User existingUserByPhone = userDAO.findByPhone(normalizedPhone);
                if (existingUserByPhone != null && existingUserByPhone.getUserId() != teacher.getUserId()) {
                    return Message.createErrorResponse(request.getAction(),
                            "Số điện thoại đã được sử dụng bởi user khác: " + normalizedPhone);
                }
            }

            if (fullName != null)
                teacher.setFullName(fullName);
            if (email != null)
                teacher.setEmail(email);
            if (phone != null) {
                // Use normalized phone (or keep current if not changed)
                teacher.setPhone(phoneChanged ? normalizedPhone : currentPhone);
            }
            if (address != null)
                teacher.setAddress(address);
            if (facultyCode != null) {
                teacher.setFacultyCode(facultyCode.trim().isEmpty() ? null : facultyCode.trim());
            }

            boolean success = userDAO.updateUser(teacher);

            if (password != null && !password.isEmpty()) {
                userDAO.changePassword(teacher.getUsername(), password);
            }

            if (success) {
                // Cập nhật data origin sau khi update thành công
                if (teacher.getUserId() > 0) {
                    saveDataOrigin("user", teacher.getUserId(), clientSource);
                }
                LOGGER.info("Teacher updated: " + teacher.getUsername() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Cập nhật giảng viên thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể cập nhật giảng viên");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating teacher", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleDeleteTeacher(Message request) {
        try {
            // Only admin can delete teachers
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa giảng viên");
            }

            Integer userId = request.getData("userId", Integer.class);
            if (userId == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu ID giảng viên");
            }

            UserDAO userDAO = new UserDAO();
            User teacher = userDAO.findById(userId);
            if (teacher == null || teacher.getRole() != User.UserRole.TEACHER) {
                return Message.createErrorResponse(request.getAction(), "Không tìm thấy giảng viên");
            }

            String teacherUsername = teacher.getUsername();

            // Kiểm tra xem có courses liên quan không
            CourseDAO courseDAO = new CourseDAO();
            List<Course> teacherCourses = courseDAO.findByTeacherUsername(teacherUsername);

            // Kiểm tra xem có class_opening_requests liên quan không
            ClassOpeningRequestService classRequestService = new ClassOpeningRequestService();
            List<ClassOpeningRequest> teacherRequests = classRequestService.getRequestsByTeacher(teacherUsername);

            if (!teacherCourses.isEmpty()) {
                // Có lớp đang diễn ra, không cho phép xóa
                String courseCodes = teacherCourses.stream()
                        .map(Course::getCourseCode)
                        .collect(java.util.stream.Collectors.joining(", "));
                String errorMsg = "Không thể vô hiệu hóa giảng viên vì vẫn còn lớp đang dạy (trạng thái ongoing). "
                        + "Các lớp chưa kết thúc: " + courseCodes;
                return Message.createErrorResponse(request.getAction(), errorMsg);
            }

            // Tự động chuyển các lớp planning thành cancelled
            List<Course> allTeacherCourses = courseDAO.findAllByTeacherUsername(teacherUsername);
            List<Course> planningCourses = allTeacherCourses.stream()
                    .filter(course -> course.getCourseStatus() == Course.CourseStatus.PLANNING)
                    .collect(java.util.stream.Collectors.toList());

            int cancelledCourses = 0;
            for (Course planningCourse : planningCourses) {
                try {
                    boolean updated = courseDAO.updateCourseStatus(planningCourse.getCourseId(),
                            Course.CourseStatus.CANCELLED);
                    if (updated) {
                        cancelledCourses++;
                        LOGGER.info("Auto-cancelled planning course " + planningCourse.getCourseCode()
                                + " for teacher " + teacherUsername);
                    } else {
                        LOGGER.warning("Failed to auto-cancel planning course " + planningCourse.getCourseCode()
                                + " for teacher " + teacherUsername);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING,
                            "Error auto-cancelling planning course " + planningCourse.getCourseCode()
                                    + " for teacher " + teacherUsername,
                            ex);
                }
            }

            // Tự động từ chối mọi yêu cầu mở lớp đang chờ xử lý
            List<ClassOpeningRequest> pendingRequests = teacherRequests.stream()
                    .filter(req -> req.getRequestStatus() == ClassOpeningRequest.RequestStatus.PENDING)
                    .collect(java.util.stream.Collectors.toList());

            int rejectedRequests = 0;
            for (ClassOpeningRequest pending : pendingRequests) {
                try {
                    boolean rejected = classRequestService.rejectRequest(
                            pending.getRequestId(),
                            currentUser.getUsername(),
                            "Tự động từ chối do vô hiệu hóa giảng viên");
                    if (rejected) {
                        rejectedRequests++;
                        LOGGER.info("Auto-rejected class opening request " + pending.getRequestId()
                                + " for teacher " + teacherUsername);
                    } else {
                        LOGGER.warning("Failed to auto-reject class opening request " + pending.getRequestId()
                                + " for teacher " + teacherUsername);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING,
                            "Error auto-rejecting class opening request " + pending.getRequestId()
                                    + " for teacher " + teacherUsername,
                            ex);
                }
            }

            // Cập nhật timestamp của source gốc trước khi deactivate
            if (teacher.getUserId() > 0) {
                String existingSource = getDataOrigin("user", teacher.getUserId());
                if (existingSource != null) {
                    updateDataOriginTimestamp("user", teacher.getUserId());
                }
            }

            // Không có dữ liệu liên quan, cho phép xóa
            boolean success = userDAO.deactivateUser(teacherUsername);

            if (success) {
                LOGGER.info("Teacher deactivated: " + teacherUsername + " by " + currentUser.getUsername());
                String successMessage = "Vô hiệu hóa giảng viên thành công";
                if (cancelledCourses > 0) {
                    successMessage += ". Đã tự động hủy " + cancelledCourses + " lớp đang ở trạng thái planning.";
                }
                if (rejectedRequests > 0) {
                    successMessage += " Đã tự động từ chối " + rejectedRequests + " yêu cầu mở lớp đang chờ.";
                }
                return Message.createSuccessResponse(request.getAction(), successMessage);
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể vô hiệu hóa giảng viên");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating teacher", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // ==================== User Activation Handler ====================

    private Message handleActivateUser(Message request) {
        try {
            // Only admin can activate users
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền kích hoạt người dùng");
            }

            Integer userId = request.getData("userId", Integer.class);
            if (userId == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu ID người dùng");
            }

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findById(userId);
            if (user == null) {
                return Message.createErrorResponse(request.getAction(), "Không tìm thấy người dùng");
            }

            boolean success = userDAO.activateUser(userId);

            if (success) {
                // Lấy lại user data sau khi activate (để có is_active = true)
                User activatedUser = userDAO.findById(userId);
                if (activatedUser != null) {
                    saveDataOrigin("user", activatedUser.getUserId(), clientSource);
                }

                String userType = user.getRole() == User.UserRole.TEACHER ? "giảng viên"
                        : user.getRole() == User.UserRole.STUDENT ? "sinh viên" : "người dùng";
                LOGGER.info("User activated: " + userId + " (" + user.getUsername() + ", " + userType + ") by "
                        + currentUser.getUsername());

                Message response = Message.createSuccessResponse(request.getAction(),
                        "Kích hoạt " + userType + " thành công");
                response.addData("user", activatedUser);

                // Nếu là student, cập nhật student status = ACTIVE và lấy student data để gửi
                // về client
                if (user.getRole() == User.UserRole.STUDENT) {
                    StudentDAO studentDAO = new StudentDAO();
                    Student student = studentDAO.findByUsername(user.getUsername());
                    if (student != null) {
                        // Set student status = ACTIVE khi activate user
                        studentDAO.updateStudentStatus(student.getStudentId(), Student.StudentStatus.ACTIVE);
                        saveDataOrigin("student", student.getStudentId(), clientSource);
                        // Lấy lại student data sau khi update status
                        student = studentDAO.findByUsername(user.getUsername());
                        if (student != null) {
                            LOGGER.info("Student status updated to ACTIVE: " + student.getStudentCode());
                            student.setStudentStatus(Student.StudentStatus.ACTIVE);
                            response.addData(Constants.KEY_STUDENT, student);
                        } else {
                            LOGGER.warning("Student not found after status update for username: " + user.getUsername());
                        }
                    } else {
                        LOGGER.warning("Student not found for username: " + user.getUsername() +
                                " - Cannot return student data to client");
                    }
                }

                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể kích hoạt người dùng");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating user", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // ==================== Teacher Handlers ====================

    private Message handleGetAllTeachers(Message request) {
        try {
            UserDAO userDAO = new UserDAO();
            List<User> teachers = userDAO.findByRole(User.UserRole.TEACHER);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + teachers.size() + " teachers");
            response.addData("teachers", teachers);

            LOGGER.info("Retrieved " + teachers.size() + " teachers");
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting teachers", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error retrieving teachers: " + e.getMessage());
        }
    }

    private Message handleGetAllTeachersIncludeInactive(Message request) {
        try {
            UserDAO userDAO = new UserDAO();
            List<User> teachers = userDAO.findByRoleIncludeInactive(User.UserRole.TEACHER);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + teachers.size() + " teachers (include inactive)");
            response.addData("teachers", teachers);

            LOGGER.info("Retrieved " + teachers.size() + " teachers (include inactive)");
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting teachers (include inactive)", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error retrieving teachers: " + e.getMessage());
        }
    }

    private Message handleSearchTeachers(Message request) {
        try {
            String keyword = request.getData("keyword", String.class);
            if (keyword == null || keyword.trim().isEmpty()) {
                return handleGetAllTeachers(request);
            }

            UserDAO userDAO = new UserDAO();
            List<User> allTeachers = userDAO.findByRole(User.UserRole.TEACHER);
            List<User> filteredTeachers = new java.util.ArrayList<>();

            String lowerKeyword = keyword.toLowerCase();
            for (User teacher : allTeachers) {
                if ((teacher.getFullName() != null && teacher.getFullName().toLowerCase().contains(lowerKeyword)) ||
                        (teacher.getUsername() != null && teacher.getUsername().toLowerCase().contains(lowerKeyword)) ||
                        (teacher.getEmail() != null && teacher.getEmail().toLowerCase().contains(lowerKeyword))) {
                    filteredTeachers.add(teacher);
                }
            }

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + filteredTeachers.size() + " teachers");
            response.addData("teachers", filteredTeachers);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching teachers", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error searching teachers: " + e.getMessage());
        }
    }

    private Message handleGetCoursesByTeacher(Message request) {
        try {
            // ✅ REFACTORED: Use teacherUsername instead of teacherId
            String teacherUsername = request.getData("teacherUsername", String.class);
            if (teacherUsername == null) {
                return Message.createErrorResponse(request.getAction(), "Teacher Username is required");
            }

            // Get teacher to fetch teacherUsername
            UserDAO userDAO = new UserDAO();
            User teacher = userDAO.findByUsername(teacherUsername);
            if (teacher == null) {
                return Message.createErrorResponse(request.getAction(), "Teacher not found");
            }

            CourseDAO courseDAO = new CourseDAO();
            List<com.university.sms.model.Course> courses = courseDAO.findByTeacherUsername(teacher.getUsername());

            LOGGER.info("ClientHandler: Retrieved " + courses.size() + " active courses (ONGOING) for teacher "
                    + teacher.getUsername());

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + courses.size() + " courses");
            response.addData("courses", courses);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting courses by teacher", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error retrieving courses: " + e.getMessage());
        }
    }

    // ==================== Subject Handlers ====================

    private Message handleGetSubjects(Message request) {
        try {
            List<com.university.sms.model.Subject> subjects = subjectService.getAllSubjects();

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + subjects.size() + " subjects");
            response.addData(Constants.KEY_SUBJECTS, subjects);

            LOGGER.info("Retrieved " + subjects.size() + " subjects");
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting subjects", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error retrieving subjects: " + e.getMessage());
        }
    }

    private Message handleGetAllSubjects(Message request) {
        return handleGetSubjects(request);
    }

    private Message handleSearchSubjects(Message request) {
        try {
            String keyword = request.getData("keyword", String.class);
            if (keyword == null || keyword.trim().isEmpty()) {
                return handleGetAllSubjects(request);
            }

            List<com.university.sms.model.Subject> allSubjects = subjectService.getAllSubjects();
            List<com.university.sms.model.Subject> filteredSubjects = new java.util.ArrayList<>();

            String lowerKeyword = keyword.toLowerCase();
            for (com.university.sms.model.Subject subject : allSubjects) {
                if ((subject.getSubjectName() != null && subject.getSubjectName().toLowerCase().contains(lowerKeyword))
                        ||
                        (subject.getSubjectCode() != null
                                && subject.getSubjectCode().toLowerCase().contains(lowerKeyword))) {
                    filteredSubjects.add(subject);
                }
            }

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + filteredSubjects.size() + " subjects");
            response.addData("subjects", filteredSubjects);

            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching subjects", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error searching subjects: " + e.getMessage());
        }
    }

    private Message handleAddSubject(Message request) {
        try {
            // Only admin can add subjects
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền thêm môn học");
            }

            com.university.sms.model.Subject subject = request.getData("subject",
                    com.university.sms.model.Subject.class);
            if (subject == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu thông tin môn học");
            }

            // Validate required fields
            if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu mã môn học");
            }
            if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu tên môn học");
            }
            if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu mã khoa");
            }

            // Validate facultyCode exists
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
            com.university.sms.model.Faculty faculty = facultyDAO.findByCode(subject.getFacultyCode());
            if (faculty == null) {
                return Message.createErrorResponse(request.getAction(),
                        "Mã khoa không tồn tại: " + subject.getFacultyCode());
            }

            // Validate prerequisiteSubjectCode exists (if provided)
            if (subject.getPrerequisiteSubjectCode() != null
                    && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
                com.university.sms.model.Subject prerequisite = subjectService
                        .getSubjectByCode(subject.getPrerequisiteSubjectCode());
                if (prerequisite == null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Môn học tiên quyết không tồn tại: " + subject.getPrerequisiteSubjectCode());
                }
                // Check circular prerequisite (direct and indirect)
                String circularError = checkCircularPrerequisite(subject.getSubjectCode(),
                        subject.getPrerequisiteSubjectCode(), subjectService);
                if (circularError != null) {
                    return Message.createErrorResponse(request.getAction(), circularError);
                }
            }

            // Check duplicate code
            com.university.sms.model.Subject existing = subjectService.getSubjectByCode(subject.getSubjectCode());
            if (existing != null) {
                return Message.createErrorResponse(request.getAction(),
                        "Mã môn học đã tồn tại: " + subject.getSubjectCode());
            }

            boolean success = subjectService.addSubject(subject);
            if (success) {
                if (subject.getSubjectId() > 0) {
                    saveDataOrigin("subject", subject.getSubjectId(), clientSource);
                }
                LOGGER.info("Subject added: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Thêm môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "Không thể thêm môn học. Vui lòng kiểm tra lại thông tin.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding subject", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleUpdateSubject(Message request) {
        try {
            // Only admin can update subjects
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền sửa môn học");
            }

            com.university.sms.model.Subject subject = request.getData("subject",
                    com.university.sms.model.Subject.class);
            if (subject == null || subject.getSubjectId() <= 0) {
                return Message.createErrorResponse(request.getAction(), "Thiếu thông tin môn học");
            }

            // Validate required fields
            if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu mã môn học");
            }
            if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu tên môn học");
            }
            if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Thiếu mã khoa");
            }

            // Validate facultyCode exists
            com.university.sms.dao.FacultyDAO facultyDAO = new com.university.sms.dao.FacultyDAO();
            com.university.sms.model.Faculty faculty = facultyDAO.findByCode(subject.getFacultyCode());
            if (faculty == null) {
                return Message.createErrorResponse(request.getAction(),
                        "Mã khoa không tồn tại: " + subject.getFacultyCode());
            }

            // Validate prerequisiteSubjectCode exists (if provided)
            if (subject.getPrerequisiteSubjectCode() != null
                    && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
                com.university.sms.model.Subject prerequisite = subjectService
                        .getSubjectByCode(subject.getPrerequisiteSubjectCode());
                if (prerequisite == null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Môn học tiên quyết không tồn tại: " + subject.getPrerequisiteSubjectCode());
                }
                // Check circular prerequisite (direct and indirect, including complex loops)
                String circularError = checkCircularPrerequisite(subject.getSubjectCode(),
                        subject.getPrerequisiteSubjectCode(), subjectService);
                if (circularError != null) {
                    return Message.createErrorResponse(request.getAction(), circularError);
                }
            }

            // Check duplicate code
            com.university.sms.model.Subject duplicate = subjectService.getSubjectByCode(subject.getSubjectCode());
            if (duplicate != null && duplicate.getSubjectId() != subject.getSubjectId()) {
                return Message.createErrorResponse(request.getAction(),
                        "Mã môn học đã tồn tại: " + subject.getSubjectCode());
            }

            boolean success = subjectService.updateSubject(subject);
            if (success) {
                // Cập nhật data origin sau khi update thành công
                if (subject.getSubjectId() > 0) {
                    saveDataOrigin("subject", subject.getSubjectId(), clientSource);
                }
                LOGGER.info("Subject updated: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Cập nhật môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "Không thể cập nhật môn học. Vui lòng kiểm tra lại thông tin.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating subject", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra vòng lặp tiên quyết (circular prerequisite)
     * Phát hiện các vòng lặp như: A -> B -> C -> A hoặc A -> B -> A
     * 
     * @param subjectCode      Mã môn học hiện tại
     * @param prerequisiteCode Mã môn học tiên quyết
     * @param subjectService   Service để lấy thông tin môn học
     * @return Thông báo lỗi nếu phát hiện vòng lặp, null nếu không có vòng lặp
     */
    private String checkCircularPrerequisite(String subjectCode, String prerequisiteCode,
            com.university.sms.service.SubjectService subjectService) {
        if (subjectCode == null || prerequisiteCode == null || subjectCode.equals(prerequisiteCode)) {
            return "Môn học không thể là môn học tiên quyết của chính nó";
        }

        // Sử dụng Set để theo dõi các môn học đã thăm (tránh vòng lặp vô hạn)
        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.List<String> path = new java.util.ArrayList<>(); // Để hiển thị đường đi của vòng lặp

        // Bắt đầu từ môn học tiên quyết
        String currentCode = prerequisiteCode;
        visited.add(subjectCode); // Thêm môn học hiện tại vào visited
        path.add(subjectCode);

        // Theo dõi chuỗi tiên quyết
        while (currentCode != null && !currentCode.trim().isEmpty()) {
            // Nếu gặp lại môn học ban đầu → phát hiện vòng lặp
            if (currentCode.equals(subjectCode)) {
                path.add(currentCode);
                return "Phát hiện vòng lặp tiên quyết: " + String.join(" -> ", path);
            }

            // Nếu đã thăm môn học này trước đó → có vòng lặp
            if (visited.contains(currentCode)) {
                path.add(currentCode);
                // Tìm vị trí bắt đầu vòng lặp
                int loopStart = path.indexOf(currentCode);
                java.util.List<String> loopPath = new java.util.ArrayList<>(path.subList(loopStart, path.size()));
                loopPath.add(currentCode); // Thêm lại để đóng vòng lặp
                return "Phát hiện vòng lặp tiên quyết: " + String.join(" -> ", loopPath);
            }

            visited.add(currentCode);
            path.add(currentCode);

            // Lấy môn học tiếp theo trong chuỗi tiên quyết
            com.university.sms.model.Subject currentSubject = subjectService.getSubjectByCode(currentCode);
            if (currentSubject == null) {
                break; // Không tìm thấy môn học, dừng lại
            }

            currentCode = currentSubject.getPrerequisiteSubjectCode();
        }

        // Không phát hiện vòng lặp
        return null;
    }

    private Message handleDeleteSubject(Message request) {
        try {
            // Only admin can delete subjects
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa môn học");
            }

            String subjectCode = request.getData("subjectCode", String.class);
            if (subjectCode == null || subjectCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Subject code không hợp lệ");
            }

            // Kiểm tra xem có courses liên quan không
            CourseDAO courseDAO = new CourseDAO();
            List<Course> subjectCourses = courseDAO.findBySubjectCode(subjectCode);

            if (!subjectCourses.isEmpty()) {
                // Có courses liên quan, không cho phép xóa
                String courseCodes = subjectCourses.stream()
                        .map(Course::getCourseCode)
                        .collect(java.util.stream.Collectors.joining(", "));
                return Message.createErrorResponse(request.getAction(),
                        "Không thể xóa môn học. Môn học này đang có " + subjectCourses.size() +
                                " lớp học phần: " + courseCodes + ". Vui lòng xóa các lớp học phần trước.");
            }

            // Kiểm tra xem có môn học khác dùng subject này làm prerequisite không
            com.university.sms.dao.SubjectDAO subjectDAO = new com.university.sms.dao.SubjectDAO();
            List<com.university.sms.model.Subject> dependentSubjects = subjectDAO.findByPrerequisite(subjectCode);
            if (!dependentSubjects.isEmpty()) {
                String dependentCodes = dependentSubjects.stream()
                        .map(com.university.sms.model.Subject::getSubjectCode)
                        .collect(java.util.stream.Collectors.joining(", "));
                return Message.createErrorResponse(request.getAction(),
                        "Không thể xóa môn học. Có " + dependentSubjects.size() +
                                " môn học khác đang dùng môn này làm tiên quyết: " + dependentCodes +
                                ". Vui lòng cập nhật hoặc xóa các môn học phụ thuộc trước.");
            }

            // Lấy subjectId trước khi xóa để cập nhật timestamp
            com.university.sms.model.Subject subject = subjectDAO.findByCode(subjectCode);
            if (subject != null && subject.getSubjectId() > 0) {
                // Cập nhật timestamp của source gốc trước khi xóa
                String existingSource = getDataOrigin("subject", subject.getSubjectId());
                if (existingSource != null) {
                    updateDataOriginTimestamp("subject", subject.getSubjectId());
                }
            }

            // Không có courses liên quan, cho phép xóa
            boolean success = subjectService.deleteSubject(subjectCode);
            if (success) {
                LOGGER.info("Subject deleted: " + subjectCode + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Xóa môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "Không thể xóa môn học");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting subject", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

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

    // ==================== Enrollment Handlers (Additional) ====================

    private Message handleGetEnrollmentsByCourse(Message request) {
        try {
            // ✅ REFACTORED: Use courseCode instead of courseId
            String courseCode = request.getData("courseCode", String.class);
            if (courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Course code is required");
            }

            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            List<com.university.sms.model.Enrollment> enrollments = enrollmentDAO.findByCourseCode(courseCode);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + enrollments.size() + " enrollments");
            response.addData("enrollments", enrollments);

            LOGGER.info("Retrieved " + enrollments.size() + " enrollments for course " + courseCode);
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting enrollments by course", e);
            return Message.createErrorResponse(request.getAction(),
                    "Error retrieving enrollments: " + e.getMessage());
        }
    }

    // ==================== Grade Handlers ====================

    private Message handleAddGrade(Message request) {
        try {
            Grade grade = request.getData(Constants.KEY_GRADE, Grade.class);
            if (grade == null) {
                return Message.createErrorResponse(request.getAction(), "Grade data is required");
            }

            boolean result = gradeService.addGrade(grade);

            if (result) {
                // Lấy grade đã được lưu để trả về cho client
                // Tìm grade bằng studentCode, courseCode, gradeType, gradeName
                Grade savedGrade = gradeService.getGradesByStudentAndCourse(
                        grade.getStudentCode(), grade.getCourseCode())
                        .stream()
                        .filter(g -> g.getGradeType() == grade.getGradeType() &&
                                g.getGradeName() != null &&
                                g.getGradeName().equals(grade.getGradeName()))
                        .findFirst()
                        .orElse(grade);

                // saveDataOrigin sẽ tự động cập nhật timestamp nếu grade có source CSV
                if (savedGrade != null && savedGrade.getGradeId() > 0) {
                    saveDataOrigin("grade", savedGrade.getGradeId(), clientSource);
                }

                Message response = Message.createSuccessResponse(request.getAction(), "Thêm điểm thành công");
                response.addData(Constants.KEY_GRADE, savedGrade);
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Thêm điểm thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding grade", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleUpdateGrade(Message request) {
        try {
            Grade grade = request.getData(Constants.KEY_GRADE, Grade.class);
            if (grade == null) {
                return Message.createErrorResponse(request.getAction(), "Grade data is required");
            }

            boolean result = gradeService.updateGrade(grade);

            if (result) {
                // Lấy grade đã được cập nhật để trả về cho client
                Grade updatedGrade = gradeService.getGradeById(grade.getGradeId());
                // saveDataOrigin sẽ tự động cập nhật timestamp nếu grade có source CSV
                if (updatedGrade != null && updatedGrade.getGradeId() > 0) {
                    saveDataOrigin("grade", updatedGrade.getGradeId(), clientSource);
                } else if (grade.getGradeId() > 0) {
                    saveDataOrigin("grade", grade.getGradeId(), clientSource);
                }
                Message response = Message.createSuccessResponse(request.getAction(), "Cập nhật điểm thành công");
                if (updatedGrade != null) {
                    response.addData(Constants.KEY_GRADE, updatedGrade);
                } else {
                    response.addData(Constants.KEY_GRADE, grade);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Cập nhật điểm thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating grade", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleDeleteGrade(Message request) {
        try {
            Integer gradeId = request.getData(Constants.KEY_GRADE_ID, Integer.class);
            if (gradeId == null || gradeId <= 0) {
                return Message.createErrorResponse(request.getAction(), "Grade ID is required");
            }

            // Lấy grade trước khi xóa để trả về cho client
            Grade deletedGrade = gradeService.getGradeById(gradeId);
            // Cập nhật timestamp của source gốc trước khi xóa
            if (deletedGrade != null && deletedGrade.getGradeId() > 0) {
                String existingSource = getDataOrigin("grade", deletedGrade.getGradeId());
                if (existingSource != null) {
                    updateDataOriginTimestamp("grade", deletedGrade.getGradeId());
                }
            }
            boolean result = gradeService.deleteGrade(gradeId);

            if (result) {
                Message response = Message.createSuccessResponse(request.getAction(), "Xóa điểm thành công");
                if (deletedGrade != null) {
                    response.addData(Constants.KEY_GRADE, deletedGrade);
                }
                return response;
            } else {
                return Message.createErrorResponse(request.getAction(), "Xóa điểm thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting grade", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetGrades(Message request) {
        try {
            // ✅ REFACTORED: Chỉ dùng codes
            String studentCode = request.getData(Constants.KEY_STUDENT_CODE, String.class);
            String courseCode = request.getData(Constants.KEY_COURSE_CODE, String.class);
            Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);

            List<Grade> grades;

            if (studentCode != null && courseCode != null) {
                grades = gradeService.getGradesByStudentAndCourse(studentCode, courseCode);
            } else if (enrollmentId != null) {
                // Tìm enrollment để lấy codes
                EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
                Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
                if (enrollment != null) {
                    grades = gradeService.getGradesByStudentAndCourse(
                            enrollment.getStudentCode(), enrollment.getCourseCode());
                } else {
                    return Message.createErrorResponse(request.getAction(), "Enrollment không tồn tại");
                }
            } else if (studentCode != null) {
                grades = gradeService.getGradesByStudent(studentCode);
            } else if (courseCode != null) {
                grades = gradeService.getGradesByCourse(courseCode);
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "student_code/course_code hoặc enrollment_id is required");
            }

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Lấy danh sách điểm thành công");
            response.addData(Constants.KEY_GRADES, grades);
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting grades", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleCalculateFinalGrade(Message request) {
        try {
            // ✅ REFACTORED: Dùng studentCode và courseCode, hoặc enrollmentId
            String studentCode = request.getData(Constants.KEY_STUDENT_CODE, String.class);
            String courseCode = request.getData(Constants.KEY_COURSE_CODE, String.class);
            Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);

            if (studentCode == null || courseCode == null) {
                if (enrollmentId == null) {
                    return Message.createErrorResponse(request.getAction(),
                            "Student code and course code (or enrollment ID) are required");
                }

                // Tìm enrollment để lấy studentCode và courseCode
                EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
                Enrollment enrollment = enrollmentDAO.findById(enrollmentId);
                if (enrollment == null) {
                    return Message.createErrorResponse(request.getAction(), "Enrollment không tồn tại");
                }

                studentCode = enrollment.getStudentCode();
                courseCode = enrollment.getCourseCode();
            }

            boolean result = gradeService.finalizeCourseGrade(studentCode, courseCode);

            if (result) {
                return Message.createSuccessResponse(request.getAction(), "Tính điểm tổng kết thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Tính điểm tổng kết thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating final grade", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // ==================== Notification Handlers ====================

    private Message handleGetNotifications(Message request) {
        try {
            Integer userId = request.getData(Constants.KEY_USER_ID, Integer.class);

            List<Notification> notifications;
            String username = null;

            if (userId != null) {
                UserDAO userDAO = new UserDAO();
                User user = userDAO.findById(userId);
                if (user != null) {
                    username = user.getUsername();
                }
            } else if (currentUser != null) {
                username = currentUser.getUsername();
            }

            if (username == null || username.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "User information is required");
            }

            notifications = notificationService.getNotificationsByUser(username);

            // Count unread
            int unreadCount = 0;
            for (Notification n : notifications) {
                if (!n.isRead()) {
                    unreadCount++;
                }
            }

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Lấy danh sách thông báo thành công");
            response.addData(Constants.KEY_NOTIFICATIONS, notifications);
            response.addData(Constants.KEY_UNREAD_COUNT, unreadCount);
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting notifications", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleSendNotification(Message request) {
        try {
            Notification notification = request.getData(Constants.KEY_NOTIFICATION, Notification.class);
            if (notification == null) {
                return Message.createErrorResponse(request.getAction(), "Notification data is required");
            }

            if (notification.getSenderUsername() == null && currentUser != null) {
                notification.setSenderUsername(currentUser.getUsername());
            }

            boolean result = notificationService.createNotification(notification);

            if (result) {
                return Message.createSuccessResponse(request.getAction(), "Gửi thông báo thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Gửi thông báo thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending notification", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleMarkNotificationRead(Message request) {
        try {
            Integer notificationId = request.getData(Constants.KEY_NOTIFICATION_ID, Integer.class);

            if (notificationId != null) {
                // Mark single notification as read
                boolean result = notificationService.markAsRead(notificationId);
                if (result) {
                    return Message.createSuccessResponse(request.getAction(), "Đánh dấu đã đọc thành công");
                } else {
                    return Message.createErrorResponse(request.getAction(), "Đánh dấu đã đọc thất bại");
                }
            } else if (currentUser != null) {
                boolean result = notificationService.markAllAsReadForUser(currentUser.getUsername());
                if (result) {
                    return Message.createSuccessResponse(request.getAction(),
                            "Đánh dấu tất cả thông báo đã đọc thành công");
                } else {
                    return Message.createErrorResponse(request.getAction(),
                            "Đánh dấu thông báo đã đọc thất bại");
                }
            } else {
                return Message.createErrorResponse(request.getAction(), "Notification ID is required");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking notification as read", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    // ==================== TIMETABLE & TRANSCRIPT HANDLERS ====================

    private Message handleGetTimetable(Message request) {
        try {
            Integer userId = request.getData(Constants.KEY_USER_ID, Integer.class);
            String userRole = request.getData(Constants.KEY_USER_ROLE, String.class);

            if (userId == null) {
                userId = currentUser != null ? currentUser.getUserId() : null;
                userRole = currentUser != null ? currentUser.getRole().toString() : null;
            }

            if (userId == null || userRole == null) {
                return Message.createErrorResponse(request.getAction(), "User information is required");
            }

            List<?> timetable = null;

            if ("STUDENT".equalsIgnoreCase(userRole)) {
                UserDAO userDAO = new UserDAO();
                User user = userDAO.findById(userId);
                if (user != null) {
                    StudentDAO studentDAO = new StudentDAO();
                    Student student = studentDAO.findByUsername(user.getUsername());
                    if (student != null) {
                        timetable = timetableService.getStudentTimetable(student.getStudentCode());
                    }
                }
            } else if ("TEACHER".equalsIgnoreCase(userRole)) {
                UserDAO userDAO = new UserDAO();
                User user = userDAO.findById(userId);
                if (user != null) {
                    timetable = timetableService.getTeacherTimetable(user.getUsername());
                }
            }

            Message response = Message.createSuccessResponse(request.getAction(), "Timetable retrieved successfully");
            response.addData(Constants.KEY_TIMETABLE, timetable);
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting timetable", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetTranscript(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            if (studentCode == null || studentCode.isEmpty()) {
                // Fallback: nếu là student thì lấy từ currentUser
                if (currentUser != null
                        && "STUDENT".equalsIgnoreCase(currentUser.getRole().toString())) {
                    StudentDAO studentDAO = new StudentDAO();
                    Student student = studentDAO.findByUsername(currentUser.getUsername());
                    if (student != null) {
                        studentCode = student.getStudentCode();
                    }
                }
            }

            if (studentCode == null || studentCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Student code is required");
            }

            var transcript = transcriptService.generateTranscript(studentCode);

            if (transcript == null) {
                return Message.createErrorResponse(request.getAction(), "Cannot generate transcript");
            }

            Message response = Message.createSuccessResponse(request.getAction(), "Transcript retrieved successfully");
            response.addData(Constants.KEY_TRANSCRIPT, transcript);
            response.addData(Constants.KEY_CUMULATIVE_GPA, transcript.getCumulativeGPA());
            response.addData(Constants.KEY_ACADEMIC_RANK, transcript.getAcademicRank());
            response.addData(Constants.KEY_TOTAL_CREDITS, transcript.getTotalCreditsEarned());
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting transcript", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetSemesterTranscript(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String academicYear = request.getData(Constants.KEY_ACADEMIC_YEAR, String.class);
            Integer semester = request.getData(Constants.KEY_SEMESTER, Integer.class);

            if (studentCode == null || studentCode.isEmpty() || academicYear == null || semester == null) {
                return Message.createErrorResponse(request.getAction(),
                        "Student code, academic year and semester are required");
            }

            var semesterRecord = transcriptService.getSemesterTranscript(studentCode, academicYear, semester);

            if (semesterRecord == null) {
                return Message.createErrorResponse(request.getAction(), "Semester transcript not found");
            }

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Semester transcript retrieved successfully");
            response.addData(Constants.KEY_SEMESTER_RECORDS, semesterRecord);
            response.addData(Constants.KEY_SEMESTER_GPA, semesterRecord.getSemesterGPA());
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting semester transcript", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetHonorStudents(Message request) {
        try {
            String facultyCode = request.getData(Constants.KEY_FACULTY_CODE, String.class);

            if (facultyCode == null) {
                return Message.createErrorResponse(request.getAction(), "Faculty ID is required");
            }

            List<?> honorStudents = transcriptService.getHonorStudents(facultyCode);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Honor students retrieved successfully");
            response.addData(Constants.KEY_HONOR_STUDENTS, honorStudents);
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting honor students", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetFacultyStatistics(Message request) {
        try {
            String facultyCode = request.getData("facultyCode", String.class);
            if (facultyCode == null || facultyCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Faculty code is required");
            }

            Map<String, Object> statistics = transcriptService.getFacultyStatistics(facultyCode);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Faculty statistics retrieved successfully");
            response.addData(Constants.KEY_STATISTICS, statistics);
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting faculty statistics", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleValidateSchedule(Message request) {
        try {
            String studentCode = request.getData("studentCode", String.class);
            String courseCode = request.getData("courseCode", String.class);

            if (studentCode == null || studentCode.isEmpty() || courseCode == null || courseCode.isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Student code and course code are required");
            }

            boolean isValid = timetableService.validateSchedule(studentCode, courseCode);

            if (isValid) {
                return Message.createSuccessResponse(request.getAction(), "No schedule conflict");
            } else {
                return Message.createErrorResponse(request.getAction(), "Schedule conflict detected");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating schedule", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Handle get server statistics (admin only)
     */
    private Message handleGetServerStatistics(Message request) {
        try {
            // Only admin can access server statistics
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), Constants.MSG_UNAUTHORIZED);
            }

            StudentManagementServer server = StudentManagementServer.getInstance();
            if (server == null) {
                return Message.createErrorResponse(request.getAction(), "Server instance not available");
            }

            StudentManagementServer.ServerStatistics stats = server.getStatistics();

            // Get server database version
            int serverDbVersion = getServerVersion();

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Server statistics retrieved successfully");
            response.addData("totalClients", stats.getConnectedClients());
            response.addData("adminClients", stats.getAdminClients());
            response.addData("teacherClients", stats.getTeacherClients());
            response.addData("studentClients", stats.getStudentClients());
            response.addData("serverDbVersion", serverDbVersion);

            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting server statistics", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Validate email format
     * 
     * @param email Email to validate
     * @return true if email format is valid, false otherwise
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // RFC 5322 compliant email regex (simplified but robust)
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.trim().matches(emailRegex);
    }

    /**
     * Validate phone number format (Vietnam phone numbers)
     * Supports:
     * - 10 digits starting with 0: 0123456789, 0912345678
     * - 11 digits starting with +84: +84123456789, +84912345678
     * - 10 digits without leading 0: 1234567890 (less common)
     * 
     * @param phone Phone number to validate
     * @return true if phone format is valid, false otherwise
     */
    private boolean isValidPhoneFormat(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String phoneStr = phone.trim();

        // Remove all non-digit characters except + for validation
        // This handles spaces, dashes, dots, parentheses, etc.
        String normalized = phoneStr.replaceAll("[^0-9+]", "");

        // If normalized is empty after removing non-digits, it's invalid
        if (normalized.isEmpty()) {
            return false;
        }

        // Pattern 1: 10 digits starting with 0 (e.g., 0123456789, 0912345678)
        if (normalized.matches("^0[0-9]{9}$")) {
            return true;
        }

        // Pattern 2: 11 digits starting with +84 (e.g., +84123456789, +84912345678)
        if (normalized.matches("^\\+84[0-9]{9}$")) {
            return true;
        }

        // Pattern 3: 10 digits without leading 0 (less common, but acceptable)
        // Only accept if it doesn't start with 0 (to avoid conflict with Pattern 1)
        if (normalized.matches("^[1-9][0-9]{9}$")) {
            return true;
        }

        return false;
    }

    /**
     * Normalize phone number by removing spaces, dashes, dots, etc.
     * Keeps the + sign if present for international format.
     * 
     * @param phone Phone number to normalize
     * @return Normalized phone number
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        // Remove all non-digit characters except +
        String normalized = phone.trim().replaceAll("[^0-9+]", "");
        return normalized;
    }

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isConnected && !clientSocket.isClosed();
    }
}
