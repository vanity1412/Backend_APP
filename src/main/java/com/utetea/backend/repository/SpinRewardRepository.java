package com.utetea.backend.repository;

import com.utetea.backend.model.SpinReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpinRewardRepository extends JpaRepository<SpinReward, Long> {
    
    List<SpinReward> findByUserIdAndIsRedeemedFalse(Long userId);
    
    Optional<SpinReward> findByIdAndUserIdAndIsRedeemedFalse(Long id, Long userId);
    
    List<SpinReward> findByUserIdOrderByCreatedAtDesc(Long userId);
}
