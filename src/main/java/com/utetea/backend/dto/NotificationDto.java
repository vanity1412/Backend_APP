package com.utetea.backend.dto;

import com.utetea.backend.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String content;
    private NotificationType type;
    private Boolean isRead;
    private Long relatedId;
    private LocalDateTime createdAt;
}
