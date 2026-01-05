package com.utetea.backend.service;

import com.utetea.backend.dto.AddToCartRequest;
import com.utetea.backend.dto.CartDto;
import com.utetea.backend.dto.CartItemDto;
import com.utetea.backend.dto.DrinkToppingDto;
import com.utetea.backend.dto.UpdateCartItemRequest;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    private final OneSignalService oneSignalService;
    private final UserMonitoringService userMonitoringService;
    
    @Transactional
    public CartDto addToCart(Long userId, AddToCartRequest request) {
        // Lấy hoặc tạo cart cho user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
        
        // Lấy thông tin drink
        Drink drink = drinkRepository.findById(request.getDrinkId())
                .orElseThrow(() -> new ResourceNotFoundException("Drink not found"));
        
        // Lấy size (có thể null nếu drink không có size options)
        DrinkSize size = null;
        if (request.getSizeId() != null && request.getSizeId() > 0) {
            size = drinkSizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Size not found"));
        }
        
        // Tính giá
        BigDecimal unitPrice = drink.getBasePrice();
        if (size != null) {
            unitPrice = unitPrice.add(size.getExtraPrice());
        }
        
        // Lấy toppings
        List<DrinkTopping> toppings = new ArrayList<>();
        if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
            toppings = drinkToppingRepository.findAllById(request.getToppingIds());
            for (DrinkTopping topping : toppings) {
                unitPrice = unitPrice.add(topping.getPrice());
            }
        }
        
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        
        // Tạo cart item
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setDrink(drink);
        cartItem.setSize(size);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setUnitPrice(unitPrice.doubleValue());
        cartItem.setTotalPrice(totalPrice.doubleValue());
        cartItem.setToppings(toppings);
        cartItem.setNote(request.getNote());
        
        cart.getItems().add(cartItem);
        cartItemRepository.save(cartItem);
        
        log.info("Added item to cart: userId={}, drinkId={}, quantity={}", userId, request.getDrinkId(), request.getQuantity());
        
        // 🛡️ Log activity - Thêm vào giỏ hàng
        try {
            userMonitoringService.logCartAddItem(userId, drink.getName(), request.getQuantity(), null);
        } catch (Exception e) {
            log.error("Failed to log cart add to monitoring", e);
        }
        
        // ❌ Không gửi thông báo khi thêm vào giỏ hàng (theo yêu cầu)
        
        return getCart(userId);
    }
    
    @Transactional
    public CartDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
        
        return convertToDto(cart);
    }
    
    @Transactional
    public CartDto updateCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }
        
        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItem.setTotalPrice(cartItem.getUnitPrice() * quantity);
            cartItemRepository.save(cartItem);
        }
        
        return getCart(userId);
    }
    
    /**
     * Cập nhật đầy đủ thông tin cart item: số lượng, size, toppings
     */
    @Transactional
    public CartDto updateCartItemFull(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }
        
        Drink drink = cartItem.getDrink();
        
        // Cập nhật size nếu có
        DrinkSize size = null;
        if (request.getSizeId() != null && request.getSizeId() > 0) {
            size = drinkSizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Size not found"));
            cartItem.setSize(size);
        } else if (request.getSizeId() != null && request.getSizeId() == 0) {
            // Nếu sizeId = 0, xóa size
            cartItem.setSize(null);
        }
        
        // Tính lại giá
        BigDecimal unitPrice = drink.getBasePrice();
        if (cartItem.getSize() != null) {
            unitPrice = unitPrice.add(cartItem.getSize().getExtraPrice());
        }
        
        // Cập nhật toppings nếu có
        if (request.getToppingIds() != null) {
            List<DrinkTopping> toppings = new ArrayList<>();
            if (!request.getToppingIds().isEmpty()) {
                toppings = drinkToppingRepository.findAllById(request.getToppingIds());
            }
            cartItem.setToppings(toppings);
            
            // Cộng giá topping
            for (DrinkTopping topping : toppings) {
                unitPrice = unitPrice.add(topping.getPrice());
            }
        } else {
            // Giữ nguyên toppings cũ, cộng giá
            for (DrinkTopping topping : cartItem.getToppings()) {
                unitPrice = unitPrice.add(topping.getPrice());
            }
        }
        
        // Cập nhật số lượng
        cartItem.setQuantity(request.getQuantity());
        cartItem.setUnitPrice(unitPrice.doubleValue());
        cartItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())).doubleValue());
        
        // Cập nhật note nếu có
        if (request.getNote() != null) {
            cartItem.setNote(request.getNote());
        }
        
        cartItemRepository.save(cartItem);
        
        log.info("Updated cart item: userId={}, cartItemId={}, quantity={}, sizeId={}", 
                userId, cartItemId, request.getQuantity(), request.getSizeId());
        
        return getCart(userId);
    }
    
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }
        
        String productName = cartItem.getDrink().getName();
        cartItemRepository.delete(cartItem);
        
        // 🛡️ Log activity - Xóa khỏi giỏ hàng
        try {
            userMonitoringService.logCartRemoveItem(userId, productName, null);
        } catch (Exception e) {
            log.error("Failed to log cart remove to monitoring", e);
        }
    }
    
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        cartItemRepository.deleteByCartId(cart.getId());
    }
    
    private CartDto convertToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);
        
        double totalAmount = itemDtos.stream()
                .mapToDouble(CartItemDto::getTotalPrice)
                .sum();
        dto.setTotalAmount(totalAmount);
        
        return dto;
    }
    
    private CartItemDto convertItemToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setDrinkId(item.getDrink().getId());
        dto.setDrinkName(item.getDrink().getName());
        dto.setDrinkImage(item.getDrink().getImageUrl());
        
        if (item.getSize() != null) {
            dto.setSizeId(item.getSize().getId());
            dto.setSizeName(item.getSize().getSizeName());
        }
        
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setNote(item.getNote());
        
        List<DrinkToppingDto> toppingDtos = item.getToppings().stream()
                .map(t -> new DrinkToppingDto(t.getId(), t.getToppingName(), t.getPrice(), t.getIsActive()))
                .collect(Collectors.toList());
        dto.setToppings(toppingDtos);
        
        return dto;
    }
}
