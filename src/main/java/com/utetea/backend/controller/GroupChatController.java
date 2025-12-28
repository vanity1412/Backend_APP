package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.GroupChatMessageDto;
import com.utetea.backend.dto.SendGroupChatRequest;
import com.utetea.backend.service.GroupChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/group-orders/{groupOrderId}/chat")
@RequiredArgsConstructor
@Tag(name = "Group Chat", description = "API chat nhóm trong phiên đặt hàng")
public class GroupChatController {
    
    private final GroupChatService groupChatService;
    
    @PostMapping
    @Operation(summary = "Gửi tin nhắn vào nhóm chat")
    public ResponseEntity<ApiResponse<GroupChatMessageDto>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupOrderId,
            @Valid @RequestBody SendGroupChatRequest request) {
        GroupChatMessageDto result = groupChatService.sendMessage(
                userDetails.getUsername(), groupOrderId, request);
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", result));
    }
    
    @GetMapping
    @Operation(summary = "Lấy lịch sử chat của nhóm")
    public ResponseEntity<ApiResponse<List<GroupChatMessageDto>>> getChatHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupOrderId) {
        List<GroupChatMessageDto> result = groupChatService.getChatHistory(
                userDetails.getUsername(), groupOrderId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Lấy tin nhắn gần đây (mặc định 50 tin)")
    public ResponseEntity<ApiResponse<List<GroupChatMessageDto>>> getRecentMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupOrderId,
            @RequestParam(defaultValue = "50") int limit) {
        List<GroupChatMessageDto> result = groupChatService.getRecentMessages(
                userDetails.getUsername(), groupOrderId, limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    // WebSocket endpoint để gửi tin nhắn realtime
    // Client gửi đến: /app/group-chat/{groupOrderId}
    // Server broadcast đến: /topic/group-chat/{groupOrderId}
    @MessageMapping("/group-chat/{groupOrderId}")
    public void handleWebSocketMessage(
            @DestinationVariable Long groupOrderId,
            @Payload SendGroupChatRequest request,
            Principal principal) {
        if (principal != null) {
            groupChatService.sendMessage(principal.getName(), groupOrderId, request);
        }
    }
}
