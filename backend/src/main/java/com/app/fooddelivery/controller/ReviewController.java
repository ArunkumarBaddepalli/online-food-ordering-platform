package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Review;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.repository.ReviewRepository;
import com.app.fooddelivery.security.CurrentUser;
import com.app.fooddelivery.service.OrderStatusFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CurrentUser currentUser;

    /** Reviews for a restaurant, newest first. Open to anyone browsing. */
    @GetMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<Map<String, Object>> getReviews(@PathVariable Long restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        Double average = reviewRepository.averageForRestaurant(restaurantId);

        Map<String, Object> body = new HashMap<>();
        body.put("reviews", reviews);
        body.put("count", reviews.size());
        body.put("average", average == null ? null : Math.round(average * 10) / 10.0);
        return ResponseEntity.ok(body);
    }

    /** Average and count for every restaurant, for the listing page. */
    @GetMapping("/restaurants/ratings")
    public ResponseEntity<Map<Long, Map<String, Object>>> getAllRatings() {
        Map<Long, Map<String, Object>> summary = new HashMap<>();
        for (Object[] row : reviewRepository.summariseByRestaurant()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("average", Math.round(((Number) row[1]).doubleValue() * 10) / 10.0);
            entry.put("count", ((Number) row[2]).intValue());
            summary.put(((Number) row[0]).longValue(), entry);
        }
        return ResponseEntity.ok(summary);
    }

    /** Whether the signed-in customer may still review this order. */
    @GetMapping("/orders/{orderId}/review")
    public ResponseEntity<?> getOrderReview(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        currentUser.requireOrderCustomer(order);

        Map<String, Object> body = new HashMap<>();
        body.put("canReview", isReviewable(order) && !reviewRepository.existsByOrderId(orderId));
        body.put("review", reviewRepository.findByOrderId(orderId).orElse(null));
        return ResponseEntity.ok(body);
    }

    /**
     * Leaves a review. Only the customer who placed the order, only once, and
     * only after the food actually arrived.
     */
    @PostMapping("/orders/{orderId}/review")
    public ResponseEntity<?> createReview(@PathVariable Long orderId, @RequestBody Map<String, Object> body) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        currentUser.requireOrderCustomer(order);

        if (!isReviewable(order)) {
            return ResponseEntity.badRequest()
                    .body("You can review an order once it has been delivered or collected.");
        }
        if (reviewRepository.existsByOrderId(orderId)) {
            return ResponseEntity.badRequest().body("You have already reviewed this order.");
        }

        int rating;
        try {
            rating = ((Number) body.get("rating")).intValue();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("A rating from 1 to 5 is required.");
        }
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("A rating must be between 1 and 5.");
        }

        Object comment = body.get("comment");

        Review review = new Review();
        review.setOrder(order);
        review.setRestaurant(order.getRestaurant());
        review.setUser(order.getUser());
        review.setRating(rating);
        review.setComment(comment == null ? null : comment.toString().trim());
        review.setAuthorName(order.getUser() == null ? "Customer" : order.getUser().getName());

        return ResponseEntity.ok(reviewRepository.save(review));
    }

    /** Food has to have arrived before there is anything to rate. */
    private boolean isReviewable(Order order) {
        return OrderStatusFlow.DELIVERED.equals(order.getStatus())
                || OrderStatusFlow.PICKED_UP.equals(order.getStatus());
    }
}
