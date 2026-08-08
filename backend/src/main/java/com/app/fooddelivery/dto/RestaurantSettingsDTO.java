package com.app.fooddelivery.dto;

import lombok.Data;

/**
 * DTO containing all restaurant configuration and settings.
 * Used for centralized restaurant information retrieval.
 */
@Data
public class RestaurantSettingsDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String imageUrl;

    // Contact
    private String phone;
    private String email;

    // Operating Hours
    private String openingTime;
    private String closingTime;
    private Boolean isOpen;
    private String currentStatus; // "Open", "Closed", "Opens at X", "Closes at X"

    // Delivery
    private Double latitude;
    private Double longitude;
    private Double deliveryRadiusKm;

    // Scheduling
    private Boolean acceptsScheduledOrders;
    private Integer slotDurationMinutes;
}
