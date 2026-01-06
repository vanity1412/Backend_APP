package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 🛡️ USER RISK SCORE - Điểm rủi ro tổng hợp của user
 * Tính toán dựa trên các hoạt động trong UserActivityLog
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_risk_scores")
public class UserRiskScore extends AuditEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private UserActivityLog.RiskLevel riskLevel = UserActivityLog.RiskLevel.NORMAL;

    // Các chỉ số chi tiết
    @Column(name = "login_failed_count")
    private Integer loginFailedCount = 0;

    @Column(name = "order_cancel_count")
    private Integer orderCancelCount = 0;

    @Column(name = "payment_failed_count")
    private Integer paymentFailedCount = 0;

    @Column(name = "rate_limit_hit_count")
    private Integer rateLimitHitCount = 0;

    @Column(name = "promotion_abuse_count")
    private Integer promotionAbuseCount = 0;

    @Column(name = "spam_request_count")
    private Integer spamRequestCount = 0;
    
    // IP gần nhất của user (lấy từ activity log)
    @Column(name = "last_ip_address", length = 50)
    private String lastIpAddress;

    // Thời gian reset (mỗi 24h reset một phần điểm)
    @Column(name = "last_score_reset")
    private LocalDateTime lastScoreReset;

    // Ghi chú từ Admin
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "noted_by")
    private Long notedBy; // Admin ID who added the note

    @Column(name = "noted_at")
    private LocalDateTime notedAt;

    // Auto-block threshold
    @Column(name = "auto_blocked")
    private Boolean autoBlocked = false;

    @Column(name = "auto_blocked_at")
    private LocalDateTime autoBlockedAt;

    @Column(name = "auto_blocked_reason", length = 500)
    private String autoBlockedReason;

    /**
     * Tính toán risk level dựa trên total score
     */
    public void calculateRiskLevel() {
        if (totalScore >= 80) {
            this.riskLevel = UserActivityLog.RiskLevel.CRITICAL;
        } else if (totalScore >= 60) {
            this.riskLevel = UserActivityLog.RiskLevel.SUSPICIOUS;
        } else if (totalScore >= 30) {
            this.riskLevel = UserActivityLog.RiskLevel.WARNING;
        } else {
            this.riskLevel = UserActivityLog.RiskLevel.NORMAL;
        }
    }

    /**
     * Thêm điểm rủi ro
     */
    public void addScore(int points) {
        this.totalScore = Math.min(100, this.totalScore + points);
        calculateRiskLevel();
    }

    /**
     * Giảm điểm rủi ro (decay theo thời gian)
     */
    public void decayScore(int points) {
        this.totalScore = Math.max(0, this.totalScore - points);
        calculateRiskLevel();
    }
}
