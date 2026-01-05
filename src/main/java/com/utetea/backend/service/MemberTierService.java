package com.utetea.backend.service;

import com.utetea.backend.config.MemberTierConfig;
import com.utetea.backend.config.MemberTierConfig.TierBenefits;
import com.utetea.backend.dto.MemberTierBenefitsDto;
import com.utetea.backend.dto.MemberTierBenefitsDto.TierInfoDto;
import com.utetea.backend.dto.TierDiscountPreview;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.NotificationType;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberTierService {
    
    private final UserRepository userRepository;
    private final MemberTierConfig tierConfig;
    private final OneSignalService oneSignalService;
    
    /**
     * Lấy thông tin quyền lợi tier của user
     */
    public MemberTierBenefitsDto getUserTierBenefits(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return buildTierBenefitsDto(user);
    }
    
    /**
     * Kiểm tra và nâng cấp tier nếu đủ điểm
     */
    @Transactional
    public MemberTier checkAndUpgradeTier(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        MemberTier newTier = tierConfig.getTierByPoints(user.getPoints());
        
        if (newTier != user.getMemberTier()) {
            MemberTier oldTier = user.getMemberTier();
            user.setMemberTier(newTier);
            userRepository.save(user);
            log.info("User {} upgraded from {} to {}", username, oldTier, newTier);
            
            // 🎖️ Gửi thông báo khi lên cấp member tier
            sendTierUpgradeNotification(user, oldTier, newTier);
        }
        
        return user.getMemberTier();
    }
    
    /**
     * Kiểm tra và nâng cấp tier theo userId
     */
    @Transactional
    public MemberTier checkAndUpgradeTierByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        MemberTier newTier = tierConfig.getTierByPoints(user.getPoints());
        
        if (newTier != user.getMemberTier()) {
            MemberTier oldTier = user.getMemberTier();
            user.setMemberTier(newTier);
            userRepository.save(user);
            log.info("User {} upgraded from {} to {}", user.getUsername(), oldTier, newTier);
            
            // 🎖️ Gửi thông báo khi lên cấp member tier
            sendTierUpgradeNotification(user, oldTier, newTier);
        }
        
        return user.getMemberTier();
    }
    
    /**
     * Gửi thông báo khi user lên cấp member tier
     */
    private void sendTierUpgradeNotification(User user, MemberTier oldTier, MemberTier newTier) {
        try {
            TierBenefits newBenefits = tierConfig.getBenefits(newTier);
            String title = "🎖️ Chúc mừng lên hạng " + newBenefits.getTierName() + "!";
            String content = "Bạn đã lên hạng từ " + tierConfig.getBenefits(oldTier).getTierName() 
                + " lên " + newBenefits.getTierName() + ". Hãy khám phá các quyền lợi mới!";
            oneSignalService.sendToUser(String.valueOf(user.getId()), title, content, 
                NotificationType.TIER_UPGRADE, null);
        } catch (Exception e) {
            log.error("Failed to send tier upgrade notification for user {}", user.getId(), e);
        }
    }
    
    /**
     * Tính discount theo tier
     */
    public BigDecimal calculateTierDiscount(MemberTier tier, BigDecimal orderTotal) {
        TierBenefits benefits = tierConfig.getBenefits(tier);
        if (benefits.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            return orderTotal.multiply(benefits.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Kiểm tra có được freeship không
     */
    public boolean isEligibleForFreeShipping(MemberTier tier, BigDecimal orderTotal) {
        TierBenefits benefits = tierConfig.getBenefits(tier);
        if (!benefits.isFreeShipping()) {
            return false;
        }
        return orderTotal.compareTo(benefits.getFreeShippingMinOrder()) >= 0;
    }
    
    /**
     * Tính điểm với multiplier theo tier
     */
    public int calculatePointsEarned(MemberTier tier, int basePoints) {
        TierBenefits benefits = tierConfig.getBenefits(tier);
        return (int) Math.round(basePoints * benefits.getPointsMultiplier());
    }
    
    /**
     * Preview tier discount cho một đơn hàng
     */
    public TierDiscountPreview previewTierDiscount(String username, Double orderTotal) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        MemberTier tier = user.getMemberTier();
        TierBenefits benefits = tierConfig.getBenefits(tier);
        
        BigDecimal total = BigDecimal.valueOf(orderTotal);
        BigDecimal discountPercent = benefits.getDiscountPercent();
        BigDecimal tierDiscount = calculateTierDiscount(tier, total);
        BigDecimal finalTotal = total.subtract(tierDiscount);
        
        // Check free shipping eligibility
        boolean eligibleForFreeShipping = isEligibleForFreeShipping(tier, total);
        BigDecimal freeShippingMinOrder = benefits.getFreeShippingMinOrder();
        
        String message;
        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("Bạn được giảm %d%% (-%s VND) với hạng %s", 
                    discountPercent.intValue(), 
                    String.format("%,.0f", tierDiscount),
                    benefits.getTierName());
        } else {
            message = "Nâng hạng để được giảm giá!";
        }
        
        // Free shipping message
        String freeShippingMessage;
        if (eligibleForFreeShipping) {
            freeShippingMessage = "🎉 Bạn được miễn phí ship với hạng " + benefits.getTierName();
        } else if (benefits.isFreeShipping()) {
            BigDecimal amountNeeded = freeShippingMinOrder.subtract(total);
            if (amountNeeded.compareTo(BigDecimal.ZERO) > 0) {
                freeShippingMessage = String.format("Mua thêm %s VND để được miễn phí ship", 
                        String.format("%,.0f", amountNeeded));
            } else {
                freeShippingMessage = "Bạn được miễn phí ship!";
            }
        } else {
            freeShippingMessage = "Nâng hạng để được miễn phí ship!";
        }
        
        return TierDiscountPreview.builder()
                .tier(tier)
                .tierName(benefits.getTierName())
                .discountPercent(discountPercent)
                .orderTotal(total)
                .tierDiscount(tierDiscount)
                .finalTotal(finalTotal)
                .message(message)
                .eligibleForFreeShipping(eligibleForFreeShipping)
                .freeShippingMinOrder(freeShippingMinOrder)
                .freeShippingMessage(freeShippingMessage)
                .build();
    }
    
    /**
     * Lấy thông tin tất cả tier
     */
    public List<TierInfoDto> getAllTiersInfo(MemberTier currentTier, int currentPoints) {
        List<TierInfoDto> tiers = new ArrayList<>();
        
        for (MemberTier tier : MemberTier.values()) {
            TierBenefits benefits = tierConfig.getBenefits(tier);
            tiers.add(TierInfoDto.builder()
                    .tier(tier)
                    .tierName(benefits.getTierName())
                    .tierColor(getTierColor(tier))
                    .minPoints(benefits.getMinPoints())
                    .discountPercent(benefits.getDiscountPercent())
                    .isCurrentTier(tier == currentTier)
                    .isUnlocked(currentPoints >= benefits.getMinPoints())
                    .build());
        }
        
        return tiers;
    }
    
    private MemberTierBenefitsDto buildTierBenefitsDto(User user) {
        MemberTier currentTier = user.getMemberTier();
        int currentPoints = user.getPoints();
        TierBenefits benefits = tierConfig.getBenefits(currentTier);
        
        // Tính tier tiếp theo và điểm cần
        MemberTier nextTier = getNextTier(currentTier);
        int pointsToNextTier = 0;
        String nextTierName = null;
        double progressPercent = 100.0;
        
        if (nextTier != null) {
            TierBenefits nextBenefits = tierConfig.getBenefits(nextTier);
            pointsToNextTier = nextBenefits.getMinPoints() - currentPoints;
            nextTierName = nextBenefits.getTierName();
            
            // Tính progress trong tier hiện tại
            int tierRange = nextBenefits.getMinPoints() - benefits.getMinPoints();
            int pointsInTier = currentPoints - benefits.getMinPoints();
            progressPercent = (double) pointsInTier / tierRange * 100;
        }
        
        // Build danh sách quyền lợi dạng text
        List<String> benefitsList = buildBenefitsList(benefits);
        
        return MemberTierBenefitsDto.builder()
                .currentTier(currentTier)
                .tierName(benefits.getTierName())
                .tierColor(getTierColor(currentTier))
                .currentPoints(currentPoints)
                .pointsToNextTier(Math.max(0, pointsToNextTier))
                .nextTier(nextTier)
                .nextTierName(nextTierName)
                .progressPercent(Math.min(100, progressPercent))
                .discountPercent(benefits.getDiscountPercent())
                .pointsMultiplier(benefits.getPointsMultiplier())
                .freeShipping(benefits.isFreeShipping())
                .freeShippingMinOrder(benefits.getFreeShippingMinOrder())
                .birthdayVoucher(benefits.isBirthdayVoucher())
                .birthdayVoucherPercent(benefits.getBirthdayVoucherPercent())
                .prioritySupport(benefits.isPrioritySupport())
                .exclusiveOffers(benefits.isExclusiveOffers())
                .earlyAccess(benefits.isEarlyAccess())
                .description(benefits.getDescription())
                .benefitsList(benefitsList)
                .allTiers(getAllTiersInfo(currentTier, currentPoints))
                .build();
    }
    
    private List<String> buildBenefitsList(TierBenefits benefits) {
        List<String> list = new ArrayList<>();
        
        if (benefits.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            list.add("Giảm " + benefits.getDiscountPercent().intValue() + "% mọi đơn hàng");
        }
        
        if (benefits.getPointsMultiplier() > 1.0) {
            list.add("Nhân " + benefits.getPointsMultiplier() + "x điểm tích lũy");
        }
        
        if (benefits.isFreeShipping()) {
            if (benefits.getFreeShippingMinOrder().compareTo(BigDecimal.ZERO) > 0) {
                list.add("Miễn phí ship đơn từ " + formatMoney(benefits.getFreeShippingMinOrder()));
            } else {
                list.add("Miễn phí ship mọi đơn hàng");
            }
        }
        
        if (benefits.isBirthdayVoucher()) {
            list.add("Voucher sinh nhật giảm " + benefits.getBirthdayVoucherPercent() + "%");
        }
        
        if (benefits.isPrioritySupport()) {
            list.add("Hỗ trợ khách hàng ưu tiên");
        }
        
        if (benefits.isExclusiveOffers()) {
            list.add("Ưu đãi độc quyền dành riêng");
        }
        
        if (benefits.isEarlyAccess()) {
            list.add("Trải nghiệm sớm sản phẩm mới");
        }
        
        return list;
    }
    
    private MemberTier getNextTier(MemberTier current) {
        return switch (current) {
            case BRONZE -> MemberTier.SILVER;
            case SILVER -> MemberTier.GOLD;
            case GOLD -> MemberTier.PLATINUM;
            case PLATINUM -> null;
        };
    }
    
    private String getTierColor(MemberTier tier) {
        return switch (tier) {
            case BRONZE -> "#CD7F32";
            case SILVER -> "#C0C0C0";
            case GOLD -> "#FFD700";
            case PLATINUM -> "#E5E4E2";
        };
    }
    
    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f VND", amount);
    }
}
