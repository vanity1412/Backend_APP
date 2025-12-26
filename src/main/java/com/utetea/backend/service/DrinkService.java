package com.utetea.backend.service;

import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.dto.DrinkSizeDto;
import com.utetea.backend.dto.DrinkToppingDto;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.Drink;
import com.utetea.backend.model.DrinkSize;
import com.utetea.backend.model.DrinkTopping;
import com.utetea.backend.repository.DrinkRepository;
import com.utetea.backend.repository.DrinkSizeRepository;
import com.utetea.backend.repository.DrinkToppingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrinkService {
    
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    
    /**
     * FIX N+1 QUERY: Load tất cả drinks với sizes trong 1 query,
     * sau đó batch load toppings
     */
    @Transactional(readOnly = true)
    public List<DrinkDto> getAllActiveDrinks() {
        // Query 1: Load drinks với sizes và category (JOIN FETCH)
        List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
        
        if (drinks.isEmpty()) {
            return List.of();
        }
        
        // Query 2: Batch load toppings cho tất cả drinks
        List<Long> drinkIds = drinks.stream().map(Drink::getId).collect(Collectors.toList());
        Map<Long, List<DrinkTopping>> toppingsMap = loadToppingsForDrinks(drinkIds);
        
        // Query 3: Load global toppings (drink_id = NULL)
        List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
        
        return drinks.stream()
            .map(drink -> mapToDtoOptimized(drink, toppingsMap.get(drink.getId()), globalToppings))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<DrinkDto> getAllActiveDrinks(Pageable pageable) {
        // Cho pagination, vẫn cần query riêng vì JOIN FETCH không hoạt động tốt với pagination
        Page<Drink> drinksPage = drinkRepository.findByIsActiveTrue(pageable);
        
        if (drinksPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Batch load toppings
        List<Long> drinkIds = drinksPage.getContent().stream().map(Drink::getId).collect(Collectors.toList());
        Map<Long, List<DrinkTopping>> toppingsMap = loadToppingsForDrinks(drinkIds);
        List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
        
        // Batch load sizes
        Map<Long, List<DrinkSize>> sizesMap = loadSizesForDrinks(drinkIds);
        
        return drinksPage.map(drink -> mapToDtoWithMaps(drink, sizesMap.get(drink.getId()), 
                                                         toppingsMap.get(drink.getId()), globalToppings));
    }
    
    @Transactional(readOnly = true)
    public DrinkDto getDrinkById(Long id) {
        Drink drink = drinkRepository.findByIdWithSizesAndCategory(id)
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", id));
        
        List<DrinkTopping> drinkToppings = drinkToppingRepository.findByDrinkIdAndIsActiveTrue(id);
        List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
        
        return mapToDtoOptimized(drink, drinkToppings, globalToppings);
    }
    
    @Transactional(readOnly = true)
    public List<DrinkDto> searchDrinks(String keyword) {
        // Input sanitization: validate and clean keyword
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        // Sanitize: remove special characters, keep Vietnamese characters
        String sanitized = keyword.replaceAll("[^a-zA-Z0-9\\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]", "");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        if (sanitized.trim().isEmpty()) {
            return List.of();
        }
        
        List<Drink> drinks = drinkRepository.searchByNameWithSizesAndCategory(sanitized);
        
        if (drinks.isEmpty()) {
            return List.of();
        }
        
        List<Long> drinkIds = drinks.stream().map(Drink::getId).collect(Collectors.toList());
        Map<Long, List<DrinkTopping>> toppingsMap = loadToppingsForDrinks(drinkIds);
        List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
        
        return drinks.stream()
            .map(drink -> mapToDtoOptimized(drink, toppingsMap.get(drink.getId()), globalToppings))
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
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", id));
        
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
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", id));
        drink.setIsActive(false);
        drinkRepository.save(drink);
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Batch load toppings cho nhiều drinks cùng lúc
     */
    private Map<Long, List<DrinkTopping>> loadToppingsForDrinks(List<Long> drinkIds) {
        List<DrinkTopping> allToppings = drinkToppingRepository.findByDrinkIdInAndIsActiveTrue(drinkIds);
        return allToppings.stream()
            .collect(Collectors.groupingBy(t -> t.getDrink().getId()));
    }
    
    /**
     * Batch load sizes cho nhiều drinks cùng lúc
     */
    private Map<Long, List<DrinkSize>> loadSizesForDrinks(List<Long> drinkIds) {
        List<DrinkSize> allSizes = drinkSizeRepository.findByDrinkIdIn(drinkIds);
        return allSizes.stream()
            .collect(Collectors.groupingBy(s -> s.getDrink().getId()));
    }
    
    /**
     * Map Drink to DTO với pre-loaded data (optimized)
     */
    private DrinkDto mapToDtoOptimized(Drink drink, List<DrinkTopping> drinkToppings, 
                                        List<DrinkTopping> globalToppings) {
        DrinkDto dto = new DrinkDto();
        dto.setId(drink.getId());
        dto.setName(drink.getName());
        dto.setDescription(drink.getDescription());
        dto.setImageUrl(drink.getImageUrl());
        dto.setBasePrice(drink.getBasePrice());
        dto.setIsActive(drink.getIsActive());
        
        if (drink.getCategory() != null) {
            dto.setCategoryId(drink.getCategory().getId());
            dto.setCategoryName(drink.getCategory().getName());
        }
        
        // Sizes đã được load qua JOIN FETCH
        List<DrinkSizeDto> sizes = drink.getSizes() != null ? 
            drink.getSizes().stream().map(this::mapSizeToDto).collect(Collectors.toList()) :
            List.of();
        dto.setSizes(sizes);
        
        // Combine drink-specific toppings với global toppings
        List<DrinkToppingDto> toppings = new java.util.ArrayList<>();
        if (drinkToppings != null) {
            toppings.addAll(drinkToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
        }
        if (globalToppings != null) {
            toppings.addAll(globalToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
        }
        dto.setToppings(toppings);
        
        return dto;
    }
    
    /**
     * Map Drink to DTO với pre-loaded maps (for pagination)
     */
    private DrinkDto mapToDtoWithMaps(Drink drink, List<DrinkSize> sizes, 
                                       List<DrinkTopping> drinkToppings, 
                                       List<DrinkTopping> globalToppings) {
        DrinkDto dto = new DrinkDto();
        dto.setId(drink.getId());
        dto.setName(drink.getName());
        dto.setDescription(drink.getDescription());
        dto.setImageUrl(drink.getImageUrl());
        dto.setBasePrice(drink.getBasePrice());
        dto.setIsActive(drink.getIsActive());
        
        if (drink.getCategory() != null) {
            dto.setCategoryId(drink.getCategory().getId());
            dto.setCategoryName(drink.getCategory().getName());
        }
        
        dto.setSizes(sizes != null ? 
            sizes.stream().map(this::mapSizeToDto).collect(Collectors.toList()) : List.of());
        
        List<DrinkToppingDto> toppings = new java.util.ArrayList<>();
        if (drinkToppings != null) {
            toppings.addAll(drinkToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
        }
        if (globalToppings != null) {
            toppings.addAll(globalToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
        }
        dto.setToppings(toppings);
        
        return dto;
    }
    
    /**
     * Legacy mapToDto - dùng cho create/update (single drink)
     */
    private DrinkDto mapToDto(Drink drink) {
        List<DrinkTopping> drinkToppings = drinkToppingRepository.findByDrinkIdAndIsActiveTrue(drink.getId());
        List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
        
        DrinkDto dto = new DrinkDto();
        dto.setId(drink.getId());
        dto.setName(drink.getName());
        dto.setDescription(drink.getDescription());
        dto.setImageUrl(drink.getImageUrl());
        dto.setBasePrice(drink.getBasePrice());
        dto.setIsActive(drink.getIsActive());
        
        if (drink.getCategory() != null) {
            dto.setCategoryId(drink.getCategory().getId());
            dto.setCategoryName(drink.getCategory().getName());
        }
        
        List<DrinkSizeDto> sizes = drinkSizeRepository.findByDrinkId(drink.getId()).stream()
            .map(this::mapSizeToDto)
            .collect(Collectors.toList());
        dto.setSizes(sizes);
        
        List<DrinkToppingDto> toppings = new java.util.ArrayList<>();
        toppings.addAll(drinkToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
        toppings.addAll(globalToppings.stream().map(this::mapToppingToDto).collect(Collectors.toList()));
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
