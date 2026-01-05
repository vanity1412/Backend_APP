package com.utetea.backend.service;

import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.mapper.DrinkCategoryMapper;
import com.utetea.backend.model.DrinkCategory;
import com.utetea.backend.repository.DrinkCategoryRepository;
import com.utetea.backend.repository.DrinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.utetea.backend.config.CacheConfig.CATEGORIES_CACHE;

@Service
@RequiredArgsConstructor
public class DrinkCategoryService {
    
    private final DrinkCategoryRepository categoryRepository;
    private final DrinkCategoryMapper categoryMapper;
    private final DrinkRepository drinkRepository;
    private final DrinkService drinkService;
    
    @Transactional(readOnly = true)
    @Cacheable(value = CATEGORIES_CACHE, key = "'all-active'")
    public List<DrinkCategoryDto> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CATEGORIES_CACHE, key = "'all'")
    public List<DrinkCategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public DrinkCategoryDto getCategoryById(Long id) {
        DrinkCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return categoryMapper.toDto(category);
    }
    
    @Transactional(readOnly = true)
    public List<DrinkDto> getDrinksByCategory(Long categoryId) {
        // Kiểm tra category có tồn tại không
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        // Lấy tất cả drinks thuộc category này và đang active
        return drinkRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream()
                .map(drink -> drinkService.getDrinkById(drink.getId()))
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = CATEGORIES_CACHE, allEntries = true)
    public DrinkCategoryDto createCategory(DrinkCategoryDto dto) {
        DrinkCategory category = categoryMapper.toEntity(dto);
        category.setId(null);
        DrinkCategory saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }
    
    @Transactional
    @CacheEvict(value = CATEGORIES_CACHE, allEntries = true)
    public DrinkCategoryDto updateCategory(Long id, DrinkCategoryDto dto) {
        DrinkCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setIsActive(dto.getIsActive());
        
        DrinkCategory updated = categoryRepository.save(category);
        return categoryMapper.toDto(updated);
    }
    
    @Transactional
    @CacheEvict(value = CATEGORIES_CACHE, allEntries = true)
    public void deleteCategory(Long id) {
        DrinkCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}
