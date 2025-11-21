package com.university.sms.client;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.Student;

import java.util.logging.Logger;

/**
 * Standard server connection for regular clients
 * Extends BaseServerConnection for socket communication
 */
public class ServerConnection extends BaseServerConnection {
    private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
    private static final long REQUEST_TIMEOUT_SECONDS = 60;

    public ServerConnection(String serverHost, int serverPort) {
        super(serverHost, serverPort);
    }

    @Override
    protected void onConnect() {
        // No special metadata for regular connection
        LOGGER.info("ServerConnection established");
    }

    /**
     * Implementation of sendRequest from IServerConnection
     */
    @Override
    public Message sendRequest(Message request) {
        return sendRequestAndWait(request, REQUEST_TIMEOUT_SECONDS);
    }

    @Override
    public Message login(String username, String password) {
        Message request = Message.createRequest(Constants.ACTION_LOGIN);
        request.addData(Constants.KEY_USERNAME, username);
        request.addData(Constants.KEY_PASSWORD, password);
        return sendRequest(request);
    }

    @Override
    public Message logout() {
        Message request = Message.createRequest(Constants.ACTION_LOGOUT);
        return sendRequest(request);
    }

    @Override
    public Message getStudentInfo(Integer studentId) {
        Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
        if (studentId != null) {
            request.addData(Constants.KEY_STUDENT_ID, studentId);
        }
        return sendRequest(request);
    }

    @Override
    public Message getAllStudents() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
        return sendRequest(request);
    }

    @Override
    public Message searchStudents(String keyword) {
        Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
        request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);
        return sendRequest(request);
    }

    @Override
    public Message deleteStudent(String studentCode) {
        Message request = Message.createRequest(Constants.ACTION_DELETE_STUDENT);
        request.addData("studentCode", studentCode);
        return sendRequest(request);
    }

    @Override
    public Message addStudent(Student student) {
        Message request = Message.createRequest(Constants.ACTION_ADD_STUDENT);
        request.addData(Constants.KEY_STUDENT, student);
        return sendRequest(request);
    }

    @Override
    public Message updateStudent(Student student) {
        Message request = Message.createRequest(Constants.ACTION_UPDATE_STUDENT);
        request.addData(Constants.KEY_STUDENT, student);
        return sendRequest(request);
    }

    @Override
    public Message getAllCourses() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
        return sendRequest(request);
    }

    @Override
    public Message getCourseInfo(int courseId) {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
        request.addData(Constants.KEY_COURSE_ID, courseId);
        return sendRequest(request);
    }

    @Override
    public Message changePassword(String newPassword) {
        Message request = Message.createRequest(Constants.ACTION_CHANGE_PASSWORD);
        request.addData(Constants.KEY_PASSWORD, newPassword);
        return sendRequest(request);
    }

    @Override
    public Message getServerStatistics() {
        Message request = Message.createRequest(Constants.ACTION_GET_SERVER_STATISTICS);
        return sendRequest(request);
    }
}
