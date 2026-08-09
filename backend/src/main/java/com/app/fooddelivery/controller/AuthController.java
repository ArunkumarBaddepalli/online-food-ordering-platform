package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.security.JwtService;
import com.app.fooddelivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    /**
     * Signing in and signing up both return a token plus a safe summary of the
     * account. The password is never part of the response.
     */
    private Map<String, Object> session(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("token", jwtService.generateToken(user));
        body.put("expiresIn", jwtService.getExpirySeconds());

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        body.put("user", profile);

        return body;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // A client must never be able to hand itself a role by posting one.
            String requested = user.getRole();
            user.setRole("RESTAURANT_OWNER".equals(requested) ? "RESTAURANT_OWNER" : "USER");

            return ResponseEntity.ok(session(userService.registerUser(user)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        User user = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
        return ResponseEntity.ok(session(user));
    }
}
