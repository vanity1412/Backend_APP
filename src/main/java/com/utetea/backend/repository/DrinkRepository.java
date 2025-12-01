package com.utetea.backend.repository;

import com.utetea.backend.model.Drink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrinkRepository extends JpaRepository<Drink, Long> {
    List<Drink> findByIsActiveTrue();
    Page<Drink> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT d FROM Drink d WHERE d.isActive = true AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Drink> searchByName(@Param("keyword") String keyword);
    
    @Query("SELECT d FROM Drink d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Drink> searchByNameAll(@Param("keyword") String keyword);
}
