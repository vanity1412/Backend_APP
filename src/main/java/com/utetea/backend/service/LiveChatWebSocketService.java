package com.utetea.backend.service;

import com.utetea.backend.dto.LiveChatDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service để gửi thông báo chat realtime qua WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChatWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Thông báo có conversation mới cho tất cả managers
     */
    public void notifyNewConversation(ConversationDto conversation) {
        log.info("Broadcasting new conversation #{} to managers", conversation.getId());
        messagingTemplate.convertAndSend("/topic/chat/conversations/new", conversation);
    }

    /**
     * Gửi tin nhắn mới đến conversation cụ thể
     */
    public void notifyNewMessage(Long conversationId, MessageDto message) {
        log.info("Broadcasting new message to conversation #{}", conversationId);
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId, message);
    }

    /**
     * Thông báo conversation đã được tiếp nhận
     */
    public void notifyConversationAccepted(ConversationDto conversation) {
        log.info("Broadcasting conversation #{} accepted", conversation.getId());
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversation.getId() + "/status", conversation);
    }

    /**
     * Thông báo conversation đã đóng
     */
    public void notifyConversationClosed(Long conversationId) {
        log.info("Broadcasting conversation #{} closed", conversationId);
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/closed", conversationId);
    }

    /**
     * Thông báo typing indicator
     */
    public void notifyTyping(Long conversationId, Long userId, boolean isTyping) {
        var payload = new TypingPayload(conversationId, userId, isTyping);
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/typing", payload);
    }

    public record TypingPayload(Long conversationId, Long userId, boolean isTyping) {}
}
