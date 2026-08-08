package com.app.fooddelivery.dto;

import lombok.Data;
import java.util.List;

@Data
public class HoursWrapperRequest {
    private List<OperatingHoursRequest> hours;
    private Boolean acceptsScheduledOrders;
    private Integer slotDurationMinutes;
}
