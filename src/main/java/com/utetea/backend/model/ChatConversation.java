package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho cuộc hội thoại giữa User và Manager
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "chat_conversations")
public class ChatConversation extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.WAITING;

    @Column(length = 255)
    private String subject;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "unread_user", nullable = false)
    private Integer unreadUser = 0;

    @Column(name = "unread_manager", nullable = false)
    private Integer unreadManager = 0;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<LiveChatMessage> messages = new ArrayList<>();
}
