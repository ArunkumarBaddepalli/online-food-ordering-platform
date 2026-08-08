package com.app.fooddelivery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> pay(@RequestBody Map<String, Object> paymentRequest) {
        // Mock payment success
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "transactionId", "TXN" + System.currentTimeMillis()));
    }
}
