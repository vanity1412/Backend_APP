package com.utetea.backend.dto;

import com.utetea.backend.model.OrderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateGroupOrderRequest {
    
    @Size(max = 100, message = "Tên nhóm không quá 100 ký tự")
    private String name;
    
    private Long storeId;
    
    private OrderType orderType;
    
    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    private String deliveryAddress;
    
    @Min(value = 2, message = "Số thành viên tối thiểu là 2")
    @Max(value = 20, message = "Số thành viên tối đa là 20")
    private Integer maxMembers = 10;
    
    // Thời gian hết hạn (phút), mặc định 60 phút
    @Min(value = 15, message = "Thời gian tối thiểu là 15 phút")
    @Max(value = 1440, message = "Thời gian tối đa là 24 giờ")
    private Integer expirationMinutes = 60;
}
