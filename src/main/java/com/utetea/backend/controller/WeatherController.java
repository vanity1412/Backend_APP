package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.WeatherDto;
import com.utetea.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho API thời tiết
 * Dùng cho Manager Dashboard để hiển thị dự báo và gợi ý kinh doanh
 */
@RestController
@RequestMapping("/api/manager/weather")
@RequiredArgsConstructor
public class WeatherController {
    
    private final WeatherService weatherService;
    
    /**
     * Lấy thời tiết hiện tại (mặc định TP.HCM)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeatherDto>> getCurrentWeather() {
        WeatherDto weather = weatherService.getCurrentWeather();
        return ResponseEntity.ok(ApiResponse.success(weather));
    }
    
    /**
     * Lấy thời tiết theo thành phố
     * @param city Tên thành phố (VD: "Ha Noi", "Da Nang")
     * @param country Mã quốc gia (mặc định: VN)
     */
    @GetMapping("/city")
    public ResponseEntity<ApiResponse<WeatherDto>> getWeatherByCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "VN") String country) {
        WeatherDto weather = weatherService.getCurrentWeather(city, country);
        return ResponseEntity.ok(ApiResponse.success(weather));
    }
}
