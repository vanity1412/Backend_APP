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
            double amount = Double.parseDouble(request.getTotal());
            String approvalUrl = payPalService.createPayment(amount, request.getCurrency(), request.getDescription());
            
            Map<String, String> result = new HashMap<>();
            result.put("approvalUrl", approvalUrl);
            
            log.info("PayPal payment created: {}", approvalUrl);
            return ResponseEntity.ok(ApiResponse.success("Payment URL created", result));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount format"));
        } catch (PayPalRESTException e) {
            log.error("PayPal error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error creating payment: " + e.getMessage()));
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
