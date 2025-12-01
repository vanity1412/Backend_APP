package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "order_items")
public class OrderItem extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id")
    private Drink drink;
    
    @Column(name = "drink_name_snapshot", length = 100)
    private String drinkNameSnapshot;
    
    @Column(name = "size_name_snapshot", length = 50)
    private String sizeNameSnapshot;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "item_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemPrice;
    
    @Column(length = 255)
    private String note;
    
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemTopping> toppings = new ArrayList<>();
}
