package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.MoMoPaymentRequest;
import com.utetea.backend.service.MoMoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/momo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MoMoController {
    
    private final MoMoService moMoService;
    
    @PostMapping("/create-payment")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> createPayment(@RequestBody MoMoPaymentRequest request) {
        log.info("MoMo payment request: amount={}", request.getAmount());
        String response = moMoService.createPaymentRequest(request.getAmount(), request.getOrderInfo());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order-status/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> checkPaymentStatus(@PathVariable String orderId) {
        log.info("MoMo status check: orderId={}", orderId);
        String response = moMoService.checkPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<String>> callback(@RequestBody String body) {
        log.info("MoMo callback received: {}", body);
        return ResponseEntity.ok(ApiResponse.success("Callback received", body));
    }
}
