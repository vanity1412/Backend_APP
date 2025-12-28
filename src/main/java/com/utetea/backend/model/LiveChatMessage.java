package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho tin nhắn trong cuộc hội thoại
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "live_chat_messages")
public class LiveChatMessage extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
}
