package com.example.connect;

import com.example.connect.MessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
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

    private final String userName;
    private final String userId;
    private Stage primaryStage;
    private Scene scene;
    private ChatView chatView; // Changed from GlobalChatView to ChatView

    // Constructor now includes error handling
    public GlobalChat(String userId, String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            // If userName is not available, use email or a default
            this.userName = "User";
            this.userId = "unknown";// You might want to modify this based on your needs
        } else {
            this.userName = userName;
            this.userId = userId;
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            this.primaryStage = stage;
            initializeChatScreen();

            // Set up the stage with error handling
            primaryStage.setTitle("Global Chat - " + userName);
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

    public class ChatView extends javafx.scene.layout.VBox {
        private final String userId;
        private final String userName;
        private final javafx.scene.layout.VBox messagesContainer;
        private final javafx.scene.control.TextField messageInput;
        private final javafx.scene.control.ScrollPane scrollPane;
        private Runnable onSendMessage;
        // private String userId;

        public ChatView(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            // Set up the basic layout
            this.setSpacing(10);
            this.setPadding(new javafx.geometry.Insets(10));
            this.getStyleClass().add("chat-view");

            // Create the messages container
            messagesContainer = new javafx.scene.layout.VBox(10);
            messagesContainer.getStyleClass().add("messages-container");

            // Configure scroll pane
            scrollPane = new javafx.scene.control.ScrollPane(messagesContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.getStyleClass().add("scroll-pane");

            // Create input area
            javafx.scene.layout.HBox inputArea = new javafx.scene.layout.HBox(10);
            inputArea.setAlignment(Pos.CENTER);

            messageInput = new javafx.scene.control.TextField();
            messageInput.setPromptText("Type a message...");
            messageInput.getStyleClass().add("message-input");

            javafx.scene.control.Button sendButton = new javafx.scene.control.Button("Send");
            sendButton.getStyleClass().add("send-button");

            // Configure input field
            javafx.scene.layout.HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);

            // Add components
            inputArea.getChildren().addAll(messageInput, sendButton);
            this.getChildren().addAll(scrollPane, inputArea);

            // Setup send button action
            sendButton.setOnAction(e -> sendMessage());

            // Setup enter key press
            messageInput.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    sendMessage();
                }
            });
        }

        private void sendMessage() {
            String message = messageInput.getText().trim();
            if (!message.isEmpty() && onSendMessage != null) {
                onSendMessage.run();
                messageInput.clear();
            }
        }

        public void addMessage(String senderId, String senderName, String content, String timestamp) {
            javafx.scene.layout.VBox messageBox = new javafx.scene.layout.VBox(5);
        
            // Determine if this message was sent by the current user
            boolean isSentByCurrentUser = senderId.equals(userId);
        
            // Add appropriate style classes
            messageBox.getStyleClass().addAll("message-box",
                    isSentByCurrentUser ? "sent" : "received");
        
            // Create text elements with proper sender name
            javafx.scene.text.Text senderText = new javafx.scene.text.Text(
                isSentByCurrentUser ? "You" : senderName
            );
            senderText.getStyleClass().add("sender-name");
        
            javafx.scene.text.Text contentText = new javafx.scene.text.Text(content);
            contentText.getStyleClass().add("message-content");
            contentText.setWrappingWidth(300);
        
            javafx.scene.text.Text timestampText = new javafx.scene.text.Text(timestamp);
            timestampText.getStyleClass().add("timestamp");
        
            // Add elements to message box
            messageBox.getChildren().addAll(senderText, contentText, timestampText);
        
            // Create container for alignment and add message box
            javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();
            container.setPadding(new javafx.geometry.Insets(5));
            container.getChildren().add(messageBox);
            
            // Set alignment based on sender
            if (isSentByCurrentUser) {
                container.setAlignment(Pos.CENTER_RIGHT);
                messageBox.setAlignment(Pos.CENTER_RIGHT);
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
                messageBox.setAlignment(Pos.CENTER_LEFT);
            }
        
            // Add to messages container
            Platform.runLater(() -> {
                messagesContainer.getChildren().add(container);
                scrollPane.setVvalue(1.0);
            });
        }

        public void setOnSendMessage(Runnable handler) {
            this.onSendMessage = handler;
        }

        public String getMessageText() {
            return messageInput.getText();
        }

        public void clearMessages() {
            messagesContainer.getChildren().clear();
        }

        // public void setUserId(String userId) {
        // this.userId = userId;
        // }
    }

    private MessageFetcher messageFetcher;

    private void initializeChatScreen() {
        // Create the chat view
        chatView = new ChatView(userId, userName);

        // Create the message fetcher
        messageFetcher = new MessageFetcher(chatView);

        // Create the message handler (which now includes WebSocket functionality)
        messageHandler = new MessageHandler(userId, userName, chatView);

        // Create the scene
        scene = new Scene(chatView, 800, 600);

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set up message handling
        setupMessageHandling();

        // Fetch existing messages when the chat opens
        messageFetcher.fetchMessages();
    }

    // In your GlobalChat class, modify the setupMessageHandling method:
    private MessageHandler messageHandler;

    private void setupMessageHandling() {
        // Initialize MessageHandler with the current user's information
        // messageHandler = new MessageHandler(userName,userId,chatView);

        // Set up the send message handler
        chatView.setOnSendMessage(() -> {
            String message = chatView.getMessageText();
            messageHandler.sendMessage(message);
        });
    }

    // private void setupMessageHandling() {
    // chatView.setOnSendMessage(() -> {
    // String message = chatView.getMessageText();
    // if (message != null && !message.trim().isEmpty()) {
    // try {
    // Map<String, Object> messageData = new HashMap<>();
    // messageData.put("senderId", username);
    // messageData.put("content", message);
    // messageData.put("isGlobal", true);
    // messageData.put("timestamp", LocalDateTime.now().toString());
    //
    // sendMessageToServer(messageData);
    // } catch (Exception e) {
    // showError("Failed to send message: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }
    // });
    //
    //// // Start message listener
    // startMessageListener();
    //
    //
    // }

    // Rest of the methods remain the same as in the previous version
    // (sendMessageToServer, startMessageListener, fetchNewMessages, showError)
    // ...

    // private void sendMessageToServer(Map<String, Object> messageData) {
    //     new Thread(() -> {
    //         try {
    //             URL url = new URL("http://localhost:8080/api/messages/send");
    //             HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    //             conn.setRequestMethod("POST");
    //             conn.setRequestProperty("Content-Type", "application/json");
    //             conn.setDoOutput(true);

    //             ObjectMapper objectMapper = new ObjectMapper();
    //             String jsonPayload = objectMapper.writeValueAsString(messageData);

    //             try (OutputStream os = conn.getOutputStream()) {
    //                 os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
    //             }

    //             int responseCode = conn.getResponseCode();
    //             if (responseCode != 200) {
    //                 Platform.runLater(() -> showError("Failed to send message. Server returned: " + responseCode));
    //             }
    //         } catch (Exception e) {
    //             Platform.runLater(() -> showError("Failed to send message: " + e.getMessage()));
    //         }
    //     }).start();
    // }

    // private void startMessageListener() {
    //     // Create a new thread for periodic message checking
    //     Thread messageThread = new Thread(() -> {
    //         while (true) {
    //             try {
    //                 // Get messages from your server
    //                 fetchNewMessages();
    //                 Thread.sleep(1000); // Poll every second
    //             } catch (InterruptedException e) {
    //                 break;
    //             } catch (Exception e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     });
    //     messageThread.setDaemon(true); // This ensures the thread doesn't prevent app shutdown
    //     messageThread.start();
    // }

    // private void fetchNewMessages() {
    //     try {
    //         URL url = new URL("http://localhost:8080/api/messages/global");
    //         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    //         conn.setRequestMethod("GET");

    //         if (conn.getResponseCode() == 200) {
    //             // Read the response
    //             ObjectMapper mapper = new ObjectMapper();
    //             List<Map<String, Object>> messages = mapper.readValue(
    //                     conn.getInputStream(),
    //                     new TypeReference<List<Map<String, Object>>>() {
    //                     });

    //             // Update UI with new messages
    //             Platform.runLater(() -> {
    //                 for (Map<String, Object> message : messages) {
    //                     System.out.println("!!!!!!!!" + message.get("useName"));
    //                     chatView.addMessage(
    //                             (String) message.get("senderId"),
    //                             (String) message.get("userName"),
    //                             (String) message.get("content"),
    //                             (String) message.get("timestamp"));
    //                 }
    //             });
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}