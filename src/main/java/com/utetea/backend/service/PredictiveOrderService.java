package com.utetea.backend.service;

import com.utetea.backend.dto.PredictiveOrderDto;
import com.utetea.backend.dto.PredictiveOrderDto.*;
import com.utetea.backend.dto.WeatherDto;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service phân tích và dự đoán món khách hàng muốn đặt
 * Dựa trên: thói quen thời gian, tần suất đặt, thời tiết (optional)
 * Nếu không có lịch sử → gợi ý sản phẩm phổ biến nhất theo thời tiết
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveOrderService {
    
    private final OrderRepository orderRepository;
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final WeatherService weatherService;
    
    // Ngưỡng confidence để hiển thị prediction (giảm để dễ test)
    private static final double MIN_CONFIDENCE = 0.2;
    
    // Cache thời tiết
    private WeatherDto cachedWeather;
    private long weatherCacheTime = 0;
    private static final long WEATHER_CACHE_DURATION = 30 * 60 * 1000; // 30 phút
    
    // Keywords cho từng loại thời tiết
    private static final List<String> HOT_WEATHER_KEYWORDS = Arrays.asList(
        "đá", "ice", "lạnh", "freeze", "sinh tố", "smoothie", "chanh", "dừa", 
        "đào", "vải", "xoài", "dâu", "việt quất", "trái cây"
    );
    
    private static final List<String> COLD_WEATHER_KEYWORDS = Arrays.asList(
        "nóng", "hot", "ấm", "cacao", "gừng", "cà phê", "coffee", "sữa nóng",
        "trà nóng", "matcha nóng", "cappuccino", "latte"
    );
    
    private static final List<String> RAINY_WEATHER_KEYWORDS = Arrays.asList(
        "nóng", "hot", "ấm", "cacao", "gừng", "trà gừng", "sữa nóng"
    );
    
    private static final List<String> NORMAL_WEATHER_KEYWORDS = Arrays.asList(
        "trà sữa", "milk tea", "trân châu", "matcha", "oolong", "trà đào"
    );
    
    /**
     * Dự đoán món khách hàng muốn đặt
     * Luôn trả về gợi ý - nếu không có lịch sử thì gợi ý sản phẩm phổ biến theo thời tiết
     */
    public PredictiveOrderDto getPrediction(Long userId, String weatherCondition) {
        try {
            log.info("Getting prediction for userId: {}", userId);
            
            // Lấy thời tiết thực tế nếu không truyền vào
            WeatherDto weather = getWeatherData();
            String actualWeather = weatherCondition;
            if ((actualWeather == null || actualWeather.isEmpty()) && weather != null) {
                actualWeather = determineWeatherType(weather);
                log.info("Auto-detected weather: {} (temp: {}°C, condition: {})", 
                    actualWeather, weather.getTemperature(), weather.getCondition());
            }
            
            List<Order> orders;
            try {
                orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(userId);
                log.info("Found {} orders for user {}", orders.size(), userId);
            } catch (Exception e) {
                log.error("Error fetching orders for user {}: {}", userId, e.getMessage());
                // Fallback: dùng query đơn giản hơn
                orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                log.info("Fallback: Found {} orders for user {}", orders.size(), userId);
            }
            
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();
            DayOfWeek currentDay = now.getDayOfWeek();
            
            // Nếu không có lịch sử đặt hàng → gợi ý sản phẩm phổ biến theo thời tiết
            if (orders == null || orders.isEmpty()) {
                log.info("No orders found for user {}, returning weather-based recommendation", userId);
                return getWeatherBasedPrediction(weather, actualWeather);
            }
            
            // Phân tích patterns
            Map<Long, DrinkPattern> drinkPatterns = analyzeDrinkPatterns(orders, currentHour, currentDay);
            
            // Nếu không tìm thấy pattern → gợi ý sản phẩm theo thời tiết
            if (drinkPatterns == null || drinkPatterns.isEmpty()) {
                log.info("No drink patterns found for user, returning weather-based recommendation");
                return getWeatherBasedPrediction(weather, actualWeather);
            }
            
            // Tìm drink có score cao nhất
            DrinkPattern bestPattern = drinkPatterns.values().stream()
                    .max(Comparator.comparingDouble(DrinkPattern::getScore))
                    .orElse(null);
            
            log.info("Best pattern: drink={}, score={}", 
                    bestPattern != null ? bestPattern.getDrinkName() : "null",
                    bestPattern != null ? bestPattern.getScore() : 0);
            
            // Nếu confidence quá thấp → gợi ý sản phẩm theo thời tiết
            if (bestPattern == null || bestPattern.getScore() < MIN_CONFIDENCE) {
                log.info("Confidence too low: {} < {}, returning weather-based recommendation", 
                        bestPattern != null ? bestPattern.getScore() : 0, MIN_CONFIDENCE);
                return getWeatherBasedPrediction(weather, actualWeather);
            }
            
            // Tìm sizeId từ sizeName và drinkId
            if (bestPattern.getSizeName() != null && !bestPattern.getSizeName().isEmpty()) {
                drinkSizeRepository.findByDrinkIdAndSizeName(bestPattern.getDrinkId(), bestPattern.getSizeName())
                        .ifPresent(size -> bestPattern.setSizeId(size.getId()));
                log.info("Found sizeId: {} for sizeName: {} and drinkId: {}", 
                        bestPattern.sizeId, bestPattern.getSizeName(), bestPattern.getDrinkId());
            }
            
            // Nếu không tìm được sizeId, lấy size đầu tiên của drink
            if (bestPattern.sizeId == null) {
                List<com.utetea.backend.model.DrinkSize> sizes = drinkSizeRepository.findByDrinkId(bestPattern.getDrinkId());
                if (!sizes.isEmpty()) {
                    bestPattern.setSizeId(sizes.get(0).getId());
                    log.info("Using default sizeId: {} for drink: {} (total sizes: {})", 
                            bestPattern.sizeId, bestPattern.getDrinkId(), sizes.size());
                } else {
                    log.warn("No sizes found for drink: {}", bestPattern.getDrinkId());
                }
            }
            
            // Build prediction response
            List<String> reasons = buildTriggerReasons(bestPattern, currentHour, currentDay, actualWeather, weather);
            
            return PredictiveOrderDto.builder()
                    .hasPrediction(true)
                    .message(buildPredictionMessage(bestPattern))
                    .predictedDrink(bestPattern.toPredictedDrink())
                    .triggerReasons(reasons)
                    .confidenceScore(bestPattern.getScore())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in getPrediction for userId {}: {}", userId, e.getMessage(), e);
            // Trả về gợi ý theo thời tiết khi có lỗi
            return getWeatherBasedPrediction(getWeatherData(), weatherCondition);
        }
    }
    
    /**
     * Lấy dữ liệu thời tiết (có cache)
     */
    private WeatherDto getWeatherData() {
        long now = System.currentTimeMillis();
        if (cachedWeather == null || (now - weatherCacheTime) > WEATHER_CACHE_DURATION) {
            try {
                cachedWeather = weatherService.getCurrentWeather();
                weatherCacheTime = now;
                log.info("Fetched weather: {}°C, {}", 
                    cachedWeather.getTemperature(), cachedWeather.getCondition());
            } catch (Exception e) {
                log.warn("Could not fetch weather data: {}", e.getMessage());
                return null;
            }
        }
        return cachedWeather;
    }
    
    /**
     * Xác định loại thời tiết từ WeatherDto
     */
    private String determineWeatherType(WeatherDto weather) {
        if (weather == null) return "normal";
        
        String condition = weather.getCondition();
        double temp = weather.getTemperature();
        
        // Kiểm tra mưa
        if (condition != null && 
            (condition.equalsIgnoreCase("Rain") || 
             condition.equalsIgnoreCase("Drizzle") ||
             condition.equalsIgnoreCase("Thunderstorm"))) {
            return "rainy";
        }
        
        // Kiểm tra nhiệt độ
        if (temp >= 32) return "hot";
        if (temp < 22) return "cold";
        
        return "normal";
    }
    
    /**
     * Gợi ý sản phẩm dựa trên thời tiết thực tế
     */
    private PredictiveOrderDto getWeatherBasedPrediction(WeatherDto weather, String weatherType) {
        try {
            log.info("Getting weather-based prediction: type={}", weatherType);
            
            List<Drink> activeDrinks = drinkRepository.findByIsActiveTrue();
            
            if (activeDrinks == null || activeDrinks.isEmpty()) {
                log.warn("No drinks available for prediction");
                return PredictiveOrderDto.builder()
                        .hasPrediction(false)
                        .message("Không có sản phẩm nào để gợi ý")
                        .build();
            }
            
            // Lọc đồ uống theo thời tiết
            List<Drink> filteredDrinks = filterDrinksByWeather(activeDrinks, weatherType);
            
            // Random chọn 1 món từ danh sách đã lọc
            Random random = new Random();
            Drink selectedDrink;
            if (!filteredDrinks.isEmpty()) {
                selectedDrink = filteredDrinks.get(random.nextInt(filteredDrinks.size()));
            } else {
                // Fallback: random từ tất cả
                selectedDrink = activeDrinks.get(random.nextInt(activeDrinks.size()));
            }
            
            log.info("Selected drink for weather {}: {}", weatherType, selectedDrink.getName());
            
            // Lấy size mặc định
            List<com.utetea.backend.model.DrinkSize> sizes = drinkSizeRepository.findByDrinkId(selectedDrink.getId());
            Long sizeId = null;
            String sizeName = null;
            if (sizes != null && !sizes.isEmpty()) {
                sizeId = sizes.get(0).getId();
                sizeName = sizes.get(0).getSizeName();
            }
            
            // Build reasons dựa trên thời tiết thực tế
            List<String> reasons = buildWeatherReasons(weather, weatherType, selectedDrink);
            
            // Build message
            String message = buildWeatherMessage(weather, weatherType, selectedDrink);
            
            PredictedDrink predictedDrink = PredictedDrink.builder()
                    .drinkId(selectedDrink.getId())
                    .drinkName(selectedDrink.getName())
                    .drinkImage(selectedDrink.getImageUrl())
                    .sizeName(sizeName)
                    .sizeId(sizeId)
                    .price(selectedDrink.getBasePrice())
                    .orderCount(0)
                    .lastOrderTime(null)
                    .toppings(new ArrayList<>())
                    .note(null)
                    .build();
            
            return PredictiveOrderDto.builder()
                    .hasPrediction(true)
                    .message(message)
                    .predictedDrink(predictedDrink)
                    .triggerReasons(reasons)
                    .confidenceScore(0.75)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in getWeatherBasedPrediction: {}", e.getMessage(), e);
            return PredictiveOrderDto.builder()
                    .hasPrediction(false)
                    .message("Không thể tải gợi ý")
                    .build();
        }
    }
    
    /**
     * Lọc đồ uống theo loại thời tiết
     */
    private List<Drink> filterDrinksByWeather(List<Drink> drinks, String weatherType) {
        List<String> keywords;
        
        switch (weatherType != null ? weatherType.toLowerCase() : "normal") {
            case "hot":
            case "sunny":
                keywords = HOT_WEATHER_KEYWORDS;
                break;
            case "cold":
                keywords = COLD_WEATHER_KEYWORDS;
                break;
            case "rainy":
            case "rain":
                keywords = RAINY_WEATHER_KEYWORDS;
                break;
            default:
                keywords = NORMAL_WEATHER_KEYWORDS;
        }
        
        return drinks.stream()
            .filter(d -> {
                String name = d.getName().toLowerCase();
                String desc = d.getDescription() != null ? d.getDescription().toLowerCase() : "";
                return keywords.stream().anyMatch(k -> name.contains(k) || desc.contains(k));
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Build reasons dựa trên thời tiết
     */
    private List<String> buildWeatherReasons(WeatherDto weather, String weatherType, Drink drink) {
        List<String> reasons = new ArrayList<>();
        
        if (weather != null) {
            String tempInfo = String.format("%.0f°C", weather.getTemperature());
            
            switch (weatherType != null ? weatherType.toLowerCase() : "normal") {
                case "hot":
                case "sunny":
                    reasons.add("🌡️ Nhiệt độ hiện tại: " + tempInfo + " - Trời nóng!");
                    reasons.add("🧊 " + drink.getName() + " mát lạnh giúp giải nhiệt");
                    reasons.add("💧 Đồ uống này rất phù hợp với thời tiết nóng");
                    break;
                case "cold":
                    reasons.add("🌡️ Nhiệt độ hiện tại: " + tempInfo + " - Trời se lạnh");
                    reasons.add("☕ " + drink.getName() + " ấm áp cho ngày lạnh");
                    reasons.add("🔥 Đồ uống nóng giúp bạn ấm người");
                    break;
                case "rainy":
                case "rain":
                    reasons.add("🌧️ Trời đang mưa - " + tempInfo);
                    reasons.add("☕ " + drink.getName() + " ấm áp cho ngày mưa");
                    reasons.add("🏠 Thưởng thức đồ uống ấm trong ngày mưa thật tuyệt!");
                    break;
                default:
                    reasons.add("🌤️ Thời tiết dễ chịu - " + tempInfo);
                    reasons.add("🧋 " + drink.getName() + " là lựa chọn tuyệt vời");
                    reasons.add("⭐ Đây là món được yêu thích");
            }
            
            if (weather.getDescription() != null) {
                reasons.add("📍 " + weather.getCity() + ": " + weather.getDescription());
            }
        } else {
            // Fallback khi không có weather
            int hour = LocalTime.now().getHour();
            if (hour < 10) {
                reasons.add("🌅 Buổi sáng tươi mới!");
                reasons.add("☕ " + drink.getName() + " để bắt đầu ngày mới");
            } else if (hour < 14) {
                reasons.add("🌞 Giữa trưa rồi!");
                reasons.add("🧊 " + drink.getName() + " giải khát buổi trưa");
            } else if (hour < 18) {
                reasons.add("🌤️ Buổi chiều thư giãn!");
                reasons.add("🧋 " + drink.getName() + " cho buổi chiều");
            } else {
                reasons.add("🌙 Buổi tối yên bình!");
                reasons.add("🍵 " + drink.getName() + " thư giãn cuối ngày");
            }
        }
        
        return reasons;
    }
    
    /**
     * Build message gợi ý theo thời tiết
     */
    private String buildWeatherMessage(WeatherDto weather, String weatherType, Drink drink) {
        if (weather != null) {
            String tempInfo = String.format("%.0f°C", weather.getTemperature());
            
            switch (weatherType != null ? weatherType.toLowerCase() : "normal") {
                case "hot":
                case "sunny":
                    return "🥵 Trời nóng " + tempInfo + "! Thử " + drink.getName() + " mát lạnh nhé?";
                case "cold":
                    return "🥶 Trời lạnh " + tempInfo + "! " + drink.getName() + " ấm áp cho bạn?";
                case "rainy":
                case "rain":
                    return "🌧️ Trời mưa rồi! " + drink.getName() + " ấm nóng nhé?";
                default:
                    return "🌤️ Thời tiết đẹp! Thử " + drink.getName() + " không?";
            }
        }
        return "Bạn có muốn thử " + drink.getName() + " không?";
    }
    
    /**
     * Lấy gợi ý sản phẩm phổ biến nhất khi không có lịch sử
     * @deprecated Sử dụng getWeatherBasedPrediction thay thế
     */
    @Deprecated
    private PredictiveOrderDto getPopularDrinkPrediction(String weatherCondition) {
        return getWeatherBasedPrediction(getWeatherData(), weatherCondition);
    }

    
    /**
     * Phân tích patterns từ order history
     */
    private Map<Long, DrinkPattern> analyzeDrinkPatterns(
            List<Order> orders, int currentHour, DayOfWeek currentDay) {
        
        Map<Long, DrinkPattern> patterns = new HashMap<>();
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        // Chỉ phân tích đơn hàng DONE
        List<Order> completedOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DONE)
                .collect(Collectors.toList());
        
        log.info("Analyzing {} completed orders out of {} total", completedOrders.size(), orders.size());
        
        if (completedOrders.isEmpty()) {
            log.info("No completed orders to analyze");
            return patterns;
        }
        
        for (Order order : completedOrders) {
            Instant orderInstant = order.getCreatedAt();
            LocalDateTime orderTime = LocalDateTime.ofInstant(orderInstant, zoneId);
            int orderHour = orderTime.getHour();
            DayOfWeek orderDay = orderTime.getDayOfWeek();
            long daysSinceOrder = ChronoUnit.DAYS.between(orderInstant, now);
            
            if (order.getItems() == null || order.getItems().isEmpty()) {
                log.warn("Order {} has no items", order.getId());
                continue;
            }
            
            for (OrderItem item : order.getItems()) {
                if (item.getDrink() == null) {
                    log.warn("OrderItem {} has no drink", item.getId());
                    continue;
                }
                
                Long drinkId = item.getDrink().getId();
                
                DrinkPattern pattern = patterns.computeIfAbsent(drinkId, 
                        k -> new DrinkPattern(item.getDrink(), item));
                
                pattern.incrementOrderCount();
                pattern.addOrderTime(orderHour);
                pattern.addOrderDay(orderDay);
                pattern.updateLastOrder(orderInstant);
                
                // Tính điểm dựa trên các yếu tố
                double timeScore = calculateTimeScore(orderHour, currentHour);
                double dayScore = calculateDayScore(orderDay, currentDay);
                double recencyScore = calculateRecencyScore(daysSinceOrder);
                double frequencyScore = pattern.getOrderCount() / (double) completedOrders.size();
                
                // Weighted score
                double score = (timeScore * 0.35) + (dayScore * 0.25) + 
                               (recencyScore * 0.2) + (frequencyScore * 0.2);
                
                pattern.updateScore(score);
            }
        }
        
        log.info("Found {} drink patterns", patterns.size());
        return patterns;
    }
    
    /**
     * Tính điểm dựa trên khung giờ tương tự
     */
    private double calculateTimeScore(int orderHour, int currentHour) {
        int diff = Math.abs(orderHour - currentHour);
        if (diff <= 1) return 1.0;
        if (diff <= 2) return 0.8;
        if (diff <= 3) return 0.5;
        return 0.2;
    }
    
    /**
     * Tính điểm dựa trên ngày trong tuần
     */
    private double calculateDayScore(DayOfWeek orderDay, DayOfWeek currentDay) {
        if (orderDay == currentDay) return 1.0;
        // Weekend vs Weekday
        boolean orderWeekend = orderDay == DayOfWeek.SATURDAY || orderDay == DayOfWeek.SUNDAY;
        boolean currentWeekend = currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY;
        if (orderWeekend == currentWeekend) return 0.7;
        return 0.3;
    }
    
    /**
     * Tính điểm dựa trên độ gần đây của order
     */
    private double calculateRecencyScore(long daysSinceOrder) {
        if (daysSinceOrder <= 3) return 1.0;
        if (daysSinceOrder <= 7) return 0.8;
        if (daysSinceOrder <= 14) return 0.6;
        if (daysSinceOrder <= 30) return 0.4;
        return 0.2;
    }
    
    /**
     * Build message gợi ý
     */
    private String buildPredictionMessage(DrinkPattern pattern) {
        return String.format("Có phải bạn đang muốn gọi lại %s %s không?",
                pattern.getDrinkName(),
                pattern.getSizeName() != null ? "size " + pattern.getSizeName() : "");
    }
    
    /**
     * Build danh sách lý do trigger prediction
     */
    private List<String> buildTriggerReasons(DrinkPattern pattern, int currentHour, 
            DayOfWeek currentDay, String weather, WeatherDto weatherDto) {
        List<String> reasons = new ArrayList<>();
        
        // Lý do về thời gian
        String timeOfDay = getTimeOfDayLabel(currentHour);
        if (pattern.hasOrdersAtTime(currentHour)) {
            reasons.add("⏰ Bạn thường đặt món này vào " + timeOfDay);
        }
        
        // Lý do về ngày
        if (pattern.hasOrdersOnDay(currentDay)) {
            reasons.add("📅 Bạn hay đặt món này vào " + getDayLabel(currentDay));
        }
        
        // Lý do về tần suất
        if (pattern.getOrderCount() >= 3) {
            reasons.add("❤️ Đây là món yêu thích của bạn (" + pattern.getOrderCount() + " lần đặt)");
        }
        
        // Lý do về thời tiết (dựa trên dữ liệu thực)
        if (weatherDto != null) {
            String tempInfo = String.format("%.0f°C", weatherDto.getTemperature());
            String weatherType = determineWeatherType(weatherDto);
            
            switch (weatherType) {
                case "hot":
                    reasons.add("🌡️ Trời nóng " + tempInfo + " - Đồ uống mát rất hợp!");
                    break;
                case "cold":
                    reasons.add("🌡️ Trời lạnh " + tempInfo + " - Đồ uống ấm rất hợp!");
                    break;
                case "rainy":
                    reasons.add("🌧️ Trời đang mưa - Thưởng thức đồ uống ấm thật tuyệt!");
                    break;
                default:
                    reasons.add("🌤️ Thời tiết dễ chịu " + tempInfo);
            }
            
            if (weatherDto.getCity() != null) {
                reasons.add("📍 " + weatherDto.getCity() + 
                    (weatherDto.getDescription() != null ? ": " + weatherDto.getDescription() : ""));
            }
        } else if (weather != null && !weather.isEmpty()) {
            // Fallback khi chỉ có weather string
            if (weather.equalsIgnoreCase("hot") || weather.equalsIgnoreCase("sunny")) {
                reasons.add("☀️ Thời tiết nóng, thích hợp với đồ uống mát");
            } else if (weather.equalsIgnoreCase("cold") || weather.equalsIgnoreCase("rainy")) {
                reasons.add("🌧️ Thời tiết se lạnh, thích hợp với đồ uống ấm");
            }
        }
        
        return reasons;
    }
    
    private String getTimeOfDayLabel(int hour) {
        if (hour >= 6 && hour < 11) return "buổi sáng";
        if (hour >= 11 && hour < 14) return "buổi trưa";
        if (hour >= 14 && hour < 18) return "buổi chiều";
        return "buổi tối";
    }
    
    private String getDayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    
    /**
     * Inner class để track pattern của từng drink
     */
    private static class DrinkPattern {
        private final Drink drink;
        private final String sizeName;
        private Long sizeId; // Thêm sizeId
        private final List<PredictedTopping> toppings;
        private final String note;
        private int orderCount = 0;
        private double bestScore = 0;
        private Instant lastOrderTime;
        private final Set<Integer> orderHours = new HashSet<>();
        private final Set<DayOfWeek> orderDays = new HashSet<>();
        
        public DrinkPattern(Drink drink, OrderItem sampleItem) {
            this.drink = drink;
            this.sizeName = sampleItem.getSizeNameSnapshot();
            this.note = sampleItem.getNote();
            this.toppings = sampleItem.getToppings().stream()
                    .map(t -> PredictedTopping.builder()
                            .toppingId(0L) // Không có topping ID trong snapshot
                            .toppingName(t.getToppingNameSnapshot())
                            .price(t.getPriceSnapshot())
                            .build())
                    .collect(Collectors.toList());
        }
        
        public void setSizeId(Long sizeId) { this.sizeId = sizeId; }
        public Long getDrinkId() { return drink.getId(); }
        
        public void incrementOrderCount() { orderCount++; }
        public int getOrderCount() { return orderCount; }
        public double getScore() { return bestScore; }
        
        public void updateScore(double score) {
            if (score > bestScore) bestScore = score;
        }
        
        public void addOrderTime(int hour) { orderHours.add(hour); }
        public void addOrderDay(DayOfWeek day) { orderDays.add(day); }
        
        public void updateLastOrder(Instant time) {
            if (lastOrderTime == null || time.isAfter(lastOrderTime)) {
                lastOrderTime = time;
            }
        }
        
        public boolean hasOrdersAtTime(int hour) {
            return orderHours.contains(hour) || 
                   orderHours.contains(hour - 1) || 
                   orderHours.contains(hour + 1);
        }
        
        public boolean hasOrdersOnDay(DayOfWeek day) {
            return orderDays.contains(day);
        }
        
        public String getDrinkName() { return drink.getName(); }
        public String getSizeName() { return sizeName; }
        
        public PredictedDrink toPredictedDrink() {
            String formattedTime = null;
            if (lastOrderTime != null) {
                LocalDateTime ldt = LocalDateTime.ofInstant(lastOrderTime, ZoneId.systemDefault());
                formattedTime = ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            
            return PredictedDrink.builder()
                    .drinkId(drink.getId())
                    .drinkName(drink.getName())
                    .drinkImage(drink.getImageUrl())
                    .sizeName(sizeName)
                    .sizeId(sizeId) // Thêm sizeId vào response
                    .price(drink.getBasePrice())
                    .orderCount(orderCount)
                    .lastOrderTime(formattedTime)
                    .toppings(toppings)
                    .note(note)
                    .build();
        }
    }
}
