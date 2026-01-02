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

        // Check nếu IP bị block
        if (blockedIPService.checkAndIncrementIfBlocked(clientIP)) {
            log.warn("🚫 Blocked request from IP: {} to {}", clientIP, request.getRequestURI());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"error\":\"ACCESS_DENIED\"," +
                "\"message\":\"IP của bạn đã bị chặn do vi phạm chính sách sử dụng. " +
                "Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Không filter cho static resources
        return path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/assets/") ||
               path.equals("/") ||
               path.equals("/favicon.ico");
    }
}
