package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "restaurant_operating_hours")
public class RestaurantOperatingHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    private String dayOfWeek; // MONDAY, TUESDAY, ..., SUNDAY

    private Boolean isOpen = true;

    private String openTime;  // HH:mm format

    private String closeTime; // HH:mm format
}
