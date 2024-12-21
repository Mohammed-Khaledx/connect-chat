package com.example.connect;

import com.example.connect.MessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalChat extends Application {
    private final String username;
    private final String userId;
    private Stage primaryStage;
    private Scene scene;
    private ChatView chatView; // Changed from GlobalChatView to ChatView

    // Constructor now includes error handling
    public GlobalChat(String username,String userId) {
        if (username == null || username.trim().isEmpty()) {
            // If username is not available, use email or a default
            this.username = "User";
            this.userId = "unknown";// You might want to modify this based on your needs
        } else {
            this.username = username;
            this.userId = userId;
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            this.primaryStage = stage;
            initializeChatScreen();

            // Set up the stage with error handling
            primaryStage.setTitle("Global Chat - " + username);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            primaryStage.setScene(scene);

            // Handle window closing
            primaryStage.setOnCloseRequest(event -> {
                if (messageHandler != null) {
                    messageHandler.disconnect();
                }
                Platform.exit();
            });

            primaryStage.show();
        } catch (Exception e) {
            showError("Failed to start chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Separate chat view class for better organization
    public class ChatView extends javafx.scene.layout.VBox {
        private javafx.scene.layout.VBox messagesContainer;
        private javafx.scene.control.TextField messageInput;
        private javafx.scene.control.ScrollPane scrollPane;
        private Runnable onSendMessage;

        public ChatView() {
            // Set up the basic layout
            this.setSpacing(10);
            this.setPadding(new javafx.geometry.Insets(10));

            // Create the messages container
            messagesContainer = new javafx.scene.layout.VBox(5);
            scrollPane = new javafx.scene.control.ScrollPane(messagesContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);

            // Create the input area
            javafx.scene.layout.HBox inputArea = new javafx.scene.layout.HBox(10);
            messageInput = new javafx.scene.control.TextField();
            javafx.scene.control.Button sendButton = new javafx.scene.control.Button("Send");

            // Configure the input field
            messageInput.setPromptText("Type your message...");
            javafx.scene.layout.HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);

            // Set up the send button action
            sendButton.setOnAction(e -> {
                String message = messageInput.getText().trim();
                if (!message.isEmpty()) {
                    if (onSendMessage != null) {
                        System.out.println("sending ...");
                        onSendMessage.run();
                    }
                    messageInput.clear();
                }
            });

            // Add components to the input area
            inputArea.getChildren().addAll(messageInput, sendButton);

            // Add all components to the main view
            this.getChildren().addAll(scrollPane, inputArea);
        }

        public void setOnSendMessage(Runnable handler) {
            this.onSendMessage = handler;
        }

        public String getMessageText() {
            return messageInput.getText();
        }

        public void addMessage(String sender, String content, String timestamp) {
            javafx.scene.layout.VBox messageBox = new javafx.scene.layout.VBox(2);
            messageBox.getStyleClass().add("message-box");

            javafx.scene.text.Text senderText = new javafx.scene.text.Text(sender);
            javafx.scene.text.Text contentText = new javafx.scene.text.Text(content);
            javafx.scene.text.Text timestampText = new javafx.scene.text.Text(timestamp);

            messageBox.getChildren().addAll(senderText, contentText, timestampText);
            messagesContainer.getChildren().add(messageBox);

            // Scroll to bottom
            scrollPane.setVvalue(1.0);
        }

        public void clearMessages() {
            messagesContainer.getChildren().clear();
        }
    }


    private MessageFetcher messageFetcher;

    private void initializeChatScreen() {
        // Create the chat view
        chatView = new ChatView();


        // Create the message fetcher
//        messageFetcher = new MessageFetcher(chatView);

        // Create the message handler (which now includes WebSocket functionality)
        messageHandler = new MessageHandler(username, userId, chatView);



        // Create the scene
        scene = new Scene(chatView, 800, 600);

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set up message handling
        setupMessageHandling();

        // Fetch existing messages when the chat opens
//        messageFetcher.fetchMessages();
    }


    // In your GlobalChat class, modify the setupMessageHandling method:
    private MessageHandler messageHandler;

    private void setupMessageHandling() {
        // Initialize MessageHandler with the current user's information
//        messageHandler = new MessageHandler(username,userId,chatView);

        // Set up the send message handler
        chatView.setOnSendMessage(() -> {
            String message = chatView.getMessageText();
            messageHandler.sendMessage(message);
        });
    }





//    private void setupMessageHandling() {
//        chatView.setOnSendMessage(() -> {
//            String message = chatView.getMessageText();
//            if (message != null && !message.trim().isEmpty()) {
//                try {
//                    Map<String, Object> messageData = new HashMap<>();
//                    messageData.put("senderId", username);
//                    messageData.put("content", message);
//                    messageData.put("isGlobal", true);
//                    messageData.put("timestamp", LocalDateTime.now().toString());
//
//                    sendMessageToServer(messageData);
//                } catch (Exception e) {
//                    showError("Failed to send message: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        });
//
////        // Start message listener
//        startMessageListener();
//
//
//    }

    // Rest of the methods remain the same as in the previous version
    // (sendMessageToServer, startMessageListener, fetchNewMessages, showError)
    // ...

    private void sendMessageToServer(Map<String, Object> messageData) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/messages/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonPayload = objectMapper.writeValueAsString(messageData);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Platform.runLater(() ->
                            showError("Failed to send message. Server returned: " + responseCode));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        showError("Failed to send message: " + e.getMessage()));
            }
        }).start();
    }


    private void startMessageListener() {
        // Create a new thread for periodic message checking
        Thread messageThread = new Thread(() -> {
            while (true) {
                try {
                    // Get messages from your server
                    fetchNewMessages();
                    Thread.sleep(1000); // Poll every second
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        messageThread.setDaemon(true); // This ensures the thread doesn't prevent app shutdown
        messageThread.start();
    }

    private void fetchNewMessages() {
        try {
            URL url = new URL("http://localhost:8080/api/messages/global");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                // Read the response
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> messages = mapper.readValue(
                        conn.getInputStream(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Update UI with new messages
                Platform.runLater(() -> {
                    for (Map<String, Object> message : messages) {
                        chatView.addMessage(
                                (String) message.get("senderId"),
                                (String) message.get("content"),
                                (String) message.get("timestamp")
                        );
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}