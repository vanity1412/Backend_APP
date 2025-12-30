package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.service.HttpSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    // Chỉ cần inject 1 service duy nhất
    private final HttpSmsService httpSmsService;

    // API 1: Gửi OTP (Chỉ cần truyền sđt, service tự tạo mã và gửi)
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestParam String phone) {
        boolean isSent = httpSmsService.sendOtp(phone);

        if (isSent) {
            return ResponseEntity.ok(ApiResponse.success("Đã gửi mã OTP thành công", null));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi gửi tin nhắn từ nhà mạng"));
        }
    }

    // API 2: Xác thực OTP
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @RequestParam String phone,
            @RequestParam String code) {

        boolean isValid = httpSmsService.verifyOtp(phone, code);

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("Xác thực thành công", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã OTP không đúng hoặc đã hết hạn"));
        }
    }
}