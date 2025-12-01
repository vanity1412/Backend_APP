package com.utetea.backend.repository;

import com.utetea.backend.model.DrinkSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrinkSizeRepository extends JpaRepository<DrinkSize, Long> {
    List<DrinkSize> findByDrinkId(Long drinkId);
}
