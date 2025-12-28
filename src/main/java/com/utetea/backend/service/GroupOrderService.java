package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupOrderService {
    
    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderMemberRepository memberRepository;
    private final GroupOrderItemRepository itemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    private final OrderService orderService;
    
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    @Transactional
    public GroupOrderDto createGroupOrder(String username, CreateGroupOrderRequest request) {
        User host = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        // Check if user already has an active group order (as host or member)
        List<GroupOrder> activeOrders = groupOrderRepository.findByMemberUserIdAndStatus(
            host.getId(), GroupOrderStatus.OPEN);
        if (!activeOrders.isEmpty()) {
            // Trả về phiên đang hoạt động thay vì throw exception
            log.info("User {} already has active group order, returning existing one", username);
            return mapToDto(activeOrders.get(0));
        }
        
        // Kiểm tra cả phiên LOCKED
        List<GroupOrder> lockedOrders = groupOrderRepository.findByMemberUserIdAndStatus(
            host.getId(), GroupOrderStatus.LOCKED);
        if (!lockedOrders.isEmpty()) {
            log.info("User {} has locked group order, returning it", username);
            return mapToDto(lockedOrders.get(0));
        }
        
        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setHostUser(host);
        groupOrder.setInviteCode(generateUniqueInviteCode());
        groupOrder.setName(request.getName() != null ? request.getName() : "Đơn nhóm của " + host.getFullName());
        groupOrder.setStatus(GroupOrderStatus.OPEN);
        groupOrder.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 10);
        groupOrder.setExpiresAt(LocalDateTime.now().plusMinutes(
            request.getExpirationMinutes() != null ? request.getExpirationMinutes() : 60));
        
        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));
            groupOrder.setStore(store);
        }
        
        groupOrder.setOrderType(request.getOrderType());
        groupOrder.setDeliveryAddress(request.getDeliveryAddress());
        
        groupOrder = groupOrderRepository.save(groupOrder);
        
        // Add host as first member
        GroupOrderMember hostMember = new GroupOrderMember();
        hostMember.setGroupOrder(groupOrder);
        hostMember.setUser(host);
        hostMember.setIsHost(true);
        memberRepository.save(hostMember);
        
        log.info("Created group order {} with invite code {} by user {}", 
            groupOrder.getId(), groupOrder.getInviteCode(), username);
        
        // Fetch lại với đầy đủ thông tin
        return getGroupOrderById(groupOrder.getId());
    }

    @Transactional
    public GroupOrderDto joinGroupOrder(String username, JoinGroupOrderRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findByInviteCodeWithMembers(
            request.getInviteCode().toUpperCase())
            .orElseThrow(() -> new BusinessException("Mã mời không hợp lệ"));
        
        // Check if already a member - trả về phiên thay vì throw exception
        if (memberRepository.existsByGroupOrderIdAndUserId(groupOrder.getId(), user.getId())) {
            log.info("User {} already member of group order {}, returning it", username, groupOrder.getId());
            return getGroupOrderById(groupOrder.getId());
        }
        
        // Validate group order status
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new BusinessException("Phiên đặt hàng nhóm đã đóng hoặc hết hạn");
        }
        
        // Check expiration
        if (groupOrder.getExpiresAt() != null && groupOrder.getExpiresAt().isBefore(LocalDateTime.now())) {
            groupOrder.setStatus(GroupOrderStatus.EXPIRED);
            groupOrderRepository.save(groupOrder);
            throw new BusinessException("Phiên đặt hàng nhóm đã hết hạn");
        }
        
        // Check max members
        int currentMembers = memberRepository.countByGroupOrderId(groupOrder.getId());
        if (currentMembers >= groupOrder.getMaxMembers()) {
            throw new BusinessException("Phiên đặt hàng đã đủ số thành viên tối đa");
        }
        
        // Add member
        GroupOrderMember member = new GroupOrderMember();
        member.setGroupOrder(groupOrder);
        member.setUser(user);
        member.setIsHost(false);
        memberRepository.save(member);
        
        log.info("User {} joined group order {}", username, groupOrder.getId());
        
        return getGroupOrderById(groupOrder.getId());
    }

    @Transactional
    public GroupOrderDto addItem(String username, Long groupOrderId, AddGroupOrderItemRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        // Validate status - chỉ cho phép thêm món khi phiên OPEN
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            if (groupOrder.getStatus() == GroupOrderStatus.LOCKED) {
                throw new BusinessException("Phiên đã bị khóa, không thể thêm món");
            }
            throw new BusinessException("Không thể thêm món vào phiên đã đóng");
        }
        
        // Check if user is a member
        if (!memberRepository.existsByGroupOrderIdAndUserId(groupOrderId, user.getId())) {
            throw new BusinessException("Bạn không phải thành viên của phiên này");
        }
        
        Drink drink = drinkRepository.findById(request.getDrinkId())
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", request.getDrinkId()));
        
        if (!drink.getIsActive()) {
            throw new BusinessException("Sản phẩm không còn khả dụng");
        }
        
        GroupOrderItem item = new GroupOrderItem();
        item.setGroupOrder(groupOrder);
        item.setUser(user);
        item.setDrink(drink);
        item.setDrinkNameSnapshot(drink.getName());
        item.setSizeName(request.getSizeName());
        item.setQuantity(request.getQuantity());
        item.setNote(request.getNote());
        
        // Calculate price
        BigDecimal unitPrice = drink.getBasePrice();
        
        // Add size price
        if (request.getSizeName() != null) {
            List<DrinkSize> sizes = drinkSizeRepository.findByDrinkId(drink.getId());
            for (DrinkSize size : sizes) {
                if (size.getSizeName().equals(request.getSizeName())) {
                    unitPrice = unitPrice.add(size.getExtraPrice());
                    break;
                }
            }
        }
        
        // Add toppings
        StringBuilder toppingsSnapshot = new StringBuilder();
        StringBuilder toppingIdsStr = new StringBuilder();
        if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
            for (Long toppingId : request.getToppingIds()) {
                DrinkTopping topping = drinkToppingRepository.findById(toppingId).orElse(null);
                if (topping != null && topping.getIsActive()) {
                    unitPrice = unitPrice.add(topping.getPrice());
                    if (toppingsSnapshot.length() > 0) {
                        toppingsSnapshot.append(", ");
                        toppingIdsStr.append(",");
                    }
                    toppingsSnapshot.append(topping.getToppingName());
                    toppingIdsStr.append(toppingId);
                }
            }
        }
        item.setToppingIdsString(toppingIdsStr.toString());
        item.setToppingsSnapshot(toppingsSnapshot.toString());
        item.setUnitPrice(unitPrice);
        item.setItemPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())));
        
        itemRepository.save(item);
        log.info("User {} added item to group order {}", username, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }

    @Transactional
    public GroupOrderDto updateItem(String username, Long groupOrderId, Long itemId, 
                                    AddGroupOrderItemRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrderItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrderItem", "id", itemId));
        
        // Validate ownership
        if (!item.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn chỉ có thể sửa món của mình", HttpStatus.FORBIDDEN);
        }
        
        // Validate group order status - chỉ cho phép sửa khi phiên OPEN
        if (item.getGroupOrder().getStatus() != GroupOrderStatus.OPEN) {
            if (item.getGroupOrder().getStatus() == GroupOrderStatus.LOCKED) {
                throw new BusinessException("Phiên đã bị khóa, không thể sửa món");
            }
            throw new BusinessException("Không thể sửa món trong phiên đã đóng");
        }
        
        // Update item
        item.setQuantity(request.getQuantity());
        item.setNote(request.getNote());
        item.setItemPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        
        itemRepository.save(item);
        log.info("User {} updated item {} in group order {}", username, itemId, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }
    
    @Transactional
    public GroupOrderDto removeItem(String username, Long groupOrderId, Long itemId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrderItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrderItem", "id", itemId));
        
        GroupOrder groupOrder = item.getGroupOrder();
        
        // Validate ownership or host
        boolean isHost = groupOrder.getHostUser().getId().equals(user.getId());
        boolean isOwner = item.getUser().getId().equals(user.getId());
        
        if (!isHost && !isOwner) {
            throw new BusinessException("Bạn không có quyền xóa món này", HttpStatus.FORBIDDEN);
        }
        
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            if (groupOrder.getStatus() == GroupOrderStatus.LOCKED) {
                throw new BusinessException("Phiên đã bị khóa, không thể xóa món");
            }
            throw new BusinessException("Không thể xóa món trong phiên đã đóng");
        }
        
        itemRepository.delete(item);
        log.info("User {} removed item {} from group order {}", username, itemId, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }

    @Transactional
    public GroupOrderDto leaveGroupOrder(String username, Long groupOrderId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        // Host cannot leave, must cancel
        if (groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Host không thể rời nhóm. Hãy hủy phiên nếu muốn kết thúc.");
        }
        
        // Chỉ cho phép rời khi phiên OPEN hoặc LOCKED
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN && 
            groupOrder.getStatus() != GroupOrderStatus.LOCKED) {
            throw new BusinessException("Không thể rời phiên đã hoàn thành hoặc đã hủy");
        }
        
        // Remove member's items
        itemRepository.deleteByGroupOrderIdAndUserId(groupOrderId, user.getId());
        
        // Remove member
        memberRepository.deleteByGroupOrderIdAndUserId(groupOrderId, user.getId());
        
        log.info("User {} left group order {}", username, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }
    
    @Transactional
    public GroupOrderDto lockGroupOrder(String username, Long groupOrderId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        // Only host can lock
        if (!groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Chỉ host mới có thể khóa phiên", HttpStatus.FORBIDDEN);
        }
        
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new BusinessException("Phiên không ở trạng thái có thể khóa");
        }
        
        groupOrder.setStatus(GroupOrderStatus.LOCKED);
        groupOrderRepository.save(groupOrder);
        
        log.info("Host {} locked group order {}", username, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }
    
    @Transactional
    public GroupOrderDto unlockGroupOrder(String username, Long groupOrderId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        if (!groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Chỉ host mới có thể mở khóa phiên", HttpStatus.FORBIDDEN);
        }
        
        if (groupOrder.getStatus() != GroupOrderStatus.LOCKED) {
            throw new BusinessException("Phiên không ở trạng thái khóa");
        }
        
        groupOrder.setStatus(GroupOrderStatus.OPEN);
        groupOrderRepository.save(groupOrder);
        
        log.info("Host {} unlocked group order {}", username, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }

    @Transactional
    public GroupOrderDto updateGroupOrder(String username, Long groupOrderId, 
                                          UpdateGroupOrderRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        if (!groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Chỉ host mới có thể cập nhật thông tin phiên", HttpStatus.FORBIDDEN);
        }
        
        if (groupOrder.getStatus() == GroupOrderStatus.COMPLETED || 
            groupOrder.getStatus() == GroupOrderStatus.CANCELLED) {
            throw new BusinessException("Không thể cập nhật phiên đã hoàn thành hoặc đã hủy");
        }
        
        if (request.getName() != null) {
            groupOrder.setName(request.getName());
        }
        
        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));
            groupOrder.setStore(store);
        }
        
        if (request.getOrderType() != null) {
            groupOrder.setOrderType(request.getOrderType());
        }
        
        if (request.getDeliveryAddress() != null) {
            groupOrder.setDeliveryAddress(request.getDeliveryAddress());
        }
        
        groupOrderRepository.save(groupOrder);
        log.info("Host {} updated group order {}", username, groupOrderId);
        
        return getGroupOrderById(groupOrderId);
    }
    
    @Transactional
    public void cancelGroupOrder(String username, Long groupOrderId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        if (!groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Chỉ host mới có thể hủy phiên", HttpStatus.FORBIDDEN);
        }
        
        if (groupOrder.getStatus() == GroupOrderStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy phiên đã hoàn thành");
        }
        
        groupOrder.setStatus(GroupOrderStatus.CANCELLED);
        groupOrderRepository.save(groupOrder);
        
        log.info("Host {} cancelled group order {}", username, groupOrderId);
    }

    @Transactional
    public OrderDto checkoutGroupOrder(String username, Long groupOrderId, 
                                       CheckoutGroupOrderRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        GroupOrder groupOrder = groupOrderRepository.findByIdWithMembers(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        // Only host can checkout
        if (!groupOrder.getHostUser().getId().equals(user.getId())) {
            throw new BusinessException("Chỉ host mới có thể thanh toán", HttpStatus.FORBIDDEN);
        }
        
        // Validate status
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN && 
            groupOrder.getStatus() != GroupOrderStatus.LOCKED) {
            throw new BusinessException("Phiên không ở trạng thái có thể thanh toán");
        }
        
        // Validate store
        if (groupOrder.getStore() == null) {
            throw new BusinessException("Vui lòng chọn chi nhánh trước khi thanh toán");
        }
        
        // Validate items - fetch riêng
        List<GroupOrderItem> items = itemRepository.findByGroupOrderIdWithDetails(groupOrderId);
        if (items.isEmpty()) {
            throw new BusinessException("Không có món nào trong đơn hàng");
        }
        
        // Validate delivery address for DELIVERY type
        if (groupOrder.getOrderType() == OrderType.DELIVERY && 
            (groupOrder.getDeliveryAddress() == null || groupOrder.getDeliveryAddress().trim().isEmpty())) {
            throw new BusinessException("Vui lòng nhập địa chỉ giao hàng");
        }
        
        // Convert to OrderRequest
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setStoreId(groupOrder.getStore().getId());
        orderRequest.setType(groupOrder.getOrderType() != null ? groupOrder.getOrderType() : OrderType.PICKUP);
        orderRequest.setAddress(groupOrder.getDeliveryAddress());
        orderRequest.setPaymentMethod(request.getPaymentMethod());
        orderRequest.setPromotionCode(request.getPromotionCode());
        orderRequest.setSpinVoucherCode(request.getSpinVoucherCode());
        
        // Convert items
        List<OrderItemRequest> orderItems = items.stream().map(item -> {
            OrderItemRequest itemReq = new OrderItemRequest();
            itemReq.setDrinkId(item.getDrink().getId());
            itemReq.setQuantity(item.getQuantity());
            itemReq.setSizeName(item.getSizeName());
            
            // Parse toppingIds from string
            List<Long> toppingIdsList = new ArrayList<>();
            if (item.getToppingIdsString() != null && !item.getToppingIdsString().isEmpty()) {
                for (String idStr : item.getToppingIdsString().split(",")) {
                    try {
                        toppingIdsList.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException e) {
                        // Ignore invalid ids
                    }
                }
            }
            itemReq.setToppingIds(toppingIdsList);
            
            itemReq.setNote(item.getNote() != null ? 
                "[" + item.getUser().getFullName() + "] " + item.getNote() : 
                "[" + item.getUser().getFullName() + "]");
            return itemReq;
        }).collect(Collectors.toList());
        orderRequest.setItems(orderItems);
        
        // Create order using existing OrderService
        OrderDto order = orderService.createOrder(username, orderRequest);
        
        // Update group order status
        groupOrder.setStatus(GroupOrderStatus.COMPLETED);
        Order finalOrder = new Order();
        finalOrder.setId(order.getId());
        groupOrder.setFinalOrder(finalOrder);
        groupOrderRepository.save(groupOrder);
        
        log.info("Host {} completed checkout for group order {}, created order {}", 
            username, groupOrderId, order.getId());
        
        return order;
    }

    @Transactional(readOnly = true)
    public GroupOrderDto getGroupOrderById(Long groupOrderId) {
        // Fetch members trước
        GroupOrder groupOrder = groupOrderRepository.findByIdWithMembers(groupOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "id", groupOrderId));
        
        // Fetch items riêng để tránh MultipleBagFetchException
        List<GroupOrderItem> items = itemRepository.findByGroupOrderIdWithDetails(groupOrderId);
        
        // Không dùng setItems() vì orphanRemoval=true, thay vào đó map trực tiếp
        return mapToDtoWithItems(groupOrder, items);
    }
    
    @Transactional(readOnly = true)
    public GroupOrderDto getGroupOrderByInviteCode(String inviteCode) {
        GroupOrder groupOrder = groupOrderRepository.findByInviteCodeWithMembers(inviteCode.toUpperCase())
            .orElseThrow(() -> new BusinessException("Mã mời không hợp lệ"));
        
        // Fetch items riêng để tránh MultipleBagFetchException
        List<GroupOrderItem> items = itemRepository.findByGroupOrderIdWithDetails(groupOrder.getId());
        
        return mapToDtoWithItems(groupOrder, items);
    }
    
    @Transactional(readOnly = true)
    public List<GroupOrderDto> getActiveGroupOrders(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        // Lấy cả phiên OPEN và LOCKED
        List<GroupOrderStatus> activeStatuses = Arrays.asList(GroupOrderStatus.OPEN, GroupOrderStatus.LOCKED);
        return groupOrderRepository.findByMemberUserIdAndStatusIn(user.getId(), activeStatuses)
            .stream()
            .map(g -> {
                // Fetch items cho mỗi group order
                List<GroupOrderItem> items = itemRepository.findByGroupOrderIdWithDetails(g.getId());
                return mapToDtoWithItems(g, items);
            })
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<GroupOrderDto> getUserGroupOrders(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        return groupOrderRepository.findByMemberUserId(user.getId())
            .stream()
            .map(g -> {
                // Fetch items cho mỗi group order
                List<GroupOrderItem> items = itemRepository.findByGroupOrderIdWithDetails(g.getId());
                return mapToDtoWithItems(g, items);
            })
            .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void expireOldGroupOrders() {
        List<GroupOrder> expiredOrders = groupOrderRepository.findExpiredGroupOrders(
            GroupOrderStatus.OPEN, LocalDateTime.now());
        
        for (GroupOrder order : expiredOrders) {
            order.setStatus(GroupOrderStatus.EXPIRED);
            groupOrderRepository.save(order);
            log.info("Group order {} expired", order.getId());
        }
    }

    private String generateUniqueInviteCode() {
        SecureRandom random = new SecureRandom();
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = sb.toString();
            attempts++;
            if (attempts > 100) {
                throw new BusinessException("Không thể tạo mã mời, vui lòng thử lại");
            }
        } while (groupOrderRepository.existsByInviteCode(code));
        return code;
    }
    
    private GroupOrderDto mapToDto(GroupOrder groupOrder) {
        return mapToDtoWithItems(groupOrder, null);
    }
    
    private GroupOrderDto mapToDtoWithItems(GroupOrder groupOrder, List<GroupOrderItem> fetchedItems) {
        GroupOrderDto dto = GroupOrderDto.builder()
            .id(groupOrder.getId())
            .inviteCode(groupOrder.getInviteCode())
            .name(groupOrder.getName())
            .status(groupOrder.getStatus())
            .hostUserId(groupOrder.getHostUser().getId())
            .hostUserName(groupOrder.getHostUser().getFullName())
            .orderType(groupOrder.getOrderType())
            .deliveryAddress(groupOrder.getDeliveryAddress())
            .expiresAt(groupOrder.getExpiresAt())
            .maxMembers(groupOrder.getMaxMembers())
            .createdAt(groupOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
            .updatedAt(groupOrder.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
            .build();
        
        if (groupOrder.getStore() != null) {
            dto.setStoreId(groupOrder.getStore().getId());
            dto.setStoreName(groupOrder.getStore().getStoreName());
        }
        
        if (groupOrder.getFinalOrder() != null) {
            dto.setFinalOrderId(groupOrder.getFinalOrder().getId());
        }
        
        // Map members - check if initialized
        List<GroupOrderMemberDto> members = new ArrayList<>();
        if (groupOrder.getMembers() != null && org.hibernate.Hibernate.isInitialized(groupOrder.getMembers())) {
            members = groupOrder.getMembers().stream()
                .map(m -> GroupOrderMemberDto.builder()
                    .id(m.getId())
                    .userId(m.getUser().getId())
                    .userName(m.getUser().getFullName())
                    .userAvatar(m.getUser().getAvatarUrl())
                    .isHost(m.getIsHost())
                    .joinedAt(m.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                    .itemCount(itemRepository.countByGroupOrderIdAndUserId(groupOrder.getId(), m.getUser().getId()))
                    .build())
                .collect(Collectors.toList());
        }
        dto.setMembers(members);
        dto.setCurrentMemberCount(members.size());
        
        // Map items - use fetchedItems if provided, otherwise check entity
        List<GroupOrderItemDto> items = new ArrayList<>();
        Collection<GroupOrderItem> itemsToMap = fetchedItems;
        
        if (itemsToMap == null && groupOrder.getItems() != null && 
            org.hibernate.Hibernate.isInitialized(groupOrder.getItems())) {
            itemsToMap = groupOrder.getItems();
        }
        
        if (itemsToMap != null) {
            items = itemsToMap.stream()
                .map(i -> {
                    // Parse toppingIds from string
                    List<Long> toppingIdsList = new ArrayList<>();
                    if (i.getToppingIdsString() != null && !i.getToppingIdsString().isEmpty()) {
                        for (String idStr : i.getToppingIdsString().split(",")) {
                            try {
                                toppingIdsList.add(Long.parseLong(idStr.trim()));
                            } catch (NumberFormatException e) {
                                // Ignore invalid ids - this is safe to catch
                            }
                        }
                    }
                    
                    return GroupOrderItemDto.builder()
                        .id(i.getId())
                        .userId(i.getUser().getId())
                        .userName(i.getUser().getFullName())
                        .drinkId(i.getDrink().getId())
                        .drinkName(i.getDrinkNameSnapshot())
                        .drinkImage(i.getDrink().getImageUrl())
                        .sizeName(i.getSizeName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .itemPrice(i.getItemPrice())
                        .note(i.getNote())
                        .toppingIds(toppingIdsList)
                        .toppingsSnapshot(i.getToppingsSnapshot())
                        .build();
                })
                .collect(Collectors.toList());
        }
        dto.setItems(items);
        
        // Calculate total
        BigDecimal total = items.stream()
            .map(GroupOrderItemDto::getItemPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalPrice(total);
        
        return dto;
    }
}
