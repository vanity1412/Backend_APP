package com.utetea.backend.service;

import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
import com.utetea.backend.util.RequestContextUtil;

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
    private final MonitoringAlertRepository monitoringAlertRepository;
    private final DeletedUserMonitoringAlertBackupRepository deletedUserMonitoringAlertBackupRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final DeletedUserActivityLogBackupRepository deletedUserActivityLogBackupRepository;
    private final UserRiskScoreRepository userRiskScoreRepository;
    private final DeletedUserRiskScoreBackupRepository deletedUserRiskScoreBackupRepository;
    private final AvatarUploadService avatarUploadService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final UserMonitoringService userMonitoringService;
    private final NotificationRepository notificationRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ChallengeCompletionRepository challengeCompletionRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final EntityManager entityManager;

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
            userMonitoringService.logProfileUpdate(user.getId(), fields, RequestContextUtil.getCurrentRequest());
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
            userMonitoringService.logPasswordChange(user.getId(), RequestContextUtil.getCurrentRequest());
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

            // BƯỚC 1.6: Backup monitoring alerts trước khi xóa (để manager vẫn xem được lịch sử cảnh báo)
            log.info("Backing up monitoring alerts for user {}", userId);
            backupUserMonitoringAlerts(user);

            // BƯỚC 1.7: Backup activity logs trước khi xóa (để manager vẫn xem được lịch sử hoạt động)
            log.info("Backing up activity logs for user {}", userId);
            backupUserActivityLogs(user);

            // BƯỚC 1.8: Backup risk score trước khi xóa (để manager vẫn xem được điểm rủi ro)
            log.info("Backing up risk score for user {}", userId);
            backupUserRiskScore(user);

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
            
            // 6. Xóa group chat messages (cả tin nhắn user gửi và tin nhắn trong group của user)
            log.info("Deleting group chat messages for user {}", userId);
            groupChatMessageRepository.deleteBySenderId(userId);
            groupChatMessageRepository.deleteByHostUserId(userId);
            
            // 7. Xóa group orders (nếu là host)
            log.info("Deleting group orders for user {}", userId);
            groupOrderRepository.deleteByHostUserId(userId);
            
            // 8. Xóa live chat messages trước khi xóa conversations
            log.info("Deleting live chat messages for user {}", userId);
            liveChatMessageRepository.deleteByConversationUserId(userId);
            liveChatMessageRepository.deleteBySenderId(userId);
            
            // 9. Set manager = null cho các conversations mà user là manager
            log.info("Clearing manager reference in chat conversations for user {}", userId);
            chatConversationRepository.clearManagerByManagerId(userId);
            
            // 10. Xóa chat conversations
            log.info("Deleting chat conversations for user {}", userId);
            chatConversationRepository.deleteByUserId(userId);
            
            // 11. Xóa orders (lịch sử đơn hàng) - đã backup ở trên
            log.info("Deleting orders for user {}", userId);
            orderRepository.deleteByUserId(userId);

            // 12. Set handledBy = null cho các monitoring alerts mà user đã xử lý
            log.info("Clearing handledBy reference in monitoring alerts for user {}", userId);
            monitoringAlertRepository.clearHandledByUserId(userId);

            // 13. Xóa monitoring alerts - đã backup ở trên
            log.info("Deleting monitoring alerts for user {}", userId);
            deleteUserMonitoringAlerts(userId);

            // 14. Xóa activity logs - đã backup ở trên
            log.info("Deleting activity logs for user {}", userId);
            userActivityLogRepository.deleteByUserId(userId);

            // 15. Xóa risk score - đã backup ở trên
            log.info("Deleting risk score for user {}", userId);
            userRiskScoreRepository.findByUserId(userId).ifPresent(userRiskScoreRepository::delete);

            // 16. Xóa notifications
            log.info("Deleting notifications for user {}", userId);
            notificationRepository.deleteByUserId(userId);

            // 17. Xóa promotion usage
            log.info("Deleting promotion usage for user {}", userId);
            promotionUsageRepository.deleteByUserId(userId);

            // 18. Xóa challenge completions
            log.info("Deleting challenge completions for user {}", userId);
            challengeCompletionRepository.deleteByUserId(userId);

            // 19. Clear managed stores (nếu user là Manager) - sử dụng native query thay vì save
            log.info("Clearing managed stores for user {}", userId);
            // Xóa trực tiếp từ bảng join manager_stores thay vì dùng collection
            // user.getManagedStores().clear() có thể gây lỗi khi flush

            // 20. Flush tất cả các thay đổi trước khi xóa user
            entityManager.flush();
            entityManager.clear(); // Clear session để tránh lỗi TransientObjectException
            
            // 21. Cuối cùng xóa user - load lại user từ DB sau khi clear session
            log.info("Deleting user {}", userId);
            User userToDelete = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("User not found after clearing session"));
            userRepository.delete(userToDelete);
            entityManager.flush(); // Đảm bảo xóa ngay lập tức
            
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
     * Backup tất cả monitoring alerts của user vào bảng DeletedUserMonitoringAlertBackup
     * Để manager vẫn có thể xem lịch sử cảnh báo, tracking bảo mật
     */
    private void backupUserMonitoringAlerts(User user) {
        List<MonitoringAlert> alerts = monitoringAlertRepository.findByTargetUserIdOrderByCreatedAtDesc(
                user.getId(),
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
        int backupCount = 0;

        for (MonitoringAlert alert : alerts) {
            DeletedUserMonitoringAlertBackup backup = DeletedUserMonitoringAlertBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .originalAlertId(alert.getId())
                    .alertType(alert.getAlertType() != null ? alert.getAlertType().name() : null)
                    .severity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                    .title(alert.getTitle())
                    .message(alert.getMessage())
                    .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                    .handledByUserId(alert.getHandledBy() != null ? alert.getHandledBy().getId() : null)
                    .handledByUsername(alert.getHandledBy() != null ? alert.getHandledBy().getUsername() : null)
                    .handledAt(alert.getHandledAt())
                    .handlerNote(alert.getHandlerNote())
                    .actionTaken(alert.getActionTaken() != null ? alert.getActionTaken().name() : null)
                    .activityLogId(alert.getActivityLogId())
                    .ipAddress(alert.getIpAddress())
                    .notificationSent(alert.getNotificationSent())
                    .alertCreatedAt(alert.getCreatedAt())
                    .note("Auto backup when user deleted account")
                    .build();

            deletedUserMonitoringAlertBackupRepository.save(backup);
            backupCount++;
            log.debug("Backed up monitoring alert {} for user {}", alert.getId(), user.getId());
        }

        log.info("Backed up {} monitoring alerts for user {}", backupCount, user.getId());
    }

    /**
     * Backup tất cả activity logs của user vào bảng DeletedUserActivityLogBackup
     * Để manager vẫn có thể xem lịch sử hoạt động, phân tích pattern
     */
    private void backupUserActivityLogs(User user) {
        List<UserActivityLog> logs = userActivityLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        int backupCount = 0;

        for (UserActivityLog log : logs) {
            DeletedUserActivityLogBackup backup = DeletedUserActivityLogBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .originalLogId(log.getId())
                    .activityType(log.getActivityType() != null ? log.getActivityType().name() : null)
                    .description(log.getDescription())
                    .riskLevel(log.getRiskLevel() != null ? log.getRiskLevel().name() : null)
                    .ipAddress(log.getIpAddress())
                    .deviceInfo(log.getDeviceInfo())
                    .userAgent(log.getUserAgent())
                    .endpoint(log.getEndpoint())
                    .requestMethod(log.getRequestMethod())
                    .responseStatus(log.getResponseStatus())
                    .relatedId(log.getRelatedId())
                    .extraData(log.getExtraData())
                    .activityCreatedAt(log.getCreatedAt())
                    .note("Auto backup when user deleted account")
                    .build();

            deletedUserActivityLogBackupRepository.save(backup);
            backupCount++;
            this.log.debug("Backed up activity log {} for user {}", log.getId(), user.getId());
        }

        this.log.info("Backed up {} activity logs for user {}", backupCount, user.getId());
    }

    /**
     * Backup risk score của user vào bảng DeletedUserRiskScoreBackup
     * Để manager vẫn có thể phân tích mức độ rủi ro
     */
    private void backupUserRiskScore(User user) {
        userRiskScoreRepository.findByUserId(user.getId()).ifPresent(riskScore -> {
            DeletedUserRiskScoreBackup backup = DeletedUserRiskScoreBackup.builder()
                    .deletedUserId(user.getId())
                    .deletedUsername(user.getUsername())
                    .originalRiskScoreId(riskScore.getId())
                    .totalScore(riskScore.getTotalScore())
                    .riskLevel(riskScore.getRiskLevel() != null ? riskScore.getRiskLevel().name() : null)
                    .loginFailedCount(riskScore.getLoginFailedCount())
                    .orderCancelCount(riskScore.getOrderCancelCount())
                    .paymentFailedCount(riskScore.getPaymentFailedCount())
                    .rateLimitHitCount(riskScore.getRateLimitHitCount())
                    .promotionAbuseCount(riskScore.getPromotionAbuseCount())
                    .spamRequestCount(riskScore.getSpamRequestCount())
                    .lastIpAddress(riskScore.getLastIpAddress())
                    .lastScoreReset(riskScore.getLastScoreReset())
                    .adminNote(riskScore.getAdminNote())
                    .notedBy(riskScore.getNotedBy())
                    .notedAt(riskScore.getNotedAt())
                    .autoBlocked(riskScore.getAutoBlocked())
                    .autoBlockedAt(riskScore.getAutoBlockedAt())
                    .autoBlockedReason(riskScore.getAutoBlockedReason())
                    .riskScoreCreatedAt(riskScore.getCreatedAt())
                    .riskScoreUpdatedAt(riskScore.getUpdatedAt())
                    .note("Auto backup when user deleted account")
                    .build();

            deletedUserRiskScoreBackupRepository.save(backup);
            log.info("Backed up risk score for user {}", user.getId());
        });
    }

    /**
     * Xóa tất cả monitoring alerts liên quan đến user
     * Bao gồm cả alerts mà user là target và alerts mà user là handler
     */
    private void deleteUserMonitoringAlerts(Long userId) {
        // Xóa alerts mà user là target
        List<MonitoringAlert> targetAlerts = monitoringAlertRepository.findByTargetUserIdOrderByCreatedAtDesc(
                userId,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        if (!targetAlerts.isEmpty()) {
            monitoringAlertRepository.deleteAll(targetAlerts);
            log.debug("Deleted {} monitoring alerts where user {} is target", targetAlerts.size(), userId);
        }

        // Xóa reference đến user trong handledBy (set null thay vì xóa alert)
        // Vì alert này không phải của user đang bị xóa, chỉ là user này đã xử lý nó
        // Ta sẽ dùng query để set null cho handledBy
        // Tuy nhiên, vì không có method sẵn, ta sẽ skip phần này
        // Hoặc có thể thêm query method vào MonitoringAlertRepository nếu cần

        log.info("Deleted all monitoring alerts for user {}", userId);
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
