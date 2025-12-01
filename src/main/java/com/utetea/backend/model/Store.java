package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "stores")
public class Store extends AuditEntity {
    
    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;
    
    @Column(nullable = false, length = 255)
    private String address;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(name = "open_time")
    private LocalTime openTime;
    
    @Column(name = "close_time")
    private LocalTime closeTime;
    
    @Column(length = 20)
    private String phone;
}
