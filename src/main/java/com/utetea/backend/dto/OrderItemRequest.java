package com.utetea.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderItemRequest {
    @NotNull
    private Long drinkId;
    
    @NotNull
    private String sizeName;
    
    @NotNull
    private Integer quantity;
    
    private String note;
    
    private List<Long> toppingIds;
}
