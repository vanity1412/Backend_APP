package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BadRequestException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;
    
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
        
        // Validate order item belongs to order
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
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
        return toDto(review);
    }
    
    public List<ReviewDto> getReviewsByDrink(Long drinkId) {
        return reviewRepository.findByDrinkIdOrderByCreatedAtDesc(drinkId)
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
    
    public boolean canUserReviewOrderItem(String username, Long orderItemId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElse(null);
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
}
