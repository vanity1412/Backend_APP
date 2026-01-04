package com.utetea.backend.dto;

import lombok.Data;

@Data
public class ZaloPayPaymentRequest {
    private Long amount;
    private String description;
    private Long orderId;
}
