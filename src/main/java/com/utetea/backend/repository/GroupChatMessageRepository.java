package com.utetea.backend.repository;

import com.utetea.backend.model.GroupChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    void deleteByGroupOrderId(Long groupOrderId);
}
