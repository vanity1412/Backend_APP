package com.utetea.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long drinkId;
    private String drinkName;
    private String drinkImage;
    private Long sizeId;
    private String sizeName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private List<DrinkToppingDto> toppings;
    private String note;
}
