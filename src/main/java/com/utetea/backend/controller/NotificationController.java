package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.NotificationDto;
import com.utetea.backend.model.NotificationType;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.NotificationService;
import com.utetea.backend.service.OneSignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "🔔 Notifications", description = "API quản lý thông báo đẩy")
public class NotificationController {

    private final OneSignalService oneSignalService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ==================== API CHO USER/MANAGER XEM THÔNG BÁO ====================

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy tất cả thông báo của user hiện tại")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        List<NotificationDto> notifications = notificationService.getUserNotifications(user.getId());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/paged")
    @Operation(summary = "Lấy thông báo có phân trang", description = "Lấy thông báo với phân trang")
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getMyNotificationsPaged(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUserFromDetails(userDetails);
        Page<NotificationDto> notifications = notificationService.getUserNotifications(
                user.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    @Operation(summary = "Lấy thông báo chưa đọc", description = "Lấy danh sách thông báo chưa đọc")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(user.getId());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Đếm thông báo chưa đọc", description = "Lấy số lượng thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        long count = notificationService.countUnread(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu một thông báo là đã đọc")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        NotificationDto notification = notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả đã đọc", description = "Đánh dấu tất cả thông báo là đã đọc")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        int updated = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updatedCount", updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông báo", description = "Xóa một thông báo")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thông báo"));
    }

    // ==================== API CHO MANAGER GỬI THÔNG BÁO ====================

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
                oneSignalService.sendToAll(request.getTitle(), request.getContent(), 
                        NotificationType.CUSTOM, null);
            } else if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                String[] userIdsArray = request.getUserIds().toArray(new String[0]);
                oneSignalService.sendToMultipleUsers(userIdsArray, request.getTitle(), request.getContent(),
                        NotificationType.CUSTOM, null);
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn đối tượng nhận thông báo"));
            }
            return ResponseEntity.ok(ApiResponse.success("Đã gửi thông báo thành công"));
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi thông báo: " + e.getMessage()));
        }
    }

    // ==================== HELPER ====================

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // DTO - Hỗ trợ cả List<String> từ Android
    @Data
    public static class NotificationRequest {
        private String title;
        private String content;
        private boolean sendAll;
        private List<String> userIds;
    }
}
