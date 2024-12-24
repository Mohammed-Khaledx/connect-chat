# Connect - Real-time Chat Application with AI Integration

## Overview
Connect is a full-stack chat application that combines real-time messaging with AI-powered responses using Google's Gemini API. Built with JavaFX (frontend) and Spring Boot (backend), it features user authentication, global chat, and AI assistance.

## Features
- User authentication (signup/login)
- Real-time global chat
- AI-powered responses via Gemini
- Message persistence using MongoDB
- Dark theme UI
- WebSocket communication

## Tech Stack
- Frontend: JavaFX
- Backend: Spring Boot
- Database: MongoDB
- Real-time: WebSocket
- AI: Google Gemini API

## Project Structure
```
connect/
├── connect/ (Frontend)
│   ├── src/main/java/com/example/connect/
│   │   ├── Main.java (Entry point, authentication)
│   │   ├── GlobalChat.java (Chat interface)
│   │   ├── MessageHandler.java (Message processing)
│   │   └── model/
│   │       └── Message.java (Frontend message model)
│   └── resources/
│       └── styles.css (Application styling)
│
└── connect-backend/ (Backend)
    └── src/main/java/com/example/connectbackend/
        ├── controller/
        │   ├── AuthController.java
        │   └── AIController.java
        ├── model/
        │   └── Message.java
        ├── service/
        │   ├── AIService.java
        │   └── MessageService.java
        └── websocket/
            └── ChatWebSocketHandler.java
```

## Setup Instructions
### Prerequisites
- Java 21+
- Maven
- MongoDB
- Gemini API key

### Backend Configuration
Create `application.properties`:
```properties
spring.application.name=connect-backend
spring.data.mongodb.uri=mongodb://localhost:27017/chatapp
server.port=8080
gemini.api.key=YOUR_GEMINI_API_KEY
```

### Build & Run
```bash
# Backend
cd connect-backend
mvn spring-boot:run

# Frontend
cd connect
mvn javafx:run
```

## Code Examples

### Authentication
```java
// Main.java handles login/signup
private void handleLogin(String email, String password) {
    // Authenticate user via backend
    // On success, launch chat interface
}
```

### Real-time Chat
```java
// GlobalChat.java manages chat interface
public class GlobalChat extends Application {
    private final String userName;
    private final String userId;
    private ChatView chatView;

    public class ChatView extends VBox {
        private final VBox messagesContainer;
        private final TextField messageInput;
        private final Button aiButton;

        // Message display
        public void addMessage(String senderId, String senderName, String content, String timestamp) {
            VBox messageBox = new VBox(5);
            boolean isSentByCurrentUser = senderId.equals(userId);
            messageBox.getStyleClass().addAll("message-box", 
                isSentByCurrentUser ? "sent" : "received");
            
            // Add message content
            Text senderText = new Text(isSentByCurrentUser ? "You" : senderName);
            Text contentText = new Text(content);
            Text timestampText = new Text(timestamp);
            messageBox.getChildren().addAll(senderText, contentText, timestampText);
        }

        public void handleAIQuestion(String question) {
            try {
                Map<String, String> requestData = new HashMap<>();
                requestData.put("question", question);
                requestData.put("userId", userId);
                
                String jsonPayload = objectMapper.writeValueAsString(requestData);
                
                // Make async API call
                new Thread(() -> {
                    try {
                        URL url = new URL("http://localhost:8080/api/ai/ask");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        // Send request and handle response
                        if (conn.getResponseCode() == 200) {
                            Message response = mapper.readValue(
                                conn.getInputStream(), 
                                Message.class
                            );
                            Platform.runLater(() -> {
                                addMessage(
                                    "AI_ASSISTANT",
                                    "AI Assistant",
                                    response.getContent(),
                                    LocalDateTime.now().toString()
                                );
                            });
                        }
                    } catch (Exception e) {
                        showError("AI request failed: " + e.getMessage());
                    }
                }).start();
            } catch (Exception e) {
                showError("Error processing question: " + e.getMessage());
            }
        }
    }
}
```

### AI Integration
```java
// AIService.java processes AI requests
@Service
public class AIService {
    @Value("${gemini.api.key}")
    private String apiKey;

    public Message processAIQuestion(String question) {
        // Call Gemini API
        String aiResponse = callGeminiAPI(question);
        
        // Create response message
        Message responseMsg = new Message();
        responseMsg.setSenderId("AI_ASSISTANT");
        responseMsg.setUserName("AI Assistant");
        responseMsg.setContent(aiResponse);
        
        return messageService.saveMessage(responseMsg);
    }
}
```

## Security Considerations
- **API Keys**
  - Store Gemini API key in application.properties
  - Add properties file to .gitignore
  - Use environment variables in production
- **Authentication**
  - Password hashing
  - Session management
  - CORS configuration
- **WebSocket Security**
  - Authentication check on connection
  - Message validation

## Development Guidelines
- **Code Structure**
  - Separate concerns (UI/business logic)
  - Use proper packaging
  - Follow naming conventions
- **Best Practices**
  - Add comments for complex logic
  - Handle exceptions properly
  - Log important events
  - Use dependency injection
- **Testing**
  - Unit tests for services
  - Integration tests for controllers
  - UI tests for frontend

## Common Issues & Solutions
```java
// WebSocket Connection
ws://localhost:8080/chat

// MongoDB Connection
mongodb://localhost:27017/chatapp

// JavaFX UI Thread
Platform.runLater(() -> {
    // Update UI here
});
```

## Future Enhancements
- Private messaging
- File sharing
- Message history
- User profiles
- Rich text formatting
- Mobile app version

## Contributing
1. Fork the repository
2. Create feature branch
3. Follow code style
4. Add tests
5. Submit pull request

## License
MIT License

## Architecture Overview

### Authentication Flow
1. User signs up/logs in via [`Main.java`](connect/src/main/java/com/example/connect/Main.java):
```java
// Authentication request handling
private void sendRequest(String urlString, Map<String, String> payload, Label errorLabel, Stage primaryStage) {
    try {
        // Send auth request to backend
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        // On successful auth, launch chat
        if (responseCode == 200) {
            Map<String, String> response = mapper.readValue(conn.getInputStream(),
                new TypeReference<Map<String, String>>() {});
            String userId = response.get("id");
            String userName = response.get("userName");
            
            // Launch chat interface
            Platform.runLater(() -> {
                GlobalChat chatApp = new GlobalChat(userId, userName);
                chatApp.start(new Stage());
            });
        }
    } catch (Exception e) {
        showError("Authentication failed: " + e.getMessage());
    }
}
```

### Styling
Create `styles.css`:
```css
.root {
    -fx-background-color: #121212;
}

.message-box.sent {
    -fx-background-color: #0B93F6;
    -fx-alignment: CENTER-RIGHT;
}

.message-box.received {
    -fx-background-color: #E5E5EA;
    -fx-alignment: CENTER-LEFT;
}

.ai-button {
    -fx-background-color: #4a90e2;
    -fx-text-fill: white;
}
```

### Message Model
```java
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
}
```
