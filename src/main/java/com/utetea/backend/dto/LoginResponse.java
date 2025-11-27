package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.UserRole;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String username;
    private String phone;
    private String fullName;
    private String address;
    private UserRole role;
    private MemberTier memberTier;
    private String token;
}
