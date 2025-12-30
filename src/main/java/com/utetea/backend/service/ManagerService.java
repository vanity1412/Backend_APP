package com.utetea.backend.service;

import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.UserDto;
import com.utetea.backend.dto.StoreDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.Store;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.DeletedUserOrderBackupRepository;
import com.utetea.backend.repository.OrderRepository;
import com.utetea.backend.repository.StoreRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DrinkCategoryService categoryService;
    private final DeletedUserOrderBackupRepository deletedUserOrderBackupRepository;
    private final UserProfileService userProfileService;
    
    @org.springframework.beans.factory.annotation.Autowired
    private jakarta.persistence.EntityManager entityManager;
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Lấy current user từ Security Context
     */
    private User getCurrentManager() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameWithManagedStores(username)
            .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + username));
    }
    
    /**
     * Kiểm tra current user có phải ADMIN không
     */
    private boolean isCurrentUserAdmin() {
        User currentUser = getCurrentManager();
        return currentUser.getRole() == UserRole.ADMIN;
    }
    
    /**
     * Yêu cầu quyền ADMIN để thực hiện action
     */
    private void requireAdminRole() {
        if (!isCurrentUserAdmin()) {
            throw new BusinessException("Chỉ Admin mới có quyền thực hiện thao tác này");
        }
    }
    
    /**
     * Lấy danh sách store IDs mà manager được phép quản lý
     * ADMIN và Super Manager (không gán store nào) -> trả về null (quản lý tất cả)
     */
    private List<Long> getManagedStoreIds(User manager) {
        // ADMIN luôn quản lý tất cả
        if (manager.getRole() == UserRole.ADMIN) {
            return null;
        }
        if (manager.isSuperManager()) {
            return null; // Super Manager - quản lý tất cả
        }
        return manager.getManagedStores().stream()
            .map(Store::getId)
            .collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra quyền truy cập store
     */
    private void validateStoreAccess(User manager, Long storeId) {
        // ADMIN có quyền truy cập tất cả
        if (manager.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!manager.canManageStore(storeId)) {
            throw new BusinessException("Bạn không có quyền quản lý cửa hàng này");
        }
    }
    
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        User manager = getCurrentManager();
        List<Long> storeIds = getManagedStoreIds(manager);
        
        log.info("Getting dashboard summary for manager: {}, stores: {}", 
            manager.getUsername(), storeIds == null ? "ALL" : storeIds);
        
        try {
            Long pendingOrders;
            Long completedOrders;
            Long canceledOrders;
            Long totalOrders;
            BigDecimal totalRevenue;
            
            if (storeIds == null) {
                // Super Manager - xem tất cả
                pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
                completedOrders = orderRepository.countByStatus(OrderStatus.DONE);
                canceledOrders = orderRepository.countByStatus(OrderStatus.CANCELED);
                totalOrders = orderRepository.count();
                
                String totalRevenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status";
                totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
                    .setParameter("status", OrderStatus.DONE)
                    .getSingleResult();
                
                // Thêm doanh thu từ backup
                BigDecimal backupRevenue = getBackupTotalRevenue();
                totalRevenue = totalRevenue.add(backupRevenue);
                
                Long backupOrderCount = deletedUserOrderBackupRepository.count();
                totalOrders = totalOrders + backupOrderCount;
            } else {
                // Store Manager - chỉ xem stores được gán
                pendingOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.PENDING);
                completedOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.DONE);
                canceledOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.CANCELED);
                
                String countQuery = "SELECT COUNT(o) FROM Order o WHERE o.store.id IN :storeIds";
                totalOrders = entityManager.createQuery(countQuery, Long.class)
                    .setParameter("storeIds", storeIds)
                    .getSingleResult();
                
                String revenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status AND o.store.id IN :storeIds";
                totalRevenue = entityManager.createQuery(revenueQuery, BigDecimal.class)
                    .setParameter("status", OrderStatus.DONE)
                    .setParameter("storeIds", storeIds)
                    .getSingleResult();
                
                // Thêm doanh thu từ backup theo stores
                BigDecimal backupRevenue = getBackupTotalRevenueByStores(storeIds);
                totalRevenue = totalRevenue.add(backupRevenue);
            }
            
            // Get top selling drinks (filtered by stores if needed)
            String topDrinksQuery;
            List<Object[]> topDrinksResults;
            
            if (storeIds == null) {
                topDrinksQuery = """
                    SELECT oi.drinkNameSnapshot,
                           SUM(oi.quantity) as totalQuantity,
                           SUM(oi.itemPrice) as totalRevenue
                    FROM OrderItem oi
                    JOIN oi.order o
                    WHERE o.status = :status
                    GROUP BY oi.drinkNameSnapshot
                    ORDER BY SUM(oi.quantity) DESC
                    """;
                topDrinksResults = entityManager.createQuery(topDrinksQuery, Object[].class)
                    .setParameter("status", OrderStatus.DONE)
                    .setMaxResults(5)
                    .getResultList();
            } else {
                topDrinksQuery = """
                    SELECT oi.drinkNameSnapshot,
                           SUM(oi.quantity) as totalQuantity,
                           SUM(oi.itemPrice) as totalRevenue
                    FROM OrderItem oi
                    JOIN oi.order o
                    WHERE o.status = :status AND o.store.id IN :storeIds
                    GROUP BY oi.drinkNameSnapshot
                    ORDER BY SUM(oi.quantity) DESC
                    """;
                topDrinksResults = entityManager.createQuery(topDrinksQuery, Object[].class)
                    .setParameter("status", OrderStatus.DONE)
                    .setParameter("storeIds", storeIds)
                    .setMaxResults(5)
                    .getResultList();
            }
            
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
    
    /**
     * Lấy tổng doanh thu từ backup theo stores
     */
    private BigDecimal getBackupTotalRevenueByStores(List<Long> storeIds) {
        try {
            String query = "SELECT COALESCE(SUM(b.finalPrice), 0) FROM DeletedUserOrderBackup b WHERE b.orderStatus = :status AND b.store.id IN :storeIds";
            BigDecimal result = entityManager.createQuery(query, BigDecimal.class)
                .setParameter("status", OrderStatus.DONE)
                .setParameter("storeIds", storeIds)
                .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error getting backup revenue by stores: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        User manager = getCurrentManager();
        List<Long> storeIds = getManagedStoreIds(manager);
        
        log.info("Getting orders with status: {}, stores: {}", status, storeIds == null ? "ALL" : storeIds);
        
        if (storeIds == null) {
            // Super Manager - xem tất cả
            if (status == null) {
                return orderService.getAllOrders(pageable);
            }
            return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(order -> orderService.getOrderById(order.getId()));
        } else {
            // Store Manager - chỉ xem stores được gán
            if (status == null) {
                return orderRepository.findByStoreIdInOrderByCreatedAtDesc(storeIds, pageable)
                    .map(order -> orderService.getOrderById(order.getId()));
            }
            return orderRepository.findByStoreIdInAndStatusOrderByCreatedAtDesc(storeIds, status, pageable)
                .map(order -> orderService.getOrderById(order.getId()));
        }
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        User manager = getCurrentManager();
        
        // Lấy order để kiểm tra quyền
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        
        // Kiểm tra quyền truy cập store
        if (!manager.isSuperManager()) {
            validateStoreAccess(manager, order.getStore().getId());
        }
        
        log.info("Manager {} updating order {} to status {}", manager.getUsername(), orderId, newStatus);
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
     * Nâng cấp User lên làm Manager để quản lý cửa hàng
     * CHỈ ADMIN mới có quyền thực hiện
     */
    @Transactional
    public UserDto promoteToManager(Long userId) {
        requireAdminRole();
        log.info("Admin promoting user {} to MANAGER", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Kiểm tra nếu đã là Manager hoặc Admin
        if (user.getRole() == UserRole.MANAGER) {
            throw new BusinessException("Người dùng này đã là Manager");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Không thể thay đổi role của Admin");
        }
        
        // Kiểm tra nếu user bị khóa
        if (user.getIsBlocked() != null && user.getIsBlocked()) {
            throw new BusinessException("Không thể nâng cấp tài khoản đang bị khóa");
        }
        
        user.setRole(UserRole.MANAGER);
        User savedUser = userRepository.save(user);
        
        log.info("User {} has been promoted to MANAGER successfully", userId);
        return new UserDto(savedUser);
    }
    
    /**
     * Hạ cấp Manager xuống User thường
     * CHỈ ADMIN mới có quyền thực hiện
     */
    @Transactional
    public UserDto demoteToUser(Long userId) {
        requireAdminRole();
        log.info("Admin demoting user {} to USER", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Không cho phép hạ cấp Admin
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Không thể hạ cấp Admin");
        }
        
        // Kiểm tra nếu đã là User
        if (user.getRole() == UserRole.USER) {
            throw new BusinessException("Người dùng này đã là User thường");
        }
        
        user.setRole(UserRole.USER);
        // Xóa tất cả stores được gán khi hạ cấp
        user.getManagedStores().clear();
        User savedUser = userRepository.save(user);
        
        log.info("User {} has been demoted to USER successfully", userId);
        return new UserDto(savedUser);
    }
    
    // ==================== STORE ASSIGNMENT FOR MANAGER ====================
    
    /**
     * Lấy danh sách stores mà Manager được gán quản lý
     */
    @Transactional(readOnly = true)
    public List<StoreDto> getManagedStores(Long managerId) {
        User manager = userRepository.findByIdWithManagedStores(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        
        if (manager.getRole() != UserRole.MANAGER) {
            throw new BusinessException("Người dùng này không phải Manager");
        }
        
        return manager.getManagedStores().stream()
            .map(this::mapToStoreDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy danh sách stores của current manager
     */
    @Transactional(readOnly = true)
    public List<StoreDto> getMyManagedStores() {
        User manager = getCurrentManager();
        
        // ADMIN xem tất cả stores
        if (manager.getRole() == UserRole.ADMIN || manager.isSuperManager()) {
            return storeRepository.findAll().stream()
                .map(this::mapToStoreDto)
                .collect(Collectors.toList());
        }
        
        return manager.getManagedStores().stream()
            .map(this::mapToStoreDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Gán store cho Manager
     * CHỈ ADMIN mới có quyền thực hiện
     */
    @Transactional
    public UserDto assignStoreToManager(Long managerId, Long storeId) {
        requireAdminRole();
        log.info("Admin assigning store {} to manager {}", storeId, managerId);
        
        User manager = userRepository.findByIdWithManagedStores(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        
        if (manager.getRole() != UserRole.MANAGER) {
            throw new BusinessException("Người dùng này không phải Manager");
        }
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        
        // Kiểm tra đã gán chưa
        if (manager.getManagedStores().contains(store)) {
            throw new BusinessException("Manager đã được gán quản lý cửa hàng này");
        }
        
        manager.getManagedStores().add(store);
        User savedUser = userRepository.save(manager);
        
        log.info("Store {} assigned to manager {} successfully", storeId, managerId);
        return new UserDto(savedUser);
    }
    
    /**
     * Gán nhiều stores cho Manager
     * CHỈ ADMIN mới có quyền thực hiện
     */
    @Transactional
    public UserDto assignStoresToManager(Long managerId, List<Long> storeIds) {
        requireAdminRole();
        log.info("Admin assigning stores {} to manager {}", storeIds, managerId);
        
        User manager = userRepository.findByIdWithManagedStores(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        
        if (manager.getRole() != UserRole.MANAGER) {
            throw new BusinessException("Người dùng này không phải Manager");
        }
        
        List<Store> stores = storeRepository.findAllById(storeIds);
        if (stores.size() != storeIds.size()) {
            throw new BusinessException("Một số cửa hàng không tồn tại");
        }
        
        // Xóa tất cả stores cũ và gán mới
        manager.getManagedStores().clear();
        manager.getManagedStores().addAll(stores);
        User savedUser = userRepository.save(manager);
        
        log.info("Stores {} assigned to manager {} successfully", storeIds, managerId);
        return new UserDto(savedUser);
    }
    
    /**
     * Bỏ gán store khỏi Manager
     * CHỈ ADMIN mới có quyền thực hiện
     */
    @Transactional
    public UserDto removeStoreFromManager(Long managerId, Long storeId) {
        requireAdminRole();
        log.info("Admin removing store {} from manager {}", storeId, managerId);
        
        User manager = userRepository.findByIdWithManagedStores(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        
        if (manager.getRole() != UserRole.MANAGER) {
            throw new BusinessException("Người dùng này không phải Manager");
        }
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        
        if (!manager.getManagedStores().contains(store)) {
            throw new BusinessException("Manager không quản lý cửa hàng này");
        }
        
        manager.getManagedStores().remove(store);
        User savedUser = userRepository.save(manager);
        
        log.info("Store {} removed from manager {} successfully", storeId, managerId);
        return new UserDto(savedUser);
    }
    
    /**
     * Kiểm tra Manager có phải Super Manager không
     */
    @Transactional(readOnly = true)
    public boolean isSuperManager(Long managerId) {
        User manager = userRepository.findByIdWithManagedStores(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        return manager.isSuperManager();
    }
    
    private StoreDto mapToStoreDto(Store store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId());
        dto.setStoreName(store.getStoreName());
        dto.setAddress(store.getAddress());
        dto.setPhone(store.getPhone());
        dto.setLatitude(store.getLatitude());
        dto.setLongitude(store.getLongitude());
        dto.setOpenTime(store.getOpenTime());
        dto.setCloseTime(store.getCloseTime());
        return dto;
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
     * Xóa user - CHỈ ADMIN mới có quyền xóa Manager
     * Doanh thu sẽ được backup trước khi xóa
     */
    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentManager();
        log.info("{} deleting user with id: {}", currentUser.getRole(), userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Không cho phép xóa ADMIN
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Không thể xóa tài khoản Admin");
        }
        
        // Chỉ ADMIN mới có quyền xóa MANAGER
        if (user.getRole() == UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Chỉ Admin mới có quyền xóa tài khoản Manager");
        }
        
        // Sử dụng UserProfileService để xóa (đã có logic backup)
        userProfileService.deleteAccount(user.getUsername());
        
        log.info("{} successfully deleted user: {} (ID: {})", currentUser.getRole(), user.getUsername(), userId);
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
