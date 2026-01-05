package com.utetea.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO để cập nhật đầy đủ thông tin cart item
 * Bao gồm: số lượng, size, toppings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity must not exceed 100")
    private Integer quantity;
    
    private Long sizeId;
    
    private List<Long> toppingIds;
    
    private String note;
}
