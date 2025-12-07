package com.utetea.backend.dto;

import com.utetea.backend.model.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePromotionRequest {
    
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher không được quá 50 ký tự")
    private String code;
    
    @Size(max = 255, message = "Mô tả không được quá 255 ký tự")
    private String description;
    
    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;
    
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
    
    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng tối thiểu phải >= 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minOrderValue;
    
    @DecimalMin(value = "0.00", message = "Giảm giá tối đa phải >= 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maxDiscountAmount;
    
    @Min(value = 0, message = "Giới hạn sử dụng phải >= 0")
    private Integer usageLimit;
    
    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;
}
