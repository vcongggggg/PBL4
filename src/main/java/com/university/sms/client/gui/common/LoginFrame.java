package com.university.sms.client.gui.common;

import com.formdev.flatlaf.FlatDarkLaf;
import com.university.sms.client.IServerConnection;
import com.university.sms.client.ServerConnection;
import com.university.sms.csvclient.CSVServerConnection;
import com.university.sms.common.Constants;
import com.university.sms.common.Message;
import com.university.sms.model.User;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

/**
 * Modern Dark Theme Login Frame - giữ structure cũ
 */
public class LoginFrame extends JFrame {
    // Factory pattern
    public interface ConnectionFactory {
        IServerConnection create(String host, int port);
    }

    public static class RegularConnectionFactory implements ConnectionFactory {
        @Override
        public IServerConnection create(String host, int port) {
            return new ServerConnection(host, port);
        }
    }

    public static class CsvConnectionFactory implements ConnectionFactory {
        @Override
        public IServerConnection create(String host, int port) {
            return new CSVServerConnection(host, port);
        }
    }

    private static final long serialVersionUID = 1L;

    // Components - giữ như cũ
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverField;
    private JTextField portField;
    private JButton loginButton;
    private JButton connectButton;
    private JButton togglePasswordButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JLabel clientTypeLabel;

    private IServerConnection serverConnection;
    private ConnectionFactory connectionFactory;
    private boolean isConnectedToServer = false;
    private boolean isPasswordVisible = false;

    // Dark theme colors
    private static final Color BG_DARK = new Color(66, 71, 85);
    private static final Color CARD_BG = new Color(82, 88, 102);
    private static final Color INPUT_BG = new Color(95, 102, 117);
    private static final Color INPUT_BORDER = new Color(115, 122, 137);
    private static final Color TEXT_PRIMARY = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(170, 178, 195);
    private static final Color ACCENT_GREEN = new Color(82, 196, 136);
    private static final Color ACCENT_GREEN_HOVER = new Color(72, 176, 116);
    private static final Color SUCCESS = new Color(82, 196, 136);
    private static final Color ERROR = new Color(235, 87, 87);
    private static final Color ACCENT_BLUE = new Color(96, 165, 250);

    public LoginFrame() {
        this(new RegularConnectionFactory());
    }

    public LoginFrame(ConnectionFactory factory) {
        this.connectionFactory = factory;
        initLookAndFeel();
        initializeComponents();
        setupLayout();
        setupEventListeners();
        setDefaultServerSettings();
    }

    private void initLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeComponents() {
        setTitle("Hệ Thống Đồng Bộ Dữ Liệu Giữa Nhiều Máy Trong Mạng — Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Text fields with dark theme
        usernameField = createStyledTextField(20);
        passwordField = createStyledPasswordField(20);
        serverField = createStyledTextField(15);
        serverField.setText("localhost");
        portField = createStyledTextField(6);
        portField.setText("8888");

        // Buttons
        loginButton = createGreenButton("ĐĂNG NHẬP");
        loginButton.setPreferredSize(new Dimension(160, 45));
        
        connectButton = createGreenButton("Kết nối");
        connectButton.setPreferredSize(new Dimension(120, 38));
        
        togglePasswordButton = createIconButton("");
        togglePasswordButton.setPreferredSize(new Dimension(44, 36));
        togglePasswordButton.setToolTipText("Hiện/Ẩn mật khẩu");
        loadIconForButton(togglePasswordButton, "/icons/eye-close.png", 20, 20);

        // Status
        statusLabel = new JLabel();
        statusLabel.setForeground(ERROR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setStatusWithIcon(statusLabel, "/icons/cross.png", "Chưa kết nối đến server", ERROR);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(400, 6));
        progressBar.setBorderPainted(false);
        progressBar.setForeground(ACCENT_GREEN);
        progressBar.setBackground(INPUT_BG);

        // Client type
        String clientType = (connectionFactory instanceof CsvConnectionFactory ? "CSV" : "Regular");
        clientTypeLabel = new JLabel();
        loadIconLabel(clientTypeLabel, "/icons/client.png", "Loại client: " + clientType, 14);
        clientTypeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clientTypeLabel.setForeground(TEXT_SECONDARY);

        // Initially disable login
        loginButton.setEnabled(false);
    }

    private JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(INPUT_BORDER, 8),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 40));
        
        // Focus effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(ACCENT_BLUE, 8, 2),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(INPUT_BORDER, 8),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }

    private JPasswordField createStyledPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(INPUT_BORDER, 8),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 40));
        field.setEchoChar('•');
        
        // Focus effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(ACCENT_BLUE, 8, 2),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(INPUT_BORDER, 8),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }

    private JButton createGreenButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (!isEnabled()) {
                    g2.setColor(INPUT_BG);
                } else if (getModel().isPressed()) {
                    g2.setColor(ACCENT_GREEN_HOVER.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(ACCENT_GREEN_HOVER);
                } else {
                    g2.setColor(ACCENT_GREEN);
                }
                
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }

    private JButton createIconButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setForeground(TEXT_PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        
        return button;
    }

    private void loadIconForButton(JButton button, String iconPath, int width, int height) {
        try {
            InputStream is = getClass().getResourceAsStream(iconPath);
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaled));
                button.setText("");
            }
        } catch (IOException e) {
            System.err.println("Cannot load icon: " + iconPath);
            button.setText("👁");
        }
    }

    private void setupLayout() {
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // Root panel
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // Header
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        root.add(createHeaderPanel(), gbc);

        // Cards container
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        
        JPanel serverCard = createRoundedCard(createServerConnectionContent(), 560);
        cards.add(serverCard);
        cards.add(Box.createVerticalStrut(12));
        
        JPanel loginCard = createRoundedCard(createLoginContent(), 560);
        cards.add(loginCard);
        
        root.add(cards, gbc);

        // Status
        gbc.gridy = 2;
        gbc.insets = new Insets(6, 0, 6, 0);
        JPanel statusPanel = new JPanel(new BorderLayout(10, 10));
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        
        JPanel pbWrap = new JPanel();
        pbWrap.setOpaque(false);
        pbWrap.add(progressBar);
        statusPanel.add(pbWrap, BorderLayout.CENTER);
        root.add(statusPanel, gbc);

        // Client info
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 0, 0);
        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER));
        info.setOpaque(false);
        info.add(clientTypeLabel);
        root.add(info, gbc);

        add(root, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(640, 750));
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                
                // Gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(82, 88, 102),
                    0, h, new Color(66, 71, 85)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.dispose();
            }
        };
        
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        header.setPreferredSize(new Dimension(560, 140));

        // Logo
        JLabel logo = createLogoLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(logo);
        header.add(Box.createVerticalStrut(12));

        // Title
        JLabel title = new JLabel("HỆ THỐNG ĐỒNG BỘ DỮ LIỆU");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("Đăng nhập để tiếp tục");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(sub);

        return header;
    }

    private JLabel createLogoLabel() {
        JLabel logo = new JLabel();
        try {
            InputStream is = getClass().getResourceAsStream("/icons/logo.png");
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                Image scaled = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(scaled));
            } else {
                logo.setText("🎓");
                logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                logo.setForeground(ACCENT_GREEN);
            }
        } catch (IOException e) {
            logo.setText("🎓");
            logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
            logo.setForeground(ACCENT_GREEN);
        }
        return logo;
    }

    private JPanel createRoundedCard(JPanel content, int width) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                for (int i = 0; i < 5; i++) {
                    g2.setColor(new Color(0, 0, 0, 15 - i * 3));
                    g2.fillRoundRect(4 + i, 4 + i, getWidth() - 8 - i * 2, 
                        getHeight() - 8 - i * 2, 12, 12);
                }

                // Card background
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 12, 12);
                g2.dispose();
            }
        };
        
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        card.add(content, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        
        return card;
    }

    private JPanel createServerConnectionContent() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        JLabel t = new JLabel();
        loadIconLabel(t, "/icons/server.png", "Kết nối Server", 18);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TEXT_PRIMARY);
        p.add(t, gbc);

        // Server field
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel serverLabel = new JLabel("Địa chỉ:");
        serverLabel.setForeground(TEXT_PRIMARY);
        p.add(serverLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        serverField.setPreferredSize(new Dimension(260, 36));
        p.add(serverField, gbc);

        // Port
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 18, 6, 6);
        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(TEXT_PRIMARY);
        p.add(portLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 6, 6, 6);
        portField.setPreferredSize(new Dimension(120, 36));
        p.add(portField, gbc);

        // Connect button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 0;
        gbc.insets = new Insets(14, 6, 6, 6);
        JPanel cb = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cb.setOpaque(false);
        cb.add(connectButton);
        p.add(cb, gbc);

        return p;
    }

    private JPanel createLoginContent() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel t = new JLabel();
        loadIconLabel(t, "/icons/login.png", "Thông tin đăng nhập", 18);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TEXT_PRIMARY);
        p.add(t, gbc);

        // Username label with icon
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JLabel uLabel = new JLabel();
        loadIconLabel(uLabel, "/icons/user.png", "Tên đăng nhập", 14);
        uLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        uLabel.setForeground(TEXT_PRIMARY);
        p.add(uLabel, gbc);

        gbc.gridy = 2;
        usernameField.setPreferredSize(new Dimension(400, 36));
        p.add(usernameField, gbc);

        // Password label with icon
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 6, 6);
        JLabel pwLabel = new JLabel();
        loadIconLabel(pwLabel, "/icons/password.png", "Mật khẩu", 14);
        pwLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pwLabel.setForeground(TEXT_PRIMARY);
        p.add(pwLabel, gbc);

        // Password field
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        passwordField.setPreferredSize(new Dimension(340, 36));
        p.add(passwordField, gbc);

        // Toggle button
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 8, 6, 6);
        p.add(togglePasswordButton, gbc);

        // Login button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 6, 8, 6);
        JPanel lb = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lb.setOpaque(false);
        lb.add(loginButton);
        p.add(lb, gbc);

        return p;
    }

    private void loadIconLabel(JLabel label, String iconPath, String text, int iconSize) {
        try {
            InputStream is = getClass().getResourceAsStream(iconPath);
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                // Tint icon to TEXT_SECONDARY color
                Image scaled = icon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaled));
                label.setText(text);
                label.setIconTextGap(8);
            } else {
                label.setText("▸ " + text);
            }
        } catch (IOException e) {
            label.setText("▸ " + text);
        }
    }

    private void setStatusWithIcon(JLabel label, String iconPath, String text, Color color) {
        try {
            InputStream is = getClass().getResourceAsStream(iconPath);
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaled));
                label.setText(text);
                label.setIconTextGap(8);
            } else {
                // Fallback to text symbols
                String symbol = iconPath.contains("check") ? "✓" : "✗";
                label.setText(symbol + " " + text);
                label.setIcon(null);
            }
            label.setForeground(color);
        } catch (IOException e) {
            // Fallback to text symbols
            String symbol = iconPath.contains("check") ? "✓" : "✗";
            label.setText(symbol + " " + text);
            label.setIcon(null);
            label.setForeground(color);
        }
    }

    private void setupEventListeners() {
        connectButton.addActionListener(e -> connectToServer());
        loginButton.addActionListener(e -> performLogin());
        
        togglePasswordButton.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordField.setEchoChar((char) 0);
                // Change to eye-open icon (hiện mật khẩu)
                loadIconForButton(togglePasswordButton, "/icons/eye-open.png", 20, 20);
            } else {
                passwordField.setEchoChar('•');
                // Change to eye-close icon (ẩn mật khẩu)
                loadIconForButton(togglePasswordButton, "/icons/eye-close.png", 20, 20);
            }
        });

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
            showProgress("Đang kết nối đến server...");
            connectButton.setEnabled(false);

            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    serverConnection = connectionFactory.create(host, port);
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
        setStatusWithIcon(statusLabel, "/icons/check.png", "Đã kết nối đến server", SUCCESS);
        loginButton.setEnabled(true);
        connectButton.setText("Ngắt kết nối");

        serverConnection.setResponseHandler(new IServerConnection.ResponseHandler() {
            @Override
            public void onResponse(Message response) {}

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

        usernameField.requestFocus();
    }

    private void onServerConnectionFailed() {
        isConnectedToServer = false;
        setStatusWithIcon(statusLabel, "/icons/cross.png", "Không thể kết nối đến server", ERROR);
        loginButton.setEnabled(false);
        connectButton.setText("Kết nối");

        showMessage("Không thể kết nối đến server. Vui lòng kiểm tra lại địa chỉ và port.",
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
    }

    private void onServerDisconnected() {
        isConnectedToServer = false;
        setStatusWithIcon(statusLabel, "/icons/cross.png", "Mất kết nối đến server", ERROR);
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

        showProgress("Đang đăng nhập...");
        loginButton.setEnabled(false);

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
            setVisible(false);
            SwingUtilities.invokeLater(() -> {
                switch (user.getRole()) {
                    case ADMIN:
                        if (serverConnection instanceof CSVServerConnection) {
                            uploadCSVDataForAdmin((CSVServerConnection) serverConnection, user);
                        } else {
                            new com.university.sms.client.gui.admin.AdminMainFrame(user, serverConnection).setVisible(true);
                        }
                        break;
                    case TEACHER:
                        new com.university.sms.client.gui.teacher.TeacherMainFrame(user, serverConnection).setVisible(true);
                        break;
                    case STUDENT:
                        new com.university.sms.client.gui.student.StudentMainFrame(user, serverConnection).setVisible(true);
                        break;
                }
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
        statusLabel.setText("⟳ " + message);
        statusLabel.setForeground(TEXT_SECONDARY);
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

    private void uploadCSVDataForAdmin(CSVServerConnection csvConnection, User admin) {
        JOptionPane.showMessageDialog(this, 
            "CSV upload functionality",
            "CSV Mode",
            JOptionPane.INFORMATION_MESSAGE);
        
        new com.university.sms.client.gui.admin.AdminMainFrame(admin, csvConnection).setVisible(true);
    }

    // Custom rounded border
    static class RoundedBorder extends AbstractBorder {
        private Color color;
        private int radius;
        private int thickness;

        RoundedBorder(Color color, int radius) {
            this(color, radius, 1);
        }

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 1, thickness + 1, thickness + 1, thickness + 1);
        }
    }
}
