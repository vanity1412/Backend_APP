package com.utetea.backend.mapper;

import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.model.DrinkCategory;
import org.springframework.stereotype.Component;

@Component
public class DrinkCategoryMapper {
    
    public DrinkCategoryDto toDto(DrinkCategory category) {
        if (category == null) return null;
        
        return DrinkCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .build();
    }
    
    public DrinkCategory toEntity(DrinkCategoryDto dto) {
        if (dto == null) return null;
        
        DrinkCategory category = new DrinkCategory();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setIsActive(dto.getIsActive());
        
        return category;
    }
}
