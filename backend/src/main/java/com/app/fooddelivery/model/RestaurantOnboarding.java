package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "restaurant_onboarding")
public class RestaurantOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long onboardingId;

    private Long userId;

    private Integer currentStep = 1;

    @Enumerated(EnumType.STRING)
    private OnboardingStatus status = OnboardingStatus.DRAFT;

    // Step 1 — Basic Info
    private String restaurantName;
    private String description;
    private String cuisineTypes; // comma-separated: "Indian,Chinese"

    @Enumerated(EnumType.STRING)
    private RestaurantType restaurantType;

    private String phone;
    private String email;

    // Step 2 — Location
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private Double latitude;
    private Double longitude;
    private Double deliveryRadiusKm;

    // Step 3 — Hours settings (rows stored in OperatingHours table)
    private Boolean acceptsScheduledOrders = false;
    private Integer slotDurationMinutes = 30;

    // Step 4 — Documents
    private String fssaiLicenseNumber;
    private String fssaiDocumentPath;
    private String panNumber;
    private String gstin;

    // Step 5 — Bank Details
    private String bankAccountHolderName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;

    // Metadata
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private Long createdRestaurantId;
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "onboarding_id")
    private List<OperatingHours> operatingHours = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
