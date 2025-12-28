package com.utetea.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrderMemberDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Boolean isHost;
    private LocalDateTime joinedAt;
    private Integer itemCount;
}
