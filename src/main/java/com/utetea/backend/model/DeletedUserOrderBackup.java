package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng backup lưu trữ thông tin đơn hàng của user đã bị xóa
 * Dùng để manager vẫn có thể quản lý doanh thu, revenue, cảnh báo
 * mà user vẫn xóa được sạch tài khoản
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "deleted_user_order_backup")
public class DeletedUserOrderBackup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Thông tin user đã xóa (lưu để tham khảo)
    @Column(name = "deleted_user_id")
    private Long deletedUserId;
    
    @Column(name = "deleted_username", length = 100)
    private String deletedUsername;
    
    @Column(name = "deleted_user_phone", length = 255)
    private String deletedUserPhone;
    
    // Thông tin đơn hàng gốc
    @Column(name = "original_order_id")
    private Long originalOrderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20)
    private OrderStatus orderStatus;
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal discount;
    
    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;
    
    // Thời gian đơn hàng gốc
    @Column(name = "order_created_at")
    private java.time.Instant orderCreatedAt;
    
    // Thời gian backup
    @Column(name = "backup_created_at")
    private java.time.Instant backupCreatedAt;
    
    // Chi tiết đơn hàng (lưu dạng JSON để giữ nguyên thông tin)
    @Column(name = "order_items_json", columnDefinition = "TEXT")
    private String orderItemsJson;
    
    // Ghi chú
    @Column(length = 500)
    private String note;
    
    @PrePersist
    protected void onCreate() {
        backupCreatedAt = java.time.Instant.now();
    }
}
