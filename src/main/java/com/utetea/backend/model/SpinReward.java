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
    
    @Column(name = "voucher_code", unique = true, nullable = false, length = 10)
    private String voucherCode; // Mã 10 ký tự
    
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent; // 0, 10, 20, 50, 100
    
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed = 5;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
}
