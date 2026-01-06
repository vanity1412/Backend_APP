package com.utetea.backend.filter;

import com.utetea.backend.service.WhitelistedIPService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 🔐 WHITELIST IP FILTER
 * Yêu cầu Admin/Manager phải truy cập từ IP trong whitelist (database)
 * User thường KHÔNG bị ảnh hưởng - truy cập thoải mái
 * Filter này chạy SAU JwtAuthenticationFilter để có thể kiểm tra role
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class WhitelistIPFilter extends OncePerRequestFilter {

    private final WhitelistedIPService whitelistedIPService;

    // Bật/tắt tính năng whitelist check cho Admin/Manager
    @Value("${whitelist.check.enabled:false}")
    private boolean whitelistCheckEnabled;

    public WhitelistIPFilter(@Lazy WhitelistedIPService whitelistedIPService) {
        this.whitelistedIPService = whitelistedIPService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Nếu tính năng whitelist check bị tắt, cho qua
        if (!whitelistCheckEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nếu chưa authenticate hoặc không phải Admin/Manager, cho qua (User thoải mái)
        if (authentication == null || !isAdminOrManager(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIP = getClientIP(request);
        String normalizedIP = normalizeIP(clientIP);

        // Kiểm tra IP có trong whitelist không
        if (!whitelistedIPService.isIPWhitelisted(normalizedIP)) {
            log.warn("🚫 WHITELIST CHECK FAILED | User: {} | Role: {} | IP: {} | Path: {}",
                    authentication.getName(),
                    getRole(authentication),
                    clientIP,
                    request.getRequestURI());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"IP_NOT_WHITELISTED\"," +
                            "\"message\":\"IP của bạn chưa được cấp quyền truy cập Admin/Manager. " +
                            "Vui lòng liên hệ quản trị viên để thêm IP vào whitelist.\"," +
                            "\"yourIP\":\"" + clientIP + "\"}"
            );
            return;
        }

        log.info("✅ WHITELIST CHECK PASSED | User: {} | Role: {} | IP: {}",
                authentication.getName(), getRole(authentication), clientIP);

        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra user có phải Admin hoặc Manager không
     */
    private boolean isAdminOrManager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));
    }

    /**
     * Lấy role của user
     */
    private String getRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "UNKNOWN";
        }
        return authentication.getAuthorities().toString();
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
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Normalize IP address
     */
    private String normalizeIP(String ip) {
        if (ip == null) return "unknown";

        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Không filter cho các endpoint public
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/assets/") ||
                path.startsWith("/api/auth/") ||
                path.equals("/") ||
                path.equals("/favicon.ico") ||
                path.equals("/actuator/health");
    }
}
