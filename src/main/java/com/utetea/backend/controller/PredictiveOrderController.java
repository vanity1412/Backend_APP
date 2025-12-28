package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.PredictiveOrderDto;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.PredictiveOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho tính năng Predictive Order
 * Dự đoán món khách hàng muốn đặt dựa trên lịch sử và thói quen
 */
@RestController
@RequestMapping("/api/predictive-order")
@RequiredArgsConstructor
public class PredictiveOrderController {
    
    private final PredictiveOrderService predictiveOrderService;
    private final UserRepository userRepository;
    
    /**
     * Lấy gợi ý món dự đoán cho user hiện tại
     * @param weather Điều kiện thời tiết (optional): hot, cold, rainy, sunny
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PredictiveOrderDto>> getPrediction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String weather) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PredictiveOrderDto prediction = predictiveOrderService.getPrediction(user.getId(), weather);
        
        return ResponseEntity.ok(ApiResponse.success(prediction));
    }
    
    /**
     * Lấy gợi ý cho user cụ thể (dùng cho testing)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PredictiveOrderDto>> getPredictionForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String weather) {
        
        PredictiveOrderDto prediction = predictiveOrderService.getPrediction(userId, weather);
        
        return ResponseEntity.ok(ApiResponse.success(prediction));
    }
}
