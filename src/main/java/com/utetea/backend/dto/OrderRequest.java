package com.utetea.backend.dto;

import com.utetea.backend.model.OrderType;
import com.utetea.backend.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotNull
    private Long storeId;
    
    @NotNull
    private OrderType type;
    
    private String address;
    private LocalDateTime pickupTime;
    
    @NotNull
    private PaymentMethod paymentMethod;
    
    private String promotionCode;
    
    @NotNull
    private List<OrderItemRequest> items;
}
