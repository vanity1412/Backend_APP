package com.utetea.backend.repository;

import com.utetea.backend.model.GroupOrderMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupOrderMemberRepository extends JpaRepository<GroupOrderMember, Long> {
    
    @Query("SELECT m FROM GroupOrderMember m " +
           "JOIN FETCH m.user " +
           "WHERE m.groupOrder.id = :groupOrderId")
    List<GroupOrderMember> findByGroupOrderIdWithUser(@Param("groupOrderId") Long groupOrderId);
    
    Optional<GroupOrderMember> findByGroupOrderIdAndUserId(Long groupOrderId, Long userId);
    
    boolean existsByGroupOrderIdAndUserId(Long groupOrderId, Long userId);
    
    int countByGroupOrderId(Long groupOrderId);
    
    @Modifying
    @Query("DELETE FROM GroupOrderMember m WHERE m.groupOrder.id = :groupOrderId AND m.user.id = :userId")
    void deleteByGroupOrderIdAndUserId(@Param("groupOrderId") Long groupOrderId, @Param("userId") Long userId);
    
    // Xóa tất cả membership của user (khi user xóa tài khoản)
    @Modifying
    void deleteByUserId(Long userId);
}
