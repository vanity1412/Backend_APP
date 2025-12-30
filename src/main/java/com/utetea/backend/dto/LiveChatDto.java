package com.utetea.backend.dto;

import com.utetea.backend.model.ConversationStatus;
import com.utetea.backend.model.SenderType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class LiveChatDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ConversationDto {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private Long managerId;
        private String managerName;
        private Long storeId;
        private String storeName;
        private ConversationStatus status;
        private String subject;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private Integer unreadUser;
        private Integer unreadManager;
        private LocalDateTime createdAt;
        private List<MessageDto> messages;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MessageDto {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String senderName;
        private String senderAvatar;
        private String content;
        private SenderType senderType;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SendMessageRequest {
        private Long conversationId;
        private String content;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StartConversationRequest {
        private String subject;
        private String initialMessage;
        private Long storeId; // Chi nhánh user chọn để tư vấn
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ConversationListDto {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private Long storeId;
        private String storeName;
        private ConversationStatus status;
        private String subject;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private Integer unreadCount;
        private LocalDateTime createdAt;
    }
}
