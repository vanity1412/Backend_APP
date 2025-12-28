package com.utetea.backend.dto;

import com.utetea.backend.model.OrderType;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateGroupOrderRequest {
    
    @Size(max = 100, message = "Tên nhóm không quá 100 ký tự")
    private String name;
    
    private Long storeId;
    
    private OrderType orderType;
    
    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    private String deliveryAddress;
}
