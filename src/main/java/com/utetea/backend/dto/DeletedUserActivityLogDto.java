package com.utetea.backend.dto;

import com.utetea.backend.model.DeletedUserActivityLogBackup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletedUserActivityLogDto {
    private Long id;
    private Long deletedUserId;
    private String deletedUsername;
    private Long originalLogId;
    private String activityType;
    private String description;
    private String riskLevel;
    private String ipAddress;
    private String deviceInfo;
    private String userAgent;
    private String endpoint;
    private String requestMethod;
    private Integer responseStatus;
    private Long relatedId;
    private String extraData;
    private Instant activityCreatedAt;
    private Instant backupCreatedAt;
    private String note;

    public static DeletedUserActivityLogDto fromEntity(DeletedUserActivityLogBackup backup) {
        return DeletedUserActivityLogDto.builder()
                .id(backup.getId())
                .deletedUserId(backup.getDeletedUserId())
                .deletedUsername(backup.getDeletedUsername())
                .originalLogId(backup.getOriginalLogId())
                .activityType(backup.getActivityType())
                .description(backup.getDescription())
                .riskLevel(backup.getRiskLevel())
                .ipAddress(backup.getIpAddress())
                .deviceInfo(backup.getDeviceInfo())
                .userAgent(backup.getUserAgent())
                .endpoint(backup.getEndpoint())
                .requestMethod(backup.getRequestMethod())
                .responseStatus(backup.getResponseStatus())
                .relatedId(backup.getRelatedId())
                .extraData(backup.getExtraData())
                .activityCreatedAt(backup.getActivityCreatedAt())
                .backupCreatedAt(backup.getBackupCreatedAt())
                .note(backup.getNote())
                .build();
    }
}
