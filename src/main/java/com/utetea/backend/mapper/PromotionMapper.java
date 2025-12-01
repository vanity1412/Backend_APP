package com.utetea.backend.mapper;

import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.model.Promotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {
    
    public PromotionDto toDto(Promotion promotion) {
        if (promotion == null) return null;
        
        return PromotionDto.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .minOrderValue(promotion.getMinOrderValue())
                .isActive(promotion.getIsActive())
                .build();
    }
}
