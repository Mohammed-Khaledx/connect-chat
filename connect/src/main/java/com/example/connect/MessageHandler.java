
package com.example.connect;

import com.example.connect.Websocket.ChatWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MessageHandler {
    private final String userId;
    private final String userName;
    private final GlobalChat.ChatView chatView;
    private final ObjectMapper objectMapper;
    private final ChatWebSocketClient webSocketClient;

    public ChatWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    
    public MessageHandler(String userId, String userName, GlobalChat.ChatView chatView) {
        this.userId = userId;
        this.userName = userName;
        this.chatView = chatView;
        this.objectMapper = new ObjectMapper();

        // Initialize WebSocket client
        this.webSocketClient = new ChatWebSocketClient(userId, userName, chatView);

        // Connect to WebSocket server
        this.webSocketClient.connect();
    }

    private Map<String, Object> createMessageObject(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", userId);
        message.put("userName", userName);
        message.put("receiverId", null);
        message.put("content", content);
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("global", true);
        return message;
    }

    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        try {
            // Create message object
            Map<String, Object> messageData = createMessageObject(content);

            // Convert to JSON
            String jsonMessage = objectMapper.writeValueAsString(messageData);

            // Send via WebSocket
            webSocketClient.send(jsonMessage);

            // // Show message in our chat view immediately
            // Platform.runLater(() -> {
            // chatView.addMessage(userId,userName, content,
            // LocalDateTime.now().toString());
            // });

        } catch (Exception e) {
            Platform.runLater(() -> showError("Error sending message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}