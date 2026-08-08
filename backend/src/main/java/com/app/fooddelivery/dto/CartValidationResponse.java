package com.app.fooddelivery.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartValidationResponse {
    private boolean valid;
    private List<CartItemValidation> items;

    @Data
    public static class CartItemValidation {
        private Long cartItemId;
        private Long foodItemId;
        private String foodItemName;
        private boolean isOOS;
        private Integer stockQuantity;
        private LocalDateTime nextAvailableAt;
        private String oosReason;
    }
}
