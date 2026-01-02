package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.model.MonitoringAlert.*;
import com.utetea.backend.model.UserActivityLog.*;
import com.utetea.backend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🛡️ USER MONITORING SERVICE
 * Core service cho hệ thống giám sát hành vi người dùng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserMonitoringService {

    private final UserActivityLogRepository activityLogRepository;
    private final UserRiskScoreRepository riskScoreRepository;
    private final MonitoringAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final OneSignalService oneSignalService;
    private final MonitoringWebSocketService monitoringWebSocketService;

    // ==================== RISK SCORING RULES ====================
    private static final int SCORE_LOGIN_FAILED = 5;
    private static final int SCORE_ORDER_CANCEL = 10;
    private static final int SCORE_PAYMENT_FAILED = 15;
    private static final int SCORE_RATE_LIMIT_HIT = 20;
    private static final int SCORE_PROMOTION_ABUSE = 25;
    private static final int SCORE_SPAM_REQUEST = 30;
    private static final int SCORE_BRUTE_FORCE = 40;
    
    private static final int AUTO_BLOCK_THRESHOLD = 80;
    private static final int ALERT_THRESHOLD_WARNING = 30;
    private static final int ALERT_THRESHOLD_SUSPICIOUS = 60;

    // ==================== ACTIVITY LOGGING ====================

    /**
     * Ghi log hoạt động của user
     */
    @Transactional
    public UserActivityLog logActivity(Long userId, ActivityType activityType, 
                                        String description, HttpServletRequest request) {
        return logActivity(userId, activityType, description, RiskLevel.NORMAL, null, request);
    }

    @Transactional
    public UserActivityLog logActivity(Long userId, ActivityType activityType, 
                                        String description, RiskLevel riskLevel,
                                        Long relatedId, HttpServletRequest request) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        UserActivityLog activityLog = UserActivityLog.builder()
            .user(user)
            .activityType(activityType)
            .description(description)
            .riskLevel(riskLevel)
            .relatedId(relatedId)
            .ipAddress(getClientIp(request))
            .deviceInfo(getDeviceInfo(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .endpoint(request != null ? request.getRequestURI() : null)
            .requestMethod(request != null ? request.getMethod() : null)
            .build();

        activityLog = activityLogRepository.save(activityLog);
        log.info("Activity logged: userId={}, type={}, risk={}", userId, activityType, riskLevel);

        // 📡 Gửi WebSocket notification cho TẤT CẢ activity
        try {
            monitoringWebSocketService.notifyNewActivity(UserActivityLogDto.fromEntity(activityLog));
        } catch (Exception e) {
            log.error("Failed to send activity WebSocket notification", e);
        }

        // Async update risk score và check alerts (chỉ cho activity có risk)
        if (userId != null && riskLevel != RiskLevel.NORMAL) {
            updateRiskScoreAsync(userId, activityType, riskLevel);
        }

        return activityLog;
    }

    /**
     * Log các hoạt động cụ thể với risk scoring tự động
     */
    @Transactional
    public void logLoginSuccess(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityType.LOGIN_SUCCESS, "Đăng nhập thành công", request);
    }

    @Transactional
    public void logLoginFailed(String username, HttpServletRequest request) {
        User user = userRepository.findByUsername(username).orElse(null);
        Long userId = user != null ? user.getId() : null;
        
        logActivity(userId, ActivityType.LOGIN_FAILED, 
            "Đăng nhập thất bại: " + username, RiskLevel.WARNING, null, request);
        
        if (userId != null) {
            incrementRiskScore(userId, ActivityType.LOGIN_FAILED, SCORE_LOGIN_FAILED);
            checkBruteForceAttempt(userId, request);
        }
    }

    @Transactional
    public void logOrderCancel(Long userId, Long orderId, HttpServletRequest request) {
        logActivity(userId, ActivityType.ORDER_CANCEL, 
            "Hủy đơn hàng #" + orderId, RiskLevel.NORMAL, orderId, request);
        
        // Check nếu hủy nhiều đơn trong ngày
        checkOrderCancelAbuse(userId, request);
    }

    @Transactional
    public void logPaymentFailed(Long userId, Long orderId, String reason, HttpServletRequest request) {
        logActivity(userId, ActivityType.PAYMENT_FAILED, 
            "Thanh toán thất bại: " + reason, RiskLevel.WARNING, orderId, request);
        
        incrementRiskScore(userId, ActivityType.PAYMENT_FAILED, SCORE_PAYMENT_FAILED);
        checkPaymentFraud(userId, request);
    }

    @Transactional
    public void logPromotionUse(Long userId, String promoCode, HttpServletRequest request) {
        logActivity(userId, ActivityType.PROMOTION_USE, 
            "Sử dụng mã khuyến mãi: " + promoCode, request);
    }

    @Transactional
    public void logPromotionAbuse(Long userId, String promoCode, String reason, HttpServletRequest request) {
        logActivity(userId, ActivityType.PROMOTION_ABUSE_ATTEMPT, 
            "Lạm dụng khuyến mãi: " + promoCode + " - " + reason, 
            RiskLevel.SUSPICIOUS, null, request);
        
        incrementRiskScore(userId, ActivityType.PROMOTION_ABUSE_ATTEMPT, SCORE_PROMOTION_ABUSE);
        createAlert(userId, AlertType.PROMOTION_ABUSE, AlertSeverity.MEDIUM,
            "Phát hiện lạm dụng khuyến mãi",
            "User đã cố gắng lạm dụng mã khuyến mãi: " + promoCode + ". Lý do: " + reason);
    }

    @Transactional
    public void logRateLimitHit(Long userId, String endpoint, HttpServletRequest request) {
        logActivity(userId, ActivityType.RATE_LIMIT_HIT, 
            "Vượt giới hạn request: " + endpoint, RiskLevel.WARNING, null, request);
        
        if (userId != null) {
            incrementRiskScore(userId, ActivityType.RATE_LIMIT_HIT, SCORE_RATE_LIMIT_HIT);
        }
    }

    @Transactional
    public void logSpamRequest(Long userId, String endpoint, int requestCount, HttpServletRequest request) {
        logActivity(userId, ActivityType.SPAM_REQUEST, 
            "Spam request: " + requestCount + " requests to " + endpoint, 
            RiskLevel.SUSPICIOUS, null, request);
        
        if (userId != null) {
            incrementRiskScore(userId, ActivityType.SPAM_REQUEST, SCORE_SPAM_REQUEST);
            createAlert(userId, AlertType.SPAM_DETECTED, AlertSeverity.HIGH,
                "Phát hiện spam request",
                "User đã gửi " + requestCount + " requests đến " + endpoint + " trong thời gian ngắn");
        }
    }

    @Transactional
    public void logAccountBlocked(Long userId, String reason, Long blockedBy, HttpServletRequest request) {
        logActivity(userId, ActivityType.ACCOUNT_BLOCKED, 
            "Tài khoản bị khóa: " + reason, RiskLevel.CRITICAL, blockedBy, request);
    }

    @Transactional
    public void logAccountUnblocked(Long userId, Long unblockedBy, HttpServletRequest request) {
        logActivity(userId, ActivityType.ACCOUNT_UNBLOCKED, 
            "Tài khoản được mở khóa", RiskLevel.NORMAL, unblockedBy, request);
    }

    // ==================== NORMAL ACTIVITY LOGGING ====================

    /**
     * 🛒 Log tạo đơn hàng
     */
    @Transactional
    public void logOrderCreate(Long userId, Long orderId, Double totalAmount, HttpServletRequest request) {
        RiskLevel riskLevel = RiskLevel.NORMAL;
        
        // 🚨 Alert nếu đơn hàng giá trị cao bất thường (> 2 triệu)
        if (totalAmount != null && totalAmount > 2000000) {
            riskLevel = RiskLevel.WARNING;
            createAlertIfNotExists(userId, AlertType.ORDER_ABUSE, AlertSeverity.LOW,
                "Đơn hàng giá trị cao",
                "User đặt đơn hàng #" + orderId + " với giá trị " + 
                String.format("%,.0f", totalAmount) + "đ. Cần xác minh.");
        }
        
        logActivity(userId, ActivityType.ORDER_CREATE, 
            "Tạo đơn hàng #" + orderId + " - " + String.format("%,.0f", totalAmount) + "đ", 
            riskLevel, orderId, request);
    }

    /**
     * 💳 Log thanh toán thành công
     */
    @Transactional
    public void logPaymentSuccess(Long userId, Long orderId, String paymentMethod, HttpServletRequest request) {
        logActivity(userId, ActivityType.PAYMENT_SUCCESS, 
            "Thanh toán thành công đơn #" + orderId + " qua " + paymentMethod, 
            RiskLevel.NORMAL, orderId, request);
    }

    /**
     * 🛒 Log thêm vào giỏ hàng
     */
    @Transactional
    public void logCartAddItem(Long userId, String productName, int quantity, HttpServletRequest request) {
        RiskLevel riskLevel = RiskLevel.NORMAL;
        
        // 🚨 Alert nếu thêm số lượng lớn bất thường (> 20)
        if (quantity > 20) {
            riskLevel = RiskLevel.WARNING;
            log.warn("User {} added unusual quantity {} of {}", userId, quantity, productName);
        }
        
        logActivity(userId, ActivityType.CART_ADD_ITEM, 
            "Thêm vào giỏ: " + productName + " x" + quantity, riskLevel, null, request);
    }

    /**
     * 🛒 Log xóa khỏi giỏ hàng
     */
    @Transactional
    public void logCartRemoveItem(Long userId, String productName, HttpServletRequest request) {
        logActivity(userId, ActivityType.CART_REMOVE_ITEM, 
            "Xóa khỏi giỏ: " + productName, request);
    }

    /**
     * 👤 Log cập nhật profile
     */
    @Transactional
    public void logProfileUpdate(Long userId, String updatedFields, HttpServletRequest request) {
        logActivity(userId, ActivityType.PROFILE_UPDATE, 
            "Cập nhật thông tin: " + updatedFields, request);
        
        // 🚨 Check nếu thay đổi nhiều lần trong ngày
        checkFrequentProfileChanges(userId, request);
    }
    
    /**
     * 🔐 Log đổi mật khẩu - QUAN TRỌNG
     */
    @Transactional
    public void logPasswordChange(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityType.PASSWORD_CHANGE, 
            "Đổi mật khẩu thành công", RiskLevel.WARNING, null, request);
        
        // 🚨 Tạo alert thông báo cho admin biết
        createAlertIfNotExists(userId, AlertType.SECURITY_VIOLATION, AlertSeverity.LOW,
            "User đổi mật khẩu",
            "User đã thay đổi mật khẩu. Nếu không phải user thực hiện, cần kiểm tra.");
    }
    
    /**
     * 🌐 Log đăng nhập từ IP/thiết bị mới
     */
    @Transactional
    public void logNewDeviceLogin(Long userId, String deviceInfo, String ipAddress, HttpServletRequest request) {
        logActivity(userId, ActivityType.DEVICE_CHANGE, 
            "Đăng nhập từ thiết bị mới: " + deviceInfo + " (IP: " + ipAddress + ")", 
            RiskLevel.WARNING, null, request);
        
        // 🚨 Tạo alert
        createAlertIfNotExists(userId, AlertType.LOGIN_ANOMALY, AlertSeverity.MEDIUM,
            "Đăng nhập từ thiết bị/IP mới",
            "User đăng nhập từ thiết bị: " + deviceInfo + ", IP: " + ipAddress + 
            ". Cần xác minh nếu đây không phải user thật.");
    }
    
    /**
     * 🚨 Check thay đổi profile thường xuyên
     */
    private void checkFrequentProfileChanges(Long userId, HttpServletRequest request) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long changeCount = activityLogRepository.countByUserIdAndActivityTypeSince(
            userId, ActivityType.PROFILE_UPDATE, since);
        
        if (changeCount >= 5) {
            logActivity(userId, ActivityType.SECURITY_VIOLATION, 
                "Thay đổi profile nhiều lần: " + changeCount + " lần trong 24h",
                RiskLevel.SUSPICIOUS, null, request);
            
            createAlertIfNotExists(userId, AlertType.SECURITY_VIOLATION, AlertSeverity.MEDIUM,
                "Thay đổi profile bất thường",
                "User đã thay đổi thông tin profile " + changeCount + " lần trong 24h. Có thể là hành vi đáng ngờ.");
        }
    }

    /**
     * 🔍 Log xem sản phẩm
     */
    @Transactional
    public void logProductView(Long userId, Long productId, String productName, HttpServletRequest request) {
        logActivity(userId, ActivityType.PRODUCT_VIEW, 
            "Xem sản phẩm: " + productName, RiskLevel.NORMAL, productId, request);
    }

    /**
     * 🔍 Log tìm kiếm
     */
    @Transactional
    public void logProductSearch(Long userId, String keyword, HttpServletRequest request) {
        logActivity(userId, ActivityType.PRODUCT_SEARCH, 
            "Tìm kiếm: " + keyword, request);
    }

    /**
     * 👥 Log tạo group order
     */
    @Transactional
    public void logGroupOrderCreate(Long userId, Long groupOrderId, HttpServletRequest request) {
        logActivity(userId, ActivityType.GROUP_ORDER_CREATE, 
            "Tạo đơn nhóm #" + groupOrderId, RiskLevel.NORMAL, groupOrderId, request);
    }

    /**
     * 👥 Log tham gia group order
     */
    @Transactional
    public void logGroupOrderJoin(Long userId, Long groupOrderId, HttpServletRequest request) {
        logActivity(userId, ActivityType.GROUP_ORDER_JOIN, 
            "Tham gia đơn nhóm #" + groupOrderId, RiskLevel.NORMAL, groupOrderId, request);
    }

    /**
     * 💬 Log bắt đầu live chat
     */
    @Transactional
    public void logLiveChatStart(Long userId, Long conversationId, HttpServletRequest request) {
        logActivity(userId, ActivityType.LIVE_CHAT_START, 
            "Bắt đầu chat hỗ trợ #" + conversationId, RiskLevel.NORMAL, conversationId, request);
    }

    /**
     * 🚪 Log đăng xuất
     */
    @Transactional
    public void logLogout(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityType.LOGOUT, "Đăng xuất", request);
    }

    // ==================== RISK SCORE MANAGEMENT ====================

    @Transactional
    public void incrementRiskScore(Long userId, ActivityType activityType, int points) {
        UserRiskScore riskScore = getOrCreateRiskScore(userId);
        
        riskScore.addScore(points);
        
        // Update specific counters
        switch (activityType) {
            case LOGIN_FAILED -> riskScore.setLoginFailedCount(riskScore.getLoginFailedCount() + 1);
            case ORDER_CANCEL, ORDER_CANCEL_MULTIPLE -> 
                riskScore.setOrderCancelCount(riskScore.getOrderCancelCount() + 1);
            case PAYMENT_FAILED, PAYMENT_FAILED_MULTIPLE -> 
                riskScore.setPaymentFailedCount(riskScore.getPaymentFailedCount() + 1);
            case RATE_LIMIT_HIT -> riskScore.setRateLimitHitCount(riskScore.getRateLimitHitCount() + 1);
            case PROMOTION_ABUSE_ATTEMPT -> 
                riskScore.setPromotionAbuseCount(riskScore.getPromotionAbuseCount() + 1);
            case SPAM_REQUEST -> riskScore.setSpamRequestCount(riskScore.getSpamRequestCount() + 1);
            default -> {}
        }
        
        riskScoreRepository.save(riskScore);
        
        // 📡 Gửi WebSocket notification khi risk score thay đổi đáng kể
        if (riskScore.getTotalScore() >= ALERT_THRESHOLD_WARNING) {
            try {
                monitoringWebSocketService.notifyRiskScoreUpdate(UserRiskScoreDto.fromEntity(riskScore));
            } catch (Exception e) {
                log.error("Failed to send risk score WebSocket notification", e);
            }
        }
        
        // Check thresholds và tạo alerts
        checkRiskThresholds(userId, riskScore);
    }

    private UserRiskScore getOrCreateRiskScore(Long userId) {
        return riskScoreRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                UserRiskScore newScore = UserRiskScore.builder()
                    .user(user)
                    .totalScore(0)
                    .riskLevel(RiskLevel.NORMAL)
                    .loginFailedCount(0)
                    .orderCancelCount(0)
                    .paymentFailedCount(0)
                    .rateLimitHitCount(0)
                    .promotionAbuseCount(0)
                    .spamRequestCount(0)
                    .autoBlocked(false)
                    .build();
                return riskScoreRepository.save(newScore);
            });
    }

    private void checkRiskThresholds(Long userId, UserRiskScore riskScore) {
        // Auto-block nếu vượt ngưỡng
        if (riskScore.getTotalScore() >= AUTO_BLOCK_THRESHOLD && !riskScore.getAutoBlocked()) {
            autoBlockUser(userId, riskScore);
        }
        // Tạo alert nếu vượt ngưỡng suspicious
        else if (riskScore.getTotalScore() >= ALERT_THRESHOLD_SUSPICIOUS) {
            createAlertIfNotExists(userId, AlertType.HIGH_RISK_SCORE, AlertSeverity.HIGH,
                "Điểm rủi ro cao: " + riskScore.getTotalScore(),
                "User có điểm rủi ro " + riskScore.getTotalScore() + "/100. Cần xem xét và có thể block.");
        }
        // Tạo alert warning
        else if (riskScore.getTotalScore() >= ALERT_THRESHOLD_WARNING) {
            createAlertIfNotExists(userId, AlertType.HIGH_RISK_SCORE, AlertSeverity.MEDIUM,
                "Điểm rủi ro tăng: " + riskScore.getTotalScore(),
                "User có điểm rủi ro " + riskScore.getTotalScore() + "/100. Cần theo dõi.");
        }
    }

    @Transactional
    public void autoBlockUser(Long userId, UserRiskScore riskScore) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        // Block user
        user.setIsBlocked(true);
        user.setActive(false);
        userRepository.save(user);
        
        // Update risk score
        riskScore.setAutoBlocked(true);
        riskScore.setAutoBlockedAt(LocalDateTime.now());
        riskScore.setAutoBlockedReason("Tự động khóa do điểm rủi ro vượt ngưỡng: " + riskScore.getTotalScore());
        riskScoreRepository.save(riskScore);
        
        // Tạo alert
        createAlert(userId, AlertType.AUTO_BLOCKED, AlertSeverity.CRITICAL,
            "Tài khoản tự động bị khóa",
            "User " + user.getUsername() + " đã bị tự động khóa do điểm rủi ro " + 
            riskScore.getTotalScore() + "/100 vượt ngưỡng " + AUTO_BLOCK_THRESHOLD);
        
        // 📡 Gửi WebSocket notification đặc biệt cho auto-block
        try {
            monitoringWebSocketService.notifyUserAutoBlocked(UserRiskScoreDto.fromEntity(riskScore));
        } catch (Exception e) {
            log.error("Failed to send auto-block WebSocket notification", e);
        }
        
        log.warn("AUTO-BLOCKED user {} due to high risk score: {}", user.getUsername(), riskScore.getTotalScore());
    }

    // ==================== ABUSE DETECTION ====================

    private void checkBruteForceAttempt(Long userId, HttpServletRequest request) {
        Instant since = Instant.now().minus(Duration.ofMinutes(15));
        long failedCount = activityLogRepository.countByUserIdAndActivityTypeSince(
            userId, ActivityType.LOGIN_FAILED, since);
        
        if (failedCount >= 5) {
            logActivity(userId, ActivityType.BRUTE_FORCE_ATTEMPT, 
                "Phát hiện brute force: " + failedCount + " lần thất bại trong 15 phút",
                RiskLevel.CRITICAL, null, request);
            
            incrementRiskScore(userId, ActivityType.BRUTE_FORCE_ATTEMPT, SCORE_BRUTE_FORCE);
            
            createAlert(userId, AlertType.BRUTE_FORCE, AlertSeverity.CRITICAL,
                "Phát hiện tấn công brute force",
                "User đã đăng nhập thất bại " + failedCount + " lần trong 15 phút. IP: " + getClientIp(request));
        }
    }

    private void checkOrderCancelAbuse(Long userId, HttpServletRequest request) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long cancelCount = activityLogRepository.countByUserIdAndActivityTypeSince(
            userId, ActivityType.ORDER_CANCEL, since);
        
        if (cancelCount >= 3) {
            logActivity(userId, ActivityType.ORDER_CANCEL_MULTIPLE, 
                "Hủy nhiều đơn: " + cancelCount + " đơn trong 24h",
                RiskLevel.WARNING, null, request);
            
            incrementRiskScore(userId, ActivityType.ORDER_CANCEL_MULTIPLE, SCORE_ORDER_CANCEL);
            
            if (cancelCount >= 5) {
                createAlert(userId, AlertType.ORDER_ABUSE, AlertSeverity.MEDIUM,
                    "Hủy đơn hàng bất thường",
                    "User đã hủy " + cancelCount + " đơn hàng trong 24h. Cần xem xét.");
            }
        }
    }

    private void checkPaymentFraud(Long userId, HttpServletRequest request) {
        Instant since = Instant.now().minus(Duration.ofHours(1));
        long failedCount = activityLogRepository.countByUserIdAndActivityTypeSince(
            userId, ActivityType.PAYMENT_FAILED, since);
        
        if (failedCount >= 3) {
            logActivity(userId, ActivityType.PAYMENT_FAILED_MULTIPLE, 
                "Thanh toán thất bại nhiều lần: " + failedCount + " lần trong 1h",
                RiskLevel.SUSPICIOUS, null, request);
            
            createAlert(userId, AlertType.PAYMENT_FRAUD, AlertSeverity.HIGH,
                "Nghi ngờ gian lận thanh toán",
                "User đã thanh toán thất bại " + failedCount + " lần trong 1 giờ. Cần kiểm tra.");
        }
    }

    // ==================== ALERT MANAGEMENT ====================

    @Transactional
    public MonitoringAlert createAlert(Long userId, AlertType alertType, AlertSeverity severity,
                                        String title, String message) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        MonitoringAlert alert = MonitoringAlert.builder()
            .targetUser(user)
            .alertType(alertType)
            .severity(severity)
            .title(title)
            .message(message)
            .status(AlertStatus.PENDING)
            .notificationSent(false)
            .build();
        
        alert = alertRepository.save(alert);
        
        // 📡 Gửi WebSocket notification cho alert mới
        try {
            monitoringWebSocketService.notifyNewAlert(MonitoringAlertDto.fromEntity(alert));
        } catch (Exception e) {
            log.error("Failed to send alert WebSocket notification", e);
        }
        
        // Gửi push notification cho Admin/Manager
        sendAlertNotification(alert);
        
        return alert;
    }

    private void createAlertIfNotExists(Long userId, AlertType alertType, AlertSeverity severity,
                                         String title, String message) {
        // Kiểm tra đã có alert tương tự trong 1h chưa
        Instant since = Instant.now().minus(Duration.ofHours(1));
        boolean exists = alertRepository.existsSimilarPendingAlert(userId, alertType, since);
        
        if (!exists) {
            createAlert(userId, alertType, severity, title, message);
        }
    }

    @Async
    public void sendAlertNotification(MonitoringAlert alert) {
        try {
            // Lấy danh sách Admin và Manager
            List<User> adminsAndManagers = userRepository.findByRoleIn(
                List.of(UserRole.ADMIN, UserRole.MANAGER));
            
            if (adminsAndManagers.isEmpty()) {
                log.warn("No admins/managers found to send alert notification");
                return;
            }
            
            String[] userIds = adminsAndManagers.stream()
                .map(u -> u.getId().toString())
                .toArray(String[]::new);
            
            String notifTitle = getAlertNotificationTitle(alert);
            String notifContent = alert.getTitle() + ": " + alert.getTargetUser().getUsername();
            
            oneSignalService.sendToMultipleUsers(userIds, notifTitle, notifContent,
                NotificationType.SYSTEM, alert.getId());
            
            // Update alert
            alert.setNotificationSent(true);
            alertRepository.save(alert);
            
            log.info("Alert notification sent to {} admins/managers", userIds.length);
        } catch (Exception e) {
            log.error("Failed to send alert notification", e);
        }
    }

    private String getAlertNotificationTitle(MonitoringAlert alert) {
        String emoji = switch (alert.getSeverity()) {
            case CRITICAL -> "🚨";
            case HIGH -> "⚠️";
            case MEDIUM -> "🟡";
            case LOW -> "🔵";
        };
        return emoji + " Cảnh báo bảo mật";
    }

    @Transactional
    public MonitoringAlert handleAlert(Long alertId, AlertStatus newStatus, 
                                        ActionTaken actionTaken, String note) {
        MonitoringAlert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        
        User handler = getCurrentUser();
        
        alert.setStatus(newStatus);
        alert.setActionTaken(actionTaken);
        alert.setHandlerNote(note);
        alert.setHandledBy(handler);
        alert.setHandledAt(LocalDateTime.now());
        
        // Nếu action là block user
        if (actionTaken == ActionTaken.TEMP_BLOCKED || actionTaken == ActionTaken.PERM_BLOCKED) {
            User targetUser = alert.getTargetUser();
            targetUser.setIsBlocked(true);
            targetUser.setActive(false);
            userRepository.save(targetUser);
            
            logAccountBlocked(targetUser.getId(), note, handler.getId(), null);
        }
        
        return alertRepository.save(alert);
    }

    // ==================== DASHBOARD & QUERIES ====================

    @Transactional(readOnly = true)
    public MonitoringDashboardDto getDashboard() {
        Instant last24h = Instant.now().minus(Duration.ofHours(24));
        
        MonitoringDashboardDto dashboard = new MonitoringDashboardDto();
        
        // Alert counts
        dashboard.setTotalPendingAlerts(alertRepository.countByStatus(AlertStatus.PENDING));
        dashboard.setCriticalAlerts(alertRepository.countByStatusAndSeverity(
            AlertStatus.PENDING, AlertSeverity.CRITICAL));
        dashboard.setHighAlerts(alertRepository.countByStatusAndSeverity(
            AlertStatus.PENDING, AlertSeverity.HIGH));
        dashboard.setMediumAlerts(alertRepository.countByStatusAndSeverity(
            AlertStatus.PENDING, AlertSeverity.MEDIUM));
        dashboard.setLowAlerts(alertRepository.countByStatusAndSeverity(
            AlertStatus.PENDING, AlertSeverity.LOW));
        
        // User risk counts
        dashboard.setNormalUsers(riskScoreRepository.countByRiskLevel(RiskLevel.NORMAL));
        dashboard.setWarningUsers(riskScoreRepository.countByRiskLevel(RiskLevel.WARNING));
        dashboard.setSuspiciousUsers(riskScoreRepository.countByRiskLevel(RiskLevel.SUSPICIOUS));
        dashboard.setCriticalUsers(riskScoreRepository.countByRiskLevel(RiskLevel.CRITICAL));
        
        // Activity stats
        List<Object[]> activityStats = activityLogRepository.countByActivityTypeSince(last24h);
        Map<String, Long> activityTypeStats = new HashMap<>();
        long totalActivities = 0;
        for (Object[] row : activityStats) {
            String type = ((ActivityType) row[0]).name();
            Long count = (Long) row[1];
            activityTypeStats.put(type, count);
            totalActivities += count;
        }
        dashboard.setActivityTypeStats(activityTypeStats);
        dashboard.setTotalActivities24h(totalActivities);
        
        // Risk level stats
        List<Object[]> riskStats = activityLogRepository.countByRiskLevelSince(last24h);
        Map<String, Long> riskLevelStats = new HashMap<>();
        long suspiciousActivities = 0;
        for (Object[] row : riskStats) {
            RiskLevel level = (RiskLevel) row[0];
            Long count = (Long) row[1];
            riskLevelStats.put(level.name(), count);
            if (level == RiskLevel.SUSPICIOUS || level == RiskLevel.CRITICAL) {
                suspiciousActivities += count;
            }
        }
        dashboard.setRiskLevelStats(riskLevelStats);
        dashboard.setSuspiciousActivities24h(suspiciousActivities);
        
        // Top risky users
        Page<UserRiskScore> topRisky = riskScoreRepository.findAllByOrderByTotalScoreDesc(
            PageRequest.of(0, 10));
        dashboard.setTopRiskyUsers(topRisky.getContent().stream()
            .map(UserRiskScoreDto::fromEntity)
            .collect(Collectors.toList()));
        
        // Recent alerts
        Page<MonitoringAlert> recentAlerts = alertRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(0, 10));
        dashboard.setRecentAlerts(recentAlerts.getContent().stream()
            .map(MonitoringAlertDto::fromEntity)
            .collect(Collectors.toList()));
        
        // Recent activities
        Page<UserActivityLog> recentActivities = activityLogRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(0, 20));
        dashboard.setRecentActivities(recentActivities.getContent().stream()
            .map(UserActivityLogDto::fromEntity)
            .collect(Collectors.toList()));
        
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogDto> getActivityLogs(Long userId, ActivityType activityType,
                                                     RiskLevel riskLevel, Instant startDate,
                                                     Instant endDate, Pageable pageable) {
        return activityLogRepository.findByFilters(userId, activityType, riskLevel, 
            startDate, endDate, pageable)
            .map(UserActivityLogDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogDto> getUserActivityLogs(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(UserActivityLogDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MonitoringAlertDto> getAlerts(Long userId, AlertType alertType,
                                               AlertSeverity severity, AlertStatus status,
                                               Instant startDate, Instant endDate, Pageable pageable) {
        return alertRepository.findByFilters(userId, alertType, severity, status,
            startDate, endDate, pageable)
            .map(MonitoringAlertDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<MonitoringAlertDto> getPendingAlerts(Pageable pageable) {
        return alertRepository.findByStatusInOrderByCreatedAtDesc(
            List.of(AlertStatus.PENDING, AlertStatus.REVIEWING), pageable)
            .map(MonitoringAlertDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<UserRiskScoreDto> getRiskScores(RiskLevel riskLevel, Pageable pageable) {
        if (riskLevel != null) {
            return riskScoreRepository.findByRiskLevelOrderByTotalScoreDesc(riskLevel, pageable)
                .map(UserRiskScoreDto::fromEntity);
        }
        return riskScoreRepository.findAllByOrderByTotalScoreDesc(pageable)
            .map(UserRiskScoreDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserRiskScoreDto getUserRiskScore(Long userId) {
        return riskScoreRepository.findByUserId(userId)
            .map(UserRiskScoreDto::fromEntity)
            .orElse(null);
    }

    // ==================== ADMIN ACTIONS ====================

    @Transactional
    public UserRiskScoreDto addAdminNote(Long userId, String note) {
        UserRiskScore riskScore = getOrCreateRiskScore(userId);
        User admin = getCurrentUser();
        
        riskScore.setAdminNote(note);
        riskScore.setNotedBy(admin.getId());
        riskScore.setNotedAt(LocalDateTime.now());
        
        return UserRiskScoreDto.fromEntity(riskScoreRepository.save(riskScore));
    }

    @Transactional
    public UserRiskScoreDto resetRiskScore(Long userId) {
        UserRiskScore riskScore = riskScoreRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Risk score not found for user: " + userId));
        
        riskScore.setTotalScore(0);
        riskScore.setRiskLevel(RiskLevel.NORMAL);
        riskScore.setLoginFailedCount(0);
        riskScore.setOrderCancelCount(0);
        riskScore.setPaymentFailedCount(0);
        riskScore.setRateLimitHitCount(0);
        riskScore.setPromotionAbuseCount(0);
        riskScore.setSpamRequestCount(0);
        riskScore.setLastScoreReset(LocalDateTime.now());
        
        log.info("Risk score reset for user {}", userId);
        return UserRiskScoreDto.fromEntity(riskScoreRepository.save(riskScore));
    }

    @Transactional
    public void unblockUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        user.setIsBlocked(false);
        user.setActive(true);
        userRepository.save(user);
        
        // Update risk score
        riskScoreRepository.findByUserId(userId).ifPresent(riskScore -> {
            riskScore.setAutoBlocked(false);
            riskScore.setAutoBlockedAt(null);
            riskScore.setAutoBlockedReason(null);
            riskScoreRepository.save(riskScore);
        });
        
        logAccountUnblocked(userId, getCurrentUser().getId(), null);
        log.info("User {} unblocked. Reason: {}", userId, reason);
    }

    // ==================== HELPER METHODS ====================

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Lấy IP đầu tiên nếu có nhiều IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getDeviceInfo(HttpServletRequest request) {
        if (request == null) return null;
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return null;
        
        // Parse basic device info from User-Agent
        if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    @Async
    private void updateRiskScoreAsync(Long userId, ActivityType activityType, RiskLevel riskLevel) {
        // Async processing để không block main thread
        try {
            int points = switch (riskLevel) {
                case WARNING -> 5;
                case SUSPICIOUS -> 15;
                case CRITICAL -> 30;
                default -> 0;
            };
            if (points > 0) {
                incrementRiskScore(userId, activityType, points);
            }
        } catch (Exception e) {
            log.error("Error updating risk score async for user {}", userId, e);
        }
    }
}
