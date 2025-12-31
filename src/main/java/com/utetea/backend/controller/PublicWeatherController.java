package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.WeatherDto;
import com.utetea.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller công khai cho API thời tiết (không cần đăng nhập)
 * Dùng cho tất cả user trên Home screen
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class PublicWeatherController {
    
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
     */
    @GetMapping("/city")
    public ResponseEntity<ApiResponse<WeatherDto>> getWeatherByCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "VN") String country) {
        WeatherDto weather = weatherService.getCurrentWeather(city, country);
        return ResponseEntity.ok(ApiResponse.success(weather));
    }
}
