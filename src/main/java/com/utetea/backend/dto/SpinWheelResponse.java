package com.utetea.backend.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinWheelResponse {
    private Long rewardId;
    private String voucherCode; // Mã voucher 10 ký tự
    private Integer discountPercent;
    private String discountLabel;
    private Integer winIndex;
    private List<Integer> wheelItems;
    private Integer remainingPoints;
    private String message;
}
