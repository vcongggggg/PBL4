package com.university.sms.client.gui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lightweight loading dialog that can be reused by any Swing client window.
 */
public class LoadingOverlay {
  private static final Map<Window, LoadingOverlay> CACHE = new WeakHashMap<>();

  public static LoadingOverlay forWindow(Window owner) {
    if (owner == null) {
      throw new IllegalArgumentException("Owner window cannot be null");
    }
    return CACHE.computeIfAbsent(owner, LoadingOverlay::new);
  }

  private final JDialog dialog;
  private final JLabel titleLabel;
  private final JLabel messageLabel;
  private final JProgressBar progressBar;
  private Timer autoHideTimer;

  private LoadingOverlay(Window owner) {
    dialog = new JDialog(owner, "Đang xử lý", Dialog.ModalityType.MODELESS);
    dialog.setUndecorated(true);
    dialog.setAlwaysOnTop(true);
    dialog.getRootPane().setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(96, 165, 250), 2),
        new EmptyBorder(16, 20, 16, 20)));

    JPanel content = new JPanel();
    content.setBackground(new Color(36, 40, 48));
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

    titleLabel = new JLabel("Đang xử lý");
    titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
    titleLabel.setForeground(new Color(242, 244, 248));
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    messageLabel = new JLabel("Vui lòng chờ…");
    messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    messageLabel.setForeground(new Color(195, 202, 215));
    messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    messageLabel.setBorder(new EmptyBorder(10, 0, 8, 0));

    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setPreferredSize(new Dimension(260, 12));
    progressBar.setMaximumSize(new Dimension(260, 12));
    progressBar.setBorderPainted(false);
    progressBar.setForeground(new Color(96, 165, 250));
    progressBar.setBackground(new Color(60, 65, 74));

    content.add(titleLabel);
    content.add(messageLabel);
    content.add(progressBar);

    dialog.setContentPane(content);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
  }

  public void show(String title, String message) {
    SwingUtilities.invokeLater(() -> {
      cancelAutoHide();
      titleLabel.setText(title != null ? title : "Đang xử lý");
      messageLabel.setText(message != null ? message : "Vui lòng chờ…");
      progressBar.setIndeterminate(true);
      progressBar.setForeground(new Color(96, 165, 250));
      dialog.setVisible(true);
    });
  }

  public void updateMessage(String message) {
    SwingUtilities.invokeLater(() -> {
      if (dialog.isVisible() && message != null) {
        messageLabel.setText(message);
      }
    });
  }

  public void updateProgress(int value, int max, String message) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setMaximum(max > 0 ? max : 100);
      progressBar.setValue(Math.max(0, Math.min(value, progressBar.getMaximum())));
      progressBar.setIndeterminate(false);
      if (message != null) {
        messageLabel.setText(message);
      }
    });
  }

  public void complete(String message, boolean success) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setValue(progressBar.getMaximum());
      progressBar.setForeground(success ? new Color(82, 196, 136) : new Color(235, 87, 87));
      messageLabel.setText(message != null ? message : (success ? "Hoàn tất" : "Thất bại"));
      scheduleAutoHide();
    });
  }

  public void hide() {
    SwingUtilities.invokeLater(() -> {
      cancelAutoHide();
      dialog.setVisible(false);
    });
  }

  private void scheduleAutoHide() {
    cancelAutoHide();
    autoHideTimer = new Timer(1400, e -> dialog.setVisible(false));
    autoHideTimer.setRepeats(false);
    autoHideTimer.start();
  }

  private void cancelAutoHide() {
    if (autoHideTimer != null) {
      autoHideTimer.stop();
      autoHideTimer = null;
    }
  }
}
