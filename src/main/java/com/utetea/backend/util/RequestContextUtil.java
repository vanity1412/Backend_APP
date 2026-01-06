package com.utetea.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class để lấy HttpServletRequest từ context
 * Dùng cho các service cần log IP mà không có request parameter
 */
public class RequestContextUtil {

    /**
     * Lấy HttpServletRequest hiện tại từ context
     * @return HttpServletRequest hoặc null nếu không có
     */
    public static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest();
            }
        } catch (Exception e) {
            // Ignore - có thể không có request context (background job, etc.)
        }
        return null;
    }
    
    /**
     * Lấy IP của client từ request hiện tại
     * @return IP address hoặc null
     */
    public static String getCurrentClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
