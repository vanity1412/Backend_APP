package com.utetea.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * FIX High #4: Removed hardcoded default values for sensitive credentials
 * These MUST be configured via environment variables or application.properties
 */
@Configuration
@Getter
public class VNPayConfig {
    
    @Value("${vnpay.tmn-code:}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret:}")
    private String hashSecret;
    
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl;
    
    @Value("${vnpay.return-url:http://localhost:8080/api/vnpay/callback}")
    private String returnUrl;
    
    @Value("${vnpay.version:2.1.0}")
    private String version;
    
    @Value("${vnpay.command:pay}")
    private String command;
    
    @Value("${vnpay.order-type:other}")
    private String orderType;
    
    @PostConstruct
    public void validateConfig() {
        if (tmnCode == null || tmnCode.isEmpty()) {
            throw new IllegalStateException("VNPay tmnCode must be configured via vnpay.tmn-code property");
        }
        if (hashSecret == null || hashSecret.isEmpty()) {
            throw new IllegalStateException("VNPay hashSecret must be configured via vnpay.hash-secret property");
        }
    }
}
