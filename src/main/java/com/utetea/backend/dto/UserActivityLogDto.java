package com.utetea.backend.dto;

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
            case ORDER_CANCEL -> "Hủy đơn hàng";
            case ORDER_CANCEL_MULTIPLE -> "Hủy nhiều đơn hàng";
            case PAYMENT_SUCCESS -> "Thanh toán thành công";
            case PAYMENT_FAILED -> "Thanh toán thất bại";
            case PAYMENT_FAILED_MULTIPLE -> "Thanh toán thất bại nhiều lần";
            case PROMOTION_USE -> "Sử dụng khuyến mãi";
            case PROMOTION_ABUSE_ATTEMPT -> "Lạm dụng khuyến mãi";
            case RATE_LIMIT_HIT -> "Vượt giới hạn request";
            case SPAM_REQUEST -> "Spam request";
            case PROFILE_UPDATE -> "Cập nhật thông tin";
            case ACCOUNT_BLOCKED -> "Tài khoản bị khóa";
            case ACCOUNT_UNBLOCKED -> "Tài khoản được mở khóa";
            case BRUTE_FORCE_ATTEMPT -> "Tấn công brute force";
            case UNUSUAL_LOCATION -> "Vị trí bất thường";
            case DEVICE_CHANGE -> "Thay đổi thiết bị";
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
}
