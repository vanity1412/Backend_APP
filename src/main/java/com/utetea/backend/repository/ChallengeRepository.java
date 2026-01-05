package com.utetea.backend.repository;

import com.utetea.backend.model.Challenge;
import com.utetea.backend.model.ChallengeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    
    List<Challenge> findByIsActiveTrue();
    
    Optional<Challenge> findByChallengeTypeAndIsActiveTrue(ChallengeType challengeType);
    
    List<Challenge> findByChallengeType(ChallengeType challengeType);
}
