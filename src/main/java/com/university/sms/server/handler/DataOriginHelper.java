package com.university.sms.server.handler;

/**
 * Interface cho các helper method liên quan đến data origin
 */
public interface DataOriginHelper {
  void saveDataOrigin(String entityType, int entityId, String source);

  String getDataOrigin(String entityType, int entityId);

  void updateDataOriginTimestamp(String entityType, int entityId);
}
