package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng backup lưu trữ thông tin monitoring alerts của user đã bị xóa
 * Dùng để manager vẫn có thể xem lịch sử cảnh báo, tracking bảo mật
 * mà user vẫn xóa được sạch tài khoản
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "deleted_user_monitoring_alert_backup")
public class DeletedUserMonitoringAlertBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin user đã xóa (target user của alert)
    @Column(name = "deleted_user_id")
    private Long deletedUserId;

    @Column(name = "deleted_username", length = 100)
    private String deletedUsername;

    // Thông tin alert gốc
    @Column(name = "original_alert_id")
    private Long originalAlertId;

    @Column(name = "alert_type", length = 50)
    private String alertType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", length = 20)
    private String status;

    // Thông tin xử lý
    @Column(name = "handled_by_user_id")
    private Long handledByUserId;

    @Column(name = "handled_by_username", length = 100)
    private String handledByUsername;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;

    @Column(name = "action_taken", length = 30)
    private String actionTaken;

    // Metadata
    @Column(name = "activity_log_id")
    private Long activityLogId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "notification_sent")
    private Boolean notificationSent;

    // Thời gian alert gốc
    @Column(name = "alert_created_at")
    private java.time.Instant alertCreatedAt;

    // Thời gian backup
    @Column(name = "backup_created_at")
    private java.time.Instant backupCreatedAt;

    // Ghi chú
    @Column(length = 500)
    private String note;

    @PrePersist
    protected void onCreate() {
        backupCreatedAt = java.time.Instant.now();
    }
}
