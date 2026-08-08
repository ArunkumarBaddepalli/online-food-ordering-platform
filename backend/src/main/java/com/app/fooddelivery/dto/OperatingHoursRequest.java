package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class OperatingHoursRequest {
    private String dayOfWeek; // "MONDAY"..."SUNDAY"
    private Boolean isOpen;
    private String openTime;  // "HH:mm"
    private String closeTime; // "HH:mm"
}
