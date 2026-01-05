package com.utetea.backend.model;

public enum NotificationType {
    ORDER_NEW,           // Đơn hàng mới (cho Manager/Admin)
    ORDER_STATUS,        // Cập nhật trạng thái đơn (cho User)
    PAYMENT_SUCCESS,     // Thanh toán thành công (cho User)
    PROMOTION,           // Thông báo khuyến mãi
    SYSTEM,              // Thông báo hệ thống
    CUSTOM,              // Thông báo tùy chỉnh từ Manager
    LIVE_CHAT,           // Tin nhắn tư vấn mới (cho Manager/Admin)
    GROUP_CHAT,          // Tin nhắn nhóm mới (cho members)
    
    // 🎁 Loyalty & Rewards
    SPIN_VOUCHER,        // Trúng voucher vòng xoay (cho User)
    CHALLENGE_COMPLETE,  // Hoàn thành challenge (cho User)
    TIER_UPGRADE,        // Lên cấp member tier (cho User)
    
    // 🛡️ User Monitoring Alerts
    SECURITY_ALERT,      // Cảnh báo bảo mật cho Admin/Manager
    USER_BLOCKED,        // Thông báo user bị khóa
    HIGH_RISK_USER       // Cảnh báo user có điểm rủi ro cao
}
