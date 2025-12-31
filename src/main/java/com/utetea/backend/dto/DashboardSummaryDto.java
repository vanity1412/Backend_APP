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
    private List<TopRatedDrinkDto> topRatedDrinks;
    
    // Thông tin về stores được quản lý (cho Manager)
    private List<ManagedStoreInfo> managedStores;
    private Boolean isAdmin;
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TopSellingDrinkDto {
        private String drinkName;
        private Long totalSold;
        private BigDecimal revenue;
    }
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TopRatedDrinkDto {
        private Long drinkId;
        private String drinkName;
        private String drinkImage;
        private Double averageRating;
        private Long totalReviews;
    }
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ManagedStoreInfo {
        private Long id;
        private String storeName;
        private String address;
    }
}
