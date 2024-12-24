package com.example.connectbackend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.connectbackend.model.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

@Service
public class AIService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Message processAIQuestion(String question, String userId, String userName) {
        try {
            // Call Gemini API
            String aiResponse = callGeminiAPI(question);

            // Create message object
            Message responseMsg = new Message();
            responseMsg.setSenderId("AI_ASSISTANT");
            responseMsg.setUserName("AI Assistant");
            responseMsg.setContent(aiResponse);
            responseMsg.setTimestamp(LocalDateTime.now());
            responseMsg.setGlobal(true);

            // Save and return the message
            return messageService.saveMessage(responseMsg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process AI question: " + e.getMessage());
        }
    }

    private String callGeminiAPI(String question) {
        try {
            String geminiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent";
            URL url = new URL(geminiUrl + "?key=" + apiKey);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Set doOutput before getting output stream
            conn.setDoOutput(true);
            // Log request URL and headers for debugging
            System.out.println("Request URL: " + url);
            System.out.println("API Key: " + apiKey);

            // Prepare Gemini request payload
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", question);

            List<Map<String, String>> parts = new ArrayList<>();
            parts.add(part);
            content.put("parts", parts);

            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(content);
            requestBody.put("contents", contents);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            // Log request payload
            System.out.println("Request Payload: " + jsonPayload);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            // Log response code
            System.out.println("Response Code: " + conn.getResponseCode());

            // Get response
            if (conn.getResponseCode() == 200) {
                // Parse response
                Map<String, Object> response = objectMapper.readValue(
                        conn.getInputStream(),
                        new TypeReference<Map<String, Object>>() {
                        });

                // Log full response
                System.out.println("Full Response: " + response);

                return extractTextFromGeminiResponse(response);
            } else {
                // Log error response
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                System.out.println("Error Response: " + errorResponse.toString());

                throw new RuntimeException("Gemini API error: " + conn.getResponseCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage());
        }
    }

    private String extractTextFromGeminiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<String, Object> part = parts.get(0);
                        String text = (String) part.get("text");
                        if (text != null) {
                            return text;
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response structure from Gemini API");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error extracting text from Gemini response: " + e.getMessage());
        }
    }
}