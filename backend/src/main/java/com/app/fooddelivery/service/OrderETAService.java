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

            // Collection orders have their own two stages. Without these they
            // fell through to the default below and a meal sitting on the
            // counter reported "Processing your order, 30 minutes".
            case "READY_FOR_PICKUP":
                return finished(order, now, "Ready to collect");

            case "PICKED_UP":
                return finished(order, now, "Order collected");

            case "DELIVERED":
                return finished(order, now, "Order delivered");

            case "CANCELLED":
                return finished(order, now, "Order cancelled");

            default:
                fallbackMinutes = 30;
                statusMessage = "Processing your order";
        }

        // Count down towards the target stored when the order was placed.
        LocalDateTime target = order.getEstimatedDeliveryAt();

        if (target == null) {
            // Orders placed before that target existed. Measure from when the
            // order was actually made, not from now — otherwise a months-old
            // order reports the same "25 minutes away" every time it is asked.
            target = order.getOrderDate() != null
                    ? order.getOrderDate().plusMinutes(fallbackMinutes)
                    : now.plusMinutes(fallbackMinutes);
        }

        long minutesRemaining = ChronoUnit.MINUTES.between(now, target);
        if (minutesRemaining < 0) {
            minutesRemaining = 0;
        }

        return new ETAResult((int) minutesRemaining, target, statusMessage);
    }

    /**
     * Nothing is left to wait for, so there are no minutes remaining.
     *
     * The time is still filled in rather than left null: most orders in the
     * database predate that column, and callers that put these values into a
     * response map cannot hold a null.
     */
    private ETAResult finished(Order order, LocalDateTime now, String message) {
        LocalDateTime when = order.getEstimatedDeliveryAt();
        if (when == null) {
            when = order.getOrderDate() != null ? order.getOrderDate() : now;
        }
        return new ETAResult(0, when, message);
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
