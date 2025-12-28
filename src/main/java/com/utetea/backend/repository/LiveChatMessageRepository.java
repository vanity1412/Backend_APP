package com.utetea.backend.repository;

import com.utetea.backend.model.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, Long> {

    @Query("SELECT m FROM LiveChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt ASC")
    List<LiveChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE LiveChatMessage m SET m.isRead = true " +
           "WHERE m.conversation.id = :conversationId AND m.senderType = :senderType AND m.isRead = false")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("senderType") com.utetea.backend.model.SenderType senderType);
}
