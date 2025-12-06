package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsDto {
    private BigDecimal totalRevenue;
    private List<DailyRevenue> dailyRevenues;
    private List<MonthlyRevenue> monthlyRevenues;
    private List<TopSellingDrink> topSellingDrinks;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
        private Long orderCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private Integer year;
        private Integer month;
        private BigDecimal revenue;
        private Long orderCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingDrink {
        private Long drinkId;
        private String drinkName;
        private String imageUrl;
        private Long totalQuantity;
        private BigDecimal totalRevenue;
    }
}
