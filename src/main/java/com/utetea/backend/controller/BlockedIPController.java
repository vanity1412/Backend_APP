package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.BlockIPRequest;
import com.utetea.backend.dto.BlockedIPDto;
import com.utetea.backend.model.BlockedIP;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.BlockedIPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🚫 BLOCKED IP CONTROLLER
 * API endpoints cho quản lý IP bị chặn
 */
@RestController
@RequestMapping("/api/blocked-ips")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🚫 Blocked IP Management", description = "API quản lý IP bị chặn")
@Slf4j
public class BlockedIPController {

    private final BlockedIPService blockedIPService;
    private final UserRepository userRepository;

    @PostMapping("/block")
    @Operation(summary = "Block IP", description = "Chặn một địa chỉ IP")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlockedIPDto>> blockIP(
            @RequestBody BlockIPRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("POST /api/blocked-ips/block - IP: {} | Type: {} | By: {}", 
            request.getIpAddress(), request.getBlockType(), username);
        
        try {
            // Lấy user từ username
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            BlockedIP blocked = blockedIPService.blockIP(request, currentUser.getId());
            BlockedIPDto dto = BlockedIPDto.fromEntity(blocked, currentUser.getUsername(), null, null);
            
            return ResponseEntity.ok(ApiResponse.success("IP đã được chặn", dto));
        } catch (RuntimeException e) {
            log.error("Failed to block IP: {} - Error: {}", request.getIpAddress(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Unblock IP", description = "Gỡ chặn một địa chỉ IP")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlockedIPDto>> unblockIP(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("POST /api/blocked-ips/{}/unblock by {}", id, username);
        
        try {
            // Lấy user từ username
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            String reason = request != null ? request.get("reason") : null;
            BlockedIP unblocked = blockedIPService.unblockIP(id, currentUser.getId(), reason);
            BlockedIPDto dto = BlockedIPDto.fromEntity(unblocked, null, currentUser.getUsername(), null);
            
            return ResponseEntity.ok(ApiResponse.success("IP đã được gỡ chặn", dto));
        } catch (RuntimeException e) {
            log.error("Failed to unblock IP id: {} - Error: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get Active Blocked IPs", description = "Lấy danh sách IP đang bị chặn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BlockedIPDto>>> getActiveBlockedIPs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/blocked-ips - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BlockedIPDto> blockedIPs = blockedIPService.getActiveBlockedIPs(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(blockedIPs));
    }

    @GetMapping("/all")
    @Operation(summary = "Get All Blocked IPs", description = "Lấy tất cả blocked IPs (bao gồm đã gỡ)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BlockedIPDto>>> getAllBlockedIPs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/blocked-ips/all - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BlockedIPDto> blockedIPs = blockedIPService.getAllBlockedIPs(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(blockedIPs));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Blocked IP", description = "Tìm kiếm theo địa chỉ IP")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BlockedIPDto>>> searchBlockedIP(
            @RequestParam String ip) {
        
        log.info("GET /api/blocked-ips/search - ip: {}", ip);
        
        List<BlockedIPDto> results = blockedIPService.searchByIP(ip);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get Statistics", description = "Lấy thống kê blocked IPs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        log.info("GET /api/blocked-ips/statistics");
        
        Map<String, Object> stats = blockedIPService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/check")
    @Operation(summary = "Check IP Status", description = "Kiểm tra IP có bị chặn không")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIPStatus(
            @RequestParam String ip) {
        
        log.info("GET /api/blocked-ips/check - ip: {}", ip);
        
        boolean isBlocked = blockedIPService.isIPBlocked(ip);
        Map<String, Object> result = Map.of(
            "ipAddress", ip,
            "isBlocked", isBlocked
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/my-ip")
    @Operation(summary = "Get My IP", description = "Lấy IP hiện tại của client")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyIP(
            HttpServletRequest request) {
        
        String clientIP = getClientIP(request);
        String normalizedIP = normalizeIP(clientIP);
        boolean isBlocked = blockedIPService.isIPBlocked(clientIP);
        boolean isNormalizedBlocked = !clientIP.equals(normalizedIP) ? 
            blockedIPService.isIPBlocked(normalizedIP) : false;
        
        Map<String, Object> result = Map.of(
            "originalIP", clientIP,
            "normalizedIP", normalizedIP,
            "isOriginalBlocked", isBlocked,
            "isNormalizedBlocked", isNormalizedBlocked,
            "headers", Map.of(
                "X-Forwarded-For", request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : "null",
                "X-Real-IP", request.getHeader("X-Real-IP") != null ? request.getHeader("X-Real-IP") : "null",
                "RemoteAddr", request.getRemoteAddr()
            )
        );
        
        log.info("GET /api/blocked-ips/my-ip - Result: {}", result);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Lấy IP thực của client (copy từ BlockedIPFilter)
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
     * Normalize IP address (copy từ BlockedIPFilter)
     */
    private String normalizeIP(String ip) {
        if (ip == null) return "unknown";
        
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        
        return ip;
    }
}
