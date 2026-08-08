package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a saved delivery address for a user.
 * Users can save multiple addresses with labels like "Home", "Office", etc.
 */
@Entity
@Data
@Table(name = "saved_address")
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String label; // e.g., "Home", "Office", "Friends Place"

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    private String country;

    // For distance calculations
    private Double latitude;
    private Double longitude;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "additional_instructions")
    private String additionalInstructions; // e.g., "Ring bell twice", "Gate code: 1234"
}
