package com.university.sms.client.gui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;
import java.util.List;

/**
 * Advanced Search Panel với auto-complete và multi-filter
 */
public class AdvancedSearchPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private JTextField searchField;
    private JPopupMenu autoCompletePopup;
    private DefaultListModel<String> autoCompleteModel;
    private JList<String> autoCompleteList;
    
    private Map<String, JComboBox<String>> filterComboBoxes;
    private JButton searchButton;
    private JButton clearButton;
    
    private SearchListener searchListener;
    
    public interface SearchListener {
        void onSearch(String searchText, Map<String, String> filters);
    }
    
    public AdvancedSearchPanel() {
        this.filterComboBoxes = new LinkedHashMap<>();
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);
        
        // Search field with auto-complete
        JPanel searchPanel = createSearchPanel();
        add(searchPanel, BorderLayout.NORTH);
        
        // Filter panel
        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Auto-complete popup
        createAutoCompletePopup();
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        // Search icon label
        JLabel iconLabel = new JLabel("🔍");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        iconLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
        panel.add(iconLabel, BorderLayout.WEST);
        
        // Search field
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        // Placeholder text
        searchField.setForeground(Color.GRAY);
        searchField.setText("Tìm kiếm...");
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Tìm kiếm...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("Tìm kiếm...");
                }
            }
        });
        
        // Auto-complete on typing
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAutoComplete();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAutoComplete();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAutoComplete();
            }
        });
        
        // Enter key to search
        searchField.addActionListener(e -> performSearch());
        
        panel.add(searchField, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("Bộ lọc"));
        
        // Filters will be added dynamically via addFilter()
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);
        
        clearButton = new JButton("🗑️ Xóa bộ lọc");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> clearFilters());
        
        searchButton = new JButton("🔍 Tìm kiếm");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.setBackground(new Color(52, 152, 219));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setOpaque(true);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> performSearch());
        
        panel.add(clearButton);
        panel.add(searchButton);
        
        return panel;
    }
    
    private void createAutoCompletePopup() {
        autoCompleteModel = new DefaultListModel<>();
        autoCompleteList = new JList<>(autoCompleteModel);
        autoCompleteList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        autoCompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        autoCompleteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = autoCompleteList.getSelectedValue();
                if (selected != null) {
                    searchField.setText(selected);
                    searchField.setForeground(Color.BLACK);
                    autoCompletePopup.setVisible(false);
                    performSearch();
                }
            }
        });
        
        autoCompletePopup = new JPopupMenu();
        JScrollPane scrollPane = new JScrollPane(autoCompleteList);
        scrollPane.setPreferredSize(new Dimension(searchField.getWidth(), 150));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        autoCompletePopup.add(scrollPane);
        autoCompletePopup.setBorder(null);
    }
    
    /**
     * Thêm filter dropdown
     */
    public void addFilter(String label, String[] options) {
        JPanel filterRow = new JPanel(new BorderLayout(10, 0));
        filterRow.setOpaque(false);
        filterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel filterLabel = new JLabel(label + ":");
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterLabel.setPreferredSize(new Dimension(120, 30));
        filterRow.add(filterLabel, BorderLayout.WEST);
        
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setPreferredSize(new Dimension(200, 30));
        filterRow.add(comboBox, BorderLayout.CENTER);
        
        filterComboBoxes.put(label, comboBox);
        
        // Add to filter panel
        Component filterPanel = getComponent(1); // Get filter panel
        if (filterPanel instanceof JPanel) {
            ((JPanel) filterPanel).add(filterRow);
            ((JPanel) filterPanel).add(Box.createVerticalStrut(5));
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * Set auto-complete suggestions
     */
    public void setAutoCompleteSuggestions(List<String> suggestions) {
        autoCompleteModel.clear();
        if (suggestions != null) {
            for (String suggestion : suggestions) {
                autoCompleteModel.addElement(suggestion);
            }
        }
    }
    
    private void updateAutoComplete() {
        String text = searchField.getText();
        
        if (text.isEmpty() || text.equals("Tìm kiếm...") || autoCompleteModel.isEmpty()) {
            autoCompletePopup.setVisible(false);
            return;
        }
        
        // Filter suggestions based on current text
        DefaultListModel<String> filteredModel = new DefaultListModel<>();
        for (int i = 0; i < autoCompleteModel.size(); i++) {
            String suggestion = autoCompleteModel.getElementAt(i);
            if (suggestion.toLowerCase().contains(text.toLowerCase())) {
                filteredModel.addElement(suggestion);
            }
        }
        
        if (filteredModel.isEmpty()) {
            autoCompletePopup.setVisible(false);
        } else {
            autoCompleteList.setModel(filteredModel);
            autoCompletePopup.setPreferredSize(new Dimension(searchField.getWidth(), 
                Math.min(150, filteredModel.size() * 25 + 10)));
            
            if (!autoCompletePopup.isVisible()) {
                autoCompletePopup.show(searchField, 0, searchField.getHeight());
            }
        }
    }
    
    private void performSearch() {
        if (searchListener == null) return;
        
        String searchText = searchField.getText();
        if (searchText.equals("Tìm kiếm...")) {
            searchText = "";
        }
        
        // Get filter values
        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, JComboBox<String>> entry : filterComboBoxes.entrySet()) {
            String filterName = entry.getKey();
            String filterValue = (String) entry.getValue().getSelectedItem();
            if (filterValue != null && !filterValue.equals("Tất cả") && !filterValue.isEmpty()) {
                filters.put(filterName, filterValue);
            }
        }
        
        searchListener.onSearch(searchText, filters);
        autoCompletePopup.setVisible(false);
    }
    
    private void clearFilters() {
        searchField.setText("");
        searchField.setForeground(Color.GRAY);
        searchField.setText("Tìm kiếm...");
        
        for (JComboBox<String> comboBox : filterComboBoxes.values()) {
            comboBox.setSelectedIndex(0);
        }
        
        if (searchListener != null) {
            searchListener.onSearch("", new HashMap<>());
        }
    }
    
    private void setupLayout() {
        // Layout is already set up
    }
    
    public void setSearchListener(SearchListener listener) {
        this.searchListener = listener;
    }
    
    public String getSearchText() {
        String text = searchField.getText();
        return text.equals("Tìm kiếm...") ? "" : text;
    }
    
    public Map<String, String> getFilters() {
        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, JComboBox<String>> entry : filterComboBoxes.entrySet()) {
            String filterValue = (String) entry.getValue().getSelectedItem();
            if (filterValue != null && !filterValue.equals("Tất cả")) {
                filters.put(entry.getKey(), filterValue);
            }
        }
        return filters;
    }
}

