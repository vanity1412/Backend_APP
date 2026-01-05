package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity lưu trữ lịch sử hoàn thành Challenge của user
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "challenge_completions")
public class ChallengeCompletion extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    
    /**
     * Số điểm đã nhận
     */
    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned;
    
    /**
     * Tên sản phẩm đã mua (cho challenge SAME_PRODUCT_IN_ORDER)
     */
    @Column(name = "drink_name", length = 100)
    private String drinkName;
    
    /**
     * Số lượng đã mua
     */
    @Column(name = "quantity_achieved")
    private Integer quantityAchieved;
}
