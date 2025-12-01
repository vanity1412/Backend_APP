package com.utetea.backend.repository;

import com.utetea.backend.model.DrinkTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrinkToppingRepository extends JpaRepository<DrinkTopping, Long> {
    List<DrinkTopping> findByDrinkIdAndIsActiveTrue(Long drinkId);
    List<DrinkTopping> findByDrinkIdIsNullAndIsActiveTrue();
    List<DrinkTopping> findByIsActiveTrue();
    
    // Get toppings chung (drink_id = NULL) + toppings riêng của drink
    @Query("SELECT t FROM DrinkTopping t WHERE (t.drink.id = :drinkId OR t.drink IS NULL) AND t.isActive = true")
    List<DrinkTopping> findByDrinkIdOrDrinkIdIsNullAndIsActiveTrue(@Param("drinkId") Long drinkId);
}
