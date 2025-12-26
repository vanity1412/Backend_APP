package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    /**
     * Tạo đánh giá mới cho sản phẩm đã mua
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto>> createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request) {
        String username = authentication.getName();
        ReviewDto review = reviewService.createReview(username, request);
        return ResponseEntity.ok(ApiResponse.success("Review created successfully", review));
    }
    
    /**
     * Lấy danh sách đánh giá của một sản phẩm
     */
    @GetMapping("/drink/{drinkId}")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getReviewsByDrink(
            @PathVariable Long drinkId) {
        List<ReviewDto> reviews = reviewService.getReviewsByDrink(drinkId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
    
    /**
     * Lấy danh sách đánh giá của một sản phẩm (có phân trang)
     */
    @GetMapping("/drink/{drinkId}/paged")
    public ResponseEntity<ApiResponse<Page<ReviewDto>>> getReviewsByDrinkPaged(
            @PathVariable Long drinkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewDto> reviews = reviewService.getReviewsByDrinkPaged(
                drinkId, 
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
    
    /**
     * Lấy thống kê đánh giá của một sản phẩm
     */
    @GetMapping("/drink/{drinkId}/summary")
    public ResponseEntity<ApiResponse<DrinkRatingSummary>> getDrinkRatingSummary(
            @PathVariable Long drinkId) {
        DrinkRatingSummary summary = reviewService.getDrinkRatingSummary(drinkId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * Lấy danh sách đánh giá của user hiện tại
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getMyReviews(
            Authentication authentication) {
        String username = authentication.getName();
        List<ReviewDto> reviews = reviewService.getReviewsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
    
    /**
     * Kiểm tra user có thể đánh giá order item không
     */
    @GetMapping("/can-review/{orderItemId}")
    public ResponseEntity<ApiResponse<Boolean>> canReviewOrderItem(
            Authentication authentication,
            @PathVariable Long orderItemId) {
        String username = authentication.getName();
        boolean canReview = reviewService.canUserReviewOrderItem(username, orderItemId);
        return ResponseEntity.ok(ApiResponse.success(canReview));
    }
    
    /**
     * Xóa đánh giá của mình
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<String>> deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId) {
        String username = authentication.getName();
        reviewService.deleteReview(username, reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }
}
