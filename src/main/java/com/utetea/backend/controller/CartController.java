package com.utetea.backend.controller;

import com.utetea.backend.dto.AddToCartRequest;
import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.CartDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🛒 Cart", description = "Quản lý giỏ hàng")
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'MANAGER')")
public class CartController {
    
    private final CartService cartService;
    private final UserRepository userRepository;
    
    /**
     * Lấy User từ Authentication và verify quyền truy cập
     */
    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
    
    /**
     * Verify user có quyền truy cập resource của userId không
     */
    private void verifyUserAccess(Authentication authentication, Long userId) {
        User currentUser = getAuthenticatedUser(authentication);
        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập giỏ hàng của người khác");
        }
    }
    
    @PostMapping("/add")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        User user = getAuthenticatedUser(authentication);
        log.info("Adding to cart: userId={}, drinkId={}", user.getId(), request.getDrinkId());
        CartDto cart = cartService.addToCart(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào giỏ hàng", cart));
    }
    
    @GetMapping
    @Operation(summary = "Lấy giỏ hàng của user hiện tại")
    public ResponseEntity<ApiResponse<CartDto>> getMyCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        CartDto cart = cartService.getCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Lấy giỏ hàng của user (cần verify quyền)")
    public ResponseEntity<ApiResponse<CartDto>> getCart(
            Authentication authentication,
            @PathVariable Long userId) {
        verifyUserAccess(authentication, userId);
        CartDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }
    
    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        User user = getAuthenticatedUser(authentication);
        CartDto cart = cartService.updateCartItemQuantity(user.getId(), cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật giỏ hàng", cart));
    }
    
    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId) {
        User user = getAuthenticatedUser(authentication);
        cartService.removeCartItem(user.getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa khỏi giỏ hàng", null));
    }
    
    @DeleteMapping("/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng của user hiện tại")
    public ResponseEntity<ApiResponse<Void>> clearMyCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa giỏ hàng", null));
    }
    
    @DeleteMapping("/{userId}/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng (cần verify quyền)")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            Authentication authentication,
            @PathVariable Long userId) {
        verifyUserAccess(authentication, userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa giỏ hàng", null));
    }
}
