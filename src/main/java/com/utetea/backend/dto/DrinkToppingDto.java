package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DrinkToppingDto {
    private Long id;
    private String toppingName;
    private BigDecimal price;
    private Boolean isActive;
}
