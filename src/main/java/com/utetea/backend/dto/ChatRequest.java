package com.utetea.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
    
    private Long userId; // Optional
}
