package com.utetea.backend.repository;

import com.utetea.backend.model.GroupChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {
    
    @Query("SELECT m FROM GroupChatMessage m WHERE m.groupOrder.id = :groupOrderId ORDER BY m.createdAt ASC")
    List<GroupChatMessage> findByGroupOrderIdOrderByCreatedAtAsc(@Param("groupOrderId") Long groupOrderId);
    
    @Query("SELECT m FROM GroupChatMessage m WHERE m.groupOrder.id = :groupOrderId ORDER BY m.createdAt DESC")
    Page<GroupChatMessage> findByGroupOrderIdOrderByCreatedAtDesc(@Param("groupOrderId") Long groupOrderId, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM GroupChatMessage m WHERE m.groupOrder.id = :groupOrderId")
    long countByGroupOrderId(@Param("groupOrderId") Long groupOrderId);
    
    @Modifying
    void deleteByGroupOrderId(Long groupOrderId);
    
    // Xóa tất cả messages của các group orders mà user là host
    @Modifying
    @Query("DELETE FROM GroupChatMessage m WHERE m.groupOrder.id IN (SELECT g.id FROM GroupOrder g WHERE g.hostUser.id = :userId)")
    void deleteByHostUserId(@Param("userId") Long userId);

    // Xóa tất cả messages mà user đã gửi (khi xóa tài khoản)
    @Modifying
    void deleteBySenderId(Long senderId);
}
