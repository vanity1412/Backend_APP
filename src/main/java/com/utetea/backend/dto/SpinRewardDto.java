package com.utetea.backend.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinRewardDto {
    private Long id;
    private String voucherCode; // Mã voucher 10 ký tự
    private Integer discountPercent;
    private String discountLabel;
    private Boolean isUsed;
    private Instant createdAt;
}
