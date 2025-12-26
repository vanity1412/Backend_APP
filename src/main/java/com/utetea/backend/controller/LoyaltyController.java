package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {
    
    private final LoyaltyService loyaltyService;
    
    /**
     * Lấy thông tin điểm của user
     */
    @GetMapping("/points")
    public ResponseEntity<ApiResponse<UserPointsDto>> getUserPoints(Authentication authentication) {
        String username = authentication.getName();
        UserPointsDto points = loyaltyService.getUserPoints(username);
        return ResponseEntity.ok(ApiResponse.success(points));
    }
    
    /**
     * Quay vòng xoay may mắn (cần 5 điểm)
     */
    @PostMapping("/spin")
    public ResponseEntity<ApiResponse<SpinWheelResponse>> spinWheel(Authentication authentication) {
        String username = authentication.getName();
        SpinWheelResponse result = loyaltyService.spinWheel(username);
        return ResponseEntity.ok(ApiResponse.success("Chúc mừng bạn đã trúng thưởng!", result));
    }
    
    /**
     * Lấy danh sách phần thưởng chưa sử dụng
     */
    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<SpinRewardDto>>> getAvailableRewards(Authentication authentication) {
        String username = authentication.getName();
        List<SpinRewardDto> rewards = loyaltyService.getAvailableRewards(username);
        return ResponseEntity.ok(ApiResponse.success(rewards));
    }
    
    /**
     * Kiểm tra có phần thưởng cho drink không
     */
    @GetMapping("/rewards/drink/{drinkId}")
    public ResponseEntity<ApiResponse<SpinRewardDto>> getRewardForDrink(
            Authentication authentication,
            @PathVariable Long drinkId) {
        String username = authentication.getName();
        SpinRewardDto reward = loyaltyService.getRewardForDrink(username, drinkId);
        return ResponseEntity.ok(ApiResponse.success(reward));
    }
    
    /**
     * Sử dụng phần thưởng
     */
    @PostMapping("/rewards/{rewardId}/redeem")
    public ResponseEntity<ApiResponse<String>> redeemReward(
            Authentication authentication,
            @PathVariable Long rewardId,
            @RequestParam(required = false) Long orderId) {
        String username = authentication.getName();
        loyaltyService.redeemReward(username, rewardId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Đã sử dụng phần thưởng thành công", null));
    }
}
