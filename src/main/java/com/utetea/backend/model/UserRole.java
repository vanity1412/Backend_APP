package com.utetea.backend.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    USER,
    MANAGER;
    
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
