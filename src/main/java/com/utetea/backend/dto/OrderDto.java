package com.utetea.backend.dto;

import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.OrderType;
import com.utetea.backend.model.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderDto {
    private Long id;
    private Long userId;
    private String userName;
    private Long storeId;
    private String storeName;
    private OrderType type;
    private String address;
    private LocalDateTime pickupTime;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private BigDecimal discount;
    private BigDecimal finalPrice;
    private PaymentMethod paymentMethod;
    private String promotionCode;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
