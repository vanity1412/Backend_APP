package com.utetea.backend.repository;

import com.utetea.backend.model.GroupOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupOrderItemRepository extends JpaRepository<GroupOrderItem, Long> {
    
    @Query("SELECT i FROM GroupOrderItem i " +
           "JOIN FETCH i.user " +
           "JOIN FETCH i.drink " +
           "WHERE i.groupOrder.id = :groupOrderId")
    List<GroupOrderItem> findByGroupOrderIdWithDetails(@Param("groupOrderId") Long groupOrderId);
    
    List<GroupOrderItem> findByGroupOrderIdAndUserId(Long groupOrderId, Long userId);
    
    @Modifying
    @Query("DELETE FROM GroupOrderItem i WHERE i.groupOrder.id = :groupOrderId AND i.user.id = :userId")
    void deleteByGroupOrderIdAndUserId(@Param("groupOrderId") Long groupOrderId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(i) FROM GroupOrderItem i WHERE i.groupOrder.id = :groupOrderId AND i.user.id = :userId")
    int countByGroupOrderIdAndUserId(@Param("groupOrderId") Long groupOrderId, @Param("userId") Long userId);
    
    // Xóa tất cả items của user (khi user xóa tài khoản)
    @Modifying
    void deleteByUserId(Long userId);
}
