package com.utetea.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @Size(max = 255, message = "Order info must not exceed 255 characters")
    private String orderInfo;
    
    private String ipAddress; // Set by server
}
