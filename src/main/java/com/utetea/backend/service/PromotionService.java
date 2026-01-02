package com.utetea.backend.service;

import com.utetea.backend.dto.CreatePromotionRequest;
import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.dto.UpdatePromotionRequest;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.mapper.PromotionMapper;
import com.utetea.backend.model.DiscountType;
import com.utetea.backend.model.Promotion;
import com.utetea.backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final OneSignalService oneSignalService;
    
    // ========== USER APIs ==========
    
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
    
    // ========== MANAGER APIs ==========
    
    @Transactional(readOnly = true)
    public List<PromotionDto> getAllPromotions() {
        log.info("Getting all promotions for manager");
        return promotionRepository.findAll()
                .stream()
                .map(promotionMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PromotionDto createPromotion(CreatePromotionRequest request) {
        log.info("Creating new promotion with code: {}", request.getCode());
        
        // Validate code uniqueness
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Mã voucher đã tồn tại: " + request.getCode());
        }
        
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        // Validate discount value for PERCENT type
        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Giá trị giảm giá phần trăm không được vượt quá 100%");
            }
        }
        
        Promotion promotion = Promotion.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .isActive(request.getIsActive())
                .build();
        
        promotion = promotionRepository.save(promotion);
        log.info("Promotion created successfully with id: {}", promotion.getId());
        
        // Gửi thông báo voucher mới cho tất cả user
        try {
            if (promotion.getIsActive()) {
                String title = "🎉 Voucher mới dành cho bạn!";
                String content = promotion.getDescription() + " - Mã: " + promotion.getCode();
                
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("type", "NEW_VOUCHER");
                data.put("voucherCode", promotion.getCode());
                data.put("promotionId", promotion.getId());
                
                oneSignalService.sendToAll(title, content, data);
                log.info("Sent new voucher notification to all users");
            }
        } catch (Exception e) {
            log.error("Failed to send new voucher notification", e);
        }
        
        return promotionMapper.toDto(promotion);
    }
    
    @Transactional
    public PromotionDto updatePromotion(Long id, UpdatePromotionRequest request) {
        log.info("Updating promotion with id: {}", id);
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
        
        // Update fields if provided
        if (request.getDescription() != null) {
            promotion.setDescription(request.getDescription());
        }
        
        if (request.getDiscountType() != null) {
            promotion.setDiscountType(request.getDiscountType());
        }
        
        if (request.getDiscountValue() != null) {
            if (promotion.getDiscountType() == DiscountType.PERCENT && 
                request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Giá trị giảm giá phần trăm không được vượt quá 100%");
            }
            promotion.setDiscountValue(request.getDiscountValue());
        }
        
        if (request.getStartDate() != null) {
            promotion.setStartDate(request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            promotion.setEndDate(request.getEndDate());
        }
        
        // Validate dates after update
        if (promotion.getEndDate().isBefore(promotion.getStartDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        if (request.getMinOrderValue() != null) {
            promotion.setMinOrderValue(request.getMinOrderValue());
        }
        
        if (request.getMaxDiscountAmount() != null) {
            promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        
        if (request.getUsageLimit() != null) {
            promotion.setUsageLimit(request.getUsageLimit());
        }
        
        if (request.getIsActive() != null) {
            promotion.setIsActive(request.getIsActive());
        }
        
        promotion = promotionRepository.save(promotion);
        log.info("Promotion updated successfully with id: {}", promotion.getId());
        
        return promotionMapper.toDto(promotion);
    }
    
    @Transactional
    public void deletePromotion(Long id) {
        log.info("Deleting promotion with id: {}", id);
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
        
        // Check if promotion is being used
        Integer usedCount = promotion.getUsedCount();
        if (usedCount != null && usedCount > 0) {
            // Voucher đã được sử dụng -> vô hiệu hóa thay vì xóa
            log.info("Promotion {} has been used {} times, deactivating instead of deleting", id, usedCount);
            promotion.setIsActive(false);
            promotionRepository.save(promotion);
            log.info("Promotion deactivated successfully with id: {}", id);
        } else {
            // Voucher chưa được sử dụng -> xóa hoàn toàn
            promotionRepository.delete(promotion);
            log.info("Promotion deleted successfully with id: {}", id);
        }
    }
    
    @Transactional
    public PromotionDto togglePromotionStatus(Long id) {
        log.info("Toggling promotion status with id: {}", id);
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
        
        promotion.setIsActive(!promotion.getIsActive());
        promotion = promotionRepository.save(promotion);
        
        log.info("Promotion status toggled to: {}", promotion.getIsActive());
        return promotionMapper.toDto(promotion);
    }
}
