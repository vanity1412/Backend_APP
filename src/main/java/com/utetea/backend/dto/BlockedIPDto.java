package com.utetea.backend.dto;

import com.utetea.backend.model.BlockedIP;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockedIPDto {
    
    private Long id;
    private String ipAddress;
    private String blockType;
    private String blockTypeDisplay;
    private String reason;
    
    private Long blockedById;
    private String blockedByUsername;
    
    private LocalDateTime blockedUntil;
    private Boolean isActive;
    private Boolean isCurrentlyBlocked;
    
    private LocalDateTime unblockedAt;
    private Long unblockedById;
    private String unblockedByUsername;
    private String unblockReason;
    
    private Long alertId;
    private Long relatedUserId;
    private String relatedUsername;
    
    private Long blockedRequestsCount;
    private LocalDateTime createdAt;

    public static BlockedIPDto fromEntity(BlockedIP entity) {
        BlockedIPDto dto = new BlockedIPDto();
        dto.setId(entity.getId());
        dto.setIpAddress(entity.getIpAddress());
        dto.setBlockType(entity.getBlockType().name());
        dto.setBlockTypeDisplay(getBlockTypeDisplay(entity.getBlockType()));
        dto.setReason(entity.getReason());
        
        if (entity.getBlockedBy() != null) {
            dto.setBlockedById(entity.getBlockedBy().getId());
            dto.setBlockedByUsername(entity.getBlockedBy().getUsername());
        }
        
        dto.setBlockedUntil(entity.getBlockedUntil());
        dto.setIsActive(entity.getIsActive());
        dto.setIsCurrentlyBlocked(entity.isCurrentlyBlocked());
        
        dto.setUnblockedAt(entity.getUnblockedAt());
        if (entity.getUnblockedBy() != null) {
            dto.setUnblockedById(entity.getUnblockedBy().getId());
            dto.setUnblockedByUsername(entity.getUnblockedBy().getUsername());
        }
        dto.setUnblockReason(entity.getUnblockReason());
        
        dto.setAlertId(entity.getAlertId());
        if (entity.getRelatedUser() != null) {
            dto.setRelatedUserId(entity.getRelatedUser().getId());
            dto.setRelatedUsername(entity.getRelatedUser().getUsername());
        }
        
        dto.setBlockedRequestsCount(entity.getBlockedRequestsCount());
        
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return dto;
    }

    private static String getBlockTypeDisplay(BlockedIP.BlockType type) {
        return switch (type) {
            case TEMPORARY -> "Tạm thời";
            case PERMANENT -> "Vĩnh viễn";
            case AUTO -> "Tự động";
        };
    }
}

/**
 * Request để block IP
 */
@Data @NoArgsConstructor @AllArgsConstructor
class BlockIPRequest {
    private String ipAddress;
    private String blockType; // TEMPORARY, PERMANENT
    private String reason;
    private Integer durationHours; // Cho TEMPORARY
    private Long relatedUserId; // Optional
    private Long alertId; // Optional
}

/**
 * Request để unblock IP
 */
@Data @NoArgsConstructor @AllArgsConstructor
class UnblockIPRequest {
    private String reason;
}
