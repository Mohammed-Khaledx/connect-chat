package com.example.connectbackend.websocket;

import com.example.connectbackend.model.Message;
import com.example.connectbackend.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    // private query

    // Store active sessions with their sender IDs
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(MessageService messageService) {
        this.messageService = messageService;
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to handle LocalDateTime
        objectMapper.findAndRegisterModules();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            // Extract userId and username from query parameters
            String query = session.getUri().getQuery();
            String userId = extractQueryParameter(query, "userId");
            String userName = extractQueryParameter(query, "userName");

            // Get the query parameters from the URI
            // String query = session.getUri().getQuery();
            System.out.println(session.getUri().getQuery());
            // String userId = extractUserId(query); // Add this helper method

            // String senderId = getUserIdFromSession(session);
            if (userId != null && userName != null) {
                sessions.put(userId, session);
                logger.info("User {} connected. Total sessions: {}", userId, userName, sessions.size());

                // Create and broadcast a connection status message
                Message statusMessage = new Message();
                statusMessage.setSenderId(userId);
                statusMessage.setUserName(userName);
                statusMessage.setGlobal(true);
                statusMessage.setContent("USER_CONNECTED");
                statusMessage.setTimestamp(LocalDateTime.now());

                broadcastGlobalMessage(statusMessage);
            } else {
                logger.error("Connection attempt without userId or userName");
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error during connection establishment", e);
        }
    }

    private String extractQueryParameter(String query, String paramName) {
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            // Extract userId and username from query parameters
            String query = session.getUri().getQuery();
            String userId = extractQueryParameter(query, "userId");
            String userName = extractQueryParameter(query, "userName");

            if (userId == null) {
                logger.warn("Received message from session without sender ID");
                return;
            }

            Message chatMessage = parseAndValidateMessage(textMessage.getPayload());
            if (chatMessage == null) {
                sendErrorToUser(userId, "Invalid message format");
                return;
            }

            // Ensure sender ID is set correctly
            chatMessage.setSenderId(userId);
            chatMessage.setUserName(userName);

            // Set timestamp if not already set
            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }

            // Save message to database
            Message savedMessage = messageService.saveMessage(chatMessage);

            // Handle message routing based on type (global or private)
            if (savedMessage.isGlobal()) {
                broadcastGlobalMessage(savedMessage);
            } else {
                // For private messages, send to specific receiver
                sendPrivateMessage(savedMessage);
            }

            logger.debug("Message handled for sender: {}, global: {}",
                    userId, savedMessage.isGlobal());

        } catch (Exception e) {
            logger.error("Error handling message", e);
            try {
                session.sendMessage(new TextMessage(createErrorMessage("Internal server error")));
            } catch (IOException ex) {
                logger.error("Error sending error message", ex);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            // Extract userId and username from query parameters
            String query = session.getUri().getQuery();
            String userId = extractQueryParameter(query, "userId");
            String userName = extractQueryParameter(query, "userName");
           
            if (userId != null) {
                sessions.remove(userId);

                // Create and broadcast a disconnection status message
                Message statusMessage = new Message();
                statusMessage.setSenderId(userId);
                statusMessage.setUserName(userName);
                statusMessage.setGlobal(true);
                statusMessage.setContent("USER_DISCONNECTED");
                statusMessage.setTimestamp(LocalDateTime.now());

                broadcastGlobalMessage(statusMessage);
                logger.info("User {} disconnected. Remaining sessions: {}", userName, sessions.size());
            }
        } catch (Exception e) {
            logger.error("Error during connection closure", e);
        }
    }

    private void broadcastGlobalMessage(Message message) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            logger.error("Error serializing message", e);
            return;
        }

        TextMessage textMessage = new TextMessage(messageJson);
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    logger.error("Error broadcasting message to session", e);
                }
            }
        });
    }

    private void sendPrivateMessage(Message message) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            logger.error("Error serializing private message", e);
            return;
        }

        TextMessage textMessage = new TextMessage(messageJson);

        // Send to recipient
        WebSocketSession recipientSession = sessions.get(message.getReceiverId());
        if (recipientSession != null && recipientSession.isOpen()) {
            try {
                recipientSession.sendMessage(textMessage);
            } catch (IOException e) {
                logger.error("Error sending message to recipient: {}", message.getReceiverId(), e);
            }
        }

        // Send to sender (so they see their own message)
        WebSocketSession senderSession = sessions.get(message.getSenderId());
        if (senderSession != null && senderSession.isOpen()) {
            try {
                senderSession.sendMessage(textMessage);
            } catch (IOException e) {
                logger.error("Error sending message to sender: {}", message.getSenderId(), e);
            }
        }
    }

    private void sendErrorToUser(String userId, String errorMessage) {
        Message error = new Message();
        error.setSenderId("SYSTEM");
        error.setReceiverId(userId);
        error.setContent(errorMessage);
        error.setGlobal(false);
        error.setTimestamp(LocalDateTime.now());

        try {
            String messageJson = objectMapper.writeValueAsString(error);
            WebSocketSession session = sessions.get(userId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (IOException e) {
            logger.error("Error sending error message to user: {}", userId, e);
        }
    }

    // private String getUserIdFromSession(String query, String paramName) {

    //     if (query == null)
    //         return null;
    //     String[] pairs = query.split("&");
    //     for (String pair : pairs) {
    //         String[] keyValue = pair.split("=");
    //         if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
    //             return keyValue[1];
    //         }
    //     }
    //     return null;
    // }

    private Message parseAndValidateMessage(String payload) {
        try {
            Message message = objectMapper.readValue(payload, Message.class);
            // Basic validation
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return null;
            }
            return message;
        } catch (IOException e) {
            logger.error("Error parsing message", e);
            return null;
        }
    }

    private String createErrorMessage(String errorMessage) {
        Message error = new Message();
        error.setSenderId("SYSTEM");
        error.setContent(errorMessage);
        error.setGlobal(true);
        error.setTimestamp(LocalDateTime.now());

        try {
            return objectMapper.writeValueAsString(error);
        } catch (IOException e) {
            logger.error("Error creating error message", e);
            return "{}";
        }
    }
}