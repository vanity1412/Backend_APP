package com.utetea.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummaryDto {
    private Map<String, Long> ordersByStatus;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private Long totalOrders;
    private Long todayOrders;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
