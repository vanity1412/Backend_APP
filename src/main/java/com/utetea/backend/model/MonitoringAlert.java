package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 🚨 MONITORING ALERT - Cảnh báo gửi đến Admin/Manager
 * Khi phát hiện hành vi bất thường, tạo alert và push notification
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "monitoring_alerts", indexes = {
    @Index(name = "idx_alert_user_id", columnList = "user_id"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_created_at", columnList = "created_at")
})
public class MonitoringAlert extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User targetUser; // User bị cảnh báo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;

    // Thông tin xử lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy; // Admin/Manager xử lý

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 30)
    private ActionTaken actionTaken;

    // Liên kết với activity log
    @Column(name = "activity_log_id")
    private Long activityLogId;
    
    // IP address liên quan đến alert
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Push notification đã gửi chưa
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    /**
     * Loại cảnh báo
     */
    public enum AlertType {
        LOGIN_ANOMALY,          // Đăng nhập bất thường
        ORDER_ABUSE,            // Lạm dụng đặt hàng
        PAYMENT_FRAUD,          // Gian lận thanh toán
        PROMOTION_ABUSE,        // Lạm dụng khuyến mãi
        RATE_LIMIT_EXCEEDED,    // Vượt giới hạn request
        SPAM_DETECTED,          // Phát hiện spam
        BRUTE_FORCE,            // Tấn công brute force
        HIGH_RISK_SCORE,        // Điểm rủi ro cao
        AUTO_BLOCKED,           // Tự động block
        SECURITY_VIOLATION      // Vi phạm bảo mật
    }

    /**
     * Mức độ nghiêm trọng
     */
    public enum AlertSeverity {
        LOW,        // 🟢 Thấp - chỉ cần theo dõi
        MEDIUM,     // 🟡 Trung bình - cần xem xét
        HIGH,       // 🟠 Cao - cần xử lý sớm
        CRITICAL    // 🔴 Nghiêm trọng - cần xử lý ngay
    }

    /**
     * Trạng thái xử lý
     */
    public enum AlertStatus {
        PENDING,    // Chờ xử lý
        REVIEWING,  // Đang xem xét
        RESOLVED,   // Đã xử lý
        DISMISSED   // Bỏ qua
    }

    /**
     * Hành động đã thực hiện
     */
    public enum ActionTaken {
        NONE,           // Không làm gì
        WARNING_SENT,   // Đã gửi cảnh báo cho user
        TEMP_BLOCKED,   // Tạm khóa
        PERM_BLOCKED,   // Khóa vĩnh viễn
        MONITORED,      // Đưa vào danh sách theo dõi
        FALSE_POSITIVE  // Cảnh báo sai
    }
}
