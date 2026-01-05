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
    
    private String spinVoucherCode; // Mã voucher từ spin wheel
    
    // GHN Shipping Info
    private Integer ghnDistrictId;  // District ID cho GHN
    private String ghnWardCode;     // Ward code cho GHN
    
    // Shipping Fee từ client
    private Integer shippingFee;    // Phí ship tính từ client
    
    @NotNull
    private List<OrderItemRequest> items;
}
