package com.utetea.backend.repository;

import com.utetea.backend.model.MonitoringAlert;
import com.utetea.backend.model.MonitoringAlert.AlertSeverity;
import com.utetea.backend.model.MonitoringAlert.AlertStatus;
import com.utetea.backend.model.MonitoringAlert.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MonitoringAlertRepository extends JpaRepository<MonitoringAlert, Long> {

    // Lấy alerts theo status
    Page<MonitoringAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);
    
    Page<MonitoringAlert> findByStatusInOrderByCreatedAtDesc(List<AlertStatus> statuses, Pageable pageable);

    // Lấy alerts theo severity
    Page<MonitoringAlert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity, Pageable pageable);
    
    Page<MonitoringAlert> findBySeverityInOrderByCreatedAtDesc(List<AlertSeverity> severities, Pageable pageable);

    // Lấy alerts theo user
    Page<MonitoringAlert> findByTargetUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Lấy tất cả alerts có phân trang
    Page<MonitoringAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Đếm alerts pending
    long countByStatus(AlertStatus status);
    
    long countByStatusAndSeverity(AlertStatus status, AlertSeverity severity);

    // Lấy alerts chưa gửi notification
    List<MonitoringAlert> findByNotificationSentFalseAndStatusOrderByCreatedAtAsc(AlertStatus status);

    // Lấy alerts theo nhiều điều kiện
    @Query("SELECT a FROM MonitoringAlert a WHERE " +
           "(:userId IS NULL OR a.targetUser.id = :userId) AND " +
           "(:alertType IS NULL OR a.alertType = :alertType) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<MonitoringAlert> findByFilters(
        @Param("userId") Long userId,
        @Param("alertType") AlertType alertType,
        @Param("severity") AlertSeverity severity,
        @Param("status") AlertStatus status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    // Thống kê theo severity
    @Query("SELECT a.severity, COUNT(a) FROM MonitoringAlert a " +
           "WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countBySeverityAndStatus(@Param("status") AlertStatus status);

    // Thống kê theo alert type
    @Query("SELECT a.alertType, COUNT(a) FROM MonitoringAlert a " +
           "WHERE a.createdAt >= :since GROUP BY a.alertType")
    List<Object[]> countByAlertTypeSince(@Param("since") Instant since);

    // Kiểm tra đã có alert tương tự chưa (tránh spam alert)
    @Query("SELECT COUNT(a) > 0 FROM MonitoringAlert a WHERE " +
           "a.targetUser.id = :userId AND a.alertType = :alertType " +
           "AND a.status IN ('PENDING', 'REVIEWING') AND a.createdAt >= :since")
    boolean existsSimilarPendingAlert(
        @Param("userId") Long userId,
        @Param("alertType") AlertType alertType,
        @Param("since") Instant since
    );

    // Set handledBy = null cho các alerts mà user đã xử lý (khi xóa tài khoản)
    @Modifying
    @Query("UPDATE MonitoringAlert a SET a.handledBy = null WHERE a.handledBy.id = :userId")
    void clearHandledByUserId(@Param("userId") Long userId);
}
