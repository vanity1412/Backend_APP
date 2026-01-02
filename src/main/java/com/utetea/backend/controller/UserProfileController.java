package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.ChangePasswordRequest;
import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileDto>> uploadAvatar(
            Authentication authentication,
            @RequestParam("image") MultipartFile file) {
        try {
            String username = authentication.getName();

            // Kiểm tra file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File cannot be empty"));
            }
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Only image files are allowed"));
            }

            UserProfileDto profile = userProfileService.updateAvatar(username, file);
            return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully", profile));

        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi để debug
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> deleteAccount(Authentication authentication) {
        try {
            String username = authentication.getName();

            // GỌI SERVICE ĐỂ XÓA TÀI KHOẢN
            userProfileService.deleteAccount(username);

            // Lưu ý: Sau khi xóa, bạn có thể cần invalidation session/token,
            // nhưng phản hồi ban đầu thường là 200 OK hoặc 204 No Content.
            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));

        } catch (Exception e) {
            // Xử lý các ngoại lệ (ví dụ: User not found)
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
