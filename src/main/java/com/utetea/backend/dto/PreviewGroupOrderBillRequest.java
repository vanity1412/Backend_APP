package com.utetea.backend.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PreviewGroupOrderBillRequest {
    
    // Sử dụng String thay vì PaymentMethod enum để tương thích với client
    private String paymentMethod;
    
    private String promotionCode;
    
    private String spinVoucherCode;
    
    // Phí ship từ client (tính theo VietnamProvinces)
    private Integer shippingFee;
}
