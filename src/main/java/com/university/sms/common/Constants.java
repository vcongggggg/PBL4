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

    // Teacher Management Actions
    public static final String ACTION_ADD_TEACHER = "ADD_TEACHER";
    public static final String ACTION_UPDATE_TEACHER = "UPDATE_TEACHER";
    public static final String ACTION_DELETE_TEACHER = "DELETE_TEACHER";
    public static final String ACTION_GET_ALL_TEACHERS_INCLUDE_INACTIVE = "GET_ALL_TEACHERS_INCLUDE_INACTIVE";

    // Student Actions
    public static final String ACTION_GET_STUDENT_INFO = "GET_STUDENT_INFO";
    public static final String ACTION_UPDATE_STUDENT = "UPDATE_STUDENT";
    public static final String ACTION_GET_ALL_STUDENTS = "GET_ALL_STUDENTS";
    public static final String ACTION_SEARCH_STUDENTS = "SEARCH_STUDENTS";
    public static final String ACTION_GET_STUDENTS_BY_CLASS = "GET_STUDENTS_BY_CLASS";
    public static final String ACTION_ADD_STUDENT = "ADD_STUDENT";
    public static final String ACTION_DELETE_STUDENT = "DELETE_STUDENT";
    public static final String ACTION_GET_ALL_STUDENTS_INCLUDE_INACTIVE = "GET_ALL_STUDENTS_INCLUDE_INACTIVE";

    // User Activation (common for both teacher and student)
    public static final String ACTION_ACTIVATE_USER = "ACTIVATE_USER";

    // Teacher Actions
    public static final String ACTION_GET_ALL_TEACHERS = "GET_ALL_TEACHERS";
    public static final String ACTION_SEARCH_TEACHERS = "SEARCH_TEACHERS";
    public static final String ACTION_GET_COURSES_BY_TEACHER = "GET_COURSES_BY_TEACHER";

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
    public static final String ACTION_GET_ENROLLMENTS_BY_COURSE = "GET_ENROLLMENTS_BY_COURSE";
    public static final String ACTION_GET_STUDENT_GRADES = "GET_STUDENT_GRADES";

    // Grade Actions
    public static final String ACTION_ADD_GRADE = "ADD_GRADE";
    public static final String ACTION_UPDATE_GRADE = "UPDATE_GRADE";
    public static final String ACTION_GET_GRADES = "GET_GRADES";
    public static final String ACTION_CALCULATE_FINAL_GRADE = "CALCULATE_FINAL_GRADE";

    // Faculty Actions
    public static final String ACTION_GET_FACULTIES = "GET_FACULTIES";
    public static final String ACTION_GET_ALL_FACULTIES = "GET_ALL_FACULTIES";
    public static final String ACTION_ADD_FACULTY = "ADD_FACULTY";
    public static final String ACTION_UPDATE_FACULTY = "UPDATE_FACULTY";

    // Subject Actions
    public static final String ACTION_GET_SUBJECTS = "GET_SUBJECTS";
    public static final String ACTION_GET_ALL_SUBJECTS = "GET_ALL_SUBJECTS";
    public static final String ACTION_SEARCH_SUBJECTS = "SEARCH_SUBJECTS";
    public static final String ACTION_ADD_SUBJECT = "ADD_SUBJECT";
    public static final String ACTION_UPDATE_SUBJECT = "UPDATE_SUBJECT";
    public static final String ACTION_DELETE_SUBJECT = "DELETE_SUBJECT";

    // Class Actions
    public static final String ACTION_GET_CLASSES = "GET_CLASSES";
    public static final String ACTION_ADD_CLASS = "ADD_CLASS";
    public static final String ACTION_UPDATE_CLASS = "UPDATE_CLASS";

    // Report Actions
    public static final String ACTION_GET_STUDENT_TRANSCRIPT = "GET_STUDENT_TRANSCRIPT";
    public static final String ACTION_GET_CLASS_REPORT = "GET_CLASS_REPORT";
    public static final String ACTION_GET_FACULTY_REPORT = "GET_FACULTY_REPORT";

    // Notification Actions
    public static final String ACTION_GET_NOTIFICATIONS = "GET_NOTIFICATIONS";
    public static final String ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION";
    public static final String ACTION_MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ";

    // System Actions
    public static final String ACTION_GET_SYSTEM_CONFIG = "GET_SYSTEM_CONFIG";
    public static final String ACTION_UPDATE_SYSTEM_CONFIG = "UPDATE_SYSTEM_CONFIG";
    public static final String ACTION_BACKUP_DATABASE = "BACKUP_DATABASE";
    public static final String ACTION_GET_LOGIN_HISTORY = "GET_LOGIN_HISTORY";

    // Sync Actions
    public static final String ACTION_SYNC_CHECK = "SYNC_CHECK";
    public static final String ACTION_SYNC_DATA = "SYNC_DATA";
    public static final String ACTION_UPLOAD_USERS = "UPLOAD_USERS";
    public static final String ACTION_UPLOAD_STUDENTS = "UPLOAD_STUDENTS";
    public static final String ACTION_UPLOAD_COURSES = "UPLOAD_COURSES";
    public static final String ACTION_UPLOAD_ENROLLMENTS = "UPLOAD_ENROLLMENTS";

    // Class Opening Request Actions (Teacher & Admin)
    public static final String ACTION_GET_ALL_CLASS_REQUESTS = "GET_ALL_CLASS_REQUESTS";
    public static final String ACTION_GET_CLASS_REQUEST_BY_ID = "GET_CLASS_REQUEST_BY_ID";
    public static final String ACTION_GET_MY_CLASS_REQUESTS = "GET_MY_CLASS_REQUESTS";
    public static final String ACTION_GET_PENDING_CLASS_REQUESTS = "GET_PENDING_CLASS_REQUESTS";
    public static final String ACTION_SUBMIT_CLASS_REQUEST = "SUBMIT_CLASS_REQUEST";
    public static final String ACTION_UPDATE_CLASS_REQUEST = "UPDATE_CLASS_REQUEST";
    public static final String ACTION_CANCEL_CLASS_REQUEST = "CANCEL_CLASS_REQUEST";
    public static final String ACTION_APPROVE_CLASS_REQUEST = "APPROVE_CLASS_REQUEST";
    public static final String ACTION_REJECT_CLASS_REQUEST = "REJECT_CLASS_REQUEST";
    public static final String ACTION_GET_CLASS_REQUEST_STATS = "GET_CLASS_REQUEST_STATS";

    // Course Registration Actions (Student & Admin)
    public static final String ACTION_GET_ALL_REGISTRATIONS = "GET_ALL_REGISTRATIONS";
    public static final String ACTION_GET_REGISTRATION_BY_ID = "GET_REGISTRATION_BY_ID";
    public static final String ACTION_GET_MY_REGISTRATIONS = "GET_MY_REGISTRATIONS";
    public static final String ACTION_GET_COURSE_REGISTRATIONS = "GET_COURSE_REGISTRATIONS";
    public static final String ACTION_GET_PENDING_REGISTRATIONS = "GET_PENDING_REGISTRATIONS";
    public static final String ACTION_REGISTER_COURSE = "REGISTER_COURSE";
    public static final String ACTION_CANCEL_REGISTRATION = "CANCEL_REGISTRATION";
    public static final String ACTION_APPROVE_REGISTRATION = "APPROVE_REGISTRATION";
    public static final String ACTION_REJECT_REGISTRATION = "REJECT_REGISTRATION";
    public static final String ACTION_VALIDATE_REGISTRATION = "VALIDATE_REGISTRATION";
    public static final String ACTION_GET_STUDENT_CREDITS = "GET_STUDENT_CREDITS";
    public static final String ACTION_GET_REGISTRATION_STATS = "GET_REGISTRATION_STATS";

    // Data Keys
    public static final String KEY_USER = "user";
    public static final String KEY_STUDENT = "student";
    public static final String KEY_COURSE = "course";
    public static final String KEY_ENROLLMENT = "enrollment";
    public static final String KEY_GRADE = "grade";
    public static final String KEY_FACULTY = "faculty";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_CLASS = "class";
    public static final String KEY_NOTIFICATION = "notification";
    public static final String KEY_CLASS_REQUEST = "classRequest";
    public static final String KEY_REGISTRATION = "registration";

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_STUDENT_ID = "studentId";
    public static final String KEY_COURSE_ID = "courseId";
    public static final String KEY_SEARCH_KEYWORD = "searchKeyword";
    public static final String KEY_CLASS_ID = "classId";
    public static final String KEY_FACULTY_ID = "facultyId";
    public static final String KEY_ACADEMIC_YEAR = "academicYear";
    public static final String KEY_SEMESTER = "semester";
    public static final String KEY_REQUEST_ID = "requestId";
    public static final String KEY_REGISTRATION_ID = "registrationId";
    public static final String KEY_TEACHER_ID = "teacherId";
    public static final String KEY_SUBJECT_ID = "subjectId";
    public static final String KEY_ADMIN_ID = "adminId";
    public static final String KEY_NOTE = "note";
    public static final String KEY_REASON = "reason";
    public static final String KEY_STATUS = "status";
    public static final String KEY_STATISTICS = "statistics";
    public static final String KEY_GRADE_ID = "gradeId";
    public static final String KEY_GRADE_TYPE = "gradeType";
    public static final String KEY_NOTIFICATION_ID = "notificationId";
    public static final String KEY_UNREAD_COUNT = "unreadCount";
    public static final String KEY_TARGET_TYPE = "targetType";
    public static final String KEY_TARGET_ID = "targetId";
    public static final String KEY_PRIORITY = "priority";

    // Lists
    public static final String KEY_STUDENTS = "students";
    public static final String KEY_COURSES = "courses";
    public static final String KEY_ENROLLMENTS = "enrollments";
    public static final String KEY_GRADES = "grades";
    public static final String KEY_FACULTIES = "faculties";
    public static final String KEY_SUBJECTS = "subjects";
    public static final String KEY_CLASSES = "classes";
    public static final String KEY_NOTIFICATIONS = "notifications";
    public static final String KEY_CLASS_REQUESTS = "classRequests";
    public static final String KEY_REGISTRATIONS = "registrations";

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

    // Timetable & Transcript Actions
    public static final String ACTION_GET_TIMETABLE = "GET_TIMETABLE";
    public static final String ACTION_GET_TRANSCRIPT = "GET_TRANSCRIPT";
    public static final String ACTION_GET_SEMESTER_TRANSCRIPT = "GET_SEMESTER_TRANSCRIPT";
    public static final String ACTION_GET_HONOR_STUDENTS = "GET_HONOR_STUDENTS";
    public static final String ACTION_GET_FACULTY_STATISTICS = "GET_FACULTY_STATISTICS";
    public static final String ACTION_VALIDATE_SCHEDULE = "VALIDATE_SCHEDULE";
    
    // Timetable & Transcript Keys
    public static final String KEY_TIMETABLE = "timetable";
    public static final String KEY_TRANSCRIPT = "transcript";
    public static final String KEY_SEMESTER_RECORDS = "semester_records";
    public static final String KEY_HONOR_STUDENTS = "honor_students";
    public static final String KEY_CUMULATIVE_GPA = "cumulative_gpa";
    public static final String KEY_SEMESTER_GPA = "semester_gpa";
    public static final String KEY_ACADEMIC_RANK = "academic_rank";
    public static final String KEY_TOTAL_CREDITS = "total_credits";
    public static final String KEY_USER_ROLE = "user_role";

    // File Paths
    public static final String CONFIG_FILE = "database.properties";
    public static final String LOG_FILE = "application.log";

    private Constants() {
        // Private constructor to prevent instantiation
    }
}
