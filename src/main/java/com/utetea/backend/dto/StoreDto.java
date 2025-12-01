package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StoreDto {
    private Long id;
    private String storeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String phone;
}
