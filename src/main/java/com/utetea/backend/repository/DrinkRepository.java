package com.utetea.backend.repository;

import com.utetea.backend.model.Drink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrinkRepository extends JpaRepository<Drink, Long> {
    List<Drink> findByIsActiveTrue();
    Page<Drink> findByIsActiveTrue(Pageable pageable);
    
    List<Drink> findByCategoryIdAndIsActiveTrue(Long categoryId);
    
    // For chatbot - search by name containing keyword
    List<Drink> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    @Query("SELECT d FROM Drink d WHERE d.isActive = true AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Drink> searchByName(@Param("keyword") String keyword);
    
    @Query("SELECT d FROM Drink d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Drink> searchByNameAll(@Param("keyword") String keyword);
    
    // ===== FIX N+1 QUERY: Sử dụng JOIN FETCH để load sizes và toppings cùng lúc =====
    
    @Query("SELECT DISTINCT d FROM Drink d " +
           "LEFT JOIN FETCH d.category " +
           "LEFT JOIN FETCH d.sizes " +
           "WHERE d.isActive = true")
    List<Drink> findByIsActiveTrueWithSizesAndCategory();
    
    @Query("SELECT DISTINCT d FROM Drink d " +
           "LEFT JOIN FETCH d.category " +
           "LEFT JOIN FETCH d.sizes " +
           "WHERE d.id = :id")
    Optional<Drink> findByIdWithSizesAndCategory(@Param("id") Long id);
    
    @Query("SELECT DISTINCT d FROM Drink d " +
           "LEFT JOIN FETCH d.category " +
           "LEFT JOIN FETCH d.sizes " +
           "WHERE d.category.id = :categoryId AND d.isActive = true")
    List<Drink> findByCategoryIdWithSizesAndCategory(@Param("categoryId") Long categoryId);
    
    @Query("SELECT DISTINCT d FROM Drink d " +
           "LEFT JOIN FETCH d.category " +
           "LEFT JOIN FETCH d.sizes " +
           "WHERE d.isActive = true AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Drink> searchByNameWithSizesAndCategory(@Param("keyword") String keyword);
}
