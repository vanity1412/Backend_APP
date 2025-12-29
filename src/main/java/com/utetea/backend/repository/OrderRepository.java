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
    
    // FIX Performance: JOIN FETCH để tránh N+1 query khi load orders với items
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.store " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.drink " +
           "LEFT JOIN FETCH i.toppings " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItemsOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // FIX Performance: JOIN FETCH cho single order
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.store " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.drink " +
           "LEFT JOIN FETCH i.toppings " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
    
    List<Order> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, OrderStatus status);
    
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
    
    // Delete all orders by user ID
    @Modifying
    void deleteByUserId(Long userId);
}
