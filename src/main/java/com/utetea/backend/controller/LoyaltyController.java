package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.LoyaltyService;
import com.utetea.backend.service.MemberTierService;
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
    private final MemberTierService memberTierService;
    
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
    
    /**
     * Lấy thông tin quyền lợi hạng thành viên
     */
    @GetMapping("/tier/benefits")
    public ResponseEntity<ApiResponse<MemberTierBenefitsDto>> getTierBenefits(Authentication authentication) {
        String username = authentication.getName();
        MemberTierBenefitsDto benefits = memberTierService.getUserTierBenefits(username);
        return ResponseEntity.ok(ApiResponse.success(benefits));
    }
    
    /**
     * Kiểm tra và nâng cấp tier (gọi sau khi hoàn thành đơn hàng)
     */
    @PostMapping("/tier/check-upgrade")
    public ResponseEntity<ApiResponse<MemberTierBenefitsDto>> checkTierUpgrade(Authentication authentication) {
        String username = authentication.getName();
        memberTierService.checkAndUpgradeTier(username);
        MemberTierBenefitsDto benefits = memberTierService.getUserTierBenefits(username);
        return ResponseEntity.ok(ApiResponse.success("Đã kiểm tra và cập nhật hạng thành viên", benefits));
    }
    
    /**
     * Tính toán tier discount cho một đơn hàng (preview trước khi đặt)
     */
    @GetMapping("/tier/preview-discount")
    public ResponseEntity<ApiResponse<TierDiscountPreview>> previewTierDiscount(
            Authentication authentication,
            @RequestParam Double orderTotal) {
        String username = authentication.getName();
        TierDiscountPreview preview = memberTierService.previewTierDiscount(username, orderTotal);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }
}
