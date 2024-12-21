package com.example.connectbackend.controller;

import com.example.connectbackend.model.Message;
import com.example.connectbackend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        return ResponseEntity.ok(messageService.saveMessage(message));
    }

    @GetMapping("/global")
    public ResponseEntity<?> getGlobalMessages() {
        return ResponseEntity.ok(messageService.getGlobalMessages());
    }

//    @GetMapping("/private/{senderId}/{receiverId}")
//    public ResponseEntity<?> getPrivateMessages(
//            @PathVariable String senderId,
//            @PathVariable String receiverId) {
//        return ResponseEntity.ok(messageService.getPrivateMessages(senderId, receiverId));
//    }
}