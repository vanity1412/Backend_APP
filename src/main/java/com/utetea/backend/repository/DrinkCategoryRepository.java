package com.utetea.backend.repository;

import com.utetea.backend.model.DrinkCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrinkCategoryRepository extends JpaRepository<DrinkCategory, Long> {
    
    List<DrinkCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<DrinkCategory> findAllByOrderByDisplayOrderAsc();
}
