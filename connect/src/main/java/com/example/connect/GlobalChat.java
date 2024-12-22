package com.example.connect;

import com.example.connect.MessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.example.connect.model.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
        this.chatView = new ChatView(userId, userName);
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
                if (chatView.messageHandler != null) {
                    chatView.messageHandler.disconnect();
                }
                Platform.exit();
            });

            primaryStage.show();
        } catch (Exception e) {
            showError("Failed to start chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeChatScreen() {
        // Create the message fetcher
        MessageFetcher messageFetcher = new MessageFetcher(chatView);

        // Create the message handler
        MessageHandler messageHandler = new MessageHandler(userId, userName, chatView);

        // Create the scene
        scene = new Scene(chatView, 800, 600);

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set up message handling with AI support
        setupMessageHandling(messageHandler);

        // Add AI button handler
        Button aiButton = new Button("Ask AI");
        aiButton.getStyleClass().add("ai-button");
        aiButton.setOnAction(e -> {
            String message = chatView.getMessageText();
            if (message != null && !message.trim().isEmpty()) {
                chatView.handleAIQuestion(message);
                chatView.clearMessages();
            }
        });

        // Add AI button to chat view
        // chatView.addAIButton(aiButton);

        // Fetch existing messages
        messageFetcher.fetchMessages();
    }

    private void setupMessageHandling(MessageHandler messageHandler) {
        chatView.setOnSendMessage(() -> {
            String message = chatView.getMessageText();
            if (message != null && !message.trim().isEmpty()) {
                messageHandler.sendMessage(message);
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
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

            Button sendButton = new javafx.scene.control.Button("Send");
            sendButton.getStyleClass().add("send-button");
            // Add an AI button next to the send button
            Button aiButton = new Button("Ask AI");
            aiButton.getStyleClass().add("ai-button");

            // Configure input field
            javafx.scene.layout.HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);

            // Add components
            inputArea.getChildren().addAll(messageInput, sendButton, aiButton);
            this.getChildren().addAll(scrollPane, inputArea);

            // Setup send button action
            sendButton.setOnAction(e -> sendMessage());

            // Setup enter key press
            messageInput.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    sendMessage();
                }
            });

            // Add AI interaction handler
            aiButton.setOnAction(e -> {
                String question = messageInput.getText().trim();
                if (!question.isEmpty()) {
                    handleAIQuestion(question);
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
                    isSentByCurrentUser ? "You" : senderName);
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

        // public void addAIButton(Button aiButton) {
        //     // Find the input area HBox and add the AI button
        //     for (Node node : getChildren()) {
        //         if (node instanceof HBox) {
        //             HBox inputArea = (HBox) node;
        //             inputArea.getChildren().add(aiButton);
        //             break;
        //         }
        //     }
        // }

        // handele AI
        public void handleAIQuestion(String question) {
            try {
                // Create request payload
                Map<String, String> requestData = new HashMap<>();
                requestData.put("question", question);
                requestData.put("userId", userId);
                requestData.put("userName", userName);

                String jsonPayload = new ObjectMapper().registerModule(new JavaTimeModule())
                        .writeValueAsString(requestData);

                // Make REST API call asynchronously
                new Thread(() -> {
                    try {
                        URL url = new URL("http://localhost:8080/api/ai/ask");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        // Send request
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                        }

                        // Handle response
                        if (conn.getResponseCode() == 200) {
                            // Read and log the full response
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(conn.getInputStream()));
                            StringBuilder responseBody = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                responseBody.append(line);
                            }
                            System.out.println("AI Response: " + responseBody.toString());

                            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                            Message response = mapper.readValue(responseBody.toString(), 
                            Message.class);

                            // Update UI with AI response
                            Platform.runLater(() -> {
                                addMessage(
                                        "AI_ASSISTANT",
                                        "AI Assistant",
                                        response.getContent(),
                                        LocalDateTime.now().toString());
                            });
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("Error getting AI response: " + e.getMessage()));
                    }
                }).start();

            } catch (Exception e) {
                showError("Error processing AI question: " + e.getMessage());
            }
        }

        // In your GlobalChat class, modify the setupMessageHandling method:
        private MessageHandler messageHandler;



    }
}