package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DrinkDto;
import com.utetea.backend.service.DrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drinks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DrinkController {
    
    private final DrinkService drinkService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DrinkDto>>> getAllDrinks() {
        List<DrinkDto> drinks = drinkService.getAllActiveDrinks();
        return ResponseEntity.ok(ApiResponse.success(drinks));
    }
    
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<DrinkDto>>> getAllDrinksPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DrinkDto> drinks = drinkService.getAllActiveDrinks(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(drinks));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DrinkDto>> getDrinkById(@PathVariable Long id) {
        DrinkDto drink = drinkService.getDrinkById(id);
        return ResponseEntity.ok(ApiResponse.success(drink));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DrinkDto>>> searchDrinks(@RequestParam String keyword) {
        List<DrinkDto> drinks = drinkService.searchDrinks(keyword);
        return ResponseEntity.ok(ApiResponse.success(drinks));
    }
}
