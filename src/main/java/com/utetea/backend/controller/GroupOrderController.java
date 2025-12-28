package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.GroupOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-orders")
@RequiredArgsConstructor
@Tag(name = "Group Order", description = "API đặt hàng nhóm")
public class GroupOrderController {
    
    private final GroupOrderService groupOrderService;
    
    @PostMapping
    @Operation(summary = "Tạo phiên đặt hàng nhóm mới")
    public ResponseEntity<ApiResponse<GroupOrderDto>> createGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateGroupOrderRequest request) {
        GroupOrderDto result = groupOrderService.createGroupOrder(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiên đặt hàng nhóm thành công", result));
    }
    
    @PostMapping("/join")
    @Operation(summary = "Tham gia phiên đặt hàng nhóm bằng mã mời")
    public ResponseEntity<ApiResponse<GroupOrderDto>> joinGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinGroupOrderRequest request) {
        GroupOrderDto result = groupOrderService.joinGroupOrder(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Tham gia phiên thành công", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phiên đặt hàng nhóm")
    public ResponseEntity<ApiResponse<GroupOrderDto>> getGroupOrder(@PathVariable Long id) {
        GroupOrderDto result = groupOrderService.getGroupOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/code/{inviteCode}")
    @Operation(summary = "Lấy thông tin phiên theo mã mời")
    public ResponseEntity<ApiResponse<GroupOrderDto>> getGroupOrderByCode(
            @PathVariable String inviteCode) {
        GroupOrderDto result = groupOrderService.getGroupOrderByInviteCode(inviteCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/active")
    @Operation(summary = "Lấy danh sách phiên đang hoạt động của user")
    public ResponseEntity<ApiResponse<List<GroupOrderDto>>> getActiveGroupOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<GroupOrderDto> result = groupOrderService.getActiveGroupOrders(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/my-orders")
    @Operation(summary = "Lấy lịch sử phiên đặt hàng nhóm của user")
    public ResponseEntity<ApiResponse<List<GroupOrderDto>>> getMyGroupOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<GroupOrderDto> result = groupOrderService.getUserGroupOrders(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin phiên (chỉ host)")
    public ResponseEntity<ApiResponse<GroupOrderDto>> updateGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupOrderRequest request) {
        GroupOrderDto result = groupOrderService.updateGroupOrder(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", result));
    }
    
    @PostMapping("/{id}/items")
    @Operation(summary = "Thêm món vào phiên đặt hàng nhóm")
    public ResponseEntity<ApiResponse<GroupOrderDto>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddGroupOrderItemRequest request) {
        GroupOrderDto result = groupOrderService.addItem(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm món thành công", result));
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "Cập nhật món trong phiên")
    public ResponseEntity<ApiResponse<GroupOrderDto>> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody AddGroupOrderItemRequest request) {
        GroupOrderDto result = groupOrderService.updateItem(userDetails.getUsername(), id, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật món thành công", result));
    }
    
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Xóa món khỏi phiên")
    public ResponseEntity<ApiResponse<GroupOrderDto>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long itemId) {
        GroupOrderDto result = groupOrderService.removeItem(userDetails.getUsername(), id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Xóa món thành công", result));
    }
    
    @PostMapping("/{id}/lock")
    @Operation(summary = "Khóa phiên (chỉ host) - không cho thêm thành viên/món")
    public ResponseEntity<ApiResponse<GroupOrderDto>> lockGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        GroupOrderDto result = groupOrderService.lockGroupOrder(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa phiên", result));
    }
    
    @PostMapping("/{id}/unlock")
    @Operation(summary = "Mở khóa phiên (chỉ host)")
    public ResponseEntity<ApiResponse<GroupOrderDto>> unlockGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        GroupOrderDto result = groupOrderService.unlockGroupOrder(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa phiên", result));
    }
    
    @PostMapping("/{id}/leave")
    @Operation(summary = "Rời khỏi phiên (không phải host)")
    public ResponseEntity<ApiResponse<GroupOrderDto>> leaveGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        GroupOrderDto result = groupOrderService.leaveGroupOrder(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã rời khỏi phiên", result));
    }
    
    @PostMapping("/{id}/checkout")
    @Operation(summary = "Thanh toán đơn hàng nhóm (chỉ host)")
    public ResponseEntity<ApiResponse<OrderDto>> checkoutGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CheckoutGroupOrderRequest request) {
        OrderDto result = groupOrderService.checkoutGroupOrder(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Đặt hàng thành công", result));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Hủy phiên đặt hàng nhóm (chỉ host)")
    public ResponseEntity<ApiResponse<Void>> cancelGroupOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        groupOrderService.cancelGroupOrder(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy phiên", null));
    }
}
