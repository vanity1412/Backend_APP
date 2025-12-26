package com.utetea.backend.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long drinkId;
    private String drinkName;
    private Long orderId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private Boolean isAnonymous;
    private Instant createdAt;
}
