package com.utetea.backend.repository;

import com.utetea.backend.model.DeletedUserMonitoringAlertBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeletedUserMonitoringAlertBackupRepository extends JpaRepository<DeletedUserMonitoringAlertBackup, Long> {

    // Tìm theo user đã xóa
    List<DeletedUserMonitoringAlertBackup> findByDeletedUserId(Long deletedUserId);
    List<DeletedUserMonitoringAlertBackup> findByDeletedUsername(String deletedUsername);

    // Tìm theo severity
    Page<DeletedUserMonitoringAlertBackup> findBySeverityOrderByAlertCreatedAtDesc(String severity, Pageable pageable);

    // Tìm theo alert type
    Page<DeletedUserMonitoringAlertBackup> findByAlertTypeOrderByAlertCreatedAtDesc(String alertType, Pageable pageable);

    // Lấy backup theo khoảng thời gian
    Page<DeletedUserMonitoringAlertBackup> findByAlertCreatedAtBetween(
            Instant startDate,
            Instant endDate,
            Pageable pageable);

    // Đếm số alerts backup theo severity
    @Query("SELECT b.severity, COUNT(b) FROM DeletedUserMonitoringAlertBackup b " +
           "GROUP BY b.severity")
    List<Object[]> countBySeverity();

    // Đếm số alerts backup theo alert type
    @Query("SELECT b.alertType, COUNT(b) FROM DeletedUserMonitoringAlertBackup b " +
           "WHERE b.alertCreatedAt >= :since " +
           "GROUP BY b.alertType")
    List<Object[]> countByAlertTypeSince(@Param("since") Instant since);

    // Thống kê alerts theo ngày
    @Query("SELECT DATE(b.alertCreatedAt) as date, COUNT(b) as alertCount " +
           "FROM DeletedUserMonitoringAlertBackup b " +
           "WHERE b.alertCreatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(b.alertCreatedAt) " +
           "ORDER BY DATE(b.alertCreatedAt)")
    List<Object[]> getDailyAlertStats(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
