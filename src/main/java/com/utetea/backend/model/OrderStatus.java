package com.utetea.backend.model;

public enum OrderStatus {
    PENDING,    // Chờ xác nhận
    MAKING,     // Đang pha chế
    SHIPPING,   // Đang giao hàng (Delivery)
    READY,      // Sẵn sàng lấy hàng (Pickup)
    DONE,       // Hoàn thành
    CANCELED    // Đã hủy
}
