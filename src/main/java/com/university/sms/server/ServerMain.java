package com.university.sms.server;

import com.university.sms.util.DatabaseConnection;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Main class để khởi động server
 */
public class ServerMain {
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    private static StudentManagementServer server;
    private static Scanner scanner;

    public static void main(String[] args) {
        LOGGER.info("Starting Student Management System Server...");
        
        // Initialize scanner for console commands
        scanner = new Scanner(System.in);
        
        // Parse command line arguments
        int port = parsePort(args);
        
        // Create and start server
        server = new StudentManagementServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received");
            if (server != null) {
                server.stop();
            }
        }));
        
        // Start server in separate thread
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                LOGGER.severe("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        serverThread.start();
        
        // Wait a moment for server to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Print server info
        printServerInfo();
        
        // Console command loop
        handleConsoleCommands();
    }

    /**
     * Parse port from command line arguments
     */
    private static int parsePort(String[] args) {
        if (args.length > 0) {
            try {
                int port = Integer.parseInt(args[0]);
                if (port > 0 && port <= 65535) {
                    LOGGER.info("Using port from command line: " + port);
                    return port;
                } else {
                    LOGGER.warning("Invalid port number. Using default port 8888");
                }
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid port format. Using default port 8888");
            }
        }
        return 8888;
    }

    /**
     * Print server information
     */
    private static void printServerInfo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("   STUDENT MANAGEMENT SYSTEM SERVER");
        System.out.println("=".repeat(60));
        System.out.println("Server Status: " + (server.isRunning() ? "RUNNING" : "STOPPED"));
        System.out.println("Port: " + server.getPort());
        System.out.println("Database Status: " + (DatabaseConnection.testConnection() ? "CONNECTED" : "DISCONNECTED"));
        System.out.println("Connected Clients: " + server.getConnectedClientCount());
        System.out.println("=".repeat(60));
        System.out.println("\nAvailable Commands:");
        System.out.println("  status    - Show server status");
        System.out.println("  clients   - Show connected clients");
        System.out.println("  stats     - Show detailed statistics");
        System.out.println("  broadcast - Send message to all clients");
        System.out.println("  stop      - Stop the server");
        System.out.println("  help      - Show this help message");
        System.out.println("=".repeat(60));
        System.out.println("\nServer is ready. Type 'help' for commands or 'stop' to shutdown.\n");
    }

    /**
     * Handle console commands
     */
    private static void handleConsoleCommands() {
        String command;
        
        while (server.isRunning()) {
            System.out.print("SMS-Server> ");
            command = scanner.nextLine().trim().toLowerCase();
            
            switch (command) {
                case "status":
                    showStatus();
                    break;
                case "clients":
                    showClients();
                    break;
                case "stats":
                    showStatistics();
                    break;
                case "broadcast":
                    handleBroadcast();
                    break;
                case "stop":
                case "quit":
                case "exit":
                    stopServer();
                    return;
                case "help":
                    showHelp();
                    break;
                case "clear":
                    clearScreen();
                    break;
                case "db":
                    testDatabase();
                    break;
                default:
                    if (!command.isEmpty()) {
                        System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
                    }
                    break;
            }
        }
    }

    /**
     * Show server status
     */
    private static void showStatus() {
        System.out.println("\n--- Server Status ---");
        System.out.println("Running: " + server.isRunning());
        System.out.println("Port: " + server.getPort());
        System.out.println("Connected Clients: " + server.getConnectedClientCount());
        System.out.println("Database: " + (DatabaseConnection.testConnection() ? "Connected" : "Disconnected"));
        System.out.println("Memory Usage: " + getMemoryUsage());
        System.out.println();
    }

    /**
     * Show connected clients
     */
    private static void showClients() {
        System.out.println("\n--- Connected Clients ---");
        String[] clients = server.getConnectedClientInfo();
        
        if (clients.length == 0) {
            System.out.println("No clients connected.");
        } else {
            for (int i = 0; i < clients.length; i++) {
                System.out.println((i + 1) + ". " + clients[i]);
            }
        }
        System.out.println();
    }

    /**
     * Show detailed statistics
     */
    private static void showStatistics() {
        System.out.println("\n--- Server Statistics ---");
        StudentManagementServer.ServerStatistics stats = server.getStatistics();
        
        System.out.println("Total Clients: " + stats.getConnectedClients());
        System.out.println("  - Admin Clients: " + stats.getAdminClients());
        System.out.println("  - Teacher Clients: " + stats.getTeacherClients());
        System.out.println("  - Student Clients: " + stats.getStudentClients());
        System.out.println("Server Port: " + stats.getPort());
        System.out.println("Server Status: " + (stats.isRunning() ? "Running" : "Stopped"));
        System.out.println("JVM Memory: " + getMemoryUsage());
        System.out.println();
    }

    /**
     * Handle broadcast message
     */
    private static void handleBroadcast() {
        System.out.print("Enter message to broadcast: ");
        String message = scanner.nextLine();
        
        if (!message.trim().isEmpty()) {
            server.broadcastMessage(message);
            System.out.println("Message broadcasted to all clients.");
        } else {
            System.out.println("Message cannot be empty.");
        }
        System.out.println();
    }

    /**
     * Stop server
     */
    private static void stopServer() {
        System.out.println("\nShutting down server...");
        server.stop();
        System.out.println("Server stopped successfully.");
        scanner.close();
    }

    /**
     * Show help
     */
    private static void showHelp() {
        System.out.println("\n--- Available Commands ---");
        System.out.println("status    - Show current server status");
        System.out.println("clients   - List all connected clients");
        System.out.println("stats     - Show detailed server statistics");
        System.out.println("broadcast - Send a message to all connected clients");
        System.out.println("db        - Test database connection");
        System.out.println("clear     - Clear the console screen");
        System.out.println("stop      - Stop the server and exit");
        System.out.println("help      - Show this help message");
        System.out.println();
    }

    /**
     * Clear console screen
     */
    private static void clearScreen() {
        // Simple clear screen implementation
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
        printServerInfo();
    }

    /**
     * Test database connection
     */
    private static void testDatabase() {
        System.out.println("\nTesting database connection...");
        
        try {
            boolean connected = DatabaseConnection.testConnection();
            if (connected) {
                System.out.println("✓ Database connection successful");
                DatabaseConnection.printDatabaseInfo();
            } else {
                System.out.println("✗ Database connection failed");
            }
        } catch (Exception e) {
            System.out.println("✗ Database connection error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Get memory usage information
     */
    private static String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format("%.2f MB / %.2f MB", 
            usedMemory / 1024.0 / 1024.0, 
            totalMemory / 1024.0 / 1024.0);
    }
}


