package com.utetea.backend.repository;

import com.utetea.backend.model.ChatConversation;
import com.utetea.backend.model.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("SELECT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.manager " +
           "LEFT JOIN FETCH c.store " +
           "WHERE c.user.id = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.manager " +
           "LEFT JOIN FETCH c.store " +
           "WHERE c.status IN :statuses " +
           "ORDER BY c.updatedAt DESC")
    List<ChatConversation> findByStatusInOrderByUpdatedAtDesc(@Param("statuses") List<ConversationStatus> statuses);

    @Query("SELECT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.manager " +
           "LEFT JOIN FETCH c.store " +
           "WHERE c.manager.id = :managerId OR c.status = 'WAITING' " +
           "ORDER BY c.updatedAt DESC")
    List<ChatConversation> findForManager(@Param("managerId") Long managerId);

    @Query("SELECT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.manager " +
           "LEFT JOIN FETCH c.store " +
           "LEFT JOIN FETCH c.messages m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE c.id = :id")
    Optional<ChatConversation> findByIdWithMessages(@Param("id") Long id);

    @Query("SELECT c FROM ChatConversation c " +
           "WHERE c.user.id = :userId AND c.status IN ('WAITING', 'ACTIVE')")
    Optional<ChatConversation> findActiveByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM ChatConversation c " +
           "WHERE c.user.id = :userId AND c.store.id = :storeId AND c.status IN ('WAITING', 'ACTIVE')")
    Optional<ChatConversation> findActiveByUserIdAndStoreId(@Param("userId") Long userId, @Param("storeId") Long storeId);

    @Query("SELECT COUNT(c) FROM ChatConversation c WHERE c.status = 'WAITING'")
    Long countWaitingConversations();
    
    @Query("SELECT COUNT(c) FROM ChatConversation c WHERE c.status = 'WAITING' AND c.store.id IN :storeIds")
    Long countWaitingByStoreIds(@Param("storeIds") List<Long> storeIds);
    
    @Query("SELECT c FROM ChatConversation c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.manager " +
           "LEFT JOIN FETCH c.store " +
           "WHERE c.store.id IN :storeIds AND (c.status = 'WAITING' OR c.status = 'ACTIVE') " +
           "ORDER BY c.updatedAt DESC")
    List<ChatConversation> findByStoreIdIn(@Param("storeIds") List<Long> storeIds);
    
    // Delete all conversations by user ID
    @Modifying
    void deleteByUserId(Long userId);
}
