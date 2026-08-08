package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class DeliveryValidationRequest {
    private String address;
    private Double latitude;
    private Double longitude;
}
