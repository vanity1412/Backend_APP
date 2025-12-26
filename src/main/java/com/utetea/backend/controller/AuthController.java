package com.utetea.backend.controller;

import com.utetea.backend.dto.*;
import com.utetea.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "🔐 Authentication", description = "Đăng ký, đăng nhập, JWT token")
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Kiểm tra service có hoạt động không")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is running"));
    }
    
//    @PostMapping("/register")
//    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản USER mới với username, phone, password")
//    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
//
//        LoginResponse response = authService.register(request);
//        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
//    }

    @PostMapping("/register-with-otp")
    @Operation(summary = "Đăng ký với OTP", description = "Bước 1: Đăng ký và gửi OTP qua email")
    public ResponseEntity<ApiResponse<String>> registerWithOtp(@Valid @RequestBody RegisterRequest request) {
        log.info("========== CONTROLLER START: /register-with-otp ==========");
        log.info("Request info - Username: {}, Email: {}", request.getUsername(), request.getEmail());

        // 1. Gọi Service
        authService.registerWithOtp(request);

        log.info("Service completed successfully. Returning response...");

        // 2. Trả về Response
        // Sử dụng hàm static 'success' có sẵn trong ApiResponse của bạn
        // Tham số 1: Message (Thông báo)
        // Tham số 2: Data (Dữ liệu kèm theo - ở đây là String hướng dẫn)
        return ResponseEntity.ok(ApiResponse.success(
                "OTP đã được gửi thành công!",
                "Vui lòng kiểm tra email: " + request.getEmail()
        ));
    }

    @PostMapping("/otp-verify")
    @Operation(summary = "Xác thực OTP", description = "Bước 2: Xác thực OTP và kích hoạt tài khoản (Không trả về Token)")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody com.utetea.backend.dto.OtpRequest request) {
        log.info("========== CONTROLLER START: /otp-verify ==========");

        // Gọi service (giờ service chỉ trả về void)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            authService.verifyOtpAndActivateByEmail(request.getEmail(), request.getOtp());
        }
        else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            authService.verifyOtpAndActivate(request.getPhone(), request.getOtp());
        }
        else {
            throw new com.utetea.backend.exception.BusinessException("Email or Phone is required");
        }

        // Trả về thông báo thành công dạng String
        // Android nhận được cái này sẽ Toast lên và chuyển về màn hình Login
        return ResponseEntity.ok(ApiResponse.success(
                "Kích hoạt tài khoản thành công!",
                "Vui lòng đăng nhập để tiếp tục."
        ));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP nếu hết hạn")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam(name = "target") String phoneOrEmail) {
        authService.resendOtp(phoneOrEmail);
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully"));
    }
    
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Login với username/phone + password, trả về JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiateForgotPassword(request);
        return ResponseEntity.ok("Mã xác nhận đã được gửi đến email của bạn.");
    }

    // API Đặt lại mật khẩu cho user quên mật khẩu
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công.");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            JwtResponse response = authService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        }
    }

}
