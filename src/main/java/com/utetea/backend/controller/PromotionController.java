package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionController {
    
    private final PromotionService promotionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getAllActivePromotions() {
        List<PromotionDto> promotions = promotionService.getAllActivePromotions();
        return ResponseEntity.ok(ApiResponse.success(promotions));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotionById(@PathVariable Long id) {
        PromotionDto promotion = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success(promotion));
    }
    
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionDto>> validatePromotion(
            @RequestParam String code,
            @RequestParam(required = false) java.math.BigDecimal orderAmount) {
        PromotionDto promotion;
        if (orderAmount != null) {
            promotion = promotionService.validatePromotionWithAmount(code, orderAmount);
        } else {
            promotion = promotionService.validatePromotionCode(code);
        }
        return ResponseEntity.ok(ApiResponse.success("Promotion is valid", promotion));
    }
}
