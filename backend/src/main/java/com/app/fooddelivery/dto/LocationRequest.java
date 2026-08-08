package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class LocationRequest {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private Double latitude;  // null triggers server-side geocoding
    private Double longitude;
    private Double deliveryRadiusKm;
}
