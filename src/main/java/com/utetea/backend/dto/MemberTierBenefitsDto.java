package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTierBenefitsDto {
    private MemberTier currentTier;
    private String tierName;
    private String tierColor;
    private int currentPoints;
    private int pointsToNextTier;
    private MemberTier nextTier;
    private String nextTierName;
    private double progressPercent;
    
    // Quyền lợi hiện tại
    private BigDecimal discountPercent;
    private double pointsMultiplier;
    private boolean freeShipping;
    private BigDecimal freeShippingMinOrder;
    private boolean birthdayVoucher;
    private int birthdayVoucherPercent;
    private boolean prioritySupport;
    private boolean exclusiveOffers;
    private boolean earlyAccess;
    private String description;
    
    // Danh sách quyền lợi dạng text
    private List<String> benefitsList;
    
    // Tất cả tier để hiển thị progress
    private List<TierInfoDto> allTiers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierInfoDto {
        private MemberTier tier;
        private String tierName;
        private String tierColor;
        private int minPoints;
        private BigDecimal discountPercent;
        private boolean isCurrentTier;
        private boolean isUnlocked;
    }
}
