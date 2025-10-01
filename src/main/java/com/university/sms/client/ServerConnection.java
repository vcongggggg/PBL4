package com.university.sms.client;

import com.university.sms.common.Constants;
import com.university.sms.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối đến server
 */
public class ServerConnection {
    private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
    
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private boolean isConnected;
    
    private String serverHost;
    private int serverPort;
    
    // Callback interface for handling server responses
    public interface ResponseHandler {
        void onResponse(Message response);
        void onError(String error);
        void onDisconnected();
    }
    
    private ResponseHandler responseHandler;

    public ServerConnection(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
    }

    /**
     * Kết nối đến server
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            
            // Initialize streams
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            
            isConnected = true;
            
            LOGGER.info("Connected to server: " + serverHost + ":" + serverPort);
            
            // Start listening for server messages in background thread
            startMessageListener();
            
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error connecting to server", e);
            isConnected = false;
            return false;
        }
    }

    /**
     * Ngắt kết nối khỏi server
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
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
        
        LOGGER.info("Disconnected from server");
        
        // Notify handler about disconnection
        if (responseHandler != null) {
            responseHandler.onDisconnected();
        }
    }

    /**
     * Gửi yêu cầu đến server
     */
    public boolean sendRequest(Message request) {
        if (!isConnected || outputStream == null) {
            LOGGER.warning("Cannot send request: Not connected to server");
            return false;
        }

        try {
            outputStream.writeObject(request);
            outputStream.flush();
            
            LOGGER.info("Request sent: " + request.getAction());
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending request", e);
            handleConnectionError();
            return false;
        }
    }

    /**
     * Gửi yêu cầu và chờ phản hồi (synchronous)
     */
    public Message sendRequestAndWait(Message request, long timeoutSeconds) {
        if (!sendRequest(request)) {
            return Message.createErrorResponse(request.getAction(), "Failed to send request");
        }

        try {
            // Wait for response with timeout
            CompletableFuture<Message> future = new CompletableFuture<>();
            
            // Temporary handler for this request
            ResponseHandler originalHandler = responseHandler;
            responseHandler = new ResponseHandler() {
                @Override
                public void onResponse(Message response) {
                    if (response.getAction().equals(request.getAction())) {
                        future.complete(response);
                        responseHandler = originalHandler; // Restore original handler
                    } else if (originalHandler != null) {
                        originalHandler.onResponse(response);
                    }
                }

                @Override
                public void onError(String error) {
                    future.complete(Message.createErrorResponse(request.getAction(), error));
                    responseHandler = originalHandler;
                }

                @Override
                public void onDisconnected() {
                    future.complete(Message.createErrorResponse(request.getAction(), "Connection lost"));
                    responseHandler = originalHandler;
                }
            };
            
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error waiting for response", e);
            return Message.createErrorResponse(request.getAction(), "Timeout or error waiting for response");
        }
    }

    /**
     * Đăng nhập
     */
    public Message login(String username, String password) {
        Message request = Message.createRequest(Constants.ACTION_LOGIN);
        request.addData(Constants.KEY_USERNAME, username);
        request.addData(Constants.KEY_PASSWORD, password);
        
        return sendRequestAndWait(request, 60);
    }

    /**
     * Đăng xuất
     */
    public Message logout() {
        Message request = Message.createRequest(Constants.ACTION_LOGOUT);
        return sendRequestAndWait(request, 60);
    }

    /**
     * Lấy thông tin sinh viên
     */
    public Message getStudentInfo(Integer studentId) {
        Message request = Message.createRequest(Constants.ACTION_GET_STUDENT_INFO);
        if (studentId != null) {
            request.addData(Constants.KEY_STUDENT_ID, studentId);
        }
        
        return sendRequestAndWait(request, 60);
    }

    /**
     * Lấy tất cả sinh viên
     */
    public Message getAllStudents() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_STUDENTS);
        return sendRequestAndWait(request, 120); // Tăng lên 2 phút
    }

    /**
     * Tìm kiếm sinh viên
     */
    public Message searchStudents(String keyword) {
        Message request = Message.createRequest(Constants.ACTION_SEARCH_STUDENTS);
        request.addData(Constants.KEY_SEARCH_KEYWORD, keyword);
        
        return sendRequestAndWait(request, 60);
    }

    /**
     * Lấy tất cả khóa học
     */
    public Message getAllCourses() {
        Message request = Message.createRequest(Constants.ACTION_GET_ALL_COURSES);
        return sendRequestAndWait(request, 120);
    }

    /**
     * Lấy danh sách khóa học
     */
    public Message getCourses() {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSES);
        return sendRequestAndWait(request, 60);
    }

    /**
     * Lấy thông tin khóa học
     */
    public Message getCourseInfo(int courseId) {
        Message request = Message.createRequest(Constants.ACTION_GET_COURSE_INFO);
        request.addData(Constants.KEY_COURSE_ID, courseId);
        
        return sendRequestAndWait(request, 60);
    }

    /**
     * Đổi mật khẩu
     */
    public Message changePassword(String newPassword) {
        Message request = Message.createRequest(Constants.ACTION_CHANGE_PASSWORD);
        request.addData(Constants.KEY_PASSWORD, newPassword);
        
        return sendRequestAndWait(request, 60);
    }

    /**
     * Bắt đầu lắng nghe tin nhắn từ server
     */
    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            while (isConnected && !socket.isClosed()) {
                try {
                    Message message = (Message) inputStream.readObject();
                    
                    LOGGER.info("Received message: " + message.getType() + " - " + message.getAction());
                    
                    // Handle message based on type
                    if (responseHandler != null) {
                        if (message.getType() == Message.MessageType.RESPONSE) {
                            responseHandler.onResponse(message);
                        } else if (message.getType() == Message.MessageType.NOTIFICATION) {
                            // Handle notifications
                            responseHandler.onResponse(message);
                        }
                    }
                    
                } catch (SocketException e) {
                    LOGGER.info("Server connection closed");
                    break;
                } catch (EOFException e) {
                    LOGGER.info("Server disconnected");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Error reading message from server", e);
                    handleConnectionError();
                    break;
                }
            }
        });
        
        listenerThread.setDaemon(true);
        listenerThread.setName("ServerMessageListener");
        listenerThread.start();
    }

    /**
     * Xử lý lỗi kết nối
     */
    private void handleConnectionError() {
        isConnected = false;
        
        if (responseHandler != null) {
            responseHandler.onError("Connection error occurred");
        }
        
        // Try to reconnect after a delay
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                if (!isConnected) {
                    LOGGER.info("Attempting to reconnect...");
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    /**
     * Đặt response handler
     */
    public void setResponseHandler(ResponseHandler handler) {
        this.responseHandler = handler;
    }

    /**
     * Lấy thông tin server
     */
    public String getServerInfo() {
        return serverHost + ":" + serverPort;
    }

    /**
     * Test kết nối
     */
    public boolean testConnection() {
        try {
            Socket testSocket = new Socket(serverHost, serverPort);
            testSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
