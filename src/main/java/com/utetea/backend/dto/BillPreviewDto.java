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
    private BigDecimal subtotal;            // Tổng tiền hàng
    
    // Phí giao hàng (GHN)
    private BigDecimal shippingFee;         // Phí giao hàng (sau khi áp dụng free ship)
    private BigDecimal originalShippingFee; // Phí giao hàng gốc (trước khi áp dụng free ship)
    private Integer ghnDistrictId;          // District ID cho GHN
    private String ghnWardCode;             // Ward code cho GHN
    private boolean freeShipping;           // Có được miễn phí ship không
    private String freeShippingReason;      // Lý do miễn phí ship (VD: "Miễn phí ship cho hạng GOLD")
    
    // Chi tiết giảm giá
    private String promotionCode;           // Mã voucher (nếu có)
    private BigDecimal voucherDiscount;     // Số tiền giảm từ voucher
    private String tierName;                // Tên hạng thành viên (BRONZE, SILVER, GOLD, PLATINUM)
    private BigDecimal tierDiscountAmount;  // Số tiền giảm từ hạng thành viên
    
    private BigDecimal totalDiscount;       // Tổng giảm giá (voucher + tier)
    private BigDecimal finalPrice;          // Thành tiền (subtotal + shippingFee - totalDiscount)
    
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
