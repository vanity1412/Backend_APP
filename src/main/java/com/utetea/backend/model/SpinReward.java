package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "spin_rewards")
public class SpinReward extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id")
    private Drink wonDrink;
    
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed = 5;
    
    @Column(name = "is_redeemed", nullable = false)
    private Boolean isRedeemed = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_order_id")
    private Order redeemedOrder;
}
