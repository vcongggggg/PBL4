package com.university.sms.common;

/**
 * Constants cho hệ thống
 */
public class Constants {
    
    // Server Actions
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_CHANGE_PASSWORD = "CHANGE_PASSWORD";
    
    // Student Actions
    public static final String ACTION_GET_STUDENT_INFO = "GET_STUDENT_INFO";
    public static final String ACTION_UPDATE_STUDENT = "UPDATE_STUDENT";
    public static final String ACTION_GET_ALL_STUDENTS = "GET_ALL_STUDENTS";
    public static final String ACTION_SEARCH_STUDENTS = "SEARCH_STUDENTS";
    public static final String ACTION_GET_STUDENTS_BY_CLASS = "GET_STUDENTS_BY_CLASS";
    public static final String ACTION_ADD_STUDENT = "ADD_STUDENT";
    
    // Course Actions
    public static final String ACTION_GET_ALL_COURSES = "GET_ALL_COURSES";
    public static final String ACTION_GET_COURSES = "GET_COURSES";
    public static final String ACTION_GET_COURSE_INFO = "GET_COURSE_INFO";
    public static final String ACTION_ADD_COURSE = "ADD_COURSE";
    public static final String ACTION_UPDATE_COURSE = "UPDATE_COURSE";
    public static final String ACTION_DELETE_COURSE = "DELETE_COURSE";
    
    // Enrollment Actions
    public static final String ACTION_ENROLL_COURSE = "ENROLL_COURSE";
    public static final String ACTION_DROP_COURSE = "DROP_COURSE";
    public static final String ACTION_GET_ENROLLMENTS = "GET_ENROLLMENTS";
    public static final String ACTION_GET_STUDENT_GRADES = "GET_STUDENT_GRADES";
    
    // Grade Actions
    public static final String ACTION_ADD_GRADE = "ADD_GRADE";
    public static final String ACTION_UPDATE_GRADE = "UPDATE_GRADE";
    public static final String ACTION_GET_GRADES = "GET_GRADES";
    public static final String ACTION_CALCULATE_FINAL_GRADE = "CALCULATE_FINAL_GRADE";
    
    // Attendance Actions
    public static final String ACTION_MARK_ATTENDANCE = "MARK_ATTENDANCE";
    public static final String ACTION_GET_ATTENDANCE = "GET_ATTENDANCE";
    public static final String ACTION_UPDATE_ATTENDANCE = "UPDATE_ATTENDANCE";
    
    // Department Actions
    public static final String ACTION_GET_DEPARTMENTS = "GET_DEPARTMENTS";
    public static final String ACTION_ADD_DEPARTMENT = "ADD_DEPARTMENT";
    public static final String ACTION_UPDATE_DEPARTMENT = "UPDATE_DEPARTMENT";
    
    // Subject Actions
    public static final String ACTION_GET_SUBJECTS = "GET_SUBJECTS";
    public static final String ACTION_ADD_SUBJECT = "ADD_SUBJECT";
    public static final String ACTION_UPDATE_SUBJECT = "UPDATE_SUBJECT";
    
    // Class Actions
    public static final String ACTION_GET_CLASSES = "GET_CLASSES";
    public static final String ACTION_ADD_CLASS = "ADD_CLASS";
    public static final String ACTION_UPDATE_CLASS = "UPDATE_CLASS";
    
    // Report Actions
    public static final String ACTION_GET_STUDENT_TRANSCRIPT = "GET_STUDENT_TRANSCRIPT";
    public static final String ACTION_GET_CLASS_REPORT = "GET_CLASS_REPORT";
    public static final String ACTION_GET_DEPARTMENT_REPORT = "GET_DEPARTMENT_REPORT";
    
    // Notification Actions
    public static final String ACTION_GET_NOTIFICATIONS = "GET_NOTIFICATIONS";
    public static final String ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION";
    public static final String ACTION_MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ";
    
    // System Actions
    public static final String ACTION_GET_SYSTEM_CONFIG = "GET_SYSTEM_CONFIG";
    public static final String ACTION_UPDATE_SYSTEM_CONFIG = "UPDATE_SYSTEM_CONFIG";
    public static final String ACTION_BACKUP_DATABASE = "BACKUP_DATABASE";
    public static final String ACTION_GET_LOGIN_HISTORY = "GET_LOGIN_HISTORY";
    
    // Data Keys
    public static final String KEY_USER = "user";
    public static final String KEY_STUDENT = "student";
    public static final String KEY_COURSE = "course";
    public static final String KEY_ENROLLMENT = "enrollment";
    public static final String KEY_GRADE = "grade";
    public static final String KEY_ATTENDANCE = "attendance";
    public static final String KEY_DEPARTMENT = "department";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_CLASS = "class";
    public static final String KEY_NOTIFICATION = "notification";
    
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_STUDENT_ID = "studentId";
    public static final String KEY_COURSE_ID = "courseId";
    public static final String KEY_SEARCH_KEYWORD = "searchKeyword";
    public static final String KEY_CLASS_ID = "classId";
    public static final String KEY_DEPARTMENT_ID = "departmentId";
    public static final String KEY_ACADEMIC_YEAR = "academicYear";
    public static final String KEY_SEMESTER = "semester";
    
    // Lists
    public static final String KEY_STUDENTS = "students";
    public static final String KEY_COURSES = "courses";
    public static final String KEY_ENROLLMENTS = "enrollments";
    public static final String KEY_GRADES = "grades";
    public static final String KEY_ATTENDANCES = "attendances";
    public static final String KEY_DEPARTMENTS = "departments";
    public static final String KEY_SUBJECTS = "subjects";
    public static final String KEY_CLASSES = "classes";
    public static final String KEY_NOTIFICATIONS = "notifications";
    
    // Response Messages
    public static final String MSG_SUCCESS = "Thao tác thành công";
    public static final String MSG_LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String MSG_LOGIN_FAILED = "Đăng nhập thất bại";
    public static final String MSG_LOGOUT_SUCCESS = "Đăng xuất thành công";
    public static final String MSG_INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng";
    public static final String MSG_USER_NOT_FOUND = "Không tìm thấy người dùng";
    public static final String MSG_STUDENT_NOT_FOUND = "Không tìm thấy sinh viên";
    public static final String MSG_COURSE_NOT_FOUND = "Không tìm thấy khóa học";
    public static final String MSG_UNAUTHORIZED = "Không có quyền truy cập";
    public static final String MSG_SERVER_ERROR = "Lỗi server";
    public static final String MSG_DATABASE_ERROR = "Lỗi cơ sở dữ liệu";
    public static final String MSG_INVALID_DATA = "Dữ liệu không hợp lệ";
    public static final String MSG_DUPLICATE_DATA = "Dữ liệu đã tồn tại";
    
    // Default Values
    public static final int DEFAULT_SERVER_PORT = 8888;
    public static final String DEFAULT_SERVER_HOST = "localhost";
    public static final int DEFAULT_SESSION_TIMEOUT = 3600000; // 1 hour in milliseconds
    public static final int DEFAULT_MAX_CONNECTIONS = 100;
    
    // File Paths
    public static final String CONFIG_FILE = "database.properties";
    public static final String LOG_FILE = "application.log";
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
