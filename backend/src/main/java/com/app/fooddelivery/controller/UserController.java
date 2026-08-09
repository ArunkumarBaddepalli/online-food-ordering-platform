package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import com.app.fooddelivery.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserProfile(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity
                .ok(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUserProfile(@PathVariable Long userId, @RequestBody User updatedUser) {
        currentUser.requireSelfOrAdmin(userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setAddress(updatedUser.getAddress());
        return ResponseEntity.ok(userRepository.save(user));
    }
}
