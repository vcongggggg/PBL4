package com.university.sms.client.gui.common;

import com.university.sms.client.IServerConnection;
import com.university.sms.common.Message;
import com.university.sms.csvclient.CSVServerConnection;

import java.awt.*;

/**
 * Bridge between CSVServerConnection sync events and a Swing loading dialog.
 */
public class CsvSyncProgressMonitor implements CSVServerConnection.SyncProgressListener {
  private final LoadingOverlay overlay;

  public static void attach(Window owner, IServerConnection connection) {
    if (connection instanceof CSVServerConnection csvConnection) {
      csvConnection.setSyncProgressListener(new CsvSyncProgressMonitor(owner));
    }
  }

  private CsvSyncProgressMonitor(Window owner) {
    this.overlay = LoadingOverlay.forWindow(owner);
  }

  @Override
  public void onSyncStart(String action) {
    if (action == null || "NO_SYNC_NEEDED".equals(action)) {
      return;
    }
    String title = "Đang đồng bộ dữ liệu";
    if ("UPLOAD_TO_SERVER".equals(action)) {
      title = "Đang upload dữ liệu CSV";
    } else if ("DOWNLOAD_FROM_SERVER".equals(action)) {
      title = "Đang download dữ liệu từ server";
    }
    overlay.show(title, "Đang chuẩn bị...");
  }

  @Override
  public void onSyncStep(String action, String message) {
    if (action == null || "NO_SYNC_NEEDED".equals(action)) {
      return;
    }
    overlay.updateMessage(message);
  }

  @Override
  public void onSyncCompleted(String action, Message result) {
    if (action == null || "NO_SYNC_NEEDED".equals(action)) {
      overlay.hide();
      return;
    }
    boolean success = result != null && result.isSuccess();
    String message;
    if (result == null) {
      message = "Kết thúc đồng bộ";
    } else if (result.getMessage() != null && !result.getMessage().isEmpty()) {
      message = result.getMessage();
    } else {
      message = success ? "Đồng bộ thành công" : "Đồng bộ thất bại";
    }
    overlay.complete(message, success);
  }
}
