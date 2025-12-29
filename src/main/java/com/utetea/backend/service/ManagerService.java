package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.UserDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.DeletedUserOrderBackupRepository;
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
    private final DeletedUserOrderBackupRepository deletedUserOrderBackupRepository;
    private final UserProfileService userProfileService;
    
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
            
            // Thêm doanh thu từ backup (user đã xóa)
            BigDecimal backupRevenue = getBackupTotalRevenue();
            totalRevenue = totalRevenue.add(backupRevenue);
            
            // Thêm số đơn hàng từ backup
            Long backupOrderCount = deletedUserOrderBackupRepository.count();
            totalOrders = totalOrders + backupOrderCount;
            
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
            
            log.info("Dashboard summary: revenue={} (including backup: {}), total={}, pending={}, completed={}", 
                totalRevenue, backupRevenue, totalOrders, pendingOrders, completedOrders);
            
            return summary;
        } catch (Exception e) {
            log.error("Error getting dashboard summary", e);
            throw e;
        }
    }
    
    /**
     * Lấy tổng doanh thu từ backup (user đã xóa)
     */
    private BigDecimal getBackupTotalRevenue() {
        try {
            String query = "SELECT COALESCE(SUM(b.finalPrice), 0) FROM DeletedUserOrderBackup b WHERE b.orderStatus = :status";
            BigDecimal result = entityManager.createQuery(query, BigDecimal.class)
                .setParameter("status", OrderStatus.DONE)
                .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error getting backup revenue: {}", e.getMessage());
            return BigDecimal.ZERO;
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
    
    /**
     * Xóa user - Manager có thể xóa bất kỳ user nào
     * Doanh thu sẽ được backup trước khi xóa
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Manager deleting user with id: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Không cho phép xóa MANAGER
        if (user.getRole() == UserRole.MANAGER) {
            throw new com.utetea.backend.exception.BusinessException("Không thể xóa tài khoản Manager");
        }
        
        // Sử dụng UserProfileService để xóa (đã có logic backup)
        userProfileService.deleteAccount(user.getUsername());
        
        log.info("Manager successfully deleted user: {} (ID: {})", user.getUsername(), userId);
    }
    
    // ==================== REVENUE STATISTICS ====================
    
    @Transactional(readOnly = true)
    public com.utetea.backend.dto.RevenueStatisticsDto getRevenueStatistics(Integer days, Integer months) {
        log.info("Getting revenue statistics - days: {}, months: {}", days, months);
        
        com.utetea.backend.dto.RevenueStatisticsDto stats = new com.utetea.backend.dto.RevenueStatisticsDto();
        
        try {
            // Calculate total revenue from DONE orders + backup
            String totalRevenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status";
            BigDecimal totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
                .setParameter("status", OrderStatus.DONE)
                .getSingleResult();
            
            // Thêm doanh thu từ backup
            BigDecimal backupRevenue = getBackupTotalRevenue();
            totalRevenue = totalRevenue.add(backupRevenue);
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
                
                // Lấy thêm daily revenue từ backup
                String dailyBackupQuery = """
                    SELECT CAST(b.order_created_at AS DATE) as order_date, 
                           COALESCE(SUM(b.final_price), 0) as revenue,
                           COUNT(b.id) as order_count
                    FROM deleted_user_order_backup b 
                    WHERE b.order_status = 'DONE' 
                      AND b.order_created_at >= :cutoffDate
                    GROUP BY CAST(b.order_created_at AS DATE)
                    """;
                
                @SuppressWarnings("unchecked")
                List<Object[]> dailyBackupResults = entityManager.createNativeQuery(dailyBackupQuery)
                    .setParameter("cutoffDate", java.sql.Timestamp.from(cutoffDate))
                    .getResultList();
                
                // Merge daily results
                java.util.Map<java.time.LocalDate, com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue> dailyMap = new java.util.HashMap<>();
                
                for (Object[] row : dailyResults) {
                    java.time.LocalDate date = parseDateFromRow(row[0]);
                    BigDecimal revenue = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
                    Long orderCount = ((Number) row[2]).longValue();
                    dailyMap.put(date, new com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue(date, revenue, orderCount));
                }
                
                // Merge backup data
                for (Object[] row : dailyBackupResults) {
                    java.time.LocalDate date = parseDateFromRow(row[0]);
                    BigDecimal revenue = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
                    Long orderCount = ((Number) row[2]).longValue();
                    
                    if (dailyMap.containsKey(date)) {
                        com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue existing = dailyMap.get(date);
                        existing.setRevenue(existing.getRevenue().add(revenue));
                        existing.setOrderCount(existing.getOrderCount() + orderCount);
                    } else {
                        dailyMap.put(date, new com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue(date, revenue, orderCount));
                    }
                }
                
                List<com.utetea.backend.dto.RevenueStatisticsDto.DailyRevenue> dailyRevenues = new ArrayList<>(dailyMap.values());
                dailyRevenues.sort((a, b) -> a.getDate().compareTo(b.getDate()));
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
                
                // Lấy thêm monthly revenue từ backup
                String monthlyBackupQuery = """
                    SELECT YEAR(b.order_created_at) as year_val,
                           MONTH(b.order_created_at) as month_val,
                           COALESCE(SUM(b.final_price), 0) as revenue,
                           COUNT(b.id) as order_count
                    FROM deleted_user_order_backup b 
                    WHERE b.order_status = 'DONE'
                      AND b.order_created_at >= :cutoffDate
                    GROUP BY YEAR(b.order_created_at), MONTH(b.order_created_at)
                    """;
                
                @SuppressWarnings("unchecked")
                List<Object[]> monthlyBackupResults = entityManager.createNativeQuery(monthlyBackupQuery)
                    .setParameter("cutoffDate", java.sql.Timestamp.from(cutoffDate))
                    .getResultList();
                
                // Merge monthly results
                java.util.Map<String, com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue> monthlyMap = new java.util.HashMap<>();
                
                for (Object[] row : monthlyResults) {
                    Integer year = ((Number) row[0]).intValue();
                    Integer month = ((Number) row[1]).intValue();
                    BigDecimal revenue = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(row[2].toString());
                    Long orderCount = ((Number) row[3]).longValue();
                    String key = year + "-" + month;
                    monthlyMap.put(key, new com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue(year, month, revenue, orderCount));
                }
                
                // Merge backup data
                for (Object[] row : monthlyBackupResults) {
                    Integer year = ((Number) row[0]).intValue();
                    Integer month = ((Number) row[1]).intValue();
                    BigDecimal revenue = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(row[2].toString());
                    Long orderCount = ((Number) row[3]).longValue();
                    String key = year + "-" + month;
                    
                    if (monthlyMap.containsKey(key)) {
                        com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue existing = monthlyMap.get(key);
                        existing.setRevenue(existing.getRevenue().add(revenue));
                        existing.setOrderCount(existing.getOrderCount() + orderCount);
                    } else {
                        monthlyMap.put(key, new com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue(year, month, revenue, orderCount));
                    }
                }
                
                List<com.utetea.backend.dto.RevenueStatisticsDto.MonthlyRevenue> monthlyRevenues = new ArrayList<>(monthlyMap.values());
                monthlyRevenues.sort((a, b) -> {
                    int yearCompare = a.getYear().compareTo(b.getYear());
                    return yearCompare != 0 ? yearCompare : a.getMonth().compareTo(b.getMonth());
                });
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
            
            log.info("Revenue statistics calculated - total: {} (including backup), daily entries: {}, monthly entries: {}, top drinks: {}",
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
    
    /**
     * Helper method để parse date từ database result
     */
    private java.time.LocalDate parseDateFromRow(Object dateObj) {
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof java.time.LocalDate) {
            return (java.time.LocalDate) dateObj;
        } else {
            return java.time.LocalDate.parse(dateObj.toString().substring(0, 10));
        }
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
