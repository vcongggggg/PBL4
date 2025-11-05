package com.university.sms.client.gui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Dark Mode Toggle Switch
 * Modern animated toggle button
 */
public class DarkModeToggle extends JComponent {
    private static final long serialVersionUID = 1L;
    
    private boolean isDarkMode = false;
    private float animationProgress = 0.0f;
    private Timer animationTimer;
    
    private static final int WIDTH = 60;
    private static final int HEIGHT = 30;
    private static final int CIRCLE_SIZE = 24;
    private static final int ANIMATION_DURATION = 200; // ms
    private static final int ANIMATION_STEPS = 20;
    
    private Color lightBgColor = new Color(220, 220, 220);
    private Color darkBgColor = new Color(52, 152, 219);
    private Color circleColor = Color.WHITE;
    
    public DarkModeToggle() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        
        // Initialize from ThemeManager
        isDarkMode = ThemeManager.getInstance().isDarkMode();
        animationProgress = isDarkMode ? 1.0f : 0.0f;
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });
    }
    
    public void toggle() {
        isDarkMode = !isDarkMode;
        animate();
        
        // Apply theme change
        ThemeManager.getInstance().toggleTheme();
    }
    
    private void animate() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        final float targetProgress = isDarkMode ? 1.0f : 0.0f;
        final float startProgress = animationProgress;
        final float delta = targetProgress - startProgress;
        final int stepDelay = ANIMATION_DURATION / ANIMATION_STEPS;
        
        animationTimer = new Timer(stepDelay, null);
        animationTimer.addActionListener(e -> {
            animationProgress += delta / ANIMATION_STEPS;
            
            if ((delta > 0 && animationProgress >= targetProgress) ||
                (delta < 0 && animationProgress <= targetProgress)) {
                animationProgress = targetProgress;
                animationTimer.stop();
            }
            
            repaint();
        });
        
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Interpolate background color
        Color bgColor = interpolateColor(lightBgColor, darkBgColor, animationProgress);
        
        // Draw background
        g2d.setColor(bgColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));
        
        // Draw circle
        float circleX = (width - CIRCLE_SIZE - 6) * animationProgress + 3;
        float circleY = (height - CIRCLE_SIZE) / 2.0f;
        
        g2d.setColor(circleColor);
        g2d.fillOval((int) circleX, (int) circleY, CIRCLE_SIZE, CIRCLE_SIZE);
        
        // Draw icon
        g2d.setColor(bgColor);
        String icon = isDarkMode ? "🌙" : "☀️";
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        FontMetrics fm = g2d.getFontMetrics();
        int iconX = (int) (circleX + (CIRCLE_SIZE - fm.stringWidth(icon)) / 2);
        int iconY = (int) (circleY + (CIRCLE_SIZE + fm.getAscent()) / 2 - 2);
        g2d.drawString(icon, iconX, iconY);
        
        g2d.dispose();
    }
    
    private Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        return new Color(r, g, b);
    }
    
    public boolean isDarkMode() {
        return isDarkMode;
    }
    
    public void setDarkMode(boolean darkMode) {
        if (this.isDarkMode != darkMode) {
            toggle();
        }
    }
    
    /**
     * Demo/Testing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dark Mode Toggle Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 50));
            
            JLabel label = new JLabel("Dark Mode:");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            
            DarkModeToggle toggle = new DarkModeToggle();
            
            panel.add(label);
            panel.add(toggle);
            
            // Listen to theme changes
            ThemeManager.getInstance().addThemeChangeListener(theme -> {
                panel.setBackground(theme.backgroundColor);
                label.setForeground(theme.textColor);
                frame.getContentPane().setBackground(theme.backgroundColor);
            });
            
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}

