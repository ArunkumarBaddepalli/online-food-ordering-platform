package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating estimated time of arrival for orders.
 * Provides ETA based on order status, preparation time, and delivery distance.
 */
@Service
public class OrderETAService {

    // Fallback estimates in minutes, used only for orders placed before
    // estimatedDeliveryAt existed.
    private static final int PREP_TIME_PLACED = 25; // Order just placed
    private static final int PREP_TIME_CONFIRMED = 20; // Restaurant confirmed
    private static final int PREP_TIME_PREPARING = 15; // Currently preparing
    private static final int PREP_TIME_OUT_FOR_DELIVERY = 10; // On the way

    /**
     * Calculate estimated time of arrival for an order.
     * 
     * @param order The order
     * @return ETAResult with estimated minutes remaining and delivery time
     */
    public ETAResult calculateETA(Order order) {
        LocalDateTime now = LocalDateTime.now();
        String status = order.getStatus() == null ? "" : order.getStatus();

        String statusMessage;
        int fallbackMinutes;

        switch (status) {
            case "PLACED":
            case "WAITING":
                fallbackMinutes = PREP_TIME_PLACED;
                statusMessage = "Order received, awaiting confirmation";
                break;

            case "CONFIRMED":
            case "RECEIVED":
                fallbackMinutes = PREP_TIME_CONFIRMED;
                statusMessage = "Restaurant is preparing your order";
                break;

            case "PREPARING":
                fallbackMinutes = PREP_TIME_PREPARING;
                statusMessage = "Your order is being prepared";
                break;

            case "OUT_FOR_DELIVERY":
                fallbackMinutes = PREP_TIME_OUT_FOR_DELIVERY;
                statusMessage = "Your order is on the way";
                break;

            case "DELIVERED":
                return new ETAResult(0, order.getEstimatedDeliveryAt(), "Order delivered");

            case "CANCELLED":
                return new ETAResult(0, order.getEstimatedDeliveryAt(), "Order cancelled");

            default:
                fallbackMinutes = 30;
                statusMessage = "Processing your order";
        }

        // Count down towards the target stored when the order was placed.
        // Older orders have no target, so fall back to the per-status estimate.
        LocalDateTime target = order.getEstimatedDeliveryAt();
        if (target == null) {
            target = now.plusMinutes(fallbackMinutes);
        }

        long minutesRemaining = ChronoUnit.MINUTES.between(now, target);
        if (minutesRemaining < 0) {
            minutesRemaining = 0;
        }

        return new ETAResult((int) minutesRemaining, target, statusMessage);
    }

    /**
     * Result of ETA calculation.
     */
    public static class ETAResult {
        private final int minutesRemaining;
        private final LocalDateTime estimatedDeliveryTime;
        private final String statusMessage;

        public ETAResult(int minutesRemaining, LocalDateTime estimatedDeliveryTime, String statusMessage) {
            this.minutesRemaining = minutesRemaining;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.statusMessage = statusMessage;
        }

        public int getMinutesRemaining() {
            return minutesRemaining;
        }

        public LocalDateTime getEstimatedDeliveryTime() {
            return estimatedDeliveryTime;
        }

        public String getStatusMessage() {
            return statusMessage;
        }
    }
}
