package com.utetea.backend.repository;

import com.utetea.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    
    // FIX LazyInitializationException: Fetch Order và User cùng lúc
    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH oi.drink " +
           "WHERE oi.id = :id")
    Optional<OrderItem> findByIdWithOrder(@Param("id") Long id);
}
