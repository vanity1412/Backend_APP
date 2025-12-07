package com.utetea.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("role")
    private UserRole role;
    
    @JsonProperty("memberTier")
    private MemberTier memberTier;
    
    private String token;
    private String refreshToken;

    @JsonProperty("avatarUrl")
    private String avatarUrl;
}
