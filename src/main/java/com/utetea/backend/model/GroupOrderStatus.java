package com.utetea.backend.model;

public enum GroupOrderStatus {
    OPEN,       // Đang mở, có thể thêm thành viên và món
    LOCKED,     // Đã khóa, không thể thêm thành viên/món, chờ thanh toán
    COMPLETED,  // Đã thanh toán và tạo đơn hàng
    CANCELLED,  // Đã hủy
    EXPIRED     // Hết hạn
}
