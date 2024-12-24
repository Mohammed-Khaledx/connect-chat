package com.example.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import com.example.connect.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GlobalChat extends Application {
    private final String userName;
    private final String userId;
    private Stage primaryStage;
    private Scene scene;
    private ChatView chatView;
    private MessageHandler messageHandler;
    private MessageFetcher messageFetcher;

    public GlobalChat(String userId, String userName) {
        validateUserCredentials(userId, userName);
        this.userId = userId;
        this.userName = userName;
        this.chatView = new ChatView(userId, userName);
    }

    private void validateUserCredentials(String userId, String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            initializeUI(stage);
            initializeMessageHandling();
            setupWindowClosing(stage);
            stage.show();
        } catch (Exception e) {
            handleInitializationError(e);
        }
    }

    private void initializeUI(Stage stage) {
        this.primaryStage = stage;
        scene = new Scene(chatView, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Global Chat - " + userName);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setScene(scene);
    }

    private void initializeMessageHandling() {
        messageHandler = new MessageHandler(userId, userName, chatView);
        messageFetcher = new MessageFetcher(chatView);

        setupMessageHandlers();
        setupAIHandler();
        fetchExistingMessages();
    }

    private void setupMessageHandlers() {
        chatView.setOnSendMessage(() -> {
            String message = chatView.getMessageText();
            if (isValidMessage(message)) {
                messageHandler.sendMessage(message);
                chatView.clearMessageInput();
            }
        });
    }

    private void setupAIHandler() {
        chatView.setOnAIRequest(question -> {
            if (isValidMessage(question)) {
                System.out.println(question);
                question = "@AI " + question;
                messageHandler.sendMessage(question);
                handleAIQuestion(question);
                chatView.clearMessageInput();
            }
        });
    }

    private boolean isValidMessage(String message) {
        return message != null && !message.trim().isEmpty();
    }

    private void handleAIQuestion(String question) {
        CompletableFuture.runAsync(() -> {
            try {
                AIRequest request = new AIRequest(userId, userName, question);

                AIResponse response = request.send();

                Platform.runLater(() -> {
                    // Create message object for WebSocket
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("senderId", "AI_ASSISTANT");
                    messageData.put("userName", "AI_Assistant");
                    messageData.put("content", response.getContent());
                    messageData.put("timestamp", LocalDateTime.now().toString());
                    messageData.put("global", true);

                    System.out.println(messageData);

                    // Send via WebSocket to ensure persistence and broadcasting
                    String jsonMessage;
                    try {
                        jsonMessage = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .writeValueAsString(messageData);
                        messageHandler.getWebSocketClient().send(jsonMessage);
                    } catch (JsonProcessingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // chatView.addMessage(
                    // "AI_ASSISTANT",
                    // "AI Assistant",
                    // response.getContent(),
                    // LocalDateTime.now().toString());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("AI request failed: " + e.getMessage()));
            }
        });
    }

    private void fetchExistingMessages() {
        messageFetcher.fetchMessages();
    }

    private void setupWindowClosing(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (messageHandler != null) {
                messageHandler.disconnect();
            }
            Platform.exit();
        });
    }

    private void handleInitializationError(Exception e) {
        showError("Failed to start chat: " + e.getMessage());
        e.printStackTrace();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Inner class for AI request handling
    private static class AIRequest {
        private final String userId;
        private final String userName;
        private final String question;

        public AIRequest(String userId, String userName, String question) {
            this.userId = userId;
            this.userName = userName;
            this.question = question;
        }

        AIResponse send() throws IOException {
            // AI request implementation
            try {
                Map<String, String> requestData = new HashMap<>();
                requestData.put("question", question);
                requestData.put("userId", userId);
                requestData.put("userName", userName);

                ObjectMapper mapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule());
                String jsonPayload = mapper.writeValueAsString(requestData);

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
                    Message response = mapper.readValue(
                            conn.getInputStream(),
                            Message.class);
                    return new AIResponse(response.getContent());
                } else {
                    throw new IOException("Server returned code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                throw new IOException("Failed to get AI response: " + e.getMessage(), e);
            }
        }
    }

    private static class AIResponse {
        private final String content;

        public AIResponse(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    public class ChatView extends javafx.scene.layout.VBox {
        private MessageHandler messageHandler;
        private final String userId;
        private final String userName;
        private final javafx.scene.layout.VBox messagesContainer;
        private final javafx.scene.control.TextField messageInput;
        private final javafx.scene.control.ScrollPane scrollPane;
        private Runnable onSendMessage;
        private java.util.function.Consumer<String> onAIRequest;

        public ChatView(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            this.setSpacing(10);
            this.setPadding(new javafx.geometry.Insets(10));
            this.getStyleClass().add("chat-view");

            messagesContainer = new javafx.scene.layout.VBox(10);
            messagesContainer.getStyleClass().add("messages-container");

            scrollPane = new javafx.scene.control.ScrollPane(messagesContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.getStyleClass().add("scroll-pane");

            javafx.scene.layout.HBox inputArea = new javafx.scene.layout.HBox(10);
            inputArea.setAlignment(Pos.CENTER);

            messageInput = new javafx.scene.control.TextField();
            messageInput.setPromptText("Type a message...");
            messageInput.getStyleClass().add("message-input");

            Button sendButton = new javafx.scene.control.Button("Send");
            sendButton.getStyleClass().add("send-button");
            Button aiButton = new Button("Ask AI");
            aiButton.getStyleClass().add("ai-button");

            javafx.scene.layout.HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);

            inputArea.getChildren().addAll(messageInput, sendButton, aiButton);
            this.getChildren().addAll(scrollPane, inputArea);

            sendButton.setOnAction(e -> sendMessage(e));

            messageInput.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    sendMessage(event);
                }
            });

            aiButton.setOnAction(e -> {
                String question = messageInput.getText().trim();
                if (!question.isEmpty() && onAIRequest != null) {
                    onAIRequest.accept(question);
                }
                // sendMessage(e);
            });
        }

        private void sendMessage(Event e) {
            System.out.println(e.getTarget());
            String message = messageInput.getText().trim();
            if (!message.isEmpty() && onSendMessage != null) {
                onSendMessage.run();
                messageInput.clear();
            }
        }

        public void addMessage(String senderId, String senderName, String content, String timestamp) {
            javafx.scene.layout.VBox messageBox = new javafx.scene.layout.VBox(5);
            System.out.println("useId = " + userId + "senderId = " + senderId);
            boolean isSentByCurrentUser = senderId.equals(userId);
            boolean isAIQuestion = content.startsWith("@AI ");
            boolean isAIResponse = senderId.equals("AI_ASSISTANT");

            // Add appropriate style classes
            messageBox.getStyleClass().addAll("message-box",
                    isSentByCurrentUser ? "sent" : "received",
                    isAIQuestion ? "ai-question" : "",
                    isAIResponse ? "ai-response" : "");

            messageBox.getStyleClass().addAll("message-box",
                    isSentByCurrentUser ? "sent" : "received");

            javafx.scene.text.Text senderText = new javafx.scene.text.Text(
                    isSentByCurrentUser ? "You" : senderName);
            senderText.getStyleClass().add("sender-name");

            javafx.scene.text.Text contentText = new javafx.scene.text.Text(content);
            contentText.getStyleClass().add("message-content");
            contentText.setWrappingWidth(300);

            javafx.scene.text.Text timestampText = new javafx.scene.text.Text(timestamp);
            timestampText.getStyleClass().add("timestamp");

            messageBox.getChildren().addAll(senderText, contentText, timestampText);

            javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();
            container.setPadding(new javafx.geometry.Insets(5));
            container.getChildren().add(messageBox);

            if (isSentByCurrentUser) {
                container.setAlignment(Pos.CENTER_RIGHT);
                messageBox.setAlignment(Pos.CENTER_RIGHT);
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
                messageBox.setAlignment(Pos.CENTER_LEFT);
            }

            Platform.runLater(() -> {
                messagesContainer.getChildren().add(container);
                scrollPane.setVvalue(2.0);
            });
        }

        public void setOnSendMessage(Runnable handler) {
            this.onSendMessage = handler;
        }

        public void setOnAIRequest(java.util.function.Consumer<String> handler) {
            this.onAIRequest = handler;
        }

        public String getMessageText() {
            return messageInput.getText();
        }

        public void clearMessageInput() {
            messageInput.clear();
        }

        public void clearMessages() {
            messagesContainer.getChildren().clear();
        }
    }
}