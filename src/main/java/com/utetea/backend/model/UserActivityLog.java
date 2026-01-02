package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 🛡️ USER ACTIVITY LOG - Defensive Monitoring System
 * Ghi lại tất cả hoạt động của user để phát hiện hành vi bất thường
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_activity_logs", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_risk_level", columnList = "risk_level"),
    @Index(name = "idx_activity_created_at", columnList = "created_at")
})
public class UserActivityLog extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.NORMAL;

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
    private Long relatedId; // Order ID, Promotion ID, etc.

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData; // JSON data for additional info

    /**
     * Các loại hoạt động cần giám sát
     */
    public enum ActivityType {
        // Authentication
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        PASSWORD_CHANGE,
        PASSWORD_RESET_REQUEST,
        
        // Orders
        ORDER_CREATE,
        ORDER_VIEW,
        ORDER_CANCEL,
        ORDER_CANCEL_MULTIPLE,
        ORDER_STATUS_UPDATE,
        
        // Payments
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_FAILED_MULTIPLE,
        
        // Promotions
        PROMOTION_USE,
        PROMOTION_ABUSE_ATTEMPT,
        
        // Cart
        CART_ADD_ITEM,
        CART_REMOVE_ITEM,
        CART_UPDATE_QUANTITY,
        CART_CLEAR,
        
        // Products
        PRODUCT_VIEW,
        PRODUCT_SEARCH,
        
        // Profile
        PROFILE_VIEW,
        PROFILE_UPDATE,
        AVATAR_UPDATE,
        
        // Rate Limiting
        RATE_LIMIT_HIT,
        SPAM_REQUEST,
        
        // Account
        ACCOUNT_BLOCKED,
        ACCOUNT_UNBLOCKED,
        
        // Suspicious
        BRUTE_FORCE_ATTEMPT,
        UNUSUAL_LOCATION,
        DEVICE_CHANGE,
        
        // Group Order
        GROUP_ORDER_CREATE,
        GROUP_ORDER_JOIN,
        GROUP_ORDER_LEAVE,
        GROUP_ORDER_CHAT,
        
        // Chat
        LIVE_CHAT_START,
        LIVE_CHAT_MESSAGE,
        
        // System
        API_ERROR,
        SECURITY_VIOLATION
    }

    /**
     * Mức độ rủi ro của hành vi
     */
    public enum RiskLevel {
        NORMAL,     // 🟢 Bình thường (0-30 điểm)
        WARNING,    // 🟡 Đáng chú ý (31-60 điểm)
        SUSPICIOUS, // 🔴 Nguy hiểm (61-100 điểm)
        CRITICAL    // ⚫ Nghiêm trọng (cần block ngay)
    }
}
