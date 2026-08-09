package com.app.fooddelivery.controller;

import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.security.CurrentUser;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CurrentUser currentUser;

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
        requireOwnOrder(orderId);
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
        Long orderId;
        try {
            orderId = Long.valueOf(body.get("orderId"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("A valid orderId is required");
        }

        // Outside the try below, so a refusal stays a 403 instead of being
        // swallowed and reported as a bad request.
        requireOwnOrder(orderId);

        try {
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

    /** Nobody may start or confirm a payment against someone else's order. */
    private void requireOwnOrder(Long orderId) {
        currentUser.requireOrderCustomer(orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found")));
    }
}
