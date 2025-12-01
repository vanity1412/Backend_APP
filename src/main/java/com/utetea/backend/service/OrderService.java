package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    private final PromotionRepository promotionRepository;
    
    @Transactional
    public OrderDto createOrder(String username, OrderRequest request) {
        log.info("Creating order for user: {}, store: {}", username, request.getStoreId());
        
        // Validate user
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        if (!user.getActive()) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }
        
        // Validate store
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));
        
        Order order = new Order();
        order.setUser(user);
        order.setStore(store);
        order.setType(request.getType());
        order.setAddress(request.getAddress());
        order.setPickupTime(request.getPickupTime());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        
        // Validate items not empty
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }
        
        for (OrderItemRequest itemReq : request.getItems()) {
            Drink drink = drinkRepository.findById(itemReq.getDrinkId())
                .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", itemReq.getDrinkId()));
            
            if (!drink.getIsActive()) {
                throw new BusinessException("Drink '" + drink.getName() + "' is not available");
            }
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setDrink(drink);
            item.setDrinkNameSnapshot(drink.getName());
            item.setSizeNameSnapshot(itemReq.getSizeName());
            item.setQuantity(itemReq.getQuantity());
            item.setNote(itemReq.getNote());
            
            BigDecimal itemPrice = drink.getBasePrice();
            
            // Add size price
            List<DrinkSize> sizes = drinkSizeRepository.findByDrinkId(drink.getId());
            boolean sizeFound = false;
            for (DrinkSize size : sizes) {
                if (size.getSizeName().equals(itemReq.getSizeName())) {
                    itemPrice = itemPrice.add(size.getExtraPrice());
                    sizeFound = true;
                    break;
                }
            }
            
            if (!sizeFound) {
                throw new BusinessException("Size '" + itemReq.getSizeName() + "' not available for drink '" + drink.getName() + "'");
            }
            
            // Add toppings
            if (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty()) {
                List<OrderItemTopping> toppings = new ArrayList<>();
                for (Long toppingId : itemReq.getToppingIds()) {
                    DrinkTopping topping = drinkToppingRepository.findById(toppingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Topping", "id", toppingId));
                    
                    if (!topping.getIsActive()) {
                        throw new BusinessException("Topping '" + topping.getToppingName() + "' is not available");
                    }
                    
                    OrderItemTopping orderTopping = new OrderItemTopping();
                    orderTopping.setOrderItem(item);
                    orderTopping.setToppingNameSnapshot(topping.getToppingName());
                    orderTopping.setPriceSnapshot(topping.getPrice());
                    toppings.add(orderTopping);
                    
                    itemPrice = itemPrice.add(topping.getPrice());
                }
                item.setToppings(toppings);
            }
            
            itemPrice = itemPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            item.setItemPrice(itemPrice);
            items.add(item);
            
            totalPrice = totalPrice.add(itemPrice);
        }
        
        order.setItems(items);
        order.setTotalPrice(totalPrice);
        
        // Apply promotion
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getPromotionCode() != null && !request.getPromotionCode().isEmpty()) {
            Promotion promotion = promotionRepository.findByCode(request.getPromotionCode())
                .orElseThrow(() -> new BusinessException("Invalid promotion code"));
            
            LocalDateTime now = LocalDateTime.now();
            if (!promotion.getIsActive()) {
                throw new BusinessException("Promotion code is inactive");
            }
            
            if (promotion.getStartDate().isAfter(now)) {
                throw new BusinessException("Promotion has not started yet");
            }
            
            if (promotion.getEndDate().isBefore(now)) {
                throw new BusinessException("Promotion has expired");
            }
            
            if (totalPrice.compareTo(promotion.getMinOrderValue()) < 0) {
                throw new BusinessException(String.format(
                    "Minimum order amount is %s VND for this promotion", 
                    promotion.getMinOrderValue()));
            }
            
            if (promotion.getDiscountType() == DiscountType.PERCENT) {
                discount = totalPrice.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
            } else {
                discount = promotion.getDiscountValue();
            }
            
            order.setPromotion(promotion);
            log.info("Applied promotion: {} with discount: {}", promotion.getCode(), discount);
        }
        
        order.setDiscount(discount);
        order.setFinalPrice(totalPrice.subtract(discount));
        
        order = orderRepository.save(order);
        log.info("Order created successfully with id: {}, final price: {}", order.getId(), order.getFinalPrice());
        return mapToDto(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<OrderDto> getUserCurrentOrders(Long userId) {
        return orderRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, OrderStatus.DONE).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToDto(order);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(this::mapToDto);
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);
        
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        
        log.info("Order {} status updated successfully", orderId);
        return mapToDto(order);
    }
    
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // PENDING can go to MAKING or CANCELED
        if (currentStatus == OrderStatus.PENDING) {
            if (newStatus != OrderStatus.MAKING && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order can only be moved to MAKING or CANCELED from PENDING");
            }
        }
        // MAKING can go to SHIPPING or CANCELED
        else if (currentStatus == OrderStatus.MAKING) {
            if (newStatus != OrderStatus.SHIPPING && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order can only be moved to SHIPPING or CANCELED from MAKING");
            }
        }
        // SHIPPING can go to DONE or CANCELED
        else if (currentStatus == OrderStatus.SHIPPING) {
            if (newStatus != OrderStatus.DONE && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order can only be moved to DONE or CANCELED from SHIPPING");
            }
        }
        // DONE and CANCELED are final states
        else if (currentStatus == OrderStatus.DONE || currentStatus == OrderStatus.CANCELED) {
            throw new BusinessException("Cannot change status of completed or canceled order");
        }
    }
    
    private OrderDto mapToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFullName());
        dto.setStoreId(order.getStore().getId());
        dto.setStoreName(order.getStore().getStoreName());
        dto.setType(order.getType());
        dto.setAddress(order.getAddress());
        dto.setPickupTime(order.getPickupTime());
        dto.setStatus(order.getStatus());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setDiscount(order.getDiscount());
        dto.setFinalPrice(order.getFinalPrice());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPromotionCode(order.getPromotion() != null ? order.getPromotion().getCode() : null);
        dto.setCreatedAt(order.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        dto.setUpdatedAt(order.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        
        List<OrderItemDto> itemDtos = order.getItems().stream()
            .map(this::mapItemToDto)
            .collect(Collectors.toList());
        dto.setItems(itemDtos);
        
        return dto;
    }
    
    private OrderItemDto mapItemToDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setDrinkName(item.getDrinkNameSnapshot());
        dto.setSizeName(item.getSizeNameSnapshot());
        dto.setQuantity(item.getQuantity());
        dto.setItemPrice(item.getItemPrice());
        dto.setNote(item.getNote());
        
        List<OrderItemToppingDto> toppingDtos = item.getToppings().stream()
            .map(t -> new OrderItemToppingDto(t.getId(), t.getToppingNameSnapshot(), t.getPriceSnapshot()))
            .collect(Collectors.toList());
        dto.setToppings(toppingDtos);
        
        return dto;
    }
}
