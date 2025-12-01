package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.service.DrinkCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DrinkCategoryController {
    
    private final DrinkCategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DrinkCategoryDto>>> getAllActiveCategories() {
        List<DrinkCategoryDto> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DrinkCategoryDto>> getCategoryById(@PathVariable Long id) {
        try {
            DrinkCategoryDto category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(ApiResponse.success(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
