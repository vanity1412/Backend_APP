package com.utetea.backend.service;

import com.utetea.backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ SECURITY: Rate Limiting Service
 * Prevent abuse của các endpoint nhạy cảm
 */
@Service
@Slf4j
public class RateLimitService {
    
    // In-memory cache để track rate limit
    // Key format: "type:identifier" (e.g., "otp:0123456789", "spin:userId123")
    private final Map<String, RateLimitEntry> rateLimitCache = new ConcurrentHashMap<>();
    
    // Rate limit constants
    private static final int OTP_MAX_ATTEMPTS = 5; // 5 lần/giờ per phone
    private static final int OTP_WINDOW_MINUTES = 60;
    
    private static final int SPIN_MAX_ATTEMPTS = 10; // 10 lần/ngày per user
    private static final int SPIN_WINDOW_MINUTES = 1440; // 24 hours
    
    private static final int ORDER_MAX_ATTEMPTS = 20; // 20 đơn/giờ per user
    private static final int ORDER_WINDOW_MINUTES = 60;
    
    /**
     * Check và increment rate limit cho OTP
     */
    public void checkOtpRateLimit(String phone) {
        String key = "otp:" + phone;
        checkRateLimit(key, OTP_MAX_ATTEMPTS, OTP_WINDOW_MINUTES, 
            "Bạn đã gửi OTP quá nhiều lần. Vui lòng thử lại sau " + OTP_WINDOW_MINUTES + " phút");
    }
    
    /**
     * Check và increment rate limit cho Spin Wheel
     */
    public void checkSpinRateLimit(Long userId) {
        String key = "spin:" + userId;
        checkRateLimit(key, SPIN_MAX_ATTEMPTS, SPIN_WINDOW_MINUTES, 
            "Bạn đã hết lượt quay hôm nay. Vui lòng quay lại vào ngày mai");
    }
    
    /**
     * Check và increment rate limit cho Create Order
     */
    public void checkOrderRateLimit(Long userId) {
        String key = "order:" + userId;
        checkRateLimit(key, ORDER_MAX_ATTEMPTS, ORDER_WINDOW_MINUTES, 
            "Bạn đã đặt quá nhiều đơn trong 1 giờ. Vui lòng thử lại sau");
    }
    
    /**
     * Core method để check rate limit
     */
    private void checkRateLimit(String key, int maxAttempts, int windowMinutes, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        
        // Cleanup expired entries (optional, để tránh memory leak)
        cleanupExpiredEntries();
        
        RateLimitEntry entry = rateLimitCache.get(key);
        
        if (entry == null) {
            // First attempt
            rateLimitCache.put(key, new RateLimitEntry(1, now));
            log.debug("Rate limit - First attempt for key: {}", key);
            return;
        }
        
        // Check if window has expired
        LocalDateTime windowStart = now.minusMinutes(windowMinutes);
        if (entry.getFirstAttemptTime().isBefore(windowStart)) {
            // Window expired, reset counter
            rateLimitCache.put(key, new RateLimitEntry(1, now));
            log.debug("Rate limit - Window expired, reset counter for key: {}", key);
            return;
        }
        
        // Check if exceeded limit
        if (entry.getAttemptCount() >= maxAttempts) {
            long minutesRemaining = windowMinutes - 
                java.time.Duration.between(entry.getFirstAttemptTime(), now).toMinutes();
            
            log.warn("Rate limit exceeded for key: {}. Attempts: {}/{}", 
                key, entry.getAttemptCount(), maxAttempts);
            
            throw new BusinessException(
                errorMessage + " (còn " + minutesRemaining + " phút)", 
                HttpStatus.TOO_MANY_REQUESTS
            );
        }
        
        // Increment counter
        entry.incrementAttempt();
        log.debug("Rate limit - Attempt {}/{} for key: {}", 
            entry.getAttemptCount(), maxAttempts, key);
    }
    
    /**
     * Cleanup expired entries để tránh memory leak
     * Chạy mỗi lần check (lightweight operation)
     */
    private void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(25); // Keep 25 hours
        rateLimitCache.entrySet().removeIf(entry -> 
            entry.getValue().getFirstAttemptTime().isBefore(cutoff)
        );
    }
    
    /**
     * Reset rate limit cho một key (dùng cho testing hoặc admin)
     */
    public void resetRateLimit(String key) {
        rateLimitCache.remove(key);
        log.info("Rate limit reset for key: {}", key);
    }
    
    /**
     * Inner class để lưu thông tin rate limit
     */
    private static class RateLimitEntry {
        private int attemptCount;
        private final LocalDateTime firstAttemptTime;
        
        public RateLimitEntry(int attemptCount, LocalDateTime firstAttemptTime) {
            this.attemptCount = attemptCount;
            this.firstAttemptTime = firstAttemptTime;
        }
        
        public void incrementAttempt() {
            this.attemptCount++;
        }
        
        public int getAttemptCount() {
            return attemptCount;
        }
        
        public LocalDateTime getFirstAttemptTime() {
            return firstAttemptTime;
        }
    }
}
