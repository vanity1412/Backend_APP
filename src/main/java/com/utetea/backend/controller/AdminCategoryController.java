package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkCategoryDto;
import com.utetea.backend.service.DrinkCategoryService;
import com.utetea.backend.service.DrinkImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminCategoryController {
    
    private final DrinkCategoryService categoryService;
    private final DrinkImageUploadService imageUploadService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DrinkCategoryDto>>> getAllCategories() {
        List<DrinkCategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<DrinkCategoryDto>> createCategory(@Valid @RequestBody DrinkCategoryDto dto) {
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
            @Valid @RequestBody DrinkCategoryDto dto) {
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
    
    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCategoryImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryName", defaultValue = "category") String categoryName) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File không được để trống"));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chỉ chấp nhận file ảnh"));
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File không được vượt quá 5MB"));
            }
            String imageUrl = imageUploadService.uploadDrinkImage(file, "category_" + categoryName);
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", Map.of("imageUrl", imageUrl)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}
