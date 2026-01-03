package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 🚫 Entity lưu trữ thông tin IP bị chặn
 */
@Entity
@Table(name = "blocked_ips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedIP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private BlockType blockType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "blocked_by_id")
    private Long blockedById;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "unblocked_at")
    private Instant unblockedAt;

    @Column(name = "unblocked_by_id")
    private Long unblockedById;

    @Column(name = "unblock_reason")
    private String unblockReason;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "related_user_id")
    private Long relatedUserId;

    @Column(name = "blocked_requests_count")
    private Long blockedRequestsCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum BlockType {
        TEMPORARY,
        PERMANENT,
        AUTO
    }

    /**
     * Kiểm tra IP có đang bị chặn không
     */
    public boolean isCurrentlyBlocked() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        if (blockType == BlockType.PERMANENT) {
            return true;
        }
        if (blockedUntil != null && blockedUntil.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Tăng số lượng request bị chặn
     */
    public void incrementBlockedCount() {
        if (blockedRequestsCount == null) {
            blockedRequestsCount = 0L;
        }
        blockedRequestsCount++;
    }
}
