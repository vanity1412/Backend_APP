package com.utetea.backend.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.OrderRequest;
import com.utetea.backend.dto.VNPayPaymentRequest;
import com.utetea.backend.dto.VNPayPaymentResponse;
import com.utetea.backend.service.OrderService;
import com.utetea.backend.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class VNPayController {
    
    private final VNPayService vnPayService;
    private final OrderService orderService;
    
    @PostMapping("/create-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createPayment(
            @Valid @RequestBody VNPayPaymentRequest request,
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
    
    /**
     * Tạo payment URL với amount (không cần orderId) - dùng cho flow VNPAY mới
     */
    @PostMapping("/create-payment-amount")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createPaymentWithAmount(
            @RequestParam("amount") Long amount,
            @RequestParam(value = "orderInfo", defaultValue = "Thanh toan UTE Tea") String orderInfo,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getIpAddress(httpRequest);
            VNPayPaymentResponse response = vnPayService.createPaymentWithAmount(amount, orderInfo, ipAddress);
            return ResponseEntity.ok(ApiResponse.success("Payment URL created", response));
        } catch (UnsupportedEncodingException e) {
            log.error("Error creating payment URL", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error creating payment URL"));
        }
    }
    
    /**
     * Tạo đơn hàng SAU KHI thanh toán VNPAY thành công
     */
    @PostMapping("/create-order-after-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderDto>> createOrderAfterPayment(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            OrderDto order = orderService.createOrder(username, request);
            return ResponseEntity.ok(ApiResponse.success("Order created after payment", order));
        } catch (Exception e) {
            log.error("Error creating order after payment", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error creating order: " + e.getMessage()));
        }
    }
    
    /**
     * Callback từ VNPAY - trả về HTML template cho WebView
     */
    @GetMapping("/callback")
    public String paymentCallback(@RequestParam Map<String, String> params, Model model) {
        log.info("Received VNPAY callback with params: {}", params);
        
        boolean isSuccess = vnPayService.handlePaymentCallback(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String amount = params.get("vnp_Amount");
        
        model.addAttribute("responseCode", responseCode);
        model.addAttribute("transactionNo", transactionNo);
        model.addAttribute("amount", amount != null ? Long.parseLong(amount) / 100 : 0);
        
        if (isSuccess) {
            model.addAttribute("message", "Thanh toán thành công!");
            return "payment_success";
        } else {
            model.addAttribute("message", "Thanh toán thất bại");
            return "payment_failure";
        }
    }
    
    /**
     * API callback trả về JSON (cho các client khác)
     */
    @GetMapping("/callback-api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentCallbackApi(@RequestParam Map<String, String> params) {
        log.info("Received VNPAY API callback with params: {}", params);
        
        boolean isSuccess = vnPayService.handlePaymentCallback(params);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", isSuccess);
        result.put("responseCode", params.get("vnp_ResponseCode"));
        result.put("transactionNo", params.get("vnp_TransactionNo"));
        
        if (isSuccess) {
            return ResponseEntity.ok(ApiResponse.success("Payment successful", result));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment failed"));
        }
    }
    
    @GetMapping("/test-config")
    @ResponseBody
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
    @ResponseBody
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
