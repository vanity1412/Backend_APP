package com.utetea.backend.service;

import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.utetea.backend.dto.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final SpinRewardRepository spinRewardRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderMemberRepository groupOrderMemberRepository;
    private final GroupOrderItemRepository groupOrderItemRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final DeletedUserOrderBackupRepository deletedUserOrderBackupRepository;
    private final DeletedUserReviewBackupRepository deletedUserReviewBackupRepository;
    private final AvatarUploadService avatarUploadService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final UserMonitoringService userMonitoringService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Check if phone is being changed and already exists
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException("Phone number already exists");
            }
            user.setPhone(request.getPhone());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        user = userRepository.save(user);
        
        // 🛡️ Log activity - Cập nhật profile
        try {
            StringBuilder updatedFields = new StringBuilder();
            if (request.getPhone() != null) updatedFields.append("phone, ");
            if (request.getFullName() != null) updatedFields.append("fullName, ");
            if (request.getAddress() != null) updatedFields.append("address, ");
            if (request.getEmail() != null) updatedFields.append("email, ");
            String fields = updatedFields.length() > 0 ? 
                updatedFields.substring(0, updatedFields.length() - 2) : "none";
            userMonitoringService.logProfileUpdate(user.getId(), fields, null);
        } catch (Exception e) {
            log.error("Failed to log profile update to monitoring", e);
        }
        
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateAvatar(String username, MultipartFile file) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // 1. Upload lên GitHub và lấy link
        String avatarUrl = avatarUploadService.uploadFile(file, String.valueOf(user.getId()));

        // 2. Lưu link vào Database
        // Giả sử entity User của bạn có setAvatarUrl
        user.setAvatarUrl(avatarUrl);
        user = userRepository.save(user);

        return mapToProfileDto(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu cũ không chính xác");
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu (nếu cần logic này ở Backend)
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        // Mã hóa mật khẩu mới và lưu xuống DB
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // 🛡️ Log activity - Đổi mật khẩu (QUAN TRỌNG)
        try {
            userMonitoringService.logPasswordChange(user.getId(), null);
        } catch (Exception e) {
            log.error("Failed to log password change to monitoring", e);
        }
    }

    @Transactional
    public void deleteAccount(String username) {
        // Tìm người dùng
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        Long userId = user.getId();
        log.info("Deleting account for user: {} (ID: {})", username, userId);

        try {
            // BƯỚC 1: Backup orders trước khi xóa (để manager vẫn quản lý được doanh thu)
            // Chỉ backup các order DONE để tính doanh thu
            log.info("Backing up DONE orders for user {}", userId);
            backupUserOrders(user);
            
            // BƯỚC 1.5: Backup reviews trước khi xóa (để manager vẫn xem được đánh giá)
            log.info("Backing up reviews for user {}", userId);
            backupUserReviews(user);
            
            // BƯỚC 2: Xóa các dữ liệu liên quan theo thứ tự để tránh lỗi khóa ngoại
            
            // 1. Xóa cart và cart items
            log.info("Deleting cart for user {}", userId);
            cartRepository.deleteByUserId(userId);
            
            // 2. Xóa reviews
            log.info("Deleting reviews for user {}", userId);
            reviewRepository.deleteByUserId(userId);
            
            // 3. Xóa spin rewards (vouchers từ vòng quay)
            log.info("Deleting spin rewards for user {}", userId);
            spinRewardRepository.deleteByUserId(userId);
            
            // 4. Xóa group order items của user (khi user tham gia group order của người khác)
            log.info("Deleting group order items for user {}", userId);
            groupOrderItemRepository.deleteByUserId(userId);
            
            // 5. Xóa group order members của user (khi user tham gia group order của người khác)
            log.info("Deleting group order members for user {}", userId);
            groupOrderMemberRepository.deleteByUserId(userId);
            
            // 6. Xóa group chat messages trước (vì có FK đến group_orders)
            log.info("Deleting group chat messages for user {}", userId);
            groupChatMessageRepository.deleteByHostUserId(userId);
            
            // 7. Xóa group orders (nếu là host)
            log.info("Deleting group orders for user {}", userId);
            groupOrderRepository.deleteByHostUserId(userId);
            
            // 8. Xóa chat conversations
            log.info("Deleting chat conversations for user {}", userId);
            chatConversationRepository.deleteByUserId(userId);
            
            // 9. Xóa orders (lịch sử đơn hàng) - đã backup ở trên
            log.info("Deleting orders for user {}", userId);
            orderRepository.deleteByUserId(userId);
            
            // 10. Cuối cùng xóa user
            log.info("Deleting user {}", userId);
            userRepository.delete(user);
            
            log.info("Successfully deleted account for user: {} (ID: {})", username, userId);
            
        } catch (Exception e) {
            log.error("Error deleting account for user {}: {}", userId, e.getMessage(), e);
            throw new BusinessException("Không thể xóa tài khoản: " + e.getMessage());
        }
    }
    
    /**
     * Backup tất cả orders DONE của user vào bảng DeletedUserOrderBackup
     * Để manager vẫn có thể quản lý doanh thu, revenue, cảnh báo
     */
    private void backupUserOrders(User user) {
        List<Order> orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(user.getId());
        int backupCount = 0;
        
        for (Order order : orders) {
            // Chỉ backup các order DONE (đã hoàn thành) để tính doanh thu
            if (order.getStatus() != OrderStatus.DONE) {
                continue;
            }
            
            // Tạo JSON cho order items
            String orderItemsJson = createOrderItemsJson(order);
            
            // Truncate phone nếu quá dài
            String phone = user.getPhone();
            if (phone != null && phone.length() > 250) {
                phone = phone.substring(0, 250);
            }
            
            DeletedUserOrderBackup backup = DeletedUserOrderBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .deletedUserPhone(phone)
                    .originalOrderId(order.getId())
                    .store(order.getStore())
                    .orderType(order.getType())
                    .orderStatus(order.getStatus())
                    .totalPrice(order.getTotalPrice())
                    .discount(order.getDiscount())
                    .finalPrice(order.getFinalPrice())
                    .paymentMethod(order.getPaymentMethod())
                    .orderCreatedAt(order.getCreatedAt())
                    .orderItemsJson(orderItemsJson)
                    .note("Auto backup when user deleted account")
                    .build();
            
            deletedUserOrderBackupRepository.save(backup);
            backupCount++;
            log.debug("Backed up order {} for user {}", order.getId(), user.getId());
        }
        
        log.info("Backed up {} DONE orders for user {}", backupCount, user.getId());
    }
    
    /**
     * Backup tất cả reviews của user vào bảng DeletedUserReviewBackup
     * Để manager vẫn có thể xem lịch sử đánh giá sản phẩm
     */
    private void backupUserReviews(User user) {
        List<Review> reviews = reviewRepository.findByUserId(user.getId());
        int backupCount = 0;
        
        for (Review review : reviews) {
            DeletedUserReviewBackup backup = DeletedUserReviewBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .deletedUserFullname(user.getFullName())
                    .originalReviewId(review.getId())
                    .drinkId(review.getDrink().getId())
                    .drinkName(review.getDrink().getName())
                    .orderId(review.getOrder().getId())
                    .orderItemId(review.getOrderItem().getId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .isAnonymous(review.getIsAnonymous())
                    .reviewCreatedAt(review.getCreatedAt())
                    .build();
            
            deletedUserReviewBackupRepository.save(backup);
            backupCount++;
            log.debug("Backed up review {} for user {}", review.getId(), user.getId());
        }
        
        log.info("Backed up {} reviews for user {}", backupCount, user.getId());
    }
    
    /**
     * Tạo JSON string chứa thông tin chi tiết order items
     */
    private String createOrderItemsJson(Order order) {
        try {
            List<Map<String, Object>> itemsList = new ArrayList<>();
            
            for (OrderItem item : order.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("drinkId", item.getDrink() != null ? item.getDrink().getId() : null);
                itemMap.put("drinkName", item.getDrinkNameSnapshot());
                itemMap.put("sizeName", item.getSizeNameSnapshot());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("itemPrice", item.getItemPrice());
                itemMap.put("note", item.getNote());
                
                // Thêm toppings
                List<Map<String, Object>> toppingsList = new ArrayList<>();
                for (OrderItemTopping topping : item.getToppings()) {
                    Map<String, Object> toppingMap = new HashMap<>();
                    toppingMap.put("toppingName", topping.getToppingNameSnapshot());
                    toppingMap.put("toppingPrice", topping.getPriceSnapshot());
                    toppingsList.add(toppingMap);
                }
                itemMap.put("toppings", toppingsList);
                
                itemsList.add(itemMap);
            }
            
            return objectMapper.writeValueAsString(itemsList);
        } catch (Exception e) {
            log.warn("Failed to create order items JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .role(user.getRole())
                .memberTier(user.getMemberTier())
                .points(user.getPoints())
                .active(user.getActive())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
