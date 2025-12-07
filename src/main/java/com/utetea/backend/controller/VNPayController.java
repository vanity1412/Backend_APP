package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.VNPayPaymentRequest;
import com.utetea.backend.dto.VNPayPaymentResponse;
import com.utetea.backend.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class VNPayController {
    
    private final VNPayService vnPayService;
    
    @PostMapping("/create-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createPayment(
            @RequestBody VNPayPaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getIpAddress(httpRequest);
            request.setIpAddress(ipAddress);
            
            VNPayPaymentResponse response = vnPayService.createPayment(request);
            return ResponseEntity.ok(ApiResponse.success("Payment URL created", response));
        } catch (UnsupportedEncodingException e) {
            log.error("Error creating payment URL", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error creating payment URL"));
        }
    }
    
    @GetMapping("/callback")
    public ResponseEntity<String> paymentCallback(@RequestParam Map<String, String> params) {
        log.info("Received VNPAY callback with params: {}", params);
        
        boolean isSuccess = vnPayService.handlePaymentCallback(params);
        
        if (isSuccess) {
            return ResponseEntity.ok("Payment successful");
        } else {
            return ResponseEntity.badRequest().body("Payment failed");
        }
    }
    
    @GetMapping("/test-config")
    public ResponseEntity<Map<String, String>> testConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("tmnCode", vnPayService.getConfig().getTmnCode());
        config.put("hashSecret", vnPayService.getConfig().getHashSecret().substring(0, 5) + "...");
        config.put("vnpUrl", vnPayService.getConfig().getVnpUrl());
        config.put("returnUrl", vnPayService.getConfig().getReturnUrl());
        config.put("version", vnPayService.getConfig().getVersion());
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/test-payment-url")
    public ResponseEntity<Map<String, String>> testPaymentUrl() {
        try {
            String testUrl = vnPayService.createTestPaymentUrl();
            Map<String, String> result = new HashMap<>();
            result.put("paymentUrl", testUrl);
            result.put("message", "Copy URL này và paste vào browser để test");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating test payment URL", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
