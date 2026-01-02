package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.model.MonitoringAlert.*;
import com.utetea.backend.model.UserActivityLog.*;
import com.utetea.backend.service.UserMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 🛡️ USER MONITORING CONTROLLER
 * API endpoints cho hệ thống giám sát hành vi người dùng
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🛡️ User Monitoring", description = "API giám sát hành vi người dùng")
@Slf4j
public class UserMonitoringController {

    private final UserMonitoringService monitoringService;

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get Monitoring Dashboard", description = "Lấy tổng quan dashboard giám sát")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MonitoringDashboardDto>> getDashboard() {
        log.info("GET /api/monitoring/dashboard");
        MonitoringDashboardDto dashboard = monitoringService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard loaded", dashboard));
    }

    // ==================== ACTIVITY LOGS ====================

    @GetMapping("/activities")
    @Operation(summary = "Get Activity Logs", description = "Lấy danh sách log hoạt động")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserActivityLogDto>>> getActivityLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/monitoring/activities - userId: {}, type: {}, risk: {}", 
            userId, activityType, riskLevel);
        
        ActivityType type = activityType != null ? ActivityType.valueOf(activityType) : null;
        RiskLevel risk = riskLevel != null ? RiskLevel.valueOf(riskLevel) : null;
        Instant start = startDate != null ? startDate.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant end = endDate != null ? endDate.atZone(ZoneId.systemDefault()).toInstant() : null;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLogDto> logs = monitoringService.getActivityLogs(
            userId, type, risk, start, end, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/activities/user/{userId}")
    @Operation(summary = "Get User Activity Logs", description = "Lấy log hoạt động của user cụ thể")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserActivityLogDto>>> getUserActivityLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/monitoring/activities/user/{}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLogDto> logs = monitoringService.getUserActivityLogs(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ==================== ALERTS ====================

    @GetMapping("/alerts")
    @Operation(summary = "Get Alerts", description = "Lấy danh sách cảnh báo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MonitoringAlertDto>>> getAlerts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/monitoring/alerts - userId: {}, type: {}, severity: {}, status: {}", 
            userId, alertType, severity, status);
        
        AlertType type = alertType != null ? AlertType.valueOf(alertType) : null;
        AlertSeverity sev = severity != null ? AlertSeverity.valueOf(severity) : null;
        AlertStatus stat = status != null ? AlertStatus.valueOf(status) : null;
        Instant start = startDate != null ? startDate.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant end = endDate != null ? endDate.atZone(ZoneId.systemDefault()).toInstant() : null;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MonitoringAlertDto> alerts = monitoringService.getAlerts(
            userId, type, sev, stat, start, end, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/alerts/pending")
    @Operation(summary = "Get Pending Alerts", description = "Lấy danh sách cảnh báo chờ xử lý")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MonitoringAlertDto>>> getPendingAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/monitoring/alerts/pending");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MonitoringAlertDto> alerts = monitoringService.getPendingAlerts(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PutMapping("/alerts/{alertId}/handle")
    @Operation(summary = "Handle Alert", description = "Xử lý cảnh báo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MonitoringAlertDto>> handleAlert(
            @PathVariable Long alertId,
            @RequestBody HandleAlertRequest request) {
        
        log.info("PUT /api/monitoring/alerts/{}/handle - status: {}, action: {}", 
            alertId, request.getStatus(), request.getActionTaken());
        
        AlertStatus status = AlertStatus.valueOf(request.getStatus());
        ActionTaken action = request.getActionTaken() != null ? 
            ActionTaken.valueOf(request.getActionTaken()) : ActionTaken.NONE;
        
        MonitoringAlertDto alert = MonitoringAlertDto.fromEntity(
            monitoringService.handleAlert(alertId, status, action, request.getNote()));
        
        return ResponseEntity.ok(ApiResponse.success("Alert handled", alert));
    }

    // ==================== RISK SCORES ====================

    @GetMapping("/risk-scores")
    @Operation(summary = "Get Risk Scores", description = "Lấy danh sách điểm rủi ro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserRiskScoreDto>>> getRiskScores(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/monitoring/risk-scores - riskLevel: {}", riskLevel);
        
        RiskLevel level = riskLevel != null ? RiskLevel.valueOf(riskLevel) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<UserRiskScoreDto> scores = monitoringService.getRiskScores(level, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    @GetMapping("/risk-scores/user/{userId}")
    @Operation(summary = "Get User Risk Score", description = "Lấy điểm rủi ro của user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRiskScoreDto>> getUserRiskScore(@PathVariable Long userId) {
        log.info("GET /api/monitoring/risk-scores/user/{}", userId);
        
        UserRiskScoreDto score = monitoringService.getUserRiskScore(userId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    @PostMapping("/risk-scores/user/{userId}/note")
    @Operation(summary = "Add Admin Note", description = "Thêm ghi chú của admin cho user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRiskScoreDto>> addAdminNote(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        
        log.info("POST /api/monitoring/risk-scores/user/{}/note", userId);
        
        String note = request.get("note");
        UserRiskScoreDto score = monitoringService.addAdminNote(userId, note);
        
        return ResponseEntity.ok(ApiResponse.success("Note added", score));
    }

    @PostMapping("/risk-scores/user/{userId}/reset")
    @Operation(summary = "Reset Risk Score", description = "Reset điểm rủi ro của user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRiskScoreDto>> resetRiskScore(@PathVariable Long userId) {
        log.info("POST /api/monitoring/risk-scores/user/{}/reset", userId);
        
        UserRiskScoreDto score = monitoringService.resetRiskScore(userId);
        return ResponseEntity.ok(ApiResponse.success("Risk score reset", score));
    }

    // ==================== USER ACTIONS ====================

    @PostMapping("/users/{userId}/unblock")
    @Operation(summary = "Unblock User", description = "Mở khóa tài khoản user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> unblockUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        
        log.info("POST /api/monitoring/users/{}/unblock", userId);
        
        String reason = request.getOrDefault("reason", "Admin unblock");
        monitoringService.unblockUser(userId, reason);
        
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully"));
    }

    // ==================== REQUEST DTOs ====================

    @lombok.Data
    public static class HandleAlertRequest {
        private String status;
        private String actionTaken;
        private String note;
    }
}
