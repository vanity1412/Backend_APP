package com.utetea.backend.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeStatusDto {
    /**
     * Danh sách các challenge đang hoạt động
     */
    private List<ChallengeDto> activeChallenges;
    
    /**
     * Lịch sử hoàn thành challenge của user
     */
    private List<ChallengeCompletionDto> completionHistory;
    
    /**
     * Tổng điểm đã nhận từ challenge
     */
    private Integer totalPointsFromChallenges;
}
