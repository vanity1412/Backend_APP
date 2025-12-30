package com.utetea.backend.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    USER,
    MANAGER,
    ADMIN;  // Admin - quản lý cao nhất, có quyền cấp quyền cho Manager
    
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
