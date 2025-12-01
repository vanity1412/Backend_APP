package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "users")
public class User extends AuditEntity {
    
    @Column(unique = true, length = 50)
    private String username;
    
    @Column(unique = true, length = 100)
    private String email;
    
    @Column(unique = true, length = 15)
    private String phone;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(name = "full_name", length = 100)
    private String fullName;
    
    @Column(length = 255)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "member_tier", length = 20)
    private MemberTier memberTier = MemberTier.BRONZE;
    
    @Column(nullable = false)
    private Integer points = 0;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private String otp;
    
    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;
    
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;
}
