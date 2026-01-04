package com.utetea.backend.service;

import com.utetea.backend.config.ZaloPayConfig;
import com.utetea.backend.util.HMACUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloPayService {
    
    private final ZaloPayConfig zaloPayConfig;
    
    private String getCurrentTimeString(String format) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setCalendar(cal);
        return fmt.format(cal.getTimeInMillis());
    }
    
    public String createOrder(Long amount, String description) {
        Random rand = new Random();
        int randomId = rand.nextInt(1000000);
        
        if (amount == null || amount <= 0) {
            return "{\"error\": \"Amount is required and must be positive\"}";
        }
        
        if (description == null || description.isEmpty()) {
            description = "UTE Tea - Payment for order #" + randomId;
        }
        
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", zaloPayConfig.getAppId());
        order.put("app_trans_id", getCurrentTimeString("yyMMdd") + "_" + randomId);
        order.put("app_time", System.currentTimeMillis());
        order.put("app_user", "user123");
        order.put("amount", amount);
        order.put("description", description);
        order.put("bank_code", "");
        order.put("item", "[{}]");
        order.put("embed_data", "{}");
        
        if (zaloPayConfig.getCallbackUrl() != null && !zaloPayConfig.getCallbackUrl().isEmpty()) {
            order.put("callback_url", zaloPayConfig.getCallbackUrl());
        }
        
        String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|"
            + order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|"
            + order.get("item");
        
        String mac = HMACUtil.hmacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data);
        order.put("mac", mac);
        
        log.info("ZaloPay MAC: {}", mac);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(zaloPayConfig.getEndpoint());
            
            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
            
            post.setEntity(new UrlEncodedFormEntity(params));
            
            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }
                log.info("ZaloPay Response: {}", resultJsonStr);
                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            log.error("ZaloPay error", e);
            return "{\"error\": \"Failed to create order: " + e.getMessage() + "\"}";
        }
    }
    
    public String getOrderStatus(String appTransId) {
        String data = zaloPayConfig.getAppId() + "|" + appTransId + "|" + zaloPayConfig.getKey1();
        String mac = HMACUtil.hmacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), data);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(zaloPayConfig.getQueryEndpoint());
            
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("app_id", zaloPayConfig.getAppId()));
            params.add(new BasicNameValuePair("app_trans_id", appTransId));
            params.add(new BasicNameValuePair("mac", mac));
            
            post.setEntity(new UrlEncodedFormEntity(params));
            
            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }
                log.info("ZaloPay Status Response: {}", resultJsonStr);
                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            log.error("ZaloPay status error", e);
            return "{\"error\": \"Failed to get order status: " + e.getMessage() + "\"}";
        }
    }
}
