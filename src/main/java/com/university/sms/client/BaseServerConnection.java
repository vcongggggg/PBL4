package com.university.sms.client;

import com.university.sms.common.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp cơ sở chia sẻ toàn bộ logic socket/listener/gửi-chờ phản hồi.
 * Các client cụ thể chỉ cần override onConnect() để gửi metadata riêng (nếu
 * có).
 */
public abstract class BaseServerConnection implements IServerConnection {
  private static final Logger LOGGER = Logger.getLogger(BaseServerConnection.class.getName());

  protected Socket socket;
  protected ObjectInputStream inputStream;
  protected ObjectOutputStream outputStream;
  protected volatile boolean connected;

  protected final String serverHost;
  protected final int serverPort;

  private ResponseHandler responseHandler;
  private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

  protected BaseServerConnection(String serverHost, int serverPort) {
    this.serverHost = serverHost;
    this.serverPort = serverPort;
  }

  @Override
  public boolean connect() {
    try {
      socket = new Socket(serverHost, serverPort);
      outputStream = new ObjectOutputStream(socket.getOutputStream());
      inputStream = new ObjectInputStream(socket.getInputStream());
      connected = true;

      LOGGER.info("Connected to server: " + serverHost + ":" + serverPort);

      startMessageListener();
      onConnect();
      return true;
    } catch (java.net.ConnectException e) {
      LOGGER.log(Level.SEVERE,
          "Cannot connect to server " + serverHost + ":" + serverPort + " - Server may not be running", e);
      connected = false;
      return false;
    } catch (java.net.UnknownHostException e) {
      LOGGER.log(Level.SEVERE, "Unknown host: " + serverHost, e);
      connected = false;
      return false;
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error connecting to server " + serverHost + ":" + serverPort, e);
      connected = false;
      return false;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error during connection: " + e.getMessage(), e);
      connected = false;
      return false;
    }
  }

  @Override
  public void disconnect() {
    connected = false;
    try {
      if (inputStream != null)
        inputStream.close();
      if (outputStream != null)
        outputStream.close();
      if (socket != null && !socket.isClosed())
        socket.close();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error closing connection", e);
    }

    if (responseHandler != null)
      responseHandler.onDisconnected();
  }

  @Override
  public boolean isConnected() {
    return connected && socket != null && !socket.isClosed();
  }

  @Override
  public void setResponseHandler(ResponseHandler handler) {
    this.responseHandler = handler;
  }

  @Override
  public String getServerInfo() {
    return serverHost + ":" + serverPort;
  }

  @Override
  public boolean testConnection() {
    try (Socket test = new Socket(serverHost, serverPort)) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  protected void startMessageListener() {
    Thread t = new Thread(() -> {
      while (connected && !socket.isClosed()) {
        try {
          Message message = (Message) inputStream.readObject();
          if (message.getRequestId() != null) {
            PendingRequest pending = pendingRequests.remove(message.getRequestId());
            if (pending != null) {
              pending.future.complete(message);
              continue;
            }
          }
          if (responseHandler != null) {
            responseHandler.onResponse(message);
          }
        } catch (SocketException | EOFException e) {
          break;
        } catch (IOException | ClassNotFoundException e) {
          LOGGER.log(Level.SEVERE, "Error reading message from server", e);
          handleConnectionError();
          break;
        }
      }
    }, getClass().getSimpleName() + "-Listener");
    t.setDaemon(true);
    t.start();
  }

  protected void handleConnectionError() {
    connected = false;
    if (responseHandler != null)
      responseHandler.onError("Connection error occurred");
    pendingRequests.values().forEach(pending -> pending.future
        .complete(Message.createErrorResponse(pending.action, "Connection error occurred")));
    pendingRequests.clear();
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(5000);
        if (!connected)
          connect();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    });
  }

  // Renamed to avoid conflict with IServerConnection.sendRequest()
  protected boolean sendRequestInternal(Message request) {
    if (!isConnected() || outputStream == null)
      return false;
    try {
      synchronized (outputStream) {
        outputStream.writeObject(request);
        outputStream.flush();
      }
      return true;
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error sending request", e);
      handleConnectionError();
      return false;
    }
  }

  protected Message sendRequestAndWait(Message request, long timeoutSeconds) {
    if (request.getRequestId() == null) {
      request.setRequestId(java.util.UUID.randomUUID().toString());
    }

    CompletableFuture<Message> future = new CompletableFuture<>();
    pendingRequests.put(request.getRequestId(), new PendingRequest(request.getAction(), future));

    if (!sendRequestInternal(request)) {
      pendingRequests.remove(request.getRequestId());
      return Message.createErrorResponse(request.getAction(), "Failed to send request");
    }

    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (Exception e) {
      pendingRequests.remove(request.getRequestId());
      LOGGER.log(Level.SEVERE, "Error waiting for response", e);
      return Message.createErrorResponse(request.getAction(), "Timeout or error waiting for response");
    }
  }

  private static class PendingRequest {
    private final String action;
    private final CompletableFuture<Message> future;

    private PendingRequest(String action, CompletableFuture<Message> future) {
      this.action = action;
      this.future = future;
    }
  }

  /**
   * Hook để client cụ thể override và gửi metadata sau khi kết nối.
   */
  protected void onConnect() {
  }
}
