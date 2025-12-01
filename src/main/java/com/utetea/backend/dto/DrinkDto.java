package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DrinkDto {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal basePrice;
    private Boolean isActive;
    
    // ✅ FIX VẤN ĐỀ 2: Thêm categoryId và categoryName để Android filter được drinks
    private Long categoryId;
    private String categoryName;
    
    private List<DrinkSizeDto> sizes;
    private List<DrinkToppingDto> toppings;
}
