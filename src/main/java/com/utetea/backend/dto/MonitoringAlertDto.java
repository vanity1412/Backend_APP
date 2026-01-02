package com.utetea.backend.dto;

import com.utetea.backend.model.MonitoringAlert;
import com.utetea.backend.model.MonitoringAlert.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonitoringAlertDto {
    
    private Long id;
    
    // Target user info
    private Long targetUserId;
    private String targetUsername;
    private String targetUserEmail;
    private String targetUserFullName;
    private String targetUserAvatarUrl;
    
    private AlertType alertType;
    private String alertTypeDisplay;
    private AlertSeverity severity;
    private String severityDisplay;
    
    private String title;
    private String message;
    
    private AlertStatus status;
    private String statusDisplay;
    
    // Handler info
    private Long handledById;
    private String handledByUsername;
    private LocalDateTime handledAt;
    private String handlerNote;
    private ActionTaken actionTaken;
    private String actionTakenDisplay;
    
    private Long activityLogId;
    private Boolean notificationSent;
    
    private LocalDateTime createdAt;

    public static MonitoringAlertDto fromEntity(MonitoringAlert alert) {
        MonitoringAlertDto dto = new MonitoringAlertDto();
        dto.setId(alert.getId());
        
        if (alert.getTargetUser() != null) {
            dto.setTargetUserId(alert.getTargetUser().getId());
            dto.setTargetUsername(alert.getTargetUser().getUsername());
            dto.setTargetUserEmail(alert.getTargetUser().getEmail());
            dto.setTargetUserFullName(alert.getTargetUser().getFullName());
            dto.setTargetUserAvatarUrl(alert.getTargetUser().getAvatarUrl());
        }
        
        dto.setAlertType(alert.getAlertType());
        dto.setAlertTypeDisplay(getAlertTypeDisplay(alert.getAlertType()));
        dto.setSeverity(alert.getSeverity());
        dto.setSeverityDisplay(getSeverityDisplay(alert.getSeverity()));
        
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        
        dto.setStatus(alert.getStatus());
        dto.setStatusDisplay(getStatusDisplay(alert.getStatus()));
        
        if (alert.getHandledBy() != null) {
            dto.setHandledById(alert.getHandledBy().getId());
            dto.setHandledByUsername(alert.getHandledBy().getUsername());
        }
        dto.setHandledAt(alert.getHandledAt());
        dto.setHandlerNote(alert.getHandlerNote());
        dto.setActionTaken(alert.getActionTaken());
        dto.setActionTakenDisplay(getActionTakenDisplay(alert.getActionTaken()));
        
        dto.setActivityLogId(alert.getActivityLogId());
        dto.setNotificationSent(alert.getNotificationSent());
        
        if (alert.getCreatedAt() != null) {
            dto.setCreatedAt(alert.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return dto;
    }

    private static String getAlertTypeDisplay(AlertType type) {
        if (type == null) return "";
        return switch (type) {
            case LOGIN_ANOMALY -> "Đăng nhập bất thường";
            case ORDER_ABUSE -> "Lạm dụng đặt hàng";
            case PAYMENT_FRAUD -> "Gian lận thanh toán";
            case PROMOTION_ABUSE -> "Lạm dụng khuyến mãi";
            case RATE_LIMIT_EXCEEDED -> "Vượt giới hạn request";
            case SPAM_DETECTED -> "Phát hiện spam";
            case BRUTE_FORCE -> "Tấn công brute force";
            case HIGH_RISK_SCORE -> "Điểm rủi ro cao";
            case AUTO_BLOCKED -> "Tự động khóa";
            case SECURITY_VIOLATION -> "Vi phạm bảo mật";
        };
    }

    private static String getSeverityDisplay(AlertSeverity severity) {
        if (severity == null) return "";
        return switch (severity) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case CRITICAL -> "Nghiêm trọng";
        };
    }

    private static String getStatusDisplay(AlertStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case REVIEWING -> "Đang xem xét";
            case RESOLVED -> "Đã xử lý";
            case DISMISSED -> "Đã bỏ qua";
        };
    }

    private static String getActionTakenDisplay(ActionTaken action) {
        if (action == null) return "";
        return switch (action) {
            case NONE -> "Không có";
            case WARNING_SENT -> "Đã gửi cảnh báo";
            case TEMP_BLOCKED -> "Tạm khóa";
            case PERM_BLOCKED -> "Khóa vĩnh viễn";
            case MONITORED -> "Theo dõi";
            case FALSE_POSITIVE -> "Cảnh báo sai";
        };
    }
}
