package com.utetea.backend.repository;

import com.utetea.backend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo của user theo thứ tự mới nhất
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy thông báo có phân trang
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Lấy thông báo chưa đọc
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Đếm số thông báo chưa đọc
    long countByUserIdAndIsReadFalse(Long userId);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    // Xóa thông báo cũ hơn X ngày
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") java.time.Instant cutoffDate);
}
