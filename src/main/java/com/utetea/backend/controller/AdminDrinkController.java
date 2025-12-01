package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.service.DrinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/drinks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDrinkController {
    
    private final DrinkService drinkService;
    
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
}
