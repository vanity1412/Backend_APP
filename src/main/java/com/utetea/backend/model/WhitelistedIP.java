package com.utetea.backend.model;

import com.utetea.backend.model.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 🔓 WHITELISTED IP - IP được phép truy cập
 * IP trong whitelist sẽ LUÔN được phép truy cập, bỏ qua mọi block
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "whitelisted_ips", indexes = {
    @Index(name = "idx_whitelist_ip", columnList = "ip_address"),
    @Index(name = "idx_whitelist_active", columnList = "is_active")
})
public class WhitelistedIP extends AuditEntity {

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String description;

    @Column(name = "added_by_id")
    private Long addedById;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
