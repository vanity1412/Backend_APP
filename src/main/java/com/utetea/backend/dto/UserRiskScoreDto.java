package com.utetea.backend.dto;

import com.utetea.backend.model.UserActivityLog.RiskLevel;
import com.utetea.backend.model.UserRiskScore;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRiskScoreDto {
    
    private Long id;
    private Long userId;
    private String username;
    private String userEmail;
    private String userFullName;
    private String userAvatarUrl;
    private Boolean userBlocked;
    private Boolean userActive;
    
    private Integer totalScore;
    private RiskLevel riskLevel;
    private String riskLevelDisplay;
    
    // Chi tiết các chỉ số
    private Integer loginFailedCount;
    private Integer orderCancelCount;
    private Integer paymentFailedCount;
    private Integer rateLimitHitCount;
    private Integer promotionAbuseCount;
    private Integer spamRequestCount;
    
    // IP gần nhất của user
    private String lastIpAddress;
    
    private LocalDateTime lastScoreReset;
    
    // Admin note
    private String adminNote;
    private Long notedBy;
    private LocalDateTime notedAt;
    
    // Auto-block info
    private Boolean autoBlocked;
    private LocalDateTime autoBlockedAt;
    private String autoBlockedReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserRiskScoreDto fromEntity(UserRiskScore score) {
        UserRiskScoreDto dto = new UserRiskScoreDto();
        dto.setId(score.getId());
        
        if (score.getUser() != null) {
            dto.setUserId(score.getUser().getId());
            dto.setUsername(score.getUser().getUsername());
            dto.setUserEmail(score.getUser().getEmail());
            dto.setUserFullName(score.getUser().getFullName());
            dto.setUserAvatarUrl(score.getUser().getAvatarUrl());
            dto.setUserBlocked(score.getUser().getIsBlocked());
            dto.setUserActive(score.getUser().getActive());
        }
        
        dto.setTotalScore(score.getTotalScore());
        dto.setRiskLevel(score.getRiskLevel());
        dto.setRiskLevelDisplay(getRiskLevelDisplay(score.getRiskLevel()));
        
        dto.setLoginFailedCount(score.getLoginFailedCount());
        dto.setOrderCancelCount(score.getOrderCancelCount());
        dto.setPaymentFailedCount(score.getPaymentFailedCount());
        dto.setRateLimitHitCount(score.getRateLimitHitCount());
        dto.setPromotionAbuseCount(score.getPromotionAbuseCount());
        dto.setSpamRequestCount(score.getSpamRequestCount());
        
        // Lấy IP gần nhất từ user nếu có
        dto.setLastIpAddress(score.getLastIpAddress());
        
        dto.setLastScoreReset(score.getLastScoreReset());
        
        dto.setAdminNote(score.getAdminNote());
        dto.setNotedBy(score.getNotedBy());
        dto.setNotedAt(score.getNotedAt());
        
        dto.setAutoBlocked(score.getAutoBlocked());
        dto.setAutoBlockedAt(score.getAutoBlockedAt());
        dto.setAutoBlockedReason(score.getAutoBlockedReason());
        
        if (score.getCreatedAt() != null) {
            dto.setCreatedAt(score.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (score.getUpdatedAt() != null) {
            dto.setUpdatedAt(score.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return dto;
    }

    private static String getRiskLevelDisplay(RiskLevel level) {
        if (level == null) return "";
        return switch (level) {
            case NORMAL -> "Bình thường";
            case WARNING -> "Cảnh báo";
            case SUSPICIOUS -> "Đáng ngờ";
            case CRITICAL -> "Nghiêm trọng";
        };
    }
}
