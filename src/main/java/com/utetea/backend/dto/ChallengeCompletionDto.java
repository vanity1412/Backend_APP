package com.utetea.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCompletionDto {
    private Long id;
    private Long challengeId;
    private String challengeName;
    private Long orderId;
    private Integer pointsEarned;
    private String drinkName;
    private Integer quantityAchieved;
    private LocalDateTime completedAt;
}
