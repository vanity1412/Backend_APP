package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.service.DrinkCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminCategoryController {
    
    private final DrinkCategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DrinkCategoryDto>>> getAllCategories() {
        List<DrinkCategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<DrinkCategoryDto>> createCategory(@RequestBody DrinkCategoryDto dto) {
        try {
            DrinkCategoryDto created = categoryService.createCategory(dto);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DrinkCategoryDto>> updateCategory(
            @PathVariable Long id, 
            @RequestBody DrinkCategoryDto dto) {
        try {
            DrinkCategoryDto updated = categoryService.updateCategory(id, dto);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(ApiResponse.success("Category đã được ẩn"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
