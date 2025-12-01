package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.StoreDto;
import com.utetea.backend.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreController {
    
    private final StoreService storeService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreDto>>> getAllStores() {
        List<StoreDto> stores = storeService.getAllStores();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreDto>> getStoreById(@PathVariable Long id) {
        StoreDto store = storeService.getStoreById(id);
        return ResponseEntity.ok(ApiResponse.success(store));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StoreDto>>> searchStores(@RequestParam String keyword) {
        List<StoreDto> stores = storeService.searchStores(keyword);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }
}
