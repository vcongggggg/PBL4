package com.university.sms.client;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;

/**
 * Kết nối chuẩn (regular) dùng socket tới server DB.
 */
public class ServerConnection extends BaseServerConnection implements IServerConnection {

    public ServerConnection(String serverHost, int serverPort) {
        super(serverHost, serverPort);
    }

    @Override
    protected void onConnect() {
        // Gửi metadata nhẹ để server biết nguồn dữ liệu của client này
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                com.university.sms.common.Message request = com.university.sms.common.Message
                        .createRequest(com.university.sms.common.Constants.ACTION_SYNC_CHECK);
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("database_type", "REGULAR");
                metadata.put("db_version", 1);
                metadata.put("total_records", 0);
                request.addData("metadata", metadata);
                sendRequestAndWait(request, 15);
            } catch (Exception ignored) {
            }
        });
    }

    // Auth
    @Override
    public Message login(String username, String password) {
        Message request = Message.createRequest(Constants.ACTION_LOGIN);
        request.addData(Constants.KEY_USERNAME, username);
        request.addData(Constants.KEY_PASSWORD, password);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message logout() {
        Message request = Message.createRequest(Constants.ACTION_LOGOUT);
        return sendRequestAndWait(request, 60);
    }

    // Students
    @Override
    public Message getStudentInfo(Integer studentId) {
        Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
        if (studentId != null)
            request.addData(Constants.KEY_STUDENT_ID, studentId);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message getAllStudents() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
        return sendRequestAndWait(request, 120);
    }

    @Override
    public Message searchStudents(String keyword) {
        Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
        request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message deleteStudent(int studentId) {
        Message request = Message.createRequest(Constants.ACTION_DELETE_STUDENT);
        request.addData(Constants.KEY_STUDENT_ID, studentId);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message addStudent(com.university.sms.model.Student student) {
        Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
        request.addData(Constants.KEY_STUDENT, student);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message updateStudent(com.university.sms.model.Student student) {
        Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
        request.addData(Constants.KEY_STUDENT, student);
        return sendRequestAndWait(request, 60);
    }

    // Courses
    @Override
    public Message getAllCourses() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
        return sendRequestAndWait(request, 120);
    }

    @Override
    public Message getCourses() {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSES);
        return sendRequestAndWait(request, 60);
    }

    @Override
    public Message getCourseInfo(int courseId) {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
        request.addData(Constants.KEY_COURSE_ID, courseId);
        return sendRequestAndWait(request, 60);
    }

    // User profile
    @Override
    public Message changePassword(String newPassword) {
        Message request = Message.createRequest(Constants.ACTION_CHANGE_PASSWORD);
        request.addData(Constants.KEY_PASSWORD, newPassword);
        return sendRequestAndWait(request, 60);
    }
}
