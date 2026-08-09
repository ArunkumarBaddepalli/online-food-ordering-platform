package com.app.fooddelivery.controller;

import com.app.fooddelivery.service.RazorpayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private RazorpayPaymentService razorpayService;

    /** Lets the checkout page know whether to offer online payment at all. */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(Map.of(
                "onlineEnabled", razorpayService.isEnabled(),
                "keyId", razorpayService.getKeyId() == null ? "" : razorpayService.getKeyId()));
    }

    /** Starts an online payment and returns what the Razorpay popup needs. */
    @PostMapping("/razorpay/order/{orderId}")
    public ResponseEntity<?> createCheckout(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(razorpayService.createCheckout(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Confirms a payment. The signature is verified server-side before the
     * order is treated as paid.
     */
    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        try {
            Long orderId = Long.valueOf(body.get("orderId"));
            return ResponseEntity.ok(razorpayService.confirmPayment(
                    orderId,
                    body.get("razorpayOrderId"),
                    body.get("razorpayPaymentId"),
                    body.get("razorpaySignature")));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("A valid orderId is required");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
