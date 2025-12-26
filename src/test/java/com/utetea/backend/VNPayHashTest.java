package com.utetea.backend;

import com.utetea.backend.util.VNPayUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIX Medium #12: Sử dụng JUnit assertions thay vì Java assert keyword
 */
public class VNPayHashTest {
    
    @Test
    public void testHashGeneration() {
        String hashSecret = "CYOVRAR3RXF4FAZNW6Z8ZT4FSPJAH08H";
        
        // Test data theo format VNPAY
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", "20251207100000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", "Test payment");
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", "http://localhost:8080/api/vnpay/callback");
        params.put("vnp_TmnCode", "GRPOHLK1");
        params.put("vnp_TxnRef", "12345678");
        params.put("vnp_Version", "2.1.0");
        
        String hash = VNPayUtil.hashAllFields(params, hashSecret);
        
        System.out.println("Generated hash: " + hash);
        System.out.println("Hash length: " + hash.length());
        
        // Hash phải có độ dài 128 ký tự (SHA512 = 512 bits = 64 bytes = 128 hex chars)
        assertEquals(128, hash.length(), "Hash length should be 128");
        assertFalse(hash.isEmpty(), "Hash should not be empty");
    }
    
    @Test
    public void testHmacSHA512() {
        String key = "CYOVRAR3RXF4FAZNW6Z8ZT4FSPJAH08H";
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=GRPOHLK1";
        
        String hash = VNPayUtil.hmacSHA512(key, data);
        
        System.out.println("HMAC SHA512: " + hash);
        System.out.println("Length: " + hash.length());
        
        assertFalse(hash.isEmpty(), "Hash should not be empty");
        assertEquals(128, hash.length(), "Hash length should be 128");
    }
    
    /**
     * FIX Medium #9: Sử dụng statistical approach để tránh flaky test
     * Thay vì so sánh 2 random numbers, kiểm tra uniqueness với nhiều samples
     */
    @Test
    public void testGetRandomNumber() {
        // Test length
        String random1 = VNPayUtil.getRandomNumber(8);
        assertEquals(8, random1.length(), "Random number should have correct length");
        
        // Test uniqueness với statistical approach
        java.util.Set<String> randoms = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            randoms.add(VNPayUtil.getRandomNumber(8));
        }
        // Với 100 lần generate, xác suất trùng gần như 0
        // Cho phép tối đa 5 trùng lặp (95% unique)
        assertTrue(randoms.size() >= 95, "Should generate mostly unique numbers, got " + randoms.size() + " unique out of 100");
    }
}
