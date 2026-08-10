package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.security.CurrentUser;
import com.app.fooddelivery.service.OrderService;
import com.app.fooddelivery.service.OrderETAService;
import com.app.fooddelivery.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderETAService etaService;

    @Autowired
    private com.app.fooddelivery.service.OrderStatusFlow statusFlow;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(
            @RequestParam Long userId,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(required = false) String scheduledTime,
            @RequestParam(required = false, defaultValue = "DELIVERY") String orderType,
            @RequestParam(required = false, defaultValue = "COD") String paymentMethod) {
        currentUser.requireSelfOrAdmin(userId);
        try {
            LocalDateTime scheduledDateTime = null;
            if (scheduledTime != null && !scheduledTime.isEmpty()) {
                scheduledDateTime = LocalDateTime.parse(scheduledTime, DateTimeFormatter.ISO_DATE_TIME);
            }

            com.app.fooddelivery.model.OrderType type;
            try {
                type = com.app.fooddelivery.model.OrderType.valueOf(orderType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid order type. Must be PICKUP or DELIVERY");
            }

            Order order = orderService.placeOrder(userId, deliveryAddress, scheduledDateTime, type, paymentMethod);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        currentUser.requireOrderParticipant(order);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        currentUser.requireOrderRestaurantOwner(order);
        try {
            return ResponseEntity.ok(orderService.advanceStatus(orderId, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Incoming orders for a restaurant, newest first, each with the next legal
     * step so the dashboard does not have to model the lifecycle itself.
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Map<String, Object>>> getRestaurantOrders(@PathVariable Long restaurantId) {
        currentUser.requireRestaurantOwner(restaurantId);
        List<Map<String, Object>> result = orderRepository
                .findByRestaurantIdOrderByOrderDateDesc(restaurantId).stream()
                .map(order -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("order", order);
                    row.put("nextStatus", statusFlow.nextForwardStep(order));
                    row.put("nextLabel", statusFlow.nextForwardLabel(order));
                    row.put("canCancel", statusFlow.canCancel(order.getStatus()));
                    row.put("isFinished", statusFlow.isTerminal(order.getStatus()));

                    OrderETAService.ETAResult eta = etaService.calculateETA(order);
                    row.put("eta", Map.of(
                            "minutesRemaining", eta.getMinutesRemaining(),
                            "statusMessage", eta.getStatusMessage()));
                    return row;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Cancel order. Only PLACED or CONFIRMED orders can be cancelled.
     * Restores stock for non-UNLIMITED items.
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        currentUser.requireOrderParticipant(order);
        try {
            return ResponseEntity.ok(orderService.cancelOrder(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveOrders(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        // Newest first: the floating tracker shows the first of these, and the
        // customer means the order they just placed, not their oldest one.
        List<Order> activeOrders = orderRepository.findByUserId(userId).stream()
                .filter(order -> !statusFlow.isTerminal(order.getStatus()))
                .sorted(java.util.Comparator.comparing(
                        Order::getOrderDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();

        List<Map<String, Object>> ordersWithETA = activeOrders.stream().map(order -> {
            OrderETAService.ETAResult eta = etaService.calculateETA(order);
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("order", order);
            orderData.put("eta", Map.of(
                    "minutesRemaining", eta.getMinutesRemaining(),
                    "estimatedDeliveryTime", eta.getEstimatedDeliveryTime(),
                    "statusMessage", eta.getStatusMessage()));
            return orderData;
        }).toList();

        return ResponseEntity.ok(ordersWithETA);
    }
}
