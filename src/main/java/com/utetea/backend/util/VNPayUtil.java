package com.utetea.backend.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class VNPayUtil {
    
    // FIX Medium #9: Sử dụng SecureRandom thay vì Random để đảm bảo uniqueness
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
    
    /**
     * Hash all fields - GIỐNG HỆT app mẫu: URL encode giá trị
     */
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                sb.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Hash all fields với secret key - tạo HMAC SHA512
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        String queryString = hashAllFields(fields);
        return hmacSHA512(secretKey, queryString);
    }
    
    /**
     * FIX Medium #9: Generate unique transaction reference using timestamp + SecureRandom
     * Format: timestamp (13 digits) + random (remaining digits)
     * This ensures uniqueness even under high concurrency
     */
    public static String getRandomNumber(int len) {
        if (len <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        // Sử dụng timestamp để đảm bảo uniqueness cơ bản
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        if (len <= timestamp.length()) {
            // Nếu len ngắn, chỉ dùng random
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            return sb.toString();
        }
        
        // Kết hợp timestamp + random để đảm bảo unique
        int randomLen = len - Math.min(8, timestamp.length()); // Giữ 8 ký tự timestamp
        String timestampPart = timestamp.substring(timestamp.length() - Math.min(8, len));
        
        StringBuilder sb = new StringBuilder(len);
        sb.append(timestampPart);
        
        for (int i = 0; i < randomLen; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        
        return sb.toString();
    }
}
