package com.utetea.backend.filter;

import com.utetea.backend.config.RateLimitConfig;
import com.utetea.backend.service.UserMonitoringService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitConfig rateLimitConfig;
    private final UserMonitoringService userMonitoringService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIP(request);
        String requestUri = request.getRequestURI();
        
        Bucket bucket;
        
        // Chọn bucket phù hợp dựa trên endpoint
        if (requestUri.contains("/api/otp/")) {
            bucket = rateLimitConfig.resolveOtpBucket(clientIp);
        } else if (requestUri.contains("/api/auth/")) {
            bucket = rateLimitConfig.resolveAuthBucket(clientIp);
        } else {
            bucket = rateLimitConfig.resolveBucket(clientIp);
        }
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Thêm headers để client biết còn bao nhiêu requests
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again in " + waitForRefill + " seconds.\"}");
            
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
            
            // 🛡️ Ghi log vào hệ thống giám sát
            try {
                userMonitoringService.logRateLimitHit(null, requestUri, request);
            } catch (Exception e) {
                log.error("Failed to log rate limit hit to monitoring", e);
            }
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        // Kiểm tra các headers phổ biến cho proxy/load balancer
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Không filter cho static resources và swagger
        return path.startsWith("/swagger") || 
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/assets/") ||
               path.equals("/") ||
               path.equals("/index.html");
    }
}
