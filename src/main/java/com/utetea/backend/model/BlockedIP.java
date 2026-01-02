package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 🚫 BLOCKED IP - Danh sách IP bị chặn
 * Admin có thể block IP khi phát hiện hành vi đáng ngờ
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "blocked_ips", indexes = {
    @Index(name = "idx_blocked_ip", columnList = "ip_address"),
    @Index(name = "idx_blocked_ip_active", columnList = "is_active")
})
public class BlockedIP extends AuditEntity {

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private BlockType blockType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private User blockedBy; // Admin/Manager đã block

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil; // null = vĩnh viễn

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unblocked_by")
    private User unblockedBy;

    @Column(name = "unblock_reason")
    private String unblockReason;

    // Liên kết với alert (nếu block từ alert)
    @Column(name = "alert_id")
    private Long alertId;

    // Liên kết với user (nếu biết user nào dùng IP này)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    private User relatedUser;

    // Số lần request bị chặn từ IP này
    @Column(name = "blocked_requests_count")
    private Long blockedRequestsCount = 0L;

    public enum BlockType {
        TEMPORARY,  // Tạm thời (có blockedUntil)
        PERMANENT,  // Vĩnh viễn
        AUTO        // Tự động block bởi hệ thống
    }

    /**
     * Kiểm tra IP có đang bị block không
     */
    public boolean isCurrentlyBlocked() {
        if (!isActive) return false;
        if (blockedUntil == null) return true; // Vĩnh viễn
        return LocalDateTime.now().isBefore(blockedUntil);
    }

    /**
     * Tăng số lần request bị chặn
     */
    public void incrementBlockedCount() {
        this.blockedRequestsCount = (this.blockedRequestsCount == null ? 0 : this.blockedRequestsCount) + 1;
    }
}
