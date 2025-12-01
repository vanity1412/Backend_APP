package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.dto.OrderRequest;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request) {
        String username = authentication.getName();
        OrderDto order = orderService.createOrder(username, request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders(@PathVariable Long userId) {
        List<OrderDto> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/user/{userId}/current")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserCurrentOrders(@PathVariable Long userId) {
        List<OrderDto> orders = orderService.getUserCurrentOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long orderId) {
        OrderDto order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderDto> orders = orderService.getAllOrders(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        OrderDto order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
}
