package com.utetea.backend.controller;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.PayPalPaymentRequest;
import com.utetea.backend.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PayPalController {
    
    private final PayPalService payPalService;
    
    @PostMapping("/create-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(@RequestBody PayPalPaymentRequest request) {
        try {
            // Validate amount
            if (request.getTotal() == null || request.getTotal().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Amount is required"));
            }
            
            double amount;
            try {
                amount = Double.parseDouble(request.getTotal());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount format: " + request.getTotal()));
            }
            
            if (amount <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Amount must be greater than 0"));
            }
            
            String approvalUrl = payPalService.createPayment(amount, request.getCurrency(), request.getDescription());
            
            Map<String, String> result = new HashMap<>();
            result.put("approvalUrl", approvalUrl);
            
            log.info("PayPal payment created: {}", approvalUrl);
            return ResponseEntity.ok(ApiResponse.success("Payment URL created", result));
        } catch (NumberFormatException e) {
            log.error("PayPal invalid amount", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount format"));
        } catch (PayPalRESTException e) {
            log.error("PayPal API error: {} - {}", e.getResponsecode(), e.getMessage());
            String errorMsg = "PayPal error: " + e.getMessage();
            if (e.getResponsecode() == 401) {
                errorMsg = "PayPal credentials invalid or expired. Please check PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET";
            } else if (e.getResponsecode() == 400) {
                errorMsg = "PayPal request invalid: " + e.getDetails();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorMsg));
        }
    }
    
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, String>>> success(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = payPalService.executePayment(paymentId, payerId);
            
            Map<String, String> result = new HashMap<>();
            result.put("paymentId", payment.getId());
            result.put("state", payment.getState());
            
            log.info("PayPal payment success: {}", payment.getId());
            return ResponseEntity.ok(ApiResponse.success("Payment successful", result));
        } catch (PayPalRESTException e) {
            log.error("PayPal execution error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Payment execution failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/cancel")
    public ResponseEntity<ApiResponse<String>> cancel() {
        log.info("PayPal payment cancelled");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Payment was cancelled"));
    }
}
