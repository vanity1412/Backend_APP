package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.NotificationDto;
import com.utetea.backend.dto.NotificationRequestDto;
import com.utetea.backend.model.Notification;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.NotificationRepository;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.service.OneSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final OneSignalService oneSignalService;

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
        
        // Convert Instant to LocalDateTime
        if (notification.getCreatedAt() != null) {
            dto.setCreatedAt(
                java.time.LocalDateTime.ofInstant(
                    notification.getCreatedAt(), 
                    java.time.ZoneId.systemDefault()
                )
            );
        }
        
        return dto;
    }

    /**
     * Gửi thông báo tùy chỉnh (CHỈ MANAGER/ADMIN)
     * POST /api/notifications/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> sendCustomNotification(
            @RequestBody NotificationRequestDto request,
            Authentication authentication) {
        try {
            String senderUsername = authentication.getName();
            User sender = userRepository.findByUsername(senderUsername)
                    .orElseThrow(() -> new RuntimeException("Sender not found"));

            log.info("Manager/Admin {} sending notification: sendAll={}, userIds={}", 
                    senderUsername, request.getSendAll(), request.getUserIds());

            // Validate input
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Tiêu đề không được để trống"));
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Nội dung không được để trống"));
            }

            if (request.getSendAll() != null && request.getSendAll()) {
                // Gửi cho tất cả user
                oneSignalService.sendToAll(request.getTitle(), request.getContent());
                log.info("Sent notification to all users");
                return ResponseEntity.ok(ApiResponse.success("Đã gửi thông báo cho tất cả người dùng"));
                
            } else {
                // Gửi cho user cụ thể
                if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.error("Vui lòng chọn ít nhất một người dùng"));
                }

                // Convert String IDs to array
                String[] userIds = request.getUserIds().toArray(new String[0]);
                oneSignalService.sendToMultipleUsers(userIds, request.getTitle(), request.getContent(),
                        com.utetea.backend.model.NotificationType.CUSTOM, null);
                
                log.info("Sent notification to {} specific users", userIds.length);
                return ResponseEntity.ok(ApiResponse.success("Đã gửi thông báo cho " + userIds.length + " người dùng"));
            }

        } catch (Exception e) {
            log.error("Error sending custom notification", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi gửi thông báo: " + e.getMessage()));
        }
    }
}
