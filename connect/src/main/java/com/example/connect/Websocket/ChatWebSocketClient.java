package com.example.connect.Websocket;

import com.example.connect.GlobalChat;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ChatWebSocketClient extends WebSocketClient {
    private final GlobalChat.ChatView chatView;

    private final ObjectMapper objectMapper;
    private final String userId;
    private final String userName;
    private boolean isOpen;

    public ChatWebSocketClient(String userId, String userName, GlobalChat.ChatView chatView) {
        // Connect to your WebSocket endpoint
        super(URI.create("ws://localhost:8080/chat?userId=" + userId + "&userName=" + userName));
        this.chatView = chatView;
        this.userId = userId;
        this.userName = userName;
        this.objectMapper = new ObjectMapper();

        // Configure ObjectMapper for dates
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("WebSocket Connection Established");
        this.isOpen = true;

    }

    // on resiving the message
    @Override
    public void onMessage(String message) {
        try {
            // Parse the message as a single Message object
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            // System.out.println("Received message: " + messageData); // Debug print

            // Convert timestamp array to string format
            Object timestamp = messageData.get("timestamp");
            if (timestamp instanceof List) {
                List<?> timestampList = (List<?>) timestamp;
                // Format: [2024,12,21,9,53,4,103178041]
                String formattedTimestamp = String.format("%d-%02d-%02dT%02d:%02d:%02d",
                        ((Number) timestampList.get(0)).intValue(),
                        ((Number) timestampList.get(1)).intValue(),
                        ((Number) timestampList.get(2)).intValue(),
                        ((Number) timestampList.get(3)).intValue(),
                        ((Number) timestampList.get(4)).intValue(),
                        ((Number) timestampList.get(5)).intValue());
                messageData.put("timestamp", formattedTimestamp);
            }

            handleSingleMessage(messageData);
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSingleMessage(Map<String, Object> messageData) {
        String senderId = (String) messageData.get("senderId");
        String senderName = (String) messageData.get("userName");
        String content = (String) messageData.get("content");
        String timestamp = (String) messageData.get("timestamp");

        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            // Don't display our own messages twice since MessageHandler already shows them
            // if (!senderId.equals(userId)

            // ) {
                chatView.addMessage(senderId, senderName, content, timestamp);
            // }
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket Connection Closed: " + reason);

        // Attempt to reconnect after a delay
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (!isOpen) {
                    // reconnect();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void send(String text) {
        if (!isOpen) {
            System.out.println("Waiting for connection...");
            try {
                connectBlocking(); // Wait for connection
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
        super.send(text);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket Error: " + ex.getMessage());
    }

}