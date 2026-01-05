package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {
    
    private final ChallengeService challengeService;
    
    /**
     * Lấy trạng thái challenge của user hiện tại
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ChallengeStatusDto> getChallengeStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(challengeService.getUserChallengeStatus(userDetails.getUsername()));
    }
    
    /**
     * Lấy danh sách tất cả challenge (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ChallengeDto>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }
    
    /**
     * Tạo challenge mới (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChallengeDto> createChallenge(@RequestBody ChallengeDto dto) {
        return ResponseEntity.ok(challengeService.createChallenge(dto));
    }
    
    /**
     * Cập nhật challenge (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChallengeDto> updateChallenge(
            @PathVariable Long id,
            @RequestBody ChallengeDto dto) {
        return ResponseEntity.ok(challengeService.updateChallenge(id, dto));
    }
}
