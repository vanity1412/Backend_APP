package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private Long drinkId;
    private Long sizeId;
    private Integer quantity;
    private List<Long> toppingIds;
    private String note;
}
