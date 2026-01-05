package com.utetea.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Configuration cho GHN (Giao Hàng Nhanh) API
 */
@Configuration
@Getter
public class GhnConfig {
    
    public static final String GHN_PROD_API_URL = "https://online-gateway.ghn.vn";
    public static final String GHN_DEV_API_URL = "https://dev-online-gateway.ghn.vn";
    
    @Value("${ghn.token:}")
    private String token;
    
    @Value("${ghn.shop-id:0}")
    private int shopId;
    
    @Value("${ghn.test-mode:true}")
    private boolean testMode;
    
    public String getHost() {
        return testMode ? GHN_DEV_API_URL : GHN_PROD_API_URL;
    }
}
