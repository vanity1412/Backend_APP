package com.utetea.backend.dto;

import com.utetea.backend.model.PaymentMethod;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PreviewGroupOrderBillRequest {
    
    private PaymentMethod paymentMethod;
    
    private String promotionCode;
    
    private String spinVoucherCode;
    
    // Phí ship từ client (tính theo VietnamProvinces)
    private Integer shippingFee;
}
