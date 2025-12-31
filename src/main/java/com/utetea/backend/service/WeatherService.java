package com.utetea.backend.service;

import com.utetea.backend.dto.WeatherDto;
import com.utetea.backend.dto.WeatherDto.WeatherBusinessInsight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service lấy dữ liệu thời tiết từ OpenWeatherMap API
 * và phân tích ảnh hưởng đến kinh doanh
 */
@Service
@Slf4j
public class WeatherService {
    
    @Value("${weather.api.key:}")
    private String apiKey;
    
    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;
    
    @Value("${weather.default.city:Ho Chi Minh City}")
    private String defaultCity;
    
    @Value("${weather.default.country:VN}")
    private String defaultCountry;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Ngưỡng nhiệt độ
    private static final double HOT_THRESHOLD = 32.0;
    private static final double WARM_THRESHOLD = 28.0;
    private static final double COOL_THRESHOLD = 22.0;
    
    /**
     * Lấy thời tiết hiện tại (có cache 30 phút)
     */
    @Cacheable(value = "weather", key = "#city + '_' + #countryCode", unless = "#result == null")
    public WeatherDto getCurrentWeather(String city, String countryCode) {
        if (city == null || city.isEmpty()) {
            city = defaultCity;
        }
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = defaultCountry;
        }
        
        log.info("Fetching weather for {}, {}", city, countryCode);
        
        // Nếu không có API key, trả về mock data
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Weather API key not configured, returning mock data");
            return getMockWeatherData(city, countryCode);
        }
        
        try {
            String url = String.format("%s?q=%s,%s&appid=%s&units=metric&lang=vi",
                    apiUrl, city, countryCode, apiKey);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() != null) {
                return parseWeatherResponse(response.getBody(), city, countryCode);
            }
        } catch (Exception e) {
            log.error("Error fetching weather data: {}", e.getMessage());
        }
        
        return getMockWeatherData(city, countryCode);
    }
    
    /**
     * Lấy thời tiết mặc định (TP.HCM)
     */
    public WeatherDto getCurrentWeather() {
        return getCurrentWeather(defaultCity, defaultCountry);
    }
    
    /**
     * Parse response từ OpenWeatherMap
     */
    @SuppressWarnings("unchecked")
    private WeatherDto parseWeatherResponse(Map<String, Object> response, String city, String country) {
        Map<String, Object> main = (Map<String, Object>) response.get("main");
        Map<String, Object> wind = (Map<String, Object>) response.get("wind");
        Map<String, Object> sys = (Map<String, Object>) response.get("sys");
        java.util.List<Map<String, Object>> weatherList = (java.util.List<Map<String, Object>>) response.get("weather");
        Map<String, Object> weather = weatherList.get(0);
        
        Double temp = ((Number) main.get("temp")).doubleValue();
        String condition = (String) weather.get("main");
        String icon = (String) weather.get("icon");
        
        WeatherDto dto = WeatherDto.builder()
                .city(city)
                .country(country)
                .temperature(temp)
                .feelsLike(((Number) main.get("feels_like")).doubleValue())
                .tempMin(((Number) main.get("temp_min")).doubleValue())
                .tempMax(((Number) main.get("temp_max")).doubleValue())
                .humidity(((Number) main.get("humidity")).intValue())
                .condition(condition)
                .description((String) weather.get("description"))
                .icon(icon)
                .iconUrl("https://openweathermap.org/img/wn/" + icon + "@2x.png")
                .windSpeed(wind != null ? ((Number) wind.get("speed")).doubleValue() : 0.0)
                .pressure(((Number) main.get("pressure")).intValue())
                .visibility(response.get("visibility") != null ? ((Number) response.get("visibility")).intValue() : 10000)
                .sunrise(sys != null ? ((Number) sys.get("sunrise")).longValue() : 0L)
                .sunset(sys != null ? ((Number) sys.get("sunset")).longValue() : 0L)
                .timestamp(System.currentTimeMillis() / 1000)
                .build();
        
        // Thêm business insight
        dto.setBusinessInsight(generateBusinessInsight(temp, condition));
        
        return dto;
    }
    
    /**
     * Tạo gợi ý kinh doanh dựa trên thời tiết
     */
    private WeatherBusinessInsight generateBusinessInsight(Double temperature, String condition) {
        String weatherType;
        String recommendation;
        String[] suggestedDrinks;
        Double expectedImpact;
        
        // Xác định loại thời tiết
        boolean isRainy = condition != null && 
                (condition.equalsIgnoreCase("Rain") || 
                 condition.equalsIgnoreCase("Drizzle") ||
                 condition.equalsIgnoreCase("Thunderstorm"));
        
        if (isRainy) {
            weatherType = "RAINY";
            recommendation = "Thời tiết mưa - Khách có xu hướng ở nhà. Đẩy mạnh delivery và đồ uống ấm.";
            suggestedDrinks = new String[]{"Trà sữa nóng", "Cacao nóng", "Trà gừng", "Cà phê nóng"};
            expectedImpact = -15.0; // Giảm 15% doanh thu tại quán
        } else if (temperature >= HOT_THRESHOLD) {
            weatherType = "HOT";
            recommendation = "Thời tiết nóng - Nhu cầu đồ uống mát tăng cao. Chuẩn bị đá và nguyên liệu đủ.";
            suggestedDrinks = new String[]{"Trà đào", "Trà vải", "Sinh tố", "Đá xay", "Trà sữa đá"};
            expectedImpact = 20.0; // Tăng 20%
        } else if (temperature >= WARM_THRESHOLD) {
            weatherType = "WARM";
            recommendation = "Thời tiết ấm áp - Điều kiện lý tưởng cho kinh doanh.";
            suggestedDrinks = new String[]{"Trà sữa", "Trà trái cây", "Matcha đá"};
            expectedImpact = 10.0;
        } else if (temperature < COOL_THRESHOLD) {
            weatherType = "COLD";
            recommendation = "Thời tiết se lạnh - Đẩy mạnh đồ uống nóng và ấm.";
            suggestedDrinks = new String[]{"Trà sữa nóng", "Cacao nóng", "Cà phê sữa nóng", "Trà gừng mật ong"};
            expectedImpact = 5.0;
        } else {
            weatherType = "NORMAL";
            recommendation = "Thời tiết bình thường - Duy trì hoạt động như thường lệ.";
            suggestedDrinks = new String[]{"Trà sữa truyền thống", "Trà đào", "Matcha"};
            expectedImpact = 0.0;
        }
        
        return WeatherBusinessInsight.builder()
                .weatherType(weatherType)
                .recommendation(recommendation)
                .suggestedDrinks(suggestedDrinks)
                .expectedImpact(expectedImpact)
                .build();
    }
    
    /**
     * Mock data khi không có API key
     */
    private WeatherDto getMockWeatherData(String city, String country) {
        // Giả lập thời tiết TP.HCM (thường nóng)
        double mockTemp = 30.0 + Math.random() * 5; // 30-35°C
        
        return WeatherDto.builder()
                .city(city)
                .country(country)
                .temperature(Math.round(mockTemp * 10.0) / 10.0)
                .feelsLike(mockTemp + 2)
                .tempMin(mockTemp - 2)
                .tempMax(mockTemp + 3)
                .humidity(70 + (int)(Math.random() * 20))
                .condition("Clouds")
                .description("mây rải rác")
                .icon("03d")
                .iconUrl("https://openweathermap.org/img/wn/03d@2x.png")
                .windSpeed(2.0 + Math.random() * 3)
                .pressure(1010)
                .visibility(10000)
                .sunrise(System.currentTimeMillis() / 1000 - 21600) // 6 giờ trước
                .sunset(System.currentTimeMillis() / 1000 + 21600)  // 6 giờ sau
                .timestamp(System.currentTimeMillis() / 1000)
                .businessInsight(generateBusinessInsight(mockTemp, "Clouds"))
                .build();
    }
}
