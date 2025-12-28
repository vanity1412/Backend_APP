package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.CreatePromotionRequest;
import com.utetea.backend.dto.PromotionDto;
import com.utetea.backend.dto.UpdatePromotionRequest;
import com.utetea.backend.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.utetea.backend.service.OneSignalService;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionController {
    
    private final PromotionService promotionService;
    private final OneSignalService oneSignalService;
    
    // ========== USER APIs ==========
    
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
        return ResponseEntity.ok(ApiResponse.success("Mã voucher hợp lệ", promotion));
    }
    
    // ========== MANAGER APIs ==========
    
    @GetMapping("/manager/all")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getAllPromotionsForManager() {
        List<PromotionDto> promotions = promotionService.getAllPromotions();
        return ResponseEntity.ok(ApiResponse.success(promotions));
    }

    @PostMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request,
            @RequestParam(defaultValue = "false") boolean sendNotification // Thêm tham số này
    ) {
        PromotionDto promotion = promotionService.createPromotion(request);

        // LOGIC GỬI THÔNG BÁO
        if (sendNotification) {
            String title = "🎁 Ưu đãi mới: " + promotion.getDescription();
            String content = "Nhập mã " + promotion.getCode() + " để nhận ưu đãi ngay! HSD: " + promotion.getEndDate();
            oneSignalService.sendToAll(title, content);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo voucher thành công", promotion));
    }
    
    @PutMapping("/manager/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionRequest request) {
        PromotionDto promotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", promotion));
    }
    
    @DeleteMapping("/manager/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa voucher thành công", null));
    }
    
    @PatchMapping("/manager/{id}/toggle-status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PromotionDto>> togglePromotionStatus(@PathVariable Long id) {
        PromotionDto promotion = promotionService.togglePromotionStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Thay đổi trạng thái voucher thành công", promotion));
    }
}
