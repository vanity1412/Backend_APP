package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.service.DrinkService;
import com.utetea.backend.service.DrinkImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/drinks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDrinkController {
    
    private final DrinkService drinkService;
    private final DrinkImageUploadService drinkImageUploadService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<DrinkDto>> createDrink(@Valid @RequestBody DrinkDto request) {
        try {
            DrinkDto drink = drinkService.createDrink(request);
            return ResponseEntity.ok(ApiResponse.success("Drink created successfully", drink));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DrinkDto>> updateDrink(
            @PathVariable Long id,
            @Valid @RequestBody DrinkDto request) {
        try {
            DrinkDto drink = drinkService.updateDrink(id, request);
            return ResponseEntity.ok(ApiResponse.success("Drink updated successfully", drink));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDrink(@PathVariable Long id) {
        try {
            drinkService.deleteDrink(id);
            return ResponseEntity.ok(ApiResponse.success("Drink deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Upload ảnh drink lên GitHub và trả về URL
     * @param file File ảnh cần upload
     * @param drinkName Tên drink để đặt tên file (optional)
     * @return URL của ảnh đã upload
     */
    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDrinkImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "drinkName", defaultValue = "drink") String drinkName) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File không được để trống"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chỉ chấp nhận file ảnh"));
            }
            
            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File không được vượt quá 5MB"));
            }
            
            String imageUrl = drinkImageUploadService.uploadDrinkImage(file, drinkName);
            
            Map<String, String> result = Map.of("imageUrl", imageUrl);
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", result));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}
