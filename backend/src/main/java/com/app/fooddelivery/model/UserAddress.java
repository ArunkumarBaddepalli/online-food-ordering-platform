package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String addressLine;
    private String label; // "Home", "Work", etc.

    // Optional: caching lat/lng to avoid re-geocoding
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String pincode;
}
