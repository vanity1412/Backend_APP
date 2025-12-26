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
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }
    
    /**
     * Lấy danh sách voucher chưa sử dụng
     */
    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<SpinRewardDto>>> getAvailableRewards(Authentication authentication) {
        String username = authentication.getName();
        List<SpinRewardDto> rewards = loyaltyService.getAvailableRewards(username);
        return ResponseEntity.ok(ApiResponse.success(rewards));
    }
    
    /**
     * Validate mã voucher (kiểm tra trước khi áp dụng)
     */
    @GetMapping("/voucher/validate")
    public ResponseEntity<ApiResponse<SpinRewardDto>> validateVoucher(@RequestParam String code) {
        SpinRewardDto reward = loyaltyService.validateVoucherCode(code);
        return ResponseEntity.ok(ApiResponse.success("Mã voucher hợp lệ, giảm " + reward.getDiscountPercent() + "%", reward));
    }
}
