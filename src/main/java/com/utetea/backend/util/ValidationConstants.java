package com.utetea.backend.util;

import java.math.BigDecimal;

/**
 * ✅ SECURITY: Constants để validate input và prevent abuse
 */
public class ValidationConstants {
    
    // Số lượng sản phẩm
    public static final int MAX_QUANTITY_PER_ITEM = 99;
    public static final int MIN_QUANTITY = 1;
    
    // Order
    public static final int MAX_ITEMS_PER_ORDER = 50;
    public static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("50000000"); // 50 triệu VNĐ
    public static final BigDecimal MIN_TOTAL_AMOUNT = new BigDecimal("1000"); // 1k VNĐ
    
    // Giá tiền (để detect giá trị bất thường)
    public static final BigDecimal MAX_REASONABLE_PRICE = new BigDecimal("1000000"); // 1 triệu VNĐ/món
    public static final BigDecimal MIN_REASONABLE_PRICE = new BigDecimal("1000"); // 1k VNĐ/món
    
    // Rate limiting
    public static final int OTP_MAX_ATTEMPTS_PER_PHONE = 5; // 5 lần/giờ
    public static final int SPIN_MAX_ATTEMPTS_PER_DAY = 10; // 10 lần/ngày
    public static final int ORDER_MAX_ATTEMPTS_PER_HOUR = 20; // 20 đơn/giờ
    
    // Voucher
    public static final int MAX_VOUCHER_USAGE_PER_USER = 1; // Mỗi user chỉ dùng 1 lần
    
    private ValidationConstants() {
        // Prevent instantiation
    }
    
    /**
     * Validate số lượng sản phẩm
     */
    public static boolean isValidQuantity(Integer quantity) {
        return quantity != null && quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY_PER_ITEM;
    }
    
    /**
     * Validate số lượng items trong order
     */
    public static boolean isValidOrderSize(int itemCount) {
        return itemCount > 0 && itemCount <= MAX_ITEMS_PER_ORDER;
    }
    
    /**
     * Validate tổng tiền đơn hàng
     */
    public static boolean isValidTotalAmount(BigDecimal amount) {
        return amount != null && 
               amount.compareTo(MIN_TOTAL_AMOUNT) >= 0 && 
               amount.compareTo(MAX_TOTAL_AMOUNT) <= 0;
    }
    
    /**
     * Validate giá tiền có hợp lý không
     */
    public static boolean isReasonablePrice(BigDecimal price) {
        return price != null && 
               price.compareTo(MIN_REASONABLE_PRICE) >= 0 && 
               price.compareTo(MAX_REASONABLE_PRICE) <= 0;
    }
}
