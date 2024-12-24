package com.example.connectbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.connectbackend.service.UserService;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")  // Add CORS support
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<List<String>> getUsersList() {
        try {
            List<String> usernames = userService.getAllUsers();
            // System.out.println("Returning users: " + usernames); // Debug output
            return ResponseEntity.ok(usernames);
        } catch (Exception e) {
            System.err.println("Error getting users: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}