package com.example.connect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageFetcher {
    private final GlobalChat.ChatView chatView;
    private final ObjectMapper objectMapper;
    private LocalDateTime lastFetchTime;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MessageFetcher(GlobalChat.ChatView chatView) {
        this.chatView = chatView;
        this.objectMapper = new ObjectMapper();
        this.lastFetchTime = LocalDateTime.now();
    }

    // Fetches all global messages from the server
    public void fetchMessages() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/messages/global");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
    
                if (conn.getResponseCode() == 200) {
                    List<Map<String, Object>> messages = objectMapper.readValue(
                        conn.getInputStream(),
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
    
                    Platform.runLater(() -> {
                        chatView.clearMessages();
                        // Only add messages that were sent before the connection
                        for (Map<String, Object> message : messages) {
                            // System.out.println("Message data: " + message);

                            String timestamp = (String) message.get("timestamp");
                            LocalDateTime messageTime = LocalDateTime.parse(timestamp);
                            
                            // Only show messages from before the connection
                            if (messageTime.isBefore(lastFetchTime)) {

                                // System.out.println(message);
                                String senderId = (String) message.get("senderId");
                                String userName = (String) message.get("userName");
                                String content = (String) message.get("content");
                                chatView.addMessage(senderId, userName, content, formatTimestamp(timestamp));
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error fetching messages: " + e.getMessage()));
            }
        }).start();
    }

    // Helper method to format timestamps
    private String formatTimestamp(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp);
            return dateTime.format(formatter);
        } catch (Exception e) {
            return timestamp; // Return original if parsing fails
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}