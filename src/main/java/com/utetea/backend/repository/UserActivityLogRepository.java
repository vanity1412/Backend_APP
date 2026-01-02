package com.utetea.backend.repository;

import com.utetea.backend.model.UserActivityLog;
import com.utetea.backend.model.UserActivityLog.ActivityType;
import com.utetea.backend.model.UserActivityLog.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    // Lấy log theo user
    Page<UserActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<UserActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy log theo risk level
    Page<UserActivityLog> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel, Pageable pageable);
    
    Page<UserActivityLog> findByRiskLevelInOrderByCreatedAtDesc(List<RiskLevel> riskLevels, Pageable pageable);

    // Lấy log theo activity type
    Page<UserActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);

    // Lấy tất cả log có phân trang
    Page<UserActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Đếm số lần activity trong khoảng thời gian
    @Query("SELECT COUNT(l) FROM UserActivityLog l WHERE l.userId = :userId " +
           "AND l.activityType = :activityType AND l.createdAt >= :since")
    long countByUserIdAndActivityTypeSince(
        @Param("userId") Long userId,
        @Param("activityType") ActivityType activityType,
        @Param("since") Instant since
    );

    // Đếm số lần activity theo risk level trong khoảng thời gian
    @Query("SELECT COUNT(l) FROM UserActivityLog l WHERE l.userId = :userId " +
           "AND l.riskLevel = :riskLevel AND l.createdAt >= :since")
    long countByUserIdAndRiskLevelSince(
        @Param("userId") Long userId,
        @Param("riskLevel") RiskLevel riskLevel,
        @Param("since") Instant since
    );

    // Lấy log gần đây của user
    @Query("SELECT l FROM UserActivityLog l WHERE l.userId = :userId " +
           "AND l.createdAt >= :since ORDER BY l.createdAt DESC")
    List<UserActivityLog> findRecentByUserId(
        @Param("userId") Long userId,
        @Param("since") Instant since
    );

    // Tìm kiếm log
    @Query("SELECT l FROM UserActivityLog l WHERE " +
           "(l.user.username LIKE %:keyword% OR l.user.email LIKE %:keyword% " +
           "OR l.ipAddress LIKE %:keyword% OR l.description LIKE %:keyword%) " +
           "ORDER BY l.createdAt DESC")
    Page<UserActivityLog> searchLogs(@Param("keyword") String keyword, Pageable pageable);

    // Lấy log theo nhiều điều kiện
    @Query("SELECT l FROM UserActivityLog l WHERE " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:activityType IS NULL OR l.activityType = :activityType) AND " +
           "(:riskLevel IS NULL OR l.riskLevel = :riskLevel) AND " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate) " +
           "ORDER BY l.createdAt DESC")
    Page<UserActivityLog> findByFilters(
        @Param("userId") Long userId,
        @Param("activityType") ActivityType activityType,
        @Param("riskLevel") RiskLevel riskLevel,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    // Thống kê theo activity type
    @Query("SELECT l.activityType, COUNT(l) FROM UserActivityLog l " +
           "WHERE l.createdAt >= :since GROUP BY l.activityType")
    List<Object[]> countByActivityTypeSince(@Param("since") Instant since);

    // Thống kê theo risk level
    @Query("SELECT l.riskLevel, COUNT(l) FROM UserActivityLog l " +
           "WHERE l.createdAt >= :since GROUP BY l.riskLevel")
    List<Object[]> countByRiskLevelSince(@Param("since") Instant since);

    // Lấy users có nhiều hoạt động đáng ngờ nhất
    @Query("SELECT l.userId, COUNT(l) as cnt FROM UserActivityLog l " +
           "WHERE l.riskLevel IN :riskLevels AND l.createdAt >= :since " +
           "GROUP BY l.userId ORDER BY cnt DESC")
    List<Object[]> findTopSuspiciousUsers(
        @Param("riskLevels") List<RiskLevel> riskLevels,
        @Param("since") Instant since,
        Pageable pageable
    );
}
