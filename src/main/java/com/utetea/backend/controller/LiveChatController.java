package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.LiveChatDto.*;
import com.utetea.backend.service.LiveChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LiveChatController {

    private final LiveChatService chatService;

    // ==================== USER APIs ====================

    /**
     * User bắt đầu cuộc hội thoại mới
     */
    @PostMapping("/conversations")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ConversationDto>> startConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StartConversationRequest request) {
        ConversationDto conversation = chatService.startConversation(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo cuộc hội thoại", conversation));
    }

    /**
     * Gửi tin nhắn
     */
    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SendMessageRequest request) {
        MessageDto message = chatService.sendMessage(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi tin nhắn", message));
    }

    /**
     * Lấy danh sách conversation của user
     */
    @GetMapping("/conversations/my")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ConversationListDto>>> getMyConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ConversationListDto> conversations = chatService.getUserConversations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * Lấy chi tiết conversation
     */
    @GetMapping("/conversations/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ConversationDto conversation = chatService.getConversation(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    /**
     * Đóng conversation
     */
    @PostMapping("/conversations/{id}/close")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ConversationDto>> closeConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ConversationDto conversation = chatService.closeConversation(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã đóng cuộc hội thoại", conversation));
    }

    // ==================== MANAGER APIs ====================

    /**
     * Manager lấy danh sách tất cả conversations (theo stores được gán)
     */
    @GetMapping("/manager/conversations")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ConversationListDto>>> getManagerConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ConversationListDto> conversations = chatService.getManagerConversations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * Đếm số conversation đang chờ (theo stores của Manager)
     */
    @GetMapping("/manager/conversations/waiting-count")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getWaitingCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long count = chatService.countWaitingConversations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
