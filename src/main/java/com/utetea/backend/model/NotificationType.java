package com.utetea.backend.model;

public enum NotificationType {
    ORDER_NEW,           // Đơn hàng mới (cho Manager)
    ORDER_STATUS,        // Cập nhật trạng thái đơn (cho User)
    PROMOTION,           // Thông báo khuyến mãi
    SYSTEM,              // Thông báo hệ thống
    CUSTOM,              // Thông báo tùy chỉnh từ Manager
    LIVE_CHAT,           // Tin nhắn tư vấn mới (cho Manager/Admin)
    GROUP_CHAT           // Tin nhắn nhóm mới (cho members)
}
