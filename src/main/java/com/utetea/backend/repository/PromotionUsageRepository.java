package com.utetea.backend.repository;

import com.utetea.backend.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    
    /**
     * ✅ SECURITY: Check xem user đã dùng voucher này chưa
     */
    boolean existsByPromotionIdAndUserId(Long promotionId, Long userId);
    
    /**
     * Lấy thông tin usage
     */
    Optional<PromotionUsage> findByPromotionIdAndUserId(Long promotionId, Long userId);
    
    /**
     * Đếm số lần user đã dùng voucher này
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.promotion.id = :promotionId AND pu.user.id = :userId")
    long countByPromotionIdAndUserId(Long promotionId, Long userId);
}
