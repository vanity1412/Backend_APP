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
    private final RateLimitService rateLimitService;
    private final UserMonitoringService userMonitoringService;
    private final ChallengeService challengeService;

    /**
     * Preview bill trước khi thanh toán
     * Tính toán chi tiết đơn hàng, giá tiền, giảm giá để user xem trước
     */
    @Transactional(readOnly = true)
    public BillPreviewDto previewBill(String username, OrderRequest request) {
        log.info("Generating bill preview for user: {}", username);
        
        // Validate request
        validateOrderRequest(request);
        
        // Get user info
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        // Get store info
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));
        
        // Calculate items
        List<BillPreviewDto.BillItemDto> billItems = new java.util.ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (OrderItemRequest itemReq : request.getItems()) {
            Drink drink = drinkRepository.findById(itemReq.getDrinkId())
                    .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", itemReq.getDrinkId()));
            
            if (!drink.getIsActive()) {
                throw new BusinessException("Drink '" + drink.getName() + "' is not available");
            }
            
            BigDecimal unitPrice = drink.getBasePrice();
            
            // Add size price
            List<DrinkSize> sizes = drinkSizeRepository.findByDrinkId(drink.getId());
            if (!sizes.isEmpty()) {
                boolean sizeFound = false;
                for (DrinkSize size : sizes) {
                    if (size.getSizeName().equals(itemReq.getSizeName())) {
                        unitPrice = unitPrice.add(size.getExtraPrice());
                        sizeFound = true;
                        break;
                    }
                }
                if (!sizeFound) {
                    throw new BusinessException("Size '" + itemReq.getSizeName() + "' not available for drink '" + drink.getName() + "'");
                }
            }
            
            // Add toppings
            List<String> toppingNames = new java.util.ArrayList<>();
            if (itemReq.getToppingIds() != null && !itemReq.getToppingIds().isEmpty()) {
                for (Long toppingId : itemReq.getToppingIds()) {
                    DrinkTopping topping = drinkToppingRepository.findByIdWithDrink(toppingId)
                            .orElseThrow(() -> new ResourceNotFoundException("Topping", "id", toppingId));
                    
                    if (topping.getDrink() != null && !topping.getDrink().getId().equals(drink.getId())) {
                        throw new BusinessException("Topping '" + topping.getToppingName() + "' không thuộc về drink '" + drink.getName() + "'");
                    }
                    
                    if (!topping.getIsActive()) {
                        throw new BusinessException("Topping '" + topping.getToppingName() + "' is not available");
                    }
                    
                    unitPrice = unitPrice.add(topping.getPrice());
                    toppingNames.add(topping.getToppingName());
                }
            }
            
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);
            
            billItems.add(BillPreviewDto.BillItemDto.builder()
                    .drinkName(drink.getName())
                    .drinkImage(drink.getImageUrl())
                    .sizeName(itemReq.getSizeName())
                    .toppings(toppingNames)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotal)
                    .note(itemReq.getNote())
                    .build());
        }
        
        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        String promotionCode = null;
        String tierDiscountInfo = null;
        
        // Check spin voucher
        if (request.getSpinVoucherCode() != null && !request.getSpinVoucherCode().isEmpty()) {
            SpinReward spinReward = spinRewardRepository.findByVoucherCode(request.getSpinVoucherCode().toUpperCase())
                    .orElse(null);
            
            if (spinReward != null && !spinReward.getIsUsed()) {
                discount = subtotal.multiply(BigDecimal.valueOf(spinReward.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100));
                promotionCode = "SPIN: " + spinReward.getVoucherCode() + " (-" + spinReward.getDiscountPercent() + "%)";
            }
        }
        // Check promotion code
        else if (request.getPromotionCode() != null && !request.getPromotionCode().isEmpty()) {
            Promotion promotion = promotionRepository.findByCode(request.getPromotionCode())
                    .orElse(null);
            
            if (promotion != null && promotion.getIsActive()) {
                LocalDateTime now = LocalDateTime.now();
                if (!promotion.getStartDate().isAfter(now) && !promotion.getEndDate().isBefore(now)) {
                    if (subtotal.compareTo(promotion.getMinOrderValue()) >= 0) {
                        if (promotion.getDiscountType() == DiscountType.PERCENT) {
                            discount = subtotal.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
                            if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                                discount = promotion.getMaxDiscountAmount();
                            }
                        } else {
                            discount = promotion.getDiscountValue();
                        }
                        promotionCode = promotion.getCode();
                    }
                }
            }
        }
        
        // Calculate tier discount
        BigDecimal tierDiscount = memberTierService.calculateTierDiscount(user.getMemberTier(), subtotal);
        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.add(tierDiscount);
            tierDiscountInfo = user.getMemberTier().name() + " (-" + formatPrice(tierDiscount) + ")";
        }
        
        BigDecimal finalPrice = subtotal.subtract(discount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }
        
        // Build response
        return BillPreviewDto.builder()
                .customerName(user.getFullName())
                .customerPhone(user.getPhone())
                .customerEmail(user.getEmail())
                .storeId(store.getId())
                .storeName(store.getStoreName())
                .storeAddress(store.getAddress())
                .orderType(request.getType().name())
                .deliveryAddress(request.getType() == OrderType.DELIVERY ? request.getAddress() : "Tại Cửa Hàng")
                .pickupTime(request.getPickupTime() != null ? 
                        request.getPickupTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null)
                .paymentMethod(getPaymentMethodDisplayName(request.getPaymentMethod()))
                .items(billItems)
                .subtotal(subtotal)
                .discount(discount)
                .promotionCode(promotionCode)
                .tierDiscount(tierDiscountInfo)
                .finalPrice(finalPrice)
                .build();
    }
    
    private String formatPrice(BigDecimal price) {
        return String.format("%,d VND", price.longValue());
    }
    
    private String getPaymentMethodDisplayName(PaymentMethod paymentMethod) {
        if (paymentMethod == null) return "Khác";
        switch (paymentMethod) {
            case COD: return "Tiền mặt";
            case VNPAY: return "VNPay";
            case VIETQR: return "VietQR";
            case MOMO: return "MoMo";
            case PAYPAL: return "PayPal";
            default: return "Khác";
        }
    }

    @Transactional
    public OrderDto createOrder(String username, OrderRequest request) {
        log.info("Creating order for user: {}, store: {}", username, request.getStoreId());

        // ✅ SECURITY: VALIDATE REQUEST TRƯỚC KHI XỬ LÝ
        validateOrderRequest(request);

        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!user.getActive()) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }
        
        // ✅ SECURITY: Check rate limit (20 đơn/giờ per user)
        rateLimitService.checkOrderRateLimit(user.getId());

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

        // 🛡️ Log activity - Tạo đơn hàng
        try {
            userMonitoringService.logOrderCreate(user.getId(), order.getId(), 
                order.getFinalPrice().doubleValue(), null);
        } catch (Exception e) {
            log.error("Failed to log order creation to monitoring", e);
        }

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

        // 💳 Email thanh toán thành công cho payment online (VNPAY, PAYPAL)
        PaymentMethod pm = order.getPaymentMethod();
        if (pm == PaymentMethod.VNPAY || pm == PaymentMethod.PAYPAL) {
            try {
                emailService.sendPaymentSuccessEmail(order);
                log.info("Payment success email sent for order #{} via {}", order.getId(), pm.name());
            } catch (Exception e) {
                log.error("Failed to send payment success email for order #{}", order.getId(), e);
            }
            
            // Log payment success cho monitoring
            try {
                userMonitoringService.logPaymentSuccess(user.getId(), order.getId(), pm.name(), null);
            } catch (Exception e) {
                log.error("Failed to log payment success to monitoring", e);
            }
        }

        // [OneSignal] Notification cho Manager
        sendNotificationToManagers(order);

        return orderDto;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(Long userId) {
        // Bước 1: Fetch orders với items và drink
        List<Order> orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(userId);
        
        // Bước 2: Fetch toppings riêng để tránh MultipleBagFetchException
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
            orderRepository.findOrderItemsWithToppings(orderIds);
            // Hibernate sẽ tự động populate toppings vào các OrderItem đã load
        }
        
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserCurrentOrders(Long userId) {
        // Bước 1: Fetch orders với items và drink (không bao gồm DONE)
        List<Order> orders = orderRepository.findByUserIdAndStatusNotWithItemsOrderByCreatedAtDesc(userId, OrderStatus.DONE);
        
        // Bước 2: Fetch toppings riêng để tránh MultipleBagFetchException
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
            orderRepository.findOrderItemsWithToppings(orderIds);
        }
        
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        // Fetch toppings riêng để tránh MultipleBagFetchException
        orderRepository.findOrderItemsWithToppingsByOrderId(orderId);
        
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

        // 🛡️ Log activity - Cập nhật trạng thái đơn hàng
        try {
            if (newStatus == OrderStatus.CANCELED) {
                userMonitoringService.logOrderCancel(userId, orderId, null);
            }
        } catch (Exception e) {
            log.error("Failed to log order status update to monitoring", e);
        }

        OrderDto orderDto = mapToDto(order);

        // WebSocket
        try {
            orderWebSocketService.notifyOrderStatusUpdate(orderDto);
        } catch (Exception e) {
            log.error("Failed to send WebSocket status update", e);
        }

        // Loyalty Points & Email (nếu DONE)
        if (newStatus == OrderStatus.DONE) {
            // 🛡️ Log activity - Thanh toán thành công
            try {
                userMonitoringService.logPaymentSuccess(userId, orderId, 
                    order.getPaymentMethod().name(), null);
            } catch (Exception e) {
                log.error("Failed to log payment success to monitoring", e);
            }
            
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
            
            // 🎯 Xử lý Challenge - Mua 3 sản phẩm giống nhau được cộng 5 điểm
            try {
                var completions = challengeService.processOrderChallenges(order);
                if (!completions.isEmpty()) {
                    log.info("🎯 User {} completed {} challenge(s) from order #{}",
                            order.getUser().getUsername(), completions.size(), orderId);
                }
            } catch (Exception e) {
                log.error("Failed to process challenges for order #{}", orderId, e);
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

        // FIX: Kiểm tra null trước khi stream để tránh NullPointerException
        List<OrderItemToppingDto> toppingDtos = item.getToppings() != null 
                ? item.getToppings().stream()
                    .map(t -> new OrderItemToppingDto(t.getId(), t.getToppingNameSnapshot(), t.getPriceSnapshot()))
                    .collect(Collectors.toList())
                : new java.util.ArrayList<>();
        dto.setToppings(toppingDtos);

        return dto;
    }
    
    /**
     * ✅ SECURITY: VALIDATE ORDER REQUEST
     * Kiểm tra tất cả input từ client để prevent abuse
     */
    private void validateOrderRequest(OrderRequest request) {
        // Import ValidationConstants
        final int MAX_ITEMS = com.utetea.backend.util.ValidationConstants.MAX_ITEMS_PER_ORDER;
        final int MAX_QTY = com.utetea.backend.util.ValidationConstants.MAX_QUANTITY_PER_ITEM;
        final int MIN_QTY = com.utetea.backend.util.ValidationConstants.MIN_QUANTITY;
        
        // 1. Validate số lượng items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }
        
        if (request.getItems().size() > MAX_ITEMS) {
            throw new BusinessException(
                String.format("Số lượng sản phẩm tối đa là %d. Bạn đang đặt %d sản phẩm", 
                    MAX_ITEMS, request.getItems().size())
            );
        }
        
        // 2. Validate từng item
        for (OrderItemRequest item : request.getItems()) {
            // Validate quantity
            if (item.getQuantity() == null || item.getQuantity() < MIN_QTY) {
                throw new com.utetea.backend.exception.InvalidQuantityException(
                    "Số lượng phải lớn hơn " + MIN_QTY
                );
            }
            
            if (item.getQuantity() > MAX_QTY) {
                throw new com.utetea.backend.exception.InvalidQuantityException(
                    item.getQuantity(), MIN_QTY, MAX_QTY
                );
            }
            
            // Validate drinkId
            if (item.getDrinkId() == null || item.getDrinkId() <= 0) {
                throw new BusinessException("DrinkId không hợp lệ");
            }
            
            // Validate sizeName
            if (item.getSizeName() == null || item.getSizeName().trim().isEmpty()) {
                throw new BusinessException("Size không được để trống");
            }
            
            // Validate toppingIds (nếu có)
            if (item.getToppingIds() != null) {
                if (item.getToppingIds().size() > 10) {
                    throw new BusinessException("Số lượng topping tối đa là 10");
                }
                
                for (Long toppingId : item.getToppingIds()) {
                    if (toppingId == null || toppingId <= 0) {
                        throw new BusinessException("ToppingId không hợp lệ");
                    }
                }
            }
        }
        
        // 3. Validate storeId
        if (request.getStoreId() == null || request.getStoreId() <= 0) {
            throw new BusinessException("StoreId không hợp lệ");
        }
        
        // 4. Validate orderType
        if (request.getType() == null) {
            throw new BusinessException("Loại đơn hàng không được để trống");
        }
        
        // 5. Validate address nếu là DELIVERY
        if (request.getType() == OrderType.DELIVERY) {
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                throw new BusinessException("Địa chỉ giao hàng không được để trống");
            }
            
            if (request.getAddress().length() > 500) {
                throw new BusinessException("Địa chỉ giao hàng quá dài (tối đa 500 ký tự)");
            }
        }
        
        // 6. Validate paymentMethod (PaymentMethod là enum)
        if (request.getPaymentMethod() == null) {
            throw new BusinessException("Phương thức thanh toán không được để trống");
        }
        
        log.info("✅ Order request validation passed");
    }
}
