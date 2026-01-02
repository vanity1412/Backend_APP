package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.NotificationDto;
import com.utetea.backend.model.Notification;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.NotificationRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách thông báo của user hiện tại
     * GET /api/notifications/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
            List<NotificationDto> dtos = notifications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("Error getting notifications", e);
            return ResponseEntity.ok(ApiResponse.error("Không thể tải thông báo: " + e.getMessage()));
        }
    }

    /**
     * Đánh dấu thông báo đã đọc
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // Kiểm tra quyền sở hữu
            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.ok(ApiResponse.error("Không có quyền truy cập thông báo này"));
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);

            return ResponseEntity.ok(ApiResponse.success(convertToDto(notification)));
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            int updated = notificationRepository.markAllAsReadByUserId(user.getId());

            Map<String, Integer> result = new HashMap<>();
            result.put("updated", updated);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error marking all notifications as read", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Xóa thông báo
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // Kiểm tra quyền sở hữu
            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.ok(ApiResponse.error("Không có quyền xóa thông báo này"));
            }

            notificationRepository.delete(notification);

            return ResponseEntity.ok(ApiResponse.success("Đã xóa thông báo"));
        } catch (Exception e) {
            log.error("Error deleting notification", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Đếm số thông báo chưa đọc
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

            Map<String, Long> result = new HashMap<>();
            result.put("count", count);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting unread count", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Convert Notification entity to DTO
     */
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setType(notification.getType());
        dto.setRelatedId(notification.getRelatedId());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
