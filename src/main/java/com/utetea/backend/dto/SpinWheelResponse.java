package com.utetea.backend.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinWheelResponse {
    private Long rewardId;
    private DrinkDto wonDrink;
    private Integer winIndex; // Vị trí trúng thưởng (0-4)
    private List<DrinkDto> wheelDrinks; // 5 món trên vòng quay
    private Integer remainingPoints;
}
