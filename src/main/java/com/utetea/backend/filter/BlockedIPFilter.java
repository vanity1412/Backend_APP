package com.utetea.backend.filter;

import com.utetea.backend.service.BlockedIPService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 🚫 BLOCKED IP FILTER
 * Chặn tất cả request từ IP bị block
 * Chạy TRƯỚC tất cả các filter khác
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE - 1) // Chạy trước RateLimitFilter
@Slf4j
public class BlockedIPFilter extends OncePerRequestFilter {

    private final BlockedIPService blockedIPService;

    public BlockedIPFilter(@Lazy BlockedIPService blockedIPService) {
        this.blockedIPService = blockedIPService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String normalizedIP = normalizeIP(clientIP);
        String requestPath = request.getRequestURI();
        
        log.info("🔍 [IP CHECK] IP: {} | Normalized: {} | Path: {}", clientIP, normalizedIP, requestPath);

        // Check nếu IP bị block (kiểm tra cả IP gốc và IP đã normalize)
        boolean isBlocked = blockedIPService.checkAndIncrementIfBlocked(clientIP);
        
        // Nếu IP gốc không bị block, kiểm tra IP đã normalize (cho trường hợp IPv6 localhost)
        if (!isBlocked && !clientIP.equals(normalizedIP)) {
            isBlocked = blockedIPService.checkAndIncrementIfBlocked(normalizedIP);
            if (isBlocked) {
                log.info("🔍 [IP CHECK] Blocked by normalized IP: {}", normalizedIP);
            }
        }
        
        if (isBlocked) {
            log.warn("🚫 BLOCKED REQUEST | IP: {} | Path: {} | Method: {}", 
                    clientIP, requestPath, request.getMethod());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"error\":\"IP_BLOCKED\"," +
                "\"message\":\"IP của bạn đã bị chặn do vi phạm chính sách sử dụng. " +
                "Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.\"," +
                "\"blockedIP\":\"" + clientIP + "\"}"
            );
            return;
        }
        
        log.debug("✅ [IP CHECK] IP {} allowed to proceed", clientIP);
        filterChain.doFilter(request, response);
    }

    /**
     * Lấy IP thực của client
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
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
    
    /**
     * Normalize IP address (convert IPv6 localhost to IPv4)
     */
    private String normalizeIP(String ip) {
        if (ip == null) return "unknown";
        
        // Convert IPv6 localhost to IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Không filter cho static resources và health check
        return path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/assets/") ||
               path.equals("/") ||
               path.equals("/favicon.ico") ||
               path.equals("/api/auth/health");
    }
}
