package com.university.sms.server;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.model.Student;
import com.university.sms.model.Course;
import com.university.sms.model.Enrollment;
import com.university.sms.service.AuthenticationService;
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
            // Initialize streams
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
                case Constants.ACTION_GET_GRADES:
                    return handleGetGrades(request);
                case Constants.ACTION_CALCULATE_FINAL_GRADE:
                    return handleCalculateFinalGrade(request);

                // Sync actions
                case Constants.ACTION_SYNC_CHECK:
                    return handleSyncCheck(request);
                case Constants.ACTION_UPLOAD_STUDENTS:
                    return handleUploadStudents(request);
                case Constants.ACTION_UPLOAD_COURSES:
                    return handleUploadCourses(request);
                case Constants.ACTION_UPLOAD_ENROLLMENTS:
                    return handleUploadEnrollments(request);
                case Constants.ACTION_UPLOAD_USERS:
                    return handleUploadUsers(request);

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

        User user = authService.authenticate(username, password);
        if (user != null) {
            this.currentUser = user;

            // Log login
            String clientIP = clientSocket.getRemoteSocketAddress().toString();
            authService.logLogin(user.getUserId(), clientIP, "Java Client", "success");

            Message response = Message.createSuccessResponse(Constants.ACTION_LOGIN, Constants.MSG_LOGIN_SUCCESS);
            response.addData(Constants.KEY_USER, user);

            LOGGER.info("User logged in successfully: " + username);
            return response;
        } else {
            // Log failed login attempt
            authService.logFailedLogin(username, clientSocket.getRemoteSocketAddress().toString());

            return Message.createErrorResponse(Constants.ACTION_LOGIN, Constants.MSG_INVALID_CREDENTIALS);
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

        boolean success = authService.changePassword(currentUser.getUserId(), newPassword);
        if (success) {
            return Message.createSuccessResponse(Constants.ACTION_CHANGE_PASSWORD, "Đổi mật khẩu thành công");
        } else {
            return Message.createErrorResponse(Constants.ACTION_CHANGE_PASSWORD, "Đổi mật khẩu thất bại");
        }
    }

    /**
     * Xử lý lấy thông tin sinh viên
     */
    private Message handleGetStudentInfo(Message request) {
        Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);

        if (currentUser.getRole() == User.UserRole.STUDENT) {
            // Sinh viên chỉ có thể xem thông tin của mình
            var student = studentService.getStudentByUserId(currentUser.getUserId());
            if (student != null) {
                Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
                        "Lấy thông tin thành công");
                response.addData(Constants.KEY_STUDENT, student);
                return response;
            }
        } else if (studentId != null && (currentUser.getRole() == User.UserRole.ADMIN ||
                currentUser.getRole() == User.UserRole.TEACHER)) {
            // Admin và giáo viên có thể xem thông tin sinh viên theo ID
            var student = studentService.getStudentById(studentId);
            if (student != null) {
                Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO,
                        "Lấy thông tin thành công");
                response.addData(Constants.KEY_STUDENT, student);
                return response;
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
            LOGGER.severe("Error getting all students: " + e.getMessage());
            e.printStackTrace();
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
            LOGGER.severe("Error getting all students (include inactive): " + e.getMessage());
            e.printStackTrace();
            return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
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

    /**
     * Xử lý thêm sinh viên mới
     */
    private Message handleAddStudent(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_UNAUTHORIZED);
        }

        com.university.sms.model.Student student = request.getData(Constants.KEY_STUDENT,
                com.university.sms.model.Student.class);
        if (student == null) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_INVALID_DATA);
        }

        // Ensure related user exists (create if missing)
        try {
            com.university.sms.dao.UserDAO userDAO = new com.university.sms.dao.UserDAO();
            boolean userOk = true;
            if (student.getUserId() <= 0 || userDAO.findById(student.getUserId()) == null) {
                com.university.sms.model.User byUsername = userDAO.findByUsername(student.getStudentCode());
                if (byUsername != null) {
                    student.setUserId(byUsername.getUserId());
                } else {
                    com.university.sms.model.User u = new com.university.sms.model.User();
                    u.setUsername(student.getStudentCode());
                    u.setPassword("password");
                    u.setFullName(student.getFullName());
                    u.setEmail(student.getEmail());
                    u.setPhone(student.getPhone());
                    u.setAddress(student.getAddress());
                    u.setRole(com.university.sms.model.User.UserRole.STUDENT);
                    userOk = userDAO.addUser(u);
                    if (userOk) {
                        student.setUserId(u.getUserId());
                        saveDataOrigin("user", u.getUserId(), clientSource);
                    }
                }
            }
            if (!userOk) {
                return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Cannot create related user");
            }
        } catch (Exception e) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Error preparing user: " + e.getMessage());
        }

        boolean ok = studentService.addStudent(student);
        if (ok) {
            saveDataOrigin("student", student.getStudentId(), clientSource);
            return Message.createSuccessResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_DATABASE_ERROR);
    }

    /**
     * Xử lý cập nhật thông tin sinh viên
     */
    private Message handleUpdateStudent(Message request) {
        String subAction = request.getData("action", String.class);
        if ("delete".equalsIgnoreCase(subAction)) {
            return handleDeleteStudent(request);
        }

        // Cho phép ADMIN và TEACHER cập nhật
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.TEACHER) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_UNAUTHORIZED);
        }

        com.university.sms.model.Student student = request.getData(Constants.KEY_STUDENT,
                com.university.sms.model.Student.class);
        if (student == null || student.getStudentId() <= 0) {
            return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_INVALID_DATA);
        }

        boolean ok = studentService.updateStudent(student);
        if (ok) {
            saveDataOrigin("student", student.getStudentId(), clientSource);
            return Message.createSuccessResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, Constants.MSG_DATABASE_ERROR);
    }

    private Message handleDeleteStudent(Message request) {
        try {
            // Only admin can delete students
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa sinh viên");
            }

            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            if (studentId == null || studentId <= 0) {
                return Message.createErrorResponse(request.getAction(), "ID sinh viên không hợp lệ");
            }

            // Get student info first
            StudentDAO studentDAO = new StudentDAO();
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                return Message.createErrorResponse(request.getAction(), "Không tìm thấy sinh viên");
            }

            // Step 1: Remove student from all enrollments and update current_students
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            List<Enrollment> enrollments = enrollmentDAO.findByStudentId(studentId);

            CourseDAO courseDAO = new CourseDAO();
            for (Enrollment enrollment : enrollments) {
                // Delete enrollment
                enrollmentDAO.deleteEnrollment(enrollment.getEnrollmentId());

                // Decrease current_students count
                Course course = courseDAO.findById(enrollment.getCourseId());
                if (course != null && course.getCurrentStudents() > 0) {
                    courseDAO.updateCurrentStudents(enrollment.getCourseId(), course.getCurrentStudents() - 1);
                }
            }

            // Step 2: Remove student from all course registrations and update
            // current_students
            CourseRegistrationDAO registrationDAO = new CourseRegistrationDAO();
            List<CourseRegistration> registrations = registrationDAO.findByStudent(studentId);

            for (CourseRegistration registration : registrations) {
                // Only count APPROVED registrations
                if (registration.getRegistrationStatus() == CourseRegistration.RegistrationStatus.APPROVED) {
                    Course course = courseDAO.findById(registration.getCourseId());
                    if (course != null && course.getCurrentStudents() > 0) {
                        courseDAO.updateCurrentStudents(registration.getCourseId(), course.getCurrentStudents() - 1);
                    }
                }
                // Delete registration
                registrationDAO.delete(registration.getRegistrationId());
            }

            // Step 3: Deactivate user
            UserDAO userDAO = new UserDAO();
            boolean success = userDAO.deactivateUser(student.getUserId());

            if (success) {
                LOGGER.info("Student deactivated and removed from all courses: " + studentId + " ("
                        + student.getStudentCode() + ") by "
                        + currentUser.getUsername() + " - Removed " + enrollments.size() + " enrollments and "
                        + registrations.size() + " registrations");
                return Message.createSuccessResponse(request.getAction(),
                        "Vô hiệu hóa sinh viên và xóa khỏi tất cả lớp học phần thành công");
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
            LOGGER.severe("Error getting all courses: " + e.getMessage());
            e.printStackTrace();
            return Message.createErrorResponse(request.getAction(), "Lỗi server: " + e.getMessage());
        }
    }

    // Removed handleGetCourses - using handleGetAllCourses for both actions

    /**
     * Xử lý lấy thông tin khóa học
     */
    private Message handleGetCourseInfo(Message request) {
        Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
        if (courseId == null) {
            return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_INVALID_DATA);
        }

        var course = courseService.getCourseById(courseId);
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
     * Xóa khóa học
     */
    private Message handleDeleteCourse(Message request) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_UNAUTHORIZED);
        }
        Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
        if (courseId == null || courseId <= 0) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_INVALID_DATA);
        }
        boolean ok = courseService.deleteCourse(courseId);
        if (ok) {
            return Message.createSuccessResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_DELETE_COURSE, Constants.MSG_DATABASE_ERROR);
    }

    /**
     * Xử lý lấy danh sách đăng ký
     */
    private Message handleGetEnrollments(Message request) {
        try {
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();

            // Student: chỉ xem của chính mình
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                var me = studentService.getStudentByUserId(currentUser.getUserId());
                if (me == null) {
                    return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS,
                            Constants.MSG_STUDENT_NOT_FOUND);
                }
                var list = enrollmentDAO.findByStudentId(me.getStudentId());
                Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
                resp.addData(Constants.KEY_ENROLLMENTS, list);
                return resp;
            }

            // Admin/Teacher: có thể truyền studentId để lấy danh sách theo SV
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            if (studentId == null || studentId <= 0) {
                return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_INVALID_DATA);
            }
            var list = enrollmentDAO.findByStudentId(studentId);
            Message resp = Message.createSuccessResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SUCCESS);
            resp.addData(Constants.KEY_ENROLLMENTS, list);
            return resp;
        } catch (Exception e) {
            LOGGER.severe("Error getting enrollments: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Xử lý lấy điểm sinh viên
     */
    private Message handleGetStudentGrades(Message request) {
        try {
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();

            int targetStudentId;
            if (currentUser.getRole() == User.UserRole.STUDENT) {
                var me = studentService.getStudentByUserId(currentUser.getUserId());
                if (me == null) {
                    return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES,
                            Constants.MSG_STUDENT_NOT_FOUND);
                }
                targetStudentId = me.getStudentId();
            } else {
                Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
                if (studentId == null || studentId <= 0) {
                    return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES, Constants.MSG_INVALID_DATA);
                }
                targetStudentId = studentId;
            }

            var all = enrollmentDAO.findByStudentId(targetStudentId);
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
            LOGGER.severe("Error getting student grades: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Đăng ký khóa học
     */
    private Message handleEnrollCourse(Message request) {
        try {
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
            if (studentId == null || courseId == null) {
                return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_INVALID_DATA);
            }

            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(studentId, courseId);
            boolean ok;
            if (existing == null) {
                com.university.sms.model.Enrollment e = new com.university.sms.model.Enrollment(studentId, courseId);
                ok = enrollmentDAO.addEnrollment(e);
                if (ok) {
                    courseService.incrementCurrentStudents(courseId);
                    saveDataOrigin("enrollment", e.getEnrollmentId(), clientSource);
                }
            } else {
                ok = true; // already enrolled
            }
            if (ok) {
                return Message.createSuccessResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SUCCESS);
            }
            return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Error enrolling course: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_ENROLL_COURSE, Constants.MSG_SERVER_ERROR);
        }
    }

    /**
     * Hủy đăng ký khóa học
     */
    private Message handleDropCourse(Message request) {
        try {
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
            if (studentId == null || courseId == null) {
                return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
            }

            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            com.university.sms.model.Enrollment existing = enrollmentDAO.findByStudentAndCourse(studentId, courseId);
            if (existing == null) {
                return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_INVALID_DATA);
            }

            boolean ok = enrollmentDAO.deleteEnrollment(existing.getEnrollmentId());
            if (ok) {
                courseService.decrementCurrentStudents(courseId);
                return Message.createSuccessResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_SUCCESS);
            }
            return Message.createErrorResponse(Constants.ACTION_DROP_COURSE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Error dropping course: " + e.getMessage());
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
            LOGGER.severe("Error updating final grade: " + e.getMessage());
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
                Constants.ACTION_SYNC_CHECK.equals(action);
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
            int clientVersion = ((Number) clientMetadata.get("db_version")).intValue();
            int clientTotalRecords = ((Number) clientMetadata.get("total_records")).intValue();

            // Lấy metadata server
            Map<String, Object> serverMetadata = getServerMetadata();
            int serverVersion = ((Number) serverMetadata.get("db_version")).intValue();
            int serverTotalRecords = ((Number) serverMetadata.get("total_records")).intValue();

            LOGGER.info("Sync check - Client: " + (clientDbType != null ? clientDbType : "UNKNOWN") +
                    " v" + clientVersion + " (" + clientTotalRecords +
                    "), Server: v" + serverVersion + " (" + serverTotalRecords + ")");

            // Quyết định sync action
            // One-way upload model: always ask client to upload
            String syncAction = "UPLOAD_TO_SERVER";

            Message response = Message.createSuccessResponse(Constants.ACTION_SYNC_CHECK,
                    "Sync check completed");
            response.addData("sync_action", syncAction);
            response.addData("server_version", serverVersion);
            response.addData("server_metadata", serverMetadata);

            return response;

        } catch (Exception e) {
            LOGGER.severe("Error handling sync check: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_SYNC_CHECK, "Error: " + e.getMessage());
        }
    }

    /**
     * Lấy metadata của server
     */
    private Map<String, Object> getServerMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Đếm records
            int studentCount = studentService.getTotalCount();
            int courseCount = courseService.getTotalCount();

            // Lấy version từ database
            int dbVersion = getServerVersion();

            metadata.put("db_version", dbVersion);
            metadata.put("student_count", studentCount);
            metadata.put("course_count", courseCount);
            metadata.put("total_records", studentCount + courseCount);

        } catch (Exception e) {
            LOGGER.warning("Error getting server metadata: " + e.getMessage());
            metadata.put("db_version", 1);
            metadata.put("student_count", 0);
            metadata.put("course_count", 0);
            metadata.put("total_records", 0);
        }

        return metadata;
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
            for (com.university.sms.model.Student student : students) {
                try {
                    // Ensure user exists and map userId
                    int userId = student.getUserId();
                    boolean userOk = true;
                    if (userId <= 0 || userDAO.findById(userId) == null) {
                        // Try to find by username = studentCode
                        com.university.sms.model.User byUsername = userDAO.findByUsername(student.getStudentCode());
                        if (byUsername != null) {
                            student.setUserId(byUsername.getUserId());
                        } else {
                            // Create a minimal user from student info
                            com.university.sms.model.User u = new com.university.sms.model.User();
                            u.setUsername(student.getStudentCode());
                            u.setPassword("password");
                            u.setFullName(student.getFullName());
                            u.setEmail(student.getEmail());
                            u.setPhone(student.getPhone());
                            u.setAddress(student.getAddress());
                            u.setRole(com.university.sms.model.User.UserRole.STUDENT);
                            userOk = userDAO.addUser(u);
                            if (userOk) {
                                student.setUserId(u.getUserId());
                                saveDataOrigin("user", u.getUserId(), clientSource);
                            }
                        }
                    }

                    if (userOk && studentDAO.addOrUpdate(student)) {
                        saveDataOrigin("student", student.getStudentId(), clientSource);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception ex) {
                    failCount++;
                }
            }

            String message = String.format("Uploaded %d students successfully, %d failed",
                    successCount, failCount);

            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_STUDENTS, message);

        } catch (Exception e) {
            LOGGER.severe("Error handling upload students: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_STUDENTS, "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload courses từ client
     */
    private Message handleUploadCourses(Message request) {
        try {
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
            for (com.university.sms.model.Course course : courses) {
                if (courseDAO.addOrUpdate(course)) {
                    saveDataOrigin("course", course.getCourseId(), clientSource);
                    successCount++;
                } else {
                    failCount++;
                }
            }

            String message = String.format("Uploaded %d courses successfully, %d failed",
                    successCount, failCount);

            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_COURSES, message);

        } catch (Exception e) {
            LOGGER.severe("Error handling upload courses: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_COURSES, "Error: " + e.getMessage());
        }
    }

    /**
     * Xử lý upload enrollments từ client
     */
    private Message handleUploadEnrollments(Message request) {
        try {
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
                    // Prefer upsert by (student_id, course_id): insert if not exists
                    com.university.sms.model.Enrollment existing = enrollmentDAO
                            .findByStudentAndCourse(e.getStudentId(), e.getCourseId());
                    boolean ok;
                    if (existing == null) {
                        ok = enrollmentDAO.addEnrollment(e);
                    } else {
                        // If exists, update simple mutable fields if provided
                        if (e.getEnrollmentStatus() != null) {
                            ok = enrollmentDAO.updateEnrollmentStatus(existing.getEnrollmentId(),
                                    e.getEnrollmentStatus());
                        } else {
                            ok = true; // nothing to update
                        }
                        e.setEnrollmentId(existing.getEnrollmentId());
                    }
                    if (ok) {
                        saveDataOrigin("enrollment", e.getEnrollmentId(), clientSource);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception ex) {
                    failCount++;
                }
            }

            String message = String.format("Uploaded %d enrollments successfully, %d failed",
                    successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_ENROLLMENTS, message);

        } catch (Exception e) {
            LOGGER.severe("Error handling upload enrollments: " + e.getMessage());
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
                    boolean ok = userDAO.addOrUpdatePreserveId(u);
                    if (ok) {
                        saveDataOrigin("user", u.getUserId(), clientSource);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception ex) {
                    failCount++;
                }
            }

            String message = String.format("Uploaded %d users successfully, %d failed",
                    successCount, failCount);
            return Message.createSuccessResponse(Constants.ACTION_UPLOAD_USERS, message);
        } catch (Exception e) {
            LOGGER.severe("Error handling upload users: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPLOAD_USERS, "Error: " + e.getMessage());
        }
    }

    /**
     * Lưu thông tin nguồn dữ liệu vào bảng data_origin
     */
    private void saveDataOrigin(String entityType, int entityId, String source) {
        if (entityId <= 0)
            return;
        String sql = "INSERT INTO data_origin (entity_type, entity_id, source) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE source = VALUES(source)";
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
            LOGGER.severe("Error getting all class requests: " + e.getMessage());
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
            LOGGER.severe("Error getting class request by ID: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetMyClassRequests(Message request) {
        try {
            int teacherId = (Integer) request.getData(Constants.KEY_TEACHER_ID);
            List<ClassOpeningRequest> requests = classRequestService.getRequestsByTeacher(teacherId);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_CLASS_REQUESTS, requests);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error getting teacher's class requests: " + e.getMessage());
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
            LOGGER.severe("Error getting pending class requests: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleSubmitClassRequest(Message request) {
        try {
            ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
            boolean success = classRequestService.submitRequest(classRequest);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Request submitted successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to submit request");
            }
        } catch (Exception e) {
            LOGGER.severe("Error submitting class request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleUpdateClassRequest(Message request) {
        try {
            ClassOpeningRequest classRequest = (ClassOpeningRequest) request.getData(Constants.KEY_CLASS_REQUEST);
            boolean success = classRequestService.updateRequest(classRequest);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Request updated successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to update request");
            }
        } catch (Exception e) {
            LOGGER.severe("Error updating class request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleCancelClassRequest(Message request) {
        try {
            int requestId = (Integer) request.getData(Constants.KEY_REQUEST_ID);
            int teacherId = (Integer) request.getData(Constants.KEY_TEACHER_ID);
            boolean success = classRequestService.cancelRequest(requestId, teacherId);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Request cancelled successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to cancel request");
            }
        } catch (Exception e) {
            LOGGER.severe("Error cancelling class request: " + e.getMessage());
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

            // Lấy adminId từ currentUser thay vì từ request
            int adminId = currentUser.getUserId();
            String note = request.getData(Constants.KEY_NOTE, String.class);

            boolean success = classRequestService.approveRequest(requestId, adminId, note);

            if (success) {
                LOGGER.info("Admin " + currentUser.getUsername() + " approved request " + requestId);
                return Message.createSuccessResponse(request.getAction(), "Đã duyệt yêu cầu thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể duyệt yêu cầu");
            }
        } catch (IllegalStateException e) {
            LOGGER.warning("Cannot approve request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Error approving class request: " + e.getMessage());
            e.printStackTrace();
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

            // Lấy adminId từ currentUser thay vì từ request
            int adminId = currentUser.getUserId();
            String reason = request.getData(Constants.KEY_REASON, String.class);

            if (reason == null || reason.trim().isEmpty()) {
                return Message.createErrorResponse(request.getAction(), "Vui lòng nhập lý do từ chối");
            }

            boolean success = classRequestService.rejectRequest(requestId, adminId, reason);

            if (success) {
                LOGGER.info("Admin " + currentUser.getUsername() + " rejected request " + requestId);
                return Message.createSuccessResponse(request.getAction(), "Đã từ chối yêu cầu");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể từ chối yêu cầu");
            }
        } catch (IllegalStateException e) {
            LOGGER.warning("Cannot reject request: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Error rejecting class request: " + e.getMessage());
            e.printStackTrace();
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
            LOGGER.severe("Error getting class request stats: " + e.getMessage());
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
            LOGGER.severe("Error getting all registrations: " + e.getMessage());
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
            LOGGER.severe("Error getting registration by ID: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetMyRegistrations(Message request) {
        try {
            Integer studentIdOrUserId = (Integer) request.getData(Constants.KEY_STUDENT_ID);

            // Try to get student by ID first, if not found try by user_id
            Student student = studentService.getStudentById(studentIdOrUserId);
            if (student == null) {
                student = studentService.getStudentByUserId(studentIdOrUserId);
            }

            int studentId = (student != null) ? student.getStudentId() : studentIdOrUserId;

            List<CourseRegistration> registrations = registrationService.getRegistrationsByStudent(studentId);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error getting student's registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleGetCourseRegistrations(Message request) {
        try {
            int courseId = (Integer) request.getData(Constants.KEY_COURSE_ID);
            List<CourseRegistration> registrations = registrationService.getRegistrationsByCourse(courseId);
            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData(Constants.KEY_REGISTRATIONS, registrations);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error getting course registrations: " + e.getMessage());
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
            LOGGER.severe("Error getting pending registrations: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleRegisterCourse(Message request) {
        try {
            Integer studentIdOrUserId = (Integer) request.getData(Constants.KEY_STUDENT_ID);
            int courseId = (Integer) request.getData(Constants.KEY_COURSE_ID);
            String notes = (String) request.getData(Constants.KEY_NOTE);

            // Try to get student by ID first, if not found try by user_id
            Student student = studentService.getStudentById(studentIdOrUserId);
            if (student == null) {
                LOGGER.info("Student not found by ID " + studentIdOrUserId + ", trying user_id lookup");
                student = studentService.getStudentByUserId(studentIdOrUserId);
            }

            if (student == null) {
                LOGGER.severe("Student not found for ID/UserID: " + studentIdOrUserId);
                return Message.createErrorResponse(request.getAction(),
                        "Student not found. Please ensure your profile is complete.");
            }

            int studentId = student.getStudentId();
            LOGGER.info("Registering course for student_id=" + studentId + " (from input=" + studentIdOrUserId + ")");

            boolean success = registrationService.registerCourse(studentId, courseId, notes);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Registration submitted successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to submit registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Error registering course: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleCancelRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            int studentId = (Integer) request.getData(Constants.KEY_STUDENT_ID);

            boolean success = registrationService.cancelRegistration(registrationId, studentId);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Registration cancelled successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to cancel registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Error cancelling registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleApproveRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            boolean success = registrationService.approveRegistration(registrationId);

            if (success) {
                return Message.createSuccessResponse(request.getAction(), "Registration approved successfully");
            } else {
                return Message.createErrorResponse(request.getAction(), "Failed to approve registration");
            }
        } catch (Exception e) {
            LOGGER.severe("Error approving registration: " + e.getMessage());
            return Message.createErrorResponse(request.getAction(), "Error: " + e.getMessage());
        }
    }

    private Message handleRejectRegistration(Message request) {
        try {
            int registrationId = (Integer) request.getData(Constants.KEY_REGISTRATION_ID);
            String reason = (String) request.getData(Constants.KEY_REASON);

            boolean success = registrationService.rejectRegistration(registrationId, reason);

            if (success) {
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
            Integer studentIdOrUserId = (Integer) request.getData(Constants.KEY_STUDENT_ID);
            int courseId = (Integer) request.getData(Constants.KEY_COURSE_ID);

            // Try to get student by ID first, if not found try by user_id
            Student student = studentService.getStudentById(studentIdOrUserId);
            if (student == null) {
                student = studentService.getStudentByUserId(studentIdOrUserId);
            }

            int studentId = (student != null) ? student.getStudentId() : studentIdOrUserId;

            CourseRegistrationService.RegistrationValidation validation = registrationService
                    .validateRegistration(studentId, courseId);

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
            Integer studentIdOrUserId = (Integer) request.getData(Constants.KEY_STUDENT_ID);
            String academicYear = (String) request.getData(Constants.KEY_ACADEMIC_YEAR);
            int semester = (Integer) request.getData(Constants.KEY_SEMESTER);

            // Try to get student by ID first, if not found try by user_id
            Student student = studentService.getStudentById(studentIdOrUserId);
            if (student == null) {
                student = studentService.getStudentByUserId(studentIdOrUserId);
            }

            int studentId = (student != null) ? student.getStudentId() : studentIdOrUserId;

            int credits = registrationService.getStudentCredits(studentId, academicYear, semester);

            Message response = Message.createSuccessResponse(request.getAction(), "Success");
            response.addData("credits", credits);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error getting student credits: " + e.getMessage());
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
            LOGGER.severe("Error getting registration stats: " + e.getMessage());
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

            if (username == null || password == null || fullName == null) {
                return Message.createErrorResponse(request.getAction(), "Thiếu thông tin bắt buộc");
            }

            // Create new teacher
            User newTeacher = new User();
            newTeacher.setUsername(username);
            newTeacher.setPassword(password);
            newTeacher.setFullName(fullName);
            newTeacher.setEmail(email);
            newTeacher.setPhone(phone);
            newTeacher.setAddress(address);
            newTeacher.setRole(User.UserRole.TEACHER);
            newTeacher.setActive(true);

            UserDAO userDAO = new UserDAO();
            boolean success = userDAO.addUser(newTeacher);

            if (success) {
                LOGGER.info("Teacher added: " + username + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Thêm giảng viên thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể thêm giảng viên");
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

            if (fullName != null)
                teacher.setFullName(fullName);
            if (email != null)
                teacher.setEmail(email);
            if (phone != null)
                teacher.setPhone(phone);
            if (address != null)
                teacher.setAddress(address);

            boolean success = userDAO.updateUser(teacher);

            // Update password if provided
            if (password != null && !password.isEmpty()) {
                userDAO.changePassword(userId, password);
            }

            if (success) {
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

            boolean success = userDAO.deactivateUser(userId);

            if (success) {
                LOGGER.info("Teacher deactivated: " + teacher.getUsername() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Vô hiệu hóa giảng viên thành công");
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
                String userType = user.getRole() == User.UserRole.TEACHER ? "giảng viên"
                        : user.getRole() == User.UserRole.STUDENT ? "sinh viên" : "người dùng";
                LOGGER.info("User activated: " + userId + " (" + user.getUsername() + ", " + userType + ") by "
                        + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Kích hoạt " + userType + " thành công");
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
            Integer teacherId = request.getData("teacherId", Integer.class);
            if (teacherId == null) {
                return Message.createErrorResponse(request.getAction(), "Teacher ID is required");
            }

            CourseDAO courseDAO = new CourseDAO();
            List<com.university.sms.model.Course> courses = courseDAO.findByTeacherId(teacherId);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + courses.size() + " courses");
            response.addData("courses", courses);

            LOGGER.info("Retrieved " + courses.size() + " courses for teacher " + teacherId);
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

            boolean success = subjectService.addSubject(subject);
            if (success) {
                LOGGER.info("Subject added: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Thêm môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể thêm môn học");
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

            boolean success = subjectService.updateSubject(subject);
            if (success) {
                LOGGER.info("Subject updated: " + subject.getSubjectCode() + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Cập nhật môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Không thể cập nhật môn học");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating subject", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleDeleteSubject(Message request) {
        try {
            // Only admin can delete subjects
            if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
                return Message.createErrorResponse(request.getAction(), "Chỉ admin mới có quyền xóa môn học");
            }

            Integer subjectId = request.getData("subjectId", Integer.class);
            if (subjectId == null || subjectId <= 0) {
                return Message.createErrorResponse(request.getAction(), "ID môn học không hợp lệ");
            }

            boolean success = subjectService.deleteSubject(subjectId);
            if (success) {
                LOGGER.info("Subject deleted: " + subjectId + " by " + currentUser.getUsername());
                return Message.createSuccessResponse(request.getAction(), "Xóa môn học thành công");
            } else {
                return Message.createErrorResponse(request.getAction(),
                        "Không thể xóa môn học (có thể đang được sử dụng)");
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

    // ==================== Enrollment Handlers (Additional) ====================

    private Message handleGetEnrollmentsByCourse(Message request) {
        try {
            Integer courseId = request.getData("courseId", Integer.class);
            if (courseId == null) {
                return Message.createErrorResponse(request.getAction(), "Course ID is required");
            }

            EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
            List<com.university.sms.model.Enrollment> enrollments = enrollmentDAO.findByCourseId(courseId);

            Message response = Message.createSuccessResponse(request.getAction(),
                    "Found " + enrollments.size() + " enrollments");
            response.addData("enrollments", enrollments);

            LOGGER.info("Retrieved " + enrollments.size() + " enrollments for course " + courseId);
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
                return Message.createSuccessResponse(request.getAction(), "Thêm điểm thành công");
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
                return Message.createSuccessResponse(request.getAction(), "Cập nhật điểm thành công");
            } else {
                return Message.createErrorResponse(request.getAction(), "Cập nhật điểm thất bại");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating grade", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }

    private Message handleGetGrades(Message request) {
        try {
            Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);
            Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            
            List<Grade> grades;
            
            if (enrollmentId != null) {
                grades = gradeService.getGradesByEnrollment(enrollmentId);
            } else if (studentId != null && courseId != null) {
                grades = gradeService.getGradesByStudentAndCourse(studentId, courseId);
            } else if (studentId != null) {
                grades = gradeService.getGradesByStudent(studentId);
            } else if (courseId != null) {
                grades = gradeService.getGradesByCourse(courseId);
            } else {
                return Message.createErrorResponse(request.getAction(), 
                    "enrollment_id, student_id, hoặc course_id is required");
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
            Integer enrollmentId = request.getData(Constants.KEY_ENROLLMENT, Integer.class);
            if (enrollmentId == null) {
                return Message.createErrorResponse(request.getAction(), "Enrollment ID is required");
            }

            boolean result = gradeService.finalizeCourseGrade(enrollmentId);
            
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
            
            if (userId != null) {
                notifications = notificationService.getNotificationsByUser(userId);
            } else if (currentUser != null) {
                notifications = notificationService.getNotificationsByUser(currentUser.getUserId());
            } else {
                return Message.createErrorResponse(request.getAction(), "User ID is required");
            }

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

            // Set sender to current user if not set
            if (notification.getSenderId() <= 0 && currentUser != null) {
                notification.setSenderId(currentUser.getUserId());
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
                // Mark all notifications of user as read
                boolean result = notificationService.markAllAsReadForUser(currentUser.getUserId());
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
                // Get student timetable
                StudentDAO studentDAO = new StudentDAO();
                Student student = studentDAO.findByUserId(userId);
                if (student != null) {
                    timetable = timetableService.getStudentTimetable(student.getStudentId());
                }
            } else if ("TEACHER".equalsIgnoreCase(userRole)) {
                // Get teacher timetable
                timetable = timetableService.getTeacherTimetable(userId);
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
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            
            // If not provided, try to get from current user
            if (studentId == null && currentUser != null && "STUDENT".equalsIgnoreCase(currentUser.getRole().toString())) {
                StudentDAO studentDAO = new StudentDAO();
                Student student = studentDAO.findByUserId(currentUser.getUserId());
                if (student != null) {
                    studentId = student.getStudentId();
                }
            }
            
            if (studentId == null) {
                return Message.createErrorResponse(request.getAction(), "Student ID is required");
            }
            
            var transcript = transcriptService.generateTranscript(studentId);
            
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
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            String academicYear = request.getData(Constants.KEY_ACADEMIC_YEAR, String.class);
            Integer semester = request.getData(Constants.KEY_SEMESTER, Integer.class);
            
            if (studentId == null || academicYear == null || semester == null) {
                return Message.createErrorResponse(request.getAction(), "Student ID, academic year and semester are required");
            }
            
            var semesterRecord = transcriptService.getSemesterTranscript(studentId, academicYear, semester);
            
            if (semesterRecord == null) {
                return Message.createErrorResponse(request.getAction(), "Semester transcript not found");
            }
            
            Message response = Message.createSuccessResponse(request.getAction(), "Semester transcript retrieved successfully");
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
            Integer facultyId = request.getData(Constants.KEY_FACULTY_ID, Integer.class);
            
            if (facultyId == null) {
                return Message.createErrorResponse(request.getAction(), "Faculty ID is required");
            }
            
            List<?> honorStudents = transcriptService.getHonorStudents(facultyId);
            
            Message response = Message.createSuccessResponse(request.getAction(), "Honor students retrieved successfully");
            response.addData(Constants.KEY_HONOR_STUDENTS, honorStudents);
            return response;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting honor students", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }
    
    private Message handleGetFacultyStatistics(Message request) {
        try {
            Integer facultyId = request.getData(Constants.KEY_FACULTY_ID, Integer.class);
            
            if (facultyId == null) {
                return Message.createErrorResponse(request.getAction(), "Faculty ID is required");
            }
            
            Map<String, Object> statistics = transcriptService.getFacultyStatistics(facultyId);
            
            Message response = Message.createSuccessResponse(request.getAction(), "Faculty statistics retrieved successfully");
            response.addData(Constants.KEY_STATISTICS, statistics);
            return response;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting faculty statistics", e);
            return Message.createErrorResponse(request.getAction(), "Lỗi: " + e.getMessage());
        }
    }
    
    private Message handleValidateSchedule(Message request) {
        try {
            Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
            Integer courseId = request.getData(Constants.KEY_COURSE_ID, Integer.class);
            
            if (studentId == null || courseId == null) {
                return Message.createErrorResponse(request.getAction(), "Student ID and Course ID are required");
            }
            
            boolean isValid = timetableService.validateSchedule(studentId, courseId);
            
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

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isConnected && !clientSocket.isClosed();
    }
}
