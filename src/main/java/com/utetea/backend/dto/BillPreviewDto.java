package com.utetea.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillPreviewDto {
    
    // Thông tin khách hàng
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    
    // Thông tin cửa hàng
    private Long storeId;
    private String storeName;
    private String storeAddress;
    
    // Thông tin đơn hàng
    private String orderType; // DELIVERY / PICKUP
    private String deliveryAddress;
    private String pickupTime;
    private String paymentMethod;
    
    // Chi tiết sản phẩm
    private List<BillItemDto> items;
    
    // Tổng tiền
    private BigDecimal subtotal;        // Tổng tiền hàng
    private BigDecimal discount;        // Giảm giá (voucher + tier)
    private String promotionCode;       // Mã voucher (nếu có)
    private String tierDiscount;        // Giảm giá theo tier (nếu có)
    private BigDecimal finalPrice;      // Thành tiền
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BillItemDto {
        private String drinkName;
        private String drinkImage;
        private String sizeName;
        private List<String> toppings;
        private Integer quantity;
        private BigDecimal unitPrice;   // Giá 1 sản phẩm (đã bao gồm size + topping)
        private BigDecimal totalPrice;  // Giá x số lượng
        private String note;
    }
}
