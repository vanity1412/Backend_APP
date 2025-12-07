package com.utetea.backend.service;

import com.utetea.backend.config.VNPayConfig;
import com.utetea.backend.dto.VNPayPaymentRequest;
import com.utetea.backend.dto.VNPayPaymentResponse;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.Order;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.repository.OrderRepository;
import com.utetea.backend.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {
    
    private final VNPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    
    public VNPayConfig getConfig() {
        return vnPayConfig;
    }
    
    public String createTestPaymentUrl() throws UnsupportedEncodingException {
        // Tạo URL test với số tiền 100,000 VND
        long amount = 100000 * 100; // 10,000,000
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", VNPayUtil.getRandomNumber(8));
        vnpParams.put("vnp_OrderInfo", "Test thanh toan");
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);
        
        // Build query
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        
        log.info("=== Test Payment URL ===");
        log.info("Hash data: {}", hashData.toString());
        log.info("Secure hash: {}", vnpSecureHash);
        
        return vnPayConfig.getVnpUrl() + "?" + queryUrl;
    }
    
    public VNPayPaymentResponse createPayment(VNPayPaymentRequest request) throws UnsupportedEncodingException {
        // Validate order exists
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));
        
        // Convert amount to VND (multiply by 100 as per VNPAY requirement)
        long amount = order.getFinalPrice().longValue() * 100;
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        
        String txnRef = VNPayUtil.getRandomNumber(8);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", request.getOrderInfo() != null ? 
            request.getOrderInfo() : "Thanh toan don hang " + order.getId());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        
        String locate = "vn";
        vnpParams.put("vnp_Locale", locate);
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        
        // Ensure IP address is not null
        String ipAddr = request.getIpAddress();
        if (ipAddr == null || ipAddr.isEmpty()) {
            ipAddr = "127.0.0.1";
        }
        vnpParams.put("vnp_IpAddr", ipAddr);
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);
        
        // Build hash data and query URL
        // hashData KHÔNG encode, query CÓ encode
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data (NO encoding - raw values)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                
                // Build query (WITH URL encoding)
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayConfig.getVnpUrl() + "?" + queryUrl;
        
        log.info("=== VNPAY Payment Debug ===");
        log.info("Order ID: {}", order.getId());
        log.info("Amount: {}", amount);
        log.info("TmnCode: {}", vnPayConfig.getTmnCode());
        log.info("Hash Secret: {}...", vnPayConfig.getHashSecret().substring(0, 10));
        log.info("Hash data: {}", hashData.toString());
        log.info("Secure hash: {}", vnpSecureHash);
        log.info("Payment URL: {}", paymentUrl);
        
        log.info("Created VNPAY payment URL for order: {}", order.getId());
        
        return new VNPayPaymentResponse(paymentUrl, "Success");
    }
    
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        log.info("=== VNPAY Callback Debug ===");
        log.info("Received params: {}", params);
        
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }
        if (params.containsKey("vnp_SecureHash")) {
            params.remove("vnp_SecureHash");
        }
        
        String signValue = VNPayUtil.hashAllFields(params, vnPayConfig.getHashSecret());
        
        log.info("Calculated hash: {}", signValue);
        log.info("Received hash: {}", vnpSecureHash);
        
        if (signValue.equals(vnpSecureHash)) {
            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            String orderInfo = params.get("vnp_OrderInfo");
            
            log.info("Response code: {}, Transaction status: {}", responseCode, transactionStatus);
            
            // Extract order ID from order info
            if (orderInfo != null && orderInfo.contains("don hang ")) {
                try {
                    String orderIdStr = orderInfo.substring(orderInfo.lastIndexOf(" ") + 1);
                    Long id = Long.parseLong(orderIdStr);
                    
                    Order order = orderRepository.findById(id).orElse(null);
                    if (order != null && "00".equals(responseCode)) {
                        // Payment successful
                        log.info("Payment successful for order: {}", id);
                        return true;
                    } else {
                        log.warn("Payment failed or order not found. Order: {}, ResponseCode: {}", id, responseCode);
                    }
                } catch (Exception e) {
                    log.error("Error parsing order ID from callback", e);
                }
            } else {
                log.warn("Invalid order info format: {}", orderInfo);
            }
        } else {
            log.error("Signature verification failed!");
        }
        
        return false;
    }
}
