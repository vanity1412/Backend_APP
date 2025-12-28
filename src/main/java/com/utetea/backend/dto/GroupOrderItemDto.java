package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrderItemDto {
    private Long id;
    private Long userId;
    private String userName;
    
    private Long drinkId;
    private String drinkName;
    private String drinkImage;
    
    private String sizeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal itemPrice;
    private String note;
    
    private List<Long> toppingIds;
    private String toppingsSnapshot;
}
