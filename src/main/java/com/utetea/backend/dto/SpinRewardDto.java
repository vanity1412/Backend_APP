package com.utetea.backend.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpinRewardDto {
    private Long id;
    private Long drinkId;
    private String drinkName;
    private String drinkImage;
    private Double drinkPrice;
    private Boolean isRedeemed;
    private Instant createdAt;
}
