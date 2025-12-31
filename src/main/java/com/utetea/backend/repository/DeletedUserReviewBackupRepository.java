package com.utetea.backend.repository;

import com.utetea.backend.model.DeletedUserReviewBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeletedUserReviewBackupRepository extends JpaRepository<DeletedUserReviewBackup, Long> {
    
    Page<DeletedUserReviewBackup> findByDrinkIdOrderByReviewCreatedAtDesc(Long drinkId, Pageable pageable);
    
    List<DeletedUserReviewBackup> findByDrinkIdOrderByReviewCreatedAtDesc(Long drinkId);
    
    @Query("SELECT AVG(b.rating) FROM DeletedUserReviewBackup b WHERE b.drinkId = :drinkId")
    Double getAverageRatingByDrinkId(@Param("drinkId") Long drinkId);
    
    @Query("SELECT COUNT(b) FROM DeletedUserReviewBackup b WHERE b.drinkId = :drinkId")
    Long countByDrinkId(@Param("drinkId") Long drinkId);
    
    @Query("SELECT b.rating, COUNT(b) FROM DeletedUserReviewBackup b WHERE b.drinkId = :drinkId GROUP BY b.rating")
    List<Object[]> getRatingDistributionByDrinkId(@Param("drinkId") Long drinkId);
}
