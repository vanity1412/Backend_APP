package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.model.User;
import com.utetea.backend.model.WhitelistedIP;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.WhitelistedIPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
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
 * 🔓 WHITELISTED IP CONTROLLER
 * API endpoints cho quản lý IP whitelist (Admin/Manager)
 */
@RestController
@RequestMapping("/api/whitelist-ips")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🔓 Whitelist IP Management", description = "API quản lý IP whitelist cho Admin/Manager")
@Slf4j
public class WhitelistedIPController {

    private final WhitelistedIPService whitelistedIPService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    @Operation(summary = "Add IP to Whitelist", description = "Thêm IP vào whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WhitelistedIP>> addToWhitelist(
            @RequestBody AddWhitelistRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/whitelist-ips/add - IP: {} | By: {}", request.getIpAddress(), username);

        try {
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            WhitelistedIP whitelisted = whitelistedIPService.addToWhitelist(
                    request.getIpAddress(),
                    request.getDescription(),
                    currentUser.getId()
            );

            return ResponseEntity.ok(ApiResponse.success("IP đã được thêm vào whitelist", whitelisted));
        } catch (RuntimeException e) {
            log.error("Failed to add IP to whitelist: {} - Error: {}", request.getIpAddress(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/remove")
    @Operation(summary = "Remove IP from Whitelist", description = "Xóa IP khỏi whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeFromWhitelist(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/whitelist-ips/{}/remove by {}", id, username);

        try {
            whitelistedIPService.removeFromWhitelist(id);
            return ResponseEntity.ok(ApiResponse.success("IP đã được xóa khỏi whitelist"));
        } catch (RuntimeException e) {
            log.error("Failed to remove IP from whitelist id: {} - Error: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get Active Whitelist", description = "Lấy danh sách IP đang trong whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WhitelistedIP>>> getActiveWhitelist() {
        log.info("GET /api/whitelist-ips");

        List<WhitelistedIP> whitelist = whitelistedIPService.getActiveWhitelist();
        return ResponseEntity.ok(ApiResponse.success(whitelist));
    }

    @GetMapping("/all")
    @Operation(summary = "Get All Whitelist", description = "Lấy tất cả whitelist (bao gồm đã xóa)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<WhitelistedIP>>> getAllWhitelist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/whitelist-ips/all - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<WhitelistedIP> whitelist = whitelistedIPService.getAllWhitelist(pageable);

        return ResponseEntity.ok(ApiResponse.success(whitelist));
    }

    @GetMapping("/check")
    @Operation(summary = "Check IP in Whitelist", description = "Kiểm tra IP có trong whitelist không")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIPWhitelist(
            @RequestParam String ip) {

        log.info("GET /api/whitelist-ips/check - ip: {}", ip);

        boolean isWhitelisted = whitelistedIPService.isIPWhitelisted(ip);
        Map<String, Object> result = Map.of(
                "ipAddress", ip,
                "isWhitelisted", isWhitelisted
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Data
    public static class AddWhitelistRequest {
        private String ipAddress;
        private String description;
    }
}
