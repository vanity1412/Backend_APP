package com.utetea.backend.dto;

import lombok.*;
import java.time.Instant;

/**
 * DTO cho quản lý đánh giá (Admin/Manager)
 * Bao gồm cả review hiện tại và backup từ user đã xóa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewManagementDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userFullName;
    private String userAvatar;
    private Long drinkId;
    private String drinkName;
    private Long orderId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private Boolean isAnonymous;
    private Instant createdAt;
    
    // Thông tin bổ sung cho admin
    private Boolean isFromDeletedUser;  // true nếu là backup từ user đã xóa
    private Boolean isHidden;           // true nếu review bị ẩn
    
    // Thống kê tổng hợp
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewStatistics {
        private Long totalReviews;
        private Long activeReviews;      // Reviews từ user còn hoạt động
        private Long backupReviews;      // Reviews từ user đã xóa
        private Double averageRating;
        private Long fiveStarCount;
        private Long fourStarCount;
        private Long threeStarCount;
        private Long twoStarCount;
        private Long oneStarCount;
    }
}
