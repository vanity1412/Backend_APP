package com.utetea.backend.service;

import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.mapper.PromotionMapper;
import com.utetea.backend.model.Promotion;
import com.utetea.backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    
    @Transactional(readOnly = true)
    public List<PromotionDto> getAllActivePromotions() {
        return promotionRepository.findByIsActiveTrueAndEndDateAfter(LocalDateTime.now())
                .stream()
                .map(promotionMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PromotionDto getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
        return promotionMapper.toDto(promotion);
    }
    
    @Transactional(readOnly = true)
    public PromotionDto validatePromotionCode(String code) {
        log.info("Validating promotion code: {}", code);
        
        Promotion promotion = promotionRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Invalid promotion code: " + code));
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate())) {
            throw new BusinessException("Promotion has not started yet");
        }
        
        if (now.isAfter(promotion.getEndDate())) {
            throw new BusinessException("Promotion has expired");
        }
        
        log.info("Promotion code {} is valid", code);
        return promotionMapper.toDto(promotion);
    }
    
    @Transactional(readOnly = true)
    public PromotionDto validatePromotionWithAmount(String code, java.math.BigDecimal orderAmount) {
        log.info("Validating promotion code: {} with order amount: {}", code, orderAmount);
        
        Promotion promotion = promotionRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Invalid promotion code: " + code));
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate())) {
            throw new BusinessException("Promotion has not started yet");
        }
        
        if (now.isAfter(promotion.getEndDate())) {
            throw new BusinessException("Promotion has expired");
        }
        
        if (orderAmount.compareTo(promotion.getMinOrderValue()) < 0) {
            throw new BusinessException(String.format(
                    "Minimum order amount is %s VND for this promotion", 
                    promotion.getMinOrderValue()));
        }
        
        log.info("Promotion code {} is valid for order amount {}", code, orderAmount);
        return promotionMapper.toDto(promotion);
    }
}
