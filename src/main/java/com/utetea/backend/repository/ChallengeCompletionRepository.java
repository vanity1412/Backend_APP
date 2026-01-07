package com.utetea.backend.repository;

import com.utetea.backend.model.ChallengeCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeCompletionRepository extends JpaRepository<ChallengeCompletion, Long> {
    
    List<ChallengeCompletion> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT cc FROM ChallengeCompletion cc " +
           "JOIN FETCH cc.challenge " +
           "WHERE cc.user.id = :userId " +
           "ORDER BY cc.createdAt DESC")
    List<ChallengeCompletion> findByUserIdWithChallenge(@Param("userId") Long userId);
    
    boolean existsByUserIdAndOrderId(Long userId, Long orderId);
    
    @Query("SELECT SUM(cc.pointsEarned) FROM ChallengeCompletion cc WHERE cc.user.id = :userId")
    Integer getTotalPointsEarnedByUserId(@Param("userId") Long userId);

    // Xóa tất cả challenge completions của user (khi xóa tài khoản)
    @Modifying
    void deleteByUserId(Long userId);
}
