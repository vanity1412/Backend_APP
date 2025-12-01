package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.UserRole;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileDto {
    private Long id;
    private String username;
    private String phone;
    private String fullName;
    private String address;
    private UserRole role;
    private MemberTier memberTier;
    private Integer points;
    private Boolean active;
}
