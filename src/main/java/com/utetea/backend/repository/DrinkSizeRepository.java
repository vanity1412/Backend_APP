package com.utetea.backend.repository;

import com.utetea.backend.model.DrinkSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrinkSizeRepository extends JpaRepository<DrinkSize, Long> {
    List<DrinkSize> findByDrinkId(Long drinkId);
    
    // Batch load sizes cho nhiều drinks (FIX N+1)
    @Query("SELECT s FROM DrinkSize s WHERE s.drink.id IN :drinkIds")
    List<DrinkSize> findByDrinkIdIn(@Param("drinkIds") List<Long> drinkIds);
    
    // Tìm size theo drinkId và sizeName (cho Predictive Order)
    @Query("SELECT s FROM DrinkSize s WHERE s.drink.id = :drinkId AND s.sizeName = :sizeName")
    java.util.Optional<DrinkSize> findByDrinkIdAndSizeName(@Param("drinkId") Long drinkId, @Param("sizeName") String sizeName);
}
