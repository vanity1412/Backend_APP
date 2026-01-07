package com.utetea.backend.repository;

import com.utetea.backend.model.DeletedUserRiskScoreBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeletedUserRiskScoreBackupRepository extends JpaRepository<DeletedUserRiskScoreBackup, Long> {

    // Tìm theo user đã xóa
    Optional<DeletedUserRiskScoreBackup> findByDeletedUserId(Long deletedUserId);
    Optional<DeletedUserRiskScoreBackup> findByDeletedUsername(String deletedUsername);

    // Tìm theo risk level
    Page<DeletedUserRiskScoreBackup> findByRiskLevelOrderByTotalScoreDesc(String riskLevel, Pageable pageable);

    // Lấy users có điểm cao nhất
    Page<DeletedUserRiskScoreBackup> findAllByOrderByTotalScoreDesc(Pageable pageable);

    // Lấy users đã bị auto-block
    Page<DeletedUserRiskScoreBackup> findByAutoBlockedTrueOrderByAutoBlockedAtDesc(Pageable pageable);

    // Đếm theo risk level
    @Query("SELECT b.riskLevel, COUNT(b) FROM DeletedUserRiskScoreBackup b " +
           "GROUP BY b.riskLevel")
    List<Object[]> countByRiskLevel();

    // Thống kê theo khoảng điểm
    @Query("SELECT " +
           "CASE " +
           "  WHEN b.totalScore >= 80 THEN 'CRITICAL' " +
           "  WHEN b.totalScore >= 60 THEN 'SUSPICIOUS' " +
           "  WHEN b.totalScore >= 30 THEN 'WARNING' " +
           "  ELSE 'NORMAL' " +
           "END as scoreRange, COUNT(b) " +
           "FROM DeletedUserRiskScoreBackup b " +
           "GROUP BY scoreRange")
    List<Object[]> countByScoreRange();

    // Tìm kiếm
    @Query("SELECT b FROM DeletedUserRiskScoreBackup b WHERE " +
           "b.deletedUsername LIKE %:keyword% " +
           "ORDER BY b.totalScore DESC")
    Page<DeletedUserRiskScoreBackup> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Lấy backup theo khoảng thời gian
    Page<DeletedUserRiskScoreBackup> findByBackupCreatedAtBetween(
            Instant startDate,
            Instant endDate,
            Pageable pageable);
}
