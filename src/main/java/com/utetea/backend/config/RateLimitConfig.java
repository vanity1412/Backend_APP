package com.utetea.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    
    // Cache để lưu bucket theo IP
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    
    // Cache cho OTP endpoints (giới hạn chặt hơn)
    private final Map<String, Bucket> otpBuckets = new ConcurrentHashMap<>();
    
    // Cache cho Auth endpoints
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    
    /**
     * Rate limit chung: 100 requests/phút cho mỗi IP
     */
    public Bucket resolveBucket(String ip) {
        return ipBuckets.computeIfAbsent(ip, this::createGeneralBucket);
    }
    
    /**
     * Rate limit cho OTP: 5 requests/phút (chống spam OTP)
     */
    public Bucket resolveOtpBucket(String ip) {
        return otpBuckets.computeIfAbsent(ip, this::createOtpBucket);
    }
    
    /**
     * Rate limit cho Auth (login/register): 10 requests/phút
     */
    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, this::createAuthBucket);
    }
    
    private Bucket createGeneralBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
    
    private Bucket createOtpBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
    
    private Bucket createAuthBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
