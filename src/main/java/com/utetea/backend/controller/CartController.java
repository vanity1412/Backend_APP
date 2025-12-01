package com.utetea.backend.controller;

import com.utetea.backend.dto.AddToCartRequest;
import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.CartDto;
import com.utetea.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🛒 Cart", description = "Quản lý giỏ hàng")
@Slf4j
public class CartController {
    
    private final CartService cartService;
    
    @PostMapping("/add")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(
            @RequestParam Long userId,
            @RequestBody AddToCartRequest request) {
        log.info("Adding to cart: userId={}, drinkId={}", userId, request.getDrinkId());
        CartDto cart = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào giỏ hàng", cart));
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Lấy giỏ hàng của user")
    public ResponseEntity<ApiResponse<CartDto>> getCart(@PathVariable Long userId) {
        CartDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }
    
    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @RequestParam Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        CartDto cart = cartService.updateCartItemQuantity(userId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật giỏ hàng", cart));
    }
    
    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @RequestParam Long userId,
            @PathVariable Long cartItemId) {
        cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa khỏi giỏ hàng", null));
    }
    
    @DeleteMapping("/{userId}/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Đã xóa giỏ hàng", null));
    }
}
