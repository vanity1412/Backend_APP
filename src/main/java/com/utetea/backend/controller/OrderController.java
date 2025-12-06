package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.OrderRequest;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'MANAGER')")
public class OrderController {
    
    private final OrderService orderService;
    private final UserRepository userRepository;
    
    /**
     * Lấy User từ Authentication
     */
    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
    
    /**
     * Verify user có quyền truy cập resource của userId không
     * Manager có thể xem tất cả, User chỉ xem của mình
     */
    private void verifyUserAccessOrManager(Authentication authentication, Long userId) {
        User currentUser = getAuthenticatedUser(authentication);
        boolean isManager = currentUser.getRole() == UserRole.MANAGER;
        boolean isOwner = currentUser.getId().equals(userId);
        
        if (!isManager && !isOwner) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng của người khác");
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request) {
        String username = authentication.getName();
        OrderDto order = orderService.createOrder(username, request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }
    
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<OrderDto> orders = orderService.getUserOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/my/current")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyCurrentOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<OrderDto> orders = orderService.getUserCurrentOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders(
            Authentication authentication,
            @PathVariable Long userId) {
        verifyUserAccessOrManager(authentication, userId);
        List<OrderDto> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/user/{userId}/current")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserCurrentOrders(
            Authentication authentication,
            @PathVariable Long userId) {
        verifyUserAccessOrManager(authentication, userId);
        List<OrderDto> orders = orderService.getUserCurrentOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            Authentication authentication,
            @PathVariable Long orderId) {
        OrderDto order = orderService.getOrderById(orderId);
        // Verify user owns this order or is manager
        verifyUserAccessOrManager(authentication, order.getUserId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderDto> orders = orderService.getAllOrders(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        OrderDto order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
}
