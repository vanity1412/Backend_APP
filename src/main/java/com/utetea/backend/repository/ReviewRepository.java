package com.utetea.backend.repository;

import com.utetea.backend.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByDrinkIdOrderByCreatedAtDesc(Long drinkId);
    
    // FIX N+1 Query: Load reviews với User và Drink trong 1 query
    @Query("SELECT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.drink " +
           "WHERE r.drink.id = :drinkId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByDrinkIdWithUserAndDrink(@Param("drinkId") Long drinkId);
    
    Page<Review> findByDrinkId(Long drinkId, Pageable pageable);
    
    List<Review> findByUserId(Long userId);
    
    Optional<Review> findByOrderItemId(Long orderItemId);
    
    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.drink.id = :drinkId")
    Double getAverageRatingByDrinkId(@Param("drinkId") Long drinkId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.drink.id = :drinkId")
    Long countByDrinkId(@Param("drinkId") Long drinkId);
    
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.drink.id = :drinkId GROUP BY r.rating")
    List<Object[]> getRatingDistributionByDrinkId(@Param("drinkId") Long drinkId);
    
    // Delete all reviews by user ID
    @Modifying
    void deleteByUserId(Long userId);
}
