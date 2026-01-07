package com.utetea.backend.dto;

import com.utetea.backend.model.DeletedUserMonitoringAlertBackup;
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
public class DeletedUserMonitoringAlertDto {
    private Long id;
    private Long deletedUserId;
    private String deletedUsername;
    private Long originalAlertId;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String status;
    private Long handledByUserId;
    private String handledByUsername;
    private LocalDateTime handledAt;
    private String handlerNote;
    private String actionTaken;
    private Long activityLogId;
    private String ipAddress;
    private Boolean notificationSent;
    private Instant alertCreatedAt;
    private Instant backupCreatedAt;
    private String note;

    public static DeletedUserMonitoringAlertDto fromEntity(DeletedUserMonitoringAlertBackup backup) {
        return DeletedUserMonitoringAlertDto.builder()
                .id(backup.getId())
                .deletedUserId(backup.getDeletedUserId())
                .deletedUsername(backup.getDeletedUsername())
                .originalAlertId(backup.getOriginalAlertId())
                .alertType(backup.getAlertType())
                .severity(backup.getSeverity())
                .title(backup.getTitle())
                .message(backup.getMessage())
                .status(backup.getStatus())
                .handledByUserId(backup.getHandledByUserId())
                .handledByUsername(backup.getHandledByUsername())
                .handledAt(backup.getHandledAt())
                .handlerNote(backup.getHandlerNote())
                .actionTaken(backup.getActionTaken())
                .activityLogId(backup.getActivityLogId())
                .ipAddress(backup.getIpAddress())
                .notificationSent(backup.getNotificationSent())
                .alertCreatedAt(backup.getAlertCreatedAt())
                .backupCreatedAt(backup.getBackupCreatedAt())
                .note(backup.getNote())
                .build();
    }
}
