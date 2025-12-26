package com.utetea.backend.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointsDto {
    private Integer currentPoints;
    private Integer pointsToSpin; // Số điểm cần để quay (5)
    private Boolean canSpin;
    private List<SpinRewardDto> availableRewards; // Phần thưởng chưa sử dụng
}
