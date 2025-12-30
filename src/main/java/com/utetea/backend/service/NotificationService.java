package com.utetea.backend.service;

import com.utetea.backend.dto.NotificationDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.Notification;
import com.utetea.backend.model.NotificationType;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.NotificationRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Lưu thông báo vào database
     */
    @Transactional
    public Notification saveNotification(Long userId, String title, String content, 
                                         NotificationType type, Long relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Saved notification for user {}: {}", userId, title);
        return notification;
    }

    /**
     * Lưu thông báo cho nhiều user
     */
    @Transactional
    public void saveNotificationForUsers(List<Long> userIds, String title, String content,
                                         NotificationType type, Long relatedId) {
        for (Long userId : userIds) {
            try {
                saveNotification(userId, title, content, type, relatedId);
            } catch (Exception e) {
                log.error("Failed to save notification for user {}", userId, e);
            }
        }
    }

    /**
     * Lấy danh sách thông báo của user
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách thông báo có phân trang
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    /**
     * Lấy thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu một thông báo là đã đọc
     */
    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        // Kiểm tra quyền sở hữu
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return mapToDto(notification);
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    /**
     * Xóa một thông báo
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .relatedId(notification.getRelatedId())
                .createdAt(notification.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
