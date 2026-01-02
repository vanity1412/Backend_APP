package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.BlockedIPDto;
import com.utetea.backend.model.BlockedIP.BlockType;
import com.utetea.backend.service.BlockedIPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🚫 BLOCKED IP CONTROLLER
 * API quản lý danh sách IP bị chặn
 * Chỉ Admin/Manager mới có quyền truy cập
 */
@RestController
@RequestMapping("/api/blocked-ips")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🚫 Blocked IP Management", description = "Quản lý IP bị chặn")
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class BlockedIPController {

    private final BlockedIPService blockedIPService;

    @PostMapping("/block")
    @Operation(summary = "Block một IP", description = "Chặn một địa chỉ IP")
    public ResponseEntity<ApiResponse<BlockedIPDto>> blockIP(@RequestBody BlockIPRequest request) {
        log.info("Block IP request: {}", request.getIpAddress());
        
        BlockType blockType = BlockType.valueOf(request.getBlockType().toUpperCase());
        
        BlockedIPDto result = blockedIPService.blockIP(
            request.getIpAddress(),
            blockType,
            request.getReason(),
            request.getDurationHours(),
            request.getRelatedUserId(),
            request.getAlertId()
        );
        
        return ResponseEntity.ok(ApiResponse.success("IP đã bị chặn thành công", result));
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Unblock một IP", description = "Gỡ chặn một địa chỉ IP")
    public ResponseEntity<ApiResponse<BlockedIPDto>> unblockIP(
            @PathVariable Long id,
            @RequestBody(required = false) UnblockIPRequest request) {
        
        String reason = request != null ? request.getReason() : "Admin unblock";
        BlockedIPDto result = blockedIPService.unblockIP(id, reason);
        
        return ResponseEntity.ok(ApiResponse.success("IP đã được gỡ chặn", result));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách IP đang bị block")
    public ResponseEntity<ApiResponse<Page<BlockedIPDto>>> getActiveBlockedIPs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<BlockedIPDto> result = blockedIPService.getActiveBlockedIPs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả lịch sử block IP")
    public ResponseEntity<ApiResponse<Page<BlockedIPDto>>> getAllBlockedIPs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<BlockedIPDto> result = blockedIPService.getAllBlockedIPs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết một blocked IP")
    public ResponseEntity<ApiResponse<BlockedIPDto>> getBlockedIPById(@PathVariable Long id) {
        BlockedIPDto result = blockedIPService.getBlockedIPById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm theo IP")
    public ResponseEntity<ApiResponse<List<BlockedIPDto>>> searchByIP(@RequestParam String ip) {
        List<BlockedIPDto> result = blockedIPService.searchByIP(ip);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy IP blocks liên quan đến user")
    public ResponseEntity<ApiResponse<List<BlockedIPDto>>> getBlockedIPsByUser(@PathVariable Long userId) {
        List<BlockedIPDto> result = blockedIPService.getBlockedIPsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/check")
    @Operation(summary = "Kiểm tra IP có bị block không")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIP(@RequestParam String ip) {
        boolean isBlocked = blockedIPService.isIPBlocked(ip);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "ip", ip,
            "isBlocked", isBlocked
        )));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Thống kê IP bị block")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = blockedIPService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Refresh cache IP bị block")
    public ResponseEntity<ApiResponse<String>> refreshCache() {
        blockedIPService.refreshCache();
        return ResponseEntity.ok(ApiResponse.success("Cache đã được refresh"));
    }
}

// Request DTOs
@lombok.Data
class BlockIPRequest {
    private String ipAddress;
    private String blockType; // TEMPORARY, PERMANENT
    private String reason;
    private Integer durationHours; // Cho TEMPORARY
    private Long relatedUserId;
    private Long alertId;
}

@lombok.Data
class UnblockIPRequest {
    private String reason;
}
