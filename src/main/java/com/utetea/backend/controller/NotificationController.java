package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.service.OneSignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "🔔 Notifications", description = "API quản lý thông báo đẩy")
public class NotificationController {

    private final OneSignalService oneSignalService;

    @PostMapping("/send")
    @Operation(summary = "Gửi thông báo", description = "Gửi push notification cho users (Manager only)")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> sendNotification(@RequestBody NotificationRequest request) {
        // Validate input
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tiêu đề không được để trống"));
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Nội dung không được để trống"));
        }
        
        log.info("Sending notification: title={}, sendAll={}, userIds={}", 
                request.getTitle(), request.isSendAll(), 
                request.getUserIds() != null ? request.getUserIds().size() : 0);
        
        try {
            if (request.isSendAll()) {
                oneSignalService.sendToAll(request.getTitle(), request.getContent());
            } else if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                String[] userIdsArray = request.getUserIds().toArray(new String[0]);
                oneSignalService.sendToMultipleUsers(userIdsArray, request.getTitle(), request.getContent());
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn đối tượng nhận thông báo"));
            }
            return ResponseEntity.ok(ApiResponse.success("Đã gửi thông báo thành công"));
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi thông báo: " + e.getMessage()));
        }
    }

    // DTO - Hỗ trợ cả List<String> từ Android
    @Data
    public static class NotificationRequest {
        private String title;
        private String content;
        private boolean sendAll;
        private List<String> userIds; // Đổi từ String[] sang List<String> để tương thích với Android
    }
}