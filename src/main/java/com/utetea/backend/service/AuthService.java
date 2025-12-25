package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import com.utetea.backend.repository.UserRepository;
import com.utetea.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;

//    @Transactional
//    public LoginResponse register(RegisterRequest request) {
//        // Validate username uniqueness
//        if (userRepository.existsByUsername(request.getUsername())) {
//            throw new BusinessException("Username already exists");
//        }
//
//        // Validate phone uniqueness (if provided)
//        if (request.getPhone() != null && !request.getPhone().isEmpty()
//            && userRepository.existsByPhone(request.getPhone())) {
//            throw new BusinessException("Phone already exists");
//        }
//
//        // Validate email uniqueness (if provided)
//        if (request.getEmail() != null && !request.getEmail().isEmpty()
//            && userRepository.existsByEmail(request.getEmail())) {
//            throw new BusinessException("Email already exists");
//        }
//
//        User user = new User();
//        user.setUsername(request.getUsername());
//        user.setEmail(request.getEmail());
//        user.setPhone(request.getPhone());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
//        user.setAddress(request.getAddress());
//        user.setRole(UserRole.USER);
//        user.setMemberTier(MemberTier.BRONZE);
//        user.setPoints(0);
//        user.setActive(true);
//        user.setIsBlocked(false);
//
//        user = userRepository.save(user);
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
//        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
//
//        return mapToLoginResponse(user, token);
//    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // FIX Low #16: Replaced System.out.println with proper logging
        log.debug("========== LOGIN SERVICE START ==========");
        log.debug("Login attempt for: {}", request.getUsernameOrPhone());

        // Authenticate with Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrPhone(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsernameOrPhone(
                request.getUsernameOrPhone(),
                request.getUsernameOrPhone()
        ).orElseThrow(() -> new BusinessException("Invalid credentials"));

        log.debug("User found - ID: {}", user.getId());
        log.debug("User found - Username: {}", user.getUsername());
        log.debug("User found - Role: {}", user.getRole());

        if (user.getIsBlocked()) {
            throw new BusinessException("Account is blocked");
        }

        if (!user.getActive()) {
            throw new BusinessException("Account is inactive");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtUtil.generateToken(userDetails, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.debug("Tokens generated successfully for user: {}", user.getUsername());

        LoginResponse response = mapToLoginResponse(user, accessToken, refreshToken);
        log.debug("========== LOGIN SERVICE END ==========");

        return response;
    }

    @Transactional
    public void registerWithOtp(RegisterRequest request) {
        // FIX Low #16: Replaced System.out.println with proper logging
        log.debug("================== START REGISTER WITH OTP ==================");
        log.debug("Request - Username: {}", request.getUsername());
        log.debug("Request - Phone: {}", request.getPhone());
        log.debug("Request - Email: {}", request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already exists - {}", request.getUsername());
            throw new BusinessException("Username already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new BusinessException("Email already exists");
        }

        log.debug("Validation passed, creating user...");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.USER);
        user.setMemberTier(MemberTier.BRONZE);
        user.setPoints(0);
        user.setActive(false);
        user.setIsBlocked(false);
        // --- LOGIC MỚI: TẠO OTP VÀ GÁN LUÔN TRƯỚC KHI SAVE ---
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Set thời hạn 5 phút

        log.debug("User object created, saving to database...");
        user = userRepository.save(user);
        log.debug("User saved! User ID: {}", user.getId());
        log.debug("User in DB - Username: {}, Active: {}", user.getUsername(), user.getActive());

        String email = request.getEmail();
        if (email == null || email.isEmpty()) {
            log.error("Email is null or empty for user: {}", request.getUsername());
            throw new BusinessException("Email is required for OTP registration");
        }

        log.debug("Calling otpService.sendOtp()...");
        try {
            otpService.sendOtp(otp, email);
            log.info("OTP sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Error in otpService.sendOtp(): {}", e.getMessage(), e);
            throw e;
        }

        log.debug("================== END REGISTER WITH OTP ==================");
    }

    @Transactional
    public LoginResponse verifyOtpAndActivate(String phone, String otp) {
        // Verify OTP
        if (!otpService.verifyOtp(phone, otp)) {
            throw new BusinessException("Invalid or expired OTP");
        }

        // Find user by phone
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Activate user
        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user = userRepository.save(user);

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return mapToLoginResponse(user, token, refreshToken);
    }

    @Transactional
    public void resendOtp(String phoneOrEmail) {
        User user = null;
        String email = null;
        String phone = null;
        String otp = otpService.generateOtp();

        if (phoneOrEmail != null && phoneOrEmail.contains("@")) {
            user = userRepository.findByEmail(phoneOrEmail).orElse(null);
            email = phoneOrEmail;
            user.setOtp(otp);
            user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Set thời hạn 5 phút

            if (user != null) phone = user.getPhone();
        } else {
            user = userRepository.findByPhone(phoneOrEmail).orElse(null);
            if (user != null) {
                phone = user.getPhone();
                email = user.getEmail();
                user.setOtp(otp);
                user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Set thời hạn 5 phút

            }
        }
        if (user == null) {
            throw new BusinessException("User not found");
        }
        if (email == null || email.isEmpty()) {
            throw new BusinessException("Email is not set for this user");
        }
        otpService.sendOtp(otp, email);
    }

    @Transactional
    public void verifyOtpAndActivateByEmail(String email, String otp) {
        // 1. Check OTP
        if (!otpService.verifyOtpByEmail(email, otp)) {
            throw new BusinessException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        // 2. Lấy User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        // 3. Kích hoạt User
        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);

        // 4. Lưu lại
        userRepository.save(user);

    }

    @Transactional
    public void initiateForgotPassword(ForgotPasswordRequest request) {
        // FIX Low #16: Replaced System.out.println with proper logging
        log.debug("========== FORGOT PASSWORD START ==========");

        // Tìm user theo Email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email không tồn tại trong hệ thống"));

        // Tạo OTP mới
        String otp = otpService.generateOtp();

        // TÁI SỬ DỤNG trường otp và otpExpiry cũ
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút

        userRepository.save(user);

        // Gửi Email
        log.debug("Sending Reset Password OTP to: {}", user.getEmail());
        try {
            otpService.sendOtp(otp, user.getEmail());
            log.info("Reset password OTP sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending reset password email: {}", e.getMessage());
            throw new BusinessException("Lỗi khi gửi email: " + e.getMessage());
        }
        log.debug("========== FORGOT PASSWORD END ==========");
    }

    // 2. Xác nhận OTP và đặt lại mật khẩu
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // FIX Low #16: Replaced System.out.println with proper logging
        log.debug("========== RESET PASSWORD START ==========");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Kiểm tra OTP (Dùng trường otp của User)
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new BusinessException("Mã OTP không chính xác");
        }

        // Kiểm tra hết hạn
        if (user.getOtpExpiry() != null &&
                user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("Mã OTP đã hết hạn");
        }

        // Đổi mật khẩu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Quan trọng: Xóa OTP sau khi dùng xong để bảo mật
        user.setOtp(null);
        user.setOtpExpiry(null);

        // (Tùy chọn) Nếu user đang bị inactive vì chưa verify lúc đăng ký,
        // thì đổi mật khẩu xong có cho active luôn không?
        // Thường là CÓ, vì họ đã chứng minh được quyền sở hữu email.
        if (!user.getActive()) {
            user.setActive(true);
        }

        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getUsername());
        log.debug("========== RESET PASSWORD END ==========");
    }

    private UserDetails convertUserToUserDetails(User user) {
        if (user == null) {
            return null;
        }

        // Tái tạo logic builder từ CustomUserDetailsService
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .accountExpired(false)
                .accountLocked(user.getIsBlocked()) // Sử dụng trường isBlocked
                .credentialsExpired(false)
                .disabled(!user.getActive()) // Sử dụng trường active
                .build();
    }

    @Transactional
    public JwtResponse refreshAccessToken(String oldRefreshToken) {
        log.info("Starting token refresh process.");

        // 1. Xác thực Refresh Token
        if (!jwtUtil.validateRefreshToken(oldRefreshToken)) {
            log.warn("Validation failed: Refresh token is invalid or expired.");
            throw new BusinessException("Invalid or expired refresh token");
        }
        log.debug("Refresh Token validated successfully.");

        // 2. Lấy tên người dùng từ token
        String username = null;
        try {
            username = jwtUtil.extractUsernameFromRefreshToken(oldRefreshToken);
            log.info("Extracted username from token: {}", username);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage(), e);
            throw new BusinessException("Failed to extract username from token.");
        }

        String finalUsername = username;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found in DB for username: {}", finalUsername);
                    return new BusinessException("User not found: " + finalUsername);
                });
        log.debug("User found in database: {}", user.getId());

        // Kiểm tra trạng thái hoạt động (Quan trọng cho bảo mật)
        if (!user.getActive()) {
            log.warn("User is inactive: {}", username);
            throw new BusinessException("User is inactive.");
        }
        log.debug("User is active.");

        // Chuyển đổi User Entity sang UserDetails
        UserDetails userDetails = convertUserToUserDetails(user);
        if (userDetails == null) {
            log.error("Failed to convert User Entity to UserDetails for: {}", username);
            throw new BusinessException("Internal error: Could not process user details.");
        }
        String role = user.getRole().name();
        log.debug("User details converted. Role: {}", role);

        // 3. Tạo Access Token và Refresh Token mới
        // Sử dụng phương thức gốc nhận UserDetails và role
        String newAccessToken = jwtUtil.generateToken(userDetails, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.info("New Access Token and Refresh Token generated.");
        log.debug("New Access Token (first 20 chars): {}", newAccessToken.substring(0, 20) + "...");
        log.debug("New Refresh Token (first 20 chars): {}", newRefreshToken.substring(0, 20) + "...");


        // 4. Trả về kết quả
        log.info("Token refresh process completed successfully for user: {}", username);
        return JwtResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationTime())
                .build();
    }

    private LoginResponse mapToLoginResponse(User user, String token, String refreshToken) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setPhone(user.getPhone());
        response.setFullName(user.getFullName());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setMemberTier(user.getMemberTier());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setAvatarUrl(user.getAvatarUrl());

        // FIX Low #16: Replaced System.out.println with proper logging
        log.debug("========== MAP TO LOGIN RESPONSE ==========");
        log.debug("User Role: {}, Response Role: {}", user.getRole(), response.getRole());
        log.debug("==========================================");

        return response;
    }
}
