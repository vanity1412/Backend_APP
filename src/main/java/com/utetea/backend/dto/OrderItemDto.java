package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderItemDto {
    private Long id;
    private String drinkName;
    private String drinkImage; // FIX: Thêm trường ảnh sản phẩm
    private String sizeName;
    private Integer quantity;
    private BigDecimal itemPrice;
    private String note;
    private List<OrderItemToppingDto> toppings;
}
