package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long canceledOrders;
    private List<TopSellingDrinkDto> topSellingDrinks;
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TopSellingDrinkDto {
        private String drinkName;
        private Long totalSold;
        private BigDecimal revenue;
    }
}
