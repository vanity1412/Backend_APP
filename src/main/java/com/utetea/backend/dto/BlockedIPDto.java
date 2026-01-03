package com.utetea.backend.dto;

import com.utetea.backend.model.BlockedIP;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 🚫 DTO cho BlockedIP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedIPDto {
    private Long id;
    private String ipAddress;
    private String blockType;
    private String blockTypeDisplay;
    private String reason;
    
    private Long blockedById;
    private String blockedByUsername;
    
    private Instant blockedUntil;
    private Boolean isActive;
    private Boolean isCurrentlyBlocked;
    
    private Instant unblockedAt;
    private Long unblockedById;
    private String unblockedByUsername;
    private String unblockReason;
    
    private Long alertId;
    private Long relatedUserId;
    private String relatedUsername;
    
    private Long blockedRequestsCount;
    private Instant createdAt;

    public static BlockedIPDto fromEntity(BlockedIP entity, String blockedByUsername, 
                                          String unblockedByUsername, String relatedUsername) {
        return BlockedIPDto.builder()
                .id(entity.getId())
                .ipAddress(entity.getIpAddress())
                .blockType(entity.getBlockType().name())
                .blockTypeDisplay(getBlockTypeDisplay(entity.getBlockType()))
                .reason(entity.getReason())
                .blockedById(entity.getBlockedById())
                .blockedByUsername(blockedByUsername)
                .blockedUntil(entity.getBlockedUntil())
                .isActive(entity.getIsActive())
                .isCurrentlyBlocked(entity.isCurrentlyBlocked())
                .unblockedAt(entity.getUnblockedAt())
                .unblockedById(entity.getUnblockedById())
                .unblockedByUsername(unblockedByUsername)
                .unblockReason(entity.getUnblockReason())
                .alertId(entity.getAlertId())
                .relatedUserId(entity.getRelatedUserId())
                .relatedUsername(relatedUsername)
                .blockedRequestsCount(entity.getBlockedRequestsCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static String getBlockTypeDisplay(BlockedIP.BlockType type) {
        return switch (type) {
            case TEMPORARY -> "Tạm thời";
            case PERMANENT -> "Vĩnh viễn";
            case AUTO -> "Tự động";
        };
    }
}
