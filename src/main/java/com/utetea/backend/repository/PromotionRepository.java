package com.utetea.backend.repository;

import com.utetea.backend.model.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);
    
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);
    
    List<Promotion> findByIsActiveTrueAndEndDateAfter(LocalDateTime date);
    
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
    
    // For chatbot - find active promotions within date range
    List<Promotion> findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * FIX Critical #1, #2: Pessimistic locking để tránh race condition khi update usedCount
     * Đảm bảo atomic read-check-update cho promotion usage
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.code = :code")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);
}
