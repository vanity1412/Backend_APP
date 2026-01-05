package com.utetea.backend.dto;

import com.utetea.backend.model.ChallengeType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private Long id;
    private String name;
    private String description;
    private ChallengeType challengeType;
    private Integer requiredQuantity;
    private Integer rewardPoints;
    private Boolean isActive;
}
