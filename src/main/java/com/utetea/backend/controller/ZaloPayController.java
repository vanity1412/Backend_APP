package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.ZaloPayPaymentRequest;
import com.utetea.backend.service.ZaloPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zalopay")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ZaloPayController {
    
    private final ZaloPayService zaloPayService;
    
    @PostMapping("/create-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> createPayment(@RequestBody ZaloPayPaymentRequest request) {
        log.info("ZaloPay payment request: amount={}", request.getAmount());
        String response = zaloPayService.createOrder(request.getAmount(), request.getDescription());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order-status/{appTransId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> getOrderStatus(@PathVariable String appTransId) {
        log.info("ZaloPay status check: appTransId={}", appTransId);
        String response = zaloPayService.getOrderStatus(appTransId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<String>> callback(@RequestBody String body) {
        log.info("ZaloPay callback received: {}", body);
        return ResponseEntity.ok(ApiResponse.success("Callback received", body));
    }
}
