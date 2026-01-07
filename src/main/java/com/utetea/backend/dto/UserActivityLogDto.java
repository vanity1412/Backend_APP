package com.utetea.backend.dto;

import com.utetea.backend.model.DeletedUserActivityLogBackup;
import com.utetea.backend.model.UserActivityLog;
import com.utetea.backend.model.UserActivityLog.ActivityType;
import com.utetea.backend.model.UserActivityLog.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserActivityLogDto {
    
    private Long id;
    private Long userId;
    private String username;
    private String userEmail;
    private String userFullName;
    private String userAvatarUrl;
    
    // Flag để đánh dấu đây là log của user đã xóa
    private Boolean isDeletedUser;
    
    private ActivityType activityType;
    private String activityTypeDisplay;
    private String description;
    private RiskLevel riskLevel;
    private String riskLevelDisplay;
    
    private String ipAddress;
    private String deviceInfo;
    private String userAgent;
    private String endpoint;
    private String requestMethod;
    private Integer responseStatus;
    
    private Long relatedId;
    private String extraData;
    
    private LocalDateTime createdAt;

    public static UserActivityLogDto fromEntity(UserActivityLog log) {
        UserActivityLogDto dto = new UserActivityLogDto();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setIsDeletedUser(false);
        
        if (log.getUser() != null) {
            dto.setUsername(log.getUser().getUsername());
            dto.setUserEmail(log.getUser().getEmail());
            dto.setUserFullName(log.getUser().getFullName());
            dto.setUserAvatarUrl(log.getUser().getAvatarUrl());
        }
        
        dto.setActivityType(log.getActivityType());
        dto.setActivityTypeDisplay(getActivityTypeDisplay(log.getActivityType()));
        dto.setDescription(log.getDescription());
        dto.setRiskLevel(log.getRiskLevel());
        dto.setRiskLevelDisplay(getRiskLevelDisplay(log.getRiskLevel()));
        
        dto.setIpAddress(log.getIpAddress());
        dto.setDeviceInfo(log.getDeviceInfo());
        dto.setUserAgent(log.getUserAgent());
        dto.setEndpoint(log.getEndpoint());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setResponseStatus(log.getResponseStatus());
        
        dto.setRelatedId(log.getRelatedId());
        dto.setExtraData(log.getExtraData());
        
        if (log.getCreatedAt() != null) {
            dto.setCreatedAt(log.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return dto;
    }

    private static String getActivityTypeDisplay(ActivityType type) {
        if (type == null) return "";
        return switch (type) {
            case LOGIN_SUCCESS -> "Đăng nhập thành công";
            case LOGIN_FAILED -> "Đăng nhập thất bại";
            case LOGOUT -> "Đăng xuất";
            case PASSWORD_CHANGE -> "Đổi mật khẩu";
            case PASSWORD_RESET_REQUEST -> "Yêu cầu reset mật khẩu";
            case ORDER_CREATE -> "Tạo đơn hàng";
            case ORDER_VIEW -> "Xem đơn hàng";
            case ORDER_CANCEL -> "Hủy đơn hàng";
            case ORDER_CANCEL_MULTIPLE -> "Hủy nhiều đơn hàng";
            case ORDER_STATUS_UPDATE -> "Cập nhật trạng thái đơn";
            case PAYMENT_SUCCESS -> "Thanh toán thành công";
            case PAYMENT_FAILED -> "Thanh toán thất bại";
            case PAYMENT_FAILED_MULTIPLE -> "Thanh toán thất bại nhiều lần";
            case PROMOTION_USE -> "Sử dụng khuyến mãi";
            case PROMOTION_ABUSE_ATTEMPT -> "Lạm dụng khuyến mãi";
            case CART_ADD_ITEM -> "Thêm vào giỏ hàng";
            case CART_REMOVE_ITEM -> "Xóa khỏi giỏ hàng";
            case CART_UPDATE_QUANTITY -> "Cập nhật số lượng";
            case CART_CLEAR -> "Xóa giỏ hàng";
            case PRODUCT_VIEW -> "Xem sản phẩm";
            case PRODUCT_SEARCH -> "Tìm kiếm sản phẩm";
            case PROFILE_VIEW -> "Xem thông tin cá nhân";
            case PROFILE_UPDATE -> "Cập nhật thông tin";
            case AVATAR_UPDATE -> "Cập nhật ảnh đại diện";
            case RATE_LIMIT_HIT -> "Vượt giới hạn request";
            case SPAM_REQUEST -> "Spam request";
            case ACCOUNT_BLOCKED -> "Tài khoản bị khóa";
            case ACCOUNT_UNBLOCKED -> "Tài khoản được mở khóa";
            case BRUTE_FORCE_ATTEMPT -> "Tấn công brute force";
            case UNUSUAL_LOCATION -> "Vị trí bất thường";
            case DEVICE_CHANGE -> "Thay đổi thiết bị";
            case GROUP_ORDER_CREATE -> "Tạo đơn nhóm";
            case GROUP_ORDER_JOIN -> "Tham gia đơn nhóm";
            case GROUP_ORDER_LEAVE -> "Rời đơn nhóm";
            case GROUP_ORDER_CHAT -> "Chat đơn nhóm";
            case LIVE_CHAT_START -> "Bắt đầu chat hỗ trợ";
            case LIVE_CHAT_MESSAGE -> "Gửi tin nhắn hỗ trợ";
            case API_ERROR -> "Lỗi API";
            case SECURITY_VIOLATION -> "Vi phạm bảo mật";
        };
    }

    private static String getRiskLevelDisplay(RiskLevel level) {
        if (level == null) return "";
        return switch (level) {
            case NORMAL -> "Bình thường";
            case WARNING -> "Cảnh báo";
            case SUSPICIOUS -> "Đáng ngờ";
            case CRITICAL -> "Nghiêm trọng";
        };
    }

    /**
     * Convert từ backup entity (user đã xóa) sang DTO
     * Để hiển thị chung với activity logs của user còn tồn tại
     */
    public static UserActivityLogDto fromBackupEntity(DeletedUserActivityLogBackup backup) {
        UserActivityLogDto dto = new UserActivityLogDto();
        dto.setId(backup.getId());
        dto.setUserId(backup.getDeletedUserId());
        dto.setUsername(backup.getDeletedUsername());
        dto.setUserFullName(backup.getDeletedUsername() + " (đã xóa)");
        dto.setIsDeletedUser(true);
        
        // Parse activity type từ string
        ActivityType activityType = null;
        if (backup.getActivityType() != null) {
            try {
                activityType = ActivityType.valueOf(backup.getActivityType());
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }
        dto.setActivityType(activityType);
        dto.setActivityTypeDisplay(getActivityTypeDisplay(activityType));
        dto.setDescription(backup.getDescription());
        
        // Parse risk level từ string
        RiskLevel riskLevel = null;
        if (backup.getRiskLevel() != null) {
            try {
                riskLevel = RiskLevel.valueOf(backup.getRiskLevel());
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }
        dto.setRiskLevel(riskLevel);
        dto.setRiskLevelDisplay(getRiskLevelDisplay(riskLevel));
        
        dto.setIpAddress(backup.getIpAddress());
        dto.setDeviceInfo(backup.getDeviceInfo());
        dto.setUserAgent(backup.getUserAgent());
        dto.setEndpoint(backup.getEndpoint());
        dto.setRequestMethod(backup.getRequestMethod());
        dto.setResponseStatus(backup.getResponseStatus());
        
        dto.setRelatedId(backup.getRelatedId());
        dto.setExtraData(backup.getExtraData());
        
        if (backup.getActivityCreatedAt() != null) {
            dto.setCreatedAt(backup.getActivityCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return dto;
    }
}
