package com.utetea.backend.dto;

import com.utetea.backend.model.DeletedUserRiskScoreBackup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletedUserRiskScoreDto {
    private Long id;
    private Long deletedUserId;
    private String deletedUsername;
    private Long originalRiskScoreId;
    private Integer totalScore;
    private String riskLevel;
    private Integer loginFailedCount;
    private Integer orderCancelCount;
    private Integer paymentFailedCount;
    private Integer rateLimitHitCount;
    private Integer promotionAbuseCount;
    private Integer spamRequestCount;
    private String lastIpAddress;
    private LocalDateTime lastScoreReset;
    private String adminNote;
    private Long notedBy;
    private LocalDateTime notedAt;
    private Boolean autoBlocked;
    private LocalDateTime autoBlockedAt;
    private String autoBlockedReason;
    private Instant riskScoreCreatedAt;
    private Instant riskScoreUpdatedAt;
    private Instant backupCreatedAt;
    private String note;

    public static DeletedUserRiskScoreDto fromEntity(DeletedUserRiskScoreBackup backup) {
        return DeletedUserRiskScoreDto.builder()
                .id(backup.getId())
                .deletedUserId(backup.getDeletedUserId())
                .deletedUsername(backup.getDeletedUsername())
                .originalRiskScoreId(backup.getOriginalRiskScoreId())
                .totalScore(backup.getTotalScore())
                .riskLevel(backup.getRiskLevel())
                .loginFailedCount(backup.getLoginFailedCount())
                .orderCancelCount(backup.getOrderCancelCount())
                .paymentFailedCount(backup.getPaymentFailedCount())
                .rateLimitHitCount(backup.getRateLimitHitCount())
                .promotionAbuseCount(backup.getPromotionAbuseCount())
                .spamRequestCount(backup.getSpamRequestCount())
                .lastIpAddress(backup.getLastIpAddress())
                .lastScoreReset(backup.getLastScoreReset())
                .adminNote(backup.getAdminNote())
                .notedBy(backup.getNotedBy())
                .notedAt(backup.getNotedAt())
                .autoBlocked(backup.getAutoBlocked())
                .autoBlockedAt(backup.getAutoBlockedAt())
                .autoBlockedReason(backup.getAutoBlockedReason())
                .riskScoreCreatedAt(backup.getRiskScoreCreatedAt())
                .riskScoreUpdatedAt(backup.getRiskScoreUpdatedAt())
                .backupCreatedAt(backup.getBackupCreatedAt())
                .note(backup.getNote())
                .build();
    }
}
