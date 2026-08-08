package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "operating_hours")
public class OperatingHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "onboarding_id")
    private Long onboardingId;

    private String dayOfWeek; // MONDAY, TUESDAY, ..., SUNDAY

    private Boolean isOpen = true;

    private String openTime;  // HH:mm format e.g. "09:00"

    private String closeTime; // HH:mm format e.g. "22:00"
}
