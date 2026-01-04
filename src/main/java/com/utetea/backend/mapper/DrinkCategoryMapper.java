package com.utetea.backend.mapper;

import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.model.DrinkCategory;
import com.utetea.backend.repository.DrinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrinkCategoryMapper {
    
    private final DrinkRepository drinkRepository;
    
    public DrinkCategoryDto toDto(DrinkCategory category) {
        if (category == null) return null;
        
        // Đếm số lượng drink trong category
        int drinkCount = drinkRepository.countByCategoryIdAndIsActiveTrue(category.getId());
        
        return DrinkCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .drinkCount(drinkCount)
                .build();
    }
    
    public DrinkCategoryDto toDtoWithAllDrinks(DrinkCategory category) {
        if (category == null) return null;
        
        // Đếm tất cả drink (kể cả inactive) trong category
        int drinkCount = drinkRepository.countByCategoryId(category.getId());
        
        return DrinkCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .drinkCount(drinkCount)
                .build();
    }
    
    public DrinkCategory toEntity(DrinkCategoryDto dto) {
        if (dto == null) return null;
        
        DrinkCategory category = new DrinkCategory();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setIsActive(dto.getIsActive());
        
        return category;
    }
}
