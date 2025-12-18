package com.university.sms.server;

import com.university.sms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Scanner;
import java.util.logging.Level;
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
                LOGGER.log(Level.SEVERE, "Server error", e);
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

        // Print database version
        int dbVersion = getServerDatabaseVersion();
        System.out.println("Database Version: " + dbVersion);

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
     * Get server database version
     */
    private static int getServerDatabaseVersion() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT MAX(UNIX_TIMESTAMP(updated_at)) as last_update FROM data_origin";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                long ts = rs.getLong("last_update");
                if (!rs.wasNull() && ts > 0) {
                    return (int) ts;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error getting server database version from data_origin: " + e.getMessage());
        }
        return 0;
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
                case "dbversion":
                case "version":
                    showDatabaseVersion();
                    break;
                case "csvversion":
                    showCSVVersion();
                    break;
                case "postgresversion":
                case "pgversion":
                    showPostgresVersion();
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
        System.out.println("Database Version: " + getServerDatabaseVersion());
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
        System.out.println("dbversion - Show database version");
        System.out.println("csvversion - Show CSV source version");
        System.out.println("postgresversion - Show PostgreSQL source version");
        System.out.println("clear     - Clear the console screen");
        System.out.println("stop      - Stop the server and exit");
        System.out.println("help      - Show this help message");
        System.out.println();
    }

    /**
     * Show database version
     */
    private static void showDatabaseVersion() {
        System.out.println("\n--- Database Version ---");
        int dbVersion = getServerDatabaseVersion();
        System.out.println("Database Version: " + dbVersion);
        System.out.println();
    }

    /**
     * Show CSV source version
     */
    private static void showCSVVersion() {
        System.out.println("\n--- CSV Source Version ---");
        try {
            int csvVersion = getCSVSourceVersion();
            int csvCount = getCSVSourceCount();
            // Format version tối đa 10 chữ số
            String versionStr = String.valueOf(csvVersion);
            if (versionStr.length() > 10) {
                versionStr = versionStr.substring(0, 10);
            }
            System.out.println("CSV Source Version: " + versionStr);
            System.out.println("CSV Source Records: " + csvCount);
        } catch (Exception e) {
            System.out.println("Error getting CSV version: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Get CSV source version from database
     */
    private static int getCSVSourceVersion() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            // Lấy timestamp của lần thay đổi cuối cùng từ data_origin cho source CSV
            // UNIX_TIMESTAMP() trả về số giây (seconds since epoch), không cần chia cho
            // 1000
            String sql = "SELECT MAX(UNIX_TIMESTAMP(updated_at)) as last_update FROM data_origin WHERE source = 'CSV'";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                long timestamp = rs.getLong("last_update");
                if (!rs.wasNull() && timestamp > 0) {
                    // UNIX_TIMESTAMP() đã trả về giây, không cần chia cho 1000
                    // Chỉ cast về int (có thể mất precision nếu > Integer.MAX_VALUE)
                    return (int) timestamp;
                }
            }

            // Fallback: đếm số records nếu không có timestamp
            return getCSVSourceCount();
        } catch (Exception e) {
            LOGGER.warning("Error getting CSV source version: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get CSV source record count
     */
    private static int getCSVSourceCount() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM data_origin WHERE source = 'CSV'";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            LOGGER.warning("Error getting CSV source count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Show PostgreSQL source version
     */
    private static void showPostgresVersion() {
        System.out.println("\n--- PostgreSQL Source Version ---");
        try {
            int postgresVersion = getPostgresSourceVersion();
            int postgresCount = getPostgresSourceCount();
            // Format version tối đa 10 chữ số
            String versionStr = String.valueOf(postgresVersion);
            if (versionStr.length() > 10) {
                versionStr = versionStr.substring(0, 10);
            }
            System.out.println("PostgreSQL Source Version: " + versionStr);
            System.out.println("PostgreSQL Source Records: " + postgresCount);
        } catch (Exception e) {
            System.out.println("Error getting PostgreSQL version: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Get PostgreSQL source version from database
     */
    private static int getPostgresSourceVersion() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            // Thử lấy từ system_config trước
            String sql = "SELECT CAST(config_value AS UNSIGNED) as version FROM system_config WHERE config_key = 'postgres_version'";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                int version = rs.getInt("version");
                if (!rs.wasNull() && version > 0) {
                    return version;
                }
            }

            // Fallback: lấy timestamp từ data_origin
            sql = "SELECT MAX(UNIX_TIMESTAMP(updated_at)) as last_update FROM data_origin WHERE source = 'POSTGRES'";
            rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                long timestamp = rs.getLong("last_update");
                if (!rs.wasNull() && timestamp > 0) {
                    return (int) timestamp;
                }
            }

            // Fallback: đếm số records nếu không có timestamp
            return getPostgresSourceCount();
        } catch (Exception e) {
            LOGGER.warning("Error getting PostgreSQL source version: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get PostgreSQL source record count
     */
    private static int getPostgresSourceCount() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM data_origin WHERE source = 'POSTGRES'";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            LOGGER.warning("Error getting PostgreSQL source count: " + e.getMessage());
        }
        return 0;
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
