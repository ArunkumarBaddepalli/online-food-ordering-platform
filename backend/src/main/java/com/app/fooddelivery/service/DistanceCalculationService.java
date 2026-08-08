package com.app.fooddelivery.service;

import org.springframework.stereotype.Service;

/**
 * Service for calculating distances between coordinates using Haversine
 * formula.
 * Provides "as-the-crow-flies" distance calculation.
 */
@Service
public class DistanceCalculationService {

    private static final double EARTH_RADIUS_KM = 6371.0; // Earth's radius in kilometers

    /**
     * Calculate distance between two points using Haversine formula.
     * Returns distance in kilometers.
     * 
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                        Math.cos(lat1Rad) * Math.cos(lat2Rad);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if delivery is possible based on distance and restaurant's delivery
     * radius.
     * 
     * @param restaurantLat       Restaurant latitude
     * @param restaurantLon       Restaurant longitude
     * @param deliveryLat         Delivery address latitude
     * @param deliveryLon         Delivery address longitude
     * @param maxDeliveryRadiusKm Maximum delivery radius in kilometers
     * @return ValidationResult indicating if delivery is possible
     */
    public DeliveryValidationResult validateDeliveryDistance(
            double restaurantLat, double restaurantLon,
            double deliveryLat, double deliveryLon,
            double maxDeliveryRadiusKm) {

        double distance = calculateDistance(restaurantLat, restaurantLon, deliveryLat, deliveryLon);

        boolean withinRange = distance <= maxDeliveryRadiusKm;
        String message = withinRange
                ? String.format("Delivery available (%.2f km away)", distance)
                : String.format("Out of delivery range. Restaurant delivers up to %.1f km, but you are %.2f km away",
                        maxDeliveryRadiusKm, distance);

        return new DeliveryValidationResult(withinRange, distance, message);
    }

    /**
     * Result of delivery distance validation.
     */
    public static class DeliveryValidationResult {
        private final boolean withinRange;
        private final double distanceKm;
        private final String message;

        public DeliveryValidationResult(boolean withinRange, double distanceKm, String message) {
            this.withinRange = withinRange;
            this.distanceKm = distanceKm;
            this.message = message;
        }

        public boolean isWithinRange() {
            return withinRange;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public String getMessage() {
            return message;
        }
    }
}
