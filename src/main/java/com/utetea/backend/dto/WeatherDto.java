package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho dữ liệu thời tiết từ OpenWeatherMap API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDto {
    
    private String city;                    // Tên thành phố
    private String country;                 // Mã quốc gia
    private Double temperature;             // Nhiệt độ (°C)
    private Double feelsLike;               // Cảm giác như (°C)
    private Double tempMin;                 // Nhiệt độ thấp nhất
    private Double tempMax;                 // Nhiệt độ cao nhất
    private Integer humidity;               // Độ ẩm (%)
    private String condition;               // Điều kiện: Clear, Clouds, Rain, etc.
    private String description;             // Mô tả chi tiết
    private String icon;                    // Icon code từ OpenWeatherMap
    private String iconUrl;                 // URL đầy đủ của icon
    private Double windSpeed;               // Tốc độ gió (m/s)
    private Integer pressure;               // Áp suất (hPa)
    private Integer visibility;             // Tầm nhìn (m)
    private Long sunrise;                   // Thời gian mặt trời mọc (Unix timestamp)
    private Long sunset;                    // Thời gian mặt trời lặn (Unix timestamp)
    private Long timestamp;                 // Thời gian cập nhật
    
    // Gợi ý kinh doanh dựa trên thời tiết
    private WeatherBusinessInsight businessInsight;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherBusinessInsight {
        private String weatherType;         // HOT, COLD, RAINY, NORMAL
        private String recommendation;      // Gợi ý cho manager
        private String[] suggestedDrinks;   // Các loại đồ uống nên đẩy mạnh
        private Double expectedImpact;      // Dự kiến ảnh hưởng doanh thu (%)
    }
}
