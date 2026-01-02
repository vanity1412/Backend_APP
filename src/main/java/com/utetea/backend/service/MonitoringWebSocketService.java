package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 🛡️ MONITORING WEBSOCKET SERVICE
 * Service gửi thông báo realtime cho hệ thống giám sát
 * Admin/Manager subscribe các topic để nhận cập nhật tức thì
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==================== TOPICS ====================
    // /topic/monitoring/alerts      - Cảnh báo mới
    // /topic/monitoring/activities  - Hoạt động mới
    // /topic/monitoring/risk-scores - Cập nhật điểm rủi ro
    // /topic/monitoring/dashboard   - Cập nhật dashboard stats

    /**
     * 🚨 Gửi cảnh báo mới đến tất cả Admin/Manager
     */
    public void notifyNewAlert(MonitoringAlertDto alert) {
        try {
            log.info("📡 Broadcasting new alert: {} - {}", alert.getAlertType(), alert.getTitle());
            messagingTemplate.convertAndSend("/topic/monitoring/alerts", alert);
        } catch (Exception e) {
            log.error("Failed to send alert via WebSocket", e);
        }
    }

    /**
     * 📋 Gửi activity log mới (chỉ các hoạt động đáng chú ý)
     */
    public void notifyNewActivity(UserActivityLogDto activity) {
        try {
            // Chỉ broadcast các activity có risk level >= WARNING
            if (activity.getRiskLevel() != null && 
                !activity.getRiskLevel().equals("NORMAL")) {
                log.info("📡 Broadcasting suspicious activity: {} - {}", 
                    activity.getActivityType(), activity.getRiskLevel());
                messagingTemplate.convertAndSend("/topic/monitoring/activities", activity);
            }
        } catch (Exception e) {
            log.error("Failed to send activity via WebSocket", e);
        }
    }

    /**
     * ⚠️ Gửi cập nhật điểm rủi ro
     */
    public void notifyRiskScoreUpdate(UserRiskScoreDto riskScore) {
        try {
            log.info("📡 Broadcasting risk score update: user {} - score {}", 
                riskScore.getUsername(), riskScore.getTotalScore());
            messagingTemplate.convertAndSend("/topic/monitoring/risk-scores", riskScore);
        } catch (Exception e) {
            log.error("Failed to send risk score via WebSocket", e);
        }
    }

    /**
     * 📊 Gửi cập nhật dashboard stats
     */
    public void notifyDashboardUpdate(MonitoringDashboardDto dashboard) {
        try {
            log.info("📡 Broadcasting dashboard update");
            messagingTemplate.convertAndSend("/topic/monitoring/dashboard", dashboard);
        } catch (Exception e) {
            log.error("Failed to send dashboard via WebSocket", e);
        }
    }

    /**
     * 🔒 Gửi thông báo user bị auto-block
     */
    public void notifyUserAutoBlocked(UserRiskScoreDto riskScore) {
        try {
            log.warn("📡 Broadcasting AUTO-BLOCK: user {} - score {}", 
                riskScore.getUsername(), riskScore.getTotalScore());
            
            // Gửi qua cả 2 channel
            messagingTemplate.convertAndSend("/topic/monitoring/risk-scores", riskScore);
            
            // Tạo alert message đặc biệt
            MonitoringAlertDto blockAlert = new MonitoringAlertDto();
            blockAlert.setAlertType(com.utetea.backend.model.MonitoringAlert.AlertType.AUTO_BLOCKED);
            blockAlert.setSeverity(com.utetea.backend.model.MonitoringAlert.AlertSeverity.CRITICAL);
            blockAlert.setTitle("🚨 User tự động bị khóa");
            blockAlert.setMessage("User " + riskScore.getUsername() + 
                " đã bị tự động khóa do điểm rủi ro " + riskScore.getTotalScore() + "/100");
            blockAlert.setTargetUsername(riskScore.getUsername());
            blockAlert.setTargetUserId(riskScore.getUserId());
            
            messagingTemplate.convertAndSend("/topic/monitoring/alerts", blockAlert);
        } catch (Exception e) {
            log.error("Failed to send auto-block notification via WebSocket", e);
        }
    }
}
