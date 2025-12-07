package com.utetea.backend.dto;

import com.utetea.backend.model.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePromotionRequest {
    
    @Size(max = 255, message = "Mô tả không được quá 255 ký tự")
    private String description;
    
    private DiscountType discountType;
    
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng tối thiểu phải >= 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minOrderValue;
    
    @DecimalMin(value = "0.00", message = "Giảm giá tối đa phải >= 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maxDiscountAmount;
    
    @Min(value = 0, message = "Giới hạn sử dụng phải >= 0")
    private Integer usageLimit;
    
    private Boolean isActive;
}
