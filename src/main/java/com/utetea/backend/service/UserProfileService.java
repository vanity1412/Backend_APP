package com.utetea.backend.service;

import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        return mapToProfileDto(user);
    }
    
    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        // Check if phone is being changed and already exists
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException("Phone number already exists");
            }
            user.setPhone(request.getPhone());
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        
        user = userRepository.save(user);
        return mapToProfileDto(user);
    }
    
    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .role(user.getRole())
                .memberTier(user.getMemberTier())
                .points(user.getPoints())
                .active(user.getActive())
                .build();
    }
}
