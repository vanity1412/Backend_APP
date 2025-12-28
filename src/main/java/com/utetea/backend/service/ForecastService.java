package com.utetea.backend.service;

import com.utetea.backend.dto.ForecastDto;
import com.utetea.backend.dto.ForecastDto.*;
import com.utetea.backend.model.OrderStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Service dự báo doanh thu và phân tích nguy cơ quá tải
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {
    
    private final EntityManager entityManager;
    
    // Cấu hình
    private static final int ORDERS_PER_STAFF_PER_HOUR = 8;  // Mỗi nhân viên xử lý 8 đơn/giờ
    private static final int MAX_CAPACITY_PER_HOUR = 50;     // Công suất tối đa/giờ
    private static final int ANALYSIS_DAYS = 30;             // Phân tích 30 ngày gần nhất
    
    /**
     * Lấy toàn bộ dữ liệu dự báo
     */
    public ForecastDto getFullForecast() {
        log.info("Generating full forecast data");
        
        ForecastDto forecast = new ForecastDto();
        
        // Gọi từng method riêng để tránh transaction rollback toàn bộ
        try {
            forecast.setRevenueForecast(calculateRevenueForecast());
        } catch (Exception e) {
            log.error("Error in revenue forecast", e);
            forecast.setRevenueForecast(new RevenueForecast());
        }
        
        try {
            forecast.setPeakHours(analyzePeakHours());
        } catch (Exception e) {
            log.error("Error in peak hours", e);
            forecast.setPeakHours(new ArrayList<>());
        }
        
        try {
            forecast.setLowStockWarnings(analyzeLowStock());
        } catch (Exception e) {
            log.error("Error in low stock", e);
            forecast.setLowStockWarnings(new ArrayList<>());
        }
        
        try {
            forecast.setStaffingRecommendations(generateStaffingRecommendations());
        } catch (Exception e) {
            log.error("Error in staffing", e);
            forecast.setStaffingRecommendations(new ArrayList<>());
        }
        
        try {
            forecast.setOverloadWarnings(detectOverloadRisks());
        } catch (Exception e) {
            log.error("Error in overload", e);
            forecast.setOverloadWarnings(new ArrayList<>());
        }
        
        return forecast;
    }
    
    /**
     * Dự báo doanh thu
     */
    @Transactional(readOnly = true)
    public RevenueForecast calculateRevenueForecast() {
        log.info("Calculating revenue forecast");
        
        RevenueForecast forecast = new RevenueForecast();
        
        try {
            // Lấy dữ liệu lịch sử theo ngày trong tuần
            Map<DayOfWeek, BigDecimal> avgRevenueByDayOfWeek = getAvgRevenueByDayOfWeek();
            Map<DayOfWeek, Long> avgOrdersByDayOfWeek = getAvgOrdersByDayOfWeek();
            
            // Dự báo hôm nay
            LocalDate today = LocalDate.now();
            DayOfWeek todayDow = today.getDayOfWeek();
            BigDecimal todayForecast = avgRevenueByDayOfWeek.getOrDefault(todayDow, BigDecimal.ZERO);
            
            // Điều chỉnh theo tiến độ trong ngày
            int currentHour = LocalDateTime.now().getHour();
            if (currentHour > 0 && currentHour < 22) {
                BigDecimal todayActual = getTodayRevenue();
                double progressRatio = currentHour / 14.0; // Giả sử hoạt động 14 tiếng/ngày
                if (progressRatio > 0 && todayActual.compareTo(BigDecimal.ZERO) > 0) {
                    todayForecast = todayActual.divide(BigDecimal.valueOf(progressRatio), 2, RoundingMode.HALF_UP);
                }
            }
            forecast.setTodayForecast(todayForecast);
            
            // Dự báo ngày mai
            DayOfWeek tomorrowDow = today.plusDays(1).getDayOfWeek();
            forecast.setTomorrowForecast(avgRevenueByDayOfWeek.getOrDefault(tomorrowDow, BigDecimal.ZERO));
            
            // Dự báo 7 ngày tới
            BigDecimal weekForecast = BigDecimal.ZERO;
            List<DailyForecast> dailyForecasts = new ArrayList<>();
            
            for (int i = 0; i < 7; i++) {
                LocalDate date = today.plusDays(i);
                DayOfWeek dow = date.getDayOfWeek();
                BigDecimal dayRevenue = avgRevenueByDayOfWeek.getOrDefault(dow, BigDecimal.ZERO);
                Long dayOrders = avgOrdersByDayOfWeek.getOrDefault(dow, 0L);
                
                weekForecast = weekForecast.add(dayRevenue);
                
                dailyForecasts.add(new DailyForecast(
                    date,
                    dow.getDisplayName(TextStyle.FULL, new Locale("vi", "VN")),
                    dayRevenue,
                    dayOrders,
                    calculateConfidence(dow)
                ));
            }
            forecast.setWeekForecast(weekForecast);
            forecast.setDailyForecasts(dailyForecasts);
            
            // Dự báo tháng (30 ngày)
            BigDecimal monthForecast = BigDecimal.ZERO;
            for (int i = 0; i < 30; i++) {
                DayOfWeek dow = today.plusDays(i).getDayOfWeek();
                monthForecast = monthForecast.add(avgRevenueByDayOfWeek.getOrDefault(dow, BigDecimal.ZERO));
            }
            forecast.setMonthForecast(monthForecast);
            
            // Tính tỷ lệ tăng trưởng
            BigDecimal lastWeekRevenue = getLastWeekRevenue();
            BigDecimal prevWeekRevenue = getPreviousWeekRevenue();
            if (prevWeekRevenue.compareTo(BigDecimal.ZERO) > 0) {
                double growthRate = lastWeekRevenue.subtract(prevWeekRevenue)
                    .divide(prevWeekRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
                forecast.setGrowthRate(growthRate);
                forecast.setTrend(growthRate > 5 ? "UP" : growthRate < -5 ? "DOWN" : "STABLE");
            } else {
                forecast.setGrowthRate(0.0);
                forecast.setTrend("STABLE");
            }
            
        } catch (Exception e) {
            log.error("Error calculating revenue forecast", e);
            forecast.setTodayForecast(BigDecimal.ZERO);
            forecast.setTomorrowForecast(BigDecimal.ZERO);
            forecast.setWeekForecast(BigDecimal.ZERO);
            forecast.setMonthForecast(BigDecimal.ZERO);
            forecast.setGrowthRate(0.0);
            forecast.setTrend("STABLE");
            forecast.setDailyForecasts(new ArrayList<>());
        }
        
        return forecast;
    }

    
    /**
     * Phân tích giờ cao điểm
     */
    @Transactional(readOnly = true)
    public List<PeakHourAnalysis> analyzePeakHours() {
        log.info("Analyzing peak hours");
        
        List<PeakHourAnalysis> peakHours = new ArrayList<>();
        
        try {
            String query = """
                SELECT HOUR(o.created_at) as hour_val,
                       COUNT(o.id) as order_count,
                       COALESCE(SUM(o.final_price), 0) as revenue
                FROM orders o
                WHERE o.status = 'DONE'
                  AND o.created_at >= :startDate
                GROUP BY HOUR(o.created_at)
                ORDER BY hour_val
                """;
            
            LocalDateTime startDate = LocalDateTime.now().minusDays(ANALYSIS_DAYS);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(query)
                .setParameter("startDate", java.sql.Timestamp.valueOf(startDate))
                .getResultList();
            
            // Tính trung bình theo ngày
            int totalDays = ANALYSIS_DAYS;
            
            for (Object[] row : results) {
                Integer hour = ((Number) row[0]).intValue();
                Long totalOrders = ((Number) row[1]).longValue();
                BigDecimal totalRevenue = row[2] instanceof BigDecimal ? 
                    (BigDecimal) row[2] : new BigDecimal(row[2].toString());
                
                Long avgOrders = totalOrders / totalDays;
                BigDecimal avgRevenue = totalRevenue.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
                
                String peakLevel = determinePeakLevel(avgOrders);
                int recommendedStaff = (int) Math.ceil((double) avgOrders / ORDERS_PER_STAFF_PER_HOUR);
                recommendedStaff = Math.max(1, recommendedStaff);
                
                peakHours.add(new PeakHourAnalysis(
                    hour,
                    String.format("%02d:00 - %02d:00", hour, hour + 1),
                    avgOrders,
                    avgRevenue,
                    peakLevel,
                    recommendedStaff
                ));
            }
            
            // Sắp xếp theo số đơn giảm dần
            peakHours.sort((a, b) -> Long.compare(b.getAvgOrders(), a.getAvgOrders()));
            
        } catch (Exception e) {
            log.error("Error analyzing peak hours", e);
        }
        
        return peakHours;
    }
    
    /**
     * Phân tích món sắp hết (dựa trên tốc độ bán)
     */
    @Transactional(readOnly = true)
    public List<LowStockWarning> analyzeLowStock() {
        log.info("Analyzing low stock warnings");
        
        List<LowStockWarning> warnings = new ArrayList<>();
        
        try {
            // Query đơn giản hơn - lấy số lượng bán hôm nay
            String todaySalesQuery = """
                SELECT oi.drink_id, d.name, d.image_url, SUM(oi.quantity) as qty
                FROM order_items oi
                JOIN drinks d ON oi.drink_id = d.id
                JOIN orders o ON oi.order_id = o.id
                WHERE o.status = 'DONE' AND DATE(o.created_at) = CURDATE()
                GROUP BY oi.drink_id, d.name, d.image_url
                ORDER BY qty DESC
                """;
            
            @SuppressWarnings("unchecked")
            List<Object[]> todaySales = entityManager.createNativeQuery(todaySalesQuery).getResultList();
            
            // Query lấy trung bình 30 ngày
            String avgSalesQuery = """
                SELECT oi.drink_id, AVG(daily_qty) as avg_qty
                FROM (
                    SELECT oi.drink_id, DATE(o.created_at) as order_date, SUM(oi.quantity) as daily_qty
                    FROM order_items oi
                    JOIN orders o ON oi.order_id = o.id
                    WHERE o.status = 'DONE' AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                    GROUP BY oi.drink_id, DATE(o.created_at)
                ) as daily_sales
                GROUP BY oi.drink_id
                """;
            
            Map<Long, Double> avgDailyMap = new HashMap<>();
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> avgResults = entityManager.createNativeQuery(avgSalesQuery).getResultList();
                for (Object[] row : avgResults) {
                    Long drinkId = ((Number) row[0]).longValue();
                    Double avgQty = ((Number) row[1]).doubleValue();
                    avgDailyMap.put(drinkId, avgQty);
                }
            } catch (Exception e) {
                log.warn("Could not get avg sales, using today sales only", e);
            }
            
            int currentHour = LocalDateTime.now().getHour();
            
            for (Object[] row : todaySales) {
                Long drinkId = ((Number) row[0]).longValue();
                String drinkName = (String) row[1];
                String imageUrl = (String) row[2];
                Long soldToday = ((Number) row[3]).longValue();
                Double avgDaily = avgDailyMap.getOrDefault(drinkId, soldToday.doubleValue());
                
                if (avgDaily <= 0) continue;
                
                // Tính tốc độ bán (đơn/giờ)
                double salesVelocity = currentHour > 8 ? (double) soldToday / (currentHour - 8) : 0;
                
                // Nếu tốc độ bán cao hơn bình thường -> cảnh báo
                double normalVelocity = avgDaily / 14.0; // 14 giờ hoạt động
                
                if (salesVelocity > normalVelocity * 1.5 && soldToday > avgDaily * 0.7) {
                    String warningLevel = salesVelocity > normalVelocity * 2 ? "CRITICAL" : "WARNING";
                    double expectedRemaining = avgDaily - soldToday;
                    double hoursUntilOut = expectedRemaining > 0 && salesVelocity > 0 ? expectedRemaining / salesVelocity : 99;
                    
                    String message = hoursUntilOut < 2 ? 
                        "Dự kiến hết hàng trong " + String.format("%.1f", hoursUntilOut) + " giờ" :
                        "Đang bán nhanh hơn bình thường " + String.format("%.0f", (salesVelocity/normalVelocity - 1) * 100) + "%";
                    
                    warnings.add(new LowStockWarning(
                        drinkId, drinkName, imageUrl,
                        soldToday, Math.round(avgDaily),
                        salesVelocity, warningLevel, message
                    ));
                }
            }
            
            // Sắp xếp theo mức độ nghiêm trọng
            warnings.sort((a, b) -> {
                int levelCompare = b.getWarningLevel().compareTo(a.getWarningLevel());
                if (levelCompare != 0) return levelCompare;
                return Double.compare(b.getSalesVelocity(), a.getSalesVelocity());
            });
            
        } catch (Exception e) {
            log.error("Error analyzing low stock", e);
        }
        
        return warnings;
    }

    
    /**
     * Đề xuất nhân sự theo ngày
     */
    @Transactional(readOnly = true)
    public List<StaffingRecommendation> generateStaffingRecommendations() {
        log.info("Generating staffing recommendations");
        
        List<StaffingRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Lấy dữ liệu đơn hàng theo ngày trong tuần và giờ
            String query = """
                SELECT DAYOFWEEK(o.created_at) as dow,
                       HOUR(o.created_at) as hour_val,
                       COUNT(o.id) as order_count
                FROM orders o
                WHERE o.status = 'DONE'
                  AND o.created_at >= :startDate
                GROUP BY DAYOFWEEK(o.created_at), HOUR(o.created_at)
                ORDER BY dow, hour_val
                """;
            
            LocalDateTime startDate = LocalDateTime.now().minusDays(ANALYSIS_DAYS);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(query)
                .setParameter("startDate", java.sql.Timestamp.valueOf(startDate))
                .getResultList();
            
            // Nhóm theo ngày trong tuần
            Map<Integer, Map<Integer, Long>> ordersByDowAndHour = new HashMap<>();
            for (Object[] row : results) {
                Integer dow = ((Number) row[0]).intValue();
                Integer hour = ((Number) row[1]).intValue();
                Long orders = ((Number) row[2]).longValue();
                
                ordersByDowAndHour.computeIfAbsent(dow, k -> new HashMap<>())
                    .put(hour, orders / (ANALYSIS_DAYS / 7)); // Trung bình theo tuần
            }
            
            // Tạo đề xuất cho 7 ngày tới
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                LocalDate date = today.plusDays(i);
                int dow = date.getDayOfWeek().getValue() % 7 + 1; // Convert to MySQL DAYOFWEEK format
                
                Map<Integer, Long> hourlyOrders = ordersByDowAndHour.getOrDefault(dow, new HashMap<>());
                
                List<HourlyStaffing> hourlyBreakdown = new ArrayList<>();
                int maxStaffNeeded = 1;
                long totalExpectedOrders = 0;
                
                for (int hour = 8; hour <= 22; hour++) {
                    Long expectedOrders = hourlyOrders.getOrDefault(hour, 0L);
                    totalExpectedOrders += expectedOrders;
                    
                    int staffNeeded = (int) Math.ceil((double) expectedOrders / ORDERS_PER_STAFF_PER_HOUR);
                    staffNeeded = Math.max(1, staffNeeded);
                    maxStaffNeeded = Math.max(maxStaffNeeded, staffNeeded);
                    
                    String loadLevel = expectedOrders < 5 ? "LOW" : 
                                       expectedOrders < 15 ? "MEDIUM" : "HIGH";
                    
                    hourlyBreakdown.add(new HourlyStaffing(
                        hour,
                        String.format("%02d:00 - %02d:00", hour, hour + 1),
                        staffNeeded,
                        expectedOrders,
                        loadLevel
                    ));
                }
                
                // Tính số nhân viên đề xuất (lấy trung bình của các giờ cao điểm)
                int recommendedStaff = (int) Math.ceil(hourlyBreakdown.stream()
                    .filter(h -> h.getLoadLevel().equals("HIGH") || h.getLoadLevel().equals("MEDIUM"))
                    .mapToInt(HourlyStaffing::getRecommendedStaff)
                    .average()
                    .orElse(1));
                
                String reason = generateStaffingReason(date.getDayOfWeek(), totalExpectedOrders, maxStaffNeeded);
                
                recommendations.add(new StaffingRecommendation(
                    date,
                    date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi", "VN")),
                    null, // currentStaff - có thể tích hợp sau
                    recommendedStaff,
                    null, // additionalNeeded - tính sau khi có currentStaff
                    reason,
                    hourlyBreakdown
                ));
            }
            
        } catch (Exception e) {
            log.error("Error generating staffing recommendations", e);
        }
        
        return recommendations;
    }
    
    /**
     * Phát hiện nguy cơ quá tải
     */
    @Transactional(readOnly = true)
    public List<OverloadWarning> detectOverloadRisks() {
        log.info("Detecting overload risks");
        
        List<OverloadWarning> warnings = new ArrayList<>();
        
        try {
            // Lấy dữ liệu lịch sử để dự đoán
            String query = """
                SELECT DAYOFWEEK(o.created_at) as dow,
                       HOUR(o.created_at) as hour_val,
                       COUNT(o.id) as order_count,
                       MAX(daily_count.cnt) as max_orders
                FROM orders o
                JOIN (
                    SELECT DATE(created_at) as order_date, HOUR(created_at) as h, COUNT(*) as cnt
                    FROM orders
                    WHERE status = 'DONE' AND created_at >= :startDate
                    GROUP BY DATE(created_at), HOUR(created_at)
                ) daily_count ON HOUR(o.created_at) = daily_count.h
                WHERE o.status = 'DONE'
                  AND o.created_at >= :startDate
                GROUP BY DAYOFWEEK(o.created_at), HOUR(o.created_at)
                HAVING MAX(daily_count.cnt) > :threshold
                ORDER BY max_orders DESC
                """;
            
            LocalDateTime startDate = LocalDateTime.now().minusDays(ANALYSIS_DAYS);
            int threshold = (int) (MAX_CAPACITY_PER_HOUR * 0.8); // 80% công suất
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(query)
                .setParameter("startDate", java.sql.Timestamp.valueOf(startDate))
                .setParameter("threshold", threshold)
                .getResultList();
            
            LocalDate today = LocalDate.now();
            
            for (Object[] row : results) {
                Integer dow = ((Number) row[0]).intValue();
                Integer hour = ((Number) row[1]).intValue();
                Long maxOrders = ((Number) row[3]).longValue();
                
                // Tìm ngày tiếp theo có cùng thứ
                LocalDate targetDate = today;
                int targetDow = dow == 1 ? 7 : dow - 1; // Convert MySQL DOW to Java DOW
                while (targetDate.getDayOfWeek().getValue() != targetDow) {
                    targetDate = targetDate.plusDays(1);
                }
                
                if (targetDate.isAfter(today.plusDays(7))) continue; // Chỉ cảnh báo 7 ngày tới
                
                double overloadPercent = ((double) maxOrders / MAX_CAPACITY_PER_HOUR - 1) * 100;
                String severity = overloadPercent > 30 ? "CRITICAL" : "WARNING";
                
                String recommendation = severity.equals("CRITICAL") ?
                    "Cần tăng cường " + (int) Math.ceil((double) maxOrders / ORDERS_PER_STAFF_PER_HOUR) + " nhân viên và chuẩn bị nguyên liệu gấp đôi" :
                    "Nên bố trí thêm 1-2 nhân viên hỗ trợ";
                
                warnings.add(new OverloadWarning(
                    targetDate,
                    hour,
                    String.format("%02d:00 - %02d:00", hour, hour + 1),
                    maxOrders,
                    (long) MAX_CAPACITY_PER_HOUR,
                    overloadPercent,
                    severity,
                    recommendation
                ));
            }
            
            // Sắp xếp theo ngày và mức độ nghiêm trọng
            warnings.sort((a, b) -> {
                int dateCompare = a.getDate().compareTo(b.getDate());
                if (dateCompare != 0) return dateCompare;
                return b.getSeverity().compareTo(a.getSeverity());
            });
            
        } catch (Exception e) {
            log.error("Error detecting overload risks", e);
        }
        
        return warnings;
    }

    
    // ==================== HELPER METHODS ====================
    
    private Map<DayOfWeek, BigDecimal> getAvgRevenueByDayOfWeek() {
        String query = """
            SELECT DAYOFWEEK(o.created_at) as dow,
                   COALESCE(SUM(o.final_price), 0) as total_revenue,
                   COUNT(DISTINCT DATE(o.created_at)) as day_count
            FROM orders o
            WHERE o.status = 'DONE'
              AND o.created_at >= :startDate
            GROUP BY DAYOFWEEK(o.created_at)
            """;
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(ANALYSIS_DAYS);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(query)
            .setParameter("startDate", java.sql.Timestamp.valueOf(startDate))
            .getResultList();
        
        Map<DayOfWeek, BigDecimal> avgRevenue = new EnumMap<>(DayOfWeek.class);
        for (Object[] row : results) {
            Integer mysqlDow = ((Number) row[0]).intValue();
            BigDecimal totalRevenue = row[1] instanceof BigDecimal ? 
                (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            Long dayCount = ((Number) row[2]).longValue();
            
            DayOfWeek dow = convertMysqlDowToJava(mysqlDow);
            BigDecimal avg = dayCount > 0 ? 
                totalRevenue.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            avgRevenue.put(dow, avg);
        }
        
        return avgRevenue;
    }
    
    private Map<DayOfWeek, Long> getAvgOrdersByDayOfWeek() {
        String query = """
            SELECT DAYOFWEEK(o.created_at) as dow,
                   COUNT(o.id) as total_orders,
                   COUNT(DISTINCT DATE(o.created_at)) as day_count
            FROM orders o
            WHERE o.status = 'DONE'
              AND o.created_at >= :startDate
            GROUP BY DAYOFWEEK(o.created_at)
            """;
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(ANALYSIS_DAYS);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(query)
            .setParameter("startDate", java.sql.Timestamp.valueOf(startDate))
            .getResultList();
        
        Map<DayOfWeek, Long> avgOrders = new EnumMap<>(DayOfWeek.class);
        for (Object[] row : results) {
            Integer mysqlDow = ((Number) row[0]).intValue();
            Long totalOrders = ((Number) row[1]).longValue();
            Long dayCount = ((Number) row[2]).longValue();
            
            DayOfWeek dow = convertMysqlDowToJava(mysqlDow);
            Long avg = dayCount > 0 ? totalOrders / dayCount : 0L;
            avgOrders.put(dow, avg);
        }
        
        return avgOrders;
    }
    
    private BigDecimal getTodayRevenue() {
        String query = """
            SELECT COALESCE(SUM(o.final_price), 0)
            FROM orders o
            WHERE o.status = 'DONE'
              AND DATE(o.created_at) = CURDATE()
            """;
        
        Object result = entityManager.createNativeQuery(query).getSingleResult();
        return result instanceof BigDecimal ? (BigDecimal) result : new BigDecimal(result.toString());
    }
    
    private BigDecimal getLastWeekRevenue() {
        String query = """
            SELECT COALESCE(SUM(o.final_price), 0)
            FROM orders o
            WHERE o.status = 'DONE'
              AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
            """;
        
        Object result = entityManager.createNativeQuery(query).getSingleResult();
        return result instanceof BigDecimal ? (BigDecimal) result : new BigDecimal(result.toString());
    }
    
    private BigDecimal getPreviousWeekRevenue() {
        String query = """
            SELECT COALESCE(SUM(o.final_price), 0)
            FROM orders o
            WHERE o.status = 'DONE'
              AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
              AND o.created_at < DATE_SUB(CURDATE(), INTERVAL 7 DAY)
            """;
        
        Object result = entityManager.createNativeQuery(query).getSingleResult();
        return result instanceof BigDecimal ? (BigDecimal) result : new BigDecimal(result.toString());
    }
    
    private DayOfWeek convertMysqlDowToJava(int mysqlDow) {
        // MySQL: 1=Sunday, 2=Monday, ..., 7=Saturday
        // Java: 1=Monday, ..., 7=Sunday
        return switch (mysqlDow) {
            case 1 -> DayOfWeek.SUNDAY;
            case 2 -> DayOfWeek.MONDAY;
            case 3 -> DayOfWeek.TUESDAY;
            case 4 -> DayOfWeek.WEDNESDAY;
            case 5 -> DayOfWeek.THURSDAY;
            case 6 -> DayOfWeek.FRIDAY;
            case 7 -> DayOfWeek.SATURDAY;
            default -> DayOfWeek.MONDAY;
        };
    }
    
    private String determinePeakLevel(Long avgOrders) {
        if (avgOrders < 5) return "LOW";
        if (avgOrders < 15) return "MEDIUM";
        if (avgOrders < 30) return "HIGH";
        return "VERY_HIGH";
    }
    
    private Double calculateConfidence(DayOfWeek dow) {
        // Cuối tuần thường có biến động cao hơn -> độ tin cậy thấp hơn
        return switch (dow) {
            case SATURDAY, SUNDAY -> 75.0;
            case FRIDAY -> 80.0;
            default -> 85.0;
        };
    }
    
    private String generateStaffingReason(DayOfWeek dow, long totalOrders, int maxStaff) {
        String dayType = switch (dow) {
            case SATURDAY, SUNDAY -> "cuối tuần";
            case FRIDAY -> "thứ Sáu";
            default -> "ngày thường";
        };
        
        if (totalOrders > 100) {
            return "Dự kiến " + totalOrders + " đơn (" + dayType + "), cần " + maxStaff + " nhân viên giờ cao điểm";
        } else if (totalOrders > 50) {
            return "Lượng đơn trung bình (" + dayType + ")";
        } else {
            return "Lượng đơn thấp (" + dayType + ")";
        }
    }
}
