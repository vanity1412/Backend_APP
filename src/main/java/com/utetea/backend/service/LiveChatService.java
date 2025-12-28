package com.utetea.backend.service;

import com.utetea.backend.dto.LiveChatDto.*;
import com.utetea.backend.exception.BusinessException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChatService {

    private final ChatConversationRepository conversationRepository;
    private final LiveChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LiveChatWebSocketService webSocketService;

    /**
     * User bắt đầu cuộc hội thoại mới hoặc tiếp tục conversation đang mở
     */
    @Transactional
    public ConversationDto startConversation(String username, StartConversationRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Kiểm tra xem user đã có conversation đang active chưa
        // Nếu có, trả về conversation đó thay vì tạo mới
        var existingConversation = conversationRepository.findActiveByUserId(user.getId());
        if (existingConversation.isPresent()) {
            ChatConversation conversation = existingConversation.get();
            
            // Nếu có tin nhắn mới, thêm vào conversation hiện tại
            if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
                LiveChatMessage message = new LiveChatMessage();
                message.setConversation(conversation);
                message.setSender(user);
                message.setContent(request.getInitialMessage());
                message.setSenderType(SenderType.USER);
                messageRepository.save(message);

                conversation.setLastMessage(request.getInitialMessage());
                conversation.setUnreadManager(conversation.getUnreadManager() + 1);
                conversationRepository.save(conversation);
                
                // Notify qua WebSocket
                MessageDto msgDto = mapMessageToDto(message);
                webSocketService.notifyNewMessage(conversation.getId(), msgDto);
            }
            
            log.info("User {} continuing existing conversation #{}", username, conversation.getId());
            return mapToDto(conversationRepository.findByIdWithMessages(conversation.getId()).orElse(conversation));
        }

        // Tạo conversation mới
        ChatConversation conversation = new ChatConversation();
        conversation.setUser(user);
        conversation.setStatus(ConversationStatus.WAITING);
        conversation.setSubject(request.getSubject());
        conversation.setUnreadManager(1);

        conversation = conversationRepository.save(conversation);

        // Thêm tin nhắn đầu tiên
        if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
            LiveChatMessage message = new LiveChatMessage();
            message.setConversation(conversation);
            message.setSender(user);
            message.setContent(request.getInitialMessage());
            message.setSenderType(SenderType.USER);
            messageRepository.save(message);

            conversation.setLastMessage(request.getInitialMessage());
            conversationRepository.save(conversation);
        }

        log.info("User {} started new conversation #{}", username, conversation.getId());

        // Notify managers về conversation mới
        ConversationDto dto = mapToDto(conversation);
        webSocketService.notifyNewConversation(dto);

        return dto;
    }


    /**
     * Gửi tin nhắn
     */
    @Transactional
    public MessageDto sendMessage(String username, SendMessageRequest request) {
        User sender = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ChatConversation conversation = conversationRepository.findById(request.getConversationId())
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

        // Kiểm tra conversation đã đóng chưa
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new BusinessException("Cuộc hội thoại đã đóng, không thể gửi tin nhắn");
        }

        // Validate quyền gửi tin nhắn
        boolean isUser = conversation.getUser().getId().equals(sender.getId());
        boolean isManager = sender.getRole() == UserRole.MANAGER;

        if (!isUser && !isManager) {
            throw new BusinessException("Bạn không có quyền gửi tin nhắn trong cuộc hội thoại này");
        }

        // Tạo tin nhắn
        LiveChatMessage message = new LiveChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setSenderType(isManager ? SenderType.MANAGER : SenderType.USER);
        message = messageRepository.save(message);

        // Cập nhật conversation
        conversation.setLastMessage(request.getContent());
        if (isUser) {
            conversation.setUnreadManager(conversation.getUnreadManager() + 1);
        } else {
            conversation.setUnreadUser(conversation.getUnreadUser() + 1);
            // Manager tiếp nhận conversation
            if (conversation.getStatus() == ConversationStatus.WAITING) {
                conversation.setStatus(ConversationStatus.ACTIVE);
                conversation.setManager(sender);
            }
        }
        conversationRepository.save(conversation);

        log.info("Message sent in conversation #{} by {}", conversation.getId(), username);

        // Notify qua WebSocket
        MessageDto dto = mapMessageToDto(message);
        webSocketService.notifyNewMessage(conversation.getId(), dto);

        return dto;
    }

    /**
     * Lấy danh sách conversation của user
     */
    @Transactional(readOnly = true)
    public List<ConversationListDto> getUserConversations(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
            .stream()
            .map(this::mapToListDto)
            .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách conversation cho manager
     */
    @Transactional(readOnly = true)
    public List<ConversationListDto> getManagerConversations(String username) {
        User manager = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return conversationRepository.findForManager(manager.getId())
            .stream()
            .map(c -> {
                ConversationListDto dto = mapToListDto(c);
                dto.setUnreadCount(c.getUnreadManager());
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết conversation với messages
     */
    @Transactional
    public ConversationDto getConversation(String username, Long conversationId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ChatConversation conversation = conversationRepository.findByIdWithMessages(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        // Validate quyền xem
        boolean isUser = conversation.getUser().getId().equals(user.getId());
        boolean isManager = user.getRole() == UserRole.MANAGER;

        if (!isUser && !isManager) {
            throw new BusinessException("Bạn không có quyền xem cuộc hội thoại này");
        }

        // Đánh dấu đã đọc
        if (isUser) {
            conversation.setUnreadUser(0);
            messageRepository.markAsRead(conversationId, SenderType.MANAGER);
        } else {
            conversation.setUnreadManager(0);
            messageRepository.markAsRead(conversationId, SenderType.USER);
        }
        conversationRepository.save(conversation);

        return mapToDto(conversation);
    }

    /**
     * Đóng conversation
     */
    @Transactional
    public ConversationDto closeConversation(String username, Long conversationId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ChatConversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        boolean isUser = conversation.getUser().getId().equals(user.getId());
        boolean isManager = user.getRole() == UserRole.MANAGER;

        if (!isUser && !isManager) {
            throw new BusinessException("Bạn không có quyền đóng cuộc hội thoại này");
        }

        conversation.setStatus(ConversationStatus.CLOSED);
        conversation = conversationRepository.save(conversation);

        // Thêm system message
        LiveChatMessage systemMsg = new LiveChatMessage();
        systemMsg.setConversation(conversation);
        systemMsg.setSender(user);
        systemMsg.setContent("Cuộc hội thoại đã được đóng bởi " + (isManager ? "nhân viên" : "khách hàng"));
        systemMsg.setSenderType(SenderType.SYSTEM);
        messageRepository.save(systemMsg);

        log.info("Conversation #{} closed by {}", conversationId, username);

        // Notify qua WebSocket
        webSocketService.notifyConversationClosed(conversationId);

        return mapToDto(conversation);
    }

    /**
     * Đếm số conversation đang chờ
     */
    public Long countWaitingConversations() {
        return conversationRepository.countWaitingConversations();
    }

    // ==================== MAPPING ====================

    private ConversationDto mapToDto(ChatConversation c) {
        return ConversationDto.builder()
            .id(c.getId())
            .userId(c.getUser().getId())
            .userName(c.getUser().getFullName())
            .userAvatar(c.getUser().getAvatarUrl())
            .managerId(c.getManager() != null ? c.getManager().getId() : null)
            .managerName(c.getManager() != null ? c.getManager().getFullName() : null)
            .status(c.getStatus())
            .subject(c.getSubject())
            .lastMessage(c.getLastMessage())
            .lastMessageTime(c.getUpdatedAt() != null ? c.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .unreadUser(c.getUnreadUser())
            .unreadManager(c.getUnreadManager())
            .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .messages(c.getMessages() != null ? c.getMessages().stream().map(this::mapMessageToDto).collect(Collectors.toList()) : null)
            .build();
    }

    private ConversationListDto mapToListDto(ChatConversation c) {
        return ConversationListDto.builder()
            .id(c.getId())
            .userId(c.getUser().getId())
            .userName(c.getUser().getFullName())
            .userAvatar(c.getUser().getAvatarUrl())
            .status(c.getStatus())
            .subject(c.getSubject())
            .lastMessage(c.getLastMessage())
            .lastMessageTime(c.getUpdatedAt() != null ? c.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .unreadCount(c.getUnreadUser())
            .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .build();
    }

    private MessageDto mapMessageToDto(LiveChatMessage m) {
        return MessageDto.builder()
            .id(m.getId())
            .conversationId(m.getConversation().getId())
            .senderId(m.getSender().getId())
            .senderName(m.getSender().getFullName())
            .senderAvatar(m.getSender().getAvatarUrl())
            .content(m.getContent())
            .senderType(m.getSenderType())
            .isRead(m.getIsRead())
            .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .build();
    }
}
