package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "users")
public class User extends AuditEntity {
    
    @Column(unique = true, length = 50)
    private String username;
    
    @Column(unique = true, length = 100)
    private String email;
    
    @Column(unique = true, length = 15, nullable = true)
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

    @Column(length = 255)
    private String avatarUrl;
    
    /**
     * Danh sách các cửa hàng mà Manager được phép quản lý
     * Nếu rỗng = quản lý tất cả (Super Manager)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "manager_stores",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private Set<Store> managedStores = new HashSet<>();
    
    /**
     * Kiểm tra xem user có quyền quản lý store này không
     * ADMIN có quyền quản lý tất cả
     * Manager PHẢI có store được gán mới có quyền
     */
    public boolean canManageStore(Long storeId) {
        // ADMIN có quyền quản lý tất cả stores
        if (this.role == UserRole.ADMIN) {
            return true;
        }
        // Nếu không phải Manager thì không có quyền
        if (this.role != UserRole.MANAGER) {
            return false;
        }
        // Manager PHẢI có stores được gán
        if (this.managedStores == null || this.managedStores.isEmpty()) {
            return false; // Không có store nào được gán -> không có quyền
        }
        // Kiểm tra store có trong danh sách được gán không
        return this.managedStores.stream()
            .anyMatch(store -> store.getId().equals(storeId));
    }
    
    /**
     * Kiểm tra xem Manager có được gán store nào không
     * @deprecated Không còn khái niệm Super Manager nữa
     */
    @Deprecated
    public boolean isSuperManager() {
        // Không còn Super Manager - Manager phải có store được gán
        return false;
    }
    
    /**
     * Kiểm tra xem Manager có được gán store nào không
     */
    public boolean hasAssignedStores() {
        return this.role == UserRole.MANAGER && 
               this.managedStores != null && 
               !this.managedStores.isEmpty();
    }
}
