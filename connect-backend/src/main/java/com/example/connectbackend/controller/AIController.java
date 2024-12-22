package com.example.connectbackend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.connectbackend.model.Message;
import com.example.connectbackend.service.AIService;
@RestController
@RequestMapping("/api/ai")
public class AIController {
    @Autowired
    private AIService aiService;

    @PostMapping("/ask")
    public ResponseEntity<?> askAI(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String userId = request.get("userId");
        String userName = request.get("userName");
        
        try {
            Message response = aiService.processAIQuestion(question, userId, userName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get AI response: " + e.getMessage()));
        }
    }
}