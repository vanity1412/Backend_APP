package com.utetea.backend.repository;

import com.utetea.backend.model.SpinReward;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpinRewardRepository extends JpaRepository<SpinReward, Long> {
    
    // Lấy voucher chưa dùng của user (discount > 0)
    List<SpinReward> findByUserIdAndIsUsedFalseAndDiscountPercentGreaterThan(Long userId, Integer minPercent);
    
    // Tìm voucher theo mã (không lock - dùng cho validate)
    Optional<SpinReward> findByVoucherCodeAndIsUsedFalse(String voucherCode);
    
    // Tìm voucher theo mã với PESSIMISTIC_WRITE lock - dùng khi tạo order
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM SpinReward sr WHERE sr.voucherCode = :voucherCode AND sr.isUsed = false")
    Optional<SpinReward> findByVoucherCodeForUpdate(@Param("voucherCode") String voucherCode);
    
    // Tìm voucher theo mã (bất kể đã dùng hay chưa)
    Optional<SpinReward> findByVoucherCode(String voucherCode);
    
    // Kiểm tra mã đã tồn tại chưa
    boolean existsByVoucherCode(String voucherCode);
    
    // Lấy tất cả voucher của user
    List<SpinReward> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Delete all spin rewards by user ID
    @Modifying
    void deleteByUserId(Long userId);
}
