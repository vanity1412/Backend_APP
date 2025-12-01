package com.utetea.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Username or phone is required")
    private String usernameOrPhone;
    
    @NotBlank(message = "Password is required")
    private String password;
}
