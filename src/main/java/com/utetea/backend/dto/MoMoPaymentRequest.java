package com.utetea.backend.dto;

import lombok.Data;

@Data
public class MoMoPaymentRequest {
    private String amount;
    private String orderInfo;
    private Long orderId;
}
