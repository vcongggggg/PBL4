package com.university.sms.server;

import com.university.sms.util.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server chính của hệ thống quản lý sinh viên
 */
public class StudentManagementServer {
    private static final Logger LOGGER = Logger.getLogger(StudentManagementServer.class.getName());
    
    private static final int DEFAULT_PORT = 8888;
    private static final int MAX_THREADS = 100;
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean isRunning;
    private int port;
    
    // Quản lý các client đang kết nối
    private ConcurrentHashMap<String, ClientHandler> connectedClients;

    public StudentManagementServer() {
        this(DEFAULT_PORT);
    }

    public StudentManagementServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.connectedClients = new ConcurrentHashMap<>();
        this.isRunning = false;
    }

    /**
     * Khởi động server
     */
    public void start() {
        try {
            // Test database connection first
            if (!DatabaseConnection.testConnection()) {
                LOGGER.severe("Cannot connect to database. Server startup aborted.");
                return;
            }
            
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            LOGGER.info("Student Management Server started on port " + port);
            LOGGER.info("Waiting for client connections...");
            
            // Accept client connections
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Create client handler
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    String clientId = clientSocket.getRemoteSocketAddress().toString();
                    
                    // Store client handler
                    connectedClients.put(clientId, clientHandler);
                    
                    // Handle client in separate thread
                    threadPool.execute(() -> {
                        try {
                            clientHandler.run();
                        } finally {
                            // Remove client when disconnected
                            connectedClients.remove(clientId);
                            LOGGER.info("Client removed: " + clientId);
                        }
                    });
                    
                    LOGGER.info("New client connected: " + clientId + 
                               " (Total clients: " + connectedClients.size() + ")");
                    
                } catch (IOException e) {
                    if (isRunning) {
                        LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                    }
                }
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting server", e);
        } finally {
            stop();
        }
    }

    /**
     * Dừng server
     */
    public void stop() {
        LOGGER.info("Stopping Student Management Server...");
        
        isRunning = false;
        
        // Disconnect all clients
        connectedClients.values().forEach(ClientHandler::disconnect);
        connectedClients.clear();
        
        // Shutdown thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
        
        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing server socket", e);
            }
        }
        
        LOGGER.info("Student Management Server stopped");
    }

    /**
     * Lấy số lượng client đang kết nối
     */
    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    /**
     * Lấy thông tin các client đang kết nối
     */
    public String[] getConnectedClientInfo() {
        return connectedClients.entrySet().stream()
            .map(entry -> {
                String clientId = entry.getKey();
                ClientHandler handler = entry.getValue();
                String username = handler.getCurrentUser() != null ? 
                    handler.getCurrentUser().getUsername() : "Anonymous";
                return clientId + " (" + username + ")";
            })
            .toArray(String[]::new);
    }

    /**
     * Kiểm tra xem server có đang chạy không
     */
    public boolean isRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * Lấy port server
     */
    public int getPort() {
        return port;
    }

    /**
     * Broadcast message to all connected clients
     */
    public void broadcastMessage(String message) {
        LOGGER.info("Broadcasting message to " + connectedClients.size() + " clients: " + message);
        
        connectedClients.values().forEach(client -> {
            try {
                // Implementation for broadcasting will be added if needed
                // client.sendNotification(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error broadcasting message to client", e);
            }
        });
    }

    /**
     * Get server statistics
     */
    public ServerStatistics getStatistics() {
        ServerStatistics stats = new ServerStatistics();
        stats.setConnectedClients(connectedClients.size());
        stats.setIsRunning(isRunning);
        stats.setPort(port);
        stats.setUptime(System.currentTimeMillis()); // Simple implementation
        
        // Count clients by role
        long adminCount = connectedClients.values().stream()
            .filter(c -> c.getCurrentUser() != null && 
                        c.getCurrentUser().getRole().name().equals("ADMIN"))
            .count();
        stats.setAdminClients((int) adminCount);
        
        long teacherCount = connectedClients.values().stream()
            .filter(c -> c.getCurrentUser() != null && 
                        c.getCurrentUser().getRole().name().equals("TEACHER"))
            .count();
        stats.setTeacherClients((int) teacherCount);
        
        long studentCount = connectedClients.values().stream()
            .filter(c -> c.getCurrentUser() != null && 
                        c.getCurrentUser().getRole().name().equals("STUDENT"))
            .count();
        stats.setStudentClients((int) studentCount);
        
        return stats;
    }

    /**
     * Shutdown hook để đảm bảo server được đóng đúng cách
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered");
            stop();
        }));
    }

    /**
     * Inner class for server statistics
     */
    public static class ServerStatistics {
        private int connectedClients;
        private boolean isRunning;
        private int port;
        private long uptime;
        private int adminClients;
        private int teacherClients;
        private int studentClients;

        // Getters and setters
        public int getConnectedClients() { return connectedClients; }
        public void setConnectedClients(int connectedClients) { this.connectedClients = connectedClients; }
        
        public boolean isRunning() { return isRunning; }
        public void setIsRunning(boolean running) { isRunning = running; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        
        public int getAdminClients() { return adminClients; }
        public void setAdminClients(int adminClients) { this.adminClients = adminClients; }
        
        public int getTeacherClients() { return teacherClients; }
        public void setTeacherClients(int teacherClients) { this.teacherClients = teacherClients; }
        
        public int getStudentClients() { return studentClients; }
        public void setStudentClients(int studentClients) { this.studentClients = studentClients; }
    }
}


