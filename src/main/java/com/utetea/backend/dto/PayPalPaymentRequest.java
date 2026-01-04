package com.utetea.backend.dto;

import lombok.Data;

@Data
public class PayPalPaymentRequest {
    private String total;
    private String currency = "USD";
    private String description;
    private Long orderId;
}
