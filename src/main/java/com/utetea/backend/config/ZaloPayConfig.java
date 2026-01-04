package com.utetea.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ZaloPayConfig {
    
    @Value("${zalopay.app-id:2554}")
    private String appId;
    
    @Value("${zalopay.key1:sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn}")
    private String key1;
    
    @Value("${zalopay.key2:trMrHtvjo6myautxDUiAcYsVtaeQ8nhf}")
    private String key2;
    
    @Value("${zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String endpoint;
    
    @Value("${zalopay.query-endpoint:https://sb-openapi.zalopay.vn/v2/query}")
    private String queryEndpoint;
    
    @Value("${zalopay.callback-url:}")
    private String callbackUrl;
}
