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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final SpinRewardRepository spinRewardRepository;

    // Services for Notifications & Logic
    private final EmailService emailService;
    private final OrderWebSocketService orderWebSocketService;
    private final OneSignalService oneSignalService;
    private final MemberTierService memberTierService;

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

        // Xử lý address
        if (request.getType() == OrderType.PICKUP) {
            order.setAddress("Tại Cửa Hàng");
        } else if (request.getType() == OrderType.DELIVERY) {
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                throw new BusinessException("Địa chỉ giao hàng không được để trống");
            }
            order.setAddress(request.getAddress());
        }

        order.setPickupTime(request.getPickupTime());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalPrice = BigDecimal.ZERO;
        Set<OrderItem> items = new HashSet<>();

        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BusinessException("Số lượng phải lớn hơn 0");
            }
            if (itemReq.getQuantity() > 100) {
                throw new BusinessException("Số lượng tối đa là 100");
            }

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
            if (!sizes.isEmpty()) {
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
            }

            // Add toppings
            if (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty()) {
                Set<OrderItemTopping> toppings = new HashSet<>();
                for (Long toppingId : itemReq.getToppingIds()) {
                    DrinkTopping topping = drinkToppingRepository.findByIdWithDrink(toppingId)
                            .orElseThrow(() -> new ResourceNotFoundException("Topping", "id", toppingId));

                    if (topping.getDrink() != null && !topping.getDrink().getId().equals(drink.getId())) {
                        throw new BusinessException("Topping '" + topping.getToppingName() + "' không thuộc về drink '" + drink.getName() + "'");
                    }

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

        // Apply promotion or spin voucher
        BigDecimal discount = BigDecimal.ZERO;

        if (request.getSpinVoucherCode() != null && !request.getSpinVoucherCode().isEmpty()) {
            // [LOGIC MỚI] Sử dụng PESSIMISTIC_WRITE lock để tránh race condition
            SpinReward spinReward = spinRewardRepository.findByVoucherCodeForUpdate(request.getSpinVoucherCode().toUpperCase())
                    .orElseThrow(() -> new BusinessException("Mã voucher spin không hợp lệ hoặc đã được sử dụng"));

            // Double-check isUsed sau khi có lock
            if (spinReward.getIsUsed()) {
                throw new BusinessException("Mã voucher spin đã được sử dụng");
            }

            // Tính discount từ spin voucher (percent)
            discount = totalPrice.multiply(BigDecimal.valueOf(spinReward.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100));

            // Đánh dấu voucher đã sử dụng và flush ngay lập tức
            spinReward.setIsUsed(true);
            spinRewardRepository.saveAndFlush(spinReward);

            log.info("Applied and marked spin voucher as used: {} with {}% discount = {}",
                    spinReward.getVoucherCode(), spinReward.getDiscountPercent(), discount);
        }
        else if (request.getPromotionCode() != null && !request.getPromotionCode().isEmpty()) {
            Promotion promotion = promotionRepository.findByCodeForUpdate(request.getPromotionCode())
                    .orElseThrow(() -> new BusinessException("Mã voucher không hợp lệ"));

            LocalDateTime now = LocalDateTime.now();
            if (!promotion.getIsActive()) throw new BusinessException("Mã voucher đã bị vô hiệu hóa");
            if (promotion.getStartDate().isAfter(now)) throw new BusinessException("Mã voucher chưa có hiệu lực");
            if (promotion.getEndDate().isBefore(now)) throw new BusinessException("Mã voucher đã hết hạn");
            if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
                throw new BusinessException("Mã voucher đã hết lượt sử dụng");
            }

            if (totalPrice.compareTo(promotion.getMinOrderValue()) < 0) {
                throw new BusinessException(String.format("Giá trị đơn hàng tối thiểu là %s VND", promotion.getMinOrderValue()));
            }

            if (promotion.getDiscountType() == DiscountType.PERCENT) {
                discount = totalPrice.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discount = promotion.getMaxDiscountAmount();
                }
            } else {
                discount = promotion.getDiscountValue();
            }

            promotion.setUsedCount(promotion.getUsedCount() + 1);
            promotionRepository.save(promotion);
            order.setPromotion(promotion);
            log.info("Applied promotion: {}", promotion.getCode());
        }

        // [LOGIC MỚI] Áp dụng thêm discount theo Member Tier (cộng dồn với voucher)
        log.info("User {} has tier: {}, calculating tier discount for total: {}",
                username, user.getMemberTier(), totalPrice);
        BigDecimal tierDiscount = memberTierService.calculateTierDiscount(user.getMemberTier(), totalPrice);

        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.add(tierDiscount);
            log.info("Applied tier {} discount: {} for user {}, total discount now: {}",
                    user.getMemberTier(), tierDiscount, username, discount);
        }

        order.setDiscount(discount);
        order.setFinalPrice(totalPrice.subtract(discount));

        order = orderRepository.save(order);
        log.info("Order created successfully with id: {}", order.getId());

        OrderDto orderDto = mapToDto(order);

        // WebSocket Notification
        try {
            orderWebSocketService.notifyNewOrder(orderDto);
            orderWebSocketService.notifyNewOrderToStore(orderDto, store.getId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }

        // Email Notification
        try {
            emailService.sendOrderConfirmationEmail(order);
        } catch (Exception e) {
            log.error("Failed to send order email", e);
        }

        // [OneSignal] Notification cho Manager
        sendNotificationToManagers(order);

        return orderDto;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(Long userId) {
        return orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(userId).stream()
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
        Order order = orderRepository.findByIdWithItems(orderId)
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

        validateStatusTransition(order.getStatus(), newStatus);

        Long userId = order.getUser().getId();
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        orderRepository.flush();

        OrderDto orderDto = mapToDto(order);

        // WebSocket
        try {
            orderWebSocketService.notifyOrderStatusUpdate(orderDto);
        } catch (Exception e) {
            log.error("Failed to send WebSocket status update", e);
        }

        // Loyalty Points & Email (nếu DONE)
        if (newStatus == OrderStatus.DONE) {
            try {
                // [LOGIC MỚI] Lấy user để tính điểm theo tier multiplier
                User user = order.getUser();
                int basePoints = 1;
                int earnedPoints = memberTierService.calculatePointsEarned(user.getMemberTier(), basePoints);

                // Sử dụng native update query để tránh lỗi Hibernate
                int updated = userRepository.addPoints(userId, earnedPoints);
                if (updated > 0) {
                    log.info("Added {} loyalty points (base: {}, tier: {}) for userId {}",
                            earnedPoints, basePoints, user.getMemberTier(), userId);

                    // Kiểm tra và nâng cấp tier nếu đủ điểm
                    memberTierService.checkAndUpgradeTierByUserId(userId);
                }
            } catch (Exception e) {
                log.error("Failed to add loyalty points", e);
            }

            try {
                emailService.sendOrderCompletionEmail(order);
            } catch (Exception e) {
                log.error("Failed to send completion email", e);
            }
        }

        // [OneSignal] Notification cho User sở hữu đơn
        sendNotificationToUser(order, newStatus);

        return orderDto;
    }

    /**
     * Gửi thông báo OneSignal cho tất cả Manager khi có đơn mới
     */
    private void sendNotificationToManagers(Order order) {
        try {
            // Tìm tất cả user có quyền MANAGER
            List<User> managers = userRepository.findByRole(UserRole.MANAGER);

            if (managers.isEmpty()) return;

            // Lấy danh sách ID
            String[] managerIds = managers.stream()
                    .map(user -> String.valueOf(user.getId()))
                    .toArray(String[]::new);

            String title = "🔔 Đơn hàng mới #" + order.getId();
            String content = "Khách hàng " + order.getUser().getFullName() +
                    " vừa đặt đơn trị giá " + order.getFinalPrice() + "đ.";

            // Gửi push notification
            oneSignalService.sendToMultipleUsers(managerIds, title, content, NotificationType.ORDER_NEW, order.getId());
            log.info("Sent push notification to {} managers", managerIds.length);

        } catch (Exception e) {
            log.error("Failed to send push notification to managers", e);
        }
    }

    /**
     * Gửi thông báo OneSignal cho User khi trạng thái đơn hàng thay đổi
     */
    private void sendNotificationToUser(Order order, OrderStatus status) {
        try {
            String userId = String.valueOf(order.getUser().getId());
            String title = "Cập nhật đơn hàng #" + order.getId();
            String content = "";

            switch (status) {
                case MAKING:
                    content = "Quán đang pha chế đồ uống cho bạn.";
                    break;
                case SHIPPING:
                    content = "Shipper đang giao trà sữa đến cho bạn!";
                    break;
                case READY:
                    content = "Đồ uống đã sẵn sàng tại quầy để bạn lấy.";
                    break;
                case DONE:
                    content = "Đơn hàng hoàn tất. Chúc bạn ngon miệng!";
                    break;
                case CANCELED:
                    content = "Đơn hàng của bạn đã bị hủy.";
                    break;
                default:
                    // Không gửi thông báo cho PENDING hoặc các trạng thái khác
                    return;
            }

            // Gửi push notification
            oneSignalService.sendToUser(userId, title, content, NotificationType.ORDER_STATUS, order.getId());
            log.info("Sent push notification to user {}", userId);

        } catch (Exception e) {
            log.error("Failed to send push notification to user", e);
        }
    }

    // ==================================================================================

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.PENDING) {
            if (newStatus != OrderStatus.MAKING && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order transition invalid from PENDING");
            }
        }
        else if (currentStatus == OrderStatus.MAKING) {
            if (newStatus != OrderStatus.SHIPPING &&
                    newStatus != OrderStatus.READY &&
                    newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order transition invalid from MAKING");
            }
        }
        else if (currentStatus == OrderStatus.SHIPPING) {
            if (newStatus != OrderStatus.DONE && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order transition invalid from SHIPPING");
            }
        }
        else if (currentStatus == OrderStatus.READY) {
            if (newStatus != OrderStatus.DONE && newStatus != OrderStatus.CANCELED) {
                throw new BusinessException("Order transition invalid from READY");
            }
        }
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
        if (item.getDrink() != null) {
            dto.setDrinkImage(item.getDrink().getImageUrl());
        }
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