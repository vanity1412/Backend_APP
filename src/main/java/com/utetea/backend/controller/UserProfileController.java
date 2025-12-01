package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('USER', 'MANAGER')")
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileDto profile = userProfileService.getProfile(username);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            String username = authentication.getName();
            UserProfileDto profile = userProfileService.updateProfile(username, request);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
