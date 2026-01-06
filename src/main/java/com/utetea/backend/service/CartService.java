package com.utetea.backend.service;

import com.utetea.backend.dto.AddToCartRequest;
import com.utetea.backend.dto.CartDto;
import com.utetea.backend.dto.CartItemDto;
import com.utetea.backend.dto.DrinkToppingDto;
import com.utetea.backend.dto.ReorderResponse;
import com.utetea.backend.dto.UpdateCartItemRequest;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utetea.backend.util.RequestContextUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final OrderRepository orderRepository;
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
            userMonitoringService.logCartAddItem(userId, drink.getName(), request.getQuantity(), 
                RequestContextUtil.getCurrentRequest());
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
            userMonitoringService.logCartRemoveItem(userId, productName, 
                RequestContextUtil.getCurrentRequest());
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
    
    /**
     * Đặt lại đơn hàng - Load lại các món từ đơn hàng cũ vào giỏ hàng
     * Kiểm tra món/topping còn bán không, nếu không thì gợi ý thay thế
     */
    @Transactional
    public ReorderResponse reorderFromHistory(Long userId, Long orderId) {
        log.info("Reordering from order {} for user {}", orderId, userId);
        
        // Lấy đơn hàng cũ
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        // Verify user owns this order
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền đặt lại đơn hàng này");
        }
        
        // Fetch toppings
        orderRepository.findOrderItemsWithToppingsByOrderId(orderId);
        
        List<ReorderResponse.ReorderItemStatus> itemStatuses = new ArrayList<>();
        boolean hasUnavailableItems = false;
        int addedCount = 0;
        
        for (OrderItem orderItem : order.getItems()) {
            ReorderResponse.ReorderItemStatus itemStatus = processReorderItem(userId, orderItem);
            itemStatuses.add(itemStatus);
            
            if (!itemStatus.isAddedToCart()) {
                hasUnavailableItems = true;
            } else {
                addedCount++;
            }
        }
        
        CartDto cart = getCart(userId);
        
        String message;
        if (hasUnavailableItems) {
            message = String.format("Đã thêm %d/%d món vào giỏ hàng. Một số món không còn bán.", 
                    addedCount, order.getItems().size());
        } else {
            message = "Đã thêm tất cả món vào giỏ hàng thành công!";
        }
        
        log.info("Reorder completed: {} items added, hasUnavailable={}", addedCount, hasUnavailableItems);
        
        return ReorderResponse.builder()
                .cart(cart)
                .itemStatuses(itemStatuses)
                .hasUnavailableItems(hasUnavailableItems)
                .message(message)
                .build();
    }
    
    private ReorderResponse.ReorderItemStatus processReorderItem(Long userId, OrderItem orderItem) {
        String drinkName = orderItem.getDrinkNameSnapshot();
        String sizeName = orderItem.getSizeNameSnapshot();
        
        ReorderResponse.ReorderItemStatus.ReorderItemStatusBuilder statusBuilder = 
                ReorderResponse.ReorderItemStatus.builder()
                        .drinkName(drinkName)
                        .sizeName(sizeName);
        
        // Tìm drink theo ID từ order item
        Drink drink = orderItem.getDrink();
        if (drink == null || !drink.getIsActive()) {
            // Drink không còn bán - tìm gợi ý thay thế
            statusBuilder.drinkAvailable(false)
                    .sizeAvailable(false)
                    .addedToCart(false)
                    .reason("Món '" + drinkName + "' hiện không còn bán");
            
            // Tìm món tương tự trong cùng category
            List<ReorderResponse.SuggestionDto> suggestions = findSimilarDrinks(drink, drinkName);
            statusBuilder.suggestions(suggestions);
            
            return statusBuilder.build();
        }
        
        statusBuilder.drinkAvailable(true);
        
        // Kiểm tra size
        Long sizeId = null;
        boolean sizeAvailable = true;
        if (sizeName != null && !sizeName.isEmpty()) {
            List<DrinkSize> sizes = drinkSizeRepository.findByDrinkId(drink.getId());
            Optional<DrinkSize> matchingSize = sizes.stream()
                    .filter(s -> s.getSizeName().equals(sizeName))
                    .findFirst();
            
            if (matchingSize.isPresent()) {
                sizeId = matchingSize.get().getId();
            } else {
                sizeAvailable = false;
                // Lấy size mặc định nếu có
                if (!sizes.isEmpty()) {
                    sizeId = sizes.get(0).getId();
                }
            }
        }
        statusBuilder.sizeAvailable(sizeAvailable);
        
        // Kiểm tra toppings
        List<Long> availableToppingIds = new ArrayList<>();
        List<ReorderResponse.ToppingStatus> toppingStatuses = new ArrayList<>();
        
        if (orderItem.getToppings() != null) {
            for (OrderItemTopping orderTopping : orderItem.getToppings()) {
                String toppingName = orderTopping.getToppingNameSnapshot();
                
                // Tìm topping theo tên trong drink hiện tại (bao gồm cả topping chung)
                List<DrinkTopping> drinkToppings = drinkToppingRepository.findByDrinkIdOrDrinkIdIsNullAndIsActiveTrue(drink.getId());
                Optional<DrinkTopping> matchingTopping = drinkToppings.stream()
                        .filter(t -> t.getToppingName().equals(toppingName))
                        .findFirst();
                
                if (matchingTopping.isPresent()) {
                    availableToppingIds.add(matchingTopping.get().getId());
                    toppingStatuses.add(ReorderResponse.ToppingStatus.builder()
                            .toppingName(toppingName)
                            .available(true)
                            .build());
                } else {
                    toppingStatuses.add(ReorderResponse.ToppingStatus.builder()
                            .toppingName(toppingName)
                            .available(false)
                            .build());
                }
            }
        }
        statusBuilder.toppingStatuses(toppingStatuses);
        
        // Thêm vào giỏ hàng
        try {
            AddToCartRequest addRequest = new AddToCartRequest();
            addRequest.setDrinkId(drink.getId());
            addRequest.setSizeId(sizeId);
            addRequest.setQuantity(orderItem.getQuantity());
            addRequest.setToppingIds(availableToppingIds.isEmpty() ? null : availableToppingIds);
            addRequest.setNote(orderItem.getNote());
            
            addToCart(userId, addRequest);
            
            statusBuilder.addedToCart(true);
            
            // Tạo reason nếu có thay đổi
            StringBuilder reason = new StringBuilder();
            if (!sizeAvailable) {
                reason.append("Size '").append(sizeName).append("' không còn, đã chọn size khác. ");
            }
            long unavailableToppings = toppingStatuses.stream().filter(t -> !t.isAvailable()).count();
            if (unavailableToppings > 0) {
                reason.append(unavailableToppings).append(" topping không còn bán.");
            }
            
            if (reason.length() > 0) {
                statusBuilder.reason(reason.toString().trim());
            }
            
        } catch (Exception e) {
            log.error("Failed to add item to cart during reorder", e);
            statusBuilder.addedToCart(false)
                    .reason("Không thể thêm vào giỏ hàng: " + e.getMessage());
        }
        
        return statusBuilder.build();
    }
    
    private List<ReorderResponse.SuggestionDto> findSimilarDrinks(Drink originalDrink, String drinkName) {
        List<ReorderResponse.SuggestionDto> suggestions = new ArrayList<>();
        
        try {
            // Tìm trong cùng category nếu có
            if (originalDrink != null && originalDrink.getCategory() != null) {
                List<Drink> sameCategoryDrinks = drinkRepository
                        .findByCategoryIdAndIsActiveTrue(originalDrink.getCategory().getId());
                
                for (Drink d : sameCategoryDrinks) {
                    if (suggestions.size() >= 3) break;
                    suggestions.add(ReorderResponse.SuggestionDto.builder()
                            .drinkId(d.getId())
                            .drinkName(d.getName())
                            .drinkImage(d.getImageUrl())
                            .basePrice(d.getBasePrice().doubleValue())
                            .reason("Cùng danh mục")
                            .build());
                }
            }
            
            // Nếu chưa đủ, tìm theo tên tương tự
            if (suggestions.size() < 3 && drinkName != null) {
                String[] keywords = drinkName.split(" ");
                for (String keyword : keywords) {
                    if (keyword.length() < 2) continue;
                    
                    List<Drink> similarDrinks = drinkRepository.searchByName(keyword);
                    for (Drink d : similarDrinks) {
                        if (suggestions.size() >= 3) break;
                        // Tránh trùng lặp
                        boolean alreadyAdded = suggestions.stream()
                                .anyMatch(s -> s.getDrinkId().equals(d.getId()));
                        if (!alreadyAdded) {
                            suggestions.add(ReorderResponse.SuggestionDto.builder()
                                    .drinkId(d.getId())
                                    .drinkName(d.getName())
                                    .drinkImage(d.getImageUrl())
                                    .basePrice(d.getBasePrice().doubleValue())
                                    .reason("Tên tương tự")
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding similar drinks", e);
        }
        
        return suggestions;
    }
}
