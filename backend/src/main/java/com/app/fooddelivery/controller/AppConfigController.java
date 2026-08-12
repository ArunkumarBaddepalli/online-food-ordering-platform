package com.app.fooddelivery.controller;

import com.app.fooddelivery.service.EmailService;
import com.app.fooddelivery.service.RazorpayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import com.app.fooddelivery.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * What this deployment can actually do, so the app offers only what works.
 *
 * The alternative is hiding things in the interface while the API still accepts
 * them, which is a lie that holds right up until somebody calls it directly.
 */
@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    @Autowired
    private RazorpayPaymentService razorpayService;

    @Autowired
    private CurrentUser currentUser;

    @Value("${orders.delivery-enabled:true}")
    private boolean deliveryEnabled;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryEnabled", deliveryEnabled);
        body.put("onlinePaymentEnabled", razorpayService.isEnabled());
        body.put("razorpayKeyId", razorpayService.getKeyId() == null ? "" : razorpayService.getKeyId());
        body.put("emailEnabled", emailService.isConfigured());
        return ResponseEntity.ok(body);
    }

    /**
     * Sends a message to a chosen address so delivery can be confirmed without
     * placing an order or resetting a password. Admin only: otherwise it is a
     * way to send mail from this server to anybody.
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> body) {
        currentUser.require();
        if (!currentUser.require().isAdmin()) {
            throw new com.app.fooddelivery.exception.ForbiddenException("Administrators only.");
        }

        String to = body.get("email");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body("An address is required.");
        }

        if (!emailService.isConfigured()) {
            return ResponseEntity.badRequest().body(
                    "No mail server is configured, so nothing was sent. "
                    + "Set MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD in backend/.env.local.");
        }

        emailService.send(to, "Test message from Food Delivery",
                emailService.layout("Email is working", """
                        <p>If you are reading this, mail is configured correctly and the app can
                           reach your inbox.</p>
                        <p>Confirmations, password resets, application decisions and order updates
                           will all arrive the same way.</p>
                        """));

        return ResponseEntity.ok("Sent to " + to + ". If it does not arrive, check the server log and your spam folder.");
    }
}
