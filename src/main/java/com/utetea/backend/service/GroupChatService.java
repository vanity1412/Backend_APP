package com.utetea.backend.service;

import com.utetea.backend.dto.GroupChatMessageDto;
import com.utetea.backend.dto.SendGroupChatRequest;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.GroupChatMessageRepository;
import com.utetea.backend.repository.GroupOrderMemberRepository;
import com.utetea.backend.repository.GroupOrderRepository;
import com.utetea.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {
    
    private final GroupChatMessageRepository chatMessageRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public GroupChatMessageDto sendMessage(String username, Long groupOrderId, SendGroupChatRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiên đặt hàng nhóm không tồn tại"));
        
        // Kiểm tra user có phải thành viên không
        boolean isMember = memberRepository.existsByGroupOrderIdAndUserId(groupOrderId, user.getId());
        if (!isMember) {
            throw new IllegalStateException("Bạn không phải thành viên của phiên này");
        }
        
        // Kiểm tra phiên còn hoạt động không
        if (groupOrder.getStatus() == GroupOrderStatus.COMPLETED || 
            groupOrder.getStatus() == GroupOrderStatus.CANCELLED) {
            throw new IllegalStateException("Phiên đã kết thúc, không thể gửi tin nhắn");
        }
        
        GroupChatMessage message = GroupChatMessage.builder()
                .groupOrder(groupOrder)
                .sender(user)
                .content(request.getContent())
                .messageType(GroupChatMessageType.TEXT)
                .build();
        
        message = chatMessageRepository.save(message);
        GroupChatMessageDto dto = toDto(message);
        
        // Broadcast tin nhắn qua WebSocket
        messagingTemplate.convertAndSend("/topic/group-chat/" + groupOrderId, dto);
        
        return dto;
    }
    
    @Transactional
    public void sendSystemMessage(Long groupOrderId, String content, GroupChatMessageType type) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId).orElse(null);
        if (groupOrder == null) return;
        
        User hostUser = groupOrder.getHostUser();
        
        GroupChatMessage message = GroupChatMessage.builder()
                .groupOrder(groupOrder)
                .sender(hostUser) // System messages attributed to host
                .content(content)
                .messageType(type)
                .build();
        
        message = chatMessageRepository.save(message);
        GroupChatMessageDto dto = toDto(message);
        
        // Broadcast tin nhắn hệ thống
        messagingTemplate.convertAndSend("/topic/group-chat/" + groupOrderId, dto);
    }
    
    @Transactional(readOnly = true)
    public List<GroupChatMessageDto> getChatHistory(String username, Long groupOrderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        
        // Kiểm tra user có phải thành viên không
        boolean isMember = memberRepository.existsByGroupOrderIdAndUserId(groupOrderId, user.getId());
        if (!isMember) {
            throw new IllegalStateException("Bạn không phải thành viên của phiên này");
        }
        
        return chatMessageRepository.findByGroupOrderIdOrderByCreatedAtAsc(groupOrderId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<GroupChatMessageDto> getRecentMessages(String username, Long groupOrderId, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        
        boolean isMember = memberRepository.existsByGroupOrderIdAndUserId(groupOrderId, user.getId());
        if (!isMember) {
            throw new IllegalStateException("Bạn không phải thành viên của phiên này");
        }
        
        return chatMessageRepository.findByGroupOrderIdOrderByCreatedAtDesc(groupOrderId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    private GroupChatMessageDto toDto(GroupChatMessage message) {
        User sender = message.getSender();
        return GroupChatMessageDto.builder()
                .id(message.getId())
                .groupOrderId(message.getGroupOrder().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
