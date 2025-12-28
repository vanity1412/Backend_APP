package com.utetea.backend.config;

import com.utetea.backend.model.MemberTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình quyền lợi cho từng hạng thành viên
 */
@Component
public class MemberTierConfig {
    
    private final Map<MemberTier, TierBenefits> tierBenefitsMap;
    
    public MemberTierConfig() {
        tierBenefitsMap = new HashMap<>();
        
        // BRONZE: 0-49 điểm
        tierBenefitsMap.put(MemberTier.BRONZE, TierBenefits.builder()
                .tier(MemberTier.BRONZE)
                .tierName("Đồng")
                .minPoints(0)
                .maxPoints(49)
                .discountPercent(BigDecimal.ZERO)
                .pointsMultiplier(1.0)
                .freeShipping(false)
                .freeShippingMinOrder(BigDecimal.ZERO)
                .birthdayVoucher(false)
                .birthdayVoucherPercent(0)
                .prioritySupport(false)
                .exclusiveOffers(false)
                .earlyAccess(false)
                .description("Hạng khởi đầu - Tích điểm để nâng hạng!")
                .nextTierPoints(50)
                .build());
        
        // SILVER: 50-149 điểm
        tierBenefitsMap.put(MemberTier.SILVER, TierBenefits.builder()
                .tier(MemberTier.SILVER)
                .tierName("Bạc")
                .minPoints(50)
                .maxPoints(149)
                .discountPercent(new BigDecimal("5"))
                .pointsMultiplier(1.5)
                .freeShipping(false)
                .freeShippingMinOrder(new BigDecimal("100000"))
                .birthdayVoucher(true)
                .birthdayVoucherPercent(10)
                .prioritySupport(false)
                .exclusiveOffers(false)
                .earlyAccess(false)
                .description("Giảm 5% mọi đơn hàng + Voucher sinh nhật 10%")
                .nextTierPoints(150)
                .build());
        
        // GOLD: 150-299 điểm
        tierBenefitsMap.put(MemberTier.GOLD, TierBenefits.builder()
                .tier(MemberTier.GOLD)
                .tierName("Vàng")
                .minPoints(150)
                .maxPoints(299)
                .discountPercent(new BigDecimal("10"))
                .pointsMultiplier(2.0)
                .freeShipping(true)
                .freeShippingMinOrder(new BigDecimal("50000"))
                .birthdayVoucher(true)
                .birthdayVoucherPercent(20)
                .prioritySupport(true)
                .exclusiveOffers(true)
                .earlyAccess(false)
                .description("Giảm 10% + Freeship đơn từ 50K + Voucher sinh nhật 20%")
                .nextTierPoints(300)
                .build());
        
        // PLATINUM: 300+ điểm
        tierBenefitsMap.put(MemberTier.PLATINUM, TierBenefits.builder()
                .tier(MemberTier.PLATINUM)
                .tierName("Bạch Kim")
                .minPoints(300)
                .maxPoints(Integer.MAX_VALUE)
                .discountPercent(new BigDecimal("15"))
                .pointsMultiplier(3.0)
                .freeShipping(true)
                .freeShippingMinOrder(BigDecimal.ZERO)
                .birthdayVoucher(true)
                .birthdayVoucherPercent(30)
                .prioritySupport(true)
                .exclusiveOffers(true)
                .earlyAccess(true)
                .description("Giảm 15% + Freeship mọi đơn + Voucher sinh nhật 30% + Ưu tiên hỗ trợ")
                .nextTierPoints(null)
                .build());
    }
    
    public TierBenefits getBenefits(MemberTier tier) {
        return tierBenefitsMap.get(tier);
    }
    
    public MemberTier getTierByPoints(int points) {
        if (points >= 300) return MemberTier.PLATINUM;
        if (points >= 150) return MemberTier.GOLD;
        if (points >= 50) return MemberTier.SILVER;
        return MemberTier.BRONZE;
    }
    
    public Map<MemberTier, TierBenefits> getAllTierBenefits() {
        return tierBenefitsMap;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierBenefits {
        private MemberTier tier;
        private String tierName;
        private int minPoints;
        private int maxPoints;
        private BigDecimal discountPercent;      // % giảm giá mặc định
        private double pointsMultiplier;          // Hệ số nhân điểm
        private boolean freeShipping;             // Miễn phí ship
        private BigDecimal freeShippingMinOrder;  // Đơn tối thiểu để freeship
        private boolean birthdayVoucher;          // Voucher sinh nhật
        private int birthdayVoucherPercent;       // % voucher sinh nhật
        private boolean prioritySupport;          // Hỗ trợ ưu tiên
        private boolean exclusiveOffers;          // Ưu đãi độc quyền
        private boolean earlyAccess;              // Truy cập sớm sản phẩm mới
        private String description;               // Mô tả quyền lợi
        private Integer nextTierPoints;           // Điểm cần để lên tier tiếp
    }
}
