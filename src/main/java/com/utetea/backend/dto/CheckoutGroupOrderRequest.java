package com.utetea.backend.dto;

import com.utetea.backend.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CheckoutGroupOrderRequest {
    
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;
    
    private String promotionCode;
    
    private String spinVoucherCode;
}
