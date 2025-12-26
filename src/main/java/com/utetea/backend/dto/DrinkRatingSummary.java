package com.utetea.backend.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrinkRatingSummary {
    private Long drinkId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution; // rating -> count
}
