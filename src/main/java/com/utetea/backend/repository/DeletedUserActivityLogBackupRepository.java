package com.utetea.backend.repository;

import com.utetea.backend.model.DeletedUserActivityLogBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeletedUserActivityLogBackupRepository extends JpaRepository<DeletedUserActivityLogBackup, Long> {

    // Tìm theo user đã xóa
    List<DeletedUserActivityLogBackup> findByDeletedUserId(Long deletedUserId);
    List<DeletedUserActivityLogBackup> findByDeletedUsername(String deletedUsername);

    // Tìm theo risk level
    Page<DeletedUserActivityLogBackup> findByRiskLevelOrderByActivityCreatedAtDesc(String riskLevel, Pageable pageable);

    // Tìm theo activity type
    Page<DeletedUserActivityLogBackup> findByActivityTypeOrderByActivityCreatedAtDesc(String activityType, Pageable pageable);

    // Lấy backup theo khoảng thời gian
    Page<DeletedUserActivityLogBackup> findByActivityCreatedAtBetween(
            Instant startDate,
            Instant endDate,
            Pageable pageable);

    // Đếm số logs backup theo risk level
    @Query("SELECT b.riskLevel, COUNT(b) FROM DeletedUserActivityLogBackup b " +
           "GROUP BY b.riskLevel")
    List<Object[]> countByRiskLevel();

    // Đếm số logs backup theo activity type
    @Query("SELECT b.activityType, COUNT(b) FROM DeletedUserActivityLogBackup b " +
           "WHERE b.activityCreatedAt >= :since " +
           "GROUP BY b.activityType")
    List<Object[]> countByActivityTypeSince(@Param("since") Instant since);

    // Thống kê logs theo ngày
    @Query("SELECT DATE(b.activityCreatedAt) as date, COUNT(b) as logCount " +
           "FROM DeletedUserActivityLogBackup b " +
           "WHERE b.activityCreatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(b.activityCreatedAt) " +
           "ORDER BY DATE(b.activityCreatedAt)")
    List<Object[]> getDailyLogStats(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // Tìm kiếm trong backup
    @Query("SELECT b FROM DeletedUserActivityLogBackup b WHERE " +
           "(b.deletedUsername LIKE %:keyword% " +
           "OR b.ipAddress LIKE %:keyword% " +
           "OR b.description LIKE %:keyword%) " +
           "ORDER BY b.activityCreatedAt DESC")
    Page<DeletedUserActivityLogBackup> searchLogs(@Param("keyword") String keyword, Pageable pageable);
}
