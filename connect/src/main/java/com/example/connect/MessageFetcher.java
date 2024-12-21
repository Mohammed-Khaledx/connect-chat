package com.example.connect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.IOException;
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
                // Connect to your messages endpoint
                URL url = new URL("http://localhost:8080/api/messages/global");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Read the response
                if (conn.getResponseCode() == 200) {
                    // Parse the JSON response into a List of messages
                    List<Map<String, Object>> messages = objectMapper.readValue(
                            conn.getInputStream(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    // Update the UI with the fetched messages
                    Platform.runLater(() -> {
                        // Clear existing messages before adding fetched ones
                        chatView.clearMessages();

                        // Add each message to the chat view
                        for (Map<String, Object> message : messages) {
                            String sender = (String) message.get("senderId");
                            String content = (String) message.get("content");
                            String timestamp = formatTimestamp((String) message.get("timestamp"));

                            chatView.addMessage(sender, content, timestamp);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        try {
                            showError(
                                    "Failed to fetch messages. Server returned: " + conn.getResponseCode()
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> showError(
                        "Error fetching messages: " + e.getMessage()
                ));
                e.printStackTrace();
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