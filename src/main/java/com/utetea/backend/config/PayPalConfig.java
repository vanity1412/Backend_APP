package com.utetea.backend.config;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Getter
public class PayPalConfig {
    
    @Value("${paypal.client.id:}")
    private String clientId;
    
    @Value("${paypal.client.secret:}")
    private String clientSecret;
    
    @Value("${paypal.mode:sandbox}")
    private String mode;
    
    @Value("${paypal.success-url:http://localhost:8080/api/paypal/success}")
    private String successUrl;
    
    @Value("${paypal.cancel-url:http://localhost:8080/api/paypal/cancel}")
    private String cancelUrl;
    
    @Bean
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> sdkConfig = new HashMap<>();
        sdkConfig.put("mode", mode);
        return sdkConfig;
    }
    
    @Bean
    public APIContext apiContext() throws PayPalRESTException {
        APIContext apiContext = new APIContext(clientId, clientSecret, mode);
        apiContext.setConfigurationMap(paypalSdkConfig());
        return apiContext;
    }
}
