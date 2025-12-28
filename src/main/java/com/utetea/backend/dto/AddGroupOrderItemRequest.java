package com.utetea.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddGroupOrderItemRequest {
    
    @NotNull(message = "Drink ID không được để trống")
    private Long drinkId;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Max(value = 20, message = "Số lượng tối đa là 20")
    private Integer quantity;
    
    private String sizeName;
    
    private List<Long> toppingIds;
    
    @Size(max = 255, message = "Ghi chú không quá 255 ký tự")
    private String note;
}
