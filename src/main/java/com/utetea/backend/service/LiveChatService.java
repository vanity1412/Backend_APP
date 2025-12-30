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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChatService {

    private final ChatConversationRepository conversationRepository;
    private final LiveChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final LiveChatWebSocketService webSocketService;
    private final OneSignalService oneSignalService;

    /**
     * User bắt đầu cuộc hội thoại mới hoặc tiếp tục conversation đang mở
     */
    @Transactional
    public ConversationDto startConversation(String username, StartConversationRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Validate storeId - bắt buộc phải chọn chi nhánh
        if (request.getStoreId() == null) {
            throw new BusinessException("Vui lòng chọn chi nhánh để được tư vấn");
        }
        
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));

        // Kiểm tra xem user đã có conversation đang active với store này chưa
        var existingConversation = conversationRepository.findActiveByUserIdAndStoreId(user.getId(), store.getId());
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
                
                // Gửi push notification cho Manager của store và Admin
                sendPushToManagersAndAdmin(store, user, "💬 Tin nhắn tư vấn mới", 
                        user.getFullName() + ": " + truncateMessage(request.getInitialMessage()), 
                        conversation.getId());
            }
            
            log.info("User {} continuing existing conversation #{} at store {}", username, conversation.getId(), store.getStoreName());
            return mapToDto(conversationRepository.findByIdWithMessages(conversation.getId()).orElse(conversation));
        }

        // Tạo conversation mới
        ChatConversation conversation = new ChatConversation();
        conversation.setUser(user);
        conversation.setStore(store);
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

        log.info("User {} started new conversation #{} at store {}", username, conversation.getId(), store.getStoreName());

        // Notify managers về conversation mới (chỉ managers quản lý store này)
        ConversationDto dto = mapToDto(conversation);
        webSocketService.notifyNewConversation(dto);

        // Gửi push notification cho Manager của store và Admin
        sendPushToManagersAndAdmin(store, user, "💬 Yêu cầu tư vấn mới", 
                user.getFullName() + " cần tư vấn tại " + store.getStoreName(), 
                conversation.getId());

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
        boolean isAdmin = sender.getRole() == UserRole.ADMIN;
        boolean isManager = sender.getRole() == UserRole.MANAGER;
        
        // Manager phải quản lý store của conversation này
        if (isManager && !isAdmin) {
            if (!canManagerAccessConversation(sender, conversation)) {
                throw new BusinessException("Bạn không có quyền tư vấn cho chi nhánh này");
            }
        }

        if (!isUser && !isManager && !isAdmin) {
            throw new BusinessException("Bạn không có quyền gửi tin nhắn trong cuộc hội thoại này");
        }

        // Tạo tin nhắn
        LiveChatMessage message = new LiveChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setSenderType((isManager || isAdmin) ? SenderType.MANAGER : SenderType.USER);
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

        // Gửi push notification
        if (isUser) {
            // User gửi tin nhắn → Thông báo cho Manager của store và Admin
            sendPushToManagersAndAdmin(conversation.getStore(), sender, "💬 Tin nhắn tư vấn mới",
                    sender.getFullName() + ": " + truncateMessage(request.getContent()),
                    conversation.getId());
        } else {
            // Manager/Admin trả lời → Thông báo cho User
            sendPushToUser(conversation.getUser(), "💬 Phản hồi từ " + 
                    (conversation.getStore() != null ? conversation.getStore().getStoreName() : "Nhân viên"),
                    sender.getFullName() + ": " + truncateMessage(request.getContent()),
                    conversation.getId());
        }

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
     * Manager chỉ thấy conversation của stores mình quản lý
     * ADMIN thấy tất cả
     */
    @Transactional(readOnly = true)
    public List<ConversationListDto> getManagerConversations(String username) {
        User manager = userRepository.findByUsernameWithManagedStores(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<ChatConversation> conversations;
        
        if (manager.getRole() == UserRole.ADMIN) {
            // ADMIN thấy tất cả
            conversations = conversationRepository.findForManager(manager.getId());
        } else {
            // Manager chỉ thấy conversations của stores được gán
            Set<Store> managedStores = manager.getManagedStores();
            if (managedStores == null || managedStores.isEmpty()) {
                log.warn("Manager {} has no assigned stores, returning empty conversations", username);
                return List.of();
            }
            
            List<Long> storeIds = managedStores.stream()
                .map(Store::getId)
                .collect(Collectors.toList());
            
            conversations = conversationRepository.findByStoreIdIn(storeIds);
        }
        
        return conversations.stream()
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
        User user = userRepository.findByUsernameWithManagedStores(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ChatConversation conversation = conversationRepository.findByIdWithMessages(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        // Validate quyền xem
        boolean isUser = conversation.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isManager = user.getRole() == UserRole.MANAGER;
        
        // Manager phải quản lý store của conversation này
        if (isManager && !isAdmin && !isUser) {
            if (!canManagerAccessConversation(user, conversation)) {
                throw new BusinessException("Bạn không có quyền xem cuộc hội thoại của chi nhánh này");
            }
        }

        if (!isUser && !isManager && !isAdmin) {
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
        User user = userRepository.findByUsernameWithManagedStores(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        ChatConversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        boolean isUser = conversation.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isManager = user.getRole() == UserRole.MANAGER;
        
        // Manager phải quản lý store của conversation này
        if (isManager && !isAdmin && !isUser) {
            if (!canManagerAccessConversation(user, conversation)) {
                throw new BusinessException("Bạn không có quyền đóng cuộc hội thoại của chi nhánh này");
            }
        }

        if (!isUser && !isManager && !isAdmin) {
            throw new BusinessException("Bạn không có quyền đóng cuộc hội thoại này");
        }

        conversation.setStatus(ConversationStatus.CLOSED);
        conversation = conversationRepository.save(conversation);

        // Thêm system message
        LiveChatMessage systemMsg = new LiveChatMessage();
        systemMsg.setConversation(conversation);
        systemMsg.setSender(user);
        systemMsg.setContent("Cuộc hội thoại đã được đóng bởi " + ((isManager || isAdmin) ? "nhân viên" : "khách hàng"));
        systemMsg.setSenderType(SenderType.SYSTEM);
        messageRepository.save(systemMsg);

        log.info("Conversation #{} closed by {}", conversationId, username);

        // Notify qua WebSocket
        webSocketService.notifyConversationClosed(conversationId);

        return mapToDto(conversation);
    }

    /**
     * Đếm số conversation đang chờ (cho Manager theo stores được gán)
     */
    @Transactional(readOnly = true)
    public Long countWaitingConversations(String username) {
        User manager = userRepository.findByUsernameWithManagedStores(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        if (manager.getRole() == UserRole.ADMIN) {
            return conversationRepository.countWaitingConversations();
        }
        
        Set<Store> managedStores = manager.getManagedStores();
        if (managedStores == null || managedStores.isEmpty()) {
            return 0L;
        }
        
        List<Long> storeIds = managedStores.stream()
            .map(Store::getId)
            .collect(Collectors.toList());
        
        return conversationRepository.countWaitingByStoreIds(storeIds);
    }
    
    /**
     * Kiểm tra Manager có quyền truy cập conversation không
     */
    private boolean canManagerAccessConversation(User manager, ChatConversation conversation) {
        // ADMIN có quyền truy cập tất cả
        if (manager.getRole() == UserRole.ADMIN) {
            return true;
        }
        
        // Conversation không có store -> cho phép (backward compatible)
        if (conversation.getStore() == null) {
            return true;
        }
        
        // Manager phải quản lý store của conversation
        Set<Store> managedStores = manager.getManagedStores();
        if (managedStores == null || managedStores.isEmpty()) {
            return false;
        }
        
        return managedStores.stream()
            .anyMatch(s -> s.getId().equals(conversation.getStore().getId()));
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
            .storeId(c.getStore() != null ? c.getStore().getId() : null)
            .storeName(c.getStore() != null ? c.getStore().getStoreName() : null)
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
            .storeId(c.getStore() != null ? c.getStore().getId() : null)
            .storeName(c.getStore() != null ? c.getStore().getStoreName() : null)
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

    // ==================== PUSH NOTIFICATION HELPERS ====================

    /**
     * Gửi push notification cho Manager của store và tất cả Admin
     */
    private void sendPushToManagersAndAdmin(Store store, User excludeUser, String title, String content, Long conversationId) {
        try {
            // Lấy tất cả Admin
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);
            
            // Lấy Manager quản lý store này
            List<User> managers = store != null ? 
                    userRepository.findManagersByStoreId(store.getId()) : 
                    userRepository.findByRole(UserRole.MANAGER);
            
            // Gộp danh sách và loại bỏ user gửi tin nhắn
            java.util.Set<Long> userIds = new java.util.HashSet<>();
            admins.forEach(u -> userIds.add(u.getId()));
            managers.forEach(u -> userIds.add(u.getId()));
            userIds.remove(excludeUser.getId());
            
            if (userIds.isEmpty()) return;
            
            String[] userIdsArray = userIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            
            oneSignalService.sendToMultipleUsers(userIdsArray, title, content, 
                    NotificationType.LIVE_CHAT, conversationId);
            
            log.info("Sent live chat push notification to {} managers/admins", userIdsArray.length);
        } catch (Exception e) {
            log.error("Failed to send push notification for live chat", e);
        }
    }

    /**
     * Gửi push notification cho User khi Manager/Admin trả lời
     */
    private void sendPushToUser(User user, String title, String content, Long conversationId) {
        try {
            oneSignalService.sendToUser(String.valueOf(user.getId()), title, content,
                    NotificationType.LIVE_CHAT, conversationId);
            log.info("Sent live chat push notification to user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send push notification to user", e);
        }
    }

    /**
     * Cắt ngắn tin nhắn để hiển thị trong notification
     */
    private String truncateMessage(String message) {
        if (message == null) return "";
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
}
