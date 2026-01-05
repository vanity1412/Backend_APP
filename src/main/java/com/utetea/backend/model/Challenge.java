package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity lưu trữ các loại Challenge trong hệ thống
 * Ví dụ: Mua 3 sản phẩm giống nhau được cộng 5 điểm
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "challenges")
public class Challenge extends AuditEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 50)
    private ChallengeType challengeType;
    
    /**
     * Số lượng cần đạt để hoàn thành challenge
     * Ví dụ: 3 sản phẩm giống nhau
     */
    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;
    
    /**
     * Số điểm thưởng khi hoàn thành challenge
     */
    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints;
    
    /**
     * Challenge có đang hoạt động không
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
