package com.utetea.backend.model;

public enum ConversationStatus {
    WAITING,    // Đang chờ manager tiếp nhận
    ACTIVE,     // Đang chat
    RESOLVED,   // Đã giải quyết
    CLOSED      // Đã đóng
}
