package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "group_order_items")
public class GroupOrderItem extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_order_id")
    private GroupOrder groupOrder;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id")
    private Drink drink;
    
    @Column(name = "drink_name_snapshot", nullable = false, length = 100)
    private String drinkNameSnapshot;
    
    @Column(name = "size_name", length = 20)
    private String sizeName;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "item_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemPrice;
    
    @Column(length = 255)
    private String note;
    
    // Lưu toppingIds dưới dạng JSON string, VD: "1,2,3"
    @Column(name = "topping_ids", length = 500)
    private String toppingIdsString;
    
    @Column(name = "toppings_snapshot", length = 500)
    private String toppingsSnapshot;
}
