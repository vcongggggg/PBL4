package com.university.sms.client.gui;

import com.formdev.flatlaf.FlatLightLaf;
import com.university.sms.client.ServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Giao diện đăng nhập
 */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverField;
    private JTextField portField;
    private JButton loginButton;
    private JButton connectButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    private ServerConnection serverConnection;
    private boolean isConnectedToServer = false;

    public LoginFrame() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
        setDefaultServerSettings();
    }

    private void initializeComponents() {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Hệ thống Quản lý Sinh viên - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Create components
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        serverField = new JTextField("localhost", 15);
        portField = new JTextField("8888", 8);
        
        loginButton = new JButton("Đăng nhập");
        connectButton = new JButton("Kết nối");
        
        statusLabel = new JLabel("Chưa kết nối đến server");
        statusLabel.setForeground(Color.RED);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        
        // Initially disable login button
        loginButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = new JLabel("HỆ THỐNG QUẢN LÝ SINH VIÊN");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Đăng nhập để tiếp tục");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        mainPanel.add(subtitleLabel, gbc);
        
        // Server connection panel
        JPanel serverPanel = createServerConnectionPanel();
        gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(serverPanel, gbc);
        
        // Login form panel
        JPanel loginPanel = createLoginPanel();
        gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(loginPanel, gbc);
        
        // Status and progress
        gbc.gridy = 4; gbc.gridwidth = 2;
        mainPanel.add(statusLabel, gbc);
        
        gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(progressBar, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout());
        footerPanel.setBackground(new Color(240, 240, 240));
        JLabel footerLabel = new JLabel("© 2024 Hệ thống Quản lý Sinh viên - Phiên bản 1.0");
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        footerLabel.setForeground(Color.GRAY);
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createServerConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Kết nối Server"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Server address
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Địa chỉ Server:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(serverField, gbc);
        
        // Port
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Port:"), gbc);
        
        gbc.gridx = 3;
        panel.add(portField, gbc);
        
        // Connect button
        gbc.gridx = 4;
        panel.add(connectButton, gbc);
        
        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin đăng nhập"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(passwordField, gbc);
        
        // Login button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0;
        loginButton.setPreferredSize(new Dimension(120, 35));
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(loginButton, gbc);
        
        return panel;
    }

    private void setupEventListeners() {
        // Connect button
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        
        // Login button
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        // Enter key listeners
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (isConnectedToServer) {
                        performLogin();
                    } else {
                        connectToServer();
                    }
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
        serverField.addKeyListener(enterKeyListener);
        portField.addKeyListener(enterKeyListener);
    }

    private void setDefaultServerSettings() {
        serverField.setText(Constants.DEFAULT_SERVER_HOST);
        portField.setText(String.valueOf(Constants.DEFAULT_SERVER_PORT));
    }

    private void connectToServer() {
        String host = serverField.getText().trim();
        String portText = portField.getText().trim();
        
        if (host.isEmpty() || portText.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin server", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int port = Integer.parseInt(portText);
            
            // Show progress
            showProgress("Đang kết nối đến server...");
            connectButton.setEnabled(false);
            
            // Connect in background thread
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    serverConnection = new ServerConnection(host, port);
                    return serverConnection.connect();
                }
                
                @Override
                protected void done() {
                    hideProgress();
                    connectButton.setEnabled(true);
                    
                    try {
                        boolean connected = get();
                        if (connected) {
                            onServerConnected();
                        } else {
                            onServerConnectionFailed();
                        }
                    } catch (Exception e) {
                        onServerConnectionFailed();
                    }
                }
            };
            
            worker.execute();
            
        } catch (NumberFormatException e) {
            showMessage("Port không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onServerConnected() {
        isConnectedToServer = true;
        statusLabel.setText("Đã kết nối đến server");
        statusLabel.setForeground(new Color(0, 150, 0));
        loginButton.setEnabled(true);
        connectButton.setText("Ngắt kết nối");
        
        // Set up response handler
        serverConnection.setResponseHandler(new ServerConnection.ResponseHandler() {
            @Override
            public void onResponse(Message response) {
                // Handle responses if needed
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    showMessage("Lỗi kết nối: " + error, "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    onServerDisconnected();
                });
            }
        });
        
        // Focus on username field
        usernameField.requestFocus();
    }

    private void onServerConnectionFailed() {
        isConnectedToServer = false;
        statusLabel.setText("Không thể kết nối đến server");
        statusLabel.setForeground(Color.RED);
        loginButton.setEnabled(false);
        connectButton.setText("Kết nối");
        
        showMessage("Không thể kết nối đến server. Vui lòng kiểm tra lại địa chỉ và port.", 
                   "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
    }

    private void onServerDisconnected() {
        isConnectedToServer = false;
        statusLabel.setText("Mất kết nối đến server");
        statusLabel.setForeground(Color.RED);
        loginButton.setEnabled(false);
        connectButton.setText("Kết nối");
        connectButton.setEnabled(true);
        
        showMessage("Mất kết nối đến server", "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin đăng nhập", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Show progress
        showProgress("Đang đăng nhập...");
        loginButton.setEnabled(false);
        
        // Login in background thread
        SwingWorker<Message, Void> worker = new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() throws Exception {
                return serverConnection.login(username, password);
            }
            
            @Override
            protected void done() {
                hideProgress();
                loginButton.setEnabled(true);
                
                try {
                    Message response = get();
                    if (response.isSuccess()) {
                        onLoginSuccess(response);
                    } else {
                        onLoginFailed(response.getMessage());
                    }
                } catch (Exception e) {
                    onLoginFailed("Lỗi khi đăng nhập: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }

    private void onLoginSuccess(Message response) {
        User user = (User) response.getData(Constants.KEY_USER);
        
        if (user != null) {
            // Hide login window
            setVisible(false);
            
            // Open main application window
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(user, serverConnection);
                mainFrame.setVisible(true);
                
                // Dispose login window
                dispose();
            });
        } else {
            showMessage("Lỗi: Không nhận được thông tin người dùng", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoginFailed(String message) {
        showMessage("Đăng nhập thất bại: " + message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
        usernameField.requestFocus();
    }

    private void showProgress(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.BLUE);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
    }

    private void hideProgress() {
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}


