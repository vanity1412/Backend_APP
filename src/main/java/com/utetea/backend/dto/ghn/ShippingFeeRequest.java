package com.utetea.backend.dto.ghn;

import lombok.Data;

/**
 * Request đơn giản để tính phí giao hàng từ client
 */
@Data
public class ShippingFeeRequest {
    /**
     * District ID nơi giao hàng
     */
    private int toDistrictId;
    
    /**
     * Ward code nơi giao hàng
     */
    private String toWardCode;
    
    /**
     * Cân nặng (gram) - mặc định 500g cho đồ uống
     */
    private Integer weight;
    
    /**
     * Giá trị đơn hàng để bảo hiểm
     */
    private Integer insuranceValue;
    
    /**
     * Số tiền thu hộ COD
     */
    private Integer codValue;
    
    /**
     * Service type: 1 = Express, 2 = Standard (mặc định)
     */
    private Integer serviceTypeId;
}
