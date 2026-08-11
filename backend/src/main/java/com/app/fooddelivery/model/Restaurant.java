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

    // Which user account runs this restaurant. Set when an onboarding
    // application is approved; null for restaurants added directly as data.
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    // Legacy Operating Hours (single pair, backward compat)
    private String openingTime; // e.g., "11:00 AM"
    private String closingTime; // e.g., "10:00 PM"
    private Boolean isOpen = true;

    // Scheduling Settings
    private Boolean acceptsScheduledOrders = true;
    private Integer slotDurationMinutes = 30;

    // When on, new orders are confirmed automatically instead of waiting for
    // the owner to tap Accept. Cooking and delivery stay manual.
    private Boolean autoAcceptOrders = false;

    // Comma separated, as captured during onboarding, e.g. "Indian,Chinese".
    // Used for browsing by cuisine on the home page.
    private String cuisineTypes;

    /**
     * Whether the kitchen is open right now, worked out from today's hours.
     *
     * Not stored: isOpen above is only the owner's on/off switch, and a listing
     * that reads that alone calls a restaurant open at midnight.
     */
    @Transient
    private Boolean currentlyOpen;

    // Per-day operating hours (populated on onboarding approval)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "restaurant_id")
    private List<RestaurantOperatingHours> operatingHours = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<FoodItem> menu;
}
