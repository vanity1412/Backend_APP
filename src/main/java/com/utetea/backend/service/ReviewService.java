package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BadRequestException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;
    private final DeletedUserReviewBackupRepository deletedUserReviewBackupRepository;
    private final UserMonitoringService userMonitoringService;
    
    @Transactional
    public ReviewDto createReview(String username, CreateReviewRequest request) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate order belongs to user and is DONE
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to this user");
        }
        
        if (order.getStatus() != OrderStatus.DONE) {
            throw new BadRequestException("Can only review completed orders");
        }
        
        // FIX: Sử dụng query với JOIN FETCH để load Order và Drink cùng lúc
        OrderItem orderItem = orderItemRepository.findByIdWithOrder(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
        
        if (!orderItem.getOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Order item does not belong to this order");
        }
        
        // Check if already reviewed
        if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), request.getOrderItemId())) {
            throw new BadRequestException("You have already reviewed this item");
        }
        
        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setDrink(orderItem.getDrink());
        review.setOrder(order);
        review.setOrderItem(orderItem);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
        
        review = reviewRepository.save(review);
        
        // 🎁 Cộng 1 điểm vòng quay khi đánh giá thành công
        try {
            int updated = userRepository.addPoints(user.getId(), 1);
            if (updated > 0) {
                log.info("Added 1 spin point for user {} after review", user.getId());
            }
        } catch (Exception e) {
            log.error("Failed to add spin point for review", e);
        }
        
        // 🛡️ Log activity - Đánh giá sản phẩm
        try {
            userMonitoringService.logActivity(user.getId(), 
                com.utetea.backend.model.UserActivityLog.ActivityType.PRODUCT_VIEW,
                "Đánh giá " + review.getRating() + " sao cho " + orderItem.getDrink().getName(), null);
        } catch (Exception e) {
            log.error("Failed to log review to monitoring", e);
        }
        
        return toDto(review);
    }
    
    public List<ReviewDto> getReviewsByDrink(Long drinkId) {
        // FIX N+1 Query: Sử dụng JOIN FETCH để load User và Drink trong 1 query
        return reviewRepository.findByDrinkIdWithUserAndDrink(drinkId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<ReviewDto> getReviewsByDrinkPaged(Long drinkId, Pageable pageable) {
        return reviewRepository.findByDrinkId(drinkId, pageable)
                .map(this::toDto);
    }
    
    public List<ReviewDto> getReviewsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reviewRepository.findByUserId(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public DrinkRatingSummary getDrinkRatingSummary(Long drinkId) {
        Double avgRating = reviewRepository.getAverageRatingByDrinkId(drinkId);
        Long totalReviews = reviewRepository.countByDrinkId(drinkId);
        
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        
        List<Object[]> rawDistribution = reviewRepository.getRatingDistributionByDrinkId(drinkId);
        for (Object[] row : rawDistribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(rating, count);
        }
        
        return DrinkRatingSummary.builder()
                .drinkId(drinkId)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews : 0L)
                .ratingDistribution(distribution)
                .build();
    }
    
    @Transactional(readOnly = true)
    public boolean canUserReviewOrderItem(String username, Long orderItemId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        
        // FIX: Sử dụng query với JOIN FETCH để load Order cùng lúc
        OrderItem orderItem = orderItemRepository.findByIdWithOrder(orderItemId).orElse(null);
        if (orderItem == null) return false;
        
        Order order = orderItem.getOrder();
        if (!order.getUser().getId().equals(user.getId())) return false;
        if (order.getStatus() != OrderStatus.DONE) return false;
        
        return !reviewRepository.existsByUserIdAndOrderItemId(user.getId(), orderItemId);
    }
    
    @Transactional
    public void deleteReview(String username, Long reviewId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own reviews");
        }
        
        reviewRepository.delete(review);
    }
    
    private ReviewDto toDto(Review review) {
        String userName = review.getIsAnonymous() ? "Ẩn danh" : review.getUser().getFullName();
        String userAvatar = review.getIsAnonymous() ? null : review.getUser().getAvatarUrl();
        
        return ReviewDto.builder()
                .id(review.getId())
                .userId(review.getIsAnonymous() ? null : review.getUser().getId())
                .userName(userName)
                .userAvatar(userAvatar)
                .drinkId(review.getDrink().getId())
                .drinkName(review.getDrink().getName())
                .orderId(review.getOrder().getId())
                .orderItemId(review.getOrderItem().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isAnonymous(review.getIsAnonymous())
                .createdAt(review.getCreatedAt())
                .build();
    }
    
    // ==================== ADMIN/MANAGER REVIEW MANAGEMENT ====================
    
    /**
     * Lấy tất cả đánh giá (bao gồm cả backup từ user đã xóa) - CHO ADMIN/MANAGER
     */
    @Transactional(readOnly = true)
    public Page<ReviewManagementDto> getAllReviewsForAdmin(Pageable pageable) {
        log.info("Getting all reviews for admin management");
        
        // Lấy reviews hiện tại
        Page<Review> activeReviews = reviewRepository.findAll(pageable);
        
        List<ReviewManagementDto> result = activeReviews.getContent().stream()
                .map(this::toManagementDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(result, pageable, activeReviews.getTotalElements());
    }
    
    /**
     * Lấy đánh giá theo sản phẩm (bao gồm backup) - CHO ADMIN/MANAGER
     */
    @Transactional(readOnly = true)
    public Page<ReviewManagementDto> getReviewsByDrinkForAdmin(Long drinkId, boolean includeBackup, Pageable pageable) {
        log.info("Getting reviews for drink {} (includeBackup: {})", drinkId, includeBackup);
        
        List<ReviewManagementDto> allReviews = new ArrayList<>();
        
        // Lấy reviews hiện tại
        Page<Review> activeReviews = reviewRepository.findByDrinkId(drinkId, pageable);
        allReviews.addAll(activeReviews.getContent().stream()
                .map(this::toManagementDto)
                .collect(Collectors.toList()));
        
        // Thêm backup reviews nếu cần
        if (includeBackup) {
            List<DeletedUserReviewBackup> backupReviews = 
                deletedUserReviewBackupRepository.findByDrinkIdOrderByReviewCreatedAtDesc(drinkId);
            allReviews.addAll(backupReviews.stream()
                    .map(this::backupToManagementDto)
                    .collect(Collectors.toList()));
        }
        
        // Sort by createdAt desc
        allReviews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        long totalElements = activeReviews.getTotalElements();
        if (includeBackup) {
            totalElements += deletedUserReviewBackupRepository.countByDrinkId(drinkId);
        }
        
        return new PageImpl<>(allReviews, pageable, totalElements);
    }
    
    /**
     * Lấy thống kê đánh giá tổng hợp (bao gồm backup) - CHO ADMIN/MANAGER
     */
    @Transactional(readOnly = true)
    public ReviewManagementDto.ReviewStatistics getReviewStatisticsForAdmin(Long drinkId) {
        log.info("Getting review statistics for drink {}", drinkId);
        
        // Thống kê từ reviews hiện tại
        Double activeAvgRating = reviewRepository.getAverageRatingByDrinkId(drinkId);
        Long activeCount = reviewRepository.countByDrinkId(drinkId);
        List<Object[]> activeDistribution = reviewRepository.getRatingDistributionByDrinkId(drinkId);
        
        // Thống kê từ backup
        Double backupAvgRating = deletedUserReviewBackupRepository.getAverageRatingByDrinkId(drinkId);
        Long backupCount = deletedUserReviewBackupRepository.countByDrinkId(drinkId);
        List<Object[]> backupDistribution = deletedUserReviewBackupRepository.getRatingDistributionByDrinkId(drinkId);
        
        // Tính tổng hợp
        long totalReviews = (activeCount != null ? activeCount : 0) + (backupCount != null ? backupCount : 0);
        
        // Tính average rating tổng hợp
        double totalAvgRating = 0.0;
        if (totalReviews > 0) {
            double activeSum = (activeAvgRating != null ? activeAvgRating : 0) * (activeCount != null ? activeCount : 0);
            double backupSum = (backupAvgRating != null ? backupAvgRating : 0) * (backupCount != null ? backupCount : 0);
            totalAvgRating = (activeSum + backupSum) / totalReviews;
        }
        
        // Tính distribution tổng hợp
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        
        for (Object[] row : activeDistribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.merge(rating, count, Long::sum);
        }
        
        for (Object[] row : backupDistribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.merge(rating, count, Long::sum);
        }
        
        return ReviewManagementDto.ReviewStatistics.builder()
                .totalReviews(totalReviews)
                .activeReviews(activeCount != null ? activeCount : 0)
                .backupReviews(backupCount != null ? backupCount : 0)
                .averageRating(Math.round(totalAvgRating * 10.0) / 10.0)
                .fiveStarCount(distribution.get(5))
                .fourStarCount(distribution.get(4))
                .threeStarCount(distribution.get(3))
                .twoStarCount(distribution.get(2))
                .oneStarCount(distribution.get(1))
                .build();
    }
    
    /**
     * Xóa đánh giá bởi Admin/Manager
     */
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        log.info("Admin deleting review {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        reviewRepository.delete(review);
        log.info("Review {} deleted by admin", reviewId);
    }
    
    /**
     * Lấy tất cả backup reviews - CHO ADMIN
     */
    @Transactional(readOnly = true)
    public Page<ReviewManagementDto> getBackupReviews(Pageable pageable) {
        log.info("Getting backup reviews for admin");
        
        Page<DeletedUserReviewBackup> backups = deletedUserReviewBackupRepository.findAll(pageable);
        
        List<ReviewManagementDto> result = backups.getContent().stream()
                .map(this::backupToManagementDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(result, pageable, backups.getTotalElements());
    }
    
    /**
     * Backup reviews của user trước khi xóa tài khoản
     */
    @Transactional
    public void backupUserReviews(User user) {
        log.info("Backing up reviews for user {}", user.getId());
        
        List<Review> reviews = reviewRepository.findByUserId(user.getId());
        int backupCount = 0;
        
        for (Review review : reviews) {
            DeletedUserReviewBackup backup = DeletedUserReviewBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .deletedUserFullname(user.getFullName())
                    .originalReviewId(review.getId())
                    .drinkId(review.getDrink().getId())
                    .drinkName(review.getDrink().getName())
                    .orderId(review.getOrder().getId())
                    .orderItemId(review.getOrderItem().getId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .isAnonymous(review.getIsAnonymous())
                    .reviewCreatedAt(review.getCreatedAt())
                    .build();
            
            deletedUserReviewBackupRepository.save(backup);
            backupCount++;
        }
        
        log.info("Backed up {} reviews for user {}", backupCount, user.getId());
    }
    
    private ReviewManagementDto toManagementDto(Review review) {
        return ReviewManagementDto.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getUsername())
                .userFullName(review.getIsAnonymous() ? "Ẩn danh" : review.getUser().getFullName())
                .userAvatar(review.getIsAnonymous() ? null : review.getUser().getAvatarUrl())
                .drinkId(review.getDrink().getId())
                .drinkName(review.getDrink().getName())
                .orderId(review.getOrder().getId())
                .orderItemId(review.getOrderItem().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isAnonymous(review.getIsAnonymous())
                .createdAt(review.getCreatedAt())
                .isFromDeletedUser(false)
                .isHidden(false)
                .build();
    }
    
    private ReviewManagementDto backupToManagementDto(DeletedUserReviewBackup backup) {
        return ReviewManagementDto.builder()
                .id(backup.getId())
                .userId(backup.getDeletedUserId())
                .userName(backup.getDeletedUsername() + " (đã xóa)")
                .userFullName(backup.getIsAnonymous() ? "Ẩn danh" : backup.getDeletedUserFullname())
                .userAvatar(null)
                .drinkId(backup.getDrinkId())
                .drinkName(backup.getDrinkName())
                .orderId(backup.getOrderId())
                .orderItemId(backup.getOrderItemId())
                .rating(backup.getRating())
                .comment(backup.getComment())
                .isAnonymous(backup.getIsAnonymous())
                .createdAt(backup.getReviewCreatedAt())
                .isFromDeletedUser(true)
                .isHidden(false)
                .build();
    }
}
