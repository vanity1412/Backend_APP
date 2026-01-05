package com.utetea.backend.model;

/**
 * Các loại Challenge trong hệ thống
 */
public enum ChallengeType {
    /**
     * Challenge mua nhiều sản phẩm giống nhau trong 1 đơn hàng
     */
    SAME_PRODUCT_IN_ORDER,
    
    /**
     * Challenge đặt nhiều đơn hàng
     */
    MULTIPLE_ORDERS,
    
    /**
     * Challenge chi tiêu tổng cộng
     */
    TOTAL_SPENDING
}
