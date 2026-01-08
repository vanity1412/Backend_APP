package com.utetea.backend.repository;

import com.utetea.backend.model.Order;
import com.utetea.backend.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // FIX: Tách query thành 2 bước để tránh MultipleBagFetchException
    // Bước 1: Fetch orders với items và drink
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.store " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.drink " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItemsOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // FIX: Fetch toppings riêng cho các order items
    @Query("SELECT DISTINCT i FROM OrderItem i " +
           "LEFT JOIN FETCH i.toppings " +
           "WHERE i.order.id IN :orderIds")
    List<com.utetea.backend.model.OrderItem> findOrderItemsWithToppings(@Param("orderIds") List<Long> orderIds);
    
    // FIX Performance: JOIN FETCH cho single order - tách thành 2 query
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.store " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.drink " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
    
    // Fetch toppings cho single order
    @Query("SELECT DISTINCT i FROM OrderItem i " +
           "LEFT JOIN FETCH i.toppings " +
           "WHERE i.order.id = :orderId")
    List<com.utetea.backend.model.OrderItem> findOrderItemsWithToppingsByOrderId(@Param("orderId") Long orderId);
    
    List<Order> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, OrderStatus status);
    
    // FIX: Query với JOIN FETCH cho current orders (không bao gồm DONE)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.store " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.drink " +
           "WHERE o.user.id = :userId AND o.status <> :status " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusNotWithItemsOrderByCreatedAtDesc(
            @Param("userId") Long userId, 
            @Param("status") OrderStatus status);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long countOrdersSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT SUM(o.finalPrice) FROM Order o WHERE o.status = 'DONE' AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    Double calculateRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // For Manager Dashboard
    @Query("SELECT SUM(o.finalPrice) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateTotalRevenue(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<Order> findByStatusAndCreatedAtBetween(
            OrderStatus status, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);
    
    // ==================== STORE-BASED QUERIES FOR MANAGER ====================
    
    // Lấy orders theo store
    Page<Order> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    
    // Lấy orders theo store và status
    Page<Order> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, OrderStatus status, Pageable pageable);
    
    // Lấy orders theo nhiều stores
    @Query("SELECT o FROM Order o WHERE o.store.id IN :storeIds ORDER BY o.createdAt DESC")
    Page<Order> findByStoreIdInOrderByCreatedAtDesc(@Param("storeIds") List<Long> storeIds, Pageable pageable);
    
    // Lấy orders theo nhiều stores và status
    @Query("SELECT o FROM Order o WHERE o.store.id IN :storeIds AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStoreIdInAndStatusOrderByCreatedAtDesc(
        @Param("storeIds") List<Long> storeIds, 
        @Param("status") OrderStatus status, 
        Pageable pageable);
    
    // Đếm orders theo store
    Long countByStoreId(Long storeId);
    
    // Đếm orders theo store và status
    Long countByStoreIdAndStatus(Long storeId, OrderStatus status);
    
    // Đếm orders theo nhiều stores và status
    @Query("SELECT COUNT(o) FROM Order o WHERE o.store.id IN :storeIds AND o.status = :status")
    Long countByStoreIdInAndStatus(@Param("storeIds") List<Long> storeIds, @Param("status") OrderStatus status);
    
    // Delete all orders by user ID
    @Modifying
    void deleteByUserId(Long userId);
    
    // ==================== SORTED BY STATUS PRIORITY (PENDING -> MAKING -> SHIPPING -> READY -> DONE) ====================
    // Sắp xếp theo thứ tự trạng thái ưu tiên, trong mỗi trạng thái sắp xếp từ cũ đến mới (ASC)
    
    @Query("SELECT o FROM Order o ORDER BY " +
           "CASE o.status " +
           "WHEN com.utetea.backend.model.OrderStatus.PENDING THEN 1 " +
           "WHEN com.utetea.backend.model.OrderStatus.MAKING THEN 2 " +
           "WHEN com.utetea.backend.model.OrderStatus.SHIPPING THEN 3 " +
           "WHEN com.utetea.backend.model.OrderStatus.READY THEN 4 " +
           "WHEN com.utetea.backend.model.OrderStatus.DONE THEN 5 " +
           "WHEN com.utetea.backend.model.OrderStatus.CANCELED THEN 6 " +
           "ELSE 7 END ASC, " +
           "o.createdAt ASC")
    Page<Order> findAllOrderByStatusPriorityAndCreatedAtAsc(Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt ASC")
    Page<Order> findByStatusOrderByCreatedAtAsc(@Param("status") OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.store.id IN :storeIds ORDER BY " +
           "CASE o.status " +
           "WHEN com.utetea.backend.model.OrderStatus.PENDING THEN 1 " +
           "WHEN com.utetea.backend.model.OrderStatus.MAKING THEN 2 " +
           "WHEN com.utetea.backend.model.OrderStatus.SHIPPING THEN 3 " +
           "WHEN com.utetea.backend.model.OrderStatus.READY THEN 4 " +
           "WHEN com.utetea.backend.model.OrderStatus.DONE THEN 5 " +
           "WHEN com.utetea.backend.model.OrderStatus.CANCELED THEN 6 " +
           "ELSE 7 END ASC, " +
           "o.createdAt ASC")
    Page<Order> findByStoreIdInOrderByStatusPriorityAndCreatedAtAsc(@Param("storeIds") List<Long> storeIds, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.store.id IN :storeIds AND o.status = :status ORDER BY o.createdAt ASC")
    Page<Order> findByStoreIdInAndStatusOrderByCreatedAtAsc(
        @Param("storeIds") List<Long> storeIds, 
        @Param("status") OrderStatus status, 
        Pageable pageable);
}
