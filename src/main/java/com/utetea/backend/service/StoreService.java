package com.utetea.backend.service;

import com.utetea.backend.dto.StoreDto;
import com.utetea.backend.dto.StoreWithManagersDto;
import com.utetea.backend.dto.UserDto;
import com.utetea.backend.model.Store;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.StoreRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.utetea.backend.config.CacheConfig.STORES_CACHE;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {
    
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = STORES_CACHE, key = "'all'")
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
    
    /**
     * Lấy danh sách stores kèm thông tin managers quản lý
     */
    @Transactional(readOnly = true)
    public List<StoreWithManagersDto> getAllStoresWithManagers() {
        log.debug("Fetching all stores with managers");
        List<Store> stores = storeRepository.findAll();
        
        // Lấy tất cả admins
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        List<UserDto> adminDtos = admins.stream()
            .map(this::mapToUserDto)
            .collect(Collectors.toList());
        
        return stores.stream()
            .map(store -> {
                StoreWithManagersDto dto = new StoreWithManagersDto();
                dto.setId(store.getId());
                dto.setStoreName(store.getStoreName());
                dto.setAddress(store.getAddress());
                dto.setLatitude(store.getLatitude());
                dto.setLongitude(store.getLongitude());
                dto.setOpenTime(store.getOpenTime());
                dto.setCloseTime(store.getCloseTime());
                dto.setPhone(store.getPhone());
                
                // Lấy managers của store này
                List<User> managers = userRepository.findManagersByStoreId(store.getId());
                List<UserDto> managerDtos = managers.stream()
                    .map(this::mapToUserDto)
                    .collect(Collectors.toList());
                dto.setManagers(managerDtos);
                dto.setAdmins(adminDtos);
                
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy managers của một store cụ thể
     */
    @Transactional(readOnly = true)
    public List<UserDto> getManagersByStoreId(Long storeId) {
        log.debug("Fetching managers for store: {}", storeId);
        List<User> managers = userRepository.findManagersByStoreId(storeId);
        return managers.stream()
            .map(this::mapToUserDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy tất cả admins
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllAdmins() {
        log.debug("Fetching all admins");
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        return admins.stream()
            .map(this::mapToUserDto)
            .collect(Collectors.toList());
    }
    
    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
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
