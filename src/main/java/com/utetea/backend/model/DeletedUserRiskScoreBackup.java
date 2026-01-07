package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng backup lưu trữ thông tin risk scores của user đã bị xóa
 * Dùng để manager vẫn có thể phân tích mức độ rủi ro, pattern hành vi
 * mà user vẫn xóa được sạch tài khoản
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "deleted_user_risk_score_backup")
public class DeletedUserRiskScoreBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin user đã xóa
    @Column(name = "deleted_user_id")
    private Long deletedUserId;

    @Column(name = "deleted_username", length = 100)
    private String deletedUsername;

    // Thông tin risk score gốc
    @Column(name = "original_risk_score_id")
    private Long originalRiskScoreId;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    // Các chỉ số chi tiết
    @Column(name = "login_failed_count")
    private Integer loginFailedCount;

    @Column(name = "order_cancel_count")
    private Integer orderCancelCount;

    @Column(name = "payment_failed_count")
    private Integer paymentFailedCount;

    @Column(name = "rate_limit_hit_count")
    private Integer rateLimitHitCount;

    @Column(name = "promotion_abuse_count")
    private Integer promotionAbuseCount;

    @Column(name = "spam_request_count")
    private Integer spamRequestCount;

    @Column(name = "last_ip_address", length = 50)
    private String lastIpAddress;

    @Column(name = "last_score_reset")
    private LocalDateTime lastScoreReset;

    // Admin note
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "noted_by")
    private Long notedBy;

    @Column(name = "noted_at")
    private LocalDateTime notedAt;

    // Auto-block info
    @Column(name = "auto_blocked")
    private Boolean autoBlocked;

    @Column(name = "auto_blocked_at")
    private LocalDateTime autoBlockedAt;

    @Column(name = "auto_blocked_reason", length = 500)
    private String autoBlockedReason;

    // Thời gian risk score gốc
    @Column(name = "risk_score_created_at")
    private java.time.Instant riskScoreCreatedAt;

    @Column(name = "risk_score_updated_at")
    private java.time.Instant riskScoreUpdatedAt;

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
