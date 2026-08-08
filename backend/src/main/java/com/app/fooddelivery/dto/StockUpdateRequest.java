package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class StockUpdateRequest {
    private Boolean inStock;
    private Integer stockQuantity;
    private String stockResetType;   // UNLIMITED | DAILY | MANUAL
    private Integer dailyStockLimit;
    private String dailyRestockTime; // "HH:mm"
    private String oosReason;
}
