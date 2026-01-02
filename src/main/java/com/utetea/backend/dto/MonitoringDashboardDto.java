package com.utetea.backend.dto;

import com.utetea.backend.model.MonitoringAlert.AlertSeverity;
import com.utetea.backend.model.UserActivityLog.RiskLevel;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard tổng quan cho User Monitoring
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonitoringDashboardDto {
    
    // Tổng quan alerts
    private Long totalPendingAlerts;
    private Long criticalAlerts;
    private Long highAlerts;
    private Long mediumAlerts;
    private Long lowAlerts;
    
    // Tổng quan users theo risk level
    private Long normalUsers;
    private Long warningUsers;
    private Long suspiciousUsers;
    private Long criticalUsers;
    
    // Thống kê hoạt động 24h qua
    private Long totalActivities24h;
    private Long suspiciousActivities24h;
    private Long blockedUsers24h;
    
    // Top users có điểm rủi ro cao
    private List<UserRiskScoreDto> topRiskyUsers;
    
    // Alerts gần đây
    private List<MonitoringAlertDto> recentAlerts;
    
    // Activities gần đây
    private List<UserActivityLogDto> recentActivities;
    
    // Thống kê theo loại activity
    private Map<String, Long> activityTypeStats;
    
    // Thống kê theo risk level
    private Map<String, Long> riskLevelStats;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AlertSummary {
        private AlertSeverity severity;
        private Long count;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RiskSummary {
        private RiskLevel riskLevel;
        private Long count;
    }
}
