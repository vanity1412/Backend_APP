package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Bảng backup lưu trữ thông tin activity logs của user đã bị xóa
 * Dùng để manager vẫn có thể xem lịch sử hoạt động, phát hiện pattern bất thường
 * mà user vẫn xóa được sạch tài khoản
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "deleted_user_activity_log_backup")
public class DeletedUserActivityLogBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin user đã xóa
    @Column(name = "deleted_user_id")
    private Long deletedUserId;

    @Column(name = "deleted_username", length = 100)
    private String deletedUsername;

    // Thông tin activity log gốc
    @Column(name = "original_log_id")
    private Long originalLogId;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    // Thời gian activity log gốc
    @Column(name = "activity_created_at")
    private java.time.Instant activityCreatedAt;

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
