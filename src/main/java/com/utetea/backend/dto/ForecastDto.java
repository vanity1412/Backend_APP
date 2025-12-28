package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO cho tính năng Dự báo doanh thu & Nguy cơ quá tải
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDto {
    
    // Dự báo doanh thu
    private RevenueForecast revenueForecast;
    
    // Phân tích giờ cao điểm
    private List<PeakHourAnalysis> peakHours;
    
    // Cảnh báo món sắp hết (dựa trên tốc độ bán)
    private List<LowStockWarning> lowStockWarnings;
    
    // Đề xuất nhân sự theo ngày
    private List<StaffingRecommendation> staffingRecommendations;
    
    // Cảnh báo quá tải
    private List<OverloadWarning> overloadWarnings;
    
    // ==================== INNER CLASSES ====================
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueForecast {
        private BigDecimal todayForecast;           // Dự báo doanh thu hôm nay
        private BigDecimal tomorrowForecast;        // Dự báo ngày mai
        private BigDecimal weekForecast;            // Dự báo 7 ngày tới
        private BigDecimal monthForecast;           // Dự báo tháng này
        private Double growthRate;                  // Tỷ lệ tăng trưởng (%)
        private String trend;                       // UP, DOWN, STABLE
        private List<DailyForecast> dailyForecasts; // Chi tiết từng ngày
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private LocalDate date;
        private String dayOfWeek;
        private BigDecimal forecastRevenue;
        private Long forecastOrders;
        private Double confidence;  // Độ tin cậy (0-100%)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHourAnalysis {
        private Integer hour;               // Giờ (0-23)
        private String timeRange;           // "10:00 - 11:00"
        private Long avgOrders;             // Số đơn trung bình
        private BigDecimal avgRevenue;      // Doanh thu trung bình
        private String peakLevel;           // LOW, MEDIUM, HIGH, VERY_HIGH
        private Integer recommendedStaff;   // Số nhân viên đề xuất
    }

    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockWarning {
        private Long drinkId;
        private String drinkName;
        private String imageUrl;
        private Long soldToday;             // Số lượng bán hôm nay
        private Long avgDailySales;         // Trung bình bán/ngày
        private Double salesVelocity;       // Tốc độ bán (đơn/giờ)
        private String warningLevel;        // WARNING, CRITICAL
        private String message;             // "Dự kiến hết hàng trong 2 giờ"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffingRecommendation {
        private LocalDate date;
        private String dayOfWeek;
        private Integer currentStaff;       // Số nhân viên hiện tại (nếu có)
        private Integer recommendedStaff;   // Số nhân viên đề xuất
        private Integer additionalNeeded;   // Số nhân viên cần thêm
        private String reason;              // Lý do đề xuất
        private List<HourlyStaffing> hourlyBreakdown; // Chi tiết theo giờ
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStaffing {
        private Integer hour;
        private String timeRange;
        private Integer recommendedStaff;
        private Long expectedOrders;
        private String loadLevel;           // LOW, MEDIUM, HIGH
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverloadWarning {
        private LocalDate date;
        private Integer hour;
        private String timeRange;
        private Long expectedOrders;
        private Long maxCapacity;           // Công suất tối đa
        private Double overloadPercent;     // % vượt công suất
        private String severity;            // WARNING, CRITICAL
        private String recommendation;      // Đề xuất xử lý
    }
}
