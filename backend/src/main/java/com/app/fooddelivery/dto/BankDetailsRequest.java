package com.app.fooddelivery.dto;

import lombok.Data;

@Data
public class BankDetailsRequest {
    private String bankAccountHolderName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
}
