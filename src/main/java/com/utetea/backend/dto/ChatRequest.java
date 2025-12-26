package com.utetea.backend.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private Long userId;
}
