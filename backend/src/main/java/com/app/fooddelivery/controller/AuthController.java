package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.security.JwtService;
import com.app.fooddelivery.service.AccountEmailService;
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

    @Autowired
    private AccountEmailService accountEmail;

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
        profile.put("emailVerified", Boolean.TRUE.equals(user.getEmailVerified()));
        body.put("user", profile);

        return body;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // A client must never be able to hand itself a role by posting one.
            String requested = user.getRole();
            user.setRole("RESTAURANT_OWNER".equals(requested) ? "RESTAURANT_OWNER" : "USER");

            User created = userService.registerUser(user);
            accountEmail.sendVerification(created);
            return ResponseEntity.ok(session(created));
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

    /** Confirms an address from the link in the email. */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        boolean ok = accountEmail.verify(body.get("token"));
        return ok
                ? ResponseEntity.ok("Your email is confirmed.")
                : ResponseEntity.badRequest().body("That link is not valid or has expired.");
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        accountEmail.resendVerification(body.get("email"));
        // Deliberately the same answer either way, so this cannot be used to
        // discover which addresses have accounts.
        return ResponseEntity.ok("If that address needs confirming, a new link is on its way.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        accountEmail.requestPasswordReset(body.get("email"));
        return ResponseEntity.ok("If that address has an account, a reset link is on its way.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            accountEmail.resetPassword(body.get("token"), body.get("password"));
            return ResponseEntity.ok("Your password has been changed. You can sign in now.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
