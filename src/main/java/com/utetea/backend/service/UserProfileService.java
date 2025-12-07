package com.utetea.backend.service;

import com.utetea.backend.dto.UpdateProfileRequest;
import com.utetea.backend.dto.UserProfileDto;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.User;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.utetea.backend.dto.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AvatarUploadService avatarUploadService;
    private final PasswordEncoder passwordEncoder;

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

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        user = userRepository.save(user);
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateAvatar(String username, MultipartFile file) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // 1. Upload lên GitHub và lấy link
        String avatarUrl = avatarUploadService.uploadFile(file, String.valueOf(user.getId()));

        // 2. Lưu link vào Database
        // Giả sử entity User của bạn có setAvatarUrl
        user.setAvatarUrl(avatarUrl);
        user = userRepository.save(user);

        return mapToProfileDto(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu cũ không chính xác");
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu (nếu cần logic này ở Backend)
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        // Mã hóa mật khẩu mới và lưu xuống DB
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String username) {
        // Tìm người dùng
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Thực hiện xóa cart trước để không xung đột khóa ngoại
        cartRepository.deleteByUserId(user.getId());

        // Thực hiện xóa
        userRepository.delete(user);

        // Tùy chọn: Nếu muốn khóa thay vì xóa vĩnh viễn:
        // user.setIsBlocked(true);
        // userRepository.save(user);
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .role(user.getRole())
                .memberTier(user.getMemberTier())
                .points(user.getPoints())
                .active(user.getActive())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
