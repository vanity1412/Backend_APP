package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "group_orders")
public class GroupOrder extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id")
    private User hostUser;
    
    @Column(name = "invite_code", unique = true, nullable = false, length = 10)
    private String inviteCode;
    
    @Column(length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupOrderStatus status = GroupOrderStatus.OPEN;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;
    
    @Column(length = 255)
    private String deliveryAddress;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "max_members")
    private Integer maxMembers = 10;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_order_id")
    private Order finalOrder;
    
    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupOrderMember> members = new HashSet<>();
    
    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupOrderItem> items = new HashSet<>();
}
