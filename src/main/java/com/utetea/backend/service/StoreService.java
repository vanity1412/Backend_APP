package com.utetea.backend.service;

import com.utetea.backend.dto.StoreDto;
import com.utetea.backend.model.Store;
import com.utetea.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {
    
    private final StoreRepository storeRepository;
    
    @Transactional(readOnly = true)
    public List<StoreDto> getAllStores() {
        return storeRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public StoreDto getStoreById(Long id) {
        Store store = storeRepository.findById(id)
            .orElseThrow(() -> new com.utetea.backend.exception.ResourceNotFoundException("Store", "id", id));
        return mapToDto(store);
    }
    
    @Transactional(readOnly = true)
    public List<StoreDto> searchStores(String keyword) {
        return storeRepository.searchStores(keyword).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    private StoreDto mapToDto(Store store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId());
        dto.setStoreName(store.getStoreName());
        dto.setAddress(store.getAddress());
        dto.setLatitude(store.getLatitude());
        dto.setLongitude(store.getLongitude());
        dto.setOpenTime(store.getOpenTime());
        dto.setCloseTime(store.getCloseTime());
        dto.setPhone(store.getPhone());
        return dto;
    }
}
