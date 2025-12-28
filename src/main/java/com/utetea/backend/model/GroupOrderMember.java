package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "group_order_members", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_order_id", "user_id"}))
public class GroupOrderMember extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_order_id")
    private GroupOrder groupOrder;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "is_host", nullable = false)
    private Boolean isHost = false;
}
