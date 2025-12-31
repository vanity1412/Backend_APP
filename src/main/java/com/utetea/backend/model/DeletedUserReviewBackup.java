package com.utetea.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Bảng backup lưu trữ đánh giá của user đã bị xóa
 * Dùng để manager vẫn có thể xem lịch sử đánh giá sản phẩm
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "deleted_user_review_backup")
public class DeletedUserReviewBackup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Thông tin user đã xóa
    @Column(name = "deleted_user_id")
    private Long deletedUserId;
    
    @Column(name = "deleted_username", length = 100)
    private String deletedUsername;
    
    @Column(name = "deleted_user_fullname", length = 255)
    private String deletedUserFullname;
    
    // Thông tin review gốc
    @Column(name = "original_review_id")
    private Long originalReviewId;
    
    @Column(name = "drink_id")
    private Long drinkId;
    
    @Column(name = "drink_name", length = 255)
    private String drinkName;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "order_item_id")
    private Long orderItemId;
    
    @Column(nullable = false)
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
    
    // Thời gian review gốc
    @Column(name = "review_created_at")
    private Instant reviewCreatedAt;
    
    // Thời gian backup
    @Column(name = "backup_created_at")
    private Instant backupCreatedAt;
    
    @PrePersist
    protected void onCreate() {
        backupCreatedAt = Instant.now();
    }
}
