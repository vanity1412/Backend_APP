package com.utetea.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class JoinGroupOrderRequest {
    
    @NotBlank(message = "Mã mời không được để trống")
    @Size(min = 6, max = 10, message = "Mã mời phải từ 6-10 ký tự")
    private String inviteCode;
}
