//package com.example.connect;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import javafx.application.Platform;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//
//// This class handles all message-related operations
//public class MessageHandler {
//    private final String userName;
//    private final String userId;
//    // Added to store the user's ID
//    private final GlobalChat.ChatView chatView;
//    private final ObjectMapper objectMapper;
//
//    public MessageHandler(String userName, String userId, GlobalChat.ChatView chatView) {
//        this.userName = userName;
//        this.userId = userId;  // Store user ID from authentication
//        this.chatView = chatView;
//        this.objectMapper = new ObjectMapper();
//    }
//
//    // Creates a message object matching your MongoDB structure
//    private Map<String, Object> createMessageObject(String content) {
//        Map<String, Object> message = new HashMap<>();
//        message.put("senderId", userId);        // Use actual user ID from authentication
//        message.put("receiverId", null);        // null for global messages
//        message.put("content", content);
//        message.put("timestamp", LocalDateTime.now().toString());
//        message.put("global", true);         // Set as global message
//
//        return message;
//    }
//
//    // Sends message to the server and handles the response
//    public void sendMessage(String content) {
//        if (content == null || content.trim().isEmpty()) {
//            return;
//        }
//
//        // Create message object
//        Map<String, Object> messageData = createMessageObject(content);
//
//        // Start a new thread for network operation
//        new Thread(() -> {
//            try {
//                System.out.println(messageData);
//                // Set up connection to your Spring Boot endpoint
//                URL url = new URL("http://localhost:8080/api/messages/send");
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("POST");
//                conn.setRequestProperty("Content-Type", "application/json");
//                conn.setDoOutput(true);
//
//                // Convert message to JSON
//                String jsonMessage = objectMapper.writeValueAsString(messageData);
//
//                // Send the message
//                try (OutputStream os = conn.getOutputStream()) {
//                    byte[] input = jsonMessage.getBytes(StandardCharsets.UTF_8);
//                    os.write(input, 0, input.length);
//                }
//
//                // Handle the response
//                int responseCode = conn.getResponseCode();
//                if (responseCode == 200 || responseCode == 201) {
//                    System.out.println("Message sent successfully");
//                    // Message sent successfully
//                    Platform.runLater(() -> {
//                        // Add message to the chat view
//                        chatView.addMessage(
//                                userName,
//                                content,
//                                LocalDateTime.now().toString()
//                        );
//                    });
//                } else {
//                    // Handle error
//                    Platform.runLater(() -> showError(
//                            "Failed to send message. Server returned: " + responseCode
//                    ));
//                }
//
//            } catch (Exception e) {
//                Platform.runLater(() -> showError(
//                        "Error sending message: " + e.getMessage()
//                ));
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//    private void showError(String message) {
//        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
//                javafx.scene.control.Alert.AlertType.ERROR
//        );
//        alert.setTitle("Error");
//        alert.setContentText(message);
//        alert.showAndWait();
//    }
//}


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
        message.put("userName" , userName);
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
            //     chatView.addMessage(userId,userName, content, LocalDateTime.now().toString());
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
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}