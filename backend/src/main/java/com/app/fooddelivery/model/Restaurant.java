package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String address;
    private String imageUrl;

    // Geolocation for distance calculations
    private Double latitude;
    private Double longitude;
    private Double deliveryRadiusKm = 10.0;

    // Contact Information
    private String phone;
    private String email;

    // Legacy Operating Hours (single pair, backward compat)
    private String openingTime; // e.g., "11:00 AM"
    private String closingTime; // e.g., "10:00 PM"
    private Boolean isOpen = true;

    // Scheduling Settings
    private Boolean acceptsScheduledOrders = true;
    private Integer slotDurationMinutes = 30;

    // Per-day operating hours (populated on onboarding approval)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "restaurant_id")
    private List<RestaurantOperatingHours> operatingHours = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<FoodItem> menu;
}
