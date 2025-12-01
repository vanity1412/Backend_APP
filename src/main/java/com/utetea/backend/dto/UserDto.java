package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private String address;
    private UserRole role;
    private MemberTier memberTier;
    private Integer points;
    private Boolean active;
    private Boolean isBlocked;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Constructor from User entity
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.fullName = user.getFullName();
        this.address = user.getAddress();
        this.role = user.getRole();
        this.memberTier = user.getMemberTier();
        this.points = user.getPoints();
        this.active = user.getActive();
        this.isBlocked = user.getIsBlocked();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
