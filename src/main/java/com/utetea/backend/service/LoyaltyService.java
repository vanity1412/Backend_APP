package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BadRequestException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyService {
    
    private final UserRepository userRepository;
    private final SpinRewardRepository spinRewardRepository;
    private final RateLimitService rateLimitService;
    
    @Value("${loyalty.points-to-spin:5}")
    private int pointsToSpin;
    
    @Value("${loyalty.voucher-length:10}")
    private int voucherLength;
    
    private static final List<Integer> WHEEL_ITEMS = Arrays.asList(0, 10, 20, 50, 100);
    private static final String VOUCHER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    /**
     * Lấy thông tin điểm của user
     */
    public UserPointsDto getUserPoints(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<SpinRewardDto> rewards = spinRewardRepository
                .findByUserIdAndIsUsedFalseAndDiscountPercentGreaterThan(user.getId(), 0)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return UserPointsDto.builder()
                .currentPoints(user.getPoints())
                .pointsToSpin(pointsToSpin)
                .canSpin(user.getPoints() >= pointsToSpin)
                .availableRewards(rewards)
                .build();
    }
    
    /**
     * Cộng điểm khi thanh toán thành công
     */
    @Transactional
    public Integer addPointForOrder(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setPoints(user.getPoints() + 1);
        userRepository.save(user);
        
        return user.getPoints();
    }
    
    /**
     * Quay vòng xoay may mắn
     * ✅ SECURITY: Check rate limit trước khi quay
     */
    @Transactional
    public SpinWheelResponse spinWheel(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // ✅ SECURITY: Check rate limit (10 lần/ngày per user)
        rateLimitService.checkSpinRateLimit(user.getId());
        
        if (user.getPoints() < pointsToSpin) {
            throw new BadRequestException("Không đủ điểm để quay. Cần " + pointsToSpin + " điểm.");
        }
        
        // Random vị trí trúng (0-4) với tỷ lệ khác nhau
        int winIndex = getRandomWinIndex();
        int discountPercent = WHEEL_ITEMS.get(winIndex);
        
        // Trừ điểm
        user.setPoints(user.getPoints() - pointsToSpin);
        userRepository.save(user);
        
        // Tạo mã voucher 10 ký tự
        String voucherCode = generateUniqueVoucherCode();
        
        // Lưu phần thưởng
        SpinReward reward = new SpinReward();
        reward.setUser(user);
        reward.setVoucherCode(voucherCode);
        reward.setDiscountPercent(discountPercent);
        reward.setPointsUsed(pointsToSpin);
        reward.setIsUsed(discountPercent == 0); // 0% đánh dấu đã dùng luôn
        reward = spinRewardRepository.save(reward);
        
        String message;
        if (discountPercent == 0) {
            message = "Chúc bạn may mắn lần sau!";
            voucherCode = null; // Không trả về mã nếu 0%
        } else if (discountPercent == 100) {
            message = "Chúc mừng! Bạn được MIỄN PHÍ 1 đơn hàng! Mã: " + voucherCode;
        } else {
            message = "Chúc mừng! Bạn nhận được voucher giảm " + discountPercent + "%! Mã: " + voucherCode;
        }
        
        return SpinWheelResponse.builder()
                .rewardId(reward.getId())
                .voucherCode(voucherCode)
                .discountPercent(discountPercent)
                .discountLabel(discountPercent + "%")
                .winIndex(winIndex)
                .wheelItems(WHEEL_ITEMS)
                .remainingPoints(user.getPoints())
                .message(message)
                .build();
    }
    
    /**
     * Tạo mã voucher unique
     */
    private String generateUniqueVoucherCode() {
        SecureRandom random = new SecureRandom();
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(voucherLength);
            for (int i = 0; i < voucherLength; i++) {
                sb.append(VOUCHER_CHARS.charAt(random.nextInt(VOUCHER_CHARS.length())));
            }
            code = sb.toString();
            attempts++;
        } while (spinRewardRepository.existsByVoucherCode(code) && attempts < 100);
        
        return code;
    }
    
    /**
     * Random với tỷ lệ:
     * 0% - 30%
     * 10% - 35%
     * 20% - 20%
     * 50% - 10%
     * 100% - 5%
     */
    private int getRandomWinIndex() {
        Random random = new Random();
        int rand = random.nextInt(100);
        
        if (rand < 30) return 0;       // 0%
        else if (rand < 65) return 1;  // 10%
        else if (rand < 85) return 2;  // 20%
        else if (rand < 95) return 3;  // 50%
        else return 4;                  // 100%
    }
    
    /**
     * Lấy danh sách voucher chưa sử dụng
     */
    public List<SpinRewardDto> getAvailableRewards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return spinRewardRepository
                .findByUserIdAndIsUsedFalseAndDiscountPercentGreaterThan(user.getId(), 0)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate và lấy thông tin voucher từ mã
     */
    public SpinRewardDto validateVoucherCode(String voucherCode) {
        SpinReward reward = spinRewardRepository.findByVoucherCodeAndIsUsedFalse(voucherCode.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ hoặc đã được sử dụng"));
        
        return toDto(reward);
    }
    
    /**
     * Đánh dấu voucher đã sử dụng
     */
    @Transactional
    public void markVoucherAsUsed(String voucherCode) {
        SpinReward reward = spinRewardRepository.findByVoucherCodeAndIsUsedFalse(voucherCode.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ hoặc đã được sử dụng"));
        
        reward.setIsUsed(true);
        spinRewardRepository.save(reward);
    }
    
    private SpinRewardDto toDto(SpinReward reward) {
        return SpinRewardDto.builder()
                .id(reward.getId())
                .voucherCode(reward.getVoucherCode())
                .discountPercent(reward.getDiscountPercent())
                .discountLabel(reward.getDiscountPercent() + "%")
                .isUsed(reward.getIsUsed())
                .createdAt(reward.getCreatedAt())
                .build();
    }
}
