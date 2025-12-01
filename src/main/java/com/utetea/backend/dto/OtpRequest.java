package com.utetea.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private String phone;
    private String email;
    
    @NotBlank(message = "OTP is required")
    private String otp;
}
