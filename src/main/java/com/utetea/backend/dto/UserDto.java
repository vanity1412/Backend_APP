package com.utetea.backend.dto;

import com.utetea.backend.model.MemberTier;
import com.utetea.backend.model.User;
import com.utetea.backend.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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
    
    // Thông tin về stores được quản lý (chỉ cho Manager)
    private List<ManagedStoreInfo> managedStores;
    private Boolean isSuperManager;
    
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
        
        // Thêm thông tin managed stores nếu là Manager
        if (user.getRole() == UserRole.MANAGER && user.getManagedStores() != null) {
            this.managedStores = user.getManagedStores().stream()
                .map(store -> new ManagedStoreInfo(store.getId(), store.getStoreName()))
                .collect(Collectors.toList());
            this.isSuperManager = user.getManagedStores().isEmpty();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagedStoreInfo {
        private Long id;
        private String storeName;
    }
}
