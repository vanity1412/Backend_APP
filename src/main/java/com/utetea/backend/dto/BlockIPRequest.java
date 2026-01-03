package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🚫 Request để block IP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockIPRequest {
    private String ipAddress;
    private String blockType; // TEMPORARY, PERMANENT
    private String reason;
    private Integer durationHours; // Cho TEMPORARY
    private Long relatedUserId;
    private Long alertId;
}
