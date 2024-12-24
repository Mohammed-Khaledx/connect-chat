package com.example.connect.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
       private String id;
    private String senderId;
    private String receiverId;
    private String userName;
    private String content;
    private LocalDateTime timestamp;
    private boolean global;

    @JsonCreator
    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    public Message(String senderId, String userName, String content) {
        this.senderId = senderId;
        this.userName = userName;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.global = true;
    }

    // Getters and setters with JsonProperty annotations
    @JsonProperty("id")
    public String getId() { return id; }
    @JsonProperty("id") 
    public void setId(String id) { this.id = id; }
    
    @JsonProperty("senderId")
    public String getSenderId() { return senderId; }
    @JsonProperty("senderId")
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    @JsonProperty("userName")
    public String getUserName() { return userName; }
    @JsonProperty("userName")
    public void setUserName(String userName) { this.userName = userName; }
    
    @JsonProperty("receiverId")
    public String getReceiverId() { return receiverId; }
    @JsonProperty("receiverId")
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    @JsonProperty("content")
    public String getContent() { return content; }
    @JsonProperty("content")
    public void setContent(String content) { this.content = content; }
    
    @JsonProperty("timestamp")
    public LocalDateTime getTimestamp() { return timestamp; }
    @JsonProperty("timestamp")
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @JsonProperty("global")
    public boolean isGlobal() { return global; }
    @JsonProperty("global")
    public void setGlobal(boolean global) { this.global = global; }
}