package com.utetea.backend.dto;

import com.utetea.backend.model.GroupChatMessageType;
import lombok.*;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupChatMessageDto {
    private Long id;
    private Long groupOrderId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private GroupChatMessageType messageType;
    private Instant createdAt;
}
