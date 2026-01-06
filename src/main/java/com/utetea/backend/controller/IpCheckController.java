package com.utetea.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔍 IP CHECK CONTROLLER
 * Endpoint để kiểm tra IP của thiết bị đang truy cập
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class IpCheckController {

    /**
     * API public để xem IP của thiết bị đang truy cập
     * Dùng để biết IP khi kết nối VPN
     */
    @GetMapping("/my-ip")
    public ResponseEntity<Map<String, Object>> getMyIp(HttpServletRequest request) {
        String clientIP = getClientIP(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("yourIP", clientIP);
        response.put("message", "Đây là IP của bạn. Nếu bạn đang dùng VPN, hãy thêm IP này vào whitelist.");
        
        // Thêm thông tin headers để debug
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        headers.put("X-Real-IP", request.getHeader("X-Real-IP"));
        headers.put("RemoteAddr", request.getRemoteAddr());
        response.put("headers", headers);
        
        log.info("🔍 IP Check | IP: {} | Headers: {}", clientIP, headers);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy IP thực của client
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
