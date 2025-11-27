package com.utetea.backend.service;

import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.dto.DrinkSizeDto;
import com.utetea.backend.dto.DrinkToppingDto;
import com.utetea.backend.model.Drink;
import com.utetea.backend.model.DrinkSize;
import com.utetea.backend.model.DrinkTopping;
import com.utetea.backend.repository.DrinkRepository;
import com.utetea.backend.repository.DrinkSizeRepository;
import com.utetea.backend.repository.DrinkToppingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrinkService {
    
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    
    @Transactional(readOnly = true)
    public List<DrinkDto> getAllActiveDrinks() {
        return drinkRepository.findByIsActiveTrue().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<DrinkDto> getAllActiveDrinks(Pageable pageable) {
        return drinkRepository.findByIsActiveTrue(pageable)
            .map(this::mapToDto);
    }
    
    @Transactional(readOnly = true)
    public DrinkDto getDrinkById(Long id) {
        Drink drink = drinkRepository.findById(id)
            .orElseThrow(() -> new com.utetea.backend.exception.ResourceNotFoundException("Drink", "id", id));
        return mapToDto(drink);
    }
    
    @Transactional(readOnly = true)
    public List<DrinkDto> searchDrinks(String keyword) {
        return drinkRepository.searchByName(keyword).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public DrinkDto createDrink(DrinkDto dto) {
        Drink drink = new Drink();
        drink.setName(dto.getName());
        drink.setDescription(dto.getDescription());
        drink.setImageUrl(dto.getImageUrl());
        drink.setBasePrice(dto.getBasePrice());
        drink.setIsActive(true);
        
        drink = drinkRepository.save(drink);
        return mapToDto(drink);
    }
    
    @Transactional
    public DrinkDto updateDrink(Long id, DrinkDto dto) {
        Drink drink = drinkRepository.findById(id)
            .orElseThrow(() -> new com.utetea.backend.exception.ResourceNotFoundException("Drink", "id", id));
        
        drink.setName(dto.getName());
        drink.setDescription(dto.getDescription());
        drink.setImageUrl(dto.getImageUrl());
        drink.setBasePrice(dto.getBasePrice());
        drink.setIsActive(dto.getIsActive());
        
        drink = drinkRepository.save(drink);
        return mapToDto(drink);
    }
    
    @Transactional
    public void deleteDrink(Long id) {
        Drink drink = drinkRepository.findById(id)
            .orElseThrow(() -> new com.utetea.backend.exception.ResourceNotFoundException("Drink", "id", id));
        drink.setIsActive(false);
        drinkRepository.save(drink);
    }
    
    private DrinkDto mapToDto(Drink drink) {
        DrinkDto dto = new DrinkDto();
        dto.setId(drink.getId());
        dto.setName(drink.getName());
        dto.setDescription(drink.getDescription());
        dto.setImageUrl(drink.getImageUrl());
        dto.setBasePrice(drink.getBasePrice());
        dto.setIsActive(drink.getIsActive());
        
        List<DrinkSizeDto> sizes = drinkSizeRepository.findByDrinkId(drink.getId()).stream()
            .map(this::mapSizeToDto)
            .collect(Collectors.toList());
        dto.setSizes(sizes);
        
        // Lấy toppings chung (drink_id = NULL) + toppings riêng của drink này
        List<DrinkToppingDto> toppings = drinkToppingRepository
            .findByDrinkIdOrDrinkIdIsNullAndIsActiveTrue(drink.getId())
            .stream()
            .map(this::mapToppingToDto)
            .collect(Collectors.toList());
        dto.setToppings(toppings);
        
        return dto;
    }
    
    private DrinkSizeDto mapSizeToDto(DrinkSize size) {
        return new DrinkSizeDto(size.getId(), size.getSizeName(), size.getExtraPrice());
    }
    
    private DrinkToppingDto mapToppingToDto(DrinkTopping topping) {
        return new DrinkToppingDto(topping.getId(), topping.getToppingName(), 
            topping.getPrice(), topping.getIsActive());
    }
}
