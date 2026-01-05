package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderResponse {
    private CartDto cart;
    private List<ReorderItemStatus> itemStatuses;
    private boolean hasUnavailableItems;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReorderItemStatus {
        private String drinkName;
        private String sizeName;
        private boolean drinkAvailable;
        private boolean sizeAvailable;
        private List<ToppingStatus> toppingStatuses;
        private boolean addedToCart;
        private String reason;
        private List<SuggestionDto> suggestions;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToppingStatus {
        private String toppingName;
        private boolean available;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuggestionDto {
        private Long drinkId;
        private String drinkName;
        private String drinkImage;
        private Double basePrice;
        private String reason;
    }
}
