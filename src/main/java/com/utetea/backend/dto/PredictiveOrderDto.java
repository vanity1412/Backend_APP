package com.utetea.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO cho tính năng Predictive Order - Dự đoán món khách hàng muốn đặt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictiveOrderDto {
    
    private boolean hasPrediction;
    private String message;
    private PredictedDrink predictedDrink;
    private List<String> triggerReasons;
    private double confidenceScore; // 0.0 - 1.0
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictedDrink {
        private Long drinkId;
        private String drinkName;
        private String drinkImage;
        private String sizeName;
        private Long sizeId;
        private BigDecimal price;
        private Integer orderCount; // Số lần đã đặt món này
        private String lastOrderTime; // Lần cuối đặt
        private List<PredictedTopping> toppings;
        private String note; // Ghi chú thường dùng
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictedTopping {
        private Long toppingId;
        private String toppingName;
        private BigDecimal price;
    }
}
