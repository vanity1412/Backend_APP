package com.utetea.backend.service;

import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.mapper.DrinkCategoryMapper;
import com.utetea.backend.model.DrinkCategory;
import com.utetea.backend.repository.DrinkCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrinkCategoryService {
    
    private final DrinkCategoryRepository categoryRepository;
    private final DrinkCategoryMapper categoryMapper;
    
    @Transactional(readOnly = true)
    public List<DrinkCategoryDto> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
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
    
    @Transactional
    public DrinkCategoryDto createCategory(DrinkCategoryDto dto) {
        DrinkCategory category = categoryMapper.toEntity(dto);
        category.setId(null);
        DrinkCategory saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }
    
    @Transactional
    public DrinkCategoryDto updateCategory(Long id, DrinkCategoryDto dto) {
        DrinkCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setIsActive(dto.getIsActive());
        
        DrinkCategory updated = categoryRepository.save(category);
        return categoryMapper.toDto(updated);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        DrinkCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}
