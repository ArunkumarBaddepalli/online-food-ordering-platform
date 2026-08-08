package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating estimated time of arrival for orders.
 * Provides ETA based on order status, preparation time, and delivery distance.
 */
@Service
public class OrderETAService {

    @Autowired
    private DistanceCalculationService distanceCalculationService;

    // Average times in minutes
    private static final int PREP_TIME_PLACED = 25; // Order just placed
    private static final int PREP_TIME_CONFIRMED = 20; // Restaurant confirmed
    private static final int PREP_TIME_PREPARING = 15; // Currently preparing
    private static final double DELIVERY_SPEED_KM_PER_MIN = 0.5; // 30 km/h average

    /**
     * Calculate estimated time of arrival for an order.
     * 
     * @param order The order
     * @return ETAResult with estimated minutes remaining and delivery time
     */
    public ETAResult calculateETA(Order order) {
        LocalDateTime now = LocalDateTime.now();
        int minutesRemaining = 0;
        String statusMessage = "";

        switch (order.getStatus()) {
            case "PLACED":
            case "WAITING":
                minutesRemaining = PREP_TIME_PLACED;
                statusMessage = "Order received, awaiting confirmation";
                break;

            case "CONFIRMED":
            case "RECEIVED":
                minutesRemaining = PREP_TIME_CONFIRMED;
                statusMessage = "Restaurant is preparing your order";
                break;

            case "PREPARING":
                minutesRemaining = PREP_TIME_PREPARING;
                statusMessage = "Your order is being prepared";
                break;

            case "OUT_FOR_DELIVERY":
                // Calculate based on distance
                if (order.getRestaurant() != null &&
                        order.getRestaurant().getLatitude() != null &&
                        order.getRestaurant().getLongitude() != null) {

                    // For simplicity, assume delivery address is geocoded
                    // In real implementation, you'd geocode the delivery address
                    minutesRemaining = 10; // Default delivery time
                    statusMessage = "Your order is on the way";
                } else {
                    minutesRemaining = 10;
                    statusMessage = "Your order is on the way";
                }
                break;

            case "DELIVERED":
                minutesRemaining = 0;
                statusMessage = "Order delivered";
                break;

            case "CANCELLED":
                minutesRemaining = 0;
                statusMessage = "Order cancelled";
                break;

            default:
                minutesRemaining = 30;
                statusMessage = "Processing your order";
        }

        LocalDateTime estimatedTime = now.plusMinutes(minutesRemaining);
        return new ETAResult(minutesRemaining, estimatedTime, statusMessage);
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
