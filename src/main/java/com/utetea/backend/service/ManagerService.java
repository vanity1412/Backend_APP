package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.UserDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.OrderRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DrinkCategoryService categoryService;
    
    @org.springframework.beans.factory.annotation.Autowired
    private jakarta.persistence.EntityManager entityManager;
    
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        log.info("Getting dashboard summary");
        
        try {
            // Count orders by status (all time)
            Long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
            Long completedOrders = orderRepository.countByStatus(OrderStatus.DONE);
            Long canceledOrders = orderRepository.countByStatus(OrderStatus.CANCELED);
            Long totalOrders = orderRepository.count();
            
            // Calculate total revenue from DONE orders only
            String totalRevenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status";
            BigDecimal totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
                .setParameter("status", OrderStatus.DONE)
                .getSingleResult();
            
            // Get top selling drinks from DONE orders
            String topDrinksQuery = """
                SELECT oi.drinkNameSnapshot,
                       SUM(oi.quantity) as totalQuantity,
                       SUM(oi.itemPrice) as totalRevenue
                FROM OrderItem oi
                JOIN oi.order o
                WHERE o.status = :status
                GROUP BY oi.drinkNameSnapshot
                ORDER BY SUM(oi.quantity) DESC
                """;
            
            @SuppressWarnings("unchecked")
            List<Object[]> topDrinksResults = entityManager.createQuery(topDrinksQuery)
                .setParameter("status", OrderStatus.DONE)
                .setMaxResults(5)
                .getResultList();
            
            List<DashboardSummaryDto.TopSellingDrinkDto> topSellingDrinks = new ArrayList<>();
            for (Object[] row : topDrinksResults) {
                String drinkName = (String) row[0];
                Long totalSold = ((Number) row[1]).longValue();
                BigDecimal revenue = (BigDecimal) row[2];
                topSellingDrinks.add(new DashboardSummaryDto.TopSellingDrinkDto(drinkName, totalSold, revenue));
            }
            
            DashboardSummaryDto summary = new DashboardSummaryDto();
            summary.setTotalRevenue(totalRevenue);
            summary.setTotalOrders(totalOrders != null ? totalOrders : 0L);
            summary.setPendingOrders(pendingOrders != null ? pendingOrders : 0L);
            summary.setCompletedOrders(completedOrders != null ? completedOrders : 0L);
            summary.setCanceledOrders(canceledOrders != null ? canceledOrders : 0L);
            summary.setTopSellingDrinks(topSellingDrinks);
            
            log.info("Dashboard summary: revenue={}, total={}, pending={}, completed={}", 
                totalRevenue, totalOrders, pendingOrders, completedOrders);
            
            return summary;
        } catch (Exception e) {
            log.error("Error getting dashboard summary", e);
            throw e;
        }
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Getting orders with status: {}", status);
        
        if (status == null) {
            return orderService.getAllOrders(pageable);
        }
        
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            .map(order -> orderService.getOrderById(order.getId()));
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Manager updating order {} to status {}", orderId, newStatus);
        return orderService.updateOrderStatus(orderId, newStatus);
    }
    
    // ==================== USER MANAGEMENT ====================
    
    /**
     * FIX MEMORY ISSUE: Sử dụng database-level pagination thay vì load tất cả vào memory
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String role, Pageable pageable) {
        log.info("Getting all users with role filter: {}", role);
        
        Page<User> usersPage;
        if (role != null && !role.isEmpty()) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                usersPage = userRepository.findByRole(userRole, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role filter: {}, returning all users", role);
                usersPage = userRepository.findAll(pageable);
            }
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        
        // Map to DTO using Page.map() - efficient, no extra memory
        return usersPage.map(UserDto::new);
    }
    
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        log.info("Getting user by id: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return new UserDto(user);
    }
    
    @Transactional
    public UserDto toggleUserBlock(Long userId, boolean blocked) {
        log.info("Toggling user {} block status to: {}", userId, blocked);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setIsBlocked(blocked);
        user.setActive(!blocked); // If blocked, set active to false
        
        User savedUser = userRepository.save(user);
        log.info("User {} block status updated to: {}", userId, blocked);
        
        return new UserDto(savedUser);
    }
    
    /**
     * FIX MEMORY ISSUE: Sử dụng database-level search với pagination
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}", keyword);
        
        // Sử dụng database query thay vì load tất cả vào memory
        Page<User> usersPage = userRepository.searchByKeyword(keyword, pageable);
        
        return usersPage.map(UserDto::new);
    }
    
    // ==================== REVENUE STATISTICS ====================
    
    @Transactional(readOnly = true)
    public com.utetea.backend.dto.RevenueStatisticsDto getRevenueStatistics(Integer days, Integer months) {
        log.info("Getting revenue statistics - days: {}, months: {}", days, months);
        
        com.utetea.backend.dto.RevenueStatisticsDto stats = new com.utetea.backend.dto.RevenueStatisticsDto();
        
        try {
            // Calculate total revenue from DONE orders
            String totalRevenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status";
            BigDecimal totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
                .setParameter("status", OrderStatus.DONE)
                .getSingleResult();
            stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Get daily revenues using native query for better compatibility
            if (days != null && days > 0) {
                java.time.Instant cutoffDate = java.time.Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
                
                // Use native query for date grouping - works with MySQL/PostgreSQL/H2
                String dailyNativeQuery = """
                    SELECT CAST(o.created_at AS DATE) as order_date, 
                           COALESCE(SUM(o.final_price), 0) as revenue,
                           COUNT(o.id) as order_count
                    FROM orders o 
                    WHERE o.status = 'DONE' 
                      AND o.created_at >= :cutoffDate
                    GROUP BY CAST(o.created_at AS DATE)
                    ORDER BY order_date ASC
                    """;
                
                @SuppressWarnings("unchecked")
                List<Object[]> dailyResults = entityManager.createNativeQuery(dailyNativeQuery)
                    .setParameter("cutoffDate", java.sql.Timestamp.from(cutoffDate))
                    .getResultList();
                
                List<com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue> dailyRevenues = new ArrayList<>();
                for (Object[] row : dailyResults) {
                    java.time.LocalDate date;
                    if (row[0] instanceof java.sql.Date) {
                        date = ((java.sql.Date) row[0]).toLocalDate();
                    } else if (row[0] instanceof java.time.LocalDate) {
                        date = (java.time.LocalDate) row[0];
                    } else {
                        date = java.time.LocalDate.parse(row[0].toString().substring(0, 10));
                    }
                    BigDecimal revenue = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
                    Long orderCount = ((Number) row[2]).longValue();
                    dailyRevenues.add(new com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue(date, revenue, orderCount));
                }
                stats.setDailyRevenues(dailyRevenues);
            }
            
            // Get monthly revenues using native query
            if (months != null && months > 0) {
                java.time.Instant cutoffDate = java.time.Instant.now().minus(months * 30L, java.time.temporal.ChronoUnit.DAYS);
                
                String monthlyNativeQuery = """
                    SELECT YEAR(o.created_at) as year_val,
                           MONTH(o.created_at) as month_val,
                           COALESCE(SUM(o.final_price), 0) as revenue,
                           COUNT(o.id) as order_count
                    FROM orders o 
                    WHERE o.status = 'DONE'
                      AND o.created_at >= :cutoffDate
                    GROUP BY YEAR(o.created_at), MONTH(o.created_at)
                    ORDER BY year_val ASC, month_val ASC
                    """;
                
                @SuppressWarnings("unchecked")
                List<Object[]> monthlyResults = entityManager.createNativeQuery(monthlyNativeQuery)
                    .setParameter("cutoffDate", java.sql.Timestamp.from(cutoffDate))
                    .getResultList();
                
                List<com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue> monthlyRevenues = new ArrayList<>();
                for (Object[] row : monthlyResults) {
                    Integer year = ((Number) row[0]).intValue();
                    Integer month = ((Number) row[1]).intValue();
                    BigDecimal revenue = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(row[2].toString());
                    Long orderCount = ((Number) row[3]).longValue();
                    monthlyRevenues.add(new com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue(year, month, revenue, orderCount));
                }
                stats.setMonthlyRevenues(monthlyRevenues);
            }
            
            // Get top selling drinks
            String topDrinksQuery = """
                SELECT oi.drink.id,
                       oi.drinkNameSnapshot,
                       oi.drink.imageUrl,
                       SUM(oi.quantity) as totalQuantity,
                       SUM(oi.itemPrice) as totalRevenue
                FROM OrderItem oi
                JOIN oi.order o
                WHERE o.status = :status
                GROUP BY oi.drink.id, oi.drinkNameSnapshot, oi.drink.imageUrl
                ORDER BY SUM(oi.quantity) DESC
                """;
            
            @SuppressWarnings("unchecked")
            List<Object[]> topDrinksResults = entityManager.createQuery(topDrinksQuery)
                .setParameter("status", OrderStatus.DONE)
                .setMaxResults(10)
                .getResultList();
            
            List<com.utetea.backend.dto.RevenueStatisticsDto.TopSellingDrink> topDrinks = new ArrayList<>();
            for (Object[] row : topDrinksResults) {
                Long drinkId = (Long) row[0];
                String drinkName = (String) row[1];
                String imageUrl = (String) row[2];
                Long totalQuantity = ((Number) row[3]).longValue();
                BigDecimal drinkRevenue = (BigDecimal) row[4];
                topDrinks.add(new com.utetea.backend.dto.RevenueStatisticsDto.TopSellingDrink(
                    drinkId, drinkName, imageUrl, totalQuantity, drinkRevenue));
            }
            stats.setTopSellingDrinks(topDrinks);
            
            log.info("Revenue statistics calculated - total: {}, daily entries: {}, monthly entries: {}, top drinks: {}",
                totalRevenue, stats.getDailyRevenues() != null ? stats.getDailyRevenues().size() : 0,
                stats.getMonthlyRevenues() != null ? stats.getMonthlyRevenues().size() : 0,
                topDrinks.size());
            
        } catch (Exception e) {
            log.error("Error calculating revenue statistics", e);
            // Return empty stats instead of throwing
            stats.setTotalRevenue(BigDecimal.ZERO);
            stats.setDailyRevenues(new ArrayList<>());
            stats.setMonthlyRevenues(new ArrayList<>());
            stats.setTopSellingDrinks(new ArrayList<>());
        }
        
        return stats;
    }
    
    // ==================== CATEGORY MANAGEMENT ====================
    
    @Transactional
    public com.utetea.backend.dto.DrinkCategoryDto createCategory(com.utetea.backend.dto.DrinkCategoryDto dto) {
        log.info("Creating category: {}", dto.getName());
        return categoryService.createCategory(dto);
    }
    
    @Transactional
    public com.utetea.backend.dto.DrinkCategoryDto updateCategory(Long id, com.utetea.backend.dto.DrinkCategoryDto dto) {
        log.info("Updating category {}: {}", id, dto.getName());
        return categoryService.updateCategory(id, dto);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category: {}", id);
        categoryService.deleteCategory(id);
    }

}
