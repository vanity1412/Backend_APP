package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.service.OneSignalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final OneSignalService oneSignalService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> sendNotification(@RequestBody NotificationRequest request) {
        if (request.isSendAll()) {
            oneSignalService.sendToAll(request.getTitle(), request.getContent());
        } else if (request.getUserIds() != null && request.getUserIds().length > 0) {
            oneSignalService.sendToMultipleUsers(request.getUserIds(), request.getTitle(), request.getContent());
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn đối tượng nhận tin"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã gửi thông báo thành công", null));
    }

    // DTO nội bộ
    @Data
    public static class NotificationRequest {
        private String title;
        private String content;
        private boolean sendAll;
        private String[] userIds;
    }
}