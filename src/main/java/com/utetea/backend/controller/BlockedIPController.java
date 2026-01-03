package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.BlockIPRequest;
import com.utetea.backend.dto.BlockedIPDto;
import com.utetea.backend.model.BlockedIP;
import com.utetea.backend.model.User;
import com.utetea.backend.service.BlockedIPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/block")
    @Operation(summary = "Block IP", description = "Chặn một địa chỉ IP")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlockedIPDto>> blockIP(
            @RequestBody BlockIPRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("POST /api/blocked-ips/block - IP: {} | Type: {} | By: {}", 
            request.getIpAddress(), request.getBlockType(), currentUser.getUsername());
        
        try {
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
            @AuthenticationPrincipal User currentUser) {
        
        log.info("POST /api/blocked-ips/{}/unblock", id);
        
        String reason = request != null ? request.get("reason") : null;
        BlockedIP unblocked = blockedIPService.unblockIP(id, currentUser.getId(), reason);
        BlockedIPDto dto = BlockedIPDto.fromEntity(unblocked, null, currentUser.getUsername(), null);
        
        return ResponseEntity.ok(ApiResponse.success("IP đã được gỡ chặn", dto));
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
}
