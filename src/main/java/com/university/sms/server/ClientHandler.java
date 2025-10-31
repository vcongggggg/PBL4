package com.university.sms.server;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;
import com.university.sms.service.AuthenticationService;
import com.university.sms.service.StudentService;
import com.university.sms.service.CourseService;

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

    // Source/client provenance (e.g., CSV, POSTGRES, etc.) captured during sync
    private String clientSource = "UNKNOWN";

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;

        // Initialize services
        this.authService = new AuthenticationService();
        this.studentService = new StudentService();
        this.courseService = new CourseService();
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

                // Enrollment actions
                case Constants.ACTION_GET_ENROLLMENTS:
                    return handleGetEnrollments(request);
                case Constants.ACTION_GET_STUDENT_GRADES:
                    return handleGetStudentGrades(request);
                case Constants.ACTION_ENROLL_COURSE:
                    return handleEnrollCourse(request);
                case Constants.ACTION_DROP_COURSE:
                    return handleDropCourse(request);
                case Constants.ACTION_ADD_GRADE:
                case Constants.ACTION_UPDATE_GRADE:
                    return handleUpdateFinalGrade(request);
                case Constants.ACTION_MARK_ATTENDANCE:
                case Constants.ACTION_UPDATE_ATTENDANCE:
                    return handleUpdateAttendanceRate(request);

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
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_STUDENT, Constants.MSG_UNAUTHORIZED);
        }
        Integer studentId = request.getData(Constants.KEY_STUDENT_ID, Integer.class);
        if (studentId == null || studentId <= 0) {
            return Message.createErrorResponse(Constants.ACTION_DELETE_STUDENT, Constants.MSG_INVALID_DATA);
        }
        com.university.sms.dao.StudentDAO dao = new com.university.sms.dao.StudentDAO();
        boolean ok = dao.deleteStudent(studentId);
        if (ok) {
            return Message.createSuccessResponse(Constants.ACTION_DELETE_STUDENT, Constants.MSG_SUCCESS);
        }
        return Message.createErrorResponse(Constants.ACTION_DELETE_STUDENT, Constants.MSG_DATABASE_ERROR);
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
     * Cập nhật tỷ lệ điểm danh tổng hợp cho enrollment
     */
    private Message handleUpdateAttendanceRate(Message request) {
        try {
            Integer enrollmentId = request.getData("enrollmentId", Integer.class);
            java.math.BigDecimal attendanceRate = request.getData("attendanceRate", java.math.BigDecimal.class);
            if (enrollmentId == null || attendanceRate == null) {
                return Message.createErrorResponse(Constants.ACTION_UPDATE_ATTENDANCE, Constants.MSG_INVALID_DATA);
            }
            com.university.sms.dao.EnrollmentDAO enrollmentDAO = new com.university.sms.dao.EnrollmentDAO();
            boolean ok = enrollmentDAO.updateAttendanceRate(enrollmentId, attendanceRate);
            if (ok) {
                return Message.createSuccessResponse(Constants.ACTION_UPDATE_ATTENDANCE, Constants.MSG_SUCCESS);
            }
            return Message.createErrorResponse(Constants.ACTION_UPDATE_ATTENDANCE, Constants.MSG_DATABASE_ERROR);
        } catch (Exception e) {
            LOGGER.severe("Error updating attendance: " + e.getMessage());
            return Message.createErrorResponse(Constants.ACTION_UPDATE_ATTENDANCE, Constants.MSG_SERVER_ERROR);
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

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isConnected && !clientSocket.isClosed();
    }
}
