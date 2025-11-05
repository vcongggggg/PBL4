package com.university.sms.client.gui.common;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Theme Manager - Quản lý Light/Dark mode
 */
public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme;
    private List<ThemeChangeListener> listeners;
    
    public interface ThemeChangeListener {
        void onThemeChanged(Theme newTheme);
    }
    
    public enum ThemeType {
        LIGHT, DARK
    }
    
    private ThemeManager() {
        this.currentTheme = Theme.LIGHT_THEME;
        this.listeners = new ArrayList<>();
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Apply theme to entire application
     */
    public void applyTheme(ThemeType type) {
        currentTheme = (type == ThemeType.DARK) ? Theme.DARK_THEME : Theme.LIGHT_THEME;
        
        // Update UIManager defaults
        updateUIManagerDefaults();
        
        // Notify listeners
        notifyListeners();
        
        // Update all windows
        updateAllWindows();
    }
    
    private void updateUIManagerDefaults() {
        Theme theme = currentTheme;
        
        // General colors
        UIManager.put("Panel.background", new ColorUIResource(theme.backgroundColor));
        UIManager.put("Panel.foreground", new ColorUIResource(theme.textColor));
        
        // Button colors
        UIManager.put("Button.background", new ColorUIResource(theme.componentBg));
        UIManager.put("Button.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("Button.select", new ColorUIResource(theme.accentColor));
        
        // Text component colors
        UIManager.put("TextField.background", new ColorUIResource(theme.inputBg));
        UIManager.put("TextField.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("TextField.caretForeground", new ColorUIResource(theme.textColor));
        UIManager.put("TextArea.background", new ColorUIResource(theme.inputBg));
        UIManager.put("TextArea.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("TextPane.background", new ColorUIResource(theme.inputBg));
        UIManager.put("TextPane.foreground", new ColorUIResource(theme.textColor));
        
        // Table colors
        UIManager.put("Table.background", new ColorUIResource(theme.componentBg));
        UIManager.put("Table.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("Table.selectionBackground", new ColorUIResource(theme.accentColor));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("TableHeader.background", new ColorUIResource(theme.headerBg));
        UIManager.put("TableHeader.foreground", new ColorUIResource(theme.textColor));
        
        // List colors
        UIManager.put("List.background", new ColorUIResource(theme.componentBg));
        UIManager.put("List.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("List.selectionBackground", new ColorUIResource(theme.accentColor));
        UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));
        
        // ComboBox colors
        UIManager.put("ComboBox.background", new ColorUIResource(theme.inputBg));
        UIManager.put("ComboBox.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(theme.accentColor));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.WHITE));
        
        // Menu colors
        UIManager.put("Menu.background", new ColorUIResource(theme.componentBg));
        UIManager.put("Menu.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("MenuItem.background", new ColorUIResource(theme.componentBg));
        UIManager.put("MenuItem.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(theme.accentColor));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("MenuBar.background", new ColorUIResource(theme.headerBg));
        UIManager.put("MenuBar.foreground", new ColorUIResource(theme.textColor));
        
        // ScrollPane colors
        UIManager.put("ScrollPane.background", new ColorUIResource(theme.backgroundColor));
        UIManager.put("ScrollBar.background", new ColorUIResource(theme.componentBg));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(theme.borderColor));
        
        // TabbedPane colors
        UIManager.put("TabbedPane.background", new ColorUIResource(theme.backgroundColor));
        UIManager.put("TabbedPane.foreground", new ColorUIResource(theme.textColor));
        UIManager.put("TabbedPane.selected", new ColorUIResource(theme.accentColor));
        
        // Border colors
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(theme.textColor));
    }
    
    private void updateAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.repaint();
        }
    }
    
    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(currentTheme);
        }
    }
    
    public void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    public boolean isDarkMode() {
        return currentTheme == Theme.DARK_THEME;
    }
    
    public void toggleTheme() {
        applyTheme(isDarkMode() ? ThemeType.LIGHT : ThemeType.DARK);
    }
    
    /**
     * Theme data class
     */
    public static class Theme {
        public final String name;
        public final Color backgroundColor;
        public final Color componentBg;
        public final Color inputBg;
        public final Color textColor;
        public final Color textSecondary;
        public final Color accentColor;
        public final Color borderColor;
        public final Color headerBg;
        public final Color successColor;
        public final Color errorColor;
        public final Color warningColor;
        public final Color infoColor;
        
        public Theme(String name, Color backgroundColor, Color componentBg, Color inputBg,
                     Color textColor, Color textSecondary, Color accentColor, Color borderColor,
                     Color headerBg, Color successColor, Color errorColor, Color warningColor, Color infoColor) {
            this.name = name;
            this.backgroundColor = backgroundColor;
            this.componentBg = componentBg;
            this.inputBg = inputBg;
            this.textColor = textColor;
            this.textSecondary = textSecondary;
            this.accentColor = accentColor;
            this.borderColor = borderColor;
            this.headerBg = headerBg;
            this.successColor = successColor;
            this.errorColor = errorColor;
            this.warningColor = warningColor;
            this.infoColor = infoColor;
        }
        
        // Light Theme
        public static final Theme LIGHT_THEME = new Theme(
            "Light",
            new Color(236, 240, 241),      // backgroundColor
            Color.WHITE,                    // componentBg
            new Color(250, 250, 250),       // inputBg
            new Color(44, 62, 80),          // textColor
            new Color(127, 140, 141),       // textSecondary
            new Color(41, 128, 185),        // accentColor (blue)
            new Color(220, 220, 220),       // borderColor
            new Color(52, 73, 94),          // headerBg (dark blue)
            new Color(46, 204, 113),        // successColor (green)
            new Color(231, 76, 60),         // errorColor (red)
            new Color(243, 156, 18),        // warningColor (orange)
            new Color(52, 152, 219)         // infoColor (blue)
        );
        
        // Dark Theme
        public static final Theme DARK_THEME = new Theme(
            "Dark",
            new Color(30, 30, 30),          // backgroundColor
            new Color(45, 45, 45),          // componentBg
            new Color(55, 55, 55),          // inputBg
            new Color(220, 220, 220),       // textColor
            new Color(170, 170, 170),       // textSecondary
            new Color(52, 152, 219),        // accentColor (bright blue)
            new Color(80, 80, 80),          // borderColor
            new Color(35, 35, 35),          // headerBg
            new Color(46, 204, 113),        // successColor (green)
            new Color(231, 76, 60),         // errorColor (red)
            new Color(243, 156, 18),        // warningColor (orange)
            new Color(52, 152, 219)         // infoColor (blue)
        );
    }
}

