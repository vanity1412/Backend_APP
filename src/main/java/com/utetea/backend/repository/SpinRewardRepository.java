package com.utetea.backend.repository;

import com.utetea.backend.model.SpinReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpinRewardRepository extends JpaRepository<SpinReward, Long> {
    
    // Lấy voucher chưa dùng của user (discount > 0)
    List<SpinReward> findByUserIdAndIsUsedFalseAndDiscountPercentGreaterThan(Long userId, Integer minPercent);
    
    // Tìm voucher theo mã
    Optional<SpinReward> findByVoucherCodeAndIsUsedFalse(String voucherCode);
    
    // Tìm voucher theo mã (bất kể đã dùng hay chưa)
    Optional<SpinReward> findByVoucherCode(String voucherCode);
    
    // Kiểm tra mã đã tồn tại chưa
    boolean existsByVoucherCode(String voucherCode);
    
    // Lấy tất cả voucher của user
    List<SpinReward> findByUserIdOrderByCreatedAtDesc(Long userId);
}
