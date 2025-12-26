package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.ChatRequest;
import com.utetea.backend.dto.ChatResponse;
import com.utetea.backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            log.info("Chatbot request: {}", request.getMessage());
            ChatResponse response = chatbotService.processMessage(request.getMessage(), request.getUserId());
            log.info("Chatbot response type: {}", response.getType());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Chatbot error: ", e);
            ChatResponse errorResponse = ChatResponse.builder()
                .message("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.")
                .type("TEXT")
                .build();
            return ResponseEntity.ok(ApiResponse.success(errorResponse));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Chatbot is running!"));
    }
}
