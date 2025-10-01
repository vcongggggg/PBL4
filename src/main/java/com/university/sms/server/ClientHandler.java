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
import java.util.logging.Level;
import java.util.logging.Logger;

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
                
                // Course actions
                case Constants.ACTION_GET_ALL_COURSES:
                    return handleGetAllCourses(request);
                case Constants.ACTION_GET_COURSES:
                    return handleGetAllCourses(request); // Use same handler
                case Constants.ACTION_GET_COURSE_INFO:
                    return handleGetCourseInfo(request);
                
                // Enrollment actions
                case Constants.ACTION_GET_ENROLLMENTS:
                    return handleGetEnrollments(request);
                case Constants.ACTION_GET_STUDENT_GRADES:
                    return handleGetStudentGrades(request);
                
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
                Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO, "Lấy thông tin thành công");
                response.addData(Constants.KEY_STUDENT, student);
                return response;
            }
        } else if (studentId != null && (currentUser.getRole() == User.UserRole.ADMIN || 
                                      currentUser.getRole() == User.UserRole.TEACHER)) {
            // Admin và giáo viên có thể xem thông tin sinh viên theo ID
            var student = studentService.getStudentById(studentId);
            if (student != null) {
                Message response = Message.createSuccessResponse(Constants.ACTION_GET_STUDENT_INFO, "Lấy thông tin thành công");
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
        // Chỉ admin mới có quyền thêm sinh viên
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, Constants.MSG_UNAUTHORIZED);
        }
        
        // Implementation sẽ được thêm sau
        return Message.createErrorResponse(Constants.ACTION_ADD_STUDENT, "Chức năng đang phát triển");
    }

    /**
     * Xử lý cập nhật thông tin sinh viên
     */
    private Message handleUpdateStudent(Message request) {
        // Implementation sẽ được thêm sau
        return Message.createErrorResponse(Constants.ACTION_UPDATE_STUDENT, "Chức năng đang phát triển");
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
            Message response = Message.createSuccessResponse(Constants.ACTION_GET_COURSE_INFO, "Lấy thông tin khóa học thành công");
            response.addData(Constants.KEY_COURSE, course);
            return response;
        }
        
        return Message.createErrorResponse(Constants.ACTION_GET_COURSE_INFO, Constants.MSG_COURSE_NOT_FOUND);
    }

    /**
     * Xử lý lấy danh sách đăng ký
     */
    private Message handleGetEnrollments(Message request) {
        // Implementation sẽ được thêm sau
        return Message.createErrorResponse(Constants.ACTION_GET_ENROLLMENTS, "Chức năng đang phát triển");
    }

    /**
     * Xử lý lấy điểm sinh viên
     */
    private Message handleGetStudentGrades(Message request) {
        // Implementation sẽ được thêm sau
        return Message.createErrorResponse(Constants.ACTION_GET_STUDENT_GRADES, "Chức năng đang phát triển");
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
        return Constants.ACTION_LOGIN.equals(action);
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
