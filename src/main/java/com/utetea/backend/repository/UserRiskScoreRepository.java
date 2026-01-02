package com.utetea.backend.repository;

import com.utetea.backend.model.UserActivityLog.RiskLevel;
import com.utetea.backend.model.UserRiskScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRiskScoreRepository extends JpaRepository<UserRiskScore, Long> {

    Optional<UserRiskScore> findByUserId(Long userId);

    // Lấy theo risk level
    Page<UserRiskScore> findByRiskLevelOrderByTotalScoreDesc(RiskLevel riskLevel, Pageable pageable);
    
    List<UserRiskScore> findByRiskLevelIn(List<RiskLevel> riskLevels);

    // Lấy users có điểm cao nhất
    Page<UserRiskScore> findAllByOrderByTotalScoreDesc(Pageable pageable);

    // Lấy users cần auto-block (score >= threshold)
    @Query("SELECT r FROM UserRiskScore r WHERE r.totalScore >= :threshold AND r.autoBlocked = false")
    List<UserRiskScore> findUsersToAutoBlock(@Param("threshold") int threshold);

    // Lấy users đã bị auto-block
    Page<UserRiskScore> findByAutoBlockedTrueOrderByAutoBlockedAtDesc(Pageable pageable);

    // Đếm theo risk level
    long countByRiskLevel(RiskLevel riskLevel);

    // Thống kê tổng quan
    @Query("SELECT r.riskLevel, COUNT(r) FROM UserRiskScore r GROUP BY r.riskLevel")
    List<Object[]> countByRiskLevelGrouped();

    // Tìm kiếm
    @Query("SELECT r FROM UserRiskScore r WHERE " +
           "r.user.username LIKE %:keyword% OR r.user.email LIKE %:keyword% " +
           "ORDER BY r.totalScore DESC")
    Page<UserRiskScore> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
