package com.utetea.backend.model;

public enum GroupChatMessageType {
    TEXT,           // Tin nhắn văn bản thường
    SYSTEM,         // Thông báo hệ thống (ai vào/ra, khóa đơn...)
    ITEM_ADDED,     // Thông báo thêm món
    ITEM_REMOVED    // Thông báo xóa món
}
