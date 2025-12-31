package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreWithManagersDto {
    private Long id;
    private String storeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String phone;
    
    // Danh sách managers quản lý store này
    private List<UserDto> managers;
    
    // Danh sách admins (để liên hệ)
    private List<UserDto> admins;
}
