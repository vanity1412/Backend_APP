package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "👔 Manager", description = "API quản lý cho Manager")
@Slf4j
public class ManagerController {
    
    private final ManagerService managerService;
    
    @GetMapping("/test")
    @Operation(summary = "Test Auth", description = "Test authentication và role")
    public ResponseEntity<ApiResponse<String>> testAuth() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info("========== TEST AUTH ==========");
        log.info("Authenticated: " + (auth != null));
        if (auth != null) {
            log.info("Principal: " + auth.getPrincipal());
            log.info("Authorities: " + auth.getAuthorities());
        }
        
        String message = auth != null ? 
            "Authenticated as: " + auth.getName() + ", Authorities: " + auth.getAuthorities() :
            "Not authenticated";
        
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Dashboard Summary", description = "Lấy tổng quan doanh thu, đơn hàng")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        log.info("========== GET /api/manager/summary ==========");
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info("User authenticated: " + auth);
        log.info("Authorities: " + auth.getAuthorities());
        
        DashboardSummaryDto summary = managerService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard loaded", summary));
    }
    
    @GetMapping("/orders")
    @Operation(summary = "Get Orders", description = "Lấy danh sách đơn hàng theo status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/orders - status: {}, page: {}", status, page);
        
        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
            }
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = managerService.getOrdersByStatus(orderStatus, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/orders/{orderId}/status")
    @Operation(summary = "Update Order Status", description = "Cập nhật trạng thái đơn hàng")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        
        log.info("PUT /api/manager/orders/{}/status - new status: {}", orderId, status);
        
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status: " + status));
        }
        
        OrderDto updatedOrder = managerService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updatedOrder));
    }
    
    // ==================== USER MANAGEMENT ====================
    
    @GetMapping("/users")
    @Operation(summary = "Get All Users", description = "Lấy danh sách tất cả người dùng")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.UserDto>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/users - role: {}, page: {}", role, page);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.UserDto> users = managerService.getAllUsers(role, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get User Detail", description = "Lấy thông tin chi tiết người dùng")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> getUserById(
            @PathVariable Long userId) {
        
        log.info("GET /api/manager/users/{}", userId);
        
        com.utetea.backend.dto.UserDto user = managerService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PutMapping("/users/{userId}/block")
    @Operation(summary = "Block/Unblock User", description = "Khóa hoặc mở khóa tài khoản người dùng")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> toggleUserBlock(
            @PathVariable Long userId,
            @RequestParam boolean blocked) {
        
        log.info("PUT /api/manager/users/{}/block - blocked: {}", userId, blocked);
        
        com.utetea.backend.dto.UserDto user = managerService.toggleUserBlock(userId, blocked);
        String message = blocked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
        return ResponseEntity.ok(ApiResponse.success(message, user));
    }
    
    @GetMapping("/users/search")
    @Operation(summary = "Search Users", description = "Tìm kiếm người dùng theo từ khóa")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.UserDto>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/users/search - keyword: {}", keyword);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.UserDto> users = managerService.searchUsers(keyword, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
