package com.utetea.backend.service;

import com.utetea.backend.dto.PredictiveOrderDto;
import com.utetea.backend.dto.PredictiveOrderDto.*;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveOrderService {
    
    private final OrderRepository orderRepository;
    private final DrinkRepository drinkRepository;
    
    // Ngưỡng confidence để hiển thị prediction
    private static final double MIN_CONFIDENCE = 0.4;
    
    /**
     * Dự đoán món khách hàng muốn đặt
     */
    public PredictiveOrderDto getPrediction(Long userId, String weatherCondition) {
        List<Order> orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(userId);
        
        if (orders.isEmpty() || orders.size() < 2) {
            return PredictiveOrderDto.builder()
                    .hasPrediction(false)
                    .message("Chưa đủ dữ liệu để dự đoán")
                    .build();
        }
        
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // Phân tích patterns
        Map<Long, DrinkPattern> drinkPatterns = analyzeDrinkPatterns(orders, currentHour, currentDay);
        
        if (drinkPatterns.isEmpty()) {
            return PredictiveOrderDto.builder()
                    .hasPrediction(false)
                    .message("Chưa tìm thấy pattern phù hợp")
                    .build();
        }
        
        // Tìm drink có score cao nhất
        DrinkPattern bestPattern = drinkPatterns.values().stream()
                .max(Comparator.comparingDouble(DrinkPattern::getScore))
                .orElse(null);
        
        if (bestPattern == null || bestPattern.getScore() < MIN_CONFIDENCE) {
            return PredictiveOrderDto.builder()
                    .hasPrediction(false)
                    .message("Độ tin cậy chưa đủ cao")
                    .build();
        }
        
        // Build prediction response
        List<String> reasons = buildTriggerReasons(bestPattern, currentHour, currentDay, weatherCondition);
        
        return PredictiveOrderDto.builder()
                .hasPrediction(true)
                .message(buildPredictionMessage(bestPattern))
                .predictedDrink(bestPattern.toPredictedDrink())
                .triggerReasons(reasons)
                .confidenceScore(bestPattern.getScore())
                .build();
    }

    
    /**
     * Phân tích patterns từ order history
     */
    private Map<Long, DrinkPattern> analyzeDrinkPatterns(
            List<Order> orders, int currentHour, DayOfWeek currentDay) {
        
        Map<Long, DrinkPattern> patterns = new HashMap<>();
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        for (Order order : orders) {
            Instant orderInstant = order.getCreatedAt();
            LocalDateTime orderTime = LocalDateTime.ofInstant(orderInstant, zoneId);
            int orderHour = orderTime.getHour();
            DayOfWeek orderDay = orderTime.getDayOfWeek();
            long daysSinceOrder = ChronoUnit.DAYS.between(orderInstant, now);
            
            for (OrderItem item : order.getItems()) {
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
                double frequencyScore = pattern.getOrderCount() / (double) orders.size();
                
                // Weighted score
                double score = (timeScore * 0.35) + (dayScore * 0.25) + 
                               (recencyScore * 0.2) + (frequencyScore * 0.2);
                
                pattern.updateScore(score);
            }
        }
        
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
            DayOfWeek currentDay, String weather) {
        List<String> reasons = new ArrayList<>();
        
        // Lý do về thời gian
        String timeOfDay = getTimeOfDayLabel(currentHour);
        if (pattern.hasOrdersAtTime(currentHour)) {
            reasons.add("Bạn thường đặt món này vào " + timeOfDay);
        }
        
        // Lý do về ngày
        if (pattern.hasOrdersOnDay(currentDay)) {
            reasons.add("Bạn hay đặt món này vào " + getDayLabel(currentDay));
        }
        
        // Lý do về tần suất
        if (pattern.getOrderCount() >= 3) {
            reasons.add("Đây là món yêu thích của bạn (" + pattern.getOrderCount() + " lần đặt)");
        }
        
        // Lý do về thời tiết (nếu có)
        if (weather != null && !weather.isEmpty()) {
            if (weather.equalsIgnoreCase("hot") || weather.equalsIgnoreCase("sunny")) {
                reasons.add("Thời tiết nóng, thích hợp với đồ uống mát");
            } else if (weather.equalsIgnoreCase("cold") || weather.equalsIgnoreCase("rainy")) {
                reasons.add("Thời tiết se lạnh, thích hợp với đồ uống ấm");
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
                    .price(drink.getBasePrice())
                    .orderCount(orderCount)
                    .lastOrderTime(formattedTime)
                    .toppings(toppings)
                    .note(note)
                    .build();
        }
    }
}
