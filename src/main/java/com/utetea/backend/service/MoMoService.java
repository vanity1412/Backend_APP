package com.utetea.backend.service;

import com.utetea.backend.config.MoMoConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {
    
    private final MoMoConfig moMoConfig;
    
    public String createPaymentRequest(String amount, String orderInfo) {
        try {
            String requestId = moMoConfig.getPartnerCode() + new Date().getTime();
            String orderId = requestId;
            String extraData = "";
            
            if (orderInfo == null || orderInfo.isEmpty()) {
                orderInfo = "UTE Tea Payment";
            }
            
            String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                moMoConfig.getAccessKey(), amount, extraData, moMoConfig.getIpnUrl(), orderId, orderInfo,
                moMoConfig.getPartnerCode(), moMoConfig.getRedirectUrl(), requestId, moMoConfig.getRequestType()
            );
            
            String signature = signHmacSHA256(rawSignature, moMoConfig.getSecretKey());
            log.info("MoMo Signature: {}", signature);
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", moMoConfig.getPartnerCode());
            requestBody.put("accessKey", moMoConfig.getAccessKey());
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", moMoConfig.getRedirectUrl());
            requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", moMoConfig.getRequestType());
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(moMoConfig.getEndpoint());
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                log.info("MoMo Response: {}", result);
                return result.toString();
            }
        } catch (Exception e) {
            log.error("MoMo payment error", e);
            return "{\"error\": \"Failed to create payment request: " + e.getMessage() + "\"}";
        }
    }
    
    public String checkPaymentStatus(String orderId) {
        try {
            String requestId = moMoConfig.getPartnerCode() + new Date().getTime();
            
            String rawSignature = String.format(
                "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                moMoConfig.getAccessKey(), orderId, moMoConfig.getPartnerCode(), requestId
            );
            
            String signature = signHmacSHA256(rawSignature, moMoConfig.getSecretKey());
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", moMoConfig.getPartnerCode());
            requestBody.put("accessKey", moMoConfig.getAccessKey());
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(moMoConfig.getQueryEndpoint());
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                log.info("MoMo Status Response: {}", result);
                return result.toString();
            }
        } catch (Exception e) {
            log.error("MoMo status check error", e);
            return "{\"error\": \"Failed to check payment status: " + e.getMessage() + "\"}";
        }
    }
    
    private String signHmacSHA256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKey);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
