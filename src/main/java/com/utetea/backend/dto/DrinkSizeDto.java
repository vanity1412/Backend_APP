package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DrinkSizeDto {
    private Long id;
    private String sizeName;
    private BigDecimal extraPrice;
}
