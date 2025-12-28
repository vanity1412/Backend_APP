package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierDiscountPreview {
    private MemberTier tier;
    private String tierName;
    private BigDecimal discountPercent;
    private BigDecimal orderTotal;
    private BigDecimal tierDiscount;
    private BigDecimal finalTotal;
    private String message;
}
