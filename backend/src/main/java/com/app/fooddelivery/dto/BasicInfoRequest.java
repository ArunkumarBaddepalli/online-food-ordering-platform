package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class BasicInfoRequest {
    private String restaurantName;
    private String description;
    private String cuisineTypes; // comma-separated: "Indian,Chinese"
    private String restaurantType; // "VEG" | "NON_VEG" | "BOTH"
    private String phone;
    private String email;
}
