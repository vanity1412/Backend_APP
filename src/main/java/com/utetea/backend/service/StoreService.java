package com.utetea.backend.service;

import com.utetea.backend.dto.StoreDto;
import com.utetea.backend.model.Store;
import com.utetea.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {
    
    private final StoreRepository storeRepository;
    
    @Transactional(readOnly = true)
    public List<StoreDto> getAllStores() {
        log.debug("Fetching all stores");
        List<StoreDto> stores = storeRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
        log.debug("Found {} stores", stores.size());
        return stores;
    }
    
    @Transactional(readOnly = true)
    public StoreDto getStoreById(Long id) {
        log.debug("Fetching store by id: {}", id);
        Store store = storeRepository.findById(id)
            .orElseThrow(() -> new com.utetea.backend.exception.ResourceNotFoundException("Store", "id", id));
        return mapToDto(store);
    }
    
    @Transactional(readOnly = true)
    public List<StoreDto> searchStores(String keyword) {
        log.debug("Searching stores with keyword: {}", keyword);
        List<StoreDto> stores = storeRepository.searchStores(keyword).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
        log.debug("Found {} stores matching keyword: {}", stores.size(), keyword);
        return stores;
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
